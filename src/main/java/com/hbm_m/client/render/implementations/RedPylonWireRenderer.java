package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;

import com.hbm_m.blockentity.network.PylonBaseBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.platform.RenderHooks;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Port of RenderPylonBase (1.7.10): draws cables between connected pylons.
 * Each pylon draws its half of the wire (from the mount point to the middle of
 * the span), the sag is a quarter-sine; the wire.png texture tiles UVs every
 * 1/8 of a block - a 1:1 port.
 */
public class RedPylonWireRenderer implements BlockEntityRenderer<PylonBaseBlockEntity>, HbmBerBounds<PylonBaseBlockEntity> {

    private static final int SEGMENTS = 10;
    private static final double GIRTH = 0.03125D;

    public static final ResourceLocation WIRE_TEX = rl("hbm_m", "textures/models/network/wire.png");
    public static final ResourceLocation WIRE_GREYSCALE_TEX = rl("hbm_m", "textures/models/network/wire_greyscale.png");

    private static ResourceLocation rl(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public RedPylonWireRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(PylonBaseBlockEntity pylon, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        Level level = pylon.getLevel();
        if (level == null || pylon.getConnected().isEmpty()) return;

        ResourceLocation tex = pylon.color == 0 ? WIRE_TEX : WIRE_GREYSCALE_TEX;
        var consumer = buffer.getBuffer(ClientRenderHandler.CustomRenderTypes.PYLON_WIRE.apply(tex));
        int rgb = pylon.color == 0 ? 0xFFFFFF : pylon.color;
        float cr = (rgb >> 16 & 0xFF) / 255F;
        float cg = (rgb >> 8 & 0xFF) / 255F;
        float cb = (rgb & 0xFF) / 255F;

        Matrix4f matrix = poseStack.last().pose();

        for (BlockPos otherPos : pylon.getConnected()) {
            if (!(level.getBlockEntity(otherPos) instanceof PylonBaseBlockEntity other)) continue;

            Vec3[] m1 = pylon.getMountPos();
            Vec3[] m2 = other.getMountPos();
            int lineCount = Math.min(m1.length, m2.length);

            for (int line = 0; line < lineCount; line++) {
                Vec3 first = m1[line % m1.length];
                int secondIndex = line % m2.length;

                // Original hack to prevent wires crossing on QUAD pylons (meta 12 and 15: EAST vs NORTH).
                if (lineCount == 4 && crosses(getFacing(pylon), getFacing(other))) {
                    secondIndex = (secondIndex + 2) % m2.length;
                }

                Vec3 second = m2[secondIndex];
                double sX = second.x + other.getBlockPos().getX() - pylon.getBlockPos().getX();
                double sY = second.y + other.getBlockPos().getY() - pylon.getBlockPos().getY();
                double sZ = second.z + other.getBlockPos().getZ() - pylon.getBlockPos().getZ();

                double midX = first.x + (sX - first.x) * 0.5;
                double midY = first.y + (sY - first.y) * 0.5;
                double midZ = first.z + (sZ - first.z) * 0.5;

                renderHalf(consumer, matrix, level, pylon.getBlockPos(), first.x, first.y, first.z, midX, midY, midZ, cr, cg, cb);
            }
        }
    }

    private static Direction getFacing(PylonBaseBlockEntity pylon) {
        BlockState state = pylon.getBlockState();
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return state.getValue(HorizontalDirectionalBlock.FACING);
        }
        return Direction.NORTH;
    }

    /** Original: ((meta-10==5 && other==2) || (2 && 5)) - EAST vs NORTH. */
    private static boolean crosses(Direction a, Direction b) {
        return (a == Direction.EAST && b == Direction.NORTH) || (a == Direction.NORTH && b == Direction.EAST);
    }

