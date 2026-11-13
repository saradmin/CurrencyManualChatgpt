# Обновление проекта и сборка APK с телефона

Архив содержит готовое дерево исходников. Распаковывайте его **поверх существующего репозитория**, не удаляя скрытую папку `.git`.

В Termux:

```bash
cd ~/storage/shared/projects/CurrencyManualChatgpt
unzip -o ~/storage/shared/Download/CurrencyManual-source-for-git.zip -d .
git status --short
git add -A
git commit -m "Implement API rates, fallback and keypad fixes"
git push origin main
```

После `git push` откройте GitHub → репозиторий `saradmin/CurrencyManualChatgpt` → **Actions** → запуск `Build Android APK`. Workflow запускает unit-тесты и Android-сборку. Дождитесь зелёного статуса, откройте запуск и скачайте Artifact `CurrencyManual-debug-apk`. Распакуйте его и установите `app-debug.apk`.

Если workflow завершится ошибкой, не делайте повторные push вслепую. Пришлите лог упавшего шага, сначала найдём первопричину.


## Archive version

This archive is CurrencyManual 1.0.1 (Android versionCode 2). After pushing, check that the GitHub Actions artifact was produced from this commit. The installed app version is visible in Settings at the bottom; tap it for package and build details.
