package com.hbm_m.client;

import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.IMultiblockPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderHighlightEvent;

import com.hbm_m.lib.RefStrings;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MultiblockHighlightHandler {

    @SubscribeEvent
    public static void onDrawSelectionBox(RenderHighlightEvent.Block event) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        BlockHitResult blockHitResult = event.getTarget();
        BlockPos hitPos = blockHitResult.getBlockPos();
        BlockState hitState = level.getBlockState(hitPos);
        BlockEntity hitBlockEntity = level.getBlockEntity(hitPos);

        BlockPos controllerPos = null;
        BlockState controllerState = null;

        // --- ШАГ 1: Находим контроллер ---
        if (hitState.getBlock() instanceof IMultiblockController) {
            controllerPos = hitPos;
            controllerState = hitState;
        } else if (hitBlockEntity instanceof IMultiblockPart part) {
            controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                controllerState = level.getBlockState(controllerPos);
                if (!(controllerState.getBlock() instanceof IMultiblockController)) {
                    controllerPos = null;
                }
            }
        }

        // --- ШАГ 2: Если мы нашли контроллер (т.е. это мультиблок) ---
        if (controllerPos != null) {

            // Если блок, который игра пытается обвести (hitPos), является контроллером,
            // но мы смотрим НЕ на него, а на другую часть структуры (hitPos != controllerPos),
            // это и есть наш "призрачный" рендер. Просто отменяем его и выходим.
            if (hitPos.equals(controllerPos) && event.getTarget().getType() == HitResult.Type.BLOCK) {
                BlockPos actualHitPos = ((BlockHitResult) event.getTarget()).getBlockPos();
                if (!actualHitPos.equals(controllerPos)) {
                    event.setCanceled(true);
                    return;
                }
            }

            IMultiblockController controller = (IMultiblockController) controllerState.getBlock();
            VoxelShape shapeToDraw;

            shapeToDraw = controller.getCustomMasterVoxelShape(controllerState);
            if (shapeToDraw.isEmpty()) {
                Direction facing = controllerState.hasProperty(HorizontalDirectionalBlock.FACING) ?
                                controllerState.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
                shapeToDraw = controller.getStructureHelper().generateShapeFromParts(facing);
            }
            
            if (shapeToDraw.isEmpty()) return;

            event.setCanceled(true);

            PoseStack poseStack = event.getPoseStack();
            VertexConsumer buffer = event.getMultiBufferSource().getBuffer(RenderType.lines());
            var cameraPos = event.getCamera().getPosition();

            poseStack.pushPose();
            poseStack.translate(
                controllerPos.getX() - cameraPos.x(),
                controllerPos.getY() - cameraPos.y(),
                controllerPos.getZ() - cameraPos.z()
            );

            shapeToDraw.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
                LevelRenderer.renderLineBox(poseStack, buffer, new AABB(minX, minY, minZ, maxX, maxY, maxZ), 0.0F, 0.0F, 0.0F, 0.4F);
            });
            
            poseStack.popPose();
        }
    }
}
