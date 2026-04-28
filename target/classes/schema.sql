-- ============================================================
-- Schema reset: remove tabelas legadas e deixa o Hibernate
-- recriar tudo limpo a partir das entidades Java.
--
-- Este script roda ANTES do Hibernate (defer=false).
-- O Hibernate (ddl-auto=update) cria as tabelas se não existirem.
-- ============================================================

DROP TABLE IF EXISTS votos CASCADE;
DROP TABLE IF EXISTS usuarios CASCADE;
DROP TABLE IF EXISTS candidatos CASCADE;
DROP TABLE IF EXISTS configuracao_eleicao CASCADE;
