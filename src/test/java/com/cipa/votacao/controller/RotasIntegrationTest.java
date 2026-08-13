package com.cipa.votacao.controller;

import com.cipa.votacao.config.CabineAuthenticationProvider;
import com.cipa.votacao.config.CustomAuthenticationProvider;
import com.cipa.votacao.config.MesarioAuthenticationProvider;
import com.cipa.votacao.config.SecurityConfig;
import com.cipa.votacao.config.WebConfig;
import com.cipa.votacao.entity.ConfiguracaoEleicao;
import com.cipa.votacao.service.AdminService;
import com.cipa.votacao.service.CabineVotacaoService;
import com.cipa.votacao.service.CandidatoService;
import com.cipa.votacao.service.ConfiguracaoService;
import com.cipa.votacao.service.ImportacaoEleitoresService;
import com.cipa.votacao.service.MesarioService;
import com.cipa.votacao.service.PlanilhaEleitoresService;
import com.cipa.votacao.service.RelatorioService;
import com.cipa.votacao.service.UploadService;
import com.cipa.votacao.service.UsuarioService;
import com.cipa.votacao.service.VotacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AdminController.class,
        MesarioController.class,
        CabineController.class,
        AuthController.class,
        VotacaoController.class,
        ImportacaoEleitoresController.class,
        CustomErrorController.class
})
@Import({SecurityConfig.class, WebConfig.class})
class RotasIntegrationTest {

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
    private CabineVotacaoService cabineVotacaoService;

    @MockBean
    private UploadService uploadService;

    @MockBean
    private ImportacaoEleitoresService importacaoEleitoresService;

    @MockBean
    private PlanilhaEleitoresService planilhaEleitoresService;

    @MockBean
    private AdminService adminService;

    @MockBean
    private MesarioService mesarioService;

    @MockBean
    private CustomAuthenticationProvider customAuthenticationProvider;

    @MockBean
    private MesarioAuthenticationProvider mesarioAuthenticationProvider;

    @MockBean
    private CabineAuthenticationProvider cabineAuthenticationProvider;

    @BeforeEach
    void setupMocks() {
        ConfiguracaoEleicao config = new ConfiguracaoEleicao();
        config.setStatus("FECHADA");
        when(configuracaoService.getConfiguracao()).thenReturn(config);
        when(usuarioService.contarTotalAtivos()).thenReturn(10L);
        when(votacaoService.contarTotalVotos()).thenReturn(5L);
        when(usuarioService.listarTodos()).thenReturn(Collections.emptyList());
        when(candidatoService.listarTodos()).thenReturn(Collections.emptyList());
        when(configuracaoService.isEleicaoAberta()).thenReturn(false);
        when(configuracaoService.isPeriodoVotacao()).thenReturn(true);
        when(cabineVotacaoService.disponivelParaIdentificacao()).thenReturn(true);
        when(cabineVotacaoService.obterEstado()).thenReturn(CabineVotacaoService.EstadoCabine.aguardando());
    }

    @Test
    @DisplayName("Raiz deve redirecionar para /auth/login sem exigir autenticação")
    void deveRedirecionarRaizParaAuthLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    @DisplayName("Páginas de login administrativas e de mesário devem ser públicas")
    void deveAcessarPaginasDeLoginPublicas() throws Exception {
        mockMvc.perform(get("/admin/login")).andExpect(status().isOk());
        mockMvc.perform(get("/mesario/login")).andExpect(status().isOk());
        mockMvc.perform(get("/cabine/login")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Administrador deve acessar todas as telas do painel administrativo")
    @WithMockUser(roles = "ADMIN")
    void devePermitirAdminAcessarAreaAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/usuarios")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/candidatos")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/configuracao")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/relatorio")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Administrador deve ter permissão para acessar áreas do mesário e da urna/cabine")
    @WithMockUser(roles = "ADMIN")
    void devePermitirAdminAcessarAreaMesarioECabine() throws Exception {
        mockMvc.perform(get("/mesario/cabine")).andExpect(status().isOk());
        mockMvc.perform(get("/auth/login")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Mesário autenticado deve acessar o controle da cabine")
    @WithMockUser(roles = "MESARIO")
    void devePermitirMesarioAcessarAreaMesario() throws Exception {
        mockMvc.perform(get("/mesario/cabine")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Mesário não deve acessar rotas do painel administrativo (403 Forbidden)")
    @WithMockUser(roles = "MESARIO")
    void deveRejeitarMesarioEmAreaAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Perfil CABINE deve acessar rotas da urna e autenticação de eleitor")
    @WithMockUser(roles = "CABINE")
    void devePermitirCabineAcessarUrna() throws Exception {
        mockMvc.perform(get("/auth/login")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Requisicões POST sem token CSRF devem ser bloqueadas (403 Forbidden)")
    @WithMockUser(roles = "ADMIN")
    void deveRejeitarPostSemCsrf() throws Exception {
        mockMvc.perform(post("/admin/usuarios/adicionar")
                        .param("matricula", "123")
                        .param("nome", "Teste"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Requisicões POST com token CSRF válido devem ser aceitas e redirecionadas")
    @WithMockUser(roles = "ADMIN")
    void deveAceitarPostComCsrf() throws Exception {
        mockMvc.perform(post("/admin/usuarios/adicionar")
                        .param("matricula", "123")
                        .param("nome", "Teste")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"));
    }
}
