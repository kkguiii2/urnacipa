# Sistema de Votação CIPA

## Documentação para apresentação

**Versão do software:** 1.0.0

**Tecnologia principal:** Java 17 e Spring Boot 3.2.0

**Documento levantado em:** 28 de julho de 2026

---

## Resumo executivo

O Sistema de Votação CIPA é uma aplicação web que concentra preparação,
execução e apuração de uma eleição. Ele oferece uma urna simples para o eleitor e
um painel protegido para administração de participantes, período e resultados.

O software é entregue como monólito Spring Boot, com PostgreSQL e páginas
Thymeleaf. Pode ser executado como JAR ou container Docker.

---

## Contexto e problema

Uma eleição exige cadastro consistente, controle de abertura, prevenção de voto
repetido e apuração organizada. O sistema reúne essas etapas:

- cadastro/importação de eleitores;
- cadastro de candidatos;
- controle do período;
- identificação do eleitor;
- registro e contagem dos votos;
- relatório.

O contexto organizacional específico que motivou sua criação:
**Esta informação não pôde ser confirmada no estado atual do projeto.**

---

## Objetivo e público

O objetivo observável é executar uma eleição CIPA pela web.

- **Administrador:** prepara e acompanha a eleição.
- **Eleitor:** identifica-se por matrícula, confirma o nome e vota.
- **Responsável pelo resultado:** obtém relatório e, se configurado, recebe-o por
  e-mail.

---

## Arquitetura

```text
Navegador → Spring Security → Controllers MVC → Services
                                         ↓
                           Repositories JPA → PostgreSQL
```

Páginas HTML são renderizadas no servidor. A matrícula do eleitor fica
temporariamente na sessão; cadastros, estado da eleição e votos ficam no banco.

Essa arquitetura mantém implantação simples para o escopo atual. Os principais
pontos de atenção são migrations ausentes, dependência de disco para fotos e
lacunas de concorrência no voto.

---

## Banco de dados

Cinco entidades representam o sistema:

- `Admin`: credencial e status do administrador;
- `Usuario`: matrícula, nome, status e indicador de voto;
- `Candidato`: número, nome, foto e status;
- `Voto`: candidato, token e momento;
- `ConfiguracaoEleicao`: início, fim e status.

O voto não guarda o eleitor. Também não existe FK entre voto e candidato no
estado atual.

---

## Tecnologias

| Área | Tecnologia |
| --- | --- |
| plataforma | Java 17 |
| aplicação | Spring Boot, MVC, Security, Data JPA |
| interface | Thymeleaf, HTML, CSS e JavaScript embutido |
| banco | PostgreSQL |
| planilhas | Apache POI |
| build | Maven |
| implantação | JAR e Docker |

---

## Funcionalidades

- painel com eleitores, votos e participação;
- cadastro manual e importação Excel de eleitores;
- candidatos com foto e ativação;
- configuração e controle da eleição;
- urna com lista e seleção de candidato;
- bloqueio lógico de nova votação;
- ranking e relatório Excel;
- encerramento automático e envio de e-mail.

O sistema não possui múltiplas eleições, paginação, edição geral, PDF efetivo ou
recuperação de senha.

---

## Segurança

- área administrativa protegida por `ROLE_ADMIN`;
- hashes BCrypt;
- administrador inativo não autentica;
- CSRF nos formulários;
- sessão com timeout de 30 minutos;
- validações estruturais contra planilhas maliciosas.

Antes de produção devem ser tratados: rotação da conta administrativa inicial,
logout GET, validação das fotos, logs que relacionam matrícula e candidato, e
proteção contra voto simultâneo.

---

## Implantação

O Maven gera um JAR executável. O Dockerfile faz build multi-stage e publica a
porta 8080. PostgreSQL e uploads precisam sobreviver à troca da aplicação.

Não há Compose, CI/CD, proxy HTTPS, health check ou backup automatizado no
repositório. Produção exige configuração externa desses elementos.

---

## Benefícios confirmados

- fluxo eleitoral concentrado em uma aplicação;
- interface separada entre urna e administração;
- validações repetidas no serviço de votação;
- importação em lote com diagnóstico por linha;
- apuração automática e exportação Excel;
- stack difundida e empacotamento reproduzível.

Métricas de economia, desempenho ou satisfação:
**Esta informação não pôde ser confirmada no estado atual do projeto.**

---

## Qualidade e manutenção

A suíte contém 46 testes, concentrados na importação de eleitores. A documentação
inclui arquitetura, endpoints, dados, segurança, operação, manutenção,
troubleshooting, ADRs e diagramas renderizados.

Próximas correções técnicas devem priorizar riscos confirmados antes de adicionar
funcionalidades.

---

## Conclusão

O projeto implementa um fluxo completo e compreensível de votação CIPA em um
monólito Spring. A estrutura em camadas e o uso de componentes padrão favorecem
manutenção. A prontidão para produção depende da resolução dos riscos de
segurança, concorrência e gestão de esquema registrados nesta documentação.
