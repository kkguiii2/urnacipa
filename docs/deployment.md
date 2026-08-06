# Implantação e operação

## Execução local para desenvolvimento

Pré-requisitos: Java 17, Maven, PostgreSQL e credenciais com acesso ao banco.

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5432/cipa'
$env:DB_USERNAME='<usuario>'
$env:DB_PASSWORD='<senha>'
$env:ADMIN_PASSWORD='<senha-inicial-forte>'
$env:MESARIO_PASSWORD='<senha-inicial-forte>'
$env:CABINE_PASSWORD='<senha-do-dispositivo>'
$env:SESSION_COOKIE_SECURE='false'
mvn spring-boot:run
```

Acesse `http://localhost:8080`. Se `DB_URL` não for definida, a aplicação tenta
criar `cipa` conectando-se ao banco `postgres` na mesma instância.

## JAR em servidor

Compile e teste em uma máquina de build:

```powershell
mvn clean verify
```

Copie `target/sistema-votacao-cipa-1.0.0.jar`, defina as variáveis no serviço do
sistema operacional e execute:

```powershell
java -jar sistema-votacao-cipa-1.0.0.jar
```

O repositório não contém unit file do systemd, serviço Windows, health check,
proxy reverso ou pipeline CI/CD. **Esta informação não pôde ser confirmada no
estado atual do projeto.**

## Docker

O Dockerfile:

1. usa Maven/JDK 17 para baixar dependências e executar
   `mvn clean package -DskipTests`;
2. copia o JAR para uma imagem JRE 17 Alpine;
3. cria `/app/uploads`;
4. expõe 8080 e executa `java -jar app.jar`.

Build:

```powershell
mvn clean verify
docker build -t sistema-votacao-cipa:1.0.0 .
```

Execução com banco no host Windows/macOS:

```powershell
docker run --name cipa-app -p 8080:8080 `
  -e DB_URL='jdbc:postgresql://host.docker.internal:5432/cipa' `
  -e DB_USERNAME='<usuario>' `
  -e DB_PASSWORD='<senha>' `
  -e ADMIN_PASSWORD='<senha-inicial-forte>' `
  -e MESARIO_PASSWORD='<senha-inicial-forte>' `
  -e CABINE_PASSWORD='<senha-do-dispositivo>' `
  -e SESSION_COOKIE_SECURE='true' `
  -v cipa-uploads:/app/uploads `
  sistema-votacao-cipa:1.0.0
```

Não há `docker-compose.yml`. Se banco e aplicação forem containers, conecte-os a
uma mesma rede Docker e use o nome do container/serviço do PostgreSQL na URL:

```text
jdbc:postgresql://<nome-do-container>:5432/cipa
```

Esse nome só é resolvido dentro da rede Docker, não pelo navegador do usuário.

## Produção

Checklist mínimo demonstrável a partir das dependências do sistema:

- banco e uploads em armazenamento persistente;
- segredos apenas por variáveis/secret manager;
- conta administrativa padrão rotacionada;
- proxy HTTPS e firewall fora da aplicação;
- backup antes da atualização;
- build validado com `mvn clean verify`;
- imagem marcada com versão imutável;
- monitoramento dos logs de inicialização, banco, scheduler e e-mail.

Não há estratégia implementada de zero downtime. Para uma instância única, a
atualização causa indisponibilidade entre parada e início.

## O que exige recompilar ou reiniciar?

| Alteração | Recompilar JAR? | Recriar imagem? | Reiniciar? |
| --- | ---: | ---: | ---: |
| classe Java/pom.xml | sim | sim, se usa Docker | sim |
| template HTML | sim, pois fica no classpath/JAR | sim | sim |
| JavaScript embutido no HTML | sim | sim | sim |
| CSS | sim | sim | sim |
| `application.properties` empacotado | sim | sim | sim |
| variável de ambiente | não | não | sim |
| dados no PostgreSQL | não | não | normalmente não |
| arquivos em volume `uploads` | não | não | não |
| Dockerfile | JAR depende da mudança | sim | recriar container |

Em execução via IDE com recursos copiados automaticamente, HTML/CSS podem
parecer atualizar sem package; isso não se aplica ao JAR/imagem de produção.

## Atualização sem perder dados

1. impeça novas operações ou escolha uma janela de manutenção;
2. faça backup do PostgreSQL e do diretório de uploads;
3. construa e teste a nova versão;
4. pare somente a aplicação;
5. preserve o banco e o volume de uploads;
6. inicie a nova versão com as mesmas variáveis;
7. valide login, dashboard, urna, imagens e relatório;
8. mantenha a imagem/JAR anterior para rollback.

Não apague volumes e não use um container efêmero sem `-v` para fotos que devem
persistir. O Hibernate pode alterar o esquema por `ddl-auto=update`; por não haver
migrations, restauração testada é especialmente importante.

## Backup e restauração

Backup custom-format do banco:

```powershell
pg_dump --format=custom --file=cipa.backup `
  --dbname='postgresql://<usuario>@<host>:5432/cipa'
```

Restauração em banco vazio:

```powershell
createdb --host=<host> --username=<usuario> cipa_restaurada
pg_restore --clean --if-exists --no-owner `
  --host=<host> --username=<usuario> `
  --dbname=cipa_restaurada cipa.backup
```

Passe a senha de forma segura (`PGPASSWORD` temporário ou arquivo de credenciais
protegido); não a inclua no histórico do terminal. Faça também cópia consistente
de `uploads/`. Não há automação de backup no repositório.

Antes de restaurar sobre produção, teste em banco separado e pare gravações.

## Troca de senha administrativa

Não existe tela ou endpoint. `ADMIN_PASSWORD` serve somente para criar a primeira
conta quando a tabela está vazia. Para trocar a senha de uma conta existente:

1. gerar um hash BCrypt com `BCryptPasswordEncoder` em ambiente controlado;
2. executar `UPDATE admins SET password = '<hash>' WHERE username = '<usuario>';`;
3. encerrar sessões existentes e testar o novo acesso.

Nunca grave a senha em texto puro. Alterar `ADMIN_PASSWORD` não muda uma conta já
existente, pois o initializer só cria quando `adminRepository.count() == 0`.

## Alterar ou conectar ao banco

- local: `jdbc:postgresql://localhost:5432/cipa`;
- aplicação em Docker → banco no host: `host.docker.internal` onde suportado;
- aplicação e banco na mesma rede Docker: nome do serviço/container;
- banco remoto: hostname/IP autorizado pelo firewall e pelo PostgreSQL.

Defina `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e reinicie. O dialeto e driver são
PostgreSQL; trocar para outro produto de banco exige mudança de dependência e
validação de código/esquema, não apenas URL.

## Endereços de rede

| Nome | Significado no contexto |
| --- | --- |
| `localhost` | nome que aponta para a própria máquina/processo de rede |
| `127.0.0.1` | endereço IPv4 de loopback da própria máquina |
| IP do servidor | endereço alcançável por outros computadores na rede |
| hostname | nome DNS/rede que resolve para um IP |
| domínio | nome DNS normalmente público ou corporativo, possivelmente via proxy |
| nome do container | DNS interno de uma rede Docker |

Dentro de um container, `localhost` é o próprio container. Para disponibilizar a
aplicação a outros computadores, publique a porta (`-p 8080:8080` no Docker),
libere o firewall necessário e acesse `http://<ip-ou-hostname-do-servidor>:8080`.
A configuração não fixa `server.address`; o bind efetivo deve ser verificado no
log do ambiente.
