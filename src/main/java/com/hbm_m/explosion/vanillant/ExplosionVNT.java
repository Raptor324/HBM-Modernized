package com.hbm_m.explosion.vanillant;

import com.hbm_m.explosion.vanillant.interfaces.IBlockAllocator;
import com.hbm_m.explosion.vanillant.interfaces.IBlockProcessor;
import com.hbm_m.explosion.vanillant.interfaces.IEntityProcessor;
import com.hbm_m.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectStandard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Модульный «ванильный» взрыв HBM
 */
public class ExplosionVNT {

    private IBlockAllocator blockAllocator;
    private IEntityProcessor entityProcessor;
    private IBlockProcessor blockProcessor;
    private IExplosionSFX[] sfx;

    public Level level;
    public double x;
    public double y;
    public double z;
    public float size;
    @Nullable public Entity exploder;

    private final Map<Player, Vec3> compatPlayers = new HashMap<>();
    public Explosion compat;

    public ExplosionVNT(Level level, double x, double y, double z, float size) {
        this(level, x, y, z, size, null);
    }

    public ExplosionVNT(Level level, double x, double y, double z, float size, @Nullable Entity exploder) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.size = size;
        this.exploder = exploder;

        this.compat = new Explosion(level, exploder, x, y, z, size, false, Explosion.BlockInteraction.KEEP) {
            @Override
            public Map<Player, Vec3> getHitPlayers() {
                return ExplosionVNT.this.compatPlayers;
            }
        };
    }

    public void explode() {
        boolean processBlocks = blockAllocator != null && blockProcessor != null;
        boolean processEntities = entityProcessor != null;

        HashSet<BlockPos> affectedBlocks = null;
        HashMap<Player, Vec3> affectedPlayers = null;

        if (processBlocks) {
            affectedBlocks = blockAllocator.allocate(this, level, x, y, z, size);
            this.compat.getToBlow().addAll(affectedBlocks);
        }
        if (processEntities) {
            affectedPlayers = entityProcessor.process(this, level, x, y, z, size);
            this.compat.getHitPlayers().putAll(affectedPlayers);
        }

        if (processBlocks) {
            blockProcessor.process(this, level, x, y, z, affectedBlocks);
        }

        if (sfx != null) {
            for (IExplosionSFX fx : sfx) {
                fx.doEffect(this, level, x, y, z, size);
            }
        }
    }

    public ExplosionVNT setBlockAllocator(IBlockAllocator blockAllocator) {
        this.blockAllocator = blockAllocator;
        return this;
    }

    public ExplosionVNT setEntityProcessor(IEntityProcessor entityProcessor) {
        this.entityProcessor = entityProcessor;
        return this;
    }

    public ExplosionVNT setBlockProcessor(IBlockProcessor blockProcessor) {
        this.blockProcessor = blockProcessor;
        return this;
    }

    public ExplosionVNT setSFX(IExplosionSFX... sfx) {
        this.sfx = sfx;
        return this;
    }

    public ExplosionVNT makeStandard() {
        this.setBlockAllocator(new BlockAllocatorStandard());
        this.setBlockProcessor(new BlockProcessorStandard());
        this.setEntityProcessor(new EntityProcessorStandard());
        this.setSFX(new ExplosionEffectStandard());
        return this;
    }

    /** 1.7.10 {@code World.createExplosion} / {@code newExplosion} через VNT. */
    public static void createExplosion(Level level, @Nullable Entity exploder, double x, double y, double z,
            float strength, boolean isSmoking) {
        newExplosion(level, exploder, x, y, z, strength, false, isSmoking);
    }

    public static void newExplosion(Level level, @Nullable Entity exploder, double x, double y, double z,
            float strength, boolean fire, boolean isSmoking) {
        ExplosionVNT vnt = new ExplosionVNT(level, x, y, z, strength, exploder)
                .setBlockAllocator(isSmoking ? new BlockAllocatorStandard() : null)
                .setBlockProcessor(isSmoking ? new BlockProcessorStandard().withBlockEffect(fire ? new BlockMutatorFire() : null) : null)
                .setEntityProcessor(new EntityProcessorStandard())
                .setSFX(new ExplosionEffectStandard());
        vnt.explode();
    }
}
