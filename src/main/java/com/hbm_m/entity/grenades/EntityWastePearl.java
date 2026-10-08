package com.hbm_m.entity.grenades;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.entity.grenade.EntityWastePearl} ("entity_waste_pearl"): beim Aufprall wird ein 7x7x7-Wuerfel
 * zu einem Drittel mit Fallout bedeckt, die uebrige Luft mit Radongas (dicht oder normal) gefuellt.
 * Im Original gibt es weder Gegenstand noch Spawnstelle, die diese Entity erzeugt.
 */
public class EntityWastePearl extends EntityGrenadeBase {

    public EntityWastePearl(EntityType<? extends EntityWastePearl> type, Level world) {
        super(type, world);
    }

    public EntityWastePearl(Level world) {
        this(ModEntities.WASTE_PEARL.get(), world);
    }

    public EntityWastePearl(Level world, LivingEntity living) {
        super(ModEntities.WASTE_PEARL.get(), world, living);
    }

    public EntityWastePearl(Level world, double x, double y, double z) {
        super(ModEntities.WASTE_PEARL.get(), world, x, y, z);
    }

    @Override
    //? if < 1.21.1 {
    protected void defineSynchedData() { }
    //?} else {
    /*protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }
    *///?}

    @Override
    public void explode() {

        if (!this.level().isClientSide) {
            this.discard();

            int x = (int) Math.floor(getX());
            int y = (int) Math.floor(getY());
            int z = (int) Math.floor(getZ());

            BlockState fallout = ModBlocks.NUCLEAR_FALLOUT.get().defaultBlockState();

            for (int ix = x - 3; ix <= x + 3; ix++) {
                for (int iy = y - 3; iy <= y + 3; iy++) {
                    for (int iz = z - 3; iz <= z + 3; iz++) {

                        BlockPos pos = new BlockPos(ix, iy, iz);
                        BlockState state = level().getBlockState(pos);

                        if (level().random.nextInt(3) == 0 && state.canBeReplaced() && fallout.canSurvive(level(), pos)) {
                            level().setBlock(pos, fallout, 3);
                        } else if (state.isAir()) {

                            if (random.nextBoolean())
                                level().setBlock(pos, ModBlocks.GAS_RADON.get().defaultBlockState(), 3);
                            else
                                level().setBlock(pos, ModBlocks.GAS_RADON_DENSE.get().defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }
}
