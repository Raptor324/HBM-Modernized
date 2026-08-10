package com.hbm_m.client.render.effect;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.entity.effect.BlackHoleEntity;
import com.hbm_m.entity.effect.RagingVortexEntity;
import com.hbm_m.entity.effect.VortexEntity;
import com.hbm_m.lib.RefStrings;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;


/**
 * Порт {@code com.hbm.render.entity.effect.RenderBlackHole} (1.7.10).
 */

public class RenderBlackHole<T extends BlackHoleEntity> extends EntityRenderer<T> {

    private static final int SPHERE_STACKS = 16;
    private static final int SPHERE_SLICES = 16;

    protected static final ResourceLocation SWIRL = RefStrings.resourceLocation("textures/entity/bhole.png");

    public RenderBlackHole(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();
        RenderSystem.disableCull();

        float size = entity.getSize();
        poseStack.scale(size, size, size);

        renderBlackSphere(poseStack, bufferSource, 1.0F);

        if (entity instanceof VortexEntity) {
            renderSwirl(entity, partialTick, poseStack, bufferSource);
        } else if (entity instanceof RagingVortexEntity) {
            renderSwirl(entity, partialTick, poseStack, bufferSource);
            renderJets(poseStack, bufferSource, entity);
        } else {
            renderDisc(entity, partialTick, poseStack, bufferSource);
            renderJets(poseStack, bufferSource, entity);
        }

        RenderSystem.enableCull();
        poseStack.popPose();
    }

    /** Solid black event horizon (1.7.10 {@code Sphere.obj} + {@code BlackHole.png}). */
    protected void renderBlackSphere(PoseStack poseStack, MultiBufferSource bufferSource, float radius) {

        VertexConsumer consumer = bufferSource.getBuffer(ClientRenderHandler.CustomRenderTypes.BHOLE_SPHERE);
        Matrix4f matrix = poseStack.last().pose();

        for (int i = 0; i < SPHERE_STACKS; i++) {
            double phi1 = Math.PI * i / SPHERE_STACKS;
            double phi2 = Math.PI * (i + 1) / SPHERE_STACKS;

            for (int j = 0; j < SPHERE_SLICES; j++) {
                double theta1 = 2.0 * Math.PI * j / SPHERE_SLICES;
                double theta2 = 2.0 * Math.PI * (j + 1) / SPHERE_SLICES;

                float x1 = (float) (radius * Math.sin(phi1) * Math.cos(theta1));
                float y1 = (float) (radius * Math.cos(phi1));
                float z1 = (float) (radius * Math.sin(phi1) * Math.sin(theta1));

                float x2 = (float) (radius * Math.sin(phi2) * Math.cos(theta1));
                float y2 = (float) (radius * Math.cos(phi2));
                float z2 = (float) (radius * Math.sin(phi2) * Math.sin(theta1));

                float x3 = (float) (radius * Math.sin(phi2) * Math.cos(theta2));
                float y3 = (float) (radius * Math.cos(phi2));
                float z3 = (float) (radius * Math.sin(phi2) * Math.sin(theta2));

                float x4 = (float) (radius * Math.sin(phi1) * Math.cos(theta2));
                float y4 = (float) (radius * Math.cos(phi1));
                float z4 = (float) (radius * Math.sin(phi1) * Math.sin(theta2));

                emitSolidVertex(consumer, matrix, x1, y1, z1);
                emitSolidVertex(consumer, matrix, x2, y2, z2);
                emitSolidVertex(consumer, matrix, x3, y3, z3);

                emitSolidVertex(consumer, matrix, x1, y1, z1);
                emitSolidVertex(consumer, matrix, x3, y3, z3);
                emitSolidVertex(consumer, matrix, x4, y4, z4);
            }
        }
    }

