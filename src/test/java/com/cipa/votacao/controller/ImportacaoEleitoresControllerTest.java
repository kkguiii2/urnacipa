package com.cipa.votacao.controller;

import com.cipa.votacao.config.SecurityConfig;
import com.cipa.votacao.config.CustomAuthenticationProvider;
import com.cipa.votacao.config.MesarioAuthenticationProvider;
import com.cipa.votacao.config.CabineAuthenticationProvider;
import com.cipa.votacao.dto.importacao.DetalheImportacaoEleitorDto;
import com.cipa.votacao.dto.importacao.ResultadoImportacaoEleitoresDto;
import com.cipa.votacao.dto.importacao.StatusImportacaoEleitor;
import com.cipa.votacao.exception.PlanilhaImportacaoException;
import com.cipa.votacao.service.AdminService;
import com.cipa.votacao.service.ImportacaoEleitoresService;
import com.cipa.votacao.service.PlanilhaEleitoresService;
import com.cipa.votacao.service.ConfiguracaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImportacaoEleitoresController.class)
@ContextConfiguration(classes = {ImportacaoEleitoresController.class, SecurityConfig.class})
class ImportacaoEleitoresControllerTest {

    private static final String XLSX_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImportacaoEleitoresService importacaoEleitoresService;

    @MockBean
    private PlanilhaEleitoresService planilhaEleitoresService;

    @MockBean
    private AdminService adminService;

    @MockBean
    private CustomAuthenticationProvider customAuthenticationProvider;

    @MockBean
    private MesarioAuthenticationProvider mesarioAuthenticationProvider;

    @MockBean
    private CabineAuthenticationProvider cabineAuthenticationProvider;

    @MockBean
    private ConfiguracaoService configuracaoService;

    @Test
    void exigeAutenticacaoParaImportar() throws Exception {
        mockMvc.perform(upload(arquivoValido()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(importacaoEleitoresService, never()).importar(any());
    }

    @Test
    void uploadsSaoLiberadosExplicitamenteSemAutenticacao() throws Exception {
        mockMvc.perform(get("/uploads/foto-inexistente.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void rejeitaUsuarioSemPerfilAdministrador() throws Exception {
        mockMvc.perform(upload(arquivoValido()).with(csrf()))
                .andExpect(status().isForbidden());

        verify(importacaoEleitoresService, never()).importar(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejeitaImportacaoSemCsrf() throws Exception {
        mockMvc.perform(upload(arquivoValido()))
                .andExpect(status().isForbidden());

        verify(importacaoEleitoresService, never()).importar(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void importaComPrgEArmazenaUltimoResultadoNaSessao() throws Exception {
        ResultadoImportacaoEleitoresDto resultado = resultadoComFalha();
        when(importacaoEleitoresService.importar(any())).thenReturn(resultado);

        mockMvc.perform(upload(arquivoValido()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute("sucesso", "Importação concluída: 1 eleitor cadastrado."))
                .andExpect(request().sessionAttribute("ultimaImportacaoEleitores", resultado));

        verify(importacaoEleitoresService).importar(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void apresentaMensagemEstruturalConhecidaSemDetalhesInternos() throws Exception {
        when(importacaoEleitoresService.importar(any()))
                .thenThrow(new PlanilhaImportacaoException("A coluna 'matricula' não foi encontrada."));

        mockMvc.perform(upload(arquivoValido()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute("erro", "A coluna 'matricula' não foi encontrada."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void removeResultadoAnteriorQuandoNovaTentativaFalha() throws Exception {
        ResultadoImportacaoEleitoresDto anterior = resultadoComFalha();
        when(importacaoEleitoresService.importar(any()))
                .thenThrow(new PlanilhaImportacaoException("O arquivo está vazio."));

        mockMvc.perform(upload(arquivoValido())
                        .sessionAttr("ultimaImportacaoEleitores", anterior)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(request().sessionAttributeDoesNotExist("ultimaImportacaoEleitores"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void apresentaMensagemGenericaParaFalhaInesperada() throws Exception {
        when(importacaoEleitoresService.importar(any())).thenThrow(new IllegalStateException("segredo interno"));

        mockMvc.perform(upload(arquivoValido()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute("erro", "Não foi possível concluir a importação."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejeitaUploadSemArquivoAntesDoService() throws Exception {
        MockMultipartFile vazio = new MockMultipartFile("arquivo", "", XLSX_TYPE, new byte[0]);

        mockMvc.perform(upload(vazio).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute("erro", "Selecione um arquivo .xlsx para importar."));

        verify(importacaoEleitoresService, never()).importar(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void baixaPlanilhaModelo() throws Exception {
        byte[] bytes = {0x50, 0x4B, 0x03, 0x04};
        when(planilhaEleitoresService.gerarModelo()).thenReturn(bytes);

        mockMvc.perform(get("/admin/usuarios/importacao/modelo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"modelo-importacao-eleitores.xlsx\""))
                .andExpect(content().contentType(XLSX_TYPE))
                .andExpect(content().bytes(bytes));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void baixaRelatorioDeErrosDaUltimaImportacao() throws Exception {
        ResultadoImportacaoEleitoresDto resultado = resultadoComFalha();
        byte[] bytes = {0x50, 0x4B, 0x03, 0x04};
        when(planilhaEleitoresService.gerarRelatorioErros(resultado)).thenReturn(bytes);

        mockMvc.perform(get("/admin/usuarios/importacao/relatorio-erros")
                        .sessionAttr("ultimaImportacaoEleitores", resultado))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"relatorio-erros-importacao-eleitores.xlsx\""))
                .andExpect(content().contentType(XLSX_TYPE))
                .andExpect(content().bytes(bytes));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void naoGeraRelatorioQuandoUltimaImportacaoNaoPossuiFalhas() throws Exception {
        ResultadoImportacaoEleitoresDto resultado = ResultadoImportacaoEleitoresDto.criar(
                "eleitores.xlsx",
                10,
                List.of(new DetalheImportacaoEleitorDto(
                        2, "12345", "Joao", StatusImportacaoEleitor.IMPORTADO, "Usuário cadastrado com sucesso.")));

        mockMvc.perform(get("/admin/usuarios/importacao/relatorio-erros")
                        .sessionAttr("ultimaImportacaoEleitores", resultado))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute("erro", "A última importação não possui linhas não importadas."));

        verify(planilhaEleitoresService, never()).gerarRelatorioErros(any());
    }

    private static MockMultipartFile arquivoValido() {
        return new MockMultipartFile("arquivo", "eleitores.xlsx", XLSX_TYPE, new byte[]{0x50, 0x4B, 0x03, 0x04});
    }

    private static org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder upload(
            MockMultipartFile file) {
        return MockMvcRequestBuilders.multipart("/admin/usuarios/importacao").file(file);
    }

    private static ResultadoImportacaoEleitoresDto resultadoComFalha() {
        return ResultadoImportacaoEleitoresDto.criar(
                "eleitores.xlsx",
                15,
                List.of(
                        new DetalheImportacaoEleitorDto(
                                2, "12345", "Joao", StatusImportacaoEleitor.IMPORTADO, "Usuário cadastrado com sucesso."),
                        new DetalheImportacaoEleitorDto(
                                3, "", "Maria", StatusImportacaoEleitor.INVALIDO, "Matrícula não informada.")));
    }
}
