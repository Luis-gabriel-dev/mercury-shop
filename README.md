# Mercury Shop

E-commerce / Order Management API — backend RESTful seguro e escalável.
A especificação completa (fonte de verdade) fica no documento interno de instruções e
roadmap do projeto, mantido fora do versionamento (ver `.gitignore`).

> **Status:** Fase 3 (Núcleo de pedidos) concluída — sobre as Fases 1 (catálogo) e 2 (usuários +
> segurança), agora com **carrinho** (Redis), **checkout transacional** com baixa de estoque sob
> **lock otimista**, **idempotência** (Idempotency-Key) e cancelamento. Arquitetura hexagonal,
> Postgres + Flyway + Redis. 41 testes verdes, incluindo teste de concorrência.

---

## Stack

Java 21 · Spring Boot 3.4 · Maven · Spring Web/Validation/Data JPA · PostgreSQL · Flyway ·
**Spring Security + OAuth2 Resource Server (JWT RSA)** · **Redis** (refresh tokens, lockout, rate limiting) ·
**Bucket4j** (rate limiting) · springdoc-openapi (dev) · Actuator (health) · JUnit 5 + Testcontainers.

RabbitMQ (assíncrono) e observabilidade completa entram nas fases seguintes.

## Arquitetura — Hexagonal (Ports & Adapters)

O domínio não conhece HTTP nem JPA. Cada feature segue `domain → application → adapter`:

```
product/   catálogo (Fase 1)
cart/      carrinho por usuário no Redis (Fase 3)
order/     checkout transacional, lock otimista, idempotência, cancelamento (Fase 3)
user/      cadastro, autenticação, RBAC (Fase 2)
  domain/         User, Role, UserStatus, OneTimeToken, PasswordPolicy + portas
                  (UserRepository, RefreshTokenStore, LoginAttemptStore, PasswordHasher,
                   TokenGenerator, AccessTokenIssuer, EmailSender, ...)
  application/    AuthService, UserService (casos de uso) + commands
  adapter/
    in/web/       AuthController, UserController, AdminUserController + DTOs (records)
    out/persistence/  JPA (users, user_roles, one_time_tokens)
    out/security/     BCrypt, emissor de JWT RSA, gerador de tokens
    out/redis/        refresh tokens (rotação/revogação) e bloqueio de login
    out/email/        stub assíncrono (loga o link; RabbitMQ na Fase 4)
shared/
  security/       SecurityConfig (deny-by-default, RBAC, headers, CORS), JWT (RSA),
                  RateLimitingFilter (Bucket4j+Redis), AuditLogger
  exception/      GlobalExceptionHandler + ApiError (formato de erro padrão)
  web/            RequestIdFilter (request_id por requisição)
  application/    PageQuery / PageResult (paginação independente de framework)
```

### Decisões de segurança (Fase 2)
- **Access token = JWT RSA (RS256)**, vida 15 min, payload só `sub`+`roles`+`exp`. Chaves de env;
  em dev/test sem chave configurada, gera-se um par **efêmero** no boot (nunca usar em prod).
- **Refresh token = string opaca** em cookie `HttpOnly; Secure; SameSite=Strict`, guardado como
  hash no Redis (whitelist) com **rotação** (o antigo é invalidado) e revogação no logout/reset.
- **Senhas com BCrypt (custo 12)**; política forte (≥ 12 chars com maiúscula, minúscula, dígito e símbolo).
- **Tokens de verificação (24h) / reset (1h)**: aleatórios, uso único, guardados como **hash**.
- **Bloqueio temporário** após N falhas de login (Redis) e **rate limiting** (Bucket4j+Redis) em
  `/v1/auth/login|register|forgot-password` → `429` + `Retry-After`.
- **Deny by default**; escrita de catálogo e `/v1/users` (admin) exigem `ADMIN`; leitura de catálogo é pública.
- Headers de segurança (CSP, X-Frame-Options DENY, nosniff, Referrer-Policy, HSTS), **CORS** com allowlist.
- **DTOs sempre** (entidades JPA nunca serializadas); `passwordHash`/`version` nunca saem nas respostas.
- Auditoria estruturada de eventos de segurança com `request_id` e e-mail mascarado.

## Endpoints

