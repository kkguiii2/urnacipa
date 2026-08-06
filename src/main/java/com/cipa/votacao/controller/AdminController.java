package com.cipa.votacao.controller;

import com.cipa.votacao.entity.Candidato;
import com.cipa.votacao.entity.Usuario;
import com.cipa.votacao.service.CandidatoService;
import com.cipa.votacao.service.ConfiguracaoService;
import com.cipa.votacao.service.RelatorioService;
import com.cipa.votacao.service.UploadService;
import com.cipa.votacao.service.UsuarioService;
import com.cipa.votacao.service.VotacaoService;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UsuarioService usuarioService;
    private final CandidatoService candidatoService;
    private final ConfiguracaoService configuracaoService;
    private final VotacaoService votacaoService;
    private final RelatorioService relatorioService;
    private final UploadService uploadService;

    @GetMapping("/login")
    public String adminLogin() {
        return "admin/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsuarios", usuarioService.contarTotalAtivos());
        model.addAttribute("totalVotos", votacaoService.contarTotalVotos());
        model.addAttribute("participacao", calcularParticipacao());
        model.addAttribute("configuracao", configuracaoService.getConfiguracao());
        return "admin/dashboard";
    }

    @GetMapping("/usuarios")
    public String listarUsuarios(Model model, HttpSession session) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute(
                "resultadoImportacao",
                session.getAttribute(ImportacaoEleitoresController.RESULTADO_SESSION_ATTRIBUTE));
        model.addAttribute("cadastroBloqueado", configuracaoService.isCadastroBloqueado());
        return "admin/usuarios";
    }

    @PostMapping("/usuarios/adicionar")
    public String adicionarUsuario(
            @RequestParam String matricula,
            @RequestParam String nome,
            RedirectAttributes redirectAttributes) {
        if (cadastroBloqueado(redirectAttributes)) {
            return "redirect:/admin/usuarios";
        }
        String matriculaNormalizada = matricula == null ? "" : matricula.trim();
        String nomeNormalizado = nome == null ? "" : nome.trim();
        if (!matriculaNormalizada.matches("^[0-9]{1,20}$")) {
            redirectAttributes.addFlashAttribute("erro", "Matrícula deve conter de 1 a 20 números.");
            return "redirect:/admin/usuarios";
        }
        if (nomeNormalizado.length() < 2 || nomeNormalizado.length() > 255) {
            redirectAttributes.addFlashAttribute("erro", "Nome deve ter entre 2 e 255 caracteres.");
            return "redirect:/admin/usuarios";
        }
        if (usuarioService.existePorMatricula(matriculaNormalizada)) {
            redirectAttributes.addFlashAttribute("erro", "Matrícula já cadastrada.");
            return "redirect:/admin/usuarios";
        }
        try {
            Usuario usuario = new Usuario();
            usuario.setMatricula(matriculaNormalizada);
            usuario.setNome(nomeNormalizado);
            usuario.setAtivo(true);
            usuario.setVotou(false);
            usuarioService.salvar(usuario);
            redirectAttributes.addFlashAttribute("sucesso", "Usuário adicionado com sucesso.");
        } catch (RuntimeException e) {
            log.error("Erro ao adicionar usuário.", e);
            redirectAttributes.addFlashAttribute("erro", "Não foi possível adicionar o usuário.");
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/excluir/{id}")
    public String excluirUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (cadastroBloqueado(redirectAttributes)) {
            return "redirect:/admin/usuarios";
        }
        try {
            usuarioService.excluir(id);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erro", "Não foi possível excluir o usuário.");
        }
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/candidatos")
    public String listarCandidatos(Model model) {
        model.addAttribute("candidatos", candidatoService.listarTodos());
        model.addAttribute("cadastroBloqueado", configuracaoService.isCadastroBloqueado());
        return "admin/candidatos";
    }

    @PostMapping("/candidatos/adicionar")
    public String adicionarCandidato(
            @RequestParam Integer numero,
            @RequestParam String nome,
            @RequestParam(required = false) MultipartFile foto,
            RedirectAttributes redirectAttributes) {
        if (cadastroBloqueado(redirectAttributes)) {
            return "redirect:/admin/candidatos";
        }
        String nomeNormalizado = nome == null ? "" : nome.trim();
        if (numero == null || numero < 1 || numero > 99
                || nomeNormalizado.length() < 2 || nomeNormalizado.length() > 255) {
            redirectAttributes.addFlashAttribute("erro", "Informe número e nome válidos.");
            return "redirect:/admin/candidatos";
        }
        if (candidatoService.existeNumero(numero)) {
            redirectAttributes.addFlashAttribute("erro", "Número de candidato já existe.");
            return "redirect:/admin/candidatos";
        }
        try {
            Candidato candidato = new Candidato();
            candidato.setNumero(numero);
            candidato.setNome(nomeNormalizado);
            candidato.setAtivo(true);
            if (foto != null && !foto.isEmpty()) {
                candidato.setFoto(uploadService.salvarImagem(foto));
            }
            candidatoService.salvar(candidato);
            redirectAttributes.addFlashAttribute("sucesso", "Candidato adicionado com sucesso.");
        } catch (IOException e) {
            log.warn("Upload de foto de candidato rejeitado: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("erro", "Foto inválida. Use JPEG ou PNG com até 5 MB.");
        } catch (RuntimeException e) {
            log.error("Erro ao adicionar candidato.", e);
            redirectAttributes.addFlashAttribute("erro", "Não foi possível adicionar o candidato.");
        }
        return "redirect:/admin/candidatos";
    }

    @PostMapping("/candidatos/toggle/{id}")
    public String toggleCandidato(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (cadastroBloqueado(redirectAttributes)) {
            return "redirect:/admin/candidatos";
        }
        candidatoService.buscarPorId(id)
                .ifPresent(candidato -> candidatoService.ativarDesativar(id, !candidato.isAtivo()));
        return "redirect:/admin/candidatos";
    }

    @PostMapping("/candidatos/excluir/{id}")
    public String excluirCandidato(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (cadastroBloqueado(redirectAttributes)) {
            return "redirect:/admin/candidatos";
        }
        try {
            candidatoService.excluir(id);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erro", e instanceof IllegalStateException
                    ? e.getMessage()
                    : "Não foi possível excluir o candidato.");
        }
        return "redirect:/admin/candidatos";
    }

    @GetMapping("/configuracao")
    public String configuracao(Model model) {
        model.addAttribute("configuracao", configuracaoService.getConfiguracao());
        return "admin/configuracao";
    }

    @PostMapping("/configuracao/salvar")
    public String salvarConfiguracao(
            @RequestParam String dataInicio,
            @RequestParam String dataFim,
            RedirectAttributes redirectAttributes) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            configuracaoService.configurarEleicao(
                    LocalDateTime.parse(dataInicio, formatter),
                    LocalDateTime.parse(dataFim, formatter));
            redirectAttributes.addFlashAttribute("sucesso", "Configuração salva com sucesso.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erro", "Informe um período válido com a eleição fechada.");
        }
        return "redirect:/admin/configuracao";
    }

    @PostMapping("/eleicao/abrir")
    public String abrirEleicao(RedirectAttributes redirectAttributes) {
        try {
            configuracaoService.abrirEleicao();
            redirectAttributes.addFlashAttribute("sucesso", "Eleição aberta com sucesso!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/eleicao/encerrar")
    public String encerrarEleicao(RedirectAttributes redirectAttributes) {
        configuracaoService.encerrarEleicao();
        try {
            relatorioService.gerarEEnviarRelatorio();
            redirectAttributes.addFlashAttribute("sucesso", "Eleição encerrada e relatório gerado.");
        } catch (RuntimeException e) {
            log.error("Eleição encerrada, mas o envio do relatório falhou.", e);
            redirectAttributes.addFlashAttribute("sucesso", "Eleição encerrada. O envio do relatório falhou.");
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/eleicao/nova")
    public String novaEleicao(RedirectAttributes redirectAttributes) {
        try {
            configuracaoService.criarNovaEleicao();
            redirectAttributes.addFlashAttribute("sucesso", "Nova eleição criada. Configure as datas antes de abrir.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/configuracao";
    }

    @GetMapping("/relatorio")
    public String verRelatorio(Model model, RedirectAttributes redirectAttributes) {
        if (configuracaoService.isEleicaoAberta()) {
            redirectAttributes.addFlashAttribute("erro", "Os resultados ficam disponíveis somente após o encerramento.");
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("relatorio", relatorioService.getResumoResultados());
        List<Map<String, Object>> resultadosEnriquecidos = new ArrayList<>();
        List<Object[]> resultados = votacaoService.getResultados();
        long totalVotos = votacaoService.contarTotalVotos();
        for (Object[] resultado : resultados) {
            Map<String, Object> item = new HashMap<>();
            Long candidatoId = (Long) resultado[0];
            Long votos = (Long) resultado[1];
            double percentual = totalVotos > 0 ? votos * 100.0 / totalVotos : 0;
            String nomeCandidato = candidatoService.buscarPorId(candidatoId)
                    .map(candidato -> candidato.getNumero() + " - " + candidato.getNome())
                    .orElse("Candidato removido");
            item.put("nome", nomeCandidato);
            item.put("votos", votos);
            item.put("percentual", Math.round(percentual * 10.0) / 10.0);
            resultadosEnriquecidos.add(item);
        }
        model.addAttribute("resultados", resultadosEnriquecidos);
        model.addAttribute("totalVotos", totalVotos);
        return "admin/relatorio";
    }

    @GetMapping("/relatorio/download")
    public Object downloadRelatorio(RedirectAttributes redirectAttributes) {
        if (configuracaoService.isEleicaoAberta()) {
            redirectAttributes.addFlashAttribute("erro", "Encerre a eleição antes de baixar resultados.");
            return "redirect:/admin/dashboard";
        }
        byte[] excelBytes = relatorioService.gerarRelatorioExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio_cipa.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(excelBytes.length)
                .body(excelBytes);
    }

    @PostMapping("/relatorio/enviar")
    public String enviarRelatorio(RedirectAttributes redirectAttributes) {
        if (configuracaoService.isEleicaoAberta()) {
            redirectAttributes.addFlashAttribute("erro", "Encerre a eleição antes de enviar resultados.");
            return "redirect:/admin/dashboard";
        }
        try {
            relatorioService.gerarEEnviarRelatorio();
            redirectAttributes.addFlashAttribute("sucesso", "Relatório enviado com sucesso!");
        } catch (RuntimeException e) {
            log.error("Erro ao enviar relatório.", e);
            redirectAttributes.addFlashAttribute("erro", "Não foi possível enviar o relatório.");
        }
        return "redirect:/admin/relatorio";
    }

    private String calcularParticipacao() {
        long total = usuarioService.contarTotalAtivos();
        long votos = votacaoService.contarTotalVotos();
        return total == 0 ? "0%" : String.format("%.1f%%", votos * 100.0 / total);
    }

    private boolean cadastroBloqueado(RedirectAttributes redirectAttributes) {
        if (!configuracaoService.isCadastroBloqueado()) {
            return false;
        }
        redirectAttributes.addFlashAttribute("erro", "Cadastros ficam bloqueados durante a eleição.");
        return true;
    }
}
