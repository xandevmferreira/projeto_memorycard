# Encerra qualquer processo usando a porta 8080 (instância antiga do MemoryCard).
$lines = netstat -ano | Select-String ":8080"
$killed = $false
foreach ($line in $lines) {
    if ($line -match '\s+(\d+)\s*$') {
        $procId = [int]$Matches[1]
        if ($procId -gt 0) {
            Write-Host "Encerrando PID $procId na porta 8080..."
            taskkill /PID $procId /F 2>$null | Out-Null
            $killed = $true
        }
    }
}
if (-not $killed) {
    Write-Host "Porta 8080 livre."
}
