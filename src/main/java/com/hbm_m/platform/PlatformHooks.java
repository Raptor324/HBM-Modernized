package com.hbm_m.platform;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.nio.file.Path;

import dev.architectury.platform.Platform;

/**
 * ЯДРО платформенного слоя: только общеплатформенные утилиты, не имеющие
 * профильного домен-класса. Вся предметная логика разнесена:
 * NBT/сериализация — {@link StackNbt}, предметы/еда — {@link ItemHooks},
 * блоки/BE — {@link BlockHooks}, сущности/игроки — {@link MobHooks},
 * уровень/звук/чанки — {@link LevelHooks}, атрибуты — {@link AttributeHooks},
 * эффекты — {@link EffectHooks}, жидкости — {@link FluidHooks},
 * клиентские модели — {@link LoaderHooks}, рендер — {@link RenderHooks},
 * серверно-безопасные уведомления — {@link CommonRenderHooks}.
 * ОБРАТНЫХ ДЕЛЕГАТОВ НЕТ — call-сайты зовут домен-классы напрямую.
 */
public final class PlatformHooks {
    private PlatformHooks() {}

    // =====================================================================================
    //  Best-effort HolderLookup.Provider для item-NBT, когда Level/Player недоступны
    //  (например статические Item-методы getName/tooltip без контекста Level).
    //  ONLY 1.21.1 — на 1.20.1 stack.save(CompoundTag)/ItemStack.of(CompoundTag) провайдер не требуют.
    //  Берётся из клиентского Level (tooltip/render — основной call-site таких Item-методов).
    //  На dedicated server возвращает null — call-site'ы обязаны деградировать (EMPTY stack).
    // =====================================================================================

    //? if >= 1.21.1 {
    /*@OnlyIn(Dist.CLIENT)
    public static net.minecraft.core.HolderLookup.Provider clientProvider() {
        return ClientHolder.clientProvider();
    }
    *///?}

    /**
     * ВЕСЬ клиентский доступ из PlatformHooks живёт здесь: holder загружается JVM только
     * при фактическом вызове на клиенте. Прямые обращения к Minecraft/PauseScreen в телах
     * методов PlatformHooks недопустимы — верификатор байткода на dedicated server
     * резолвит типы из instanceof и падает NoClassDefFoundError.
     */
    @OnlyIn(Dist.CLIENT)
    private static final class ClientHolder {
        static boolean isPauseMenuOpen() {
            return net.minecraft.client.Minecraft.getInstance().screen
                    instanceof net.minecraft.client.gui.screens.PauseScreen;
        }

        //? if >= 1.21.1 {
        /*static net.minecraft.core.HolderLookup.Provider clientProvider() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null) return mc.level.registryAccess();
            if (mc.getConnection() != null) return mc.getConnection().registryAccess();
            return null;
        }
        *///?}
    }

    /**
     * Провайдер реестров «из любой доступной стороны»: клиентский Level на клиенте,
     * текущий сервер на dedicated server. null на 1.20.1 (не нужен) или если сторона
     * ещё не поднята — вызыватели обязаны деградировать (EMPTY stack), не падать.
     */
    public static net.minecraft.core.HolderLookup.Provider bestEffortProvider() {
        //? if < 1.21.1 {
        return null;
        //?} else {
        /*if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            return clientProvider();
        }
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.registryAccess() : null;
        *///?}
    }

    /**
     * Открыто ли меню паузы (Esc) в синглплеере — единственный штатный случай,
     * когда интегрированный сервер не тикает, но это не зависание. Экраны
     * сохранения мира сюда не попадают (ProgressScreen — не PauseScreen).
     *
     * <p>Вызывать только при {@code !server.isDedicatedServer()}; клиентский доступ
     * изолирован в ClientHolder (см. комментарий выше).
     */
    public static boolean isPauseMenuOpen() {
        return net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient() && ClientHolder.isPauseMenuOpen();
    }

    // =====================================================================================
    //  Каталог конфигурации (config dir).
    //  Кросс-лоадерный доступ к <game>/config. Используется HbmConfigStore / ConfigPaths
    //  для JSON-бэкенда (замена AutoConfig/Toml4j). Повторяет оригинальный
    //  RunningConfig.getConfig() из 1.7.10 (configHbmDir).
    // =====================================================================================

    /** Глобальный каталог {@code <game>/config}. Подкаталог мода — {@code config/hbm_m}. */
    public static Path getConfigDir() {
        return Platform.getConfigFolder();
    }

    /** Парсинг JSON-строки в Component (учитывает смену сигнатуры Serializer.fromJson в 1.21.1). */
    public static net.minecraft.network.chat.Component parseComponentJson(String json, net.minecraft.core.HolderLookup.Provider provider) {
        if (json == null || json.isEmpty()) return null;
        //? if < 1.21.1 {
        return net.minecraft.network.chat.Component.Serializer.fromJson(com.google.gson.JsonParser.parseString(json));
        //?} else {
        /*return net.minecraft.network.chat.Component.Serializer.fromJson(json, provider);
        *///?}
    }

    /** Сериализация Component в JSON-строку. */
    public static String componentToJson(net.minecraft.network.chat.Component component, net.minecraft.core.HolderLookup.Provider provider) {
        if (component == null) return null;
        //? if < 1.21.1 {
        return net.minecraft.network.chat.Component.Serializer.toJson(component);
        //?} else {
        /*return net.minecraft.network.chat.Component.Serializer.toJson(component, provider);
        *///?}
    }

    /**
     * Namespace of conventional item/block tags: "forge" on 1.20.1, "c" on 1.21+
     * (the NeoForge build remaps generated forge: tag JSONs to c:).
     */
    public static String conventionalTagNamespace() {
        //? if < 1.21.1 {
        return "forge";
        //?} else {
        /*return "c";
        *///?}
    }

    /**
     * Средний MSPT сервера. 1.20.1: {@code getAverageTickTime()} (миллисекунды, float);
     * 1.21.1: {@code getAverageTickTimeNanos()} (наносекунды).
     */
    public static double averageTickMs(net.minecraft.server.MinecraftServer server) {
        //? if < 1.21.1 {
        return server.getAverageTickTime();
        //?} else {
        /*return server.getAverageTickTimeNanos() / 1_000_000.0;
         *///?}
    }
}
