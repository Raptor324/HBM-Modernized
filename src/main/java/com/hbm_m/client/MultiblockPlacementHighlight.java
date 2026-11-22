package com.hbm_m.client;

import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockBlockItem;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import java.util.ArrayList;
import java.util.List;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * Кубическая рамка выделения при удержании предмета мультиблока (порт
 * {@code BlockDummyable.drawPlacementHighlight} из 1.7.10).
 *
 * <p>Ванильный контур цели отменяется, вместо него рисуется контур
 * {@code generateShapeFromParts(facing)} — футпринт всей структуры, повёрнутый
 * по FACING и смещённый к позиции ядра (placement-offset учтён через
 * {@link MultiblockBlockItem#updatePlacementContext}). Цвет как в оригинале:
 * пульсирующий зелёный, если место свободно, пульсирующий красный — если нет.
 * Также рисуются квадраты портов (жёлтая пульсирующая рамка) и AABBExtras (оранжевые
 * рамки термопортов и т.д.).
 *
 * <p>ВНИМАНИЕ: PoseStack события не содержит трансляции камеры (в 1.21.1 он вообще
 * пустой — перед ним checkPoseStack), поэтому координаты отдаются как мир - камера,
 * ровно как это делает ванильный renderHitOutline в обеих версиях.
 */
@OnlyIn(Dist.CLIENT)
public final class MultiblockPlacementHighlight {

    private MultiblockPlacementHighlight() {}

    public static void render(Level level, Player player, MultiblockBlockItem item,
                              BlockHitResult target, PoseStack poseStack) {
        render(level, player, item, InteractionHand.MAIN_HAND, target, poseStack);
    }

    public static void render(Level level, Player player, MultiblockBlockItem item,
                              InteractionHand hand, BlockHitResult target, PoseStack poseStack) {
        if (target.getType() != BlockHitResult.Type.BLOCK) return;

        // Контекст установки от клетки клика (facade) — тот же расчёт FACING, что и при реальной установке.
        BlockPlaceContext facadeContext = new BlockPlaceContext(player, hand,
                player.getItemInHand(hand), target);
        BlockState preview = item.getBlock().getStateForPlacement(facadeContext);
        if (preview == null || !preview.hasProperty(HorizontalDirectionalBlock.FACING)) return;
        Direction facing = preview.getValue(HorizontalDirectionalBlock.FACING);

        if (!(item.getBlock() instanceof IMultiblockController controller)) return;
        MultiblockStructureHelper helper = controller.getStructureHelper();
        if (helper == null) return;

        BlockPos facadePos = facadeContext.getClickedPos();
        BlockPlaceContext shifted = item.updatePlacementContext(facadeContext);
        BlockPos corePos = shifted != null ? shifted.getClickedPos() : facadePos;

        boolean canPlace;
        // E7 (Fork, nur mit Space-Addon): Ausrichtung/Kern nach der Klickseite
        BlockPlaceContext fork = item instanceof com.hbm_m.multiblock.DoorBlockItem ? null
                : com.hbm_m.multiblock.ForkMultiblockPlacement.shift(facadeContext, item.getBlock());
        if (fork != null) {
            BlockState forkState = item.getBlock().getStateForPlacement(fork);
            if (forkState == null || !forkState.hasProperty(HorizontalDirectionalBlock.FACING)) return;
            facing = forkState.getValue(HorizontalDirectionalBlock.FACING);
            corePos = fork.getClickedPos();
            canPlace = helper.checkPlacementQuiet(level, corePos, facing);
        } else {
            canPlace = helper.checkPlacementFromFacadeQuiet(level, facadePos, facing, item.getBlock());
        }

        // Пульсация цвета как в оригинале 1.7.10: (ms % 1000*PI)/250, амплитуда 0.75..1.0
        float pulse = (float) (Math.sin((System.currentTimeMillis() % 3141.59) / 250.0) * 0.25 + 0.75);
        float r = canPlace ? 0.0F : pulse;
        float g = canPlace ? pulse : 0.0F;

        AABB frame = helper.generateShapeFromParts(facing).bounds().inflate(0.002D);

        // Контракт ванильного renderHitOutline (одинаков в 1.20.1 и 1.21.1):
        // вершины передаются как мир - камера. PoseStack события пустой,
        // камера живёт в model-view матрице (только поворот).
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        poseStack.pushPose();
        // On a Sable ship the outline event fires INSIDE the ship render pose that Sable's
        // LevelRenderer mixin pushes before ClientHooks.onDrawHighlight. Applying our own
        // ship transform here would rotate the frame twice, so instead we draw core-relative
        // offsets in the ship's plot grid, camera-relative (same convention as the vanilla
        // outline, whose camera Sable swaps to the sublevel one). Off a ship the stack is
        // empty and vertices are passed as world - camera.
        Vec3 plotCam = com.hbm_m.compat.sable.SableClientCompat.plotCamera(facadePos, cam);
        double baseX, baseY, baseZ;
        if (plotCam != null) {
            poseStack.translate((float) (corePos.getX() - plotCam.x), (float) (corePos.getY() - plotCam.y),
                    (float) (corePos.getZ() - plotCam.z));
            baseX = 0.0;
            baseY = 0.0;
            baseZ = 0.0;
        } else {
            baseX = corePos.getX() - cam.x;
            baseY = corePos.getY() - cam.y;
            baseZ = corePos.getZ() - cam.z;
            frame = frame.move(baseX, baseY, baseZ);
        }

        VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, consumer, frame, r, g, 0.0F, 1.0F);

        // boo-yeah: getAABBExtras (orange heat ports, etc.)
        double[][] extras = controller.getAABBExtras();
        if (extras != null && extras.length > 0) {
            Direction rot = facing.getClockWise();
            for (double[] extra : extras) {
                if (extra == null || extra.length < 6) continue;
                float rExtra = 0.0F, gExtra = 0.0F, bExtra = pulse, aExtra = 1.0F;
                if (extra.length > 6) {
                    if (extra[6] == 1.0) {
                        rExtra = 1.0F;
                        gExtra = 165.0F / 255.0F;
                        bExtra = 0.0F;
                    } else {
                        int hex = (int) Math.round(extra[6]);
                        rExtra = ((hex >> 16) & 0xFF) / 255.0F;
                        gExtra = ((hex >> 8) & 0xFF) / 255.0F;
                        bExtra = (hex & 0xFF) / 255.0F;
                    }
                }
                double cX = baseX + 0.5;
                double cY = baseY;
                double cZ = baseZ + 0.5;
                double upr = extra[0], lwr = extra[1];
                double fwd = extra[2], bwd = extra[3];
                double lft = extra[4], rgt = extra[5];

                double x0 = cX + fwd * facing.getStepX() + lft * rot.getStepX();
                double x1 = cX + bwd * facing.getStepX() + rgt * rot.getStepX();
                double y0 = cY + lwr;
                double y1 = cY + upr;
                double z0 = cZ + fwd * facing.getStepZ() + lft * rot.getStepZ();
                double z1 = cZ + bwd * facing.getStepZ() + rgt * rot.getStepZ();

                Matrix4f matrix = poseStack.last().pose();
                float fx0 = (float) x0, fx1 = (float) x1;
                float fy0 = (float) y0, fy1 = (float) y1;
                float fz0 = (float) z0, fz1 = (float) z1;

                line(consumer, matrix, fx0, fy0, fz0, fx0, fy0, fz1, rExtra, gExtra, bExtra, aExtra);
                line(consumer, matrix, fx1, fy0, fz0, fx1, fy0, fz1, rExtra, gExtra, bExtra, aExtra);
                line(consumer, matrix, fx0, fy0, fz0, fx1, fy0, fz0, rExtra, gExtra, bExtra, aExtra);
                line(consumer, matrix, fx0, fy0, fz1, fx1, fy0, fz1, rExtra, gExtra, bExtra, aExtra);

                line(consumer, matrix, fx0, fy1, fz0, fx0, fy1, fz1, rExtra, gExtra, bExtra, aExtra);
                line(consumer, matrix, fx1, fy1, fz0, fx1, fy1, fz1, rExtra, gExtra, bExtra, aExtra);
                line(consumer, matrix, fx0, fy1, fz0, fx1, fy1, fz0, rExtra, gExtra, bExtra, aExtra);
                line(consumer, matrix, fx0, fy1, fz1, fx1, fy1, fz1, rExtra, gExtra, bExtra, aExtra);

                line(consumer, matrix, fx0, fy0, fz0, fx0, fy1, fz0, rExtra, gExtra, bExtra, aExtra);
                line(consumer, matrix, fx1, fy0, fz0, fx1, fy1, fz0, rExtra, gExtra, bExtra, aExtra);
                line(consumer, matrix, fx0, fy0, fz1, fx0, fy1, fz1, rExtra, gExtra, bExtra, aExtra);
                line(consumer, matrix, fx1, fy0, fz1, fx1, fy1, fz1, rExtra, gExtra, bExtra, aExtra);
            }
        }

        // rainbow yellow! Ports rendering
        List<int[]> portsList = new ArrayList<>();
        int[][] explicitPorts = controller.getAllPorts(facing);
        if (explicitPorts != null && explicitPorts.length > 0) {
            for (int[] p : explicitPorts) {
                if (p != null && p.length >= 4) {
                    portsList.add(p);
                }
            }
        } else {
            portsList.addAll(helper.getAutoPorts(controller, facing));
        }

        if (!portsList.isEmpty()) {
            Matrix4f matrix = poseStack.last().pose();
            float s = 0.3F;
            for (int[] port : portsList) {
                double jX = baseX + port[0];
                double jY = baseY + port[1];
                double jZ = baseZ + port[2];
                Direction dir = Direction.from3DDataValue(port[3]);

                if (dir == Direction.UP || dir == Direction.DOWN) {
                    float fy = (float) (jY + (dir == Direction.UP ? 1.0 : 0.0));
                    float x0 = (float) (jX + 0.5 - s), x1 = (float) (jX + 0.5 + s);
                    float z0 = (float) (jZ + 0.5 - s), z1 = (float) (jZ + 0.5 + s);
                    line(consumer, matrix, x0, fy, z0, x1, fy, z0, pulse, pulse, 0.0F, 1.0F);
                    line(consumer, matrix, x1, fy, z0, x1, fy, z1, pulse, pulse, 0.0F, 1.0F);
                    line(consumer, matrix, x1, fy, z1, x0, fy, z1, pulse, pulse, 0.0F, 1.0F);
                    line(consumer, matrix, x0, fy, z1, x0, fy, z0, pulse, pulse, 0.0F, 1.0F);
                } else if (dir == Direction.EAST || dir == Direction.WEST) {
                    float fx = (float) (jX + (dir == Direction.EAST ? 1.0 : 0.0));
                    float y0 = (float) (jY + 0.5 - s), y1 = (float) (jY + 0.5 + s);
                    float z0 = (float) (jZ + 0.5 - s), z1 = (float) (jZ + 0.5 + s);
                    line(consumer, matrix, fx, y0, z0, fx, y0, z1, pulse, pulse, 0.0F, 1.0F);
                    line(consumer, matrix, fx, y0, z1, fx, y1, z1, pulse, pulse, 0.0F, 1.0F);
                    line(consumer, matrix, fx, y1, z1, fx, y1, z0, pulse, pulse, 0.0F, 1.0F);
                    line(consumer, matrix, fx, y1, z0, fx, y0, z0, pulse, pulse, 0.0F, 1.0F);
                } else {
                    float fz = (float) (jZ + (dir == Direction.SOUTH ? 1.0 : 0.0));
                    float x0 = (float) (jX + 0.5 - s), x1 = (float) (jX + 0.5 + s);
                    float y0 = (float) (jY + 0.5 - s), y1 = (float) (jY + 0.5 + s);
                    line(consumer, matrix, x0, y0, fz, x1, y0, fz, pulse, pulse, 0.0F, 1.0F);
                    line(consumer, matrix, x1, y0, fz, x1, y1, fz, pulse, pulse, 0.0F, 1.0F);
                    line(consumer, matrix, x1, y1, fz, x0, y1, fz, pulse, pulse, 0.0F, 1.0F);
                    line(consumer, matrix, x0, y1, fz, x0, y0, fz, pulse, pulse, 0.0F, 1.0F);
                }
            }
        }

        Minecraft.getInstance().renderBuffers().bufferSource().endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private static void line(VertexConsumer buf, Matrix4f m, float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float r, float g, float b, float a) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) len = 1;
        dx /= len; dy /= len; dz /= len;
        com.hbm_m.platform.RenderHooks.vertexColorNormal(buf, m, x0, y0, z0, r, g, b, a, dx, dy, dz);
        com.hbm_m.platform.RenderHooks.vertexColorNormal(buf, m, x1, y1, z1, r, g, b, a, dx, dy, dz);
    }
}
