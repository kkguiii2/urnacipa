package com.cipa.votacao.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(assignableTypes = ImportacaoEleitoresController.class)
public class ImportacaoUploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tratarArquivoMuitoGrande(
            MaxUploadSizeExceededException exception,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(
                "erro", "O arquivo enviado ultrapassa o limite permitido de 5 MB.");
        return "redirect:/admin/usuarios";
    }
}
