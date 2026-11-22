package com.hbm_m.mixin.client;

import com.hbm_m.client.render.NucleusTrackedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Attaches {@link NucleusTrackedBlockEntity} to {@link BlockEntity} for O(1)
 * identity flag checks during Embeddium / dispatcher bypass.
 */
@Mixin(BlockEntity.class)
public class MixinBlockEntity implements NucleusTrackedBlockEntity {

    @Unique
    private boolean hbm_m$nucleusRegistered = false;

    @Unique
    private Object hbm_m$renderState = null;

    @Unique
    private Object hbm_m$boundsEntry = null;

    @Unique
    private boolean hbm_m$demolished = false;

    @Override
    public boolean hbm_m$isNucleusRegistered() {
        return this.hbm_m$nucleusRegistered;
    }

    @Override
    public void hbm_m$setNucleusRegistered(boolean registered) {
        this.hbm_m$nucleusRegistered = registered;
    }

    @Override
    public Object hbm_m$getRenderState() {
        return this.hbm_m$renderState;
    }

    @Override
    public void hbm_m$setRenderState(Object state) {
        this.hbm_m$renderState = state;
    }

    @Override
    public Object hbm_m$getBoundsEntry() {
        return this.hbm_m$boundsEntry;
    }

    @Override
    public void hbm_m$setBoundsEntry(Object entry) {
        this.hbm_m$boundsEntry = entry;
    }

    @Override
    public boolean hbm_m$isDemolished() {
        return this.hbm_m$demolished;
    }

    @Override
    public void hbm_m$setDemolished(boolean demolished) {
        this.hbm_m$demolished = demolished;
    }
}
