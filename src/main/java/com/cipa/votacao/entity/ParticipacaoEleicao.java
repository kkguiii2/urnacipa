package com.cipa.votacao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "participacoes_eleicao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_participacao_eleicao_usuario",
                columnNames = {"eleicao_id", "usuario_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipacaoEleicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "eleicao_id", nullable = false)
    private Long eleicaoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "votou_em")
    private LocalDateTime votouEm;
}
