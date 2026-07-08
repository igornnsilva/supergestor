# SuperGestor

Sistema web para gestao de supermercado com backend Spring Boot, Gradle, API REST e frontend Vue.js 3.

## Tecnologias

- Java 17
- Spring Boot
- Gradle
- Spring Web
- Spring Data JPA
- H2 Database
- Swagger/OpenAPI
- Vue.js 3
- Vite
- Vue Router
- Pinia
- Axios

## Estrutura

```text
supergestor/
  backend/   API REST Spring Boot
  frontend/  SPA Vue.js 3
```

## Como rodar o backend

```bash
cd backend
./gradlew bootRun
```

No Windows:

```bash
cd backend
gradlew.bat bootRun
```

## Login inicial

Ao iniciar o backend, o sistema cria estes usuarios de demonstracao:

```text
admin@supergestor.local    senha: 123456    perfil: ADMIN
caixa@supergestor.local    senha: 123456    perfil: CAIXA
gerente@supergestor.local  senha: 123456    perfil: GERENTE
estoque@supergestor.local  senha: 123456    perfil: ESTOQUISTA
```

## Rodando com MySQL

O H2 continua disponivel para desenvolvimento rapido. Para usar MySQL, crie o banco:

```sql
CREATE DATABASE supergestor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Opcionalmente, crie um usuario dedicado usando [docs/mysql-setup.sql](docs/mysql-setup.sql).

Depois rode o backend com o profile `mysql`.

PowerShell com usuario dedicado:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:MYSQL_USER="supergestor_user"
$env:MYSQL_PASSWORD="123456"
.\gradlew.bat bootRun
```

PowerShell usando `root`:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="sua_senha_do_mysql"
.\gradlew.bat bootRun
```

API:

- http://localhost:8080/api
- Swagger: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console

Dados do H2:

- JDBC URL: `jdbc:h2:mem:supergestor`
- Usuario: `sa`
- Senha: vazia

## Como rodar o frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend:

- http://localhost:5173

## Modulos iniciais

- Dashboard gerencial
- Produtos
- Categorias
- Fornecedores
- Clientes
- Estoque baixo
- Nova venda com baixa automatica de estoque
- Historico de vendas
- Usuarios e papeis modelados no backend
