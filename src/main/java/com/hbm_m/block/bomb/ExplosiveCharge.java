package com.hbm_m.block.bomb;

import javax.annotation.Nullable;

import com.hbm_m.api.block.IDetConnectible;
import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.item.EntityTNTPrimedBase;
import com.hbm_m.entity.logic.EntityNukeExplosionMK5;
import com.hbm_m.explosion.ExplosionNT;
import com.hbm_m.particle.helper.ExplosionCreator;
import com.hbm_m.particle.helper.NukeTorexCreator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code ExplosiveCharge}: {@code det_charge} (ExplosionNT 15, Aufloesung 64, Standard-Explosionseffekt) und
 * {@code det_nuke} (MK5 mit {@code missileRadius} + Pilzwolke). Redstone zuendet, Zuendschnur schliesst an, andere
 * Explosionen zuenden sofort.
 */
public class ExplosiveCharge extends BlockDetonatable implements IBomb, IDetConnectible {

    public final boolean nuke;

    public ExplosiveCharge(Properties properties, boolean nuke) {
        super(properties, 0, 0, 0, false, false);
        this.nuke = nuke;
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (world.hasNeighborSignal(pos)) explode(world, pos);
    }

    @Override
    public BombReturnCode explode(Level world, BlockPos pos) {
        if (!world.isClientSide) {
            world.removeBlock(pos, false);
            double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
            if (!nuke) {
                new ExplosionNT(world, null, x, y, z, 15).overrideResolution(64).explode();
                if (world instanceof ServerLevel server) ExplosionCreator.composeEffectStandard(server, x, pos.getY() + 1, z);
            } else {
                int r = ModClothConfig.get().missileRadius;
                EntityNukeExplosionMK5.start(world, r, x, y, z);
                NukeTorexCreator.statFacStandard(world, x, y, z, r);
            }
        }
        return BombReturnCode.DETONATED;
    }

    @Override
    public void explodeEntity(Level world, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
        explode(world, BlockPos.containing(x, y, z));
    }
}
