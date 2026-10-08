package com.hbm_m.block.explosives;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.block.IFuckingExplode;
import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.entity.item.EntityTNTPrimedBase;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.explosion.ExplosionNT;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code DetMiner}: Bergbauladung. Redstone oder Zuender ({@link IBomb}) loesen eine schadlose ExplosionNT
 * der Staerke 4 aus, die alle Bloecke fallen laesst; von einer Explosion getroffen wird sie zu einem sofort
 * zuendenden Sprengkoerper. Faellt selbst nie als Gegenstand heraus (Loot-Tabelle leer).
 */
public class DetMinerBlock extends Block implements IBomb, IFuckingExplode {

    public DetMinerBlock(Properties properties) {
        super(properties);
    }

    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        // Original DetMiner hat keine Tooltipzeilen
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack,
                                net.minecraft.world.item.Item.TooltipContext level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        // Original DetMiner hat keine Tooltipzeilen
    }
    *///?}

    @Override
    public BombReturnCode explode(Level world, BlockPos pos) {

        if (!world.isClientSide) {

            world.destroyBlock(pos, false);
            ExplosionNT explosion = new ExplosionNT(world, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4);
            explosion.addAttrib(ExplosionNT.ExAttrib.ALLDROP);
            explosion.addAttrib(ExplosionNT.ExAttrib.NOHURT);
            explosion.explode();

            ExplosionLarge.spawnParticles(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30);
        }

        return BombReturnCode.DETONATED;
    }

    @Override
    public void wasExploded(Level world, BlockPos pos, Explosion explosion) {
        if (!world.isClientSide) {
            LivingEntity placer = explosion != null ? explosion.getIndirectSourceEntity() : null;
            EntityTNTPrimedBase tntPrimed = new EntityTNTPrimedBase(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, placer, this);
            tntPrimed.fuse = 0;
            tntPrimed.detonateOnCollision = false;
            world.addFreshEntity(tntPrimed);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (world.hasNeighborSignal(pos)) {
            this.explode(world, pos);
        }
    }

    @Override
    public void explodeEntity(Level world, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
        explode(world, BlockPos.containing(Mth.floor(x), Mth.floor(y), Mth.floor(z)));
    }
}
