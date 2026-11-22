package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.fusion.FusionPlasmaForgeBlockEntity;
import com.hbm_m.blockentity.machines.fusion.FusionTorusBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.hbm_m.recipe.FusionRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Nucleus renderers for thermonuclear fusion installations:
 * 1. Fusion Torus: static chamber torus, GPU rotating magnetic confinement ring,
 *    4 quadrant conditional port coupling bolts, and triple-layer scrolled toroidal
 *    plasma ring hook in recipe-modulated emission colors.
 * 2. Fusion Plasma Forge: static forge hall, feed-side torus bolts, rotating robotic
 *    gantry with striker arm and plasma jet arm joint hierarchies, glowing plasma
 *    crucible pool, hovering forged recipe item, upward plasma pillar, and jet torch flame.
 */
public final class MachineFusionComplexRenderer {

    private static final String TORUS_OBJ = "models/block/machines/torus.obj";
    private static final String FORGE_OBJ = "models/block/machines/plasma_forge.obj";

    private static final ResourceLocation PLASMA_TEX = tex("block/machine/plasma");
    private static final ResourceLocation PLASMA_GLOW_TEX = tex("block/machine/plasma_glow");
    private static final ResourceLocation PLASMA_SPARKLE_TEX = tex("block/machine/plasma_sparkle");
    private static final ResourceLocation BEAM_TEX = tex("block/fluids/stellar_flux");

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/" + path + ".png");
    }

    public static void register() {
        // ── Fusion Torus ─────────────────────────────────────────────────────
        MachineRenderers.machine("fusion_torus",
                ModBlockEntities.FUSION_TORUS_BE.get(),
                FusionTorusBlockEntity.class)
            .rotateY("Magnet", 0f, 0f, (be, pt) -> (double) Mth.lerp(pt, be.prevMagnet, be.magnet))
            .conditionalPart("Bolts2", be -> be.connections[0])
            .conditionalPart("Bolts4", be -> be.connections[1])
            .conditionalPart("Bolts3", be -> be.connections[2])
            .conditionalPart("Bolts1", be -> be.connections[3])
            .hook(new MachineRenderHook<FusionTorusBlockEntity>() {
                @Override
                public void render(FusionTorusBlockEntity be, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    if (be.plasmaEnergy <= 0 || be.getLevel() == null) return;

                    FusionRecipe recipe = be.getRecipe(be.getLevel());
                    if (recipe == null) return;

                    Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(TORUS_OBJ);
                    if (obj.isEmpty()) return;

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

                    int fullBright = LightTexture.pack(15, 15);

                    drawScrolledGroup(buf.getBuffer(RenderType.eyes(PLASMA_TEX)), ps, plasma,
                            0D, mainOsc, r, g, b, alpha, fullBright);

                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null && mc.player.distanceToSqr(
                            be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 2.5,
                            be.getBlockPos().getZ() + 0.5) < 100 * 100) {

                        drawScrolledGroup(buf.getBuffer(RenderType.eyes(PLASMA_GLOW_TEX)), ps, plasma,
                                0D, glowOsc + glowExtra, r * 2F, g * 2F, b * 2F, alpha * 2F, fullBright);

                        drawScrolledGroup(buf.getBuffer(RenderType.eyes(PLASMA_SPARKLE_TEX)), ps, plasma,
                                sparkleSpin, sparkleOsc, r * 2F, g * 2F, b * 2F, 0.75F, fullBright);
                    }
                }

                @Override
                public boolean mayDrawContent(FusionTorusBlockEntity be) {
                    return be.plasmaEnergy > 0;
                }
            })
            .register();

        // ── Fusion Plasma Forge ──────────────────────────────────────────────
        MachineRenderers.machine("fusion_plasma_forge",
                ModBlockEntities.FUSION_PLASMA_FORGE_BE.get(),
                FusionPlasmaForgeBlockEntity.class)
            .hook(new MachineRenderHook<FusionPlasmaForgeBlockEntity>() {
                @Override
                public void render(FusionPlasmaForgeBlockEntity be, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(FORGE_OBJ);
                    if (obj.isEmpty()) return;

                    TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/plasma_forge");
                    VertexConsumer vc = buf.getBuffer(RenderType.cutout());

                    // Additional 90 degree rotation relative to facing in original
                    ps.pushPose();
                    ps.mulPose(Axis.YP.rotationDegrees(90F));

                    // Connected feed-side bolt ring
                    if (be.connected) {
                        Map<String, List<float[]>> torusObj = RBMKColumnRenderer.getObj(TORUS_OBJ);
                        TextureAtlasSprite torusSprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/torus");
                        ps.pushPose();
                        ps.translate(-2D, 0D, 0D);
                        RBMKColumnRenderer.renderObjGroup(vc, ps.last().pose(), torusObj.get("Bolts1"), torusSprite,
                                1F, 1F, 1F, light, overlay);
                        ps.popPose();
                    }

                    renderPlasma(be, vc, ps, buf, obj, sprite, light, overlay);
                    renderRecipeItem(be, ps, buf, pt, light);
                    renderBeam(be, ps, buf, pt);

                    double[] striker = be.armStriker.getPositions(pt);
                    double[] jet = be.armJet.getPositions(pt);
                    double rotor = be.prevRing + (be.ring - be.prevRing) * pt;

                    ps.pushPose();
                    ps.mulPose(Axis.YP.rotationDegrees((float) rotor));

                    renderStrikerArm(vc, ps, obj, sprite, striker, light, overlay);
                    renderJetArm(be, vc, ps, obj, sprite, jet, buf, light, overlay);

                    ps.popPose();
                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(FusionPlasmaForgeBlockEntity be) {
                    return true;
                }
            })
            .register();
    }

    private static double sps(double x) {
        return (Math.sin(x) + 1D) * 0.5D;
    }

    private static void renderPlasma(FusionPlasmaForgeBlockEntity be, VertexConsumer vc, PoseStack ps,
                                     MultiBufferSource buffer, Map<String, List<float[]>> obj,
                                     TextureAtlasSprite sprite, int light, int overlay) {
        List<float[]> plasma = obj.get("Plasma");
        if (plasma == null) return;

        if (be.plasmaEnergySync <= 0) {
            RBMKColumnRenderer.renderObjGroup(vc, ps.last().pose(), plasma, sprite, 0F, 0F, 0F, light, overlay);
            return;
        }

        long time = System.currentTimeMillis() + be.renderTimeOffset();
        float alpha = 0.5F + (float) (Math.sin(time / 500D) * 0.25F);
        double mainOsc = sps(time / 750D) % 1D;
        double glowOsc = Math.sin(time / 1000D) % 1D;
        double glowExtra = time / 10000D % 1D;

        int fullBright = LightTexture.pack(15, 15);

        drawScrolledGroup(buffer.getBuffer(RenderType.eyes(PLASMA_TEX)), ps, plasma,
                0D, mainOsc, be.plasmaRed * alpha, be.plasmaGreen * alpha, be.plasmaBlue * alpha, 1F, fullBright);

        drawScrolledGroup(buffer.getBuffer(RenderType.eyes(PLASMA_GLOW_TEX)), ps, plasma,
                0D, glowOsc + glowExtra, be.plasmaRed * 2F, be.plasmaGreen * 2F, be.plasmaBlue * 2F, 1F, fullBright);

        double glowOsc2 = Math.sin(time / 600D + 2) % 1D;
        double glowExtra2 = time / 5000D % 1D;
        drawScrolledGroup(buffer.getBuffer(RenderType.eyes(PLASMA_GLOW_TEX)), ps, plasma,
                0D, glowOsc2 + glowExtra2, be.plasmaRed * 2F, be.plasmaGreen * 2F, be.plasmaBlue * 2F, 1F, fullBright);
    }

    private static void renderRecipeItem(FusionPlasmaForgeBlockEntity be, PoseStack ps,
                                         MultiBufferSource buffer, float pt, int light) {
        Minecraft mc = Minecraft.getInstance();
        if (be.getLevel() == null || mc.player == null) return;
        if (mc.player.distanceToSqr(be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 1,
                be.getBlockPos().getZ() + 0.5) > 35 * 35) return;

        var recipe = be.getRecipe(be.getLevel());
        if (recipe == null) return;

        ItemStack stack = recipe.getOutput();
        if (stack.isEmpty()) return;
        stack.setCount(1);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(90F));
        ps.translate(0D, 1.75D, 0D);

        float bob = (float) (Math.sin((mc.player.tickCount + pt) * 0.1) * 0.0625);
        ps.translate(0D, bob, 0D);
        ps.scale(1.5F, 1.5F, 1.5F);

        mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                ps, buffer, be.getLevel(), 0);

        ps.popPose();
    }

    private static void renderBeam(FusionPlasmaForgeBlockEntity be, PoseStack ps,
                                   MultiBufferSource buffer, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (be.getLevel() == null || mc.player == null) return;
        if (mc.player.distanceToSqr(be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 1,
                be.getBlockPos().getZ() + 0.5) > 50 * 50) return;

        if (be.getRecipe(be.getLevel()) == null) return;

        VertexConsumer vc = buffer.getBuffer(RenderType.eyes(BEAM_TEX));
        org.joml.Matrix4f m = ps.last().pose();

        float offset = (float) (((mc.player.tickCount + pt) / 15D) % 1D);
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

    private static void beamQuad(VertexConsumer vc, org.joml.Matrix4f m,
                                 float x0, float y0, float z0, float u0, float v0,
                                 float x1, float y1, float z1, float u1, float v1,
                                 float x2, float y2, float z2, float u2, float v2,
                                 float x3, float y3, float z3, float u3, float v3) {
        float base = 1F;
        RenderHooks.vertexTexColor(vc, m, x0, y0, z0, u0, v0, 255, 255, 255, y0 <= base ? 255 : 0);
        RenderHooks.vertexTexColor(vc, m, x1, y1, z1, u1, v1, 255, 255, 255, y1 <= base ? 255 : 0);
        RenderHooks.vertexTexColor(vc, m, x2, y2, z2, u2, v2, 255, 255, 255, y2 <= base ? 255 : 0);
        RenderHooks.vertexTexColor(vc, m, x3, y3, z3, u3, v3, 255, 255, 255, y3 <= base ? 255 : 0);
    }

    private static void renderStrikerArm(VertexConsumer vc, PoseStack ps,
                                         Map<String, List<float[]>> obj, TextureAtlasSprite sprite,
                                         double[] a, int light, int overlay) {
        ps.pushPose();

        draw(vc, ps, obj, "SliderStriker", sprite, light, overlay);

        ps.translate(-2.75D, 2.5D, 0D);
        ps.mulPose(Axis.ZP.rotationDegrees((float) -a[0]));
        ps.translate(2.75D, -2.5D, 0D);
        draw(vc, ps, obj, "ArmLowerStriker", sprite, light, overlay);

        ps.translate(-2.75D, 3.75D, 0D);
        ps.mulPose(Axis.ZP.rotationDegrees((float) -a[1]));
        ps.translate(2.75D, -3.75D, 0D);
        draw(vc, ps, obj, "ArmUpperStriker", sprite, light, overlay);

        ps.translate(-1.5D, 3.75D, 0D);
        ps.mulPose(Axis.ZP.rotationDegrees((float) -a[2]));
        ps.translate(1.5D, -3.75D, 0D);
        draw(vc, ps, obj, "StrikerMount", sprite, light, overlay);

        ps.pushPose();
        ps.translate(0D, 3.375D, 0.5D);
        ps.mulPose(Axis.XP.rotationDegrees((float) a[3]));
        ps.translate(0D, -3.375D, -0.5D);
        draw(vc, ps, obj, "StrikerRight", sprite, light, overlay);
        ps.translate(0D, -a[4], 0D);
        draw(vc, ps, obj, "PistonRight", sprite, light, overlay);
        ps.popPose();

        ps.pushPose();
        ps.translate(0D, 3.375D, -0.5D);
        ps.mulPose(Axis.XP.rotationDegrees((float) -a[3]));
        ps.translate(0D, -3.375D, 0.5D);
        draw(vc, ps, obj, "StrikerLeft", sprite, light, overlay);
        ps.translate(0D, -a[5], 0D);
        draw(vc, ps, obj, "PistonLeft", sprite, light, overlay);
        ps.popPose();

        ps.popPose();
    }

    private static void renderJetArm(FusionPlasmaForgeBlockEntity be, VertexConsumer vc, PoseStack ps,
                                     Map<String, List<float[]>> obj, TextureAtlasSprite sprite,
                                     double[] a, MultiBufferSource buffer, int light, int overlay) {
        ps.pushPose();

        draw(vc, ps, obj, "SliderJet", sprite, light, overlay);

        ps.translate(2.75D, 2.5D, 0D);
        ps.mulPose(Axis.ZP.rotationDegrees((float) a[0]));
        ps.translate(-2.75D, -2.5D, 0D);
        draw(vc, ps, obj, "ArmLowerJet", sprite, light, overlay);

        ps.translate(2.75D, 3.75D, 0D);
        ps.mulPose(Axis.ZP.rotationDegrees((float) a[1]));
        ps.translate(-2.75D, -3.75D, 0D);
        draw(vc, ps, obj, "ArmUpperJet", sprite, light, overlay);

        ps.translate(1.5D, 3.75D, 0D);
        ps.mulPose(Axis.ZP.rotationDegrees((float) a[2]));
        ps.translate(-1.5D, -3.75D, 0D);
        draw(vc, ps, obj, "Jet", sprite, light, overlay);

        if (be.didProcess && be.armJet.angles[2] == be.armJet.prevAngles[2] && be.armJet.angles[2] != 0) {
            renderJetFlame(be, ps, buffer);
        }

        ps.popPose();
    }

    private static void renderJetFlame(FusionPlasmaForgeBlockEntity be, PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        var matrix = ps.last().pose();

        double outerLen = 1 + FusionPlasmaForgeBlockEntity.RAND.nextDouble() * 0.125;
        double narrow = 0.01;
        double side = 0.125;
        double near = 1.375;
        double far = 1.625;

        float r = Mth.clamp(be.plasmaRed, 0F, 1F);
        float g = Mth.clamp(be.plasmaGreen, 0F, 1F);
        float b = Mth.clamp(be.plasmaBlue, 0F, 1F);

        flameQuad(vc, matrix, r, g, b, near, 3, side, far, 3, side,
                far - narrow, 3 - outerLen, side - narrow, near + narrow, 3 - outerLen, side - narrow);
        flameQuad(vc, matrix, r, g, b, near, 3, -side, far, 3, -side,
                far - narrow, 3 - outerLen, -side + narrow, near + narrow, 3 - outerLen, -side + narrow);
        flameQuad(vc, matrix, r, g, b, near, 3, side, near, 3, -side,
                near + narrow, 3 - outerLen, -side + narrow, near + narrow, 3 - outerLen, side - narrow);
        flameQuad(vc, matrix, r, g, b, far, 3, side, far, 3, -side,
                far - narrow, 3 - outerLen, -side + narrow, far - narrow, 3 - outerLen, side - narrow);
    }

    private static void flameQuad(VertexConsumer vc, org.joml.Matrix4f m, float r, float g, float b,
                                  double x0, double y0, double z0, double x1, double y1, double z1,
                                  double x2, double y2, double z2, double x3, double y3, double z3) {
        int ir = (int) (r * 255F), ig = (int) (g * 255F), ib = (int) (b * 255F);
        RenderHooks.vertexColor(vc, m, (float) x0, (float) y0, (float) z0, ir, ig, ib, 255);
        RenderHooks.vertexColor(vc, m, (float) x1, (float) y1, (float) z1, ir, ig, ib, 255);
        RenderHooks.vertexColor(vc, m, (float) x2, (float) y2, (float) z2, ir, ig, ib, 0);
        RenderHooks.vertexColor(vc, m, (float) x3, (float) y3, (float) z3, ir, ig, ib, 0);
    }

    private static void draw(VertexConsumer vc, PoseStack ps, Map<String, List<float[]>> obj,
                             String group, TextureAtlasSprite sprite, int light, int overlay) {
        RBMKColumnRenderer.renderObjGroup(vc, ps.last().pose(), obj.get(group), sprite, 1F, 1F, 1F, light, overlay);
    }

    static void drawScrolledGroup(VertexConsumer vc, PoseStack pose, List<float[]> triangles,
                                  double du, double dv, float r, float g, float b, float a, int light) {
        float cr = Mth.clamp(r, 0F, 1F);
        float cg = Mth.clamp(g, 0F, 1F);
        float cb = Mth.clamp(b, 0F, 1F);
        float ca = Mth.clamp(a, 0F, 1F);

        for (float[] tri : triangles) {
            for (int i = 0; i < 3; i++) {
                int base = i * 8;
                float u = (float) (tri[base + 3] + du);
                float v = (float) (tri[base + 4] + dv);
                RenderHooks.vertexFull(vc, pose.last(), tri[base], tri[base + 1], tri[base + 2],
                        (int) (cr * 255F), (int) (cg * 255F), (int) (cb * 255F), (int) (ca * 255F),
                        u, 1F - v,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, light,
                        tri[base + 5], tri[base + 6], tri[base + 7]);
            }
        }
    }

    public static void applyFacing(BlockState state, PoseStack pose) {
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

    private MachineFusionComplexRenderer() {}
}
