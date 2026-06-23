# hospedagem

> Escolha e passos para colocar no ar — resumo. Detalhes em [DEPLOY.md](./DEPLOY.md).

## O que pagar

- **Hetzner CX22** (~€4,5/mês) — servidor
- **Domínio** (~R$ 40/ano) — opcional mas recomendado
- **Cloudflare** — grátis (DNS + proteção)

## Arquivos no projeto

| Arquivo | Função |
|---------|--------|
| `docker-compose.prod.yml` | App + Postgres + Caddy (HTTPS) |
| `Dockerfile` | Imagem do site |
| `deploy/Caddyfile` | HTTPS automático |
| `deploy/.env.example` | Modelo de variáveis |
| `scripts/generate-deploy-env.ps1` | Gera senhas do `.env` |
| `docs/DEPLOY.md` | Tutorial passo a passo |

## Comando rápido (no servidor)

```bash
docker compose -f docker-compose.prod.yml up -d --build
```
