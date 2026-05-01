package com.cipa.votacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class SistemaVotacaoCipaApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Manaus"));
    }

    public static void main(String[] args) {
        SpringApplication.run(SistemaVotacaoCipaApplication.class, args);
    }
}