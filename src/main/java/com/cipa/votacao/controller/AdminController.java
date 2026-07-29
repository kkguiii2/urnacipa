package com.cipa.votacao.controller;

import com.cipa.votacao.entity.Candidato;
import com.cipa.votacao.entity.ConfiguracaoEleicao;
import com.cipa.votacao.entity.Usuario;
import com.cipa.votacao.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Atende as páginas administrativas de cadastros, configuração da eleição,
 * dashboard e relatórios.
 */
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

    @PostMapping("/login")
    public String adminLoginPost(@RequestParam String username, @RequestParam String senha, HttpSession session, Model model) {
        return "redirect:/admin/dashboard";
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
        List<Usuario> usuarios = usuarioService.listarTodos();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute(
                "resultadoImportacao",
                session.getAttribute(ImportacaoEleitoresController.RESULTADO_SESSION_ATTRIBUTE));
        return "admin/usuarios";
    }

    @PostMapping("/usuarios/adicionar")
    public String adicionarUsuario(@RequestParam String matricula, @RequestParam String nome,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Validate matricula is numeric
            if (!matricula.matches("^[0-9]+$")) {
                redirectAttributes.addFlashAttribute("erro", "Matrícula deve conter apenas números.");
                return "redirect:/admin/usuarios";
            }
            
            if (usuarioService.existePorMatricula(matricula)) {
                redirectAttributes.addFlashAttribute("erro", "Matrícula já cadastrada.");
                return "redirect:/admin/usuarios";
            }
            
            Usuario usuario = new Usuario();
            usuario.setMatricula(matricula.trim());
            usuario.setNome(nome.trim());
            usuario.setAtivo(true);
            usuario.setVotou(false);
            usuarioService.salvar(usuario);
            redirectAttributes.addFlashAttribute("sucesso", "Usuário adicionado com sucesso.");
        } catch (Exception e) {
            log.error("Erro ao adicionar usuário: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("erro", "Erro ao adicionar usuário: " + e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/excluir/{id}")
    public String excluirUsuario(@PathVariable Long id) {
        usuarioService.excluir(id);
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/candidatos")
    public String listarCandidatos(Model model) {
        List<Candidato> candidatos = candidatoService.listarTodos();
        model.addAttribute("candidatos", candidatos);
        return "admin/candidatos";
    }

    @PostMapping("/candidatos/adicionar")
    public String adicionarCandidato(
            @RequestParam Integer numero,
            @RequestParam String nome,
            @RequestParam(required = false) MultipartFile foto,
            RedirectAttributes redirectAttributes) {
        
        try {
            if (candidatoService.existeNumero(numero)) {
                redirectAttributes.addFlashAttribute("erro", "Número de candidato já existe.");
                return "redirect:/admin/candidatos";
            }
            
            Candidato candidato = new Candidato();
            candidato.setNumero(numero);
            candidato.setNome(nome.trim());
            candidato.setAtivo(true);
            
            if (foto != null && !foto.isEmpty()) {
                String caminho = uploadService.salvarImagem(foto);
                candidato.setFoto(caminho);
            }
            
            candidatoService.salvar(candidato);
            redirectAttributes.addFlashAttribute("sucesso", "Candidato adicionado com sucesso.");
        } catch (IOException e) {
            log.error("Erro ao salvar foto: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar foto do candidato.");
        } catch (Exception e) {
            log.error("Erro ao adicionar candidato: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("erro", "Erro ao adicionar candidato: " + e.getMessage());
        }
        
        return "redirect:/admin/candidatos";
    }

    @PostMapping("/candidatos/toggle/{id}")
    public String toggleCandidato(@PathVariable Long id) {
        candidatoService.buscarPorId(id)
                .ifPresent(c -> candidatoService.ativarDesativar(id, !c.isAtivo()));
        return "redirect:/admin/candidatos";
    }

    @PostMapping("/candidatos/excluir/{id}")
    public String excluirCandidato(@PathVariable Long id) {
        candidatoService.excluir(id);
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
            LocalDateTime inicio = LocalDateTime.parse(dataInicio, formatter);
            LocalDateTime fim = LocalDateTime.parse(dataFim, formatter);
            
            configuracaoService.configurarEleicao(inicio, fim);
            redirectAttributes.addFlashAttribute("sucesso", "Configuração salva com sucesso.");
        } catch (Exception e) {
            log.error("Erro ao salvar configuração: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar configuração.");
        }
        return "redirect:/admin/configuracao";
    }

    @PostMapping("/eleicao/abrir")
    public String abrirEleicao(RedirectAttributes redirectAttributes) {
        configuracaoService.abrirEleicao();
        redirectAttributes.addFlashAttribute("sucesso", "Eleição aberta com sucesso!");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/eleicao/encerrar")
    public String encerrarEleicao(RedirectAttributes redirectAttributes) {
        configuracaoService.encerrarEleicao();
        try {
            relatorioService.gerarEEnviarRelatorio();
            redirectAttributes.addFlashAttribute("sucesso", "Eleição encerrada e relatório gerado.");
        } catch (Exception e) {
            log.error("Erro ao gerar relatório: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("sucesso", "Eleição encerrada. Erro ao enviar relatório por e-mail (verifique as configurações).");
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/relatorio")
    public String verRelatorio(Model model) {
        model.addAttribute("relatorio", relatorioService.getResumoResultados());
        
        // Build enriched results with candidato names
        List<Map<String, Object>> resultadosEnriquecidos = new ArrayList<>();
        List<Object[]> resultados = votacaoService.getResultados();
        long totalVotos = votacaoService.contarTotalVotos();
        
        for (Object[] resultado : resultados) {
            Map<String, Object> item = new HashMap<>();
            Long candidatoId = (Long) resultado[0];
            Long votos = (Long) resultado[1];
            double percentual = totalVotos > 0 ? (votos * 100.0 / totalVotos) : 0;
            
            String nomeCandidato = candidatoService.buscarPorId(candidatoId)
                    .map(c -> c.getNumero() + " - " + c.getNome())
                    .orElse("Candidato #" + candidatoId);
            
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
    public ResponseEntity<byte[]> downloadRelatorio() {
        byte[] excelBytes = relatorioService.gerarRelatorioExcel();
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio_cipa.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(excelBytes.length)
                .body(excelBytes);
    }

    @PostMapping("/relatorio/enviar")
    public String enviarRelatorio(RedirectAttributes redirectAttributes) {
        try {
            relatorioService.gerarEEnviarRelatorio();
            redirectAttributes.addFlashAttribute("sucesso", "Relatório enviado com sucesso!");
        } catch (Exception e) {
            log.error("Erro ao enviar relatório: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("erro", "Erro ao enviar relatório: " + e.getMessage());
        }
        return "redirect:/admin/relatorio";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/admin/login";
    }

    private String calcularParticipacao() {
        long total = usuarioService.contarTotalAtivos();
        long votos = votacaoService.contarTotalVotos();
        if (total == 0) return "0%";
        return String.format("%.1f%%", (votos * 100.0 / total));
    }
}
