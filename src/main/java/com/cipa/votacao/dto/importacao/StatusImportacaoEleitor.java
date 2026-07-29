package com.cipa.votacao.dto.importacao;

public enum StatusImportacaoEleitor {
    IMPORTADO("Importado", "badge-success"),
    DUPLICADO_NA_PLANILHA("Duplicado na planilha", "badge-warning"),
    JA_CADASTRADO("Já cadastrado", "badge-warning"),
    INVALIDO("Inválido", "badge-danger"),
    ERRO("Erro", "badge-danger");

    private final String descricao;
    private final String cssClass;

    StatusImportacaoEleitor(String descricao, String cssClass) {
        this.descricao = descricao;
        this.cssClass = cssClass;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getCssClass() {
        return cssClass;
    }
}
