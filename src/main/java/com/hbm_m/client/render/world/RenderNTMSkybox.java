package com.hbm_m.client.render.world;

import org.joml.Matrix4f;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.GeneralConfig;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.handler.ImpactWorldHandler;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.util.Vec3NT;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 port of sky additions from {@code RenderNTMSkyboxChainloader} and {@code RenderNTMSkyboxImpact}.
 * <p>Original hooks as sky renderer before/after existing sky; here rendering executes directly
 * after vanilla sky ({@code AFTER_SKY}). Normal: Lode Star (visible only through polarized glass), Digamma Star
 * and Bobmazon satellite. Post-impact (dust/fire > 0): Digamma Star with alpha {@code dust} and
 * satellite with alpha {@code dust * (1 - rain)}; sun/moon/stars and dawn attenuated by {@code LevelRendererImpactMixin}.</p>
 */
public final class RenderNTMSkybox {

    private RenderNTMSkybox() {}

    private static final ResourceLocation digammaStar = tex("textures/misc/star_digamma.png");
    private static final ResourceLocation lodeStar = tex("textures/misc/star_lode.png");
    private static final ResourceLocation bobmazonSat = tex("textures/misc/sat_bobmazon.png");

    /** Original {@code ModEventHandlerClient.renderLodeStar} / {@code lastStarCheck}. */
    public static boolean renderLodeStar = false;
    public static long lastStarCheck = 0L;

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
    }

    /** Original {@code ModEventHandlerClient.onClientTickLast}: impact sky or chainloader, overworld only. */
    public static boolean isImpactSky(ClientLevel world) {
        return ImpactWorldHandler.getDustForClient(world) > 0 || ImpactWorldHandler.getFireForClient(world) > 0;
    }

    public static void renderAfterSky(PoseStack poseStack, float partialTicks) {
        if (!GeneralConfig.enableSkyboxes) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;
        if (world == null || mc.player == null) return;
        if (world.dimension() != Level.OVERWORLD) return;
        if (com.hbm_m.api.space.SpaceApi.overridesSky(world)) return; // Space-Addon zeichnet den Himmel selbst

        // Sky base (R_cam): on 1.20.1 event pose stack already includes rotation; on 1.21.1 identity.
        com.hbm_m.platform.RenderHooks.applySkyViewBasis(poseStack);

        // Lode Star raycast runs in client tick (CLIENT_POST), not per frame.

        poseStack.pushPose();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc( org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        if (isImpactSky(world)) {
            renderImpact(poseStack, partialTicks, mc, world);
        } else {
            renderChainloader(poseStack, partialTicks, mc, world);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        poseStack.popPose();
    }

    /** {@code RenderNTMSkyboxChainloader.render} section executed after parent sky. */
    private static void renderChainloader(PoseStack ps, float partialTicks, Minecraft mc, ClientLevel world) {
        if (renderLodeStar) {
            // Original twinkle size - sampled only when star is rendered (client world random must not be consumed from render thread per frame).
            float var12 = 0.5F + world.random.nextFloat() * 0.25F;
            ps.pushPose();
            ps.mulPose(Axis.XP.rotationDegrees(-75.0F));
            ps.mulPose(Axis.YP.rotationDegrees(10.0F));
            quad(ps, lodeStar, var12, 100D, 1F, 1F, 1F, 1.0F);
            ps.popPose();
        }
        renderCelestialObjects(ps, partialTicks, mc, world, 1.0F, 1.0F);
    }

    /** Digamma / satellite section from {@code RenderNTMSkyboxImpact.render}. */
    private static void renderImpact(PoseStack ps, float partialTicks, Minecraft mc, ClientLevel world) {
        float atmosphericDust = ImpactWorldHandler.getDustForClient(world);
        float dust = Math.max((1.0F - (atmosphericDust * 2)), 0);
        float rain = dust * (1.0F - world.getRainLevel(partialTicks));
        renderCelestialObjects(ps, partialTicks, mc, world, dust, rain);
    }

    /** Shared Digamma star / satellite section of both sky variants (differing only in alpha values). */
    private static void renderCelestialObjects(PoseStack ps, float partialTicks, Minecraft mc, ClientLevel world,
                                               float starAlpha, float satAlpha) {
        float celestial = world.getTimeOfDay(partialTicks);
        float brightness = (float) Math.sin(celestial * Math.PI);
        brightness *= brightness;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-90.0F));
        ps.mulPose(Axis.XP.rotationDegrees(celestial * 360.0F));
        ps.mulPose(Axis.XP.rotationDegrees(140.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(-40.0F));

        float digamma = HbmLivingProps.getDigamma(mc.player);
        float var12 = 1F * (1 + digamma * 0.25F);
        double dist = 100D - digamma * 2.5;

        quad(ps, digammaStar, var12, dist, brightness, brightness, brightness, starAlpha);
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(-40.0F));
        ps.mulPose(Axis.YP.rotationDegrees(System.currentTimeMillis() % (360 * 1000) / 1000F));
        ps.mulPose(Axis.XP.rotationDegrees(System.currentTimeMillis() % (360 * 100) / 100F));

        var12 = 0.5F;
        dist = 100D;

        // Original: color of Digamma star remains active
        quad(ps, bobmazonSat, var12, dist, brightness, brightness, brightness, satAlpha);
        ps.popPose();
    }

    /** Original tessellator quads: (-s,-s) 0/0, (s,-s) 0/1, (s,s) 1/1, (-s,s) 1/0. */
    private static void quad(PoseStack ps, ResourceLocation texture, float s, double dist, float r, float g, float b, float a) {
        if (a <= 0.0F || r <= 0.0F && g <= 0.0F && b <= 0.0F) return;
        Matrix4f m = ps.last().pose();
        float d = (float) dist;
        if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive()
                && com.hbm_m.client.render.shader.PackAdditiveQueue.engaged()) {
            float[] verts = new float[] {
                    -s, d, -s, 0.0F, 0.0F, r, g, b, a, 0.0F, 1.0F, 0.0F,
                     s, d, -s, 0.0F, 1.0F, r, g, b, a, 0.0F, 1.0F, 0.0F,
                     s, d,  s, 1.0F, 1.0F, r, g, b, a, 0.0F, 1.0F, 0.0F,
                    -s, d,  s, 1.0F, 0.0F, r, g, b, a, 0.0F, 1.0F, 0.0F
            };
            com.hbm_m.client.render.shader.PackAdditiveQueue.pushWithDepthMode(
                    texture, org.lwjgl.opengl.GL11.GL_QUADS, m, verts, verts.length,
                    com.hbm_m.client.render.shader.PackAdditiveQueue.DEPTH_MODE_SKY);
            return;
        }
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(r, g, b, a);
        com.mojang.blaze3d.vertex.BufferBuilder bb = com.hbm_m.platform.RenderHooks.beginTesselator(
                Tesselator.getInstance(), VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        com.hbm_m.platform.RenderHooks.vertexTex(bb, m, -s, d, -s, 0.0F, 0.0F);
        com.hbm_m.platform.RenderHooks.vertexTex(bb, m, s, d, -s, 0.0F, 1.0F);
        com.hbm_m.platform.RenderHooks.vertexTex(bb, m, s, d, s, 1.0F, 1.0F);
        com.hbm_m.platform.RenderHooks.vertexTex(bb, m, -s, d, s, 1.0F, 0.0F);
        com.hbm_m.platform.RenderHooks.drawWithShader(bb);
    }

    /**
     * Original {@code onClientTickLast}: raycast 25 blocks north elevated 15 degrees every 200 ms;
     * if it intersects polarized glass, the Lode Star becomes visible. Executes in CLIENT_POST tick
     * rather than render frame.
     */
    public static void updateLodeStar(Minecraft mc, ClientLevel world) {
        long millis = System.currentTimeMillis();
        if (lastStarCheck + 200 < millis) {
            renderLodeStar = false;
            lastStarCheck = millis;

            if (mc.player != null) {
                Vec3 eye = mc.player.getEyePosition();
                Vec3NT pos = new Vec3NT(eye.x, eye.y, eye.z);
                Vec3NT lodestarHeading = new Vec3NT(0, 0, -1D).rotateAroundXDeg(-15).multiply(25);
                Vec3NT nextPos = new Vec3NT(pos.xCoord, pos.yCoord, pos.zCoord).add(lodestarHeading.xCoord, lodestarHeading.yCoord, lodestarHeading.zCoord);
                BlockHitResult mop = world.clip(new ClipContext(pos.toVec3(), nextPos.toVec3(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                if (mop != null && mop.getType() == HitResult.Type.BLOCK && world.getBlockState(mop.getBlockPos()).is(ModBlocks.GLASS_POLARIZED.get())) {
                    renderLodeStar = true;
                }
            }
        }
    }
}
