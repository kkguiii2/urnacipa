package com.cipa.votacao.controller;

import com.cipa.votacao.dto.importacao.ImportacaoEleitoresFormDto;
import com.cipa.votacao.dto.importacao.ResultadoImportacaoEleitoresDto;
import com.cipa.votacao.exception.PlanilhaImportacaoException;
import com.cipa.votacao.service.ImportacaoEleitoresService;
import com.cipa.votacao.service.PlanilhaEleitoresService;
import com.cipa.votacao.service.ConfiguracaoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Expõe o fluxo administrativo de importação, o modelo de planilha e o
 * relatório das linhas que não foram importadas.
 */
@Controller
@RequestMapping("/admin/usuarios/importacao")
@RequiredArgsConstructor
@Slf4j
public class ImportacaoEleitoresController {

    public static final String RESULTADO_SESSION_ATTRIBUTE = "ultimaImportacaoEleitores";
    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ImportacaoEleitoresService importacaoEleitoresService;
    private final PlanilhaEleitoresService planilhaEleitoresService;
    private final ConfiguracaoService configuracaoService;

    /**
     * Processa o upload usando Post/Redirect/Get e mantém o resultado detalhado
     * na sessão autenticada para a página e o relatório subsequentes.
     */
    @PostMapping
    public String importar(
            @ModelAttribute ImportacaoEleitoresFormDto form,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        session.removeAttribute(RESULTADO_SESSION_ATTRIBUTE);
        if (configuracaoService.isCadastroBloqueado()) {
            redirectAttributes.addFlashAttribute("erro", "Importação bloqueada durante a eleição.");
            return "redirect:/admin/usuarios";
        }
        if (form.getArquivo() == null || form.getArquivo().isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Selecione um arquivo .xlsx para importar.");
            return "redirect:/admin/usuarios";
        }

        try {
            ResultadoImportacaoEleitoresDto resultado = importacaoEleitoresService.importar(form.getArquivo());
            session.setAttribute(RESULTADO_SESSION_ATTRIBUTE, resultado);
            redirectAttributes.addFlashAttribute("sucesso", mensagemSucesso(resultado.importados()));
        } catch (PlanilhaImportacaoException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Falha inesperada na importação de eleitores: {}", e.getClass().getSimpleName());
            redirectAttributes.addFlashAttribute("erro", "Não foi possível concluir a importação.");
        }
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/modelo")
    public ResponseEntity<byte[]> baixarModelo() {
        return download(
                planilhaEleitoresService.gerarModelo(),
                "modelo-importacao-eleitores.xlsx");
    }

    @GetMapping("/relatorio-erros")
    public Object baixarRelatorioErros(HttpSession session, RedirectAttributes redirectAttributes) {
        Object value = session.getAttribute(RESULTADO_SESSION_ATTRIBUTE);
        if (!(value instanceof ResultadoImportacaoEleitoresDto resultado) || !resultado.possuiFalhas()) {
            redirectAttributes.addFlashAttribute(
                    "erro", "A última importação não possui linhas não importadas.");
            return "redirect:/admin/usuarios";
        }
        return download(
                planilhaEleitoresService.gerarRelatorioErros(resultado),
                "relatorio-erros-importacao-eleitores.xlsx");
    }

    private ResponseEntity<byte[]> download(byte[] content, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(XLSX_MEDIA_TYPE)
                .contentLength(content.length)
                .body(content);
    }

    private String mensagemSucesso(int importados) {
        if (importados == 1) {
            return "Importação concluída: 1 eleitor cadastrado.";
        }
        return "Importação concluída: " + importados + " eleitores cadastrados.";
    }
}
