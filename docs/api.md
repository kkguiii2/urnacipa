# Endpoints HTTP

## Natureza da interface

O sistema expõe principalmente endpoints MVC para navegador, não uma API REST
versionada. As respostas são templates HTML, redirecionamentos, JSON pontual ou
arquivos Excel. Não existe especificação OpenAPI.

## Convenções

- rotas `/admin/**`: exigem autenticação e `ROLE_ADMIN`, exceto a página e o
  processamento de login;
- demais rotas: permitidas pelo filtro, com controles adicionais por sessão e
  regras de negócio;
- `POST`: protegido por CSRF, salvo se outra regra do Spring não se aplicar;
- erros de formulário usam `Model` ou flash attributes e normalmente retornam ou
  redirecionam para a página de origem;
- não há tratamento HTTP global para produzir JSON de erro.

## Entrada e autenticação

| Método | URL | Entrada/DTO | Resposta | Acesso | Erros observáveis |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/` | — | `302` para `/auth/login` | público | — |
| `GET` | `/admin/login` | query opcional `error`, `logout` consumida pelo fluxo Spring | template `admin/login` | público | a configuração redireciona falha para `?error=true`; o template não exibe essas queries explicitamente |
| `POST` | `/admin/login` | form `username`, `password`; sem DTO | Spring Security autentica e redireciona para `/admin/dashboard` | público | credencial inválida → `/admin/login?error=true`; admin inativo → falha de autenticação |
| `POST` | `/admin/logout` | CSRF | invalida sessão, limpa `JSESSIONID` e redireciona para `/admin/login?logout=true` | autenticado | CSRF ausente → `403` |
| `GET` | `/admin/logout` | — | controller apenas redireciona para `/admin/login` | `ROLE_ADMIN` | não executa o logout configurado; a sessão pode permanecer autenticada |

Existe um método `AdminController.adminLoginPost(username, senha, ...)` também
mapeado para `POST /admin/login`, porém o filtro de login usa essa mesma URL e o
form envia `password`, não `senha`. O comportamento efetivo confirmado pelos
componentes é o processamento do Spring Security.

## Administração

| Método | URL | Entrada/DTO | Resposta | Acesso | Erros observáveis |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/admin/dashboard` | — | `admin/dashboard`; totais, participação e configuração | `ROLE_ADMIN` | falha de banco propaga como erro do servidor |
| `GET` | `/admin/usuarios` | sessão opcional `ultimaImportacaoEleitores` | `admin/usuarios` | `ROLE_ADMIN` | falha de banco propaga |
| `POST` | `/admin/usuarios/adicionar` | form `matricula`, `nome`; sem DTO | redirect `/admin/usuarios` | `ROLE_ADMIN` | matrícula não numérica/duplicada ou exceção → flash `erro` |
| `POST` | `/admin/usuarios/excluir/{id}` | path `id: Long` | redirect `/admin/usuarios` | `ROLE_ADMIN` | ID inexistente/erro de integridade pode propagar |
| `GET` | `/admin/candidatos` | — | `admin/candidatos` | `ROLE_ADMIN` | falha de banco propaga |
| `POST` | `/admin/candidatos/adicionar` | form `numero: Integer`, `nome`, `foto?: MultipartFile`; sem DTO | redirect `/admin/candidatos` | `ROLE_ADMIN` | número duplicado, erro de upload ou persistência → flash `erro` |
| `POST` | `/admin/candidatos/toggle/{id}` | path `id: Long` | redirect `/admin/candidatos` | `ROLE_ADMIN` | ID inexistente é ignorado |
| `POST` | `/admin/candidatos/excluir/{id}` | path `id: Long` | redirect `/admin/candidatos` | `ROLE_ADMIN` | ID inexistente/erro de persistência pode propagar |
| `GET` | `/admin/configuracao` | — | `admin/configuracao` | `ROLE_ADMIN` | falha de banco propaga |
| `POST` | `/admin/configuracao/salvar` | `dataInicio`, `dataFim` em `yyyy-MM-dd'T'HH:mm` | redirect `/admin/configuracao` | `ROLE_ADMIN` | parse/persistência → flash genérico `erro` |
| `POST` | `/admin/eleicao/abrir` | — | redirect `/admin/dashboard` | `ROLE_ADMIN` | falha de banco propaga |
| `POST` | `/admin/eleicao/encerrar` | — | fecha, tenta gerar/enviar relatório e redireciona | `ROLE_ADMIN` | falha de relatório/e-mail vira mensagem de sucesso parcial |
| `GET` | `/admin/relatorio` | — | `admin/relatorio`, ranking e resumo | `ROLE_ADMIN` | falha de banco propaga |
| `GET` | `/admin/relatorio/download` | — | bytes XLSX, `relatorio_cipa.xlsx` | `ROLE_ADMIN` | geração pode devolver arquivo de zero bytes em caso de `IOException` |
| `POST` | `/admin/relatorio/enviar` | — | redirect `/admin/relatorio` | `ROLE_ADMIN` | exceção → flash `erro`; destinatário vazio não lança erro |

