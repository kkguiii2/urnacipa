# Sistema de Votação CIPA

Sistema completo de votação eletrônica para eleição da Comissão Interna de Prevenção de Acidentes.

## 🚀 Como Executar (Local)

### Pré-requisitos
- Java 17+
- PostgreSQL 14+ (instalado localmente)
- Maven 3.8+

### 1. Instalar e Configurar PostgreSQL

1. Baixe e instale o PostgreSQL: https://www.postgresql.org/download/
2. Durante a instalação, defina a senha do usuário `postgres` (ex: `micro123`)
3. Certifique-se de que o serviço PostgreSQL está rodando na porta `5432`

### 2. Criar Banco de Dados

Abra o **pgAdmin** ou o terminal `psql` e execute:

```sql
CREATE DATABASE cipa;
```

Se houver script de schema disponível:
```bash
psql -U postgres -d cipa -f src/main/resources/db/schema.sql
```

> **Nota:** O Spring Boot irá criar/atualizar as tabelas automaticamente via `ddl-auto=update`.

### 3. Configurar Aplicação

Edite o arquivo `src/main/resources/application.properties`:

```properties
# Banco de dados (ajuste a senha conforme sua instalação)
spring.datasource.url=jdbc:postgresql://localhost:5432/cipa
spring.datasource.username=postgres
spring.datasource.password=micro123

# IP permitido para votação (urna) — seu IPv4 na rede
app.ip.permitido=192.168.100.46

# IP permitido para painel admin — seu IPv4 na rede
app.ip.admin.permitido=192.168.100.46
```

**Para descobrir seu IP na rede:**
```bash
# Windows
ipconfig

# Linux/Mac
ip addr show
```

### 4. Compilar e Executar

```bash
# Compilar
mvn clean package -DskipTests

# Executar
java -jar target/sistema-votacao-cipa-1.0.0.jar
```

Ou diretamente via Maven:
```bash
mvn spring-boot:run
```

### 5. Acessar

- **Na mesma máquina:** http://localhost:8080
- **Na rede local:** http://<SEU_IP>:8080 (ex: http://192.168.100.46:8080)

## 🌐 Acesso na Rede Local

O sistema já está configurado com `server.address=0.0.0.0`, o que permite acesso de qualquer máquina na mesma rede.

Para liberar acesso de outras máquinas:
1. Verifique se o Firewall do Windows permite conexões na porta `8080`
2. Configure `app.ip.permitido` com o IP da máquina que será a urna
3. Configure `app.ip.admin.permitido` com o IP da máquina do administrador
4. Múltiplos IPs podem ser separados por vírgula: `192.168.1.10,192.168.1.11`

## 📱 Fluxo de Uso

### Tela de Votação (Urna)
1. Acesse `/auth/login`
2. Digite a matrícula do funcionário
3. Confirme a identidade
4. Vote no candidato
5. Receba comprovante

### Painel Administrativo
1. Acesse `/admin/login`
2. Senha: `admin123`
3. Gerencie candidatos, usuários e configure a eleição

## 📊 Funcionalidades

- ✅ Votação anônima
- ✅ Restrição por IP
- ✅ Controle de horário
- ✅ Upload de fotos de candidatos
- ✅ Relatório CSV e Excel
- ✅ Envio automático por e-mail
- ✅ Interface moderna estilo urna eletrônica

## 🗂️ Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/cipa/votacao/
│   │   ├── controller/  # Controllers web
│   │   ├── entity/      # Entidades JPA
│   │   ├── repository/  # Repositories
│   │   ├── service/     # Business logic
│   │   ├── config/      # Configurações
│   │   └── filter/      # Filtro de IP
│   └── resources/
│       ├── templates/   # Templates Thymeleaf
│       ├── static/css/  # CSS
│       └── application.properties
```

## ⚙️ Configuração de IP

Para alterar o IP permitido, edite `application.properties`:
```properties
# Urna de votação
app.ip.permitido=192.168.0.50

# Painel admin
app.ip.admin.permitido=192.168.0.100

# Deixe em branco para liberar todos (apenas dev)
app.ip.permitido=
```

## 📧 Configuração de E-mail (Locaweb)

O sistema já está configurado para Locaweb:
- Host: smtp.locaweb.com.br
- Porta: 587
- TLS: Enabled

## 🔒 Segurança

- Voto único por usuário
- IP restrito para votação
- Não há relação entre voto e usuário (anonimato)
- Sessão autenticada para admin