    /** Half of the wire: from the mount point (relative to the block) to the middle of the span. Port of renderLine. */
    private static void renderHalf(com.mojang.blaze3d.vertex.VertexConsumer consumer, Matrix4f matrix,
                                   Level level, BlockPos pylonPos,
                                   double x0, double y0, double z0, double x1, double y1, double z1,
                                   float cr, float cg, float cb) {
        double dX = x0 - x1, dY = y0 - y1, dZ = z0 - z1;
        double span = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
        double hang = Math.min(span / 15D, 2.5D);

        // Original 1.7.10 vector orientation math:
        double hyp = Math.sqrt(dX * dX + dZ * dZ);
        double yaw = Math.atan2(dX, dZ);
        double pitch = Math.atan2(dY, hyp);
        double rotator = Math.PI * 0.5D;
        double newPitch = pitch + rotator;
        double newYaw = yaw + rotator;
        double iZ = Math.cos(yaw) * Math.cos(newPitch) * GIRTH;
        double iX = Math.sin(yaw) * Math.cos(newPitch) * GIRTH;
        double iY = Math.sin(newPitch) * GIRTH;
        double jZ = Math.cos(newYaw) * GIRTH;
        double jX = Math.sin(newYaw) * GIRTH;

        double deltaX = x1 - x0, deltaY = y1 - y0, deltaZ = z1 - z0;

        for (int j = 0; j < SEGMENTS; j++) {
            int k = j + 1;
            double sagJ = Math.sin(j / (double) SEGMENTS * Math.PI * 0.5) * hang;
            double sagK = Math.sin(k / (double) SEGMENTS * Math.PI * 0.5) * hang;
            double sagMean = (sagJ + sagK) * 0.5D;

            double ja = j + 0.5D;
            double lx = pylonPos.getX() + x0 + deltaX / (double) SEGMENTS * ja;
            double ly = pylonPos.getY() + y0 + deltaY / (double) SEGMENTS * ja - sagMean;
            double lz = pylonPos.getZ() + z0 + deltaZ / (double) SEGMENTS * ja;

            int brightness = LevelRenderer.getLightColor(level, BlockPos.containing(lx, ly, lz));
            float lightF = Math.max(0.2F, Math.max((brightness >> 4) & 0xF, (brightness >> 20) & 0xF) / 15F);

            double ax = x0 + deltaX * j / (double) SEGMENTS;
            double ay = y0 + deltaY * j / (double) SEGMENTS - sagJ;
            double az = z0 + deltaZ * j / (double) SEGMENTS;
            double bx = x0 + deltaX * k / (double) SEGMENTS;
            double by = y0 + deltaY * k / (double) SEGMENTS - sagK;
            double bz = z0 + deltaZ * k / (double) SEGMENTS;

            double segDX = bx - ax, segDY = by - ay, segDZ = bz - az;
            double length = Math.sqrt(segDX * segDX + segDY * segDY + segDZ * segDZ);
            int wrap = (int) Math.ceil(length * 8);
            double fjX = jX, fjZ = jZ;
            if (segDX + segDZ < 0) {
                wrap *= -1;
                fjZ *= -1;
                fjX *= -1;
            }

            quad(consumer, matrix,
                    ax + iX, ay + iY, az + iZ,
                    ax - iX, ay - iY, az - iZ,
                    bx - iX, by - iY, bz - iZ,
                    bx + iX, by + iY, bz + iZ,
                    0, 0, 0, 1, wrap, 1, wrap, 0,
                    cr * lightF, cg * lightF, cb * lightF);
            quad(consumer, matrix,
                    ax + fjX, ay, az + fjZ,
                    ax - fjX, ay, az - fjZ,
                    bx - fjX, by, bz - fjZ,
                    bx + fjX, by, bz + fjZ,
                    0, 0, 0, 1, wrap, 1, wrap, 0,
                    cr * lightF, cg * lightF, cb * lightF);
        }
    }

    /** Ribbon quad with UVs (vertex and UV order as in the original drawLineSegment). */
    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer consumer, Matrix4f matrix,
                             double x0, double y0, double z0,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             double x3, double y3, double z3,
                             double u0, double v0, double u1, double v1, double u2, double v2, double u3, double v3,
                             float r, float g, float b) {
        RenderHooks.vertexTexColor(consumer, matrix, (float) x0, (float) y0, (float) z0, (float) u0, (float) v0, color(r), color(g), color(b), 0xFF);
        RenderHooks.vertexTexColor(consumer, matrix, (float) x1, (float) y1, (float) z1, (float) u1, (float) v1, color(r), color(g), color(b), 0xFF);
        RenderHooks.vertexTexColor(consumer, matrix, (float) x2, (float) y2, (float) z2, (float) u2, (float) v2, color(r), color(g), color(b), 0xFF);
        RenderHooks.vertexTexColor(consumer, matrix, (float) x3, (float) y3, (float) z3, (float) u3, (float) v3, color(r), color(g), color(b), 0xFF);
    }

    /** double 0..1 -> byte for the hooks' int color. */
    private static int color(double c) {
        return (int) (c * 255.0 + 0.5);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen(PylonBaseBlockEntity blockEntity) {
        return true;
    }
}
