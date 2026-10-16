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

    @Override
    public boolean hbm_m$isNucleusRegistered() {
        return this.hbm_m$nucleusRegistered;
    }

    @Override
    public void hbm_m$setNucleusRegistered(boolean registered) {
        this.hbm_m$nucleusRegistered = registered;
    }
}
