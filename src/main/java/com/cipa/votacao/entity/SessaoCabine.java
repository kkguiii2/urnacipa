package com.cipa.votacao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "sessoes_cabine",
        indexes = {
                @Index(name = "idx_sessao_cabine_eleicao", columnList = "eleicao_id"),
                @Index(name = "idx_sessao_cabine_usuario", columnList = "usuario_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessaoCabine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "eleicao_id", nullable = false)
    private Long eleicaoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "mesario_username", nullable = false, length = 100)
    private String mesarioUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CabineStatus status;

    @Column(name = "liberada_em", nullable = false)
    private LocalDateTime liberadaEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "identificada_em")
    private LocalDateTime identificadaEm;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @Column(nullable = false)
    private int tentativas;
}
