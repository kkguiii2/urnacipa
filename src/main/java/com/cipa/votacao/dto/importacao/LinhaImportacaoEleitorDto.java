package com.cipa.votacao.dto.importacao;

public record LinhaImportacaoEleitorDto(
        int linha,
        String matricula,
        String nome,
        String erroLeitura) {
}
