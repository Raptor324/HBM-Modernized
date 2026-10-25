package com.hbm_m.block.bomb;

import com.hbm_m.api.block.IFuckingExplode;
import com.hbm_m.entity.item.EntityTNTPrimedBase;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockDetonatable}: von einer Explosion getroffen wird der Block zu einem gezuendeten Sprengkoerper mit
 * kurzer Zufallslunte ({@code popFuse}), er faellt nie als Gegenstand heraus. Feuer daneben zuendet ihn ebenfalls.
 */
public abstract class BlockDetonatable extends BlockFlammable implements IFuckingExplode {

    protected final int popFuse;
    protected final boolean detonateOnCollision;
    protected final boolean detonateOnShot;

    public BlockDetonatable(Properties properties, int en, int flam, int popFuse, boolean detonateOnCollision, boolean detonateOnShot) {
        super(properties, en, flam);
        this.popFuse = popFuse;
        this.detonateOnCollision = detonateOnCollision;
        this.detonateOnShot = detonateOnShot;
    }

    @Override
    public void wasExploded(Level world, BlockPos pos, Explosion explosion) {
        if (!world.isClientSide) {
            LivingEntity placer = explosion != null ? explosion.getIndirectSourceEntity() : null;
            EntityTNTPrimedBase tntPrimed = new EntityTNTPrimedBase(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, placer, this);
            tntPrimed.fuse = popFuse <= 0 ? 0 : world.random.nextInt(popFuse) + popFuse / 2;
            tntPrimed.detonateOnCollision = detonateOnCollision;
            world.addFreshEntity(tntPrimed);
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (!world.isClientSide && shouldIgnite(world, pos)) {
            world.removeBlock(pos, false);
            wasExploded(world, pos, null);
        }
    }

    /** Von Geschossen getroffen ({@code onShot}): nur wenn {@code detonateOnShot}, dann sofort. */
    public void onShot(Level world, BlockPos pos) {
        if (!detonateOnShot) return;
        world.removeBlock(pos, false);
        explodeEntity(world, pos.getX(), pos.getY(), pos.getZ(), null);
    }
}
