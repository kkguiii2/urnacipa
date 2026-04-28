-- ============================================================
-- Schema migration: garante que todas as colunas existem
-- Roda automaticamente na inicialização do Spring Boot
-- ============================================================

-- Tabela usuarios: adiciona colunas que podem estar faltando
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS votou BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
