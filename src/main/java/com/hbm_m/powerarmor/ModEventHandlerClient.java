package com.hbm_m.powerarmor;

import java.util.HashSet;
import java.util.Set;

// Импорты HBM Modernized
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
// Основные импорты Forge
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderItemInFrameEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Клиентский обработчик событий для HBM's Nuclear Tech Modernized
 * Портировано с 1.7.10 на 1.20.1
 *
 * Реализует различные клиентские функции:
 * - HUD элементы и оверлеи
 * - Визуальные эффекты
 * - Обработка ввода
 * - Система подсказок
 * - Звуковые эффекты
 * - Рендеринг специальных эффектов
 */
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ModEventHandlerClient {

    static {
        // Инициализация при загрузке мода
    }
    public static final int FLASH_DURATION = 5_000;
    public static long flashTimestamp;
    public static final int SHAKE_DURATION = 1_500;
    public static long shakeTimestamp;

    // Статические поля для оверлеев
    public static boolean ducked = false;

    // Иконки частиц
    public static TextureAtlasSprite particleBase;
    public static TextureAtlasSprite particleLeaf;
    public static TextureAtlasSprite particleSplash;
    public static TextureAtlasSprite particleAshes;

    // Состояние VATS
    private static boolean vatsActive = false;
    private static long vatsActivatedTime = 0;

    // Состояние тепловизора
    private static boolean thermalActive = false;

    // Состояние spectral fallback (подсветка мобов и ночное зрение)
    private static final Set<Integer> spectralHighlighted = new HashSet<>();
    private static final Set<Integer> spectralHighlightedThisTick = new HashSet<>();
    private static boolean spectralNightVisionApplied = false;

    // Система сплешей
    private static String modSplashText; // null = ванилла
    private static net.minecraft.network.chat.Component currentSplash = null;
    private static int lastTitleScreenId = 0;
    // baseline = то, что было у TitleScreen после Init.Post (ванилла или другой мод, который успел раньше)
    private static SplashRenderer baselineSplash = null;

    // our cache
    private static SplashRenderer hbmmSplashRenderer = null;
    private static String hbmmSplashTextCached = null;

    // cached reflection
    private static java.lang.reflect.Field splashField = null;
    private static boolean splashFieldResolved = false;

    // настройка совместимости: true = не перетирать чужие сплеши, если они отличаются от baseline
    private static final boolean COOPERATIVE_SPLASH = true;

    // Кэши для оптимизации производительности
    private static long lastPerformanceCheck = 0;
    private static final long PERFORMANCE_CHECK_INTERVAL = 1000; // Проверка каждую секунду
    private static boolean performanceWarningsLogged = false;

    /**
     * Оверлей для ядерной вспышки
     * Рисуется поверх прицела для максимальной видимости
     * Оптимизировано - проверка видимости перед рендерингом
     */
    public static final IGuiOverlay NUCLEAR_FLASH_OVERLAY = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        // Быстрая проверка - закончилась ли вспышка
        long currentTime = System.currentTimeMillis();
        if (currentTime - flashTimestamp >= FLASH_DURATION) {
            return; // Вспышка закончилась, ничего не рендерим
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        // Дополнительная проверка игрока
        if (player == null) {
            return;
        }

        // Отключаем текстуры и включаем blending
        RenderSystem.setShaderTexture(0, 0); // Отключаем текстуры
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        // Создаем буфер для рендеринга
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Вычисляем яркость вспышки (от 1.0 до 0.0)
        // Оптимизировано - используем уже рассчитанное время
        float brightness = (flashTimestamp + FLASH_DURATION - currentTime) / (float) FLASH_DURATION;
        brightness = Mth.clamp(brightness, 0.0F, 1.0F);

        // Если яркость слишком низкая, не рендерим
        if (brightness <= 0.01F) {
            return;
        }

        // Рисуем белый прямоугольник поверх всего экрана
        buffer.vertex(0, screenHeight, 0).color(1.0F, 1.0F, 1.0F, brightness).endVertex();
        buffer.vertex(screenWidth, screenHeight, 0).color(1.0F, 1.0F, 1.0F, brightness).endVertex();
        buffer.vertex(screenWidth, 0, 0).color(1.0F, 1.0F, 1.0F, brightness).endVertex();
        buffer.vertex(0, 0, 0).color(1.0F, 1.0F, 1.0F, brightness).endVertex();

        tesselator.end();

        // Восстанавливаем состояния OpenGL
        RenderSystem.disableBlend();
    };

    /**
     * Оверлей тепловизора для силовой брони
     */
    public static final IGuiOverlay THERMAL_OVERLAY = ModEventHandlerClient::onRenderThermalOverlay;

    // TODO: Реализовать остальные поля и методы

    /**
     * Регистрация GUI оверлеев
     */
    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        // Регистрируем оверлей ядерной вспышки над всеми остальными элементами
        event.registerAboveAll("nuclear_flash", NUCLEAR_FLASH_OVERLAY);

        MainRegistry.LOGGER.info("Registered HBM nuclear flash overlay.");
        // Примечание: Thermal overlay регистрируется в ClientSetup.java
    }

    /**
     * Обработка клиентского тика
     * Оптимизировано для производительности - тяжелые проверки выполняются периодически
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (event.phase == TickEvent.Phase.START) {

            // Периодическая проверка производительности (раз в секунду)
            if (currentTime - lastPerformanceCheck > PERFORMANCE_CHECK_INTERVAL) {
                lastPerformanceCheck = currentTime;
                checkPerformanceWarnings(mc);
            }

            // TODO: Реализовать обработку пепельного шторма (BlockAshes.ashes)
            // TODO: Реализовать обновление яркости (currentBrightness/lastBrightness)
            // TODO: Реализовать проверку маски (ArmorUtil.isWearingEmptyMask)
        }

        if (event.phase == TickEvent.Phase.END) {
            // Обработка ввода делается в ModConfigKeybindHandler.java

            // Автоматическая деактивация VATS и тепловизора, если броня их не поддерживает
            if (mc.player != null) {
                if (vatsActive && ModPowerArmorItem.hasFSBArmor(mc.player)) {
                    var chestplate = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                    if (chestplate.getItem() instanceof ModPowerArmorItem armorItem) {
                        if (!armorItem.getSpecs().hasVats) {
                            vatsActive = false;
                        }
                    } else {
                        vatsActive = false;
                    }
                } else if (vatsActive && !ModPowerArmorItem.hasFSBArmor(mc.player)) {
                    vatsActive = false;
                }

                if (thermalActive && ModPowerArmorItem.hasFSBArmor(mc.player)) {
                    var chestplate = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                    if (chestplate.getItem() instanceof ModPowerArmorItem armorItem) {
                        if (!armorItem.getSpecs().hasThermal) {
                            thermalActive = false;
                        }
                    } else {
                        thermalActive = false;
                    }
                } else if (thermalActive && !ModPowerArmorItem.hasFSBArmor(mc.player)) {
                    thermalActive = false;
                }
            }

            handleThermalSpectralFallback(mc);

            // TODO: Реализовать обработку высоты шага для FSB брони
            // TODO: Реализовать отдачу оружия
        }
    }


    /**
     * Проверка производительности и вывод предупреждений
     * Выполняется периодически для избежания спама в логах
     */
    private static void checkPerformanceWarnings(Minecraft mc) {
        // TODO: Добавить проверки производительности
        // - Количество активных частиц
        // - Количество рендерящихся сущностей
        // - Использование памяти
        // - FPS и другие метрики

        // Пока просто логгируем что проверка работает
        // MainRegistry.LOGGER.debug("Performance check completed");
    }

    /**
     * Обработка вспомогательных эффектов тепловизора:
     * - в режиме ORIGINAL_FALLBACK даём игроку ночное зрение и подсвечиваем всех живых существ белым контуром;
     * - в режиме FULL_SHADER не трогаем эффекты, вся яркость управляется шейдером и миксинами.
     */
    private static void handleThermalSpectralFallback(Minecraft mc) {
        // Если тепловизор не активен – чистим все эффекты и выходим
        if (!thermalActive) {
            clearSpectralHighlights(mc);
            clearSpectralNightVision(mc);
            return;
        }

        if (mc.level == null || mc.player == null) {
            clearSpectralHighlights(mc);
            clearSpectralNightVision(mc);
            return;
        }

        // Во всех режимах, кроме ORIGINAL_FALLBACK, вспомогательные эффекты не нужны.
        if (ModClothConfig.get().thermalRenderMode != ModClothConfig.ThermalRenderMode.ORIGINAL_FALLBACK) {
            clearSpectralHighlights(mc);
            clearSpectralNightVision(mc);
            return;
        }

        LocalPlayer player = mc.player;

        // Фолбек-режим: выдаём длительный бафф ночного зрения, а при выключении тепловизора
        // снимаем его вручную. Так избегаем ванильного "мигания" эффекта на низких таймерах.
        MobEffectInstance currentNv = player.getEffect(MobEffects.NIGHT_VISION);
        if (currentNv == null) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0, false, false, false));
            spectralNightVisionApplied = true;
        }

        // Подсветка всех живых существ (кроме самого игрока), только при прямой видимости.
        // Используем стандартный ванильный эффект GLOWING + client-only флаг glowingTag.
        // Локально каждый тик продлеваем очень короткий невидимый эффект, а при потере
        // прямой видимости сразу его снимаем, чтобы не было X‑ray сквозь стены.
        spectralHighlightedThisTick.clear();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && entity != player) {
                int id = entity.getId();

                // Проверяем, виден ли энтити с глаз игрока.
                if (canSeeEntity(player, entity)) {
                    // Короткий, бесчастичный эффект подсветки как у спектральной стрелы.
                    MobEffectInstance glow = living.getEffect(MobEffects.GLOWING);
                    if (glow == null || glow.getDuration() <= 10 || !glow.isVisible()) {
                        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20, 0, false, false, false));
                    }

                    living.setGlowingTag(true);
                    spectralHighlighted.add(id);
                    spectralHighlightedThisTick.add(id);
                } else if (spectralHighlighted.contains(id)) {
                    // Если сущность больше не видна (закрыта блоками) — снимаем локальный glow.
                    living.setGlowingTag(false);
                    MobEffectInstance glow = living.getEffect(MobEffects.GLOWING);
                    if (glow != null && !glow.isVisible()) {
                        living.removeEffect(MobEffects.GLOWING);
                    }
                }
            }
        }

        // Снимаем подсветку с тех, кого больше нет в текущем кадре
        if (!spectralHighlighted.isEmpty()) {
            java.util.Iterator<Integer> it = spectralHighlighted.iterator();
            while (it.hasNext()) {
                int id = it.next();
                if (!spectralHighlightedThisTick.contains(id)) {
                    Entity entity = mc.level.getEntity(id);
                    if (entity instanceof LivingEntity living) {
                        living.setGlowingTag(false);
                    }
                    it.remove();
                }
            }
        }
    }

    private static void clearSpectralHighlights(Minecraft mc) {
        if (mc.level == null) {
            spectralHighlighted.clear();
            spectralHighlightedThisTick.clear();
            return;
        }

        for (Integer id : spectralHighlighted) {
            Entity entity = mc.level.getEntity(id);
            if (entity instanceof LivingEntity living) {
                living.setGlowingTag(false);
                MobEffectInstance glow = living.getEffect(MobEffects.GLOWING);
                if (glow != null && !glow.isVisible()) {
                    living.removeEffect(MobEffects.GLOWING);
                }
            }
        }

        spectralHighlighted.clear();
        spectralHighlightedThisTick.clear();
    }

    private static void clearSpectralNightVision(Minecraft mc) {
        if (!spectralNightVisionApplied) {
            return;
        }

        LocalPlayer player = mc.player;
        if (player != null) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }

        spectralNightVisionApplied = false;
    }

    /**
     * Проверка прямой видимости сущности с глаз игрока.
     * Используется только в клиентском ORIGINAL_FALLBACK‑режиме тепловизора,
     * чтобы локальный glow не работал как X‑ray сквозь стены.
     */
    private static boolean canSeeEntity(LocalPlayer player, Entity entity) {
        if (player == null || entity == null) {
            return false;
        }
        // Используем готовую проверку линии обзора из Minecraft,
        // чтобы поведение совпадало с ванильной логикой видимости.
        return player.hasLineOfSight(entity);
    }

    /**
     * Обработка подсказок предметов
     * Дополняет HazardTooltipHandler дополнительными функциями
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (stack.isEmpty()) {
            return;
        }

        // TODO: Добавить сопротивление урону (DamageResistanceHandler.addInfo)
        // TODO: Добавить информацию о радиационной защите (HazmatRegistry)
        // TODO: Добавить информацию о модификациях брони (ArmorModHandler)
        // TODO: Добавить информацию о кастомных ядерных боеголовках
        // TODO: Добавить QMAW и cannery подсказки

        // Пока добавим базовую подсказку для отладки
        if (event.getFlags().isAdvanced()) {
            // TODO: Добавить расширенную информацию при включенных advanced tooltips
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen ts)) return;

        int id = System.identityHashCode(ts);
        if (id != lastTitleScreenId) {
            lastTitleScreenId = id;

            var splash = pickSplash();
            modSplashText = (splash == null) ? null : splash.getString();

            // снимем baseline после инициализации экрана
            baselineSplash = getSplash(ts);

            // сбросим кэш от прошлого экрана
            hbmmSplashRenderer = null;
            hbmmSplashTextCached = null;
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onTitleRenderPre(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof TitleScreen ts)) return;
    
        // если по шансам выбрали ваниллу — не вмешиваемся
        if (modSplashText == null) {
            // опционально: если вдруг остался наш сплеш (редко), можно восстановить baseline
            // restoreBaselineIfOurs(ts);
            return;
        }
    
        SplashRenderer current = getSplash(ts);
    
        // Вежливый режим: если другой мод поменял splash на что-то своё (не baseline и не наш),
        // то не перетираем его.
        if (COOPERATIVE_SPLASH) {
            boolean isOurs = (current != null && current == hbmmSplashRenderer);
            boolean isBaseline = (current == baselineSplash);
            boolean isEmpty = (current == null);
    
            if (!(isOurs || isBaseline || isEmpty)) {
                return;
            }
        }
    
        // кэшируем объект SplashRenderer: создаём только если изменился текст
        if (hbmmSplashRenderer == null || !modSplashText.equals(hbmmSplashTextCached)) {
            hbmmSplashRenderer = new SplashRenderer(modSplashText);
            hbmmSplashTextCached = modSplashText;
        }
    
        setSplash(ts, hbmmSplashRenderer);
    }

    private static java.lang.reflect.Field resolveSplashField() {
        if (splashFieldResolved) return splashField;
        splashFieldResolved = true;
    
        try {
            for (java.lang.reflect.Field f : TitleScreen.class.getDeclaredFields()) {
                if (f.getType() == SplashRenderer.class) {
                    f.setAccessible(true);
                    splashField = f;
                    return splashField;
                }
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("Failed to resolve TitleScreen splash field", t);
        }
    
        return null;
    }

    private static SplashRenderer getSplash(TitleScreen ts) {
        java.lang.reflect.Field f = resolveSplashField();
        if (f == null) return null;
        try {
            return (SplashRenderer) f.get(ts);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void setSplash(TitleScreen ts, SplashRenderer renderer) {
        java.lang.reflect.Field f = resolveSplashField();
        if (f == null) return;
        try {
            f.set(ts, renderer);
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("Failed to set TitleScreen splash", t);
        }
    }


    /**
     * Получает случайный веселый сплеш-текст или null для ванильного сплеша
     * Кастомные сплеши появляются с шансом 30%
     */
    private static net.minecraft.network.chat.Component pickSplash() {
        double r = Math.random();
    
        // 70% vanilla
        if (r < 0.70) {
            return null;
        }
    
        // +0.2% rare (absolute)
        if (r < 0.702) {
            return net.minecraft.network.chat.Component.literal("Redditors aren't people!");
        }
    
        // 29.8% normal mod splashes
        int rand = (int) (Math.random() * 26);
        String text = switch (rand) {
            case 0 -> "Floppenheimer!";
            case 1 -> "i should dip my balls in sulfuric acid";
            case 2 -> "All answers are popbob!";
            case 3 -> "None may enter The Orb!";
            case 4 -> "Wacarb wasn't here";
            case 5 -> "STUNTMAN? More like FAILMAN!";
            case 6 -> "SpongeBoy me Bob I am overdosing on ketamine agagagagaga";
            case 7 -> ChatFormatting.RED + "I know where you live, " + System.getProperty("user.name");
            case 8 -> "Nice toes, now hand them over.";
            case 9 -> "I smell burnt toast!";
            case 10 -> "Imagine being scared by splash texts!";
            case 11 -> "Semantic versioning? More like pedantic versioning.";
            case 12 -> "Now with HBM's Nuclear Tech Modernized!";
            case 13 -> "Caution: May contain radiation!";
            case 14 -> "More fallout than you can handle!";
            case 15 -> "Nuclear apocalypse approved!";
            case 16 -> "Tactical nuclear penguin!";
            case 17 -> "Explosions per minute!";
            case 18 -> "Lead lined underwear not included!";
            case 19 -> "1984 is here!";
            case 20 -> "Diddy Edition!";
            case 21 -> "CREATE HARAM";
            case 22 -> "''playing HBM'' - means fondling my balls. © Bob";
            case 23 -> "";
            case 24 -> "STUNTMAN, touch some grass!";
            case 25 -> "STUNTMAN tried to swim in lava";
            default -> "Nuclear winter is coming!";
        };
        return net.minecraft.network.chat.Component.literal(text);
    }

    /**
     * Обработка изменения FOV (поля зрения)
     * TODO: Проверить доступность FOVUpdateEvent
     */
    // @SubscribeEvent
    // public static void onFOVUpdate(FOVUpdateEvent event) {
    //     // Изменение FOV для оружия
    // }

    /**
     * Обработка рендеринга игроков (перед рендерингом)
     */
    // @SubscribeEvent
    // public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
    //     // TODO: Реализовать невидимость (MainRegistry.proxy.isVanished)
    //     // TODO: Реализовать плащи (RenderAccessoryUtility.getCloakFromPlayer)
    //     // TODO: Реализовать анимацию оружия (IHoldableWeapon)
    // }

    /**
     * Обработка рендеринга брони
     * TODO: Проверить правильное имя события для рендеринга брони
     */
    // @SubscribeEvent
    // public static void onRenderArmor(RenderPlayerEvent.SetArmorModel event) {
    //     // Модификации брони и аксессуары
    // }

    /**
     * Обработка рендеринга сущностей (перед рендерингом, высокий приоритет)
     */
    // @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    // public static void onRenderLivingPre(RenderLivingEvent.Pre event) {
    //     // TODO: Реализовать невидимость сущностей (MainRegistry.proxy.isVanished)
    // }

    // /**
    //  * Обработка рендеринга сущностей (перед рендерингом)
    //  */
    // @SubscribeEvent
    // public static void onRenderLivingPreNormal(RenderLivingEvent.Pre event) {
    //     Minecraft mc = Minecraft.getInstance();
    //     LocalPlayer player = mc.player;

    //     if (player == null) return;

    //     // TODO: Реализовать VATS систему для отображения здоровья сущностей
    //     // TODO: Проверить наличие FSB брони и VATS режима
    // }

    /**
     * Обработка воспроизведения звуков
     * TODO: Проверить доступность PlaySoundEvent
     */
    // @SubscribeEvent
    // public static void onPlaySound(PlaySoundEvent event) {
    //     // Вакуумные блоки, движущиеся звуки
    // }

    /**
     * Обработка текстур (предварительная стадия)
     * TODO: Проверить доступность TextureStitchEvent
     */
    // @SubscribeEvent
    // public static void onTextureStitchPre(TextureStitchEvent.Pre event) {
    //     // Регистрация текстур частиц
    // }

    /**
     * Обработка текстур (пост-стадия)
     * TODO: Проверить доступность TextureStitchEvent
     */
    // @SubscribeEvent
    // public static void onTextureStitchPost(TextureStitchEvent.Post event) {
    //     // Получение спрайтов частиц
    // }

    /**
     * Обработка рендеринга предметов в рамке
     */
    @SubscribeEvent
    public static void onRenderItemInFrame(RenderItemInFrameEvent event) {
        ItemStack item = event.getItemStack();
        Minecraft mc = Minecraft.getInstance();

        // TODO: Реализовать проверку для flame_pony и paper предметов
        // Пока оставим как заглушку

        if (item != null && !item.isEmpty()) {
            // TODO: Добавить специальный рендеринг для определенных предметов
            // Пример для flame_pony - рендеринг постера
            // Пример для paper - рендеринг кошачьего постера
            MainRegistry.LOGGER.debug("Item in frame: " + item.getItem());
        }
    }

    /**
     * Обработка тика мира
     * TODO: Проверить доступность WorldTickEvent
     */
    // @SubscribeEvent
    // public static void onWorldTick(TickEvent.WorldTickEvent event) {
    //     // Обработка тика мира
    // }


    /**
     * Дополнительный рендер‑хук для ORIGINAL_FALLBACK:
     * рисуем простые белые боксы вокруг живых сущностей, видимые только
     * для игрока с включённым тепловизором и не сквозь блоки
     * (используем depth‑buffer основного рендера).
     *
     * Используем тот же паттерн, что и большинство примеров Forge:
     * Stage.AFTER_ENTITIES + translate(-camPos) поверх poseStack,
     * в которой уже учтён поворот камеры.
     */
    @SubscribeEvent
    public static void onRenderWorldThermalFallback(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.level == null) {
            return;
        }

        if (!thermalActive) {
            return;
        }
        if (ModClothConfig.get().thermalRenderMode != ModClothConfig.ThermalRenderMode.ORIGINAL_FALLBACK) {
            return;
        }

        renderThermalFallbackOutlines(mc, event);
    }

    /**
     * Рисует белые контуры вокруг всех видимых живых сущностей.
     * Это чисто клиентский эффект: никаких эффектов/пакетов на сервер.
     */
    private static void renderThermalFallbackOutlines(Minecraft mc, RenderLevelStageEvent event) {
        PoseStack poseStack = event.getPoseStack();
        var bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

        LocalPlayer player = mc.player;
        var camPos = mc.getEntityRenderDispatcher().camera.getPosition();

        poseStack.pushPose();
        // Как в типичных примерах Forge: Stage.AFTER_ENTITIES уже учитывает
        // поворот камеры, поэтому добавляем только сдвиг в -camPos.
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        try {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity == player) continue;
                if (!player.hasLineOfSight(entity)) continue;

                var worldBox = living.getBoundingBox().inflate(0.05);

                // Ярко‑красная рамка (R,G,B,A)
                LevelRenderer.renderLineBox(
                        poseStack,
                        consumer,
                        worldBox.minX, worldBox.minY, worldBox.minZ,
                        worldBox.maxX, worldBox.maxY, worldBox.maxZ,
                        1.0F, 0.0F, 0.0F, 0.9F
                );
            }
        } finally {
            poseStack.popPose();
            bufferSource.endBatch(RenderType.lines());
        }
    }

    /**
     * Активирует ядерную вспышку
     * Вызывается при ядерном взрыве или подобных событиях
     */
    public static void triggerNuclearFlash() {
        flashTimestamp = System.currentTimeMillis();
    }


    /**
     * VATS система - методы управления
     */
    public static void activateVATS() {
        vatsActive = true;
        vatsActivatedTime = System.currentTimeMillis();
    }

    public static void deactivateVATS() {
        vatsActive = false;
    }

    public static boolean isVATSActive() {
        return vatsActive;
    }

    /**
     * Тепловизор - методы управления
     */
    public static void activateThermal() {
        if (!thermalActive) {
            Minecraft mc = Minecraft.getInstance();
            // First-time per-world warning gate:
            // - Показываем предупреждение один раз на мир/сервер
            // - Не включаем тепловизор при первом нажатии, независимо от режима.
            if (com.hbm_m.powerarmor.overlay.ThermalVisionWarningStore.shouldBlockFirstActivation(mc)) {
                return;
            }

            thermalActive = true;
            if (MainRegistry.LOGGER.isDebugEnabled()) {
                MainRegistry.LOGGER.debug("[ThermalVision] Activated thermal vision");
            }
            LocalPlayer player = mc.player;
            var level = mc.level;
            if (player != null && level != null) {
                var soundEvent = com.hbm_m.sound.ModSounds.NVG_ON.get();
                if (soundEvent != null) {
                    RandomSource random = level.getRandom();
                    float pitch = 0.9F + random.nextFloat() * 0.2F;
                    level.playSound(player, player.getX(), player.getY(), player.getZ(), 
                        soundEvent, SoundSource.PLAYERS, 0.2F, pitch);
                }
            }
        }
    }

    public static void deactivateThermal() {
        if (thermalActive) {
            thermalActive = false;
            if (MainRegistry.LOGGER.isDebugEnabled()) {
                MainRegistry.LOGGER.debug("[ThermalVision] Deactivated thermal vision");
            }
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            var level = mc.level;
            if (player != null && level != null) {
                var soundEvent = com.hbm_m.sound.ModSounds.NVG_OFF.get();
                if (soundEvent != null) {
                    RandomSource random = level.getRandom();
                    float pitch = 0.9F + random.nextFloat() * 0.2F; // Random pitch between 0.9 and 1.1
                    level.playSound(player, player.getX(), player.getY(), player.getZ(),
                        soundEvent, SoundSource.PLAYERS, 0.6F, pitch);
                }
            }
        }
        // ThermalVisionRenderer.clearSpectralHighlights();
    }

    public static boolean isThermalActive() {
        return thermalActive;
    }  

    /**
     * Рендерит оверлей тепловизора
     */
    public static void onRenderThermalOverlay(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.options.hideGui || !thermalActive) {
            return;
        }

        // Проверяем наличие силовой брони с тепловизором
        if (!ModPowerArmorItem.hasFSBArmor(player)) {
            // Если броня снята, деактивируем тепловизор
            thermalActive = false;
            return;
        }

        var chestplate = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chestplate.getItem() instanceof ModPowerArmorItem armorItem)) {
            thermalActive = false;
            return;
        }

        var specs = armorItem.getSpecs();
        // Проверяем, поддерживает ли броня тепловизор
        if (!specs.hasThermal) {
            // Если броня не поддерживает тепловизор, деактивируем его
            thermalActive = false;
            return;
        }

        guiGraphics.drawString(mc.font, "THERMAL VISION", 10, 10, 0x00FF00);
    }

    // TODO: Добавить методы для HUD оверлеев
    // TODO: Добавить методы для визуальных эффектов
    // TODO: Добавить методы для обработки ввода
    // Текстуры для предметов в рамках
    private static final ResourceLocation POSTER = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/misc/poster.png");
    private static final ResourceLocation POSTER_CAT = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/misc/poster_cat.png");

    // ===== TODO ЗАГЛУШКИ ДЛЯ НЕРЕАЛИЗОВАННЫХ ФУНКЦИЙ =====
    // Эти функции требуют дополнительных зависимостей или инфраструктуры,
    // которая еще не портирована на 1.20.1

    /**
     * TODO: Dodd RBMK диагностика
     * Оригинальный код: onOverlayRender - DODD RBMK DIAGNOSTIC HOOK
     * Требует: ILookOverlay интерфейс, RBMK система
     * Сложность: Высокая - требует полной RBMK системы
     */

    /**
     * TODO: Рельсовая система (IRailNTM)
     * Оригинальный код: onOverlayRender - RailContext, IRailNTM
     * Требует: Полную рельсовую систему для поездов
     * Сложность: Очень высокая - комплексная физика движения
     */

    /**
     * TODO: QMAW система (Quick Manual And Wiki)
     * Оригинальный код: drawTooltip, clientTick - QMAWLoader, CanneryBase
     * Требует: QMAW интерфейс, cannery систему
     * Сложность: Средняя - требует контентной системы
     */

    /**
     * TODO: HTTP капсула система
     * Оригинальный код: onRenderWorldLastEvent - HTTPHandler.capsule
     * Требует: HTTP обработчик для загрузки внешнего контента
     * Сложность: Средняя - сетевые функции
     */

    /**
     * Hazard система реализована в com.hbm_m.hazard
     * @see com.hbm_m.hazard.HazardSystem
     * @see com.hbm_m.hazard.HazardData
     */

    /**
     * Armor модификации реализованы в com.hbm_m.armormod
     * @see com.hbm_m.armormod.item.ItemArmorMod
     * @see com.hbm_m.armormod.util.ArmorModificationHelper
     */

    /**
     * TODO: Кастомные ядерные боеголовки
     * Оригинальный код: drawTooltip - TileEntityNukeCustom
     * Требует: Систему кастомных ядерных боеголовок
     * Сложность: Высокая - требует nuke системы
     */

    /**
     * Звуковая система реализована в com.hbm_m.sound
     * @see com.hbm_m.sound.ModSounds
     * @see com.hbm_m.sound.PowerArmorSoundHandler
     */

    /**
     * TODO: RenderOverhead система
     * Оригинальный код: onRenderWorldLastEvent - RenderOverhead
     * Требует: Систему оверлейного рендеринга для маркеров и эффектов
     * Сложность: Высокая - комплексный рендеринг
     */

    /**
     * TODO: HbmLivingProps и HbmPlayerProps
     * Оригинальный код: Различные методы - HbmLivingProps, HbmPlayerProps
     * Требует: Систему свойств сущностей игрока
     * Сложность: Средняя - capability система
     */

    /**
     * TODO: Clock системное время
     * Оригинальный код: onRenderWorldLastEvent - Clock.get_ms()
     * Требует: Синхронизированную систему времени
     * Сложность: Низкая - утилитарная функция
     */

    // TODO: Добавить вспомогательные методы

}