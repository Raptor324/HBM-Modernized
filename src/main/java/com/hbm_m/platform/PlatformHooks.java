package com.hbm_m.platform;

import java.nio.file.Path;
import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;

/**
 * Платформенный слой Strategy B: тонкие адаптеры для API, которые НЕ покрывает Architectury.
 *
 * <p>Архитектура (см. правило {@code platform-strategy-b.md}):
 * <ul>
 *   <li>Если Architectury уже даёт абстракцию — используем её напрямую (например, меню:
 *       {@code dev.architectury.registry.menu.MenuRegistry.openExtendedMenu}).</li>
 *   <li>Если абстракции нет — hook добавляется сюда с ветвлением через stonecutter
 *       {@code //? if forge/fabric/neoforge}.</li>
 * </ul>
 *
 * <h2>Item custom-NBT / DataComponents bridge</h2>
 * Кросс-версионный доступ к «произвольным данным предмета». Это главная разница 1.20.1 ↔ 1.21.1:
 * <ul>
 *   <li><b>1.20.1</b> (forge/fabric): {@code ItemStack.getTag()/getOrCreateTag()/setTag()} —
 *       живой {@link CompoundTag}, мутации сохраняются сразу.</li>
 *   <li><b>1.21.1+</b> (neoforge): {@code DataComponents.CUSTOM_DATA} ({@code CustomData}) —
 *       иммутабельное значение; чтение даёт <b>копию</b>, для сохранения изменений нужен
 *       {@code stack.set(DataComponents.CUSTOM_DATA, ...)}.</li>
 * </ul>
 *
 * <p><b>Следствие:</b> идеальной drop-in замены {@code getOrCreateTag()} (живая ссылка) на 1.21.1
 * не существует. Поэтому:
 * <ul>
 *   <li>Для мутаций используйте {@link #editItemTag} (read-modify-write, сохраняется на обеих версиях).</li>
 *   <li>Для чтения — {@link #getItemTag} (возвращает копию на 1.21.1; не мутируйте).</li>
 * </ul>
 *
 * <p><b>Gating-нюанс:</b> NBT→DataComponents — это ВЕРСИОННОЕ различие, но пока gated по loader'у
 * ({@code forge||fabric} = 1.20.1 NBT, {@code neoforge} = 1.21.1 DataComponents) — корректно для
 * текущего набора (1.20.1-forge/fabric, 1.21.1-neoforge). При активации 1.21.1-fabric потребуется
 * рефакторинг на version-gating (константа {@code data_components} в stonecutter.gradle.kts).
 */
public final class PlatformHooks {
    private PlatformHooks() {}

    /**
     * Custom-NBT предмета (только для чтения).
     *
     * <p><b>ВАЖНО:</b> на 1.21.1 возвращается КОПИЯ — не мутируйте её с ожиданием сохранения.
     * Для записи используйте {@link #editItemTag} или {@link #setItemTag}.
     *
     * @return тег или {@code null}, если у предмета нет custom-NBT
     */
    public static CompoundTag getItemTag(ItemStack stack) {
        //? if forge || fabric {
        return stack.getTag();
        //?}
        //? if neoforge {
        /*net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
        *///?}
    }

    /**
     * Тег из {@link ClientboundBlockEntityDataPacket} (только для чтения).
     *
     * <p><b>Версионная инвариантность:</b> {@code pkt.getTag()} существует и работает
     * одинаково на 1.20.1 и 1.21.1 — это сетевой пакет, а не ItemStack, поэтому
     * DataComponents здесь не применимы. Stonecutter-gating не нужен.
     */
    public static CompoundTag getItemTag(ClientboundBlockEntityDataPacket pkt) {
        return pkt.getTag();
    }

    /** Есть ли у предмета custom-NBT. */
    public static boolean hasItemTag(ItemStack stack) {
        //? if forge || fabric {
        return stack.hasTag();
        //?}
        //? if neoforge {
        /*return stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        *///?}
    }

    /**
     * Read-modify-write: изменения сохраняются на ОБЕИХ версиях.
     *
     * <p>Рекомендованный примитив для мутаций. Заменяет паттерн
     * {@code CompoundTag t = stack.getOrCreateTag(); t.putX(...); }.
     *
     * <pre>{@code
     * // было (1.20.1):     stack.getOrCreateTag().putLong("energy", v);
     * // стало (обе версии): PlatformHooks.editItemTag(stack, t -> t.putLong("energy", v));
     * }</pre>
     */
    public static void editItemTag(ItemStack stack, Consumer<CompoundTag> editor) {
        //? if forge || fabric {
        editor.accept(stack.getOrCreateTag());
        //?}
        //? if neoforge {
        /*net.minecraft.world.item.component.CustomData data = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY);
        net.minecraft.nbt.CompoundTag tag = data.copyTag();
        editor.accept(tag);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
        *///?}
    }

