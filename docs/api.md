# Endpoints HTTP

O sistema usa MVC/Thymeleaf. Todos os formulários `POST` exigem CSRF.

## Mesário

| Método | URL | Função | Acesso |
| --- | --- | --- | --- |
| `GET/POST` | `/mesario/login` | login independente | público no chain do mesário |
| `GET` | `/mesario/cabine` | estado e liberação da cabine | `ROLE_MESARIO` |
| `POST` | `/mesario/cabine/liberar` | libera a matrícula conferida | `ROLE_MESARIO` |
| `POST` | `/mesario/cabine/cancelar` | cancela a sessão ativa | `ROLE_MESARIO` |
| `POST` | `/mesario/logout` | encerra a sessão | `ROLE_MESARIO` |

## Dispositivo e votação

| Método | URL | Função | Acesso |
| --- | --- | --- | --- |
| `GET/POST` | `/cabine/login` | ativa o dispositivo | público no chain da cabine |
| `POST` | `/cabine/logout` | desativa o dispositivo | `ROLE_CABINE` |
| `GET` | `/auth/login` | matrícula ou tela de espera | `ROLE_CABINE` |
| `POST` | `/auth/verificar` | consome a liberação da matrícula exata | `ROLE_CABINE` |
| `GET` | `/votacao/tela` | urna e candidatos ativos | `ROLE_CABINE` + sessão liberada |
| `GET` | `/votacao/candidato/{numero}` | candidato ativo em JSON | `ROLE_CABINE` + sessão liberada |
| `POST` | `/votacao/votar` | registra voto e conclui a sessão | `ROLE_CABINE` + sessão liberada |
| `GET` | `/votacao/sucesso` | confirma somente que houve registro | `ROLE_CABINE` |

## Administração

`/admin/login` é público no chain administrativo. Os demais `/admin/**`
exigem `ROLE_ADMIN`: dashboard, usuários, importação XLSX, candidatos,
configuração, abrir/encerrar/nova eleição e relatórios. Mutações usam
`POST`; logout é `POST /admin/logout`. Relatório, download e envio são recusados
enquanto a eleição estiver aberta.

## Recursos

`/css/**`, `/js/**`, `/img/**`, `/images/**`, `/webjars/**` e `/uploads/**` são
públicos. Qualquer rota não listada é negada por padrão.
