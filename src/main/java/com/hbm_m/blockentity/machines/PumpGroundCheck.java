package com.hbm_m.blockentity.machines;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared ground-water check for the pump family (both steam and electric variants), 1:1 port of
 * {@code TileEntityMachinePumpBase#checkGround}. Factored out into a static helper (rather than
 * living on a common base class) because the electric variant needs to extend
 * {@code BaseMachineBlockEntity} for its FE receiving instead of {@link PumpBlockEntity}.
 */
public final class PumpGroundCheck {

    public static final int GROUND_HEIGHT = 70;
    public static final int GROUND_DEPTH = 4;
    /** NTM Space {@code TileEntityMachinePumpBase.nonWaterDebuff}: andere Wassertabellen als Wasser pumpen 100x langsamer. */
    public static final int NON_WATER_DEBUFF = 100;

    private static final Set<Block> VALID_GROUND = new HashSet<>();

    private PumpGroundCheck() {}

    private static void init() {
        if (!VALID_GROUND.isEmpty()) return;
        VALID_GROUND.add(Blocks.GRASS_BLOCK);
        VALID_GROUND.add(Blocks.DIRT);
        VALID_GROUND.add(Blocks.SAND);
        VALID_GROUND.add(Blocks.MYCELIUM);
        VALID_GROUND.add(com.hbm_m.block.ModBlocks.WASTE_EARTH.get());
        VALID_GROUND.add(com.hbm_m.block.ModBlocks.DIRT_DEAD.get());
        VALID_GROUND.add(com.hbm_m.block.ModBlocks.DIRT_OILY.get());
        VALID_GROUND.add(com.hbm_m.block.ModBlocks.SAND_DIRTY.get());
        VALID_GROUND.add(com.hbm_m.block.ModBlocks.SAND_DIRTY_RED.get());
    }

    public static boolean check(Level level, BlockPos pos) {
        init();
        if (!level.dimensionType().hasSkyLight()) return false;
        // NTM Space: im Orbit (WorldProviderOrbit) und ohne Wassertabelle (CBT_Water) kein Pumpen - ohne Space-Addon
        // gibt es immer die Wassertabelle "Wasser"
        if (com.hbm_m.api.space.SpaceApi.waterTableFluid(level, com.hbm_m.inventory.fluid.ModFluids.WATER.getSource()) == null) return false;

        int valid = 0, invalid = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y >= -GROUND_DEPTH; y--) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos check = pos.offset(x, y, z);
                    BlockState state = level.getBlockState(check);
                    if (y == -1 && !state.isRedstoneConductor(level, check)) return false;
                    if (VALID_GROUND.contains(state.getBlock())
                            || com.hbm_m.api.space.SpaceResources.isExtraPumpGround(state.getBlock())) valid++;
                    else invalid++;
                }
            }
        }
        return valid >= invalid;
    }

    /**
     * NTM Space {@code checkGround}: {@code water.setTankType(table.fluid)} - der Pumpentank folgt der Wassertabelle des
     * Koerpers. Ohne Space-Addon ist das immer HBM-Wasser (Tank bleibt unveraendert).
     */
    public static void applyWaterTable(Level level, com.hbm_m.inventory.fluid.tank.FluidTank water) {
        if (!com.hbm_m.api.space.SpaceApi.isInstalled()) return;
        net.minecraft.world.level.material.Fluid table =
                com.hbm_m.api.space.SpaceApi.waterTableFluid(level, com.hbm_m.inventory.fluid.ModFluids.WATER.getSource());
        if (table != null && table != water.getTankType()) water.setTankType(table);
    }

    /** NTM Space: {@code water.getTankType() == Fluids.WATER ? speed : speed / nonWaterDebuff}. */
    public static int pumpSpeed(net.minecraft.world.level.material.Fluid tankType, int speed) {
        if (!com.hbm_m.api.space.SpaceApi.isInstalled()) return speed;
        return com.hbm_m.api.fluids.VanillaFluidEquivalence.isWater(tankType) ? speed : speed / NON_WATER_DEBUFF;
    }
}
