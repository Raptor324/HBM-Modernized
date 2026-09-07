package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;

import com.hbm_m.blockentity.machines.fusion.FusionPlasmaForgeBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1-Port von {@code RenderFusionPlasmaForge} (1.7.10).
 *
 * <p>Der Hallenkoerper ({@code Body}) steckt im Blockmodell; hier laufen der drehbare Ring mit
 * beiden Roboterarmen (Schlaegerarm mit zwei Kolben, Duesenarm mit Plasmaflamme) und - wenn ein
 * Torus angeschlossen ist - der Bolzenkranz an der Einspeiseseite.</p>
 *
 * <p>Ebenfalls enthalten: das Plasmabecken ({@code renderPlasma}), der schwebende Gegenstand des
 * laufenden Rezepts ({@code renderItem}) und die Plasmasaeule darueber ({@code renderBeam}).</p>
 */
public class FusionPlasmaForgeRenderer implements BlockEntityRenderer<FusionPlasmaForgeBlockEntity> {

    private static final String OBJ = "models/block/machines/plasma_forge.obj";
    private static final String TORUS_OBJ = "models/block/machines/torus.obj";

    private static final ResourceLocation PLASMA_TEX = tex("block/machine/plasma");
    private static final ResourceLocation PLASMA_GLOW_TEX = tex("block/machine/plasma_glow");
    /** Original: {@code Fluids.STELLAR_FLUX.getTexture()} - das Original recycelt hier bewusst die Fluidtextur. */
    private static final ResourceLocation BEAM_TEX = tex("block/fluids/stellar_flux");

    private static ResourceLocation tex(String path) {
        //? if fabric && < 1.21.1 {
        /*return new ResourceLocation(RefStrings.MODID, "textures/" + path + ".png");
        *///?} else {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/" + path + ".png");
        //?}
    }

    public FusionPlasmaForgeRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public boolean shouldRenderOffScreen(FusionPlasmaForgeBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 192;
    }

    @Override
    public void render(FusionPlasmaForgeBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(OBJ);
        if (obj.isEmpty()) return;

        TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/plasma_forge");
        VertexConsumer vc = buffer.getBuffer(RenderType.cutout());

        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        FusionTorusRenderer.applyFacing(be.getBlockState(), pose);
        // Original: eine zusaetzliche 90-Grad-Drehung vor der Ausrichtung.
        pose.mulPose(Axis.YP.rotationDegrees(90F));

        // Bolzenkranz der Einspeiseseite, sobald ein Plasmanetz haengt.
        if (be.connected) {
            Map<String, List<float[]>> torusObj = RBMKColumnRenderer.getObj(TORUS_OBJ);
            TextureAtlasSprite torusSprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/torus");
            pose.pushPose();
            pose.translate(-2D, 0D, 0D);
            RBMKColumnRenderer.renderObjGroup(vc, pose.last().pose(), torusObj.get("Bolts1"), torusSprite,
                    1F, 1F, 1F, packedLight, packedOverlay);
            pose.popPose();
        }

        renderPlasma(be, vc, pose, buffer, obj, sprite, packedLight, packedOverlay);
        renderRecipeItem(be, pose, buffer, partialTick, packedLight);
        renderBeam(be, pose, buffer, partialTick);

        double[] striker = be.armStriker.getPositions(partialTick);
        double[] jet = be.armJet.getPositions(partialTick);
        double rotor = be.prevRing + (be.ring - be.prevRing) * partialTick;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((float) rotor));

        renderStrikerArm(be, vc, pose, obj, sprite, striker, packedLight, packedOverlay);
        renderJetArm(be, vc, pose, obj, sprite, jet, buffer, packedLight, packedOverlay);

