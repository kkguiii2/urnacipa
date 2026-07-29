package com.cipa.votacao.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "app.importacao-eleitores")
public class ImportacaoEleitoresProperties {

    @Min(1)
    private long maxFileSizeBytes = 5 * 1024 * 1024;

    @Min(1)
    private int maxDataRows = 5_000;

    @Min(1)
    private long maxExpandedSizeBytes = 50 * 1024 * 1024;

    @Min(1)
    private int maxZipEntries = 200;
}
