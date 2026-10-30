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
 * 1:1-Port der Himmelszusaetze aus {@code RenderNTMSkyboxChainloader} und {@code RenderNTMSkyboxImpact}.
 * <p>Das Original haengt sich als Sky-Renderer vor/hinter den vorhandenen Himmel; hier laeuft das Zeichnen direkt
 * nach dem Vanilla-Himmel ({@code AFTER_SKY}). Normal: Leitstern (nur durch polarisiertes Glas), Digamma-Stern
 * und Bobmazon-Satellit. Nach dem Einschlag (Staub/Feuer > 0): Digamma-Stern mit Alpha {@code dust} und
 * Satellit mit Alpha {@code dust * (1 - Regen)}; Sonne/Mond/Sterne und Morgenrot daempft
 * {@code LevelRendererImpactMixin}.</p>
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

    /** Original {@code ModEventHandlerClient.onClientTickLast}: impact-Himmel, sonst Chainloader, nur Oberwelt. */
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

        // Himmel-Basis (R_cam): auf 1.20.1 traegt das Event-PoseStack die Rotation bereits,
        // auf 1.21.1 ist es eine leere Identity - der Hook ergaenzte dort die Kamerarotation.
        com.hbm_m.platform.RenderHooks.applySkyViewBasis(poseStack);

        // Leitstern-Raycast laeuft im Client-Tick (CLIENT_POST), nicht pro Frame.

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

    /** {@code RenderNTMSkyboxChainloader.render} ab der Stelle nach dem Elternhimmel. */
    private static void renderChainloader(PoseStack ps, float partialTicks, Minecraft mc, ClientLevel world) {
        if (renderLodeStar) {
            // Twinkle-Size des Originals - nur ziehen, wenn der Stern auch gezeichnet
            // wird (der Zufall des Client-Worlds darf nicht pro Frame aus dem
            // Render-Thread konsumiert werden, unsichtbar oder nicht).
            float var12 = 0.5F + world.random.nextFloat() * 0.25F;
            ps.pushPose();
            ps.mulPose(Axis.XP.rotationDegrees(-75.0F));
            ps.mulPose(Axis.YP.rotationDegrees(10.0F));
            quad(ps, lodeStar, var12, 100D, 1F, 1F, 1F, 1.0F);
            ps.popPose();
        }
        renderCelestialObjects(ps, partialTicks, mc, world, 1.0F, 1.0F);
    }

    /** Der Digamma-/Satelliten-Block aus {@code RenderNTMSkyboxImpact.render}. */
    private static void renderImpact(PoseStack ps, float partialTicks, Minecraft mc, ClientLevel world) {
        float atmosphericDust = ImpactWorldHandler.getDustForClient(world);
        float dust = Math.max((1.0F - (atmosphericDust * 2)), 0);
        float rain = dust * (1.0F - world.getRainLevel(partialTicks));
        renderCelestialObjects(ps, partialTicks, mc, world, dust, rain);
    }

    /** Gemeinsamer Digamma-Stern/Satellit-Block beider Sky-Varianten (nur die Alpha-Werte unterscheiden sich). */
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

        // Original: die Farbe vom Digamma-Stern bleibt gesetzt
        quad(ps, bobmazonSat, var12, dist, brightness, brightness, brightness, satAlpha);
        ps.popPose();
    }

    /** Die Tessellator-Quads des Originals: (-s,-s) 0/0, (s,-s) 0/1, (s,s) 1/1, (-s,s) 1/0. */
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
     * Original {@code onClientTickLast}: alle 200 ms ein Strahl 25 Bloecke nach Norden, 15 Grad angehoben; trifft er
     * polarisiertes Glas, ist der Leitstern zu sehen. Laeuft im CLIENT_POST-Tick - der
     * Raycast gehoert nicht in den Render-Frame (Reihenfolge: Tick N setzt das Flag,
     * Frame N zeichnet danach damit).
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
