package com.hbm_m.client;

import java.util.Map;

import com.hbm_m.block.custom.decorations.DoorBlock;
import com.hbm_m.block.custom.machines.UniversalMachinePartBlock;
import com.hbm_m.multiblock.IMultiblockPart;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// Новый класс DoorOutlineRenderer.java (клиентский)
@OnlyIn(Dist.CLIENT)
public class DoorOutlineRenderer {
    
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHit)) return;
        
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        
        // Проверяем, это фантомный блок двери?
        if (!(state.getBlock() instanceof UniversalMachinePartBlock)) return;
        if (!(mc.level.getBlockEntity(pos) instanceof IMultiblockPart part)) return;
        
        BlockPos controllerPos = part.getControllerPos();
        if (controllerPos == null) return;
        
        BlockState controllerState = mc.level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof DoorBlock doorBlock)) return;
        
        // Получаем все AABB частей
        String doorId = doorBlock.getDoorDeclId();
        Direction facing = controllerState.getValue(DoorBlock.FACING);
        Map<String, AABB> partAABBs = MultiblockStructureHelper.getDoorPartAABBs(doorId);
        
        if (partAABBs.isEmpty()) return;
        
        PoseStack poseStack = event.getPoseStack();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        
        VertexConsumer consumer = mc.renderBuffers().bufferSource()
            .getBuffer(RenderType.lines());
        
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        
        // Рисуем каждый AABB как отдельную рамку
        for (Map.Entry<String, AABB> entry : partAABBs.entrySet()) {
            AABB aabb = entry.getValue();
            
            // Поворачиваем по facing
            AABB rotated = MultiblockStructureHelper.rotateAABBByFacing(aabb, facing);
            
            // Смещаем в мировые координаты контроллера
            AABB worldAABB = rotated.move(
                controllerPos.getX(),
                controllerPos.getY(),
                controllerPos.getZ()
            );
            
            // Рисуем линии рамки
            LevelRenderer.renderLineBox(
                poseStack, 
                consumer, 
                worldAABB,
                0.0f, 0.0f, 0.0f, 0.4f // RGBA цвет рамки
            );
        }
        
        poseStack.popPose();
    }
}
