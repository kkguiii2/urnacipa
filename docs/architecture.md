# Arquitetura

## Classificação

O sistema é um **monólito em camadas**, executado como um único processo Spring
Boot e empacotado como JAR. A interface usa MVC server-side com Thymeleaf; não há
frontend separado nem conjunto de microserviços.

```text
Navegador
   ↓ HTTP + sessão + CSRF
Spring Security
   ↓
Controllers MVC
   ↓
Services / componentes
   ↓
Repositories Spring Data JPA
   ↓
PostgreSQL
```

## Camadas

### Apresentação

`controller/` contém controllers MVC e um `ControllerAdvice`. Eles recebem
parâmetros, preparam `Model`, usam flash attributes e retornam nomes de templates,
redirecionamentos ou downloads binários.

`templates/` contém 6 páginas administrativas e 5 páginas da urna. O JavaScript é
embutido em quatro templates: prévia de foto, atualização do dashboard, controle
da importação e comportamento da urna/modal. Não há arquivos `.js`.

### Aplicação e negócio

`service/` concentra validação e orquestração. Os principais fluxos são:

- `VotacaoService`: autorização lógica e registro transacional do voto;
- `ConfiguracaoService`: configuração e status da eleição;
- `ImportacaoEleitoresService`: classificação e persistência parcial das linhas;
- `LeitorPlanilhaEleitores`: validação estrutural e leitura segura do `.xlsx`;
- `RelatorioService`: apuração e geração do Excel;
- serviços CRUD para admins, usuários e candidatos.

DTOs existem apenas no módulo de importação. Os demais formulários usam
`@RequestParam` e entidades.

### Persistência

As entidades JPA são `Admin`, `Candidato`, `ConfiguracaoEleicao`, `Usuario` e
`Voto`. Cinco interfaces `JpaRepository` implementam o acesso. O Hibernate está
configurado com `ddl-auto=update`.

### Infraestrutura

- `SecurityConfig` define a cadeia de filtros e o formulário administrativo;
- `CustomAuthenticationProvider` autentica admin ativo e atribui `ROLE_ADMIN`;
- `DatabaseInitializer` tenta criar o banco local antes do uso do JPA;
- `WebConfig` registra redirecionamento raiz e recursos estáticos/uploads;
- `EleicaoScheduler` executa a verificação de encerramento;
- `Dockerfile` compila e executa o mesmo monólito em duas etapas.

## Como uma requisição percorre o sistema

Exemplo de voto:

1. `SecurityFilterChain` permite a rota pública, mantendo CSRF ativo no `POST`.
2. `VotacaoController.registrarVoto` exige `matricula` na sessão.
3. O controller verifica o período e a existência do candidato.
4. `VotacaoService.registrarVoto` repete as validações críticas.
5. O serviço salva `Voto` e marca `Usuario.votou=true` na mesma transação.
6. O controller invalida a sessão e renderiza `urna/sucesso`.

O serviço repete verificações do controller para que a regra não dependa apenas
da interface.

## Estado e transações

- sessão HTTP: matrícula/nome do eleitor e último resultado de importação;
- banco: estado durável de eleição, cadastros e votos;
- `VotacaoService.registrarVoto`: `@Transactional`;
- inserções da importação: uma transação `REQUIRES_NEW` por eleitor;
- operações de configuração e toggle de candidato: transacionais.

## Avaliação da arquitetura existente

### Adequações

- as camadas têm responsabilidades reconhecíveis;
- um monólito é coerente com o escopo compacto demonstrado;
- validações críticas de voto são centralizadas no service;
- repositories evitam SQL espalhado;
- a importação separa leitura, classificação, persistência e geração de arquivos;
- testes cobrem com profundidade o módulo de importação.

### Acoplamentos e riscos

- `AdminController` é amplo e mistura cadastros, eleição e relatório;
- entidades são usadas diretamente na apresentação fora da importação;
- `Voto.candidatoId` é escalar, sem relacionamento/FK;
- `ddl-auto=update` e `schema.sql` coexistem sem migrations versionadas;
- fotos dependem do disco local;
- não há lock pessimista/otimista nem restrição única por eleitor no registro de
  voto; duas requisições concorrentes para a mesma matrícula podem passar pela
  leitura de `votou=false`;
- a exclusão de candidatos pode deixar votos cujo candidato não existe mais;
- o Dockerfile não executa testes durante o build.

Esta avaliação não altera a arquitetura; registra consequências do código atual.

## Perfis Spring

Não existem perfis específicos nem arquivos `application-*.properties/yml`.
Todos os ambientes usam `application.properties` com substituição por variáveis.

## Diagramas e decisões

- [Arquitetura renderizada](diagrams/architecture.svg)
- [Fluxo de requisição](diagrams/request-flow.svg)
- [Dependências entre módulos](diagrams/modules.svg)
- [ADRs retrospectivos](adr/README.md)