    /**
     * Полная перезапись custom-NBT предмета. {@code tag == null} очищает данные (аналог
     * {@code stack.setTag(null)} на 1.20.1).
     */
    public static void setItemTag(ItemStack stack, CompoundTag tag) {
        //? if forge || fabric {
        stack.setTag(tag);
        //?}
        //? if neoforge {
        /*if (tag == null || tag.isEmpty()) {
            stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        } else {
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag));
        }
        *///?}
    }

    // =====================================================================================
    //  ItemStack сравнение и сериализация (версионные обёртки).
    //
    //  1.20.1: isSameItemSameTags, ItemStack.of(CompoundTag), stack.save(CompoundTag).
    //  1.21.1: isSameItemSameComponents, ItemStack.parseOptional(Provider, CompoundTag),
    //          stack.save(Provider, CompoundTag) — все требуют HolderLookup.Provider.
    // =====================================================================================

    /**
     * Сравнение двух ItemStack по предмету и данным (NBT на 1.20.1 / DataComponents на 1.21.1).
     * Заменяет {@code ItemStack.isSameItemSameTags(a, b)}.
     */
    public static boolean isSameItemSameTags(ItemStack a, ItemStack b) {
        //? if forge || fabric {
        return ItemStack.isSameItemSameTags(a, b);
        //?}
        //? if neoforge {
        /*return ItemStack.isSameItemSameComponents(a, b);
        *///?}
    }

    /**
     * Создание ItemStack из NBT-тега. Заменяет {@code ItemStack.of(tag)} на 1.20.1.
     * На 1.21.1 требует {@code HolderLookup.Provider} (DataComponents несут реестровые ссылки).
     *
     * @param provider реестры (передавайте {@code level.holderLookup()} или {@code player.registryAccess()})
     */
    public static ItemStack itemStackOf(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        //? if forge || fabric {
        return ItemStack.of(tag);
        //?}
        //? if neoforge {
        /*return ItemStack.parseOptional(provider, tag);
        *///?}
    }

    /**
     * Сохранение ItemStack в NBT-тег. Заменяет {@code stack.save(tag)} на 1.20.1.
     * Возвращает тот же (заполненный) тег на обеих версиях.
     *
     * @param provider реестры (на 1.21.1 обязателен)
     */
    public static CompoundTag saveItemStack(ItemStack stack, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        //? if forge || fabric {
        return stack.save(tag);
        //?}
        //? if neoforge {
        /*return (CompoundTag) stack.save(provider, tag);
        *///?}
    }

    /**
     * Удобная обёртка: сохраняет {@code stack} в НОВЫЙ {@link CompoundTag} и возвращает его.
     * Используйте, когда не нужен конкретный целевой тег (как {@code stack.save(new CompoundTag())}).
     */
    public static CompoundTag safeItemSave(ItemStack stack, net.minecraft.core.HolderLookup.Provider provider) {
        return saveItemStack(stack, new CompoundTag(), provider);
    }

    // =====================================================================================
    //  Типизированные хелперы (COMMON — делегируют в getItemTag/editItemTag, без gating).
    //  Цель: сохранить линейный стиль вызова, близкий к stack.getOrCreateTag().putX(...),
    //  без лямбд и лишних скобок. Каждый putX делает read-modify-write (сохраняется на обеих версиях).
    // =====================================================================================

    /** {@code stack.getTag().getInt(key)} с защитой от null (0 если тега/ключа нет). */
    public static int getInt(ItemStack stack, String key) {
        CompoundTag t = getItemTag(stack);
        return t == null ? 0 : t.getInt(key);
    }

    /** {@code stack.getTag().getLong(key)} с защитой от null. */
    public static long getLong(ItemStack stack, String key) {
        CompoundTag t = getItemTag(stack);
        return t == null ? 0L : t.getLong(key);
    }

    /** {@code stack.getTag().getBoolean(key)} с защитой от null. */
    public static boolean getBoolean(ItemStack stack, String key) {
        CompoundTag t = getItemTag(stack);
        return t != null && t.getBoolean(key);
    }

    /** {@code stack.getTag().getString(key)} с защитой от null. */
    public static String getString(ItemStack stack, String key) {
        CompoundTag t = getItemTag(stack);
        return t == null ? "" : t.getString(key);
    }

