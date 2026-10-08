package com.hbm_m.platform;

//? if < 1.21.1 {
import net.minecraft.core.BlockSource;
//?} else {
/*import net.minecraft.core.dispenser.BlockSource;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Versionsfassade fuer den Werfer-Kontext {@code BlockSource} (Phase B, Agent C).
 * 1.20.1: Interface {@code net.minecraft.core.BlockSource} ({@code getLevel/getPos/getBlockState/x/y/z}).
 * 1.21.1: Record {@code net.minecraft.core.dispenser.BlockSource} ({@code level/pos/state/center}).
 * Aufrufer weichen nur die Import-Zeile von {@code BlockSource}.
 */
public final class DispenseHooks {
    private DispenseHooks() {}

    public static ServerLevel level(BlockSource source) {
        //? if < 1.21.1 {
        return source.getLevel();
        //?} else {
        /*return source.level();
        *///?}
    }

    public static BlockPos pos(BlockSource source) {
        //? if < 1.21.1 {
        return source.getPos();
        //?} else {
        /*return source.pos();
        *///?}
    }

    public static BlockState state(BlockSource source) {
        //? if < 1.21.1 {
        return source.getBlockState();
        //?} else {
        /*return source.state();
        *///?}
    }

    /** Blockmitte X ({@code source.x()}). */
    public static double x(BlockSource source) {
        //? if < 1.21.1 {
        return source.x();
        //?} else {
        /*return source.center().x();
        *///?}
    }

    public static double y(BlockSource source) {
        //? if < 1.21.1 {
        return source.y();
        //?} else {
        /*return source.center().y();
        *///?}
    }

    public static double z(BlockSource source) {
        //? if < 1.21.1 {
        return source.z();
        //?} else {
        /*return source.center().z();
        *///?}
    }
}
