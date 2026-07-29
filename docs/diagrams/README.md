# Diagramas

Cada diagrama possui fonte Mermaid (`.mmd`) e versões renderizadas em SVG e PNG.

| Diagrama | Fonte | SVG | PNG |
| --- | --- | --- | --- |
| arquitetura | [architecture.mmd](architecture.mmd) | [architecture.svg](architecture.svg) | [architecture.png](architecture.png) |
| fluxo de requisição | [request-flow.mmd](request-flow.mmd) | [request-flow.svg](request-flow.svg) | [request-flow.png](request-flow.png) |
| fluxo eleitoral/voto | [ticket-flow.mmd](ticket-flow.mmd) | [ticket-flow.svg](ticket-flow.svg) | [ticket-flow.png](ticket-flow.png) |
| banco de dados | [database-er.mmd](database-er.mmd) | [database-er.svg](database-er.svg) | [database-er.png](database-er.png) |
| autenticação | [authentication-flow.mmd](authentication-flow.mmd) | [authentication-flow.svg](authentication-flow.svg) | [authentication-flow.png](authentication-flow.png) |
| módulos | [modules.mmd](modules.mmd) | [modules.svg](modules.svg) | [modules.png](modules.png) |
| implantação Docker | [docker-deployment.mmd](docker-deployment.mmd) | [docker-deployment.svg](docker-deployment.svg) | [docker-deployment.png](docker-deployment.png) |

O nome `ticket-flow` foi mantido por compatibilidade com a estrutura solicitada.
O conteúdo representa o fluxo de votação existente. Não há módulo de chamados.

## Renderização

```powershell
Get-ChildItem docs/diagrams -Filter *.mmd | ForEach-Object {
  mmdc -i $_.FullName -o ($_.FullName -replace '\.mmd$', '.svg') -b transparent
  mmdc -i $_.FullName -o ($_.FullName -replace '\.mmd$', '.png') -b white -s 2
}
```