### Autenticação (`/v1/auth`, público + rate limited)
| Método | Rota | Descrição |
|---|---|---|
| POST | `/register` | cadastro (cria `PENDING_VERIFICATION`, dispara e-mail de verificação) |
| GET | `/verify?token=` | ativa a conta |
| POST | `/login` | retorna access token (JWT) + cookie `refresh_token` |
| POST | `/refresh` | novo access token, rotaciona o refresh (lê o cookie) |
| POST | `/logout` | revoga o refresh token |
| POST | `/forgot-password` | envia token de reset (resposta sempre genérica) |
| POST | `/reset-password` | redefine a senha |

### Usuário (`/v1/users`)
| Método | Rota | Acesso |
|---|---|---|
| GET | `/me` | autenticado |
| PATCH | `/me` | autenticado |
| POST | `/me/change-password` | autenticado (exige senha atual) |
| GET | `/` · `/{id}` | **ADMIN** |

### Catálogo (`/v1`)
Leitura (`GET /v1/products`, `/v1/categories`) **pública**; escrita (`POST/PATCH/DELETE`) exige **ADMIN**.

### Carrinho (`/v1/cart`, autenticado)
| Método | Rota | Descrição |
|---|---|---|
| GET | `/v1/cart` | carrinho atual com preços e total |
| POST | `/v1/cart/items` | adiciona/incrementa item `{productId, quantity}` |
| PUT | `/v1/cart/items/{productId}` | define quantidade (`0` remove) |
| DELETE | `/v1/cart/items/{productId}` · `/v1/cart` | remove item · limpa |

### Pedidos (`/v1/orders`, autenticado)
| Método | Rota | Descrição |
|---|---|---|
| POST | `/v1/orders` | checkout do carrinho — header **`Idempotency-Key` obrigatório**; baixa estoque (lock otimista) → `PENDING` |
| GET | `/v1/orders` · `/v1/orders/{id}` | próprios pedidos (outro usuário → 404) |
| POST | `/v1/orders/{id}/cancel` | cancela `PENDING` e restaura estoque |
| GET | `/v1/admin/orders` | todos os pedidos (**ADMIN**) |

Formato de erro padrão:

```json
{ "error": { "code": "UNAUTHORIZED", "message": "E-mail ou senha inválidos", "requestId": "req_ab12cd34ef56" } }
```

## Como rodar (dev)

Pré-requisitos: **Java 21**, **Docker**. Maven **não** é necessário — use o wrapper (`mvnw`/`mvnw.cmd`).

```bash
# 1. Variáveis de ambiente
cp .env.example .env            # ajuste as credenciais

# 2. Sobe Postgres + Redis
docker compose up -d

# 3. Roda a aplicação no perfil dev (sem JWT_*_KEY no .env, gera par RSA efêmero)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev      # Linux/macOS
# Windows (PowerShell):
# .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

- API: `http://localhost:8080` · Swagger (dev): `/swagger-ui.html` · Health: `/actuator/health`
- Para chaves RSA fixas em prod, gere e injete via env (`JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY`):
  ```bash
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
  openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem
  ```
- Cookie do refresh é `Secure`: em navegador, use HTTPS; via curl/MockMvc funciona em HTTP.

## Testes

```bash
./mvnw test          # ou .\mvnw.cmd test no Windows
```

- **Unitários:** invariantes de domínio (`Product`, `User`, `PasswordPolicy`, `Order`, `Cart`) — sem Spring.
- **Integração (Testcontainers, Postgres + Redis):** catálogo com RBAC; fluxo de auth (register →
  verify → login → `/me`, rotação de refresh, rate limit `429`, 401/403); carrinho → checkout →
  idempotência → cancelamento. Requer Docker.
- **Concorrência:** M compradores no último item; o estoque **nunca fica negativo** (lock otimista).

## Build / Docker

```bash
./mvnw clean package
docker build -t mercury-shop:latest .   # multi-stage (runtime JRE 21, usuário não-root)
```

## Roadmap

Fase 1 ✅ Fundação · Fase 2 ✅ Usuários + Segurança · Fase 3 ✅ Pedidos (checkout/lock otimista/idempotência) ·
Fase 4 Assíncrono (RabbitMQ) + cache · Fase 5 Produção (observabilidade, Caddy/HTTPS, compose completo, CI/CD).
