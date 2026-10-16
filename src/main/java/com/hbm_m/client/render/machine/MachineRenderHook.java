package com.hbm_m.client.render.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Extra machine render pass that is not OBJ geometry: fluids (UV scroll),
 * NFPA diamonds, recipe item icons, etc. Draws through the regular immediate
 * path ({@code bufferSource}); the engine's VBO pipeline does not apply to it.
 * <p>
 * Contract: {@code poseStack} already carries the block transform (center +
 * facing); the hook must push/pop itself.
 */
@FunctionalInterface
public interface MachineRenderHook<T extends BlockEntity> {
    void render(T blockEntity, float partialTick, PoseStack poseStack,
                MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                MachineRenderApi api);
}