    /** {@code stack.getTag().getCompound(key)} с защитой от null. */
    public static CompoundTag getCompound(ItemStack stack, String key) {
        CompoundTag t = getItemTag(stack);
        return t == null ? new CompoundTag() : t.getCompound(key);
    }

    /** {@code stack.getOrCreateTag().putInt(key, val)} — read-modify-write, сохраняется на обеих версиях. */
    public static void putInt(ItemStack stack, String key, int val) { editItemTag(stack, t -> t.putInt(key, val)); }

    /** {@code stack.getOrCreateTag().putLong(key, val)}. */
    public static void putLong(ItemStack stack, String key, long val) { editItemTag(stack, t -> t.putLong(key, val)); }

    /** {@code stack.getOrCreateTag().putBoolean(key, val)}. */
    public static void putBoolean(ItemStack stack, String key, boolean val) { editItemTag(stack, t -> t.putBoolean(key, val)); }

    /** {@code stack.getOrCreateTag().putString(key, val)}. */
    public static void putString(ItemStack stack, String key, String val) { editItemTag(stack, t -> t.putString(key, val)); }

    /** {@code stack.getOrCreateTag().put(key, val)} для вложенного CompoundTag. */
    public static void put(ItemStack stack, String key, CompoundTag val) { editItemTag(stack, t -> t.put(key, val)); }

    /** {@code stack.hasTag() && stack.getTag().contains(key)}. */
    public static boolean contains(ItemStack stack, String key) {
        CompoundTag t = getItemTag(stack);
        return t != null && t.contains(key);
    }

    /** {@code stack.getTag().remove(key)} — read-modify-write, сохраняется на обеих версиях. */
    public static void remove(ItemStack stack, String key) { editItemTag(stack, t -> t.remove(key)); }

    // =====================================================================================
    //  Tooltip Level extraction (appendHoverText signature bridge).
    //
    //  1.20.1 (forge/fabric): Item.appendHoverText(ItemStack, @Nullable Level, List<Component>, TooltipFlag).
    //  1.21.1 (neoforge):     Item.appendHoverText(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag).
    //      Item.TooltipContext.level() возвращает @Nullable Level (Neo patch).
    //
    //  Хелпер извлекает Level из аргумента tooltip-метода на обеих версиях.
    //  Используется, когда тело appendHoverText действительно обращается к Level
    //  (например, RecipeManager). В остальных случаях параметр не трогается телом.
    // =====================================================================================

    /**
     * Извлекает {@link Level} из второго аргумента {@code appendHoverText}.
     * Используйте ВНУТРИ тела tooltip-метода, если нужен Level (например, для
     * {@code level.getRecipeManager()}).
     *
     * <p><b>Паттерн вызова:</b> сигнатура метода обёрнута через stonecutter, имя
     * параметра сохранено как {@code level}; для извлечения собственно Level:
     * <pre>{@code
     * //? if < 1.21.1 {
     * public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag f) {
     * //?} else {
     * public void appendHoverText(ItemStack stack, Item.TooltipContext level, List<Component> tip, TooltipFlag f) {
     * //?}
     *     Level lvl = PlatformHooks.tooltipLevel(level);
     *     ...
     * }
     * }</pre>
     *
     * @param levelOrContext на 1.20.1 — {@link Level}; на 1.21.1 — {@code Item.TooltipContext}
     * @return {@link Level} или {@code null}, если контекст без level
     */
    public static Level tooltipLevel(Object levelOrContext) {
        //? if forge || fabric {
        return (Level) levelOrContext;
        //?}
        //? if neoforge {
        /*if (levelOrContext instanceof net.minecraft.world.item.Item.TooltipContext ctx) {
            return ctx.level();
        }
        return null;
        *///?}
    }

    // =====================================================================================
    //  Каталог конфигурации (config dir).
    //  Кросс-лоадерный доступ к <game>/config. Используется HbmConfigStore / ConfigPaths
    //  для JSON-бэкенда (замена AutoConfig/Toml4j). Путь глобальный, не world-local —
    //  повторяет оригинальный RunningConfig.getConfig() из 1.7.10 (configHbmDir).
    //  Fabric: FabricLoader; Forge/NeoForge: FMLPaths.
    // =====================================================================================

    /** Глобальный каталог {@code <game>/config}. Подкаталог мода — {@code config/hbm_m}. */
    public static Path getConfigDir() {
        //? if forge {
        return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        //?}
        //? if fabric {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        *///?}
        //? if neoforge {
        /*return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        *///?}
    }
}
