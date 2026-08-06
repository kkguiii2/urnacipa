package com.cipa.votacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cipa.votacao.entity.CabineStatus;
import com.cipa.votacao.entity.CabineVotacao;
import com.cipa.votacao.entity.ConfiguracaoEleicao;
import com.cipa.votacao.entity.SessaoCabine;
import com.cipa.votacao.entity.Usuario;
import com.cipa.votacao.exception.CabineVotacaoException;
import com.cipa.votacao.repository.CabineVotacaoRepository;
import com.cipa.votacao.repository.SessaoCabineRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CabineVotacaoServiceTest {

    @Mock private CabineVotacaoRepository cabineRepository;
    @Mock private SessaoCabineRepository sessaoRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private ConfiguracaoService configuracaoService;
    @Mock private ParticipacaoEleicaoService participacaoService;

    private CabineVotacaoService service;
    private CabineVotacao cabine;
    private Usuario eleitor;

    @BeforeEach
    void setUp() {
        service = new CabineVotacaoService(
                cabineRepository,
                sessaoRepository,
                usuarioService,
                configuracaoService,
                participacaoService);
        ReflectionTestUtils.setField(service, "liberacaoSegundos", 180L);
        ReflectionTestUtils.setField(service, "votacaoSegundos", 600L);
        ReflectionTestUtils.setField(service, "maxTentativas", 3);

        ConfiguracaoEleicao eleicao = new ConfiguracaoEleicao();
        eleicao.setId(7L);
        eleicao.setStatus("ABERTA");
        eleicao.setDataInicio(LocalDateTime.now().minusMinutes(1));
        eleicao.setDataFim(LocalDateTime.now().plusHours(1));
        when(configuracaoService.getConfiguracao()).thenReturn(eleicao);

        cabine = new CabineVotacao(1L, null, 0L);
        when(cabineRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(cabine));

        eleitor = new Usuario();
        eleitor.setId(11L);
        eleitor.setMatricula("12345");
        eleitor.setNome("Eleitor Teste");
        eleitor.setAtivo(true);
        when(usuarioService.buscarPorMatricula("12345")).thenReturn(Optional.of(eleitor));
        when(participacaoService.jaVotou(7L, 11L)).thenReturn(false);
        when(sessaoRepository.saveAndFlush(any(SessaoCabine.class))).thenAnswer(invocation -> {
            SessaoCabine sessao = invocation.getArgument(0);
            sessao.setId(99L);
            return sessao;
        });
    }

    @Test
    void liberaSomenteEleitorValidadoPeloMesario() {
        SessaoCabine sessao = service.liberar("12345", "mesario1");

        assertThat(sessao.getUsuarioId()).isEqualTo(11L);
        assertThat(sessao.getEleicaoId()).isEqualTo(7L);
        assertThat(sessao.getMesarioUsername()).isEqualTo("mesario1");
        assertThat(sessao.getStatus()).isEqualTo(CabineStatus.LIBERADA);
        assertThat(cabine.getSessaoAtualId()).isEqualTo(99L);
        verify(participacaoService).garantirCadastro(7L, 11L);
    }

    @Test
    void bloqueiaLiberacaoDepoisDeTresMatriculasIncorretas() {
        SessaoCabine sessao = service.liberar("12345", "mesario1");
        when(sessaoRepository.findById(99L)).thenReturn(Optional.of(sessao));
        when(usuarioService.buscarPorMatricula("99999")).thenReturn(Optional.empty());

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.identificar("99999"))
                    .isInstanceOf(CabineVotacaoException.class)
                    .hasMessage("Matrícula não corresponde à liberação atual.");
        }

        assertThat(sessao.getTentativas()).isEqualTo(3);
        assertThat(sessao.getStatus()).isEqualTo(CabineStatus.BLOQUEADA);
    }

    @Test
    void recusaLiberarSegundoEleitorEnquantoCabineEstaOcupada() {
        SessaoCabine sessao = service.liberar("12345", "mesario1");
        when(sessaoRepository.findById(99L)).thenReturn(Optional.of(sessao));

        assertThatThrownBy(() -> service.liberar("12345", "mesario1"))
                .isInstanceOf(CabineVotacaoException.class)
                .hasMessage("A cabine já possui uma liberação ativa.");
    }
}
