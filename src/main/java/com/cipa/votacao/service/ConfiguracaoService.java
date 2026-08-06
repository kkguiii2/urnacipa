package com.cipa.votacao.service;

import com.cipa.votacao.entity.ConfiguracaoEleicao;
import com.cipa.votacao.repository.ConfiguracaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConfiguracaoService {

    private final ConfiguracaoRepository configuracaoRepository;
    private final UsuarioService usuarioService;

    public ConfiguracaoEleicao getConfiguracao() {
        Optional<ConfiguracaoEleicao> config = configuracaoRepository.findTopByOrderByIdDesc();
        return config.orElseGet(() -> {
            ConfiguracaoEleicao nova = new ConfiguracaoEleicao();
            nova.setStatus("FECHADA");
            return configuracaoRepository.save(nova);
        });
    }

    @Transactional
    public ConfiguracaoEleicao configurarEleicao(LocalDateTime dataInicio, LocalDateTime dataFim) {
        if (dataInicio == null || dataFim == null || !dataFim.isAfter(dataInicio)) {
            throw new IllegalArgumentException("A data final deve ser posterior à data inicial.");
        }
        ConfiguracaoEleicao config = getConfiguracao();
        if (config.isAberta()) {
            throw new IllegalStateException("Não é possível alterar datas durante a eleição.");
        }
        config.setDataInicio(dataInicio);
        config.setDataFim(dataFim);
        return configuracaoRepository.save(config);
    }

    @Transactional
    public ConfiguracaoEleicao abrirEleicao() {
        ConfiguracaoEleicao config = getConfiguracao();
        if (config.getDataInicio() == null || config.getDataFim() == null
                || !config.getDataFim().isAfter(config.getDataInicio())) {
            throw new IllegalStateException("Configure um período válido antes de abrir a eleição.");
        }
        config.setStatus("ABERTA");
        return configuracaoRepository.save(config);
    }

    @Transactional
    public ConfiguracaoEleicao encerrarEleicao() {
        ConfiguracaoEleicao config = getConfiguracao();
        config.setStatus("FECHADA");
        return configuracaoRepository.save(config);
    }

    public boolean isEleicaoAberta() {
        return getConfiguracao().isAberta();
    }

    public boolean isPeriodoVotacao() {
        return getConfiguracao().isPeriodoVotacao();
    }

    public boolean isEleicaoEncerrada() {
        ConfiguracaoEleicao config = getConfiguracao();
        return "FECHADA".equals(config.getStatus()) || 
               (config.getDataFim() != null && LocalDateTime.now().isAfter(config.getDataFim()));
    }

    public boolean isCadastroBloqueado() {
        return getConfiguracao().isAberta();
    }

    @Transactional
    public ConfiguracaoEleicao criarNovaEleicao() {
        ConfiguracaoEleicao atual = getConfiguracao();
        if (atual.isAberta()) {
            throw new IllegalStateException("Encerre a eleição atual antes de criar outra.");
        }
        ConfiguracaoEleicao nova = new ConfiguracaoEleicao();
        nova.setStatus("FECHADA");
        ConfiguracaoEleicao salva = configuracaoRepository.save(nova);
        usuarioService.resetarIndicadorVoto();
        return salva;
    }
}
