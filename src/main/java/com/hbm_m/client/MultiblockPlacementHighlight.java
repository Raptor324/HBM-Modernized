package com.hbm_m.client;

import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockBlockItem;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Кубическая рамка выделения при удержании предмета мультиблока (порт
 * {@code BlockDummyable.drawPlacementHighlight} из 1.7.10).
 *
 * <p>Ванильный контур цели отменяется, вместо него рисуется контур
 * {@code generateShapeFromParts(facing)} — футпринт всей структуры, повёрнутый
 * по FACING и смещённый к позиции ядра (placement-offset учтён через
 * {@link MultiblockBlockItem#updatePlacementContext}). Цвет как в оригинале:
 * пульсирующий зелёный, если место свободно, пульсирующий красный — если нет.
 *
 * <p>ВНИМАНИЕ: PoseStack события не содержит трансляции камеры (в 1.21.1 он вообще
 * пустой — перед ним checkPoseStack), поэтому координаты отдаются как мир - камера,
 * ровно как это делает ванильный renderHitOutline в обеих версиях.
 */
public final class MultiblockPlacementHighlight {

    private MultiblockPlacementHighlight() {}

    public static void render(Level level, Player player, MultiblockBlockItem item,
                              BlockHitResult target, PoseStack poseStack) {
        if (target.getType() != BlockHitResult.Type.BLOCK) return;

        // Контекст установки от клетки клика (facade) — тот же расчёт FACING, что и при реальной установке.
        BlockPlaceContext facadeContext = new BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                player.getMainHandItem(), target);
        BlockState preview = item.getBlock().getStateForPlacement(facadeContext);
        if (preview == null || !preview.hasProperty(HorizontalDirectionalBlock.FACING)) return;
        Direction facing = preview.getValue(HorizontalDirectionalBlock.FACING);

        if (!(item.getBlock() instanceof IMultiblockController controller)) return;
        MultiblockStructureHelper helper = controller.getStructureHelper();
        if (helper == null) return;

        BlockPos facadePos = facadeContext.getClickedPos();
        BlockPlaceContext shifted = item.updatePlacementContext(facadeContext);
        BlockPos corePos = shifted != null ? shifted.getClickedPos() : facadePos;

        boolean canPlace = helper.checkPlacementFromFacadeQuiet(level, facadePos, facing, item.getBlock());

        // Пульсация цвета как в оригинале 1.7.10: (ms % 1000*PI)/250, амплитуда 0.75..1.0
        float pulse = (float) (Math.sin((System.currentTimeMillis() % 3141.59) / 250.0) * 0.25 + 0.75);
        float r = canPlace ? 0.0F : pulse;
        float g = canPlace ? pulse : 0.0F;

        AABB frame = helper.generateShapeFromParts(facing).bounds()
                .move(corePos)
                .inflate(0.002D);

        // Контракт ванильного renderHitOutline (одинаков в 1.20.1 и 1.21.1):
        // вершины передаются как мир - камера. PoseStack события пустой,
        // камера живёт в model-view матрице (только поворот).
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        frame = frame.move(-cam.x, -cam.y, -cam.z);

        VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, consumer, frame, r, g, 0.0F, 1.0F);
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch(RenderType.lines());
    }
}
