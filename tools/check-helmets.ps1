$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskCommands = Join-Path $taskRoot 'run-server/verification-commands.txt'
$taskRequest = Join-Path $taskRoot 'run-verify-viewer/verification-shot.txt'
foreach ($taskHelmet in @('leather_helmet','chainmail_helmet','iron_helmet','golden_helmet','diamond_helmet','netherite_helmet','turtle_helmet')) {
    Set-Content -LiteralPath $taskCommands -Value "item replace entity VerifySender armor.head with minecraft:$taskHelmet"
    for ($taskWait=0; $taskWait -lt 40 -and (Test-Path -LiteralPath $taskCommands); $taskWait++) { Start-Sleep -Milliseconds 100 }
    Start-Sleep -Milliseconds 700
    Set-Content -LiteralPath $taskRequest -Value "vanilla-$taskHelmet"
    $taskImage = Join-Path $taskRoot "run-verify-viewer/screenshots/facecam-vanilla-$taskHelmet.png"
    for ($taskWait=0; $taskWait -lt 40 -and -not (Test-Path -LiteralPath $taskImage); $taskWait++) { Start-Sleep -Milliseconds 100 }
    Write-Output "$taskHelmet screenshot=$(Test-Path -LiteralPath $taskImage)"
}
Set-Content -LiteralPath $taskCommands -Value 'item replace entity VerifySender armor.head with minecraft:air'
