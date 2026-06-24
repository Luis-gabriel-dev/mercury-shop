# Mercury Shop

E-commerce / Order Management API — backend RESTful seguro e escalável.
A especificação completa (fonte de verdade) fica no documento interno de instruções e
roadmap do projeto, mantido fora do versionamento (ver `.gitignore`).

> **Status:** Fase 1 (Fundação) — catálogo de **produtos e categorias** sobre PostgreSQL + Flyway,
> documentado via OpenAPI (apenas em `dev`), em arquitetura hexagonal, com testes e Dockerfile.

---

## Stack (Fase 1)

Java 21 · Spring Boot 3.4 · Maven · Spring Web/Validation/Data JPA · PostgreSQL · Flyway ·
springdoc-openapi (dev) · Actuator (health) · JUnit 5 + Testcontainers.

Redis, RabbitMQ, Spring Security/JWT, rate limiting e observabilidade completa entram nas fases seguintes.

## Arquitetura — Hexagonal (Ports & Adapters)

O domínio não conhece HTTP nem JPA. Cada feature segue `domain → application → adapter`:

```
product/
  domain/         Product, Category (modelos puros) + portas (ProductRepository, CategoryRepository)
  application/    CategoryService, ProductService (casos de uso) + commands
  adapter/
    in/web/       Controllers + DTOs (records) + WebMapper
    out/persistence/  JPA entities, repositórios Spring Data e adapters que implementam as portas
shared/
  application/    PageQuery / PageResult (paginação independente de framework)
  config/         OpenApiConfig (perfil dev)
  exception/      GlobalExceptionHandler + ApiError (formato de erro padrão)
  web/            RequestIdFilter (request_id por requisição)
```

### Decisões desta fase
- **Produto + Categoria** com relação (FK `products.category_id` → `categories.id`).
- **Spring Security adiado para a Fase 2** — nesta fase os endpoints de catálogo ficam abertos (somente para dev). Toda a seção 7 de segurança entra na Fase 2.
- **`@Version`** já presente em `Product` (e `Category`), preparando o **lock otimista** que será usado no checkout (Fase 3).
- **Identidade = UUID** gerada no domínio; **DTOs sempre** (entidades JPA nunca são serializadas); campos internos como `version` não saem nas respostas.
- **Schema 100% Flyway** (`V1__init.sql`); aplicação roda com `ddl-auto=validate`.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| POST | `/v1/categories` | cria categoria (409 se nome duplicado) |
| GET | `/v1/categories` | lista paginada |
| GET | `/v1/categories/{id}` | busca por id (404 se não existe) |
| PATCH | `/v1/categories/{id}` | atualização parcial |
| DELETE | `/v1/categories/{id}` | remove |
| POST | `/v1/products` | cria produto |
| GET | `/v1/products` | lista paginada — filtros `name`, `categoryId`; `page`, `size`, `sort`, `direction` |
| GET | `/v1/products/{id}` | busca por id |
| PATCH | `/v1/products/{id}` | atualização parcial |
| DELETE | `/v1/products/{id}` | remove |

Formato de erro padrão:

```json
{ "error": { "code": "NOT_FOUND", "message": "Produto não encontrado", "requestId": "req_ab12cd34ef56" } }
```

## Como rodar (dev)

Pré-requisitos: **Java 21**, **Docker**. Maven **não** é necessário — use o wrapper (`mvnw`/`mvnw.cmd`).

```bash
# 1. Variáveis de ambiente
cp .env.example .env            # ajuste as credenciais

# 2. Sobe o Postgres (banco mercury_shop_db)
docker compose up -d

# 3. Roda a aplicação no perfil dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev      # Linux/macOS
# Windows (PowerShell):
# .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

- API: `http://localhost:8080`
- Swagger (somente em `dev`): `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

> No perfil `prod`, Swagger/OpenAPI ficam **desligados** e o Actuator expõe apenas `health` sem detalhes.

## Testes

```bash
./mvnw test          # ou .\mvnw.cmd test no Windows
```

- **Unitário:** invariantes de domínio de `Product` (sem Spring).
- **Integração (Testcontainers):** sobe Postgres real, roda Flyway e exercita `POST → GET` de produto + 404.
  Requer Docker em execução.

## Build / Docker

```bash
./mvnw clean package                 # gera target/mercury-shop-0.1.0-SNAPSHOT.jar
docker build -t mercury-shop:latest .   # imagem multi-stage (runtime JRE 21, usuário não-root)
```

## Roadmap

Fase 1 ✅ Fundação · Fase 2 Usuários + Segurança · Fase 3 Pedidos (checkout/lock otimista/idempotência) ·
Fase 4 Assíncrono (RabbitMQ) + cache · Fase 5 Produção (observabilidade, Caddy/HTTPS, compose completo, CI/CD).
