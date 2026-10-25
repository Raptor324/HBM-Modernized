package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCombinationOvenBlockEntity;
import com.hbm_m.blockentity.machines.MachineFurnaceIronBlockEntity;
import com.hbm_m.blockentity.machines.MachineFurnaceSteelBlockEntity;
import com.hbm_m.blockentity.machines.MachineRotaryFurnaceBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Modern Nucleus machine renderers for industrial metallurgical furnaces:
 * - Iron Furnace: Animated door / firebox state ("On" fullbright vs "Off")
 * - Steel Furnace: Incandescent molten steel interior glow hook
 * - Rotary Furnace: Continuous rotary drum rotation and reciprocating pushrod piston
 * - Combination Oven: Heating chamber with animated flame billboard hook
 */
public final class MachineFurnaceRenderer {

    private static final ResourceLocation FIRE_TEX =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/rbmk_fire.png");

    public static void register() {
        registerIronFurnace();
        registerSteelFurnace();
        registerRotaryFurnace();
        registerCombinationOven();
    }

    private static void registerIronFurnace() {
        MachineRenderers.machine("furnace_iron", ModBlockEntities.FURNACE_IRON_BE.get(),
                MachineFurnaceIronBlockEntity.class)
            .part("Main")
            .conditionalPart("On", be -> be.wasOn)
            .conditionalPart("Off", be -> !be.wasOn)
            .lightOverride("On", be -> LightTexture.FULL_BRIGHT)
            .blockTransform(MachineFurnaceRenderer::applyIronFurnaceTransform)
            .register();
    }

