package com.cipa.votacao.dto.importacao;

import java.io.Serializable;
import java.util.List;

public record ResultadoImportacaoEleitoresDto(
        String nomeArquivo,
        int totalLinhas,
        int importados,
        int duplicadosNaPlanilha,
        int jaCadastrados,
        int rejeitados,
        long duracaoMillis,
        List<DetalheImportacaoEleitorDto> detalhes) implements Serializable {

    public ResultadoImportacaoEleitoresDto {
        detalhes = List.copyOf(detalhes);
    }

    public static ResultadoImportacaoEleitoresDto criar(
            String nomeArquivo,
            long duracaoMillis,
            List<DetalheImportacaoEleitorDto> detalhes) {
        int importados = contar(detalhes, StatusImportacaoEleitor.IMPORTADO);
        int duplicados = contar(detalhes, StatusImportacaoEleitor.DUPLICADO_NA_PLANILHA);
        int existentes = contar(detalhes, StatusImportacaoEleitor.JA_CADASTRADO);
        int rejeitados = contar(detalhes, StatusImportacaoEleitor.INVALIDO)
                + contar(detalhes, StatusImportacaoEleitor.ERRO);
        return new ResultadoImportacaoEleitoresDto(
                nomeArquivo,
                detalhes.size(),
                importados,
                duplicados,
                existentes,
                rejeitados,
                duracaoMillis,
                detalhes);
    }

    public int ignoradosPorDuplicidade() {
        return duplicadosNaPlanilha + jaCadastrados;
    }

    public boolean possuiFalhas() {
        return detalhes.stream().anyMatch(detalhe -> detalhe.status() != StatusImportacaoEleitor.IMPORTADO);
    }

    private static int contar(List<DetalheImportacaoEleitorDto> detalhes, StatusImportacaoEleitor status) {
        return (int) detalhes.stream().filter(detalhe -> detalhe.status() == status).count();
    }
}
