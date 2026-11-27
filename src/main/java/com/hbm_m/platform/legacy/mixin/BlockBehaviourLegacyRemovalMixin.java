package com.hbm_m.platform.legacy.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.hbm_m.platform.legacy.LegacyBlockRemoval;

import net.minecraft.world.level.block.state.BlockBehaviour;

/** [26.x] Hängt {@link LegacyBlockRemoval} an alle Blöcke (Gegenstück zur Interface-Injection im Build). */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourLegacyRemovalMixin implements LegacyBlockRemoval {
}
