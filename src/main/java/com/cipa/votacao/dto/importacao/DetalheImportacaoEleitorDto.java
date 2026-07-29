package com.cipa.votacao.dto.importacao;

import java.io.Serializable;

public record DetalheImportacaoEleitorDto(
        int linha,
        String matricula,
        String nome,
        StatusImportacaoEleitor status,
        String motivo) implements Serializable {
}