`GET /admin/relatorio/download` aparece na lista de exceção CSRF, mas GET não é
normalmente sujeito à proteção CSRF.

## Importação de eleitores

| Método | URL | Entrada/DTO | Resposta | Acesso | Erros observáveis |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/admin/usuarios/importacao` | multipart `arquivo`; binding `ImportacaoEleitoresFormDto` | PRG para `/admin/usuarios`; resultado na sessão | `ROLE_ADMIN` + CSRF | arquivo ausente, `PlanilhaImportacaoException`, limite multipart ou erro inesperado → flash `erro` |
| `GET` | `/admin/usuarios/importacao/modelo` | — | XLSX `modelo-importacao-eleitores.xlsx` | `ROLE_ADMIN` | falha de geração propaga `PlanilhaImportacaoException` |
| `GET` | `/admin/usuarios/importacao/relatorio-erros` | sessão `ultimaImportacaoEleitores` | XLSX com linhas não importadas | `ROLE_ADMIN` | sem resultado/falhas → redirect com flash `erro` |

O DTO de resultado contém nome do arquivo, totais por status, duração e detalhes
por linha. Ele é serializável e fica somente na sessão HTTP.

## Urna e identificação do eleitor

| Método | URL | Entrada/DTO | Resposta | Acesso | Erros observáveis |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/auth/login` | — | `urna/login` ou `urna/indisponivel` | público | eleição fechada/fora do período → indisponível |
| `POST` | `/auth/verificar` | form `matricula`; sem DTO | `urna/confirmar` ou `urna/login` | público + CSRF | formato, ausência, inatividade ou voto anterior → mensagem no model |
| `POST` | `/auth/confirmar` | matrícula da sessão | redirect `/votacao/tela` | público + CSRF | sessão sem matrícula → login |
| `POST` | `/auth/corrigir` | sessão | remove matrícula/nome e redireciona | público + CSRF | — |
| `GET` | `/auth/logout` | sessão | invalida sessão e redireciona | público | — |
| `GET` | `/votacao/tela` | matrícula da sessão | `urna/votacao` ou indisponível | público; sessão exigida pelo controller | sem sessão → login |
| `GET` | `/votacao/candidato/{numero}` | path `numero: Integer` | objeto JSON com `id`, `numero`, `nome`, `foto` ou corpo vazio | público | conversão inválida → `400`; inexistente → `200` com retorno nulo |
| `POST` | `/votacao/votar` | form `candidatoId: Long`; matrícula da sessão | `urna/sucesso`, redirect para urna ou indisponível | público + CSRF; sessão exigida | candidato/voto inválido → mensagem ou redirect |
| `GET` | `/votacao/sucesso` | — | `urna/sucesso` | público | — |
| `GET` | `/votacao/reset` | sessão | invalida sessão e redireciona | público | — |

## Status HTTP

Os controllers não atribuem status de domínio específicos. Sucesso de página é
normalmente `200`, downloads são `200`, PRG usa `302`, falha CSRF/autorização usa
`403` e falha de conversão de parâmetro tende a `400` pelo Spring MVC. Exceções
não tratadas tendem a `500`.

Rate limiting, versionamento, CORS explícito e contrato de compatibilidade:
**Esta informação não pôde ser confirmada no estado atual do projeto.**
