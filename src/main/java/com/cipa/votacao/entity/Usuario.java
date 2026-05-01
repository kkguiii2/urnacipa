package com.cipa.votacao.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @Pattern(regexp = "^[0-9]+$", message = "Matrícula deve conter apenas números")
    @Size(min = 1, max = 20, message = "Matrícula deve ter entre 1 e 20 caracteres")
    private String matricula;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private boolean votou = false;

    @Column(nullable = false)
    private boolean ativo = true;
}