package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.MachinePumpBlock;
import com.hbm_m.blockentity.machines.MachinePumpElectricBlockEntity;
import com.hbm_m.blockentity.machines.PumpBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code RenderPump}: Sockel, Schwungrad, Pleuel und Kolben der Grundwasserpumpe; die Textur unterscheidet Dampf
 * und Elektro.
 */
public class PumpRenderer<T extends BlockEntity> implements com.hbm_m.client.render.HbmBerBounds<T> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/pump.obj"));
    public static final ResourceLocation STEAM_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/pump_steam.png");
    public static final ResourceLocation ELECTRIC_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/pump_electric.png");

    public PumpRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(T tile, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 3/5/2/4 -> 90/180/270/0 Grad
        switch (tile.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        float rotor = 0, lastRotor = 0;
        if (tile instanceof PumpBlockEntity p) { rotor = p.rotor; lastRotor = p.lastRotor; }
        if (tile instanceof MachinePumpElectricBlockEntity p) { rotor = p.rotor; lastRotor = p.lastRotor; }
        float angle = lastRotor + (rotor - lastRotor) * interp;

        boolean electric = tile.getBlockState().getBlock() instanceof MachinePumpBlock pump && pump.isElectric();
        renderCommon(ps, buf.getBuffer(RenderType.entityCutoutNoCull(electric ? ELECTRIC_TEX : STEAM_TEX)), light, angle);

        ps.popPose();
    }

    public static void renderCommon(PoseStack ps, VertexConsumer vc, int light, double rot) {
        MODEL.renderPart("Base", ps, vc, light);

        ps.pushPose();
        ps.translate(0, 2.25, 0);
        ps.mulPose(Axis.ZP.rotationDegrees((float) (rot - 90)));
        ps.translate(0, -2.25, 0);
        MODEL.renderPart("Rotor", ps, vc, light);
        ps.popPose();

        double sin = Math.sin(rot * Math.PI / 180D) * 0.5D - 0.5D;
        double cos = Math.cos(rot * Math.PI / 180D) * 0.5D;
        double ang = Math.acos(cos / 2D);
        double cath = Math.sqrt(1 + (cos * cos) / 2);

        ps.pushPose();
        ps.translate(0, 1 - cath + sin, 0);
        ps.translate(0, 4.75, 0);
        ps.mulPose(Axis.ZN.rotationDegrees((float) (ang * 180D / Math.PI - 90D)));
        ps.translate(0, -4.75, 0);
        MODEL.renderPart("Arms", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 1 - cath + sin, 0);
        MODEL.renderPart("Piston", ps, vc, light);
        ps.popPose();
    }

    @Override public int getViewDistance() { return 256; }
}
