# Banco de dados

## Tecnologia e criação

O datasource é PostgreSQL. Em ambiente sem `DB_URL`, `DatabaseInitializer`
extrai o nome da URL JDBC, conecta ao banco `postgres`, consulta `pg_database` e
tenta criar o banco ausente. Quando `DB_URL` está definida, essa criação é
pulada.

As tabelas são mantidas pelo Hibernate com
`spring.jpa.hibernate.ddl-auto=update`. Existe também
`src/main/resources/db/schema.sql`, mas não há
`spring.sql.init.mode=always`; sua execução automática no PostgreSQL não está
confirmada.

Não há Flyway, Liquibase ou diretório de migrations.

## Modelo

### `admins`

| Coluna | Modelo JPA | Restrições |
| --- | --- | --- |
| `id` | `Long` | PK identity |
| `username` | `String` | única, não nula |
| `password` | `String` | não nula; hash BCrypt no fluxo de criação |
| `ativo` | `boolean` | não nula |

A tabela não aparece no `schema.sql`; é esperada via Hibernate.

### `usuarios`

| Coluna | Modelo JPA | Restrições |
| --- | --- | --- |
| `id` | `Long` | PK identity |
| `matricula` | `String` | única, não nula; validação 1–20 dígitos |
| `nome` | `String` | não nula |
| `votou` | `boolean` | não nula, padrão `false` |
| `ativo` | `boolean` | não nula, padrão `true` |

### `candidatos`

| Coluna | Modelo JPA | Restrições |
| --- | --- | --- |
| `id` | `Long` | PK identity |
| `numero` | `Integer` | único, não nulo |
| `nome` | `String` | não nulo |
| `foto` | `String` | até 500 no JPA |
| `ativo` | `boolean` | não nulo, padrão `true` |

### `votos`

| Coluna | Modelo JPA | Restrições |
| --- | --- | --- |
| `id` | `Long` | PK identity |
| `candidato_id` | `Long` | não nulo, sem FK declarada |
| `token` | `String` | único, não nulo |
| `data_hora` | `LocalDateTime` | não nula; preenchida em `@PrePersist` |

### `configuracao_eleicao`

| Coluna | Modelo JPA | Restrições |
| --- | --- | --- |
| `id` | `Long` | PK identity |
| `data_inicio` | `LocalDateTime` | opcional |
| `data_fim` | `LocalDateTime` | opcional |
| `status` | `String` | não nulo; valores usados: `ABERTA`, `FECHADA` |

O serviço sempre recupera a linha de maior `id`. Se não houver, cria uma linha
`FECHADA`.

## Relacionamentos e cardinalidade

O domínio sugere muitos votos para um candidato, mas o código persiste apenas o
valor numérico `candidato_id`. Não existe `@ManyToOne` nem `FOREIGN KEY`.
Também não existe relacionamento persistido entre voto e usuário.

Logo, a cardinalidade abaixo é lógica, não imposta pelo banco:

- `Candidato 1 → 0..N Voto`;
- `Usuario` não possui relacionamento com `Voto`;
- `ConfiguracaoEleicao` não possui relacionamento com votos ou candidatos.

## Índices confirmados no SQL

- `idx_usuarios_matricula`;
- `idx_candidatos_numero`;
- `idx_votos_candidato`;
- `idx_votos_token`.

`matricula`, `numero` e `token` também têm restrições únicas. Os nomes e índices
gerados pelo Hibernate em uma instalação criada somente por JPA podem diferir.

## Divergências relevantes

| Tema | Entidade/código | `schema.sql` |
| --- | --- | --- |
| matrícula | valida até 20; `@Column` sem length explícito | `VARCHAR(50)` |
| admins | entidade presente | tabela ausente |
| votos→candidato | campo escalar | sem FK |
| status | `String` livre | `VARCHAR(20)` |

Antes de padronizar o esquema, inspecione a base real. O estado efetivo de uma
base já implantada: **Esta informação não pôde ser confirmada no estado atual do
projeto.**

## Backup e restauração

Os comandos compatíveis com o PostgreSQL estão em
[deployment.md](deployment.md#backup-e-restauração). Não há script de backup
incluído no repositório.

## Diagrama

Consulte [database-er.svg](diagrams/database-er.svg) ou a
[fonte Mermaid](diagrams/database-er.mmd).
