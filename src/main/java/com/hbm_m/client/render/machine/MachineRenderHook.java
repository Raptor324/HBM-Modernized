package com.hbm_m.client.render.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Дополнительный рендер-проход станка, не являющийся OBJ-геометрией:
 * жидкости (UV-скролл), NFPA-алмазы, предметы-иконки рецептов и т.п.
 * Рисует через обычный immediate-путь ({@code bufferSource}); VBO-пайплайн
 * движка на него не распространяется.
 * <p>
 * Контракт: {@code poseStack} уже несёт блочный трансформ (центр + facing);
 * хук обязан сам push/pop.
 */
@FunctionalInterface
public interface MachineRenderHook<T extends BlockEntity> {
    void render(T blockEntity, float partialTick, PoseStack poseStack,
                MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                MachineRenderApi api);
}
