package com.cipa.votacao.controller;

import com.cipa.votacao.dto.importacao.DetalheImportacaoEleitorDto;
import com.cipa.votacao.dto.importacao.ResultadoImportacaoEleitoresDto;
import com.cipa.votacao.dto.importacao.StatusImportacaoEleitor;
import com.cipa.votacao.service.CandidatoService;
import com.cipa.votacao.service.ConfiguracaoService;
import com.cipa.votacao.service.RelatorioService;
import com.cipa.votacao.service.UploadService;
import com.cipa.votacao.service.UsuarioService;
import com.cipa.votacao.service.VotacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminUsuariosTemplateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private CandidatoService candidatoService;

    @MockBean
    private ConfiguracaoService configuracaoService;

    @MockBean
    private VotacaoService votacaoService;

    @MockBean
    private RelatorioService relatorioService;

    @MockBean
    private UploadService uploadService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void renderizaFormularioInstrucoesEResumoDaUltimaImportacao() throws Exception {
        when(usuarioService.listarTodos()).thenReturn(List.of());
        ResultadoImportacaoEleitoresDto resultado = ResultadoImportacaoEleitoresDto.criar(
                "eleitores.xlsx",
                20,
                List.of(new DetalheImportacaoEleitorDto(
                        2, "", "Maria", StatusImportacaoEleitor.INVALIDO, "Matrícula não informada.")));

        String html = mockMvc.perform(get("/admin/usuarios")
                        .sessionAttr(ImportacaoEleitoresController.RESULTADO_SESSION_ATTRIBUTE, resultado))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains("Importar eleitores por Excel");
        assertThat(html).contains("enctype=\"multipart/form-data\"");
        assertThat(html).contains("accept=\".xlsx\"");
        assertThat(html).contains("id=\"arquivoImportacao\"");
        assertThat(html).contains("id=\"botaoImportar\"");
        assertThat(html).contains("disabled");
        assertThat(html).contains("modelo-importacao-eleitores.xlsx");
        assertThat(html).contains("matricula", "nome", "matrícula deve ser única");
        assertThat(html).contains("Resultado da última importação", "eleitores.xlsx", "Inválido");
        assertThat(html).contains("relatorio-erros-importacao-eleitores.xlsx");
        assertThat(count(html, "id=\"arquivoImportacao\"")).isEqualTo(1);
        assertThat(count(html, "id=\"botaoImportar\"")).isEqualTo(1);
    }

    private static int count(String value, String token) {
        return (value.length() - value.replace(token, "").length()) / token.length();
    }
}
