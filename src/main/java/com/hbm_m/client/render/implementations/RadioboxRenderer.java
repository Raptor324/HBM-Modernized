package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.RadioboxBlock;
import com.hbm_m.blockentity.machines.RadioboxBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderDecoBlock} fuer die Radiobox mit {@code ModelRadio} (32x32): Kasten, Frontplatte und der Hebel,
 * der aus (20 Grad) oder an (160 Grad) steht.
 */
public class RadioboxRenderer implements BlockEntityRenderer<RadioboxBlockEntity> {

    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/model_radio.png");

    private final ModelPart box;
    private final ModelPart plate;
    private final ModelPart lever;

    public RadioboxRenderer(BlockEntityRendererProvider.Context ctx) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("box", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0F, 0F, 0F, 8, 14, 4), PartPose.offset(-4F, 9F, -12F));
        root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(0F, 0F, 0F, 7, 13, 1), PartPose.offset(-3.5F, 9.5F, -12.5F));
        root.addOrReplaceChild("lever", CubeListBuilder.create().texOffs(16, 18).mirror().addBox(0F, -1F, -1F, 2, 8, 2), PartPose.offset(4F, 16F, -10F));
        ModelPart model = LayerDefinition.create(mesh, 32, 32).bakeRoot();
        this.box = model.getChild("box");
        this.plate = model.getChild("plate");
        this.lever = model.getChild("lever");
    }

    @Override
    public void render(RadioboxBlockEntity te, float partialTick, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5F, 1.5F, 0.5F);
        ps.mulPose(Axis.ZP.rotationDegrees(180));

        switch (te.getBlockState().getValue(RadioboxBlock.FACING)) {
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            default -> { }
        }
        ps.translate(0, 0, 1);

        int rotation = te.getBlockState().getValue(RadioboxBlock.ON) ? 160 : 20;
        lever.xRot = -(float) (rotation / 180F * Math.PI);

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        box.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        plate.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        lever.render(ps, vc, light, OverlayTexture.NO_OVERLAY);

        ps.popPose();
    }
}