        pose.popPose();
        pose.popPose();
    }

    /** 1:1-Port von {@code renderPlasma}: dunkel wenn kalt, sonst drei leuchtende Schichten. */
    private void renderPlasma(FusionPlasmaForgeBlockEntity be, VertexConsumer vc, PoseStack pose,
                              MultiBufferSource buffer, Map<String, List<float[]>> obj,
                              TextureAtlasSprite sprite, int light, int overlay) {

        List<float[]> plasma = obj.get("Plasma");
        if (plasma == null) return;

        if (be.plasmaEnergySync <= 0) {
            // Original: schwarz eingefaerbt, ohne Textur.
            RBMKColumnRenderer.renderObjGroup(vc, pose.last().pose(), plasma, sprite,
                    0F, 0F, 0F, light, overlay);
            return;
        }

        long time = System.currentTimeMillis() + be.renderTimeOffset();
        float alpha = 0.5F + (float) (Math.sin(time / 500D) * 0.25F);
        double mainOsc = sps(time / 750D) % 1D;
        double glowOsc = Math.sin(time / 1000D) % 1D;
        double glowExtra = time / 10000D % 1D;

        int fullBright = LightTexture.pack(15, 15);

        FusionTorusRenderer.drawScrolledGroup(buffer.getBuffer(RenderType.eyes(PLASMA_TEX)), pose, plasma,
                0D, mainOsc, be.plasmaRed * alpha, be.plasmaGreen * alpha, be.plasmaBlue * alpha, 1F, fullBright);

        FusionTorusRenderer.drawScrolledGroup(buffer.getBuffer(RenderType.eyes(PLASMA_GLOW_TEX)), pose, plasma,
                0D, glowOsc + glowExtra, be.plasmaRed * 2F, be.plasmaGreen * 2F, be.plasmaBlue * 2F, 1F, fullBright);

        double glowOsc2 = Math.sin(time / 600D + 2) % 1D;
        double glowExtra2 = time / 5000D % 1D;
        FusionTorusRenderer.drawScrolledGroup(buffer.getBuffer(RenderType.eyes(PLASMA_GLOW_TEX)), pose, plasma,
                0D, glowOsc2 + glowExtra2, be.plasmaRed * 2F, be.plasmaGreen * 2F, be.plasmaBlue * 2F, 1F, fullBright);
    }

    /** Original: {@code BobMathUtil.sps}. */
    private static double sps(double x) {
        return (Math.sin(x) + 1D) * 0.5D;
    }

    /** 1:1-Port von {@code renderItem}: das Rezeptergebnis schwebt ueber dem Amboss. */
    private void renderRecipeItem(FusionPlasmaForgeBlockEntity be, PoseStack pose,
                                  MultiBufferSource buffer, float partialTick, int light) {

        Minecraft mc = Minecraft.getInstance();
        if (be.getLevel() == null || mc.player == null) return;
        // Original: ab 35 Bloecken Abstand wird nicht mehr gezeichnet.
        if (mc.player.distanceToSqr(be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 1,
                be.getBlockPos().getZ() + 0.5) > 35 * 35) return;

        var recipe = be.getRecipe(be.getLevel());
        if (recipe == null) return;

        ItemStack stack = recipe.getOutput();
        if (stack.isEmpty()) return;
        stack.setCount(1);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(90F));
        pose.translate(0D, 1.75D, 0D);

        float bob = (float) (Math.sin((mc.player.tickCount + partialTick) * 0.1) * 0.0625);
        pose.translate(0D, bob, 0D);
        pose.scale(1.5F, 1.5F, 1.5F);

        mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                pose, buffer, be.getLevel(), 0);

        pose.popPose();
    }

    /** 1:1-Port von {@code renderBeam}: vier Wandflaechen, die nach oben hin ausblenden. */
    private void renderBeam(FusionPlasmaForgeBlockEntity be, PoseStack pose,
                            MultiBufferSource buffer, float partialTick) {

        Minecraft mc = Minecraft.getInstance();
        if (be.getLevel() == null || mc.player == null) return;
        if (mc.player.distanceToSqr(be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 1,
                be.getBlockPos().getZ() + 0.5) > 50 * 50) return;

        if (be.getRecipe(be.getLevel()) == null) return;

        VertexConsumer vc = buffer.getBuffer(RenderType.eyes(BEAM_TEX));
        org.joml.Matrix4f m = pose.last().pose();

        float offset = (float) (((mc.player.tickCount + partialTick) / 15D) % 1D);
        float in = 0.4375F;
        float b = 1F;
        float t = 1.5F;
        float h = b + t;

        beamQuad(vc, m, -in, b, in, offset + t, 0, -in, h, in, offset, 0,
                -in, h, -in, offset, 1, -in, b, -in, offset + t, 1);
        beamQuad(vc, m, in, h, in, offset, 0, in, b, in, offset + t, 0,
                in, b, -in, offset + t, 1, in, h, -in, offset, 1);
        beamQuad(vc, m, in, b, in, offset + t, 0, in, h, in, offset, 0,
                -in, h, in, offset, 1, -in, b, in, offset + t, 1);
        beamQuad(vc, m, in, h, -in, offset, 0, in, b, -in, offset + t, 0,
                -in, b, -in, offset + t, 1, -in, h, -in, offset, 1);
    }

    /** Eckpunkte auf Basishoehe sind deckend, die oberen transparent - wie im Original. */
    private static void beamQuad(VertexConsumer vc, org.joml.Matrix4f m,
                                 float x0, float y0, float z0, float u0, float v0,
                                 float x1, float y1, float z1, float u1, float v1,
                                 float x2, float y2, float z2, float u2, float v2,
                                 float x3, float y3, float z3, float u3, float v3) {
        float base = 1F;
        vc.vertex(m, x0, y0, z0).color(1F, 1F, 1F, y0 <= base ? 1F : 0F).uv(u0, v0).endVertex();
        vc.vertex(m, x1, y1, z1).color(1F, 1F, 1F, y1 <= base ? 1F : 0F).uv(u1, v1).endVertex();
        vc.vertex(m, x2, y2, z2).color(1F, 1F, 1F, y2 <= base ? 1F : 0F).uv(u2, v2).endVertex();
        vc.vertex(m, x3, y3, z3).color(1F, 1F, 1F, y3 <= base ? 1F : 0F).uv(u3, v3).endVertex();
    }

    private void renderStrikerArm(FusionPlasmaForgeBlockEntity be, VertexConsumer vc, PoseStack pose,
                                  Map<String, List<float[]>> obj, TextureAtlasSprite sprite,
                                  double[] a, int light, int overlay) {
        pose.pushPose();

        draw(vc, pose, obj, "SliderStriker", sprite, light, overlay);

        pose.translate(-2.75D, 2.5D, 0D);
        pose.mulPose(Axis.ZP.rotationDegrees((float) -a[0]));
        pose.translate(2.75D, -2.5D, 0D);
        draw(vc, pose, obj, "ArmLowerStriker", sprite, light, overlay);

        pose.translate(-2.75D, 3.75D, 0D);
        pose.mulPose(Axis.ZP.rotationDegrees((float) -a[1]));
        pose.translate(2.75D, -3.75D, 0D);
        draw(vc, pose, obj, "ArmUpperStriker", sprite, light, overlay);

        pose.translate(-1.5D, 3.75D, 0D);
        pose.mulPose(Axis.ZP.rotationDegrees((float) -a[2]));
        pose.translate(1.5D, -3.75D, 0D);
        draw(vc, pose, obj, "StrikerMount", sprite, light, overlay);

        pose.pushPose();
        pose.translate(0D, 3.375D, 0.5D);
        pose.mulPose(Axis.XP.rotationDegrees((float) a[3]));
        pose.translate(0D, -3.375D, -0.5D);
        draw(vc, pose, obj, "StrikerRight", sprite, light, overlay);
        pose.translate(0D, -a[4], 0D);
        draw(vc, pose, obj, "PistonRight", sprite, light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0D, 3.375D, -0.5D);
        pose.mulPose(Axis.XP.rotationDegrees((float) -a[3]));
        pose.translate(0D, -3.375D, 0.5D);
        draw(vc, pose, obj, "StrikerLeft", sprite, light, overlay);
        pose.translate(0D, -a[5], 0D);
        draw(vc, pose, obj, "PistonLeft", sprite, light, overlay);
        pose.popPose();

        pose.popPose();
    }

    private void renderJetArm(FusionPlasmaForgeBlockEntity be, VertexConsumer vc, PoseStack pose,
                              Map<String, List<float[]>> obj, TextureAtlasSprite sprite,
                              double[] a, MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();

        draw(vc, pose, obj, "SliderJet", sprite, light, overlay);

        pose.translate(2.75D, 2.5D, 0D);
        pose.mulPose(Axis.ZP.rotationDegrees((float) a[0]));
        pose.translate(-2.75D, -2.5D, 0D);
        draw(vc, pose, obj, "ArmLowerJet", sprite, light, overlay);

        pose.translate(2.75D, 3.75D, 0D);
        pose.mulPose(Axis.ZP.rotationDegrees((float) a[1]));
        pose.translate(-2.75D, -3.75D, 0D);
        draw(vc, pose, obj, "ArmUpperJet", sprite, light, overlay);

        pose.translate(1.5D, 3.75D, 0D);
        pose.mulPose(Axis.ZP.rotationDegrees((float) a[2]));
        pose.translate(-1.5D, -3.75D, 0D);
        draw(vc, pose, obj, "Jet", sprite, light, overlay);

        // Original: Flamme nur, wenn die Schmiede laeuft, der Arm steht und nicht in Grundstellung ist.
        if (be.didProcess && be.armJet.angles[2] == be.armJet.prevAngles[2] && be.armJet.angles[2] != 0) {
            renderJetFlame(be, pose, buffer);
        }

        pose.popPose();
    }

    /** 1:1-Port von {@code renderJet}: vier additive Quads, die nach unten hin ausblenden. */
    private void renderJetFlame(FusionPlasmaForgeBlockEntity be, PoseStack pose, MultiBufferSource buffer) {

        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        var matrix = pose.last().pose();

        double outerLen = 1 + FusionPlasmaForgeBlockEntity.RAND.nextDouble() * 0.125;
        double narrow = 0.01;
        double side = 0.125;
        double near = 1.375;
        double far = 1.625;

        float r = Mth.clamp(be.plasmaRed, 0F, 1F);
        float g = Mth.clamp(be.plasmaGreen, 0F, 1F);
        float b = Mth.clamp(be.plasmaBlue, 0F, 1F);

        quad(vc, matrix, r, g, b,
                near, 3, side, far, 3, side,
                far - narrow, 3 - outerLen, side - narrow, near + narrow, 3 - outerLen, side - narrow);
        quad(vc, matrix, r, g, b,
                near, 3, -side, far, 3, -side,
                far - narrow, 3 - outerLen, -side + narrow, near + narrow, 3 - outerLen, -side + narrow);
        quad(vc, matrix, r, g, b,
                near, 3, side, near, 3, -side,
                near + narrow, 3 - outerLen, -side + narrow, near + narrow, 3 - outerLen, side - narrow);
        quad(vc, matrix, r, g, b,
                far, 3, side, far, 3, -side,
                far - narrow, 3 - outerLen, -side + narrow, far - narrow, 3 - outerLen, side - narrow);
    }

    /** Zwei Eckpunkte voll deckend, die beiden unteren transparent - wie im Original. */
    private static void quad(VertexConsumer vc, org.joml.Matrix4f m, float r, float g, float b,
                             double x0, double y0, double z0, double x1, double y1, double z1,
                             double x2, double y2, double z2, double x3, double y3, double z3) {
        vc.vertex(m, (float) x0, (float) y0, (float) z0).color(r, g, b, 1F).endVertex();
        vc.vertex(m, (float) x1, (float) y1, (float) z1).color(r, g, b, 1F).endVertex();
        vc.vertex(m, (float) x2, (float) y2, (float) z2).color(r, g, b, 0F).endVertex();
        vc.vertex(m, (float) x3, (float) y3, (float) z3).color(r, g, b, 0F).endVertex();
    }

    private static void draw(VertexConsumer vc, PoseStack pose, Map<String, List<float[]>> obj,
                             String group, TextureAtlasSprite sprite, int light, int overlay) {
        RBMKColumnRenderer.renderObjGroup(vc, pose.last().pose(), obj.get(group), sprite,
                1F, 1F, 1F, light, overlay);
    }
}
