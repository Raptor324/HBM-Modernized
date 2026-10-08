package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.LaunchPadLargeBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.missile.MissileRenderData;
import com.hbm_m.client.render.missile.MissileRenderRegistry;
import com.hbm_m.item.missile.MissileItem;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code RenderLaunchPadLarge}: Rampe ({@code Pad}), je Bauform der Rakete Sockel, Seil, Drehgelenk und Aufrichter
 * (Aufrichter dreht um {@code -erector} um die X-Achse am Gelenk, Hubtisch faehrt um {@code lift} hoch), darauf die
 * Rakete zwei Bloecke ueber dem Kern - stehend, sobald {@code erected}, sonst mit dem Aufrichter.
 */
public class LaunchPadLargeRenderer implements com.hbm_m.client.render.HbmBerBounds<LaunchPadLargeBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/weapons/launch_pad_erector.obj"));
    private static final ResourceLocation PAD = tex("pad");
    private static final ResourceLocation MICRO = tex("erector_micro");
    private static final ResourceLocation V2 = tex("erector_v2");
    private static final ResourceLocation STRONG = tex("erector_strong");
    private static final ResourceLocation HUGE = tex("erector_huge");
    private static final ResourceLocation ATLAS = tex("erector_atlas");
    private static final ResourceLocation ABM = tex("erector_abm");

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/launchpad/" + name + ".png");
    }

    public LaunchPadLargeRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(LaunchPadLargeBlockEntity pad, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        switch (ObjBerHelper.meta(pad)) {
            case 2 -> ObjBerHelper.rotY(ps, 90);
            case 4 -> ObjBerHelper.rotY(ps, 180);
            case 3 -> ObjBerHelper.rotY(ps, 270);
            case 5 -> ObjBerHelper.rotY(ps, 0);
        }

        MODEL.renderPart("Pad", ps, buf.getBuffer(RenderType.entityCutout(PAD)), light);

        MissileItem.MissileFormFactor[] forms = MissileItem.MissileFormFactor.values();
        if (pad.formFactor >= 0 && pad.formFactor < forms.length) {

            String[] parts;
            double[] offset;
            ResourceLocation tex;

            switch (forms[pad.formFactor]) {
                case MICRO -> { parts = new String[] {"Micro_Pad", "Micro_Erector", "Micro_Pivot", "Micro_Rope"}; offset = new double[] {1.5D, 1.25D}; tex = MICRO; }
                case V2 -> { parts = new String[] {"V2_Pad", "V2_Erector", "V2_Pivot", "V2_Rope"}; offset = new double[] {1.75D, 1.25D}; tex = V2; }
                case STRONG -> { parts = new String[] {"Strong_Pad", "Strong_Erector", "Strong_Pivot", "Strong_Rope"}; offset = new double[] {3D, 1.5D}; tex = STRONG; }
                case HUGE -> { parts = new String[] {"Huge_Pad", "Huge_Erector", "Huge_Pivot", "Huge_Rope"}; offset = new double[] {3D, 1.5D}; tex = HUGE; }
                case ATLAS -> { parts = new String[] {"Atlas_Pad", "Atlas_Erector", "Atlas_Pivot", "Atlas_Rope"}; offset = new double[] {4D, 1.5D}; tex = ATLAS; }
                // ABM und OTHER (und die Port-Bauformen ohne eigenen Aufrichter)
                default -> { parts = new String[] {"ABM_Pad", "ABM_Erector", "ABM_Pivot", "ABM_Rope"}; offset = new double[] {1.5D, 1.25D}; tex = ABM; }
            }

            float erectorAngle = pad.prevErector + (pad.erector - pad.prevErector) * f;
            float erectorLift = pad.prevLift + (pad.lift - pad.prevLift) * f;
            ItemStack toRender = pad.getMissilePreviewStack();
            boolean hasMissile = !toRender.isEmpty();

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(tex));
            ps.pushPose();
            MODEL.renderPart(parts[0], ps, vc, light);
            if (hasMissile && pad.erected) MODEL.renderPart(parts[3], ps, vc, light);
            ps.translate(0, offset[1], -offset[0]);
            ps.mulPose(Axis.XP.rotationDegrees(-erectorAngle));
            ps.translate(0, -offset[1], offset[0]);
            MODEL.renderPart(parts[2], ps, vc, light);
            ps.translate(0, erectorLift, 0);
            MODEL.renderPart(parts[1], ps, vc, light);

            if (pad.erected) {
                ps.popPose();
                ps.pushPose();
            }

            if (hasMissile && (pad.erected || pad.readyToLoad)) {
                ps.translate(0, 2, 0);
                MissileRenderData renderData = MissileRenderRegistry.get(toRender);
                if (renderData != null) renderData.render(ps, light, pad.getBlockPos(), buf, pad);
            }
            ps.popPose();
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(LaunchPadLargeBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
