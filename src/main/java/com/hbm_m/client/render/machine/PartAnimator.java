package com.hbm_m.client.render.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Animation of a machine model part - the only thing a specific machine's
 * developer writes.
 * <p>
 * Contract: {@code pose} already carries the block transform (block center +
 * facing rotation) and is ALREADY pushed by the engine - just do
 * translate/rotate/scale relative to the block and DO NOT call push/pop: the
 * engine takes the final matrix itself and unwinds the stack after return.
 *
 * @return {@code true} - the part is drawn this frame; {@code false} - skip it
 *         (e.g. animation data not ready yet).
 */
@FunctionalInterface
public interface PartAnimator<T extends BlockEntity> {
    boolean animate(T blockEntity, float partialTick, long gameTime, PoseStack pose);
}
