# Hbm's Nuclear Tech Mod Modernized

## ENG Version 🇺🇸 | [RU Версия 🇷🇺](/README.ru.md)

**Status:** Pre-Alpha \
**Minecraft Version:** 1.20.1\
**Mod ID:** `hbm_m`

---

## ⚠️ Warning

> **This mod is currently in early alpha stage.**
> **Do NOT use it in worlds you care about!**
> There may be bugs, crashes, and potential incompatibilities with other mods.
> Please report any issues via the [GitHub Issues](../../issues) page.

-----

## About
This is a modernized rewrite of the classic Hbm's Nuclear Tech Mod, aiming to bring nuclear technology, radiation, and advanced weaponry to Minecraft 1.20.1+ with a fresh codebase and improved architecture. Work in progress!

![HBM_M Preview](docs/images/2025-07-21_04.46.43.png)

### 🟢 [Modrinth](https://modrinth.com/mod/hbms-nuclear-tech-modernized) | 🔨 [Curseforge](https://www.curseforge.com/minecraft/mc-mods/hbms-nuclear-tech-modernized)

## Dependency
[Cloth Config API v1.11.136](https://www.curseforge.com/minecraft/mc-mods/cloth-config/files?version=1.20)

![Cloth Config Required](https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSS8bhleCTOb7V1wJ3mq33rA1gjUdWOPxBbVxEcHzeMOCeh3PGC_DAWvep3eIWxsavaNFI&usqp=CAU)

**Note:**
I'm new to Minecraft modding, so please don't judge too harshly!
Constructive feedback and bug reports are very welcome.
All crafts are currently missing, not playable in survival mode.

-----

## Features (Alpha Preview)

  - **Custom Creative Tabs:**
      - Resources, Fuel, Templates, Ores, Machines, Bombs, Missiles, Weapons, Consumables. (Not all of them are filled, so there are gaps in the creative menu)

      ![Creative Tab](docs/images/2025-07-21_05.26.00.png)

  - **Items:**
      - **Advanced Alloy Sword:** A powerful custom sword.
      - **Uranium Ingot:** Radioactive material.
      - **Geiger Counter:** Detects environmental and inventory radiation, with sound and HUD overlay (WIP).
      - **Dosimeter:** A simpler device compared to the Geiger counter. It only shows the approximate environmental radiation.

  - **Blocks:**
    - **Uranium Block**
    - **Plutonium Block**
    - **Plutonium Fuel Block**
    - **Polonium-210 Block**
        - Emit radiation into the chunk
    - **Dead Grass and Foliage** - Part of the world destruction system

    ![Mod Blocks](docs/images/GIF_20250721_062357_193.gif)
    - **Armor Modification Table** - WIP. The block itself and a correct GUI are implemented.

    ![Armor Modification Table GUI](docs/images/2025-07-21_05.17.46.png)

    - **Uranium Ore** - Generates naturally in the world.

    ![Naturally generated Uranium Ore](docs/images/2025-07-21_05.20.20.png)


  - **Radiation System:**
      - Players accumulate radiation from the environment and inventory.
      - Radiation effects: blindness, confusion, weakness, hunger, poison, and even death at high doses.
      - Radiation is persistent and saved per player.
      - Chunk-based radiation system with spread and decay.
      - Radioactive blocks increase chunk radiation.
      - Debug overlay for chunk radiation (creative/spectator mode by default).
      - Mutation and block decay system - at high radiation doses in a chunk, foliage and grass blocks will be replaced with their dead counterparts.

      ![Radiation System](docs/images/2025-07-21_04.55.27.png)
      
      ![Radiation System (GIF)](docs/images/GIF_20250721_062913_819.gif)

  - **Sounds:**
      - Geiger counter clicks when there is radiation around.

  - **Commands:**
      - `/hbm_m rad` for manipulating player radiation (add, remove, clear).
      ![Player rad cleanup command](docs\images\20250721_480p_15f_20250721_064516.gif)

  - **Advancements**
    - **Hooray, Radiation!** Reach a radiation level of 200 RAD.
    - **Ouch, Radiation!** Die from radiation sickness.
      ![Advancements](docs\images\2025-07-21_06.49.19.png)

  - **Client Features:**
      - Debug chunk radiation renderer (toggleable).

      ![Chunk Radiation Renderer](docs/images/2025-07-21_04.56.36.png)
      - Deep Cloth Config API integration, so you can easily tweak core mod settings. (This is a required dependency for now, but I might make it optional in the future.)

      ![Cloth Config Screen](docs\images\2025-07-21_06.38.23.png)
-----

## Installation

1.  Download the latest version from [Releases](../../releases).
2.  Download the [Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config/files?version=1.20).
      - Make sure you download the version compatible with Minecraft 1.20.1.
3.  Place both `.jar` files into your Minecraft `mods` folder.
4.  Launch Minecraft with Forge 1.20.1.

-----

## Known Issues & Compatibility

  - **Alpha quality:** Expect bugs, missing features, and possible world corruption.
  - **Mod compatibility:** Not tested with other mods. Conflicts may occur.

If you encounter any problems, please report them on the [Issues](../../issues) page with details.

-----

## Contributing

Pull requests, suggestions, and bug reports are welcome!
If you want to help, feel free to fork the repository or submit pull requests.

-----

## Credits
  - Original Hbm's Nuclear Tech Mod created by **The Bobcat**.
  - The Forge team and Mojang for their work on Minecraft.
  - Modernization and rewrite by [Raptor324].

-----

## Final Note
Please be patient and understanding—I'm learning as I go!
Thank you for trying out the mod and helping with feedback.
