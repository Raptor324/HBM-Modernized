package com.hbm_m.client.render.item;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.item.missile.ItemCustomMissilePart;
import com.hbm_m.item.missile.ItemCustomMissilePart.PartType;
import com.hbm_m.item.missile.MissilePartItems;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 1:1 {@code ItemRenderMissilePart}: warheads, bodies, fins and thrusters of modular missile render their
 * OBJ model. Inventory (ForgeHooksClient#renderInventoryItem without render helper, lighting disabled): height = guiheight (0 -> 4),
 * scale 10/height, T(h/2*s, 0, 0) Rz(135) Rx(145), warhead +h/8*s, body +h/4*s, T(3.5, 14, 0) S(-s), slow
 * rotation around -Y (milliseconds / 25). In hand T(0.5, 0, 0) S(0.4), on ground S(0.4).
 */
public class ItemRenderMissilePart extends BlockEntityWithoutLevelRenderer {

    private static ItemRenderMissilePart instance;

    public static ItemRenderMissilePart instance() {
        if (instance == null) instance = new ItemRenderMissilePart();
        return instance;
    }

    private static final Map<String, SimpleObjModel> MODELS = new HashMap<>();

    private ItemRenderMissilePart() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!(stack.getItem() instanceof ItemCustomMissilePart part)) return;
        MissilePartItems.RenderInfo info = MissilePartItems.render(part);
        if (info == null) return;
        SimpleObjModel model = MODELS.computeIfAbsent(info.model(), p -> new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p)));
        ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, info.texture());

        ps.pushPose();
        if (ctx == ItemDisplayContext.GUI) {
            // GUI pose from renderByItem (T(x+8,y+8) S(16,-16,16) T(-0.5)) -> top-left slot corner, Y downward
            ps.translate(0.5F, 0.5F, 0.5F);
            ps.scale(1F / 16F, -1F / 16F, 1F / 16F);
            ps.translate(-8F, -8F, 0F);
            double height = info.guiHeight();
            if (height == 0D) height = 4D;
            double size = 10;
            double scale = size / height;
            ps.translate(height / 2 * scale, 0, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(135));
            ps.mulPose(Axis.XP.rotationDegrees(145));
            if (info.type() == PartType.WARHEAD) ps.translate(0, height / 8 * scale, 0);
            if (info.type() == PartType.FUSELAGE) ps.translate(0, height / 4 * scale, 0);
            ps.translate(3.5, 14, 0);
            ps.scale((float) -scale, (float) -scale, (float) -scale);
            ps.mulPose(Axis.YN.rotationDegrees(System.currentTimeMillis() / 25 % 360));
            // Original: GL_LIGHTING disabled -> unlit fullbright (pure texture)
            model.renderAll(ps, buf.getBuffer(RenderType.text(tex)), 0xF000F0);
        } else {
            RBMKColumnItemRenderer.applyDisplay(stack, ctx, ps);
            ps.translate(0.5, 0, 0.5);
            if (ctx != ItemDisplayContext.GROUND && ctx != ItemDisplayContext.FIXED) ps.translate(0.5, 0, 0);
            ps.scale(0.4F, 0.4F, 0.4F);
            model.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(tex)), light);
        }
        ps.popPose();
    }
}
