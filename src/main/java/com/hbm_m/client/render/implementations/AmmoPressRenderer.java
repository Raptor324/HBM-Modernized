package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineAmmoPressBlockEntity;
import com.hbm_m.blockentity.machines.MachineAmmoPressBlockEntity.AnimationState;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderAmmoPress}: Gestell, Pressstempel und die Huelsenplatte, auf der nach dem Pressen die Geschosse sitzen. */
public class AmmoPressRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineAmmoPressBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/ammo_press.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/ammo_press.png");

    public AmmoPressRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineAmmoPressBlockEntity tile, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 2/4/3/5 -> 90/180/270/0 Grad
        switch (tile.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        float press = tile.prevPress + (tile.press - tile.prevPress) * f;
        float lift = tile.prevLift + (tile.lift - tile.prevLift) * f;

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        MODEL.renderPart("Frame", ps, vc, light);

        ps.pushPose();
        ps.translate(0, -press * 0.25F, 0);
        MODEL.renderPart("Press", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, lift * 0.5F - 0.5F, 0);
        MODEL.renderPart("Shells", ps, vc, light);
        if (tile.animState == AnimationState.RETRACTING || tile.animState == AnimationState.LOWERING) MODEL.renderPart("Bullets", ps, vc, light);
        ps.popPose();

        ps.popPose();
    }

    @Override public int getViewDistance() { return 256; }
}
