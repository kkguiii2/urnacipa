# Solução de problemas

## Banco não conecta

**Sintomas:** falha na inicialização do datasource, timeout, `Connection refused`
ou erro de autenticação.

**Causas:** PostgreSQL parado; URL/porta incorreta; container usando `localhost`;
credenciais inválidas; firewall; permissão insuficiente para criar o banco.

**Ações:** valide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; teste com `psql`; em
Docker use `host.docker.internal` ou o nome do serviço; verifique o log do
`DatabaseInitializer`. O initializer registra a falha, mas a inicialização JPA
ainda pode falhar em seguida.

## Docker não inicia

**Sintomas:** `docker` não reconhecido, build falha, container encerra.

**Causas:** Docker ausente; daemon parado; download de dependências falhou; JAR
não foi gerado; banco inacessível.

**Ações:** confirme `docker version`; execute `mvn clean verify`; faça
`docker build --no-cache` somente para investigar cache; consulte
`docker logs <container>`; valide rede e variáveis. Não há Compose no projeto.

## Aplicação não sobe

**Sintomas:** processo termina ou porta nunca abre.

**Causas:** Java diferente de 17; porta 8080 ocupada; bean/configuração inválida;
banco/SMTP/propriedades; falha de validação dos limites de importação.

**Ações:** execute `java -version`, `mvn clean verify` e observe a primeira
exceção causal. Para limites `@Min`, valores zero/negativos impedem o contexto.

## Login administrativo falha

**Sintomas:** retorno para `/admin/login?error=true`.

**Causas:** conta ausente, inativa, senha incorreta ou hash inválido.

**Ações:** consulte `admins` sem exibir o hash; confirme `ativo=true`; gere BCrypt
com o encoder da aplicação. A conta inicial só é criada quando a tabela está
vazia e `ADMIN_PASSWORD` está definida.

## Clicar em “Sair” não encerra a sessão

**Sintoma:** voltar ao dashboard ainda funciona.

**Causa:** templates chamam `GET /admin/logout`, mas o logout do Spring Security
é processado em POST.

**Ação:** até a correção funcional, feche a sessão/navegador em ambiente
controlado. A solução de código requer formulário POST com CSRF e está fora desta
entrega documental.

## Modal de candidatos não abre

**Sintomas:** botão não reage ou console mostra erro.

**Causas:** JavaScript interrompido antes de `toggleCandidatos`, IDs ausentes,
HTML/JS antigo em cache ou dados serializados inválidos.

**Ações:** abra o console; confirme `modalOverlay`, `candidatesGrid` e `numero`;
force recarga; confirme que o template novo está dentro do JAR/imagem.

## Modal abre sozinho

**Sintomas:** overlay recebe `active` ao carregar.

**Causas:** no código atual, `active` só é alternado por clique/ESC. Cache, script
injetado/extensão, template divergente ou chamada duplicada são hipóteses.

**Ações:** inspecione a classe inicial do overlay e event listeners. Se o artefato
for igual ao repositório e ainda ocorrer, **Esta informação não pôde ser
confirmada no estado atual do projeto.**

## Paginação ou listas lentas

**Sintomas:** página de usuários/candidatos grande, memória/HTML elevados.

**Causa:** `findAll()` e tabelas sem paginação.

**Ação:** não há parâmetro de página existente. Uma correção exige mudança
funcional em repository, controller e template; meça antes de implementar.

## Exportação Excel vazia ou falha

**Sintomas:** download de zero bytes, relatório de erros indisponível ou e-mail
sem anexo.

**Causas:** `RelatorioService` devolve `byte[0]` em `IOException`; relatório de
importação só existe quando a sessão guarda falhas; resultado pode expirar com a
sessão.

**Ações:** consulte logs; teste `/admin/relatorio/download`; repita a importação
se a sessão expirou; valide memória e integridade do workbook.

## E-mail não chega

**Sintomas:** ação aparenta sucesso, mas nenhuma mensagem é recebida.

**Causas:** destinatário vazio faz retorno silencioso; SMTP/porta/STARTTLS,
credenciais ou política do provedor.

**Ações:** defina `EMAIL_HOST`, `EMAIL_PORT`, `EMAIL_USERNAME`,
`EMAIL_PASSWORD`, `EMAIL_DESTINATARIO`; consulte logs. Não há fila nem retry
implementado.

## CSRF / HTTP 403

**Sintomas:** POST retorna `403`.

**Causas:** token ausente/expirado, sessão perdida, requisição manual fora do
formulário Thymeleaf ou perfil sem `ROLE_ADMIN`.

**Ações:** recarregue a página e envie o formulário renderizado; não desabilite
CSRF para contornar o problema; em testes use o suporte `csrf()`.

## Porta ocupada

```powershell
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
```

Pare o processo correto ou defina `SERVER_PORT`/`server.port` e reinicie. Atualize
proxy, firewall e mapeamento Docker de acordo.

## Template não encontrado

**Sintomas:** erro de resolução de view.

**Causas:** caminho/nome diferente do retorno do controller, arquivo fora de
`src/main/resources/templates`, JAR antigo.

**Ações:** compare o retorno (`admin/...`, `urna/...`), inspecione o conteúdo do
JAR e recompile.

## JavaScript

**Sintomas:** prévia de foto, importação, modal ou urna não reagem.

**Causas:** erro anterior no script, ID duplicado/ausente, cache ou dados
inesperados.

**Ações:** use console e aba Network; confirme que o HTML atual foi entregue. O
projeto não contém bundle nem arquivos `.js`; scripts ficam nos templates.

## Fotos não aparecem

**Sintomas:** `/uploads/<arquivo>` retorna 404.

**Causas:** diretório diferente, volume não montado, arquivo perdido ao recriar
container ou registro aponta para arquivo removido.

**Ações:** confira `app.upload.path`, volume `/app/uploads`, permissões e nome no
banco. Backup de banco sem backup de uploads é incompleto.

## Firewall/acesso por outro computador

**Sintomas:** funciona em `localhost`, mas não remotamente.

**Causas:** firewall, NAT, porta Docker não publicada, hostname/DNS, proxy.

**Ações:** teste no servidor, depois `http://<IP>:8080`; libere somente a origem e
porta necessárias; confirme `-p 8080:8080`. Não exponha a porta 5432
publicamente sem necessidade.
