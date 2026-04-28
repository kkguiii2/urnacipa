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
        ConfiguracaoEleicao config = getConfiguracao();
        config.setDataInicio(dataInicio);
        config.setDataFim(dataFim);
        return configuracaoRepository.save(config);
    }

    @Transactional
    public ConfiguracaoEleicao abrirEleicao() {
        ConfiguracaoEleicao config = getConfiguracao();
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
}