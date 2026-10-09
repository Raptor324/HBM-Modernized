package com.hbm_m.platform;

/**
 * Versionsbruecke fuer Block-/BlockEntity-APIs (Phase B, Agent B).
 * Die 1.20.1-Seite jeder Methode ist exakt der bisherige Vanilla/Forge-Aufruf.
 */
public final class BlockHooks {
    private BlockHooks() {}

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
        /*be.loadWithComponents(tag, be.getLevel() != null ? be.getLevel().registryAccess() : PlatformHooks.bestEffortProvider());
        *///?}
    }

    /** {@code be.saveWithoutMetadata()} mit dem Provider aus der Welt der BE (1.21.1). */
    public static net.minecraft.nbt.CompoundTag saveWithoutMetadata(net.minecraft.world.level.block.entity.BlockEntity be) {
        //? if < 1.21.1 {
        return be.saveWithoutMetadata();
        //?} else {
        /*return be.saveWithoutMetadata(be.getLevel() != null ? be.getLevel().registryAccess() : PlatformHooks.bestEffortProvider());
        *///?}
    }

    /** {@code be.saveToItem(stack)} mit dem Provider aus der Welt der BE (1.21.1). */
    public static void saveToItem(net.minecraft.world.level.block.entity.BlockEntity be, net.minecraft.world.item.ItemStack stack) {
        //? if < 1.21.1 {
        be.saveToItem(stack);
        //?} else {
        /*be.saveToItem(stack, be.getLevel() != null ? be.getLevel().registryAccess() : PlatformHooks.bestEffortProvider());
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
}
