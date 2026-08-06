package com.cipa.votacao;

import com.cipa.votacao.entity.ConfiguracaoEleicao;
import com.cipa.votacao.repository.ParticipacaoEleicaoRepository;
import com.cipa.votacao.repository.UsuarioRepository;
import com.cipa.votacao.repository.VotoRepository;
import com.cipa.votacao.service.ConfiguracaoService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LegacyElectionMigration implements CommandLineRunner {

    private final ConfiguracaoService configuracaoService;
    private final VotoRepository votoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ParticipacaoEleicaoRepository participacaoRepository;

    @Override
    @Transactional
    public void run(String... args) {
        ConfiguracaoEleicao atual = configuracaoService.getConfiguracao();
        votoRepository.associarVotosLegados(atual.getId());
        usuarioRepository.findByVotouTrue().forEach(usuario -> {
            participacaoRepository.inserirSeAusente(atual.getId(), usuario.getId());
            participacaoRepository.marcarComoVotouSeDisponivel(
                    atual.getId(), usuario.getId(), LocalDateTime.now());
        });
    }
}
