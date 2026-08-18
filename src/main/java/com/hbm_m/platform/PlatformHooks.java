package com.hbm_m.platform;

import java.nio.file.Path;
import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import dev.architectury.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import com.hbm_m.lib.RefStrings;

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
        //? if < 1.21.1 {
        return stack.getTag();
        //?} else {
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

    public static boolean hasItemTag(ItemStack stack) {
        //? if < 1.21.1 {
        return stack.hasTag();
        //?} else {
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
        //? if < 1.21.1 {
        editor.accept(stack.getOrCreateTag());
        //?} else {
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
        //? if < 1.21.1 {
        stack.setTag(tag);
        //?} else {
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
     * Заменяет {@code NbtUtils.readBlockPos(tag.getCompound(key))}.
     * 1.21.1: NbtUtils.readBlockPos(CompoundTag, String) возвращает Optional.
     */
    public static net.minecraft.core.BlockPos readBlockPos(net.minecraft.nbt.CompoundTag tag, String key) {
        //? if < 1.21.1 {
        return net.minecraft.nbt.NbtUtils.readBlockPos(tag.getCompound(key));
        //?} else {
        /*return net.minecraft.nbt.NbtUtils.readBlockPos(tag, key).orElse(net.minecraft.core.BlockPos.ZERO);
        *///?}
    }

    /**
     * Кросс-версионная проверка на съедобность предмета.
     */
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
     * Кросс-версионное проигрывание звука (скрывает SoundEvent vs Holder<SoundEvent>).
     */
    public static void playSound(Level level, net.minecraft.core.BlockPos pos, Object sound, net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        net.minecraft.sounds.SoundEvent se = null;
        if (sound instanceof net.minecraft.core.Holder<?> holder) {
            se = (net.minecraft.sounds.SoundEvent) holder.value();
        } else if (sound instanceof net.minecraft.sounds.SoundEvent s) {
            se = s;
        }
        if (se != null) {
            level.playSound(null, pos, se, source, volume, pitch);
        }
    }

    /**
     * Кросс-версионная фабрика музыкальных пластинок.
     */
    public static net.minecraft.world.item.Item createRecordItem(int comparatorValue, Object sound, net.minecraft.world.item.Item.Properties properties, int lengthInSeconds) {
        //? if < 1.21.1 {
        return new net.minecraft.world.item.RecordItem(comparatorValue, (net.minecraft.sounds.SoundEvent) sound, properties, lengthInSeconds * 20);
        //?} else {
        /*return new net.minecraft.world.item.Item(properties);
        *///?}
    }

    public static boolean isSameItemSameTags(ItemStack a, ItemStack b) {
        //? if < 1.21.1 {
        return ItemStack.isSameItemSameTags(a, b);
        //?} else {
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
        //? if < 1.21.1 {
        return ItemStack.of(tag);
        //?} else {
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
        //? if < 1.21.1 {
        return stack.save(tag);
        //?} else {
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
    //  Best-effort HolderLookup.Provider для item-NBT, когда Level/Player недоступны
    //  (например статические Item-методы getName/tooltip без контекста Level).
    //  ONLY 1.21.1 — на 1.20.1 stack.save(CompoundTag)/ItemStack.of(CompoundTag) провайдер не требуют.
    //  Берётся из клиентского Level (tooltip/render — основной call-site таких Item-методов).
    //  На dedicated server возвращает null — call-site'ы ItemAssemblyTemplate деградируют в EMPTY.
    // =====================================================================================

    //? if >= 1.21.1 {
    /*//? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    //?} elif neoforge {
    /^@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    ^///?}
    public static net.minecraft.core.HolderLookup.Provider clientProvider() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level != null) return mc.level.registryAccess();
        if (mc.getConnection() != null) return mc.getConnection().registryAccess();
        return null;
    }
    *///?}

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
        //? if < 1.21.1 {
        return (Level) levelOrContext;
        //?} else {
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
        return Platform.getConfigFolder();
    }

    // =====================================================================================
    //  AttributeModifier constructors bridge.
    //
    //  1.20.1: (UUID, String, double, Operation) и (String, double, Operation).
    //  1.21.1: UUID-конструктор deprecated (но работает с warning), String-only
    //          конструктор удалён — нужно (ResourceLocation, double, Operation).
    //  Хук позволяет ItemArmorMod не ветвиться stonecutter'ом.
    // =====================================================================================

    /**
     * Создание {@link AttributeModifier} с UUID-идентификатором.
     * Заменяет {@code new AttributeModifier(uuid, name, value, operation)} на 1.20.1.
     *
     * <p>На 1.21.1 UUID-конструктор deprecated (но работоспособен), хук делегирует
     * в него, чтобы сохранить UUID-семантику привязки модификатора к слоту брони.
     */

    public static AttributeModifier attributeModifier(
            java.util.UUID uuid, String name, double value, AttributeModifier.Operation operation) {
        //? if < 1.21.1 {
        return new AttributeModifier(uuid, name, value, operation);
        //?} else {
        /*// 1.21.1: UUID-конструктор удалён. Привязка модификатора к слоту брони теперь по
        // ResourceLocation (производное от UUID, чтобы остаться уникальным и стабильным).
        return new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, 
                        "am_" + uuid.toString().replace('-', '_')),
                value, operation);
        *///?}
    }

    /**
     * Создание {@link AttributeModifier} с именем-идентификатором (без UUID).
     * Заменяет {@code new AttributeModifier(name, value, operation)} на 1.20.1.
     *
     * <p>На 1.21.1 string-only конструктор удалён — нужно {@code ResourceLocation}.
     * Хук стабильно генерирует RL из имени с {@code hbm_m:} namespace.
     */
    public static AttributeModifier attributeModifier(
            String name, double value, AttributeModifier.Operation operation) {
        //? if < 1.21.1 {
        return new AttributeModifier(name, value, operation);
        //?} else {
        /*return new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, 
                        name.toLowerCase(java.util.Locale.ROOT).replace(' ', '_').replace(':', '.')),
                value, operation);
        *///?}
    }

    // =====================================================================================
    //  Model Registration & Handling Bridge
    //
    //  1.20.1: ModelResourceLocation extends ResourceLocation.
    //  1.21.1: ModelResourceLocation is a record (does NOT extend ResourceLocation).
    //          RegisterGeometryLoaders требует ResourceLocation, RegisterAdditional требует ModelResourceLocation.
    // =====================================================================================

    /**
     * Создание ModelResourceLocation. Возвращает Object, чтобы не зависеть от изменения
     * иерархии наследования.
     */
    public static Object createModelLocation(ResourceLocation id, String variant) {
        return new net.minecraft.client.resources.model.ModelResourceLocation(id, variant);
    }

    /**
     * Извлечение ResourceLocation (ID) из ModelResourceLocation.
     */
    public static ResourceLocation getModelId(Object modelResourceLocation) {
        //? if < 1.21.1 {
        return (ResourceLocation) modelResourceLocation;
        //?} else {
        /*return ((net.minecraft.client.resources.model.ModelResourceLocation) modelResourceLocation).id();
        *///?}
    }

    /**
     * Регистрация дополнительной standalone-модели.
     */
    public static void registerAdditionalModel(Object event, ResourceLocation loc) {
        //? if < 1.21.1 {
        ((net.minecraftforge.client.event.ModelEvent.RegisterAdditional) event).register(loc);
        //?} else {
        /*((net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional) event).register(
                new net.minecraft.client.resources.model.ModelResourceLocation(loc, "standalone"));
        *///?}
    }

    /**
     * Регистрация Geometry Loader.
     */
    public static void registerGeometryLoader(Object event, String name, Object loader) {
        //? if < 1.21.1 {
        ((net.minecraftforge.client.event.ModelEvent.RegisterGeometryLoaders) event).register(
                name, (net.minecraftforge.client.model.geometry.IGeometryLoader<?>) loader);
        //?} else {
        /*((net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders) event).register(
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, name),
                (net.neoforged.neoforge.client.model.geometry.IGeometryLoader<?>) loader);
        *///?}
    }

    /**
     * Получение BakedModel из ModelManager по ResourceLocation.
     * Используйте это в рендерерах (EntityRenderer/BlockEntityRenderer), чтобы не зависеть
     * от изменения сигнатуры getModel() на 1.21.1.
     */
    public static net.minecraft.client.resources.model.BakedModel getModel(
            net.minecraft.client.resources.model.ModelManager manager, ResourceLocation loc) {
        //? if < 1.21.1 {
        return manager.getModel(loc);
        //?} else {
        /*return manager.getModel(new net.minecraft.client.resources.model.ModelResourceLocation(loc, "standalone"));
        *///?}
    }
    // =====================================================================================
    //  VertexFormatElement Bridge
    //
    //  В 1.21.1 класс стал record и требует `id` первым параметром. 
    //  В 1.20.1 это обычный класс без id.
    // =====================================================================================

    /**
     * Создание VertexFormatElement (мост между 1.20.1 и 1.21.1).
     */

    public static com.mojang.blaze3d.vertex.VertexFormatElement createVertexFormatElement(
            int index, com.mojang.blaze3d.vertex.VertexFormatElement.Type type,
            com.mojang.blaze3d.vertex.VertexFormatElement.Usage usage, int count) {
        //? if < 1.21.1 {
        return new com.mojang.blaze3d.vertex.VertexFormatElement(index, type, usage, count);
        //?} else {
        /*return new com.mojang.blaze3d.vertex.VertexFormatElement(0, index, type, usage, count);
        *///?}
    }

    // =====================================================================================
    //  Food Properties Builder Bridge
    //
    //  Различия версий:
    //  1.20.1: saturationMod(), meat(), effect(Supplier<MobEffectInstance>, float).
    //  1.21.1: saturationModifier(), meat() удалён, effect(MobEffectInstance, float).
    // =====================================================================================

    /** 
     * Создаёт билдер и применяет кросс-версионный параметр насыщения (saturation). 
     */
    public static net.minecraft.world.food.FoodProperties.Builder foodBuilder(int nutrition, float saturation) {
        net.minecraft.world.food.FoodProperties.Builder builder = new net.minecraft.world.food.FoodProperties.Builder().nutrition(nutrition);
        //? if < 1.21.1 {
        return builder.saturationMod(saturation);
        //?} else {
        /*return builder.saturationModifier(saturation);
        *///?}
    }

    /**
     * Кросс-версионное добавление эффекта. 
     * Параметр `effect` передается как Object, чтобы обходить разницу между MobEffect (1.20.1) и Holder<MobEffect> (1.21.1).
     */
    public static net.minecraft.world.food.FoodProperties.Builder addFoodEffect(
            net.minecraft.world.food.FoodProperties.Builder builder,
            Object effect, int duration, float probability) {
        return addFoodEffect(builder, effect, duration, 0, probability);
    }

    /**
     * Кросс-версионное добавление эффекта с усилителем (amplifier).
     */
    public static net.minecraft.world.food.FoodProperties.Builder addFoodEffect(
            net.minecraft.world.food.FoodProperties.Builder builder,
            Object effect, int duration, int amplifier, float probability) {
        //? if < 1.21.1 {
        return builder.effect(() -> new net.minecraft.world.effect.MobEffectInstance((net.minecraft.world.effect.MobEffect) effect, duration, amplifier), probability);
        //?} else {
        /*return builder.effect(new net.minecraft.world.effect.MobEffectInstance((net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) effect, duration, amplifier), probability);
        *///?}
    }

    /** 
     * Помечает еду как мясо (на 1.21.1 игнорируется в коде, используйте теги minecraft:meat / minecraft:wolf_food). 
     */
    public static net.minecraft.world.food.FoodProperties.Builder setMeat(net.minecraft.world.food.FoodProperties.Builder builder) {
        //? if < 1.21.1 {
        return builder.meat();
        //?} else {
        /*return builder;
        *///?}
    }

    // =====================================================================================
    //  Block Construction Helpers
    // =====================================================================================

    /**
     * Кросс-версионное создание DoorBlock (Properties, BlockSetType на 1.20.1 vs BlockSetType, Properties на 1.21.1).
     */
    public static net.minecraft.world.level.block.DoorBlock createDoorBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties props, net.minecraft.world.level.block.state.properties.BlockSetType type) {
        //? if < 1.21.1 {
        return new net.minecraft.world.level.block.DoorBlock(props, type);
        //?} else {
        /*return new net.minecraft.world.level.block.DoorBlock(type, props);
        *///?}
    }

    /**
     * Кросс-версионное создание DropExperienceBlock (Properties на 1.20.1 vs ConstantInt, Properties на 1.21.1).
     */
    public static net.minecraft.world.level.block.DropExperienceBlock createDropExperienceBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties props) {
        //? if < 1.21.1 {
        return new net.minecraft.world.level.block.DropExperienceBlock(props);
        //?} else {
        /*return new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.ConstantInt.of(0), props);
        *///?}
    }

    /**
     * Кросс-версионное создание FlowerBlock (MobEffect, int на 1.20.1 vs Holder<MobEffect>, float на 1.21.1).
     */
    public static net.minecraft.world.level.block.FlowerBlock createFlowerBlock(Object effect, int durationTicks, net.minecraft.world.level.block.state.BlockBehaviour.Properties props) {
        //? if < 1.21.1 {
        return new net.minecraft.world.level.block.FlowerBlock((net.minecraft.world.effect.MobEffect) effect, durationTicks, props);
        //?} else {
        /*return new net.minecraft.world.level.block.FlowerBlock((net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) effect, (float) durationTicks, props);
        *///?}
    }
    // =====================================================================================
    //  Advancement & Entity Hooks
    // =====================================================================================

    /**
     * Кросс-версионная проверка и выдача достижения игроку.
     */
    public static void awardAdvancementIfEligible(net.minecraft.server.level.ServerPlayer player, ResourceLocation id, boolean eligible) {
        if (!eligible) return;
        var server = player.getServer();
        if (server == null) return;
        var advancementManager = server.getAdvancements();
        //? if < 1.21.1 {
        net.minecraft.advancements.Advancement adv = advancementManager.getAdvancement(id);
        if (adv != null) {
            net.minecraft.advancements.AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(adv, criterion);
                }
            }
        }
        //?} else {
        /*net.minecraft.advancements.AdvancementHolder holder = advancementManager.get(id);
        if (holder != null) {
            net.minecraft.advancements.AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(holder, criterion);
                }
            }
        }
        *///?}
    }

    /**
     * Кросс-версионное поджигание сущности.
     */
    public static void setSecondsOnFire(net.minecraft.world.entity.Entity entity, int seconds) {
        //? if < 1.21.1 {
        entity.setSecondsOnFire(seconds);
        //?} else {
        /*entity.igniteForSeconds((float) seconds);
        *///?}
    }

    /**
     * Кросс-платформенная проверка, является ли предмет контейнером жидкости.
     */
    public static boolean isFluidContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        //? if forge {
        return stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        //?} elif neoforge {
        /*return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null;
        *///?}
    }

    // =====================================================================================
    //  BlockEntity NBT Load & Component JSON Bridge
    // =====================================================================================

    /**
     * Кросс-версионная загрузка NBT-данных в BlockEntity.
     */
    public static void loadBlockEntityTag(net.minecraft.world.level.block.entity.BlockEntity be, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        //? if < 1.21.1 {
        be.load(tag);
        //?} else {
        /*be.loadCustomOnly(tag, provider);
        *///?}
    }

    /**
     * Парсинг JSON-строки в Component (учитывает смену сигнатуры Serializer.fromJson в 1.21.1).
     */
    public static net.minecraft.network.chat.Component parseComponentJson(String json, net.minecraft.core.HolderLookup.Provider provider) {
        if (json == null || json.isEmpty()) return null;
        //? if < 1.21.1 {
        return net.minecraft.network.chat.Component.Serializer.fromJson(com.google.gson.JsonParser.parseString(json));
        //?} else {
        /*return net.minecraft.network.chat.Component.Serializer.fromJson(json, provider);
        *///?}
    }

    /**
     * Сериализация Component в JSON-строку.
     */
    public static String componentToJson(net.minecraft.network.chat.Component component, net.minecraft.core.HolderLookup.Provider provider) {
        if (component == null) return null;
        //? if < 1.21.1 {
        return net.minecraft.network.chat.Component.Serializer.toJson(component);
        //?} else {
        /*return net.minecraft.network.chat.Component.Serializer.toJson(component, provider);
        *///?}
    }

    /**
     * Кросс-версионный расчёт отбрасывания взрывом с учётом зачарований защиты.
     * ProtectionEnchantment (1.20.1) -> EnchantmentHelper (1.21.1).
     */
    public static double getExplosionKnockbackAfterDampener(net.minecraft.world.entity.LivingEntity entity, double knockback) {
        //? if < 1.21.1 {
        return net.minecraft.world.item.enchantment.ProtectionEnchantment.getExplosionKnockbackAfterDampener(entity, knockback);
        //?} else {
        /*double res = entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
        return knockback * (1.0 - res);
        *///?}
    }

    // =====================================================================================
    //  Entity riding offset bridge.
    //
    //  1.20.1: Entity.getMyRidingOffset() — высота смещения седока верхом.
    //  1.21.1: переименован в getMyRidingOffset() -> удалён;取代 — rideHeight через
    //          Entity.getPassengersRidingOffset() / снимается getMyRidingOffset.
    // =====================================================================================
    public static double getMyRidingOffset(net.minecraft.world.entity.Entity entity) {
        //? if < 1.21.1 {
        return entity.getMyRidingOffset();
        //?} else {
        /*// 1.21.1: Entity.getPassengersRidingOffset() и getMyRidingOffset() удалены —
        // смещение пассажира теперь через EntityAttachments (getAttachments()).
        // Возвращаем 0.0D как stub (оригинальное поведение по умолчанию для большинства сущностей).
        return 0.0D;
        *///?}
    }

    // =====================================================================================
    //  Blocks renames bridge (1.20.1 -> 1.21.1).
    //   Blocks.GLASS / Blocks.TALL_GRASS etc. перенесены/переименованы.
    // =====================================================================================

    /**
     * Проверка «трава/короткая трава» с учётом ренейма Blocks.GRASS -> Blocks.SHORT_GRASS
     * в 1.21.1 (тот же блок, новый идентификатор).
     */
    public static boolean isGrassBlock(net.minecraft.world.level.block.state.BlockState state) {
        //? if < 1.21.1 {
        return state.is(net.minecraft.world.level.block.Blocks.GRASS);
        //?} else {
        /*return state.is(net.minecraft.world.level.block.Blocks.SHORT_GRASS);
        *///?}
    }

    // =====================================================================================
    //  Blocks.GlassBlock constructor bridge.
    //   1.20.1: net.minecraft.world.level.block.GlassBlock существует.
    //   1.21.1: удалён — GlassBlock.if() помечен deprecated; используйте прямой Block.
    //   Возвращаем Block (базовый класс), поведение стекло-props задаётся Properties.
    // =====================================================================================
    public static net.minecraft.world.level.block.Block createGlassBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties props) {
        //? if < 1.21.1 {
        return new net.minecraft.world.level.block.GlassBlock(props);
        //?} else {
        /*return new net.minecraft.world.level.block.Block(props);
        *///?}
    }
}