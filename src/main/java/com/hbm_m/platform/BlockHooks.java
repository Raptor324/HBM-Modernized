package com.hbm_m.platform;

/**
 * Versionsbruecke fuer Block-/BlockEntity-APIs (Phase B, Agent B).
 * Die 1.20.1-Seite jeder Methode ist exakt der bisherige Vanilla/Forge-Aufruf.
 */
public final class BlockHooks {
    private BlockHooks() {}

    /**
     * {@code BlockBehaviour.Properties.copy(block)} — кросс-версионный (бывший BlockProps):
     * на 1.20.1 Forge копирует через {@code copy(block)}, на 1.21.1 метод депрекейтнут
     * и заменён на {@code ofFullCopy(block)}. 800+ call-сайтов в ModBlocks сведены сюда.
     */
    public static net.minecraft.world.level.block.state.BlockBehaviour.Properties copy(net.minecraft.world.level.block.Block block) {
        //? if < 1.21.1 {
        return net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(block);
        //?} else {
        /*return net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(block);
        *///?}
    }

    /**
     * Отключает спавн частиц разрушения блока (terrain particles): 1.20.1 Forge —
     * {@code noParticlesOnBreak()}, 1.21.1 — ванильный {@code noTerrainParticles()}.
     */
    public static net.minecraft.world.level.block.state.BlockBehaviour.Properties noParticlesOnBreak(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        //? if < 1.21.1 {
        return properties.noParticlesOnBreak();
        //?} else {
        /*return properties.noTerrainParticles();
        *///?}
    }

    /**
     * Nur 1.21.1: Block-Codec-Platzhalter fuer {@code codec()} (seit 1.20.5 abstrakt in BaseEntityBlock,
     * BushBlock, FallingBlock ...). Mod-Bloecke werden nie ueber den Codec (de)serialisiert —
     * der Codec dient nur der Typ-Registrierung (бывший BlockCodecs).
     */
    //? if >= 1.21.1 {
    /*public static <B> com.mojang.serialization.MapCodec<B> unsupportedCodec(Class<B> type) {
        return com.mojang.serialization.MapCodec.unit(() -> {
            throw new UnsupportedOperationException("Kein Codec fuer " + type.getName());
        });
    }
    *///?}

    /**
     * Nur 1.21.1: {@code Block#use(...)} -> {@code useItemOn(...)} — перевод InteractionResult
     * (PASS -> PASS_TO_DEFAULT_BLOCK_INTERACTION, damit Vanilla wie bisher weiterreicht;
     * бывший BlockUseHooks). Muster siehe VERSIONPORT.md.
     */
    //? if >= 1.21.1 {
    /*public static net.minecraft.world.ItemInteractionResult useItemResult(net.minecraft.world.InteractionResult r) {
        return switch (r) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> net.minecraft.world.ItemInteractionResult.SUCCESS;
            case CONSUME -> net.minecraft.world.ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> net.minecraft.world.ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> net.minecraft.world.ItemInteractionResult.FAIL;
            case PASS -> net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }
    *///?}

