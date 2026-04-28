-- Script SQL para criação do banco de dados CIPA
-- Execute este script no PostgreSQL

-- Criar banco de dados (se não existir)
-- CREATE DATABASE cipa;

-- Criar tabelas
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    matricula VARCHAR(50) UNIQUE NOT NULL,
    nome VARCHAR(255) NOT NULL,
    votou BOOLEAN DEFAULT FALSE NOT NULL,
    ativo BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE TABLE IF NOT EXISTS candidatos (
    id BIGSERIAL PRIMARY KEY,
    numero INTEGER UNIQUE NOT NULL,
    nome VARCHAR(255) NOT NULL,
    foto VARCHAR(500),
    ativo BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE TABLE IF NOT EXISTS votos (
    id BIGSERIAL PRIMARY KEY,
    candidato_id BIGINT NOT NULL,
    token VARCHAR(36) UNIQUE NOT NULL,
    data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS configuracao_eleicao (
    id BIGSERIAL PRIMARY KEY,
    data_inicio TIMESTAMP,
    data_fim TIMESTAMP,
    status VARCHAR(20) DEFAULT 'FECHADA' NOT NULL
);

-- Inserir configuration inicial
INSERT INTO configuracao_eleicao (status) 
SELECT 'FECHADA' WHERE NOT EXISTS (SELECT 1 FROM configuracao_eleicao);

-- Exemplos de usuários para teste
-- INSERT INTO usuarios (matricula, nome) VALUES 
-- ('0001', 'João Silva'),
-- ('0002', 'Maria Santos'),
-- ('0003', 'Pedro Oliveira');

-- Exemplos de candidatos para teste
-- INSERT INTO candidatos (numero, nome, ativo) VALUES 
-- (10, 'Candidato A', TRUE),
-- (20, 'Candidato B', TRUE);

-- Criar índice para performance
CREATE INDEX IF NOT EXISTS idx_usuarios_matricula ON usuarios(matricula);
CREATE INDEX IF NOT EXISTS idx_candidatos_numero ON candidatos(numero);
CREATE INDEX IF NOT EXISTS idx_votos_candidato ON votos(candidato_id);
CREATE INDEX IF NOT EXISTS idx_votos_token ON votos(token);