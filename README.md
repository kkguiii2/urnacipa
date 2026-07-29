# Sistema de Votação CIPA

Aplicação web para administrar e executar uma eleição da Comissão Interna de
Prevenção de Acidentes (CIPA). O sistema mantém eleitores e candidatos, controla
o período da eleição, registra votos, acompanha a participação e gera relatório
Excel dos resultados.

> A documentação descreve exclusivamente o estado atual do código. Lacunas
> históricas ou operacionais não demonstráveis estão identificadas de forma
> explícita.

## Funcionalidades confirmadas

- autenticação administrativa com Spring Security e senha BCrypt;
- cadastro e exclusão de eleitores;
- importação de eleitores por planilha `.xlsx`, com relatório de linhas não
  importadas;
- cadastro, ativação, desativação e exclusão de candidatos, com foto opcional;
- configuração, abertura e encerramento manual da eleição;
- encerramento automático por agendamento a cada 60 segundos;
- identificação do eleitor por matrícula e bloqueio lógico de novo voto;
- votação em candidato ativo durante o período configurado;
- dashboard de participação e ranking;
- geração e download de relatório Excel;
- envio opcional do relatório por e-mail.

## Tecnologias

- Java 17;
- Spring Boot 3.2.0;
- Spring MVC, Thymeleaf, Spring Security, Spring Data JPA e Bean Validation;
- PostgreSQL;
- Apache POI 5.2.5;
- Maven;
- Docker com build multi-stage.

## Arquitetura resumida

O projeto é um monólito Spring Boot com interface renderizada no servidor.
Controllers MVC recebem as requisições, Services concentram a orquestração e as
regras, Repositories Spring Data acessam o PostgreSQL e templates Thymeleaf
produzem HTML. O estado transitório do eleitor e da última importação fica na
sessão HTTP.

Detalhes: [arquitetura](docs/architecture.md) e
[diagramas](docs/diagrams/README.md).

## Requisitos

- JDK 17;
- Maven 3.9 ou compatível;
- PostgreSQL acessível;
- Docker apenas para a execução conteinerizada;
- servidor SMTP somente se o envio de relatórios for utilizado.

## Instalação rápida

1. Crie ou disponibilize um banco PostgreSQL.
1. Defina `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e, na primeira inicialização,
   `ADMIN_PASSWORD`.
1. Compile e teste:

```powershell
mvn clean verify
```

1. Inicie:

```powershell
mvn spring-boot:run
```

1. Acesse `http://localhost:8080`.

Se `DB_URL` não estiver definida, a aplicação tenta criar o banco indicado na
URL JDBC padrão conectando-se ao banco `postgres`. A conta informada precisa ter
permissão para isso.

## Execução do JAR

```powershell
mvn clean package
java -jar target/sistema-votacao-cipa-1.0.0.jar
```

Alterações em Java exigem nova compilação. Alterações em templates, CSS ou
JavaScript embutido também precisam ser empacotadas novamente para aparecerem no
JAR e na imagem Docker.

## Docker

```powershell
docker build -t sistema-votacao-cipa:1.0.0 .
docker run --rm -p 8080:8080 `
  -e DB_URL="jdbc:postgresql://host.docker.internal:5432/cipa" `
  -e DB_USERNAME="<usuario>" `
  -e DB_PASSWORD="<senha>" `
  sistema-votacao-cipa:1.0.0
```

O repositório não contém `docker-compose.yml`. O diretório `/app/uploads`
existe na imagem, mas o Dockerfile não declara volume; monte armazenamento
persistente em produção.

## Variáveis de ambiente

| Variável | Finalidade | Padrão confirmado |
| --- | --- | --- |
| `DB_URL` | URL JDBC do PostgreSQL | `jdbc:postgresql://localhost:5432/cipa` |
| `DB_USERNAME` | usuário do banco | `postgres` |
| `DB_PASSWORD` | senha do banco | vazio |
| `EMAIL_HOST` | servidor SMTP | `smtp.gmail.com` |
| `EMAIL_PORT` | porta SMTP | `587` |
| `EMAIL_USERNAME` | conta SMTP | vazio |
| `EMAIL_PASSWORD` | segredo SMTP | vazio |
| `EMAIL_DESTINATARIO` | destinatário do relatório | vazio |
| `ADMIN_USERNAME` | usuário administrativo inicial | `admin` |
| `ADMIN_PASSWORD` | senha administrativa inicial quando não há admins | vazio |
| `IMPORTACAO_MAX_FILE_SIZE` | limite multipart do arquivo | `5MB` |
| `IMPORTACAO_MAX_REQUEST_SIZE` | limite da requisição | `6MB` |
| `IMPORTACAO_MAX_FILE_SIZE_BYTES` | limite validado pela aplicação | `5242880` |
| `IMPORTACAO_MAX_DATA_ROWS` | linhas de dados | `5000` |
| `IMPORTACAO_MAX_EXPANDED_SIZE_BYTES` | conteúdo descompactado | `52428800` |
| `IMPORTACAO_MAX_ZIP_ENTRIES` | entradas internas do ZIP | `200` |

Não publique valores reais de senha em documentação, logs ou controle de
versão. Consulte [segurança](docs/security.md).

## Estrutura

```text
src/main/java/com/cipa/votacao/
├── config/       configuração MVC, segurança, banco e importação
├── controller/   páginas e endpoints HTTP
├── dto/          objetos da importação de eleitores
├── entity/       entidades JPA
├── exception/    erro de planilha
├── repository/   persistência Spring Data
├── scheduler/    encerramento automático
└── service/      regras e orquestração
src/main/resources/
├── db/schema.sql
├── static/css/
├── templates/admin/
└── templates/urna/
```

## Documentação

- [Índice da documentação](docs/README.md)
- [Visão geral](docs/overview.md)
- [Arquitetura](docs/architecture.md)
- [Banco de dados](docs/database.md)
- [Endpoints HTTP](docs/api.md)
- [Regras de negócio](docs/business-rules.md)
- [Segurança](docs/security.md)
- [Implantação](docs/deployment.md)
- [Manutenção](docs/maintenance.md)
- [Solução de problemas](docs/troubleshooting.md)
- [Testes](docs/testing.md)
- [Changelog verificável](docs/changelog.md)
- [Documento de apresentação](docs/presentation.md)
- [ADRs](docs/adr/README.md)
