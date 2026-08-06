# Liberação de cabine — plano de implementação

## Fase 1 — domínio e transações

- criar conta de mesário, sessão da cabine e participação por eleição;
- criar operações com lock para liberar, identificar, cancelar, expirar e concluir;
- tornar a marcação de voto única uma atualização condicional atômica.

## Fase 2 — autenticação e telas

- adicionar cadeia Spring Security e login próprios do mesário;
- adicionar painel de liberação/status;
- integrar a urna ao estado da cabine e à matrícula liberada.

## Fase 3 — hardening

- remover associação eleitor–candidato de logs e sucesso;
- validar/regravar uploads e liberar `/uploads/**` explicitamente;
- remover `innerHTML` com dados cadastrados;
- corrigir logout, sessões e bloqueio de alterações durante eleição aberta;
- impedir resultados antes do encerramento.

## Fase 4 — validação

- testes de autenticação, autorização, transições da cabine e voto único;
- build completo e atualização da documentação operacional.