    private static void registerSteelFurnace() {
        MachineRenderers.machine("furnace_steel", ModBlockEntities.FURNACE_STEEL_BE.get(),
                MachineFurnaceSteelBlockEntity.class)
            .part("Body")
            .blockTransform(MachineFurnaceRenderer::applySteelFurnaceTransform)
            .hook(new MachineRenderHook<MachineFurnaceSteelBlockEntity>() {
                @Override
                public void render(MachineFurnaceSteelBlockEntity furnace, float partialTick, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    if (!furnace.wasOn) return;

                    float col = (float) Math.sin(System.currentTimeMillis() * 0.001);
                    float r = 0.875F + col * 0.125F;
                    float g = 0.625F + col * 0.375F;
                    float b = 0.0F;
                    float a = 0.5F;

                    VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.DETONATOR_LASER_GLOW);
                    Matrix4f m = ps.last().pose();
                    for (int i = 0; i < 4; i++) {
                        float x = 1.0F + i * 0.0625F;
                        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, x, 1.0F, -1.0F, r, g, b, a);
                        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, x, 1.0F, 1.0F, r, g, b, a);
                        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, x, 0.5F, 1.0F, r, g, b, a);
                        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, x, 0.5F, -1.0F, r, g, b, a);
                    }
                }

                @Override
                public boolean mayDrawContent(MachineFurnaceSteelBlockEntity furnace) {
                    return furnace.wasOn;
                }
            })
            .register();
    }

    private static void registerRotaryFurnace() {
        MachineRenderers.machine("rotary_furnace", ModBlockEntities.ROTARY_FURNACE_BE.get(),
                MachineRotaryFurnaceBlockEntity.class)
            .part("Furnace")
            .translatingPart("Piston", new Vector3f(0f, 1f, 0f), (be, pt) -> {
                float anim = Mth.lerp(pt, be.lastAnim, be.anim);
                return (float) (sps((anim * 0.75) * 0.125) * 0.5 - 0.5);
            })
            .blockTransform(MachineFurnaceRenderer::applyRotaryFurnaceTransform)
            .register();
    }

    private static void registerCombinationOven() {
        MachineRenderers.machine("combination_oven", ModBlockEntities.COMBINATION_OVEN_BE.get(),
                MachineCombinationOvenBlockEntity.class)
            .part("Body")
            .blockTransform((be, animator) -> animator.translate(0.5, 0.0, 0.5))
            .hook(new MachineRenderHook<MachineCombinationOvenBlockEntity>() {
                @Override
                public void render(MachineCombinationOvenBlockEntity te, float partialTick, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    if (!te.wasOn || te.getLevel() == null) return;

                    ps.pushPose();
                    int texIndex = (int) (te.getLevel().getGameTime() / 2 % 14);
                    float f0 = 1.0F / 14.0F;
                    float uMin = (texIndex % 5) * f0;
                    float uMax = uMin + f0;
                    float vMin = 0.0F;
                    float vMax = 1.0F;

                    ps.translate(0.0, 1.75, 0.0);
                    ps.mulPose(Axis.YP.rotationDegrees(-Minecraft.getInstance().gameRenderer.getMainCamera().getYRot()));

                    float scaleH = 1.0F;
                    float scaleV = 3.0F;

                    VertexConsumer vc = buf.getBuffer(RenderType.eyes(FIRE_TEX));
                    Matrix4f m = ps.last().pose();
                    Matrix3f n = ps.last().normal();

                    renderFireQuad(vc, m, n, -scaleH, scaleH, 0, scaleV, uMin, uMax, vMin, vMax);
                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(MachineCombinationOvenBlockEntity te) {
                    return te.wasOn;
                }
            })
            .register();
    }

    private static void applyIronFurnaceTransform(MachineFurnaceIronBlockEntity furnace, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        switch (ObjBerHelper.meta(furnace)) {
            case 3 -> animator.rotate(0, 0, 1, 0);
            case 5 -> animator.rotate(90, 0, 1, 0);
            case 2 -> animator.rotate(180, 0, 1, 0);
            case 4 -> animator.rotate(270, 0, 1, 0);
            default -> {}
        }
        animator.translate(-0.5, 0.0, -0.5);
    }

    private static void applySteelFurnaceTransform(MachineFurnaceSteelBlockEntity furnace, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        if (furnace.getBlockState().hasProperty(DummyableMachineBlock.FACING)) {
            switch (furnace.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                case EAST -> animator.rotate(90, 0, 1, 0);
                case NORTH -> animator.rotate(180, 0, 1, 0);
                case WEST -> animator.rotate(270, 0, 1, 0);
                default -> {}
            }
        }
        animator.rotate(-90, 0, 1, 0);
    }

    private static void applyRotaryFurnaceTransform(MachineRotaryFurnaceBlockEntity furnace, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        if (furnace.getBlockState().hasProperty(DummyableMachineBlock.FACING)) {
            switch (furnace.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                case NORTH -> animator.rotate(90, 0, 1, 0);
                case WEST -> animator.rotate(180, 0, 1, 0);
                case SOUTH -> animator.rotate(270, 0, 1, 0);
                default -> {}
            }
        }
    }

    private static void renderFireQuad(VertexConsumer vc, Matrix4f m, Matrix3f n,
                                       float xMin, float xMax, float yMin, float yMax,
                                       float uMin, float uMax, float vMin, float vMax) {
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, xMin, yMin, 0, 255, 255, 255, 255, uMax, vMax, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0F, 1F, 0F);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, xMin, yMax, 0, 255, 255, 255, 255, uMax, vMin, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0F, 1F, 0F);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, xMax, yMax, 0, 255, 255, 255, 255, uMin, vMin, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0F, 1F, 0F);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, xMax, yMin, 0, 255, 255, 255, 255, uMin, vMax, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0F, 1F, 0F);

        // Reverse side for double-sided visibility
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, xMax, yMin, 0, 255, 255, 255, 255, uMin, vMax, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0F, 1F, 0F);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, xMax, yMax, 0, 255, 255, 255, 255, uMin, vMin, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0F, 1F, 0F);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, xMin, yMax, 0, 255, 255, 255, 255, uMax, vMin, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0F, 1F, 0F);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, xMin, yMin, 0, 255, 255, 255, 255, uMax, vMax, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0F, 1F, 0F);
    }

    private static double sps(double x) {
        return Math.sin(Math.PI / 2.0 * Math.cos(x));
    }

    private MachineFurnaceRenderer() {}
}
