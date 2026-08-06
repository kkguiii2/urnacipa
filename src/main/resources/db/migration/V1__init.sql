-- ============================================================
-- V1 — Estrutura inicial do banco de dados CIPA
-- Criado automaticamente pelo Flyway na primeira inicialização
-- ============================================================

-- Usuários (eleitores)
CREATE TABLE IF NOT EXISTS usuarios (
    id        BIGSERIAL    PRIMARY KEY,
    matricula VARCHAR(50)  UNIQUE NOT NULL,
    nome      VARCHAR(255) NOT NULL,
    votou     BOOLEAN      DEFAULT FALSE NOT NULL,
    ativo     BOOLEAN      DEFAULT TRUE  NOT NULL
);

-- Candidatos
CREATE TABLE IF NOT EXISTS candidatos (
    id     BIGSERIAL    PRIMARY KEY,
    numero INTEGER      UNIQUE NOT NULL,
    nome   VARCHAR(255) NOT NULL,
    foto   VARCHAR(500),
    ativo  BOOLEAN      DEFAULT TRUE NOT NULL
);

-- Configuração da eleição
CREATE TABLE IF NOT EXISTS configuracao_eleicao (
    id          BIGSERIAL  PRIMARY KEY,
    data_inicio TIMESTAMP,
    data_fim    TIMESTAMP,
    status      VARCHAR(20) DEFAULT 'FECHADA' NOT NULL
);

-- Votos
CREATE TABLE IF NOT EXISTS votos (
    id           BIGSERIAL   PRIMARY KEY,
    candidato_id BIGINT      NOT NULL,
    eleicao_id   BIGINT,
    token        VARCHAR(36) UNIQUE NOT NULL,
    data_hora    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Administradores
CREATE TABLE IF NOT EXISTS admins (
    id       BIGSERIAL    PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    ativo    BOOLEAN      DEFAULT TRUE NOT NULL
);

-- Mesários
CREATE TABLE IF NOT EXISTS mesarios (
    id       BIGSERIAL    PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    ativo    BOOLEAN      DEFAULT TRUE NOT NULL
);

-- Cabine de votação (singleton)
CREATE TABLE IF NOT EXISTS cabine_votacao (
    id              BIGINT PRIMARY KEY,
    sessao_atual_id BIGINT,
    versao          BIGINT DEFAULT 0 NOT NULL
);

-- Sessões de cabine
CREATE TABLE IF NOT EXISTS sessoes_cabine (
    id               BIGSERIAL   PRIMARY KEY,
    eleicao_id       BIGINT      NOT NULL,
    usuario_id       BIGINT      NOT NULL,
    mesario_username VARCHAR(100) NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    liberada_em      TIMESTAMP   NOT NULL,
    expira_em        TIMESTAMP   NOT NULL,
    identificada_em  TIMESTAMP,
    concluida_em     TIMESTAMP,
    tentativas       INTEGER     DEFAULT 0 NOT NULL
);

-- Participações por eleição
CREATE TABLE IF NOT EXISTS participacoes_eleicao (
    id         BIGSERIAL PRIMARY KEY,
    eleicao_id BIGINT    NOT NULL,
    usuario_id BIGINT    NOT NULL,
    votou_em   TIMESTAMP,
    CONSTRAINT uk_participacao_eleicao_usuario UNIQUE (eleicao_id, usuario_id)
);

-- ============================================================
-- Índices de performance
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_usuarios_matricula    ON usuarios(matricula);
CREATE INDEX IF NOT EXISTS idx_candidatos_numero     ON candidatos(numero);
CREATE INDEX IF NOT EXISTS idx_votos_candidato       ON votos(candidato_id);
CREATE INDEX IF NOT EXISTS idx_votos_token           ON votos(token);
CREATE INDEX IF NOT EXISTS idx_votos_eleicao         ON votos(eleicao_id);
CREATE INDEX IF NOT EXISTS idx_sessao_cabine_eleicao ON sessoes_cabine(eleicao_id);
CREATE INDEX IF NOT EXISTS idx_sessao_cabine_usuario ON sessoes_cabine(usuario_id);

-- ============================================================
-- Chaves estrangeiras
-- ============================================================
ALTER TABLE votos
    ADD CONSTRAINT fk_voto_candidato
        FOREIGN KEY (candidato_id) REFERENCES candidatos(id) ON DELETE RESTRICT;

ALTER TABLE votos
    ADD CONSTRAINT fk_voto_eleicao
        FOREIGN KEY (eleicao_id) REFERENCES configuracao_eleicao(id) ON DELETE RESTRICT;

ALTER TABLE sessoes_cabine
    ADD CONSTRAINT fk_sessao_eleicao
        FOREIGN KEY (eleicao_id) REFERENCES configuracao_eleicao(id) ON DELETE RESTRICT;

ALTER TABLE sessoes_cabine
    ADD CONSTRAINT fk_sessao_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT;

ALTER TABLE participacoes_eleicao
    ADD CONSTRAINT fk_participacao_eleicao
        FOREIGN KEY (eleicao_id) REFERENCES configuracao_eleicao(id) ON DELETE RESTRICT;

ALTER TABLE participacoes_eleicao
    ADD CONSTRAINT fk_participacao_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT;

-- ============================================================
-- Dado inicial: configuração da eleição (apenas se não existir)
-- ============================================================
INSERT INTO configuracao_eleicao (status)
SELECT 'FECHADA'
WHERE NOT EXISTS (SELECT 1 FROM configuracao_eleicao);
