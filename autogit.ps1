# Скрипт для автоматического коммита и пуша всех изменений в репозиторий
# Запускать из PowerShell в корне проекта

# Добавить все изменения
 git add -A

# Сформировать сообщение с датой и временем
$commitMsg = "Auto-commit: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

# Сделать коммит (если есть изменения)
git diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
    git commit -m "$commitMsg"
    git push
} else {
    Write-Host "Нет изменений для коммита."
}