    /** Schlichter Rieselblock: 1.20.1 {@code new FallingBlock(p)} (abstrakt ab 1.20.3), 1.21.1 {@code ColoredFallingBlock} mit derselben Staubfarbe (-16777216). */
    public static net.minecraft.world.level.block.FallingBlock fallingBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties props) {
        //? if < 1.21.1 {
        return new net.minecraft.world.level.block.FallingBlock(props);
        //?} else {
        /*return new net.minecraft.world.level.block.ColoredFallingBlock(new net.minecraft.util.ColorRGBA(-16777216), props);
        *///?}
    }

    /** Treppe: 1.20.1 Forge-Konstruktor mit Supplier, 1.21.1 nur noch mit fertigem BlockState (Basisblock ist dann schon registriert). */
    public static net.minecraft.world.level.block.StairBlock stairs(java.util.function.Supplier<net.minecraft.world.level.block.state.BlockState> base, net.minecraft.world.level.block.state.BlockBehaviour.Properties props) {
        //? if < 1.21.1 {
        return new net.minecraft.world.level.block.StairBlock(base, props);
        //?} else {
        /*return new net.minecraft.world.level.block.StairBlock(base.get(), props);
        *///?}
    }

    /** {@code Blocks.GRASS} (1.20.1) / {@code Blocks.SHORT_GRASS} (1.21.1). */
    public static net.minecraft.world.level.block.Block shortGrass() {
        //? if < 1.21.1 {
        return net.minecraft.world.level.block.Blocks.GRASS;
        //?} else {
        /*return net.minecraft.world.level.block.Blocks.SHORT_GRASS;
        *///?}
    }

    /** {@code be.load(tag)} - 1.21.1 {@code loadWithComponents(tag, registries)} (voller Ladeweg inkl. Komponenten). */
    public static void loadFull(net.minecraft.world.level.block.entity.BlockEntity be, net.minecraft.nbt.CompoundTag tag) {
        //? if < 1.21.1 {
        be.load(tag);
        //?} else {
        /*be.loadWithComponents(tag, registries(be));
        *///?}
    }

    /** {@code be.saveWithoutMetadata()} mit dem Provider aus der Welt der BE (1.21.1). */
    public static net.minecraft.nbt.CompoundTag saveWithoutMetadata(net.minecraft.world.level.block.entity.BlockEntity be) {
        return saveWithoutMetadata(be, null);
    }

    /** {@code be.saveToItem(stack)} mit dem Provider aus der Welt der BE (1.21.1). */
    public static void saveToItem(net.minecraft.world.level.block.entity.BlockEntity be, net.minecraft.world.item.ItemStack stack) {
        //? if < 1.21.1 {
        be.saveToItem(stack);
        //?} else {
        /*be.saveToItem(stack, registries(be));
        *///?}
    }

    /** Provider der BE-Welt: 1.20.1 {@code null} (nicht noetig), 1.21.1 {@code level.registryAccess()} bzw. best effort. */
    public static net.minecraft.core.HolderLookup.Provider registries(net.minecraft.world.level.block.entity.BlockEntity be) {
        //? if < 1.21.1 {
        return null;
        //?} else {
        /*return be.getLevel() != null ? be.getLevel().registryAccess() : PlatformHooks.bestEffortProvider();
        *///?}
    }

    /** {@code new AABB(BlockPos a, BlockPos b)} (Ecke zu Ecke, ohne +1) - der Konstruktor entfaellt in 1.21.1. */
    public static net.minecraft.world.phys.AABB aabb(net.minecraft.core.BlockPos a, net.minecraft.core.BlockPos b) {
        //? if < 1.21.1 {
        return new net.minecraft.world.phys.AABB(a, b);
        //?} else {
        /*return new net.minecraft.world.phys.AABB(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
        *///?}
    }

    /** {@code ClientboundBlockEntityDataPacket.create(be, b -> tag)} - 1.21.1 erwartet {@code (be, registryAccess) -> tag}. */
    public static net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket dataPacket(net.minecraft.world.level.block.entity.BlockEntity be, net.minecraft.nbt.CompoundTag tag) {
        //? if < 1.21.1 {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(be, b -> tag);
        //?} else {
        /*return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(be, (b, ra) -> tag);
        *///?}
    }

    /** {@code registries}, falls gesetzt, sonst der Provider der BE-Welt (fuer readNbtData(tag, null) aus Client-Paketen). 1.20.1: unveraendert durchgereicht. */
    public static net.minecraft.core.HolderLookup.Provider registriesOr(net.minecraft.core.HolderLookup.Provider registries, net.minecraft.world.level.block.entity.BlockEntity be) {
        //? if < 1.21.1 {
        return registries;
        //?} else {
        /*return registries != null ? registries : registries(be);
        *///?}
    }

    /** Provider einer Welt: 1.20.1 {@code null}, 1.21.1 {@code level.registryAccess()}. */
    public static net.minecraft.core.HolderLookup.Provider registries(net.minecraft.world.level.Level level) {
        //? if < 1.21.1 {
        return null;
        //?} else {
        /*return level != null ? level.registryAccess() : PlatformHooks.bestEffortProvider();
        *///?}
    }

    // =====================================================================================
    //  Создание блоков (кросс-версионные фабрики, бывшие PlatformHooks.create*).
    // =====================================================================================

    /** Кросс-версионное создание DoorBlock (Properties, BlockSetType на 1.20.1 vs BlockSetType, Properties на 1.21.1). */
    public static net.minecraft.world.level.block.DoorBlock door(net.minecraft.world.level.block.state.BlockBehaviour.Properties props, net.minecraft.world.level.block.state.properties.BlockSetType type) {
        //? if < 1.21.1 {
        return new net.minecraft.world.level.block.DoorBlock(props, type);
        //?} else {
        /*return new net.minecraft.world.level.block.DoorBlock(type, props);
        *///?}
    }

    /** Кросс-версионное создание DropExperienceBlock (Properties на 1.20.1 vs ConstantInt, Properties на 1.21.1). */
    public static net.minecraft.world.level.block.DropExperienceBlock dropExperience(net.minecraft.world.level.block.state.BlockBehaviour.Properties props) {
        //? if < 1.21.1 {
        return new net.minecraft.world.level.block.DropExperienceBlock(props);
        //?} else {
        /*return new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.ConstantInt.of(0), props);
        *///?}
    }

    /**
     * Кросс-версионное создание FlowerBlock. Эффект — {@code Object} (MobEffect /
     * RegistrySupplier / Holder), разрешается через {@link EffectHooks#resolve}.
     *
     * <p>Семантика duration повторяет 1.20.1: для обычных эффектов — СЕКУНДЫ
     * (ваниль умножает на 20), для мгновенных (isInstantenous) — тики.
     * На 1.21.1 (конструктор принимает float-секунды, всегда ×20) мгновенные
     * эффекты конвертируются тики→секунды, чтобы stew-длительность совпала.
     */
    public static net.minecraft.world.level.block.FlowerBlock flower(Object effect, int duration, net.minecraft.world.level.block.state.BlockBehaviour.Properties props) {
        //? if < 1.21.1 {
        net.minecraft.world.effect.MobEffect me = EffectHooks.resolve(effect);
        if (me == null) throw new IllegalArgumentException("BlockHooks.flower: unresolved effect " + effect);
        return new net.minecraft.world.level.block.FlowerBlock(me, duration, props);
        //?} else {
        /*net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> h = EffectHooks.resolve(effect);
        if (h == null) throw new IllegalArgumentException("BlockHooks.flower: unresolved effect " + effect);
        float seconds = h.value().isInstantenous() ? duration / 20.0F : (float) duration;
        return new net.minecraft.world.level.block.FlowerBlock(h, seconds, props);
        *///?}
    }

    /**
     * Стекло: 1.20.1 {@code GlassBlock} (extends AbstractGlassBlock -> HalfTransparentBlock:
     * skipRendering, shade 1.0, skylight, empty visual shape); 1.21.1 — переименован
     * в {@code TransparentBlock} (тот же конструктор Properties).
     */
    public static net.minecraft.world.level.block.Block glass(net.minecraft.world.level.block.state.BlockBehaviour.Properties props) {
        //? if < 1.21.1 {
        return new net.minecraft.world.level.block.GlassBlock(props);
        //?} else {
        /*return new net.minecraft.world.level.block.TransparentBlock(props);
        *///?}
    }

    /** Проверка «трава/короткая трава» (ренейм Blocks.GRASS -> SHORT_GRASS в 1.21.1). */
    public static boolean isShortGrass(net.minecraft.world.level.block.state.BlockState state) {
        return state != null && state.is(shortGrass());
    }

    /**
     * Кросс-версионная загрузка ПОЛЬЗОВАТЕЛЬСКОГО NBT в BlockEntity.
     * 1.20.1: {@code be.load(tag)}; 1.21.1: {@code be.loadCustomOnly(tag, registries)}.
     * Для полной загрузки включая компоненты — {@link #loadFull(BlockEntity, CompoundTag)}.
     */
    public static void loadCustomOnly(net.minecraft.world.level.block.entity.BlockEntity be, net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        //? if < 1.21.1 {
        be.load(tag);
        //?} else {
        /*be.loadCustomOnly(tag, registriesOr(provider, be));
        *///?}
    }

    /** {@code be.saveWithoutMetadata()} с явным провайдером (1.21.1; null → провайдер мира BE). */
    public static net.minecraft.nbt.CompoundTag saveWithoutMetadata(net.minecraft.world.level.block.entity.BlockEntity be, net.minecraft.core.HolderLookup.Provider provider) {
        //? if < 1.21.1 {
        return be.saveWithoutMetadata();
        //?} else {
        /*return be.saveWithoutMetadata(registriesOr(provider, be));
        *///?}
    }

    /**
     * Заменяет {@code NbtUtils.readBlockPos(tag.getCompound(key))} с backward-compat:
     * на 1.21.1 нативный {@code NbtUtils.readBlockPos(CompoundTag, String)} читает int[3],
     * а старые сейвы 1.20.1 хранят позицию как {@code CompoundTag {X, Y, Z}} — проверяем оба.
     *
     * @return parsed BlockPos, or {@code null} if the key is absent or unreadable
     */
    @org.jetbrains.annotations.Nullable
    public static net.minecraft.core.BlockPos readBlockPos(net.minecraft.nbt.CompoundTag tag, String key) {
        //? if < 1.21.1 {
        if (!tag.contains(key)) return null;
        net.minecraft.nbt.CompoundTag sub = tag.getCompound(key);
        if (sub.isEmpty()) return null;
        return net.minecraft.nbt.NbtUtils.readBlockPos(sub);
        //?} else {
        /*// Attempt 1: 1.21.1 native format (IntArrayTag stored directly under 'key')
        java.util.Optional<net.minecraft.core.BlockPos> result = net.minecraft.nbt.NbtUtils.readBlockPos(tag, key);
        if (result.isPresent()) return result.get();
        // Attempt 2: legacy 1.20.1 compound format {X, Y, Z} — for old worlds migrated to 1.21.1
        if (tag.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            net.minecraft.nbt.CompoundTag sub = tag.getCompound(key);
            if (sub.contains("X") && sub.contains("Y") && sub.contains("Z")) {
                return new net.minecraft.core.BlockPos(sub.getInt("X"), sub.getInt("Y"), sub.getInt("Z"));
            }
        }
        return null;
        *///?}
    }

    /** Заменяет {@code tag.put(key, NbtUtils.writeBlockPos(pos))}. */
    public static void writeBlockPos(net.minecraft.nbt.CompoundTag tag, String key, net.minecraft.core.BlockPos pos) {
        tag.put(key, net.minecraft.nbt.NbtUtils.writeBlockPos(pos));
    }

    // Приватный конструктор FallingBlockEntity (см. newFallingBlock). MethodHandle вместо
    // Constructor.newInstance: без бокса double-аргументов и без varargs-массива на каждый
    // спавн осыпи. ОБЯЗАТЕЛЬНО static final: C2 сворачивает invokeExact в прямой вызов
    // только для константного хендла — из volatile-поля каждый вызов шёл через полный
    // диспетчер с проверкой MethodType.
    private static final java.lang.invoke.MethodHandle FALLING_BLOCK_CTOR;
    static {
        try {
            // Тот же уровень доступа, что раньше давал ctor.setAccessible(true):
            // privateLookupIn дает прямой доступ к приватному конструктору
            // (обе версии — unnamed module, проверка доступа идентична).
            java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.privateLookupIn(
                    net.minecraft.world.entity.item.FallingBlockEntity.class,
                    java.lang.invoke.MethodHandles.lookup());
            FALLING_BLOCK_CTOR = lookup.findConstructor(net.minecraft.world.entity.item.FallingBlockEntity.class,
                    java.lang.invoke.MethodType.methodType(void.class,
                            net.minecraft.world.level.Level.class,
                            double.class, double.class, double.class,
                            net.minecraft.world.level.block.state.BlockState.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Falling entity «как в ванильном {@code FallingBlockEntity.fall}», но без его
     * Level.setBlock. Конструктор приватен на обеих версиях — зовём через
     * MethodHandle (статический fall внутри делает Level.setBlock, который Sable
     * заворачивает с синхронной догрузкой соседних чанков — недопустимо в
     * массовых спавнах осыпи).
     */
    public static net.minecraft.world.entity.item.FallingBlockEntity newFallingBlock(
            net.minecraft.world.level.Level level, double x, double y, double z,
            net.minecraft.world.level.block.state.BlockState state) {
        try {
            return (net.minecraft.world.entity.item.FallingBlockEntity)
                    FALLING_BLOCK_CTOR.invokeExact(level, x, y, z, state);
        } catch (Throwable e) {
            throw new RuntimeException("FallingBlockEntity private ctor", e);
        }
    }
}
