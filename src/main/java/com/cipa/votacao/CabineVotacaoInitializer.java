package com.cipa.votacao;

import com.cipa.votacao.entity.CabineVotacao;
import com.cipa.votacao.repository.CabineVotacaoRepository;
import com.cipa.votacao.service.CabineVotacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CabineVotacaoInitializer implements CommandLineRunner {

    private final CabineVotacaoRepository repository;

    @Override
    public void run(String... args) {
        if (repository.existsById(CabineVotacaoService.CABINE_UNICA_ID)) {
            return;
        }
        CabineVotacao cabine = new CabineVotacao();
        cabine.setId(CabineVotacaoService.CABINE_UNICA_ID);
        try {
            repository.saveAndFlush(cabine);
        } catch (DataIntegrityViolationException ignored) {
            // Outra instância pode ter inicializado a cabine simultaneamente.
        }
    }
}
