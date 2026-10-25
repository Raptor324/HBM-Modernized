package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineHephaestusBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.UvShiftConsumer;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderHephaestus}: Gehaeuse, drei drehende Rotoren und der Kern - im Betrieb gluehende Lava (volle
 * Helligkeit), sonst abgedunkelter Bruchstein; die Textur laeuft mit der Rotation nach oben.
 */
public class HephaestusRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineHephaestusBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/hephaestus.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/hephaestus.png");
    public static final ResourceLocation LAVA = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/lava.png");
    public static final ResourceLocation COBBLE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/cobblestone.png");

    public HephaestusRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineHephaestusBlockEntity geo, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        MODEL.renderPart("Main", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);

        float movement = geo.prevRot + (geo.rot - geo.prevRot) * interp;
        boolean isOn = geo.bufferedHeat > 0;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(movement));
        for (int i = 0; i < 3; i++) {
            MODEL.renderPart("Rotor", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);
            ps.mulPose(Axis.YP.rotationDegrees(120));
        }
        ps.popPose();

        var core = new UvShiftConsumer(buf.getBuffer(RenderType.entityCutout(isOn ? LAVA : COBBLE)), 0F, movement / 10F, 0.5F, 0.5F);
        if (isOn) {
            MODEL.renderPart("Core", ps, core, 0xF000F0);
        } else {
            MODEL.renderPartTinted("Core", ps, core, light, 0.5F, 0.5F, 0.5F);
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineHephaestusBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
