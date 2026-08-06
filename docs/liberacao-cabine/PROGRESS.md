# Liberação de cabine — progresso

## Status

Concluído e validado com `mvn clean verify`.

## Decisões

- sessão server-side em vez de JWT;
- papéis separados para administrador, mesário e cabine;
- cabine única representada por registro bloqueável no PostgreSQL;
- matrícula liberada nunca é persistida no voto;
- credenciais iniciais do mesário vêm de variáveis de ambiente.

## Fases

- [x] Domínio e transações
- [x] Autenticação e telas
- [x] Hardening
- [x] Testes e documentação
