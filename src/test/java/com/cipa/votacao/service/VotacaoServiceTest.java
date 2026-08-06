package com.cipa.votacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cipa.votacao.entity.Candidato;
import com.cipa.votacao.entity.ConfiguracaoEleicao;
import com.cipa.votacao.entity.Usuario;
import com.cipa.votacao.entity.Voto;
import com.cipa.votacao.repository.VotoRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VotacaoServiceTest {

    @Mock private VotoRepository votoRepository;
    @Mock private CandidatoService candidatoService;
    @Mock private ConfiguracaoService configuracaoService;
    @Mock private UsuarioService usuarioService;
    @Mock private ParticipacaoEleicaoService participacaoService;
    @Mock private CabineVotacaoService cabineService;

    private VotacaoService service;

    @BeforeEach
    void setUp() {
        service = new VotacaoService(
                votoRepository,
                candidatoService,
                configuracaoService,
                usuarioService,
                participacaoService,
                cabineService);

        ConfiguracaoEleicao eleicao = new ConfiguracaoEleicao();
        eleicao.setId(3L);
        eleicao.setStatus("ABERTA");
        eleicao.setDataInicio(LocalDateTime.now().minusMinutes(1));
        eleicao.setDataFim(LocalDateTime.now().plusHours(1));
        when(configuracaoService.getConfiguracao()).thenReturn(eleicao);

        Usuario usuario = new Usuario();
        usuario.setId(8L);
        usuario.setAtivo(true);
        when(usuarioService.buscarPorId(8L)).thenReturn(Optional.of(usuario));

        Candidato candidato = new Candidato();
        candidato.setId(5L);
        candidato.setAtivo(true);
        when(candidatoService.buscarPorId(5L)).thenReturn(Optional.of(candidato));
    }

    @Test
    void registraVotoAnonimoEEncerraSessaoDaCabine() {
        when(participacaoService.marcarComoVotou(3L, 8L)).thenReturn(true);
        when(votoRepository.saveAndFlush(any(Voto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Voto> resultado = service.registrarVoto(5L, 8L, 21L);

        assertThat(resultado).isPresent();
        ArgumentCaptor<Voto> votoCaptor = ArgumentCaptor.forClass(Voto.class);
        verify(votoRepository).saveAndFlush(votoCaptor.capture());
        assertThat(votoCaptor.getValue().getEleicaoId()).isEqualTo(3L);
        assertThat(votoCaptor.getValue().getCandidatoId()).isEqualTo(5L);
        assertThat(votoCaptor.getValue().getToken()).isNotBlank();
        verify(cabineService).validarParaVoto(21L, 8L);
        verify(cabineService).concluir(21L, 8L);
        verify(usuarioService).marcarComoVotou(8L);
    }

    @Test
    void operacaoAtomicaImpedeSegundoVoto() {
        when(participacaoService.marcarComoVotou(3L, 8L)).thenReturn(false);

        Optional<Voto> resultado = service.registrarVoto(5L, 8L, 21L);

        assertThat(resultado).isEmpty();
        verify(votoRepository, never()).saveAndFlush(any(Voto.class));
        verify(cabineService, never()).concluir(any(), any());
        verify(usuarioService, never()).marcarComoVotou(any());
    }
}
