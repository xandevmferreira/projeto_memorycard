# seguranca

> Medidas aplicadas no código (2025).

## Correções implementadas

- `/uploads/archives` e `/uploads/screenshots` exigem **login**
- Apenas `/uploads/covers` é público
- **CSRF** em formulários web (API `/api/**` isenta para o sync agent)
- **Rate limit** em login e registro
- Cookie JWT **secure + SameSite Strict** em produção
- **HSTS** e headers de segurança em produção
- Validação na subida: produção **não inicia** com JWT ou senha DB padrão

## Variáveis obrigatórias em produção

- `JWT_SECRET` (32+ caracteres)
- `POSTGRES_PASSWORD` (forte, não `memorycard`)
- `SPRING_PROFILES_ACTIVE=prod`

## Pendente / futuro

- Backup automático agendado no servidor
- WAF Cloudflare (regras extras)
- Scan de vírus em uploads (opcional)
