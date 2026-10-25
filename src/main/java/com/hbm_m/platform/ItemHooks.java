package com.hbm_m.platform;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * Платформенный и версионный слой для сглаживания различий работы с предметами,
 * зачарованиями и их прочностью между 1.20.1 и 1.21.1.
 */
public final class ItemHooks {
    private ItemHooks() {}

    /**
     * Кросс-версионное нанесение урона предмету (замена stack.hurtAndBreak).
     * В 1.20.1 принимал Consumer с броадкастом, в 1.21.1 принимает EquipmentSlot.
     */
    public static void hurtAndBreak(ItemStack stack, int amount, LivingEntity entity, net.minecraft.world.InteractionHand hand) {
        //? if < 1.21.1 {
        stack.hurtAndBreak(amount, entity, e -> e.broadcastBreakEvent(hand));
        //?} else {
        /*stack.hurtAndBreak(amount, entity, hand == net.minecraft.world.InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        *///?}
    }

    /**
     * Ersatz fuer {@code stack.hurtAndBreak(n, e, x -> x.broadcastBreakEvent(slot))} (Ausruestungsslot statt Hand).
     */
    public static void hurtAndBreak(ItemStack stack, int amount, LivingEntity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        //? if < 1.21.1 {
        stack.hurtAndBreak(amount, entity, e -> e.broadcastBreakEvent(slot));
        //?} else {
        /*stack.hurtAndBreak(amount, entity, slot);
        *///?}
    }

    /**
     * Ersatz fuer {@code stack.hurtAndBreak(n, e, x -> {})} (ohne Zerbrech-Animation). Auf 1.21.1 nur
     * serverseitig wirksam (wie Vanilla).
     */
    public static void hurtAndBreakSilent(ItemStack stack, int amount, LivingEntity entity) {
        //? if < 1.21.1 {
        stack.hurtAndBreak(amount, entity, e -> {});
        //?} else {
        /*if (entity.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            stack.hurtAndBreak(amount, sl, entity, item -> {});
        }
        *///?}
    }

    /**
     * Кросс-версионное чтение уровня зачарования (string-форма — парсит ResourceLocation).
     * Горячим путям предпочтительна типизированная форма
     * ({@code Enchantments.SILK_TOUCH} и т.п.) — она не парсит строку.
     * Невалидный/несуществующий id — тихо 0 (tryParse, без исключений, паритет версий).
     */
    public static int getEnchantmentLevel(ItemStack stack, LevelAccessor level, String enchName) {
        //? if < 1.21.1 {
        return getEnchantmentLevel(stack, level, resolveEnchantment(enchName));
        //?} else {
        /*net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(enchName);
        if (id == null) return 0;
        return getEnchantmentLevel(stack, level, net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.ENCHANTMENT, id));
        *///?}
    }

    /**
     * Типизированное чтение уровня зачарования: на 1.20.1 принимает {@code Enchantment}
     * (константы {@code Enchantments.*}), на 1.21.1 — {@code ResourceKey<Enchantment>}
     * (поля {@code Enchantments.*} переехали на ResourceKey) — call-сайт с константой
     * компилируется на обеих версиях без гейта.
     */
    //? if < 1.21.1 {
    public static int getEnchantmentLevel(ItemStack stack, LevelAccessor level, net.minecraft.world.item.enchantment.Enchantment ench) {
        if (ench == null) return 0;
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(ench, stack);
    }
    //?} else {
    /*public static int getEnchantmentLevel(ItemStack stack, LevelAccessor level,
            net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchKey) {
        // Ранний выход ДО registry-lookup: на незачарованных стаках (99% обычных предметов)
        // дергать registryOrThrow(...).getHolder(...) каждый тик — чистый оверхед.
        if (stack.isEmpty() || !stack.isEnchanted()) return 0;
        var holder = resolveEnchantmentHolder(level, enchKey);
        if (holder == null) return 0;
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }
    *///?}

    /**
     * Кросс-версионная установка уровня зачарования (обновляет или добавляет).
     * Скрывает работу с Map в 1.20.1 и ItemEnchantments.Mutable в 1.21.1.
     * {@code level <= 0} УДАЛЯЕТ зачарование на обеих версиях (паритет: на 1.20.1
     * {@code map.put(ench, 0)} писал бы уровень 0 в NBT, на 1.21.1 Mutable#set сам удаляет).
     */
    public static void setEnchantmentLevel(ItemStack stack, LevelAccessor level, String enchName, int enchLevel) {
        //? if < 1.21.1 {
        setEnchantmentLevel(stack, level, resolveEnchantment(enchName), enchLevel);
        //?} else {
        /*net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(enchName);
        if (id == null) return;
        setEnchantmentLevel(stack, level, net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.ENCHANTMENT, id), enchLevel);
        *///?}
    }

