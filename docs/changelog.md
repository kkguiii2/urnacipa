# Changelog verificável

Este arquivo reconstrói somente o que pôde ser observado no Git e no working
tree em 2026-07-28. As mensagens históricas são genéricas (`commit 1` a
`commit 7`), portanto não permitem atribuir intenção além dos diffs.

## Não lançado — estado atual do working tree

### Importação de eleitores

- importação administrativa de `.xlsx`;
- validação de pacote OOXML, assinatura ZIP, macro, fórmula e limites;
- identificação de duplicados internos e já cadastrados;
- persistência independente por linha e tratamento de SQLSTATE `23505`;
- planilha-modelo e relatório de erros;
- resultado mantido em sessão e interface responsiva;
- configuração de limites por variáveis;
- testes de service, MVC, segurança, template e geração de planilha.

### Segurança

- administradores inativos passaram a ser rejeitados pelo provider;
- teste específico para o comportamento.
- senhas fixas foram removidas da configuração e do bootstrap administrativo;
- primeira conta administrativa passou a depender de `ADMIN_PASSWORD`.

### Documentação

- README principal ampliado;
- documentação técnica, operacional, diagramas e ADRs adicionados nesta entrega.

## 2026-05-10

- ajustes em `.gitignore` nos commits `84204b7` e `c01dc4c`.

## 2026-05-01 — `2f21833`

- introdução das entidades/repository/service e configuração de autenticação
  administrativa;
- inclusão de Spring Security e testes no `pom.xml`;
- alterações de login administrativo, configuração e inicialização;
- remoção do README existente daquele commit.

As justificativas e a lista de correções pretendidas pelo autor:
**Esta informação não pôde ser confirmada no estado atual do projeto.**

## 2026-04-30 — `b36dca7`

- ajuste pontual no template de indisponibilidade.

## 2026-04-28 — `39138b9`, `b7faf0e`, `5356240`, `983c5fa`

- ajustes sucessivos em inicialização do banco, repository de usuários,
  propriedades e script SQL;
- os commits não descrevem o defeito corrigido.

## 2026-04-28 — `e311919`

- baseline do sistema: aplicação Spring Boot, camadas MVC/JPA, templates, CSS,
  SQL, Dockerfile e documentação inicial.

## Limitação histórica

Não existem tags/releases no histórico apresentado nem changelogs anteriores.
Uma lista completa de “todas as correções” fora dos diffs disponíveis:
**Esta informação não pôde ser confirmada no estado atual do projeto.**
