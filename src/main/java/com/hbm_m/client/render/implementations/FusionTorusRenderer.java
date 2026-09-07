package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;

import com.hbm_m.blockentity.machines.fusion.FusionTorusBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.FusionRecipe;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code RenderFusionTorus} (1.7.10).
 *
 * <p>Das statische Gehaeuse ({@code Torus}) steckt im Blockmodell; hier laufen nur die beweglichen
 * und bedingten Teile: der rotierende Magnetring, die vier Bolzenkraenze je nach angeschlossenem
 * Port und das Plasma in Rezeptfarbe.</p>
 *
 * <p><b>Abweichung:</b> Das Original scrollt die drei Plasmaschichten ueber die Texturmatrix.
 * Hier wird stattdessen der UV-Versatz beim Aufbau der Geometrie addiert - das Ergebnis ist
 * dasselbe Wandern der Textur, ohne dass eine Texturmatrix gebraucht wird.</p>
 */
public class FusionTorusRenderer implements BlockEntityRenderer<FusionTorusBlockEntity> {

    private static final String OBJ = "models/block/machines/torus.obj";

    private static final ResourceLocation PLASMA_TEX = tex("plasma");
    private static final ResourceLocation PLASMA_GLOW_TEX = tex("plasma_glow");
    private static final ResourceLocation PLASMA_SPARKLE_TEX = tex("plasma_sparkle");

    private static ResourceLocation tex(String name) {
        //? if fabric && < 1.21.1 {
        /*return new ResourceLocation(RefStrings.MODID, "textures/block/machine/" + name + ".png");
        *///?} else {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/" + name + ".png");
        //?}
    }

    public FusionTorusRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public boolean shouldRenderOffScreen(FusionTorusBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void render(FusionTorusBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(OBJ);
        if (obj.isEmpty()) return;

        TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/torus");

        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        applyFacing(be.getBlockState(), pose);

        VertexConsumer solid = buffer.getBuffer(RenderType.cutout());

        // Magnetring
        pose.pushPose();
        float rot = Mth.lerp(partialTick, be.prevMagnet, be.magnet);
        pose.mulPose(Axis.YP.rotationDegrees(rot));
        RBMKColumnRenderer.renderObjGroup(solid, pose.last().pose(), obj.get("Magnet"), sprite,
                1F, 1F, 1F, packedLight, packedOverlay);
        pose.popPose();

        // Bolzenkraenze - Zuordnung 1:1 aus dem Original (0->Bolts2, 1->Bolts4, 2->Bolts3, 3->Bolts1)
        if (be.connections[0]) renderGroup(solid, pose, obj, "Bolts2", sprite, packedLight, packedOverlay);
        if (be.connections[1]) renderGroup(solid, pose, obj, "Bolts4", sprite, packedLight, packedOverlay);
        if (be.connections[2]) renderGroup(solid, pose, obj, "Bolts3", sprite, packedLight, packedOverlay);
        if (be.connections[3]) renderGroup(solid, pose, obj, "Bolts1", sprite, packedLight, packedOverlay);

        renderPlasma(be, pose, buffer, obj);

        pose.popPose();
    }

    private void renderPlasma(FusionTorusBlockEntity be, PoseStack pose, MultiBufferSource buffer,
                              Map<String, List<float[]>> obj) {

        if (be.plasmaEnergy <= 0 || be.getLevel() == null) return;

        FusionRecipe recipe = be.getRecipe(be.getLevel());
        if (recipe == null) return;

        List<float[]> plasma = obj.get("Plasma");
        if (plasma == null) return;

        long time = System.currentTimeMillis() + be.renderTimeOffset();

        float alpha = 0.35F + (float) (Math.sin(time / 1000D) * 0.25F);
        float r = recipe.getR();
        float g = recipe.getG();
        float b = recipe.getB();

        double mainOsc = sps(time / 1000D) % 1D;
        double glowOsc = Math.sin(time / 2000D) % 1D;
        double glowExtra = time / 10000D % 1D;
        double sparkleSpin = time / 500D * -1 % 1D;
        double sparkleOsc = Math.sin(time / 1000D) * 0.5D % 1D;

        // Vollhelle Beleuchtung wie im Original (setLightmapTextureCoords 240/240).
        int fullBright = LightTexture.pack(15, 15);

        drawScrolledGroup(buffer.getBuffer(RenderType.eyes(PLASMA_TEX)), pose, plasma,
                0D, mainOsc, r, g, b, alpha, fullBright);

        // Sparmassnahme des Originals: Zusatzschichten nur innerhalb von 100 Bloecken.
        if (Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.distanceToSqr(
                        be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 2.5,
                        be.getBlockPos().getZ() + 0.5) < 100 * 100) {

            drawScrolledGroup(buffer.getBuffer(RenderType.eyes(PLASMA_GLOW_TEX)), pose, plasma,
                    0D, glowOsc + glowExtra, r * 2F, g * 2F, b * 2F, alpha * 2F, fullBright);

            drawScrolledGroup(buffer.getBuffer(RenderType.eyes(PLASMA_SPARKLE_TEX)), pose, plasma,
                    sparkleSpin, sparkleOsc, r * 2F, g * 2F, b * 2F, 0.75F, fullBright);
        }
    }

    /** Original: {@code BobMathUtil.sps} - Sinus, auf 0..1 abgebildet. */
    private static double sps(double x) {
        return (Math.sin(x) + 1D) * 0.5D;
    }

    private static void renderGroup(VertexConsumer vc, PoseStack pose, Map<String, List<float[]>> obj,
                                    String group, TextureAtlasSprite sprite, int light, int overlay) {
        RBMKColumnRenderer.renderObjGroup(vc, pose.last().pose(), obj.get(group), sprite,
                1F, 1F, 1F, light, overlay);
    }

    /**
     * Zeichnet eine Gruppe mit direkter Textur (kein Atlas) und einem UV-Versatz - das ersetzt die
     * Texturmatrix-Verschiebung des Originals.
     */
    static void drawScrolledGroup(VertexConsumer vc, PoseStack pose, List<float[]> triangles,
                                     double du, double dv, float r, float g, float b, float a, int light) {
        var matrix = pose.last().pose();
        var normal = pose.last().normal();

        float cr = Mth.clamp(r, 0F, 1F);
        float cg = Mth.clamp(g, 0F, 1F);
        float cb = Mth.clamp(b, 0F, 1F);
        float ca = Mth.clamp(a, 0F, 1F);

        for (float[] tri : triangles) {
            for (int i = 0; i < 3; i++) {
                int base = i * 8;
                float u = (float) (tri[base + 3] + du);
                float v = (float) (tri[base + 4] + dv);
                vc.vertex(matrix, tri[base], tri[base + 1], tri[base + 2])
                        .color(cr, cg, cb, ca)
                        .uv(u, 1F - v)
                        .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                        .uv2(light)
                        .normal(normal, tri[base + 5], tri[base + 6], tri[base + 7])
                        .endVertex();
            }
        }
    }

    /** Blockstate-Drehung, identisch zu den uebrigen Maschinen-Renderern dieses Ports. */
    static void applyFacing(BlockState state, PoseStack pose) {
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) return;
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        float rot = switch (facing) {
            case SOUTH -> 180F;
            case EAST -> 270F;
            case WEST -> 90F;
            default -> 0F;
        };
        if (rot != 0F) pose.mulPose(Axis.YP.rotationDegrees(rot));
    }
}
