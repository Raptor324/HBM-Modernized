# Hbm's Nuclear Tech Mod Modernized

## ENG Version 🇺🇸

**Status:** Pre-Alpha  
**Minecraft Version:** 1.20.1  
**Mod ID:** `hbm_m`

---

## ⚠️ Warning

> **This mod is currently in early alpha stage.**  
> **Do NOT use it in worlds you care about!**  
> There may be bugs, crashes, and potential incompatibilities with other mods.  
> Please report any issues via the [GitHub Issues](../../issues) page.

---

## About

This is a modernized rewrite of the classic Hbm's Nuclear Tech Mod, aiming to bring nuclear technology, radiation, and advanced weaponry to Minecraft 1.20.1+ with a fresh codebase and improved architecture.

## Dependency

[Cloth Config API v1.11.136](https://www.curseforge.com/minecraft/mc-mods/cloth-config/files?version=1.20)

**Note:**  
I'm new to Minecraft modding and Forge, so please don't judge too harshly!  
Constructive feedback and bug reports are very welcome. All crafts are currently missing, not playable in survival mode.

---

## Features (Alpha Preview)

- **Custom Creative Tabs:**  
  - Resources, Fuel, Templates, Ores, Machines, Bombs, Missiles, Weapons, Consumables. (Not all of them are filled, so there are gaps in the creative menu)

- **Items:**  
  - **Alloy Sword:** A powerful custom sword.
  - **Uranium Ingot:** Radioactive material.
  - **Geiger Counter:** Detects environmental and inventory radiation, with sound and HUD overlay (currently just a placeholder).

- **Blocks:**  
  - **Uranium Block:** Emits radiation into chunk.

- **Radiation System:**  
  - Players accumulate radiation from environment and inventory.
  - Radiation effects: blindness, confusion, weakness, hunger, poison, and even death at high doses.
  - Radiation is persistent and saved per player.
  - Chunk-based radiation system with spread and decay.
  - Radioactive blocks increase chunk radiation.
  - Debug overlay for chunk radiation (creative/spectator by default).

- **Sounds:**  
  - Geiger counter clicks when there is radiation around.

- **Commands:**  
  - `/hbm_m rad` for manipulating player radiation (add, remove, clear).

- **Client Features:**  
  - Debug chunk radiation renderer (toggleable).
  - Deep Cloth Config API integration, so you can easily tweak mod settings. (This is a required dependency for now, but I might make it optional in the future.)

---

## Installation

1. Download the latest release from [Releases](../../releases).
2. Download the [Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config/files?version=1.20).
   - Make sure to get the version compatible with Minecraft 1.20.1.
3. Place both `.jar` file into your Minecraft `mods` folder.
4. Launch Minecraft with Forge 1.20.1.

---

## Known Issues & Compatibility

- **Alpha quality:** Expect bugs, missing features, and possible world corruption.
- **Mod compatibility:** Not tested with other mods. There may be conflicts.

If you encounter any problems, please open an [issue](../../issues) with details.

---

## Contributing

Pull requests, suggestions, and bug reports are welcome!  
If you want to help, feel free to fork the repo and submit improvements.

---

## Credits

- Original Hbm's Nuclear Tech Mod made by Hbm.
- Forge and Mojang team for their work on Minecraft.
- Modernization and rewrite by [Raptor324].

---

## Final Note

Please be patient and understanding—I'm learning as I go!  
Thank you for trying out the mod and helping with feedback.

---


## RU Версия 🇷🇺

**Статус:** Пре-Альфа \
**Версия Minecraft:** 1.20.1\
**ID мода:** `hbm_m`

---

## ⚠️ Внимание

> **Этот мод в настоящее время находится на ранней стадии альфа-тестирования.**
> **НЕ используйте его в мирах, которыми вы дорожите\!**
> Возможны баги, вылеты и потенциальная несовместимость с другими модами.
> Пожалуйста, сообщайте о любых проблемах на странице [GitHub Issues](../../issues)

-----

## Описание
Это модернизированная переработка классического мода Hbm's Nuclear Tech Mod, призванная привнести ядерные технологии, радиацию и передовое вооружение в Minecraft 1.20.1+ с новой кодовой базой и улучшенной архитектурой. В процессе разработки!

## Зависимость
[Cloth Config API v1.11.136](https://www.curseforge.com/minecraft/mc-mods/cloth-config/files?version=1.20)

**Примечание:**
Я новичок в Minecraft моддинге, поэтому, пожалуйста, не судите слишком строго\!
Конструктивная обратная связь и отчеты об ошибках очень приветствуются.
Все крафты на данный момент отсутствуют, в режиме выживания не играбельно.

-----

## Возможности (Предварительная альфа-версия)

  - **Пользовательские творческие вкладки:**
      - Ресурсы, Топливо, Шаблоны, Руды, Механизмы, Бомбы, Ракеты, Оружие, Расходные материалы. (Не все из них заполнены, поэтому присутствуют провалы в меню креатива)

  - **Предметы:**
      - **Меч из продвинутого сплава:** Мощный пользовательский меч.
      - **Урановый слиток:** Радиоактивный материал.
      - **Счетчик Гейгера:** Обнаруживает радиацию в окружающей среде и инвентаре, со звуковым сопровождением и HUD-наложением (в настоящее время просто заглушка).

  - **Блоки:**
    - **Урановый блок:** Излучает радиацию в чанк.

  - **Система радиации:**
      - Игроки накапливают радиацию из окружающей среды и инвентаря.
      - Эффекты радиации: слепота, замешательство, слабость, голод, отравление и даже смерть при высоких дозах.
      - Радиация является постоянной и сохраняется для каждого игрока.
      - Система радиации с распространением по чанкам и распадом.
      - Радиоактивные блоки увеличивают радиацию в чанке.
      - Отладочное наложение для радиации в чанке (по умолчанию только в творческом режиме/режиме наблюдателя).

  - **Звуки:**
      - Щелчки счетчика Гейгера при наличии радиации вокруг.

  - **Команды:**
      - `/hbm_m rad` для управления радиацией игрока (добавить, удалить, очистить).

  - **Клиентские функции:**
      - Отладочный рендерер радиации чанков (переключаемый).
      - Глубокая интеграция с Cloth Config API, так что вы можете легко настраивать основные параметры мода. (В настоящее время это обязательная зависимость, но в будущем я, возможно, сделаю ее опциональной.)

-----

## Установка

1.  Загрузите последнюю версию из [Releases](../../releases).
2.  Загрузите [Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config/files?version=1.20).
      - Убедитесь, что вы скачали версию, совместимую с Minecraft 1.20.1.
3.  Поместите оба `.jar` файла в папку `mods` вашего Minecraft.
4.  Запустите Minecraft с Forge 1.20.1.

-----

## Известные проблемы и совместимость

  - **Альфа-версия:** Ожидайте баги, отсутствующие функции и возможную порчу вашего мира.
  - **Совместимость модов:** Не тестировалось с другими модами. Возможны конфликты.

Если вы столкнетесь с какими-либо проблемами, пожалуйста, сообщите в [Issues](../../issues) с подробностями.

-----

## Участие в разработке

Запросы на слияние, предложения и отчеты об ошибках приветствуются\!
Если вы хотите помочь, не стесняйтесь форкать репозиторий или предлагать пулл-реквесты.

-----

## Благодарности
  - Оригинальный Hbm's Nuclear Tech Mod созданный HBM.
  - Команде Forge и Mojang за их работу над Minecraft.
  - Модернизация и переработка [Raptor324].

-----

## Заключительное примечание
Пожалуйста, будьте терпеливы и понимаючи — я учусь по ходу дела\!
Спасибо, что попробовали мод и помогли с обратной связью.

-----