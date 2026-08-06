package com.cipa.votacao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cabine_votacao")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CabineVotacao {

    @Id
    private Long id;

    @Column(name = "sessao_atual_id")
    private Long sessaoAtualId;

    @Version
    private long versao;
}
