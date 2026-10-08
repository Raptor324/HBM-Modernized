package com.hbm_m.entity.missile;

import com.hbm_m.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Прототип баллистической ракеты (Tier 0, missile_test).
 *
 * 1:1 EntityMissileTest: beim Einschlag wird eine Kugel (r=50) zu Sellafit verglast.
 */
public class MissileTestEntity extends MissileBaseEntity {

    public MissileTestEntity(EntityType<? extends MissileTestEntity> type, Level level) {
        super(type, level);
    }

    public MissileTestEntity(Level level) {
        this(ModEntities.MISSILE_TEST.get(), level);
    }

    @Override
    protected void onMissileImpact(BlockPos pos) {
        if (this.level().isClientSide) {
            return;
        }
        // 1:1 EntityMissileTest: Kugel r=50 - feste Bloecke werden Sellafit (Farbstufe nach Abstand),
        // alles andere Luft. Port: sellafield_slaked + COLOR_LEVEL = min(meta, 10) wie RadLavaBlock.
        Level world = this.level();
        int x = (int) Math.floor(this.getX());
        int y = (int) Math.floor(this.getY());
        int z = (int) Math.floor(this.getZ());
        int range = 50;
        net.minecraft.world.level.block.Block slaked = com.hbm_m.block.ModBlocks.SELLAFIELD_SLAKED.get();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

        for (int iX = -range; iX <= range; iX++) {
            for (int iY = -range; iY <= range; iY++) {
                for (int iZ = -range; iZ <= range; iZ++) {
                    double dist = Math.sqrt(iX * iX + iY * iY + iZ * iZ);
                    if (dist > range) continue;
                    p.set(x + iX, y + iY, z + iZ);
                    net.minecraft.world.level.block.state.BlockState state = world.getBlockState(p);
                    int charMeta = (int) net.minecraft.util.Mth.clamp(12 - (dist / range) * (dist / range) * 13, 0, 12);
                    int level = Math.min(charMeta, 10);

                    if (state.isRedstoneConductor(world, p)) { // isNormalCube
                        if (state.getBlock() != slaked || state.getValue(com.hbm_m.block.generic.BlockSellafieldSlaked.COLOR_LEVEL) < level) {
                            world.setBlock(p, com.hbm_m.block.generic.BlockSellafieldSlaked.getStateForPos(slaked, p)
                                    .setValue(com.hbm_m.block.generic.BlockSellafieldSlaked.COLOR_LEVEL, level), 3);
                        }
                    } else if (!state.isAir()) {
                        world.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}

