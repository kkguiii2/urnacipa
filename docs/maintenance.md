# Manutenção e evolução

## Princípio

Preserve o fluxo observado:

```text
Template/Form → Controller → Service → Repository → Entity/PostgreSQL
```

Regras que mudam estado ficam no Service e operações múltiplas devem declarar a
fronteira transacional. Controllers devem continuar responsáveis por HTTP e
modelos de tela, não por persistência direta.

## Criar uma Entity

1. adicione a classe em `entity/` com `@Entity` e `@Table`;
2. declare PK e estratégia de geração;
3. torne explícitos nome, nullability, unicidade e tamanho das colunas;
4. modele relacionamento apenas se ele realmente existir no banco;
5. adicione validações Jakarta coerentes com o esquema;
6. defina como o esquema será atualizado antes de implantar;
7. teste persistência, constraints e valores padrão.

Não use `ddl-auto=update` como substituto de migration em uma base crítica.

## Criar um DTO

O padrão mais claro está em `dto/importacao/`: records para valores imutáveis e
classe com getters/setters para binding multipart. Não exponha entidade como
contrato quando a entrada ou saída tiver regras próprias.

Checklist:

- nome representa a operação;
- campos mínimos;
- validações declarativas quando aplicáveis;
- sem segredo em `toString`;
- serializável somente se for guardado em sessão.

## Criar um Repository

1. estenda `JpaRepository<Entidade, TipoId>`;
2. prefira derived queries simples;
3. use `@Query` quando agregação/ordenação não for expressiva pelo nome;
4. teste queries que sustentam regras;
5. avalie índice para colunas usadas em busca/agrupamento;
6. não duplique consultas dentro de loops quando uma busca em lote resolver.

## Criar um Service

1. use `@Service` e injeção por construtor;
2. mantenha validações de negócio nele;
3. use `@Transactional` em mudanças atômicas;
4. documente propagação especial, como `REQUIRES_NEW`;
5. converta falhas técnicas em resultados seguros quando a UI não deve receber
   detalhes internos;
6. escreva testes unitários para sucesso, limites e concorrência.

## Criar um Controller ou endpoint

1. escolha `@Controller` para HTML/redirect/download e `@RestController` somente
   para contrato JSON;
2. defina método HTTP coerente; mudança de estado deve usar POST/PUT/PATCH/DELETE;
3. use DTO/binding explícito;
4. valide autenticação, autorização e CSRF;
5. mantenha o padrão Post/Redirect/Get para formulários;
6. trate erros previsíveis com mensagem segura;
7. documente a rota em [api.md](api.md);
8. adicione teste MockMvc para acesso anônimo, perfil incorreto, CSRF, sucesso e
   erro.

## Criar um Template

1. coloque em `templates/admin` ou `templates/urna`;
2. use `th:href`/`th:action`, não URLs absolutas desnecessárias;
3. associe `label` e `input`;
4. forneça status/alerta acessível;
5. evite `innerHTML` com dados;
6. garanta token CSRF nos formulários de mudança;
7. teste renderização e comportamento JavaScript.

## Criar uma migration

O projeto não possui mecanismo de migrations. Adicionar um SQL avulso a
`schema.sql` não cria histórico de versão.

Para iniciar migrations de forma segura, é necessária uma decisão arquitetural:

1. escolher Flyway ou Liquibase;
2. levantar o esquema real de todos os ambientes;
3. criar baseline sem reaplicar objetos existentes;
4. definir política para substituir `ddl-auto=update`;
5. versionar mudanças incrementais e reversão/restore;
6. validar em cópia do banco.

Até essa decisão, **não existe procedimento de migration confirmado no projeto**.

## Criar ou alterar relatório

O padrão atual está em `RelatorioService` e `PlanilhaEleitoresService`:

- dados obtidos por services/repositories;
- workbook Apache POI em try-with-resources;
- estilos e larguras definidos;
- resultado retornado como `byte[]`;
- controller define media type, tamanho e `Content-Disposition`;
- testes reabrem o arquivo e verificam células.

Ao alterar, teste conjunto vazio, candidato ausente, empate, percentuais, arquivo
válido e memória para grandes volumes.

## Checklist de desenvolvimento

- [ ] requisito e não-objetivos registrados;
- [ ] arquivos e camadas afetados identificados;
- [ ] regra no Service, HTTP no Controller;
- [ ] validações e mensagens seguras;
- [ ] transação e concorrência analisadas;
- [ ] schema/migration e índices avaliados;
- [ ] autorização e CSRF testados;
- [ ] unidade, MVC e regressão executados;
- [ ] documentação e diagramas atualizados;
- [ ] `mvn clean verify` aprovado;
- [ ] build Docker validado onde Docker estiver disponível;
- [ ] backup/rollback planejados para mudanças de dados.

## Dívida técnica visível

- migrations ausentes e esquema divergente;
- testes concentrados na importação;
- controller administrativo extenso;
- logout GET incompatível com o fluxo de segurança;
- bootstrap administrativo dependente de segredo externo;
- upload de foto sem validação de conteúdo;
- ausência de FK de voto e controle de concorrência por eleitor;
- dependências iText e Commons CSV sem uso confirmado.
