package com.hbm_m.client.render.item;

import com.hbm_m.client.render.legacy.ModelB92;
import com.hbm_m.item.weapon.GunB92Item;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ItemRenderGunAnim} fuer die B92: Inventar = flaches Icon (Original ohne INVENTORY-Zweig),
 * Hand/Boden = {@link ModelB92} mit den Original-GL-Schritten. Die 1.7.10-Bezugsraeume wie im {@code GunItemRenderer}.
 */
public class GunB92ItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static GunB92ItemRenderer instance;
    public static final ResourceLocation ICON_MODEL = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "item/gun_b92_icon");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/ModelB92SM.png");

    private ModelB92 model;

    public static GunB92ItemRenderer instance() {
        if (instance == null) instance = new GunB92ItemRenderer();
        return instance;
    }

    private GunB92ItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (model == null) model = new ModelB92();

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5); // ItemRenderer verschiebt vor dem BEWLR um -0.5

        switch (ctx) {
            case GUI -> {
                BakedModel icon = com.hbm_m.platform.PlatformHooks.getModel(Minecraft.getInstance().getModelManager(), ICON_MODEL);
                Minecraft.getInstance().getItemRenderer().render(stack, ctx, false, ps, buf, light, overlay, icon);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                ps.mulPose(Axis.YP.rotationDegrees(-90));
                // EQUIPPED_FIRST_PERSON
                ps.mulPose(Axis.ZP.rotationDegrees(-135.0F));
                ps.translate(-0.5F, 0.0F, -0.2F);
                ps.scale(0.5F, 0.5F, 0.5F);
                ps.scale(0.5F, 0.5F, 0.5F);
                ps.translate(-0.2F, -0.1F, -0.1F);
                float rot = GunB92Item.getRotationFromAnim(stack);
                if (rot > 0) {
                    float off = rot * 2;
                    ps.mulPose(Axis.ZP.rotationDegrees(rot * -90)); // glRotatef(rot * -90, 0, 0, 1)
                    ps.translate(off * -0.5F, off * -0.5F, 0.0F);
                }
                model.renderAnim(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light, OverlayTexture.NO_OVERLAY, GunB92Item.getTransFromAnim(stack));
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                boolean left = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                ps.translate((left ? 1 : -1) / 16F, -0.125F, 0.625F);
                ps.mulPose(Axis.YP.rotationDegrees(-180));
                ps.mulPose(Axis.XP.rotationDegrees(90));
                ps.translate(-0.0625F, 0.4375F, 0.0625F);
                ps.translate(0.25F, 0.1875F, -0.1875F);
                ps.scale(0.375F, 0.375F, 0.375F);
                ps.mulPose(Axis.ZP.rotationDegrees(60));
                ps.mulPose(Axis.XP.rotationDegrees(-90));
                ps.mulPose(Axis.ZP.rotationDegrees(20));
                renderEquipped(stack, ps, buf, light);
            }
            default -> {
                ps.scale(0.5F, 0.5F, 0.5F);
                renderEquipped(stack, ps, buf, light);
            }
        }
        ps.popPose();
    }

    /** Zweig EQUIPPED / ENTITY des Originals. */
    private void renderEquipped(ItemStack stack, PoseStack ps, MultiBufferSource buf, int light) {
        ps.mulPose(Axis.ZP.rotationDegrees(-200.0F));
        ps.mulPose(Axis.YP.rotationDegrees(75.0F));
        ps.mulPose(Axis.XP.rotationDegrees(-30.0F));
        ps.translate(0.0F, -0.2F, -0.5F);
        ps.mulPose(Axis.ZP.rotationDegrees(-5.0F));
        ps.scale(0.5F, 0.5F, 0.5F);
        ps.translate(-0.3F, -0.4F, 0.15F);
        model.renderAnim(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light, OverlayTexture.NO_OVERLAY, GunB92Item.getTransFromAnim(stack));
    }
}
