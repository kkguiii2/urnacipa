# Testes

## Estratégia existente

A suíte usa JUnit 5, AssertJ, Mockito, Spring MVC Test e Spring Security Test.
Foram encontradas 46 rotinas `@Test` em 6 classes.

| Classe | Testes | Escopo |
| --- | ---: | --- |
| `ImportacaoEleitoresServiceTest` | 30 | leitura, validação, limites, duplicidade, concorrência de matrícula e persistência parcial |
| `ImportacaoEleitoresControllerTest` | 11 | autenticação, role, CSRF, PRG, downloads e mensagens |
| `PlanilhaEleitoresServiceTest` | 2 | modelo e relatório de erros XLSX |
| `CustomAuthenticationProviderTest` | 1 | rejeição de admin inativo |
| `AdminUsuariosTemplateTest` | 1 | renderização do formulário e resultado |
| `ImportacaoUploadExceptionHandlerTest` | 1 | limite multipart |

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

Lacunas:

- registro de voto e concorrência;
- autenticação completa e logout;
- configuração e scheduler;
- cadastros e upload de foto;
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
