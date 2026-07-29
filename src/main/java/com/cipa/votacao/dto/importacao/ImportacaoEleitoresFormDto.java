package com.cipa.votacao.dto.importacao;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ImportacaoEleitoresFormDto {

    private MultipartFile arquivo;
}
