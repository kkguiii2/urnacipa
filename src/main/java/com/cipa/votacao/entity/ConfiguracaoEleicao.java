package com.cipa.votacao.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracao_eleicao")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracaoEleicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    @Column(nullable = false)
    private String status = "FECHADA";

    public boolean isAberta() {
        return "ABERTA".equals(status);
    }

    public boolean isPeriodoVotacao() {
        if (!isAberta()) return false;
        LocalDateTime agora = LocalDateTime.now();
        return (dataInicio == null || !agora.isBefore(dataInicio)) && 
               (dataFim == null || !agora.isAfter(dataFim));
    }
}