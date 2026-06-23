# MemoryCard

Plataforma web de catálogo pessoal de jogos. Permite criar conta, fazer login, registrar jogos com status, notas, horas jogadas, capas e screenshots — tudo isolado por usuário.

## Stack

- **Java 21** + **Spring Boot 3.3**
- **PostgreSQL** com migrations **Flyway**
- **Spring Security** + autenticação **JWT** (cookie HttpOnly para web + Bearer token para API)
- **Spring Data JPA** / Hibernate
- **Thymeleaf** (frontend inicial)
- Upload local com abstração para futura migração S3/R2
- Service preparado para API externa de jogos (**RAWG**)

## Pré-requisitos

- Java 21
- Maven 3.9+
- Docker (opcional, para PostgreSQL)

## Como rodar

### 1. Subir o banco de dados

```bash
docker compose up -d
```

Isso cria um PostgreSQL em `localhost:5432` com:

| Campo    | Valor        |
|----------|--------------|
| Database | memorycard   |
| User     | memorycard   |
| Password | memorycard   |

### 2. Configurar variáveis (opcional)

As configurações padrão estão em `src/main/resources/application.yml`. Para produção, defina:

```bash
# Windows PowerShell
$env:JWT_SECRET = "sua-chave-secreta-longa-e-aleatoria"
$env:RAWG_API_KEY = "sua-chave-rawg"   # opcional
$env:RAWG_ENABLED = "true"              # opcional
```

### 3. Executar a aplicação

```bash
mvn spring-boot:run
```

Acesse: **http://localhost:8080**

- `/register` — criar conta
- `/login` — entrar
- `/dashboard` — painel privado
- `/games` — listar jogos
- `/games/new` — cadastrar jogo

## API REST

Todos os endpoints (exceto auth) exigem autenticação via cookie JWT ou header `Authorization: Bearer <token>`.

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/register` | Cadastro |
| POST | `/api/auth/login` | Login |
| POST | `/api/auth/logout` | Logout |
| GET | `/api/dashboard` | Dados do dashboard |
| GET | `/api/games` | Listar jogos |
| GET | `/api/games/{id}` | Detalhe do jogo |
| POST | `/api/games` | Criar jogo (multipart) |
| PUT | `/api/games/{id}` | Atualizar jogo (multipart) |
| DELETE | `/api/games/{id}` | Excluir jogo |
| POST | `/api/games/{id}/screenshots` | Upload de screenshot |
| DELETE | `/api/games/{gameId}/screenshots/{screenshotId}` | Remover screenshot |
| GET | `/api/external-games/search?query=zelda` | Busca na API RAWG |

### Exemplo de cadastro via API

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Alex","email":"alex@email.com","password":"123456"}'
```

## Arquitetura

```
com.memorycard
├── config/          # Propriedades e configurações
├── controller/
│   ├── api/         # REST controllers
│   └── web/         # MVC controllers (Thymeleaf)
├── dto/
│   ├── request/
│   └── response/
├── entity/          # User, Game, GameScreenshot
├── exception/
├── repository/
├── security/        # JWT, filtros, SecurityConfig
├── service/         # Regras de negócio
└── storage/         # LocalStorageService (+ stub S3)
```

## Entidades

### User
`id`, `name`, `email`, `passwordHash`, `subscriptionStatus` (FREE/ACTIVE/EXPIRED/CANCELLED), `createdAt`

### Game
`id`, `userId`, `title`, `platform`, `status`, `hoursPlayed`, `personalRating`, `externalRating`, `notes`, `coverUrl`, `startedAt`, `completedAt`, `createdAt`

### GameScreenshot
`id`, `gameId`, `filePath`, `uploadedAt`

## Status dos jogos

| Valor | Significado |
|-------|-------------|
| PLAYING | Jogando |
| COMPLETED | Zerado |
| ABANDONED | Abandonado |
| PAUSED | Pausado |

## Uploads

Arquivos são salvos em `./uploads/` por padrão. Para mudar:

```yaml
app:
  storage:
    type: local
    local:
      base-path: ./uploads
```

Futuramente, altere `app.storage.type` para `s3` e implemente `S3StorageService`.

## Integração RAWG

1. Obtenha uma API key em [rawg.io/apidocs](https://rawg.io/apidocs)
2. Configure:

```yaml
app:
  rawg:
    api-key: sua-chave
    enabled: true
```

Ao cadastrar um jogo, o sistema tenta preencher automaticamente capa, plataforma e nota externa.

## Roadmap (futuro)

- [ ] Assinatura paga (Stripe / Mercado Pago) com bloqueio por `subscriptionStatus`
- [ ] Upload de arquivos de save
- [ ] Storage em S3 / Cloudflare R2
- [ ] Ranking real de jogos populares via API externa
- [ ] Frontend React (migração gradual da API REST)

## Build

```bash
mvn clean package
java -jar target/memorycard-0.1.0.jar
```

## Testes

```bash
mvn test
```

## Git e trabalho em vários computadores

### Primeira vez (subir para o GitHub)

1. Crie um repositório vazio em [github.com/new](https://github.com/new) (sem README, sem .gitignore).
2. No projeto, conecte e envie:

```bash
git remote add origin https://github.com/SEU_USUARIO/memorycard.git
git branch -M main
git push -u origin main
```

### Trabalhar em casa (outra branch)

```bash
git clone https://github.com/SEU_USUARIO/memorycard.git
cd memorycard
git checkout -b casa/desenvolvimento
# ... edite o código ...
git add .
git commit -m "Descrição do que você fez"
git push -u origin casa/desenvolvimento
```

### Voltar no outro PC e trazer as mudanças

```bash
git fetch origin
git checkout casa/desenvolvimento
git pull
```

### Integrar na branch principal (quando estiver pronto)

No GitHub: abra um **Pull Request** de `casa/desenvolvimento` → `main`, revise e faça merge.

Ou pelo terminal (na branch `main`):

```bash
git checkout main
git pull
git merge casa/desenvolvimento
git push
```

### Boas práticas

- Nunca commite `.env`, `uploads/` ou `target/` (já estão no `.gitignore`).
- Use branches por tarefa: `casa/zerar-jogo`, `feature/dashboard`, etc.
- Antes de começar a codar: `git pull` para pegar o que mudou no remoto.
- Cada máquina precisa de **Java 21**, **PostgreSQL** (ou Docker) e as mesmas configs do `.env.example`.
