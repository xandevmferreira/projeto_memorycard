# MemoryCard Sync Agent

Programa desktop **opcional** para Windows/Mac/Linux. Roda **apenas no PC do usuário** e envia saves/ROMs da pasta do emulador para **a própria conta** no MemoryCard (site na nuvem).

- Não acessa outros computadores
- Autenticação por **token de sync** (gerado em Perfil no site) — sem senha
- Ícone na **bandeja do sistema** — fecha a janela e continua em segundo plano
- Instalador **.exe** para Windows via `jpackage`

## Compilar

Requer Java 21+:

```powershell
powershell -ExecutionPolicy Bypass -File ..\scripts\build-sync-agent.ps1
```

Com Maven (alternativa):

```bash
cd sync-agent && mvn package
```

## Executar

```powershell
powershell -ExecutionPolicy Bypass -File ..\scripts\run-sync-agent.ps1
```

Ou:

```bash
java -jar target/memorycard-sync-agent-0.0.11.jar
```

## Instalador Windows (.exe)

Requer JDK com `jpackage`:

```powershell
powershell -ExecutionPolicy Bypass -File ..\scripts\package-sync-agent.ps1
```

- **Com WiX** instalado: gera `sync-agent/target/installer/MemoryCard Sync-0.0.11.exe`
- **Sem WiX** (padrão): gera app em `sync-agent/target/installer/MemoryCard Sync/` + script:

```powershell
powershell -ExecutionPolicy Bypass -File "sync-agent\target\installer\instalar-memorycard-sync.ps1"
```

Isso copia para `%LOCALAPPDATA%\MemoryCard Sync` e cria atalhos na área de trabalho e no Menu Iniciar.

## Uso

1. No site: **Perfil → MemoryCard Sync → Gerar token** (copie o `mc_sync_...`)
2. Abra o app, informe a URL do site e cole o token
3. **Conectar** — carrega seus jogos
4. Escolha o jogo e a pasta de saves do emulador
5. **Iniciar sync** — minimize para a bandeja enquanto joga

Os arquivos aparecem no site em **Fitas digitais → Sync automático — data**.

Configuração salva em `%USERPROFILE%\.memorycard\sync-agent.properties` (inclui token local).

## Bandeja do sistema

- Fechar a janela (X) ou minimizar → app continua na bandeja
- Clique no ícone ou menu **Abrir** para restaurar
- Menu: Iniciar sync / Parar sync / Sair
