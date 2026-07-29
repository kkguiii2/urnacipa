package com.cipa.votacao.scheduler;

import com.cipa.votacao.service.ConfiguracaoService;
import com.cipa.votacao.service.RelatorioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Verifica periodicamente se a eleição aberta ultrapassou a data final e, nesse
 * caso, encerra-a antes de gerar e enviar o relatório.
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class EleicaoScheduler {

    private final ConfiguracaoService configuracaoService;
    private final RelatorioService relatorioService;

    /**
     * Executa a verificação a cada 60 segundos, conforme o {@code fixedRate}.
     */
    @Scheduled(fixedRate = 60000)
    public void verificarEncerramentoEleicao() {
        log.info("Verificando encerramento da eleição...");
        
        var config = configuracaoService.getConfiguracao();
        
        if (config.isAberta() && config.getDataFim() != null) {
            if (LocalDateTime.now().isAfter(config.getDataFim())) {
                log.info("Eleição encerrada automaticamente!");
                configuracaoService.encerrarEleicao();
                relatorioService.gerarEEnviarRelatorio();
                log.info("Relatório enviado automaticamente.");
            }
        }
    }
}
