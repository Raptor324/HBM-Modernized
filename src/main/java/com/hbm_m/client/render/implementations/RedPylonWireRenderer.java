package com.hbm_m.client.render.implementations;

import com.hbm_m.block.network.RedPylonMediumBlock;
import com.hbm_m.blockentity.network.PylonBaseBlockEntity;
import com.hbm_m.client.ClientRenderHandler;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Порт RenderPylonBase (1.7.10): рисует кабели между подключёнными пилонами.
 * Каждый пилон рисует свою половину провода (от крепления до середины пролёта),
 * провис — четверть-синус; текстура wire.png с тайлингом UV каждые 1/8 блока — 1:1.
 */
public class RedPylonWireRenderer implements BlockEntityRenderer<PylonBaseBlockEntity> {

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

        Vec3 origin = Vec3.atLowerCornerOf(pylon.getBlockPos());

        for (BlockPos otherPos : pylon.getConnected()) {
            if (!(level.getBlockEntity(otherPos) instanceof PylonBaseBlockEntity other)) continue;

            Vec3[] m1 = pylon.getMountPos();
            Vec3[] m2 = other.getMountPos();
            int lineCount = Math.min(m1.length, m2.length);

            for (int line = 0; line < lineCount; line++) {
                Vec3 first = m1[line % m1.length];
                int secondIndex = line % m2.length;

                // Хак оригинала против пересечения проводов на QUAD-пилонах (meta 12 и 15: EAST vs NORTH).
                if (lineCount == 4 && crosses(getFacing(pylon), getFacing(other))) {
                    secondIndex = (secondIndex + 2) % m2.length;
                }

                Vec3 second = m2[secondIndex];
                double sX = second.x + other.getBlockPos().getX() - pylon.getBlockPos().getX();
                double sY = second.y + other.getBlockPos().getY() - pylon.getBlockPos().getY();
                double sZ = second.z + other.getBlockPos().getZ() - pylon.getBlockPos().getZ();

                double midX = origin.x + first.x + (sX - first.x) * 0.5;
                double midY = origin.y + first.y + (sY - first.y) * 0.5;
                double midZ = origin.z + first.z + (sZ - first.z) * 0.5;

                renderHalf(consumer, level, origin, first.x, first.y, first.z, midX, midY, midZ, cr, cg, cb);
            }
        }
    }

    private static Direction getFacing(PylonBaseBlockEntity pylon) {
        if (pylon.getBlockState().getBlock() instanceof RedPylonMediumBlock) {
            return pylon.getBlockState().getValue(RedPylonMediumBlock.FACING);
        }
        return Direction.NORTH;
    }

    /** Оригинал: ((meta-10==5 && other==2) || (2 && 5)) → EAST vs NORTH. */
    private static boolean crosses(Direction a, Direction b) {
        return (a == Direction.EAST && b == Direction.NORTH) || (a == Direction.NORTH && b == Direction.EAST);
    }

    /** Половина провода: от крепления (относительно блока) до середины пролёта. Порт renderLine. */
    private static void renderHalf(com.mojang.blaze3d.vertex.VertexConsumer consumer, Level level, Vec3 origin,
                                   double x0, double y0, double z0, double x1, double y1, double z1,
                                   float cr, float cg, float cb) {
        double dX = x0 - x1, dY = y0 - y1, dZ = z0 - z1;
        double span = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
        double hang = Math.min(span / 15D, 2.5D);

        // Ортогональные оси сечения (аналог i/j из оригинала).
        double jX = -dZ, jZ = dX;
        double jLen = Math.sqrt(jX * jX + jZ * jZ);
        if (jLen < 1.0E-4) { jX = 1; jZ = 0; jLen = 1; }
        jX /= jLen; jZ /= jLen;
        double iX = -jZ * dY, iY = jZ * dX - jX * dZ, iZ = jX * dY;
        double iLen = Math.sqrt(iX * iX + iY * iY + iZ * iZ);
        if (iLen < 1.0E-4) { iX = 0; iY = 0; iZ = 0; } else { iX /= iLen; iY /= iLen; iZ /= iLen; }
        iX *= GIRTH; iY *= GIRTH; iZ *= GIRTH;
        jX *= GIRTH; jZ *= GIRTH;

        double deltaX = x1 - x0, deltaY = y1 - y0, deltaZ = z1 - z0;

        for (int j = 0; j < SEGMENTS; j++) {
            int k = j + 1;
            double sagJ = Math.sin(j / (double) SEGMENTS * Math.PI * 0.5) * hang;
            double sagK = Math.sin(k / (double) SEGMENTS * Math.PI * 0.5) * hang;

            double ja = j + 0.5D;
            double lx = origin.x + x0 + deltaX / SEGMENTS * ja;
            double ly = origin.y + y0 + deltaY / SEGMENTS * ja - sagJ;
            double lz = origin.z + z0 + deltaZ / SEGMENTS * ja;

            int brightness = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, BlockPos.containing(lx, ly, lz));
            float lightF = Math.max(0.2F, Math.max((brightness >> 4) & 0xF, (brightness >> 20) & 0xF) / 15F);

            double ax = origin.x + x0 + deltaX * j / SEGMENTS;
            double ay = origin.y + y0 + deltaY * j / SEGMENTS - sagJ;
            double az = origin.z + z0 + deltaZ * j / SEGMENTS;
            double bx = origin.x + x0 + deltaX * k / SEGMENTS;
            double by = origin.y + y0 + deltaY * k / SEGMENTS - sagK;
            double bz = origin.z + z0 + deltaZ * k / SEGMENTS;

            double segDX = bx - ax, segDY = by - ay, segDZ = bz - az;
            double length = Math.sqrt(segDX * segDX + segDY * segDY + segDZ * segDZ);
            int wrap = (int) Math.ceil(length * 8);
            double fjX = jX, fjZ = jZ;
            if (segDX + segDZ < 0) {
                wrap *= -1;
                fjZ *= -1;
                fjX *= -1;
            }

            quad(consumer,
                    ax + iX, ay + iY, az + iZ,
                    ax - iX, ay - iY, az - iZ,
                    bx - iX, by - iY, bz - iZ,
                    bx + iX, by + iY, bz + iZ,
                    0, 0, 0, 1, wrap, 1, wrap, 0,
                    fjX, fjZ, cr * lightF, cg * lightF, cb * lightF);
            quad(consumer,
                    ax + fjX, ay, az + fjZ,
                    ax - fjX, ay, az - fjZ,
                    bx - fjX, by, bz - fjZ,
                    bx + fjX, by, bz + fjZ,
                    0, 0, 0, 1, wrap, 1, wrap, 0,
                    0, 0, cr * lightF, cg * lightF, cb * lightF);
        }
    }

    /** Лента-квад с UV (порядок вершин и UV — как в drawLineSegment оригинала). */
    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer consumer,
                             double x0, double y0, double z0,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             double x3, double y3, double z3,
                             double u0, double v0, double u1, double v1, double u2, double v2, double u3, double v3,
                             double jX, double jZ, float r, float g, float b) {
        //? if < 1.21.1 {
        com.mojang.blaze3d.vertex.VertexConsumer c = consumer;
        c.vertex(x0, y0, z0).color(r, g, b, 1F).uv((float) u0, (float) v0).endVertex();
        c.vertex(x1, y1, z1).color(r, g, b, 1F).uv((float) u1, (float) v1).endVertex();
        c.vertex(x2, y2, z2).color(r, g, b, 1F).uv((float) u2, (float) v2).endVertex();
        c.vertex(x3, y3, z3).color(r, g, b, 1F).uv((float) u3, (float) v3).endVertex();
        //?} else {
        /*consumer.addVertex((float) x0, (float) y0, (float) z0).setColor(r, g, b, 1F).setUv((float) u0, (float) v0);
        consumer.addVertex((float) x1, (float) y1, (float) z1).setColor(r, g, b, 1F).setUv((float) u1, (float) v1);
        consumer.addVertex((float) x2, (float) y2, (float) z2).setColor(r, g, b, 1F).setUv((float) u2, (float) v2);
        consumer.addVertex((float) x3, (float) y3, (float) z3).setColor(r, g, b, 1F).setUv((float) u3, (float) v3);
        *///?}
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
