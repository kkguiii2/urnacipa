package com.cipa.votacao.dto.importacao;

import java.util.List;

public record PlanilhaEleitoresDto(
        String nomeArquivo,
        List<LinhaImportacaoEleitorDto> linhas) {
}
