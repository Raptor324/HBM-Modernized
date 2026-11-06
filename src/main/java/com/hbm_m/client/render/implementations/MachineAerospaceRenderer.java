package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.LaunchPadLargeBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.missile.MissileRenderData;
import com.hbm_m.client.render.missile.MissileRenderRegistry;
import com.hbm_m.item.missile.MissileItem;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Nucleus renderers for heavy aerospace & rocketry installations:
 * 1. Large Launch Pad: structural launch platform, modular form factor erector
 *    gantries (Micro, V2, Strong, Huge, Atlas, ABM), dynamic hydraulic angle tilt
 *    and lift elevator, crane cables, and live payload missile preview stack hook.
 */
public final class MachineAerospaceRenderer {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/weapons/launch_pad_erector.obj"));
    private static final ResourceLocation PAD_TEX = tex("pad");
    private static final ResourceLocation MICRO_TEX = tex("erector_micro");
    private static final ResourceLocation V2_TEX = tex("erector_v2");
    private static final ResourceLocation STRONG_TEX = tex("erector_strong");
    private static final ResourceLocation HUGE_TEX = tex("erector_huge");
    private static final ResourceLocation ATLAS_TEX = tex("erector_atlas");
    private static final ResourceLocation ABM_TEX = tex("erector_abm");

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/launchpad/" + name + ".png");
    }

    public static void register() {
        MachineRenderers.machine("launch_pad_large",
                ModBlockEntities.LAUNCH_PAD_LARGE_BE.get(),
                LaunchPadLargeBlockEntity.class)
            .part("Pad")
            .hook(new MachineRenderHook<LaunchPadLargeBlockEntity>() {
                @Override
                public void render(LaunchPadLargeBlockEntity pad, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    MissileItem.MissileFormFactor[] forms = MissileItem.MissileFormFactor.values();
                    if (pad.formFactor < 0 || pad.formFactor >= forms.length) return;

                    String[] parts;
                    double[] offset;
                    ResourceLocation tex;

                    switch (forms[pad.formFactor]) {
                        case MICRO -> {
                            parts = new String[] {"Micro_Pad", "Micro_Erector", "Micro_Pivot", "Micro_Rope"};
                            offset = new double[] {1.5D, 1.25D};
                            tex = MICRO_TEX;
                        }
                        case V2 -> {
                            parts = new String[] {"V2_Pad", "V2_Erector", "V2_Pivot", "V2_Rope"};
                            offset = new double[] {1.75D, 1.25D};
                            tex = V2_TEX;
                        }
                        case STRONG -> {
                            parts = new String[] {"Strong_Pad", "Strong_Erector", "Strong_Pivot", "Strong_Rope"};
                            offset = new double[] {3.0D, 1.5D};
                            tex = STRONG_TEX;
                        }
                        case HUGE -> {
                            parts = new String[] {"Huge_Pad", "Huge_Erector", "Huge_Pivot", "Huge_Rope"};
                            offset = new double[] {3.0D, 1.5D};
                            tex = HUGE_TEX;
                        }
                        case ATLAS -> {
                            parts = new String[] {"Atlas_Pad", "Atlas_Erector", "Atlas_Pivot", "Atlas_Rope"};
                            offset = new double[] {4.0D, 1.5D};
                            tex = ATLAS_TEX;
                        }
                        default -> {
                            parts = new String[] {"ABM_Pad", "ABM_Erector", "ABM_Pivot", "ABM_Rope"};
                            offset = new double[] {1.5D, 1.25D};
                            tex = ABM_TEX;
                        }
                    }

                    float erectorAngle = Mth.lerp(pt, pad.prevErector, pad.erector);
                    float erectorLift = Mth.lerp(pt, pad.prevLift, pad.lift);
                    ItemStack toRender = pad.getMissilePreviewStack();
                    boolean hasMissile = !toRender.isEmpty();

                    VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(tex));

                    ps.pushPose();
                    MODEL.renderPart(parts[0], ps, vc, light);
                    if (hasMissile && pad.erected) {
                        MODEL.renderPart(parts[3], ps, vc, light);
                    }

                    // Hydraulic arm pivot rotation
                    ps.translate(0, offset[1], -offset[0]);
                    ps.mulPose(Axis.XP.rotationDegrees(-erectorAngle));
                    ps.translate(0, -offset[1], offset[0]);
                    MODEL.renderPart(parts[2], ps, vc, light);

                    // Vertical elevator lift
                    ps.translate(0, erectorLift, 0);
                    MODEL.renderPart(parts[1], ps, vc, light);

                    if (pad.erected) {
                        ps.popPose();
                        ps.pushPose();
                    }

                    // Missile payload preview stack
                    if (hasMissile && (pad.erected || pad.readyToLoad)) {
                        ps.translate(0, 2, 0);
                        MissileRenderData renderData = MissileRenderRegistry.get(toRender);
                        if (renderData != null) {
                            renderData.render(ps, light, pad.getBlockPos(), buf, pad);
                        }
                    }

                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(LaunchPadLargeBlockEntity pad) {
                    return true;
                }
            })
            .itemParts("Pad")
            .register();
    }

    private MachineAerospaceRenderer() {}
}
