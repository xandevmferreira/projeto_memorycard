# Colocar o MemoryCard no ar (guia simples)

Escolhemos **um servidor VPS barato + Docker**. Você paga **um serviço** (~€4–5/mês) e opcionalmente um **domínio**. HTTPS e banco já vêm no pacote.

## O que pagar

| Item | Onde | Preço aprox. |
|------|------|----------------|
| **Servidor VPS** | [Hetzner Cloud](https://www.hetzner.com/cloud) — plano **CX22** (2 GB RAM) | ~€4,5/mês (~R$ 28) |
| **Domínio** (opcional mas recomendado) | Registro.br, Cloudflare Registrar, etc. | ~R$ 40/ano |
| **Cloudflare** | DNS apontando pro servidor | **Grátis** |
| **HTTPS** | Caddy no Docker (Let's Encrypt) | **Grátis** |

**Total para começar:** ~R$ 30/mês + domínio.

Alternativa mais simples (um pouco mais cara): [Railway](https://railway.app) ou [Render](https://render.com) — menos configuração, ~US$ 10–15/mês.

---

## Passo a passo (Hetzner + Docker)

### 1. Criar o servidor

1. Conta em **Hetzner Cloud** → Create Server  
2. **Ubuntu 24.04** · tipo **CX22** · região perto de você (Falkenstein ou Helsinki)  
3. Adicione sua **chave SSH** (ou anote a senha root)  
4. Anote o **IP público** (ex: `123.45.67.89`)

### 2. Apontar o domínio (Cloudflare grátis)

1. Crie site no Cloudflare e coloque o domínio  
2. Registro **A**: `@` → IP do servidor  
3. Registro **A**: `www` → IP do servidor  
4. Proxy laranja **ligado** (proteção DDoS grátis)

### 3. No seu PC — preparar o projeto

```powershell
cd projeto_memorycard-main
powershell -File scripts\build.ps1
powershell -File scripts\generate-deploy-env.ps1
```

Isso cria o arquivo `.env` com senhas fortes. Edite `DOMAIN` e `ACME_EMAIL` se precisar.

### 4. Enviar pro servidor

No PowerShell (troque IP e usuário):

```powershell
scp -r . root@SEU_IP:/opt/memorycard
```

Ou use Git no servidor: `git clone` do seu repositório.

### 5. No servidor — instalar Docker (uma vez)

Conecte por SSH:

```bash
ssh root@SEU_IP
curl -fsSL https://get.docker.com | sh
apt install -y docker-compose-plugin
```

### 6. Subir o site

```bash
cd /opt/memorycard
# Se não tiver .env, copie: cp deploy/.env.example .env && nano .env

docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d
```

Aguarde 1–2 minutos. Acesse `https://seudominio.com.br`.

### 7. Atualizar depois

No servidor:

```bash
cd /opt/memorycard
git pull   # ou envie arquivos de novo
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d
```

---

## O que já está configurado no código

- **Fitas e saves privados** — só com login (não ficam públicos em `/uploads`)
- **Capas** — públicas (normal para listagens)
- **HTTPS** — cookie seguro em produção
- **CSRF** — proteção em formulários do site
- **Rate limit** — login/registro limitados (anti brute-force)
- **Senhas** — BCrypt
- **Produção** — recusa subir se `JWT_SECRET` ou senha do banco forem fracas

---

## Comandos úteis no servidor

```bash
docker compose -f docker-compose.prod.yml logs -f app   # ver logs
docker compose -f docker-compose.prod.yml ps          # status
docker compose -f docker-compose.prod.yml restart app # reiniciar app
```

Backup do banco (manual):

```bash
docker exec memorycard-db pg_dump -U memorycard memorycard > backup.sql
```

---

## Desenvolvimento local (sem mudanças)

Continue usando `scripts\run.ps1` e `docker-compose.yml` só para o Postgres local. O perfil `prod` só roda no servidor.

---

## Suporte

Se o site não abrir:

1. DNS propagou? (`ping seudominio.com.br`)  
2. Portas 80 e 443 abertas no firewall Hetzner?  
3. `docker compose -f docker-compose.prod.yml logs caddy`  
4. `docker compose -f docker-compose.prod.yml logs app`
