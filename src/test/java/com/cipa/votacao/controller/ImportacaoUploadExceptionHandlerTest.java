package com.cipa.votacao.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;

class ImportacaoUploadExceptionHandlerTest {

    @Test
    void converteExcessoMultipartEmRedirectComMensagemObjetiva() {
        ImportacaoUploadExceptionHandler handler = new ImportacaoUploadExceptionHandler();
        RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();

        String view = handler.tratarArquivoMuitoGrande(
                new MaxUploadSizeExceededException(5 * 1024 * 1024),
                attributes);

        assertThat(view).isEqualTo("redirect:/admin/usuarios");
        assertThat(attributes.getFlashAttributes().get("erro"))
                .isEqualTo("O arquivo enviado ultrapassa o limite permitido de 5 MB.");
    }
}