    private static void emitSolidVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z) {
        consumer.vertex(matrix, x, y, z)
                .color(0, 0, 0, 255)
                .endVertex();
    }

    protected void renderDisc(T entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        float glow = 0.75F;
        ResourceLocation disc = discTex();

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getId() % 90 - 45));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));

        beginDiscPass(disc);
        Vec3 vec = new Vec3(1, 0, 0);
        int count = 16;

        for (int k = 0; k < steps(); k++) {
            poseStack.pushPose();
            float rotation = (entity.tickCount + partialTick % 360) * -((float) Math.pow(k + 1, 1.25));
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            Matrix4f matrix = poseStack.last().pose();
            double s = 3 - k * 0.175D;

            for (int j = 0; j < 2; j++) {
                vec = new Vec3(1, 0, 0);
                if (j == 1) {
                    RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                }

                BufferBuilder buffer = Tesselator.getInstance().getBuilder();
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                for (int i = 0; i < count; i++) {
                    int[] color1 = (j == 0) ? colorFromIteration(k, 1F) : new int[]{255, 255, 255, (int) (glow * 255)};
                    int[] color2 = colorFromIteration(k, 0F);
                    addTexturedVertex(buffer, matrix, vec, s, color1, 0.25);
                    addTexturedVertex(buffer, matrix, vec, s * 2, color2, 0.5);
                    vec = rotateY(vec, (float) (Math.PI * 2 / count));
                    addTexturedVertex(buffer, matrix, vec, s * 2, color2, 0.5);
                    addTexturedVertex(buffer, matrix, vec, s, color1, 0.25);
                }
                BufferUploader.drawWithShader(buffer.end());
            }

            setDiscBlendNormal();
            poseStack.popPose();
        }

        endDiscPass();
        poseStack.popPose();
    }

    protected void renderSwirl(T entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        float glow = entity instanceof RagingVortexEntity ? 0.25F : 0.75F;
        int[] colorFull = getColorFull(entity);
        int[] colorNone = getColorNone(entity);

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getId() % 90 - 45));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick % 360) * -5));

        beginDiscPass(SWIRL);
        Matrix4f matrix = poseStack.last().pose();
        Vec3 vec = new Vec3(1, 0, 0);
        double s = 3;
        int count = 16;

        for (int j = 0; j < 2; j++) {
            if (j == 1) {
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            }
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            vec = new Vec3(1, 0, 0);
            for (int i = 0; i < count; i++) {
                addTexturedVertex(buffer, matrix, vec, 0.9, new int[]{0, 0, 0, 255}, 0.25 / s * 0.9);
                int[] c1 = (j == 0) ? colorFull : new int[]{255, 255, 255, (int) (glow * 255)};
                addTexturedVertex(buffer, matrix, vec, s, c1, 0.25);
                vec = rotateY(vec, (float) (Math.PI * 2 / count));
                int[] c2 = (j == 0) ? colorFull : new int[]{255, 255, 255, (int) (glow * 255)};
                addTexturedVertex(buffer, matrix, vec, s, c2, 0.25);
                addTexturedVertex(buffer, matrix, vec, 0.9, new int[]{0, 0, 0, 255}, 0.25 / s * 0.9);
            }
            BufferUploader.drawWithShader(buffer.end());
        }

        setDiscBlendNormal();
        for (int j = 0; j < 2; j++) {
            if (j == 1) {
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            }
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            vec = new Vec3(1, 0, 0);
            for (int i = 0; i < count; i++) {
                int[] c1 = (j == 0) ? colorFull : new int[]{255, 255, 255, (int) (glow * 255)};
                addTexturedVertex(buffer, matrix, vec, s, c1, 0.25);
                addTexturedVertex(buffer, matrix, vec, s * 2, colorNone, 0.5);
                vec = rotateY(vec, (float) (Math.PI * 2 / count));
                addTexturedVertex(buffer, matrix, vec, s * 2, colorNone, 0.5);
                int[] c2 = (j == 0) ? colorFull : new int[]{255, 255, 255, (int) (glow * 255)};
                addTexturedVertex(buffer, matrix, vec, s, c2, 0.25);
            }
            BufferUploader.drawWithShader(buffer.end());
        }

        endDiscPass();
        poseStack.popPose();
    }

    protected void renderJets(PoseStack poseStack, MultiBufferSource bufferSource, T entity) {
        
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getId() % 90 - 45));
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(ClientRenderHandler.CustomRenderTypes.BHOLE_JETS);
        int innerAlpha = (int) (0.35F * 255);

        for (int j = -1; j <= 1; j += 2) {
            Vec3 jet = new Vec3(0.5, 0, 0);
            Vec3[] ring = new Vec3[13];
            for (int i = 0; i <= 12; i++) {
                ring[i] = jet;
                jet = rotateY(jet, (float) (Math.PI / 6 * -j));
            }

            for (int i = 0; i < 12; i++) {
                consumer.vertex(matrix, 0, 0, 0).color(255, 255, 255, innerAlpha).endVertex();
                consumer.vertex(matrix, (float) ring[i].x, 10 * j, (float) ring[i].z).color(255, 255, 255, 0).endVertex();
                consumer.vertex(matrix, (float) ring[i + 1].x, 10 * j, (float) ring[i + 1].z).color(255, 255, 255, 0).endVertex();
            }
        }
        poseStack.popPose();
    }

    private static void beginDiscPass(ResourceLocation texture) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        setDiscBlendNormal();
    }

    private static void setDiscBlendNormal() {
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
    }

    private static void endDiscPass() {
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void addTexturedVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 vec, double scale,
                                          int[] color, double uvScale) {
        buffer.vertex(matrix, (float) (vec.x * scale), 0, (float) (vec.z * scale))
                .uv((float) (0.5 + vec.x * uvScale), (float) (0.5 + vec.z * uvScale))
                .color(color[0], color[1], color[2], color[3])
                .endVertex();
    }



    protected int steps() {
        return 15;
    }



    protected int[] colorFromIteration(int iteration, float alpha) {

        int a = (int) (alpha * 255);

        if (iteration < 5) {
            float g = 0.125F + iteration * (1F / 10F);
            return new int[]{255, (int) (g * 255), 0, a};
        }

        if (iteration == 5) {
            return new int[]{255, 255, 0, a};
        }

        if (iteration > 5) {
            int i = iteration - 6;
            float r = 1.0F - i * (1F / 9F);
            float g = 1F - i * (1F / 9F);
            float b = i * (1F / 5F);
            return new int[]{(int) (r * 255), (int) (g * 255), (int) (b * 255), a};
        }
        return new int[]{255, 255, 255, a};
    }



    protected int[] getColorFull(Entity entity) {

        if (entity instanceof VortexEntity) {
            return new int[]{0x38, 0x98, 0xb3, 255};
        }

        if (entity instanceof RagingVortexEntity) {
            return new int[]{0xe8, 0x39, 0x0d, 255};
        }
        return new int[]{0xFF, 0xB9, 0x00, 255};
    }

    protected int[] getColorNone(Entity entity) {

        if (entity instanceof VortexEntity) {
            return new int[]{0x38, 0x98, 0xb3, 0};
        }

        if (entity instanceof RagingVortexEntity) {
            return new int[]{0xe8, 0x39, 0x0d, 0};
        }
        return new int[]{0xFF, 0xB9, 0x00, 0};
    }

    protected ResourceLocation discTex() {
        return RefStrings.resourceLocation("textures/entity/bhole_disc.png");
    }



    protected static Vec3 rotateY(Vec3 vec, float angle) {

        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(vec.x * cos + vec.z * sin, vec.y, -vec.x * sin + vec.z * cos);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return RefStrings.resourceLocation("textures/models/blackhole.png");
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    protected int getBlockLightLevel(T entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }
}