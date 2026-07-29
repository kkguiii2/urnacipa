# Documentação técnica

Esta documentação foi levantada diretamente do código-fonte do Sistema de
Votação CIPA. Ela não constitui uma especificação de funcionalidades futuras.

## Mapa

| Documento | Conteúdo |
| --- | --- |
| [Visão geral](overview.md) | objetivo, escopo, atores, funcionalidades e limites |
| [Arquitetura](architecture.md) | camadas, componentes, requisições e avaliação |
| [Banco de dados](database.md) | entidades, tabelas, chaves, índices e divergências |
| [Endpoints HTTP](api.md) | rotas MVC, parâmetros, respostas, segurança e erros |
| [Regras de negócio](business-rules.md) | eleição, voto, cadastros, importação e relatórios |
| [Segurança](security.md) | autenticação, autorização, CSRF, sessões e riscos |
| [Implantação](deployment.md) | local, JAR, Docker, servidor, backup e rede |
| [Manutenção](maintenance.md) | como evoluir cada camada e checklist |
| [Troubleshooting](troubleshooting.md) | sintomas, causas e ações verificáveis |
| [Testes](testing.md) | suíte existente, execução, cobertura e regressão |
| [Changelog](changelog.md) | histórico que pôde ser confirmado no Git |
| [Apresentação](presentation.md) | versão preparada para impressão/apresentação |
| [Diagramas](diagrams/README.md) | fontes Mermaid e imagens renderizadas |
| [ADRs](adr/README.md) | decisões arquiteturais observadas |

## Convenções de confiabilidade

- **Confirmado**: demonstrável por arquivo, anotação, configuração ou teste.
- **Inferência**: conclusão técnica derivada de mais de uma evidência, indicada
  como tal.
- **Não confirmado**: usa a frase exigida no briefing:
  “Esta informação não pôde ser confirmada no estado atual do projeto.”

## Inventário

| Grupo | Itens confirmados |
| --- | ---: |
| aplicação/initializers | 3 |
| configurações Java | 6 |
| controllers/advice | 5 |
| entidades JPA | 5 |
| DTOs/enums de importação | 6 |
| repositories | 5 |
| services/componentes de negócio | 12 |
| scheduler | 1 |
| templates Thymeleaf | 11 |
| folhas CSS | 1 |
| arquivos JavaScript externos | 0 |
| classes de teste | 6 |
| scripts SQL | 1 |
| Dockerfiles | 1 |

Não foram encontrados `docker-compose.yml`, arquivos `application-*.yml` ou
`application-*.properties`, migrations Flyway/Liquibase, scripts operacionais,
especificação OpenAPI ou perfis Spring próprios.
