# Testes

## Estratégia existente

A suíte usa JUnit 5, AssertJ, Mockito, Spring MVC Test e Spring Security Test.
O build atual executa 58 testes sem falhas em 11 classes.

| Classe | Testes | Escopo |
| --- | ---: | --- |
| `ImportacaoEleitoresServiceTest` | 30 | leitura, validação, limites, duplicidade, concorrência de matrícula e persistência parcial |
| `ImportacaoEleitoresControllerTest` | 12 | autenticação, role, CSRF, uploads públicos, PRG e downloads |
| `PlanilhaEleitoresServiceTest` | 2 | modelo e relatório de erros XLSX |
| `CustomAuthenticationProviderTest` | 1 | rejeição de admin inativo |
| `AdminUsuariosTemplateTest` | 1 | renderização do formulário e resultado |
| `ImportacaoUploadExceptionHandlerTest` | 1 | limite multipart |
| `CabineVotacaoServiceTest` | 3 | liberação única, matrícula incorreta e bloqueio |
| `VotacaoServiceTest` | 2 | voto anônimo e bloqueio atômico de duplicidade |
| `UploadServiceTest` | 2 | imagem válida regravada e arquivo falso rejeitado |
| `MesarioAuthenticationProviderTest` | 2 | papel exclusivo e conta inativa |
| `CabineAuthenticationProviderTest` | 2 | credencial correta e senha inválida |

## Execução

```powershell
mvn test
```

Validação completa do build:

```powershell
mvn clean verify
```

Teste isolado:

```powershell
mvn -Dtest=ImportacaoEleitoresServiceTest test
```

Relatórios ficam em `target/surefire-reports/`.

## Cobertura

Não há JaCoCo nem limiar de cobertura configurado; percentual de cobertura:
**Esta informação não pôde ser confirmada no estado atual do projeto.**

Cobertura forte:

- importação `.xlsx`;
- segurança do endpoint de importação;
- resultado de importação na sessão;
- geração de arquivos da importação;
- administrador inativo.

Lacunas restantes:

- autenticação completa e logout;
- configuração e scheduler;
- cadastros administrativos completos;
- geração do relatório principal/e-mail;
- banco real PostgreSQL;
- Docker;
- JavaScript/modal/urna;
- fluxos end-to-end.

## Checklist de regressão manual

- [ ] login de admin ativo funciona e inativo falha;
- [ ] logout realmente invalida a sessão;
- [ ] cadastro manual e importação de eleitores;
- [ ] arquivo inválido não persiste dados;
- [ ] candidato com/sem foto aparece na urna;
- [ ] eleição fechada bloqueia login e voto;
- [ ] limites de início/fim funcionam no fuso de Manaus;
- [ ] eleitor ativo vota uma vez;
- [ ] candidato inativo não recebe voto;
- [ ] duas requisições simultâneas da mesma matrícula não duplicam voto;
- [ ] encerramento manual e automático;
- [ ] dashboard, ranking, download e e-mail;
- [ ] recriação do container preserva uploads;
- [ ] páginas responsivas e navegação por teclado;
- [ ] CSRF rejeita POST sem token.

## Dados e ambiente de teste

Os testes existentes usam mocks e workbooks em memória; não exigem PostgreSQL.
Não há Testcontainers, fixtures SQL dedicadas ou perfil `test`.