    /**
     * Типизированная установка уровня зачарования: {@code Enchantment} на 1.20.1,
     * {@code ResourceKey<Enchantment>} на 1.21.1 (см. типизированный getEnchantmentLevel).
     * {@code level <= 0} удаляет зачарование.
     */
    //? if < 1.21.1 {
    public static void setEnchantmentLevel(ItemStack stack, LevelAccessor level, net.minecraft.world.item.enchantment.Enchantment ench, int enchLevel) {
        if (ench == null) return;
        java.util.Map<net.minecraft.world.item.enchantment.Enchantment, Integer> map = new java.util.HashMap<>(net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack));
        if (enchLevel <= 0) {
            map.remove(ench);
        } else {
            map.put(ench, enchLevel);
        }
        net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(map, stack);
    }
    //?} else {
    /*public static void setEnchantmentLevel(ItemStack stack, LevelAccessor level,
            net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchKey, int enchLevel) {
        var holder = resolveEnchantmentHolder(level, enchKey);
        if (holder == null) return;
        net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(stack, m -> m.set(holder, enchLevel));
    }
    *///?}

    /**
     * Кросс-версионное удаление зачарования — частный случай установки
     * уровня {@code 0} (setEnchantmentLevel удаляет при level <= 0 на обеих версиях).
     */
    public static void removeEnchantment(ItemStack stack, LevelAccessor level, String enchName) {
        setEnchantmentLevel(stack, level, enchName, 0);
    }

    //? if < 1.21.1 {
    /** Единственный lookup зачарования на 1.20.1: tryParse (не бросает), null = нет такого. */
    private static net.minecraft.world.item.enchantment.Enchantment resolveEnchantment(String enchName) {
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(enchName);
        return id == null ? null : net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(id);
    }
    //?} else {
    /*// Единственный lookup зачарования на 1.21.1: по ResourceKey (типизированная форма),
    // string-форма оборачивается в ResourceKey.create (tryParse, не бросает), null = нет такого.
    private static net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> resolveEnchantmentHolder(
            LevelAccessor level, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchKey) {
        // null-защита: 1.20.1-ветки level игнорируют, сюда мог прийти null с обеих.
        if (level == null || enchKey == null) return null;
        net.minecraft.core.Registry<net.minecraft.world.item.enchantment.Enchantment> registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        return registry.getHolder(enchKey).orElse(null);
    }
    *///?}

    /**
     * Кросс-версионное получение максимального размера стака для предмета (Item).
     * 1.20.1: item.getMaxStackSize()
     * 1.21.1: item.getDefaultMaxStackSize()
     */
    public static int getItemMaxStackSize(net.minecraft.world.item.Item item) {
        //? if < 1.21.1 {
        return item.getMaxStackSize();
        //?} else {
        /*return item.getDefaultMaxStackSize();
        *///?}
    }

    // =====================================================================================
    //  Еда (FoodProperties), музыкальные диски, топливо, tooltip Level.
    // =====================================================================================

    /** Кросс-версионная проверка на съедобность предмета. */
    public static boolean isEdible(ItemStack stack) {
        if (stack.isEmpty()) return false;
        //? if < 1.21.1 {
        return stack.getItem().isEdible();
        //?} else {
        /*return stack.has(net.minecraft.core.component.DataComponents.FOOD);
        *///?}
    }

    public static boolean isEdible(net.minecraft.world.item.Item item) {
        //? if < 1.21.1 {
        return item.isEdible();
        //?} else {
        /*return item.components().has(net.minecraft.core.component.DataComponents.FOOD);
        *///?}
    }

    /**
     * A jukebox-playable music disc.
     *
     * <p>1.21.1 removed {@code RecordItem}: playback is driven by the {@code jukebox_playable}
     * component pointing at a {@code JukeboxSong} datapack entry. {@code songId} names the entry
     * under {@code data/hbm_m/jukebox_song/}.</p>
     */
    public static net.minecraft.world.item.Item createRecordItem(int comparatorValue, Object sound, net.minecraft.world.item.Item.Properties properties, int lengthInSeconds, String songId) {
        //? if < 1.21.1 {
        return new net.minecraft.world.item.RecordItem(comparatorValue, (net.minecraft.sounds.SoundEvent) sound, properties, lengthInSeconds * 20);
        //?} else {
        /*return new net.minecraft.world.item.Item(jukeboxProperties(properties, songId));
        *///?}
    }

    /** Adds the jukebox_playable component on 1.21.1; a no-op on older versions. */
    public static net.minecraft.world.item.Item.Properties jukeboxProperties(net.minecraft.world.item.Item.Properties properties, String songId) {
        //? if < 1.21.1 {
        return properties;
        //?} else {
        /*if (songId == null || songId.isEmpty()) {
            return properties;
        }
        return properties.jukeboxPlayable(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.JUKEBOX_SONG,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, songId)));
        *///?}
    }

    /** Создаёт билдер еды и применяет кросс-версионный параметр насыщения (saturation). */
    public static net.minecraft.world.food.FoodProperties.Builder foodBuilder(int nutrition, float saturation) {
        net.minecraft.world.food.FoodProperties.Builder builder = new net.minecraft.world.food.FoodProperties.Builder().nutrition(nutrition);
        //? if < 1.21.1 {
        return builder.saturationMod(saturation);
        //?} else {
        /*return builder.saturationModifier(saturation);
        *///?}
    }

    /** Кросс-версионное добавление эффекта (делегат с amplifier=0). */
    public static net.minecraft.world.food.FoodProperties.Builder addFoodEffect(
            net.minecraft.world.food.FoodProperties.Builder builder,
            Object effect, int duration, float probability) {
        return addFoodEffect(builder, effect, duration, 0, probability);
    }

    /**
     * Кросс-версионное добавление эффекта с усилителем (amplifier).
     *
     * <p>Эффект разрешается ЛЕНИВО (в момент поедания), а не при вызове: call-сайты
     * ({@code ModFoods}) работают в static-init до регистрации эффектов мода.
     * Разрешение — через {@link EffectHooks#resolve}: на 1.21.1 RegistrySupplier
     * обязан заворачиваться в реальный {@code Holder.Reference}.
     */
    public static net.minecraft.world.food.FoodProperties.Builder addFoodEffect(
            net.minecraft.world.food.FoodProperties.Builder builder,
            Object effect, int duration, int amplifier, float probability) {
        return builder.effect(() -> {
            Object resolved = EffectHooks.resolve(effect);
            //? if < 1.21.1 {
            return new net.minecraft.world.effect.MobEffectInstance((net.minecraft.world.effect.MobEffect) resolved, duration, amplifier);
            //?} else {
            /*return new net.minecraft.world.effect.MobEffectInstance((net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) resolved, duration, amplifier);
            *///?}
        }, probability);
    }

    /**
     * Помечает еду как мясо (на 1.21.1 игнорируется в коде, используйте теги
     * minecraft:meat / minecraft:wolf_food).
     */
    public static net.minecraft.world.food.FoodProperties.Builder setMeat(net.minecraft.world.food.FoodProperties.Builder builder) {
        //? if < 1.21.1 {
        return builder.meat();
        //?} else {
        /*return builder;
        *///?}
    }

    /**
     * Время горения предмета как печного топлива (тики), 0 = не топливо.
     * Учитывает ванильную карту топлива и переопределение {@code getBurnTime} на предмете.
     */
    public static int getFuelBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        //? if forge {
        return Math.max(0, net.minecraftforge.common.ForgeHooks.getBurnTime(stack, net.minecraft.world.item.crafting.RecipeType.SMELTING));
        //?} elif neoforge {
        /*int t = stack.getItem().getBurnTime(stack, net.minecraft.world.item.crafting.RecipeType.SMELTING);
        if (t < 0) {
            t = net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
        }
        return Math.max(0, t);
        *///?}
    }

    /**
     * Извлекает {@link Level} из второго аргумента {@code appendHoverText} (кросс-версионно).
     * 1.20.1 — сам {@code Level}; 1.21.1 — {@code Item.TooltipContext} (Neo-патч даёт level()).
     *
     * @param levelOrContext на 1.20.1 — {@link Level}; на 1.21.1 — {@code Item.TooltipContext}
     * @return {@link Level} или {@code null}, если контекст без level
     */
    public static Level tooltipLevel(Object levelOrContext) {
        //? if < 1.21.1 {
        return (Level) levelOrContext;
        //?} else {
        /*if (levelOrContext instanceof net.minecraft.world.item.Item.TooltipContext ctx) {
            return ctx.level();
        }
        return null;
        *///?}
    }
}