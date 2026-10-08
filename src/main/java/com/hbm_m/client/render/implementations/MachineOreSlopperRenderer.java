package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineOreSlopperBlockEntity;
import com.hbm_m.blockentity.machines.MachineOreSlopperBlockEntity.SlopperAnimation;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code RenderOreSlopper}: Sockel, Schlitten (faehrt 3 Bloecke), Hydraulik und Schaufel (senken sich bis 1,25),
 * waehrend des Hebens ein Erzbrocken in der Schaufel, gegenlaeufige Schreddermesser und der Luefter.
 */
public class MachineOreSlopperRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineOreSlopperBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/ore_slopper.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/ore_slopper.png");

    public MachineOreSlopperRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineOreSlopperBlockEntity slopper, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 3/5/2/4 -> 180/270/0/90 Grad
        switch (slopper.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            default -> { }
        }

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Base", ps, vc, light);

        ps.pushPose();

        double slide = slopper.prevSlider + (slopper.slider - slopper.prevSlider) * interp;
        ps.translate(0, 0, slide * -3);
        MODEL.renderPart("Slider", ps, vc, light);

        ps.pushPose();
        double extend = (slopper.prevBucket + (slopper.bucket - slopper.prevBucket) * interp) * 1.5;
        ps.translate(0, -Mth.clamp(extend - 0.25, 0, 1.25), 0);
        MODEL.renderPart("Hydraulics", ps, vc, light);
        ps.translate(0, -Mth.clamp(extend, 0, 1.25), 0);
        MODEL.renderPart("Bucket", ps, vc, light);

        if (slopper.animation == SlopperAnimation.LIFTING) {
            ps.translate(0.0625D, 4.3125D, 2D);
            ps.mulPose(Axis.YP.rotationDegrees(90));
            ps.mulPose(Axis.XP.rotationDegrees(-90));
            ps.scale(1.75F, 1.75F, 1.75F);
            // Original: ItemStack(ModItems.bedrock_ore, 1, 0) = BedrockOreGrade.BASE, BedrockOreType.LIGHT
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ModItems.BEDROCK_ORE_BASE_LIGHT.get()),
                    ItemDisplayContext.FIXED, light, overlay, ps, buf, slopper.getLevel(), 0);
            vc = buf.getBuffer(RenderType.entityCutout(TEX));
        }

        ps.popPose();
        ps.popPose();

        float blades = slopper.prevBlades + (slopper.blades - slopper.prevBlades) * interp;

        ps.pushPose();
        ps.translate(0.375, 2.75, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(blades));
        ps.translate(-0.375, -2.75, 0);
        MODEL.renderPart("BladesLeft", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(-0.375, 2.75, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(-blades));
        ps.translate(0.375, -2.75, 0);
        MODEL.renderPart("BladesRight", ps, vc, light);
        ps.popPose();

        float fan = slopper.prevFan + (slopper.fan - slopper.prevFan) * interp;

        ps.pushPose();
        ps.translate(0, 1.875, -1);
        ps.mulPose(Axis.XP.rotationDegrees(-fan));
        ps.translate(0, -1.875, 1);
        MODEL.renderPart("Fan", ps, vc, light);
        ps.popPose();

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineOreSlopperBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
