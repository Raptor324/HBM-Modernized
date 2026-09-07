![HBM M Banner](docs/images/20251013_170609.png)

***

## RU Версия 🇷🇺 | [ENG Version 🇺🇸](/README.md)

**Статус:** Пре-Альфа \
**Версия Minecraft:** 1.20.1 \
**Mod ID:** `hbm_m` \
**Лицензия:** GPL-3.0-only (с сохранением LGPL-3.0 для материала оригинала 1.7.10 - см. [Лицензия](#-лицензия))

***

## 📥 Официальные площадки

<div align="center">

# <img src="https://cdn-icons-png.flaticon.com/128/5968/5968756.png" height=28 /> <a href="https://discord.gg/f2BhvzG6CS">Discord</a> | <img src="https://cdn2.steamgriddb.com/icon/46bbc4a56de136ad319e59e37ef55644/32/256x256.png" height=30 /> <a href="https://modrinth.com/mod/hbms-nuclear-tech-modernized">Modrinth</a> | <img src="https://cdn2.steamgriddb.com/logo/946b656620286beea9d58a29d1587d10.png" height=23 /> <a href="https://www.curseforge.com/minecraft/mc-mods/hbms-nuclear-tech-modernized">CurseForge</a> | <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/VK_Compact_Logo_%282021-present%29.svg/1200px-VK_Compact_Logo_%282021-present%29.svg.png" height=23 /> <a href="https://vk.com/hbm_modernized">VK</a>
</div>

> [!WARNING]
> **Мод находится на стадии пре-альфа.**
> **НЕ используйте его в важных для вас мирах!**
> Возможны баги, краши и несовместимость с другими модами.
> Сообщайте о проблемах в [GitHub Issues](../../issues)

***

## О моде

HBM-Modernized - современная переработка [Hbm's Nuclear Tech Mod](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT) для Minecraft 1.20.1: ядерные технологии, радиация, передовое вооружение и промышленная автоматизация на переписанной кодовой базе и современной архитектуре рендеринга.

Проект является независимым портом и не является официальным релизом HBM/NTM.

***

## Исходники и авторство

Этот проект является изменённой и модернизированной работой, созданной на основе **[Hbm's Nuclear Tech Mod](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT)** для Minecraft 1.7.10. **The Bobcat и оригинальные контрибьюторы HBM/NTM сохраняют заслуги и копирайт** на исходный код, ассеты, игровой дизайн и документацию.

Вся работа по модернизации - кодовая база 1.20.1, система сборки, архитектура рендеринга и кросс-версионный платформенный слой - выполнена командой HBM-Modernized (список контрибьюторов см. в [gradle.properties](gradle.properties)).

Подробное указание авторства - в [NOTICE.md](NOTICE.md).

***

## 📄 Лицензия

Проект распространяется под лицензией **[GPL-3.0-only](LICENSE)**.

Лицензирование по происхождению кода:

| Область | Лицензия |
|---|---|
| Комбинированная работа: исходники и JAR HBM-Modernized | GPL-3.0-only |
| Материал, перенесённый из HBM/NTM 1.7.10 (код, ассеты, механики, документация) | LGPL-3.0-only - исходный нотис сохранён ([LICENSE.LESSER](LICENSE.LESSER)) внутри комбинированной GPLv3-работы |
| Собственные доработки и изменения HBM-Modernized | GPL-3.0-only |
| Зависимости (Minecraft, Forge/NeoForge, Architectury и др.) | Их собственные лицензии |

Примечания:

- Применение GPLv3 к комбинированной работе не отменяет копирайт оригинала и LGPL-3.0-нотис на перенесённые части.
- Поле `license` в `mods.toml` указано как `GPL-3.0-only` - это эффективная лицензия распространяемой комбинированной работы. Составное выражение используется намеренно не: `GPL AND LGPL` неверно утверждало бы, что каждая часть находится под обеими лицензиями одновременно, а `GPL OR LGPL` неверно предлагало бы LGPL-вариант для нашего собственного GPLv3-материала.
- `LICENSE`, `LICENSE.LESSER` и `NOTICE.md` включаются в релизные JAR-файлы в `META-INF/`.
- Распространяя скомпилированный JAR, вы обязаны предоставить полный соответствующий исходный код - включая скрипты сборки - способом, совместимым с GPLv3. Канонический репозиторий исходников - этот.

***

## 🏗️ Сборка

Проект использует единый source set с [stonecutter](https://stonecutter.kikoz.dev/)-препроцессингом для поддержки нескольких версий. Активные таргеты: `1.20.1-forge` и `1.21.1-neoforge`.

```bash
./gradlew "Set active project to 1.21.1-neoforge"   # переключить активный проект stonecutter
./gradlew "Reset active project"   # сбросить активный проект stonecutter на 1.20.1-forge - по умолчанию. Запускать всегда перед любым коммитом.
./gradlew :1.20.1-forge:build      # собрать JAR
./gradlew :1.20.1-forge:runClient  # запустить дев-клиент
./gradlew :1.20.1-forge:runData    # запустить датаген (переводы, блокстейты и тд)
```

Аналогично для `1.21.1-neoforge`. Единственный авторитет сборки - Gradle-компилятор: препроцессор активно трансформирует исходники под активный таргет.

***

## 📦 Установка

1. Установите **Forge 1.20.1**
2. Скачайте последнюю версию со страницы [Releases](../../releases)
3. Положите `.jar` в папку `mods`

***

## 🤝 Участие в разработке

Pull request'ы, предложения и сообщения об ошибках приветствуются. Форкайте репозиторий и предлагайте улучшения, либо сообщайте о проблемах в [Issues](../../issues) с подробным описанием.

***

## 💝 Благодарности

**The Bobcat** - автор оригинального HBM's Nuclear Tech Mod

**RaptorDev / Raptor324** и прочим контрибьюторам - модернизация и переработка

Команды Forge и Mojang за инструменты разработки
