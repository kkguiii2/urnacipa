# Segurança

## Autenticação administrativa

`SecurityConfig` configura form login em `/admin/login`.
`CustomAuthenticationProvider`:

1. consulta `Admin` por username;
2. rejeita usuário inexistente;
3. rejeita administrador com `ativo=false`;
4. compara a senha recebida com o hash usando `PasswordEncoder`;
5. concede `ROLE_ADMIN`.

O encoder é `BCryptPasswordEncoder`. A sessão administrativa permite no máximo
uma sessão concorrente por conta; um novo login substitui a sessão anterior.

## Identificação do eleitor

O eleitor não usa Spring Security. A matrícula válida é guardada em sessão após
consulta a `Usuario`. Controllers de votação verificam a existência dessa
matrícula. Isso é uma identificação por dado cadastral, não autenticação por
segredo.

## Matriz de permissões

| Recurso | Público | Eleitor com matrícula na sessão | `ROLE_ADMIN` |
| --- | ---: | ---: | ---: |
| `/`, `/auth/**` | sim | sim | sim |
| `/votacao/candidato/**`, `/votacao/sucesso`, `/votacao/reset` | sim | sim | sim |
| `/votacao/tela`, `POST /votacao/votar` | filtro permite; controller bloqueia sem sessão | sim | somente se também houver sessão de eleitor |
| `/admin/login` | sim | sim | sim |
| `/admin/**` restante | não | não | sim |
| `/css/**`, `/js/**`, `/images/**`, `/webjars/**` | sim | sim | sim |
| `/uploads/**` | permitido por `anyRequest().permitAll()` | sim | sim |

## CSRF

CSRF permanece habilitado. Formulários Thymeleaf `POST` recebem o token do Spring
Security. O endpoint `/admin/relatorio/download` é ignorado explicitamente, ainda
que seja GET. Os testes confirmam `403` para importação administrativa sem token.

## Sessão e logout

- timeout configurado: 30 minutos;
- logout Spring Security: `POST /admin/logout`, invalida sessão, limpa contexto e
  cookie;
- os templates administrativos usam um link GET para `/admin/logout`;
- o controller GET apenas redireciona e não invalida a sessão.

Esse desencontro significa que clicar em “Sair” pode não encerrar a autenticação
administrativa. A documentação não altera o comportamento, mas o risco deve ser
tratado antes de produção.

## Validações de entrada

- matrícula manual/importada aceita somente dígitos;
- importação limita arquivo, linhas, expansão ZIP e entradas, rejeita macro e
  fórmula;
- falhas inesperadas de importação recebem mensagem genérica;
- nome do arquivo importado é reduzido ao último segmento;
- foto de candidato recebe nome UUID.

A foto não é validada no servidor por MIME, assinatura ou dimensão; `accept`
no HTML é apenas uma orientação ao navegador. Uploads são servidos publicamente.

## Credenciais e segredos

Não há mais senha administrativa ou senha de banco fixa no código. A senha do
banco deve ser fornecida por `DB_PASSWORD`. Quando a tabela `admins` está vazia,
o bootstrap só cria a primeira conta se `ADMIN_PASSWORD` estiver definida; o
username vem de `ADMIN_USERNAME`, cujo padrão é `admin`.

Antes de implantar:

1. use segredos externos (`DB_PASSWORD`, `EMAIL_PASSWORD`);
1. forneça `ADMIN_PASSWORD` somente no bootstrap e remova-a do ambiente depois;
1. altere/desative a conta administrativa inicial quando necessário;
1. não inclua propriedades reais em imagens, logs ou commits.

Não existe endpoint de troca ou recuperação de senha. Para rotação, gere um hash
BCrypt com `BCryptPasswordEncoder` em ambiente controlado e atualize `admins`
diretamente, ou implemente futuramente um fluxo auditado.

## Headers, TLS, CORS e portas

- não há política customizada de headers; aplicam-se os defaults da versão do
  Spring Security usada;
- TLS/HTTPS não é configurado na aplicação;
- CORS não é configurado explicitamente;
- a aplicação escuta na porta 8080 por padrão;
- firewall e proxy reverso não fazem parte do repositório.

Em produção, termine TLS em proxy/plataforma, restrinja a exposição do PostgreSQL
e publique apenas a porta HTTP/HTTPS necessária.

## Riscos técnicos adicionais

| Risco | Evidência | Efeito possível |
| --- | --- | --- |
| voto concorrente | leitura de `votou` sem lock/constraint por eleitor | mais de um voto para a mesma matrícula em corrida |
| HTML criado com `innerHTML` | nomes/fotos de candidatos concatenados no modal | conteúdo cadastrado por admin pode virar markup no navegador |
| log de matrícula e candidato | `VotacaoService` registra dados do voto | reduz a separação prática entre eleitor e escolha nos logs |
| upload público | `/uploads/**` sem autenticação | qualquer conhecedor da URL acessa a foto |
| GET de logout | link não aciona o logout Spring | sessão administrativa persiste |
| esquema sem FK | `votos.candidato_id` escalar | votos órfãos após exclusão de candidato |

## Não confirmado

Política de retenção, base legal, auditoria, classificação dos dados, exigências
da organização e hardening da infraestrutura: **Esta informação não pôde ser
confirmada no estado atual do projeto.**
