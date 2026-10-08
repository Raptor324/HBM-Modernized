//? if forge {
package com.hbm_m.client;

import com.hbm_m.block.UniversalMachinePartBlock;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.interfaces.IMultiblockPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * w16b: Auswahlrahmen fuer Mehrblock-Maschinen.
 *
 * <p>Seit {@code MultiblockStructureHelper.cellShape} liefert jede Zelle nur noch ihren eigenen Formanteil
 * (Raycast wie im Original pro Block, keine vom Server verworfenen Klicks). Damit der Rahmen trotzdem die
 * ganze Maschine zeigt - im Original {@code BlockDummyable.drawHighlight} mit der bounding-Liste -, wird hier
 * die Masterform am Kern gezeichnet und der Vanilla-Rahmen der Einzelzelle unterdrueckt.</p>
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class MultiblockOutlineForge {
    private MultiblockOutlineForge() {}

    @SubscribeEvent
    public static void onHighlight(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) return;
        BlockHitResult target = event.getTarget();
        if (target.getType() == BlockHitResult.Type.MISS) return;
        if (com.hbm_m.compat.ContraptionDoorState.isContraptionWorld(level)) return;

        BlockPos pos = target.getBlockPos();
        BlockState state = level.getBlockState(pos);
        BlockPos corePos;
        if (state.getBlock() instanceof UniversalMachinePartBlock) {
            if (!(level.getBlockEntity(pos) instanceof IMultiblockPart part) || part.getControllerPos() == null) return;
            corePos = part.getControllerPos();
        } else if (state.getBlock() instanceof IMultiblockController) {
            corePos = pos;
        } else {
            return;
        }

        BlockState coreState = level.getBlockState(corePos);
        VoxelShape master = UniversalMachinePartBlock.resolveMasterShape(level, corePos, coreState, CollisionContext.of(mc.player));
        if (master == null || master.isEmpty()) return;

        event.setCanceled(true);
        Vec3 cam = event.getCamera().getPosition();
        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.lines());
        renderShape(event.getPoseStack(), consumer, master,
                corePos.getX() - cam.x, corePos.getY() - cam.y, corePos.getZ() - cam.z);
    }

    /** Wie {@code LevelRenderer.renderShape} (privat): Kanten der Form, Vanilla-Farbe 0/0/0/0.4. */
    private static void renderShape(PoseStack poseStack, VertexConsumer consumer, VoxelShape shape, double x, double y, double z) {
        PoseStack.Pose pose = poseStack.last();
        shape.forAllEdges((x0, y0, z0, x1, y1, z1) -> {
            float dx = (float) (x1 - x0);
            float dy = (float) (y1 - y0);
            float dz = (float) (z1 - z0);
            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= len;
            dy /= len;
            dz /= len;
            consumer.vertex(pose.pose(), (float) (x0 + x), (float) (y0 + y), (float) (z0 + z))
                    .color(0.0F, 0.0F, 0.0F, 0.4F).normal(pose.normal(), dx, dy, dz).endVertex();
            consumer.vertex(pose.pose(), (float) (x1 + x), (float) (y1 + y), (float) (z1 + z))
                    .color(0.0F, 0.0F, 0.0F, 0.4F).normal(pose.normal(), dx, dy, dz).endVertex();
        });
    }
}
//?}
