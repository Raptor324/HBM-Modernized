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

    /**
     * Whether this hook has immediate content for this BE this frame. Consulted
     * by the fast-assert path: hooks reporting false on a machine whose
     * animation epoch is stable let the WHOLE machine roster-assert (renderAll
     * is skipped entirely - a hook that would draw must veto). Contract: the
     * predicate is re-evaluated every frame, and any state change that can
     * produce content must also flip the spec's animation epoch (the
     * assembler's recipe icon is gated on the crafting state, which the ring
     * segment epoch covers) - otherwise the frame the content appears would
     * still assert it away.
     */
    default boolean mayDrawContent(T blockEntity) {
        return true;
    }
}
