package com.hbm_m.client.render.plane;

import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.hbm_m.entity.logic.EntityBomber;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * 1:1 port of {@code RenderBomber}. Two airframes with four skins each: the small Dornier for the
 * conventional loadouts, the B-29 for the atomic one. Both rock gently around their roll axis on a
 * slow sine, which is the only animation the original gives them.
 */
public class BomberRenderer extends EntityRenderer<EntityBomber> {

    private static final String DORNIER = "models/planes/dornier.obj";
    private static final String B29     = "models/planes/b29.obj";

    public BomberRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public void render(@NotNull EntityBomber bomber, float yaw, float partialTick,
                       @NotNull PoseStack ps, @NotNull MultiBufferSource buffer, int light) {
        int type = bomber.getPlaneType();
        boolean heavy = type >= 5;

        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(heavy ? B29 : DORNIER);
        if (obj.isEmpty()) return;

        TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID,
                "entity_obj/" + skinFor(type));

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(
                Mth.lerp(partialTick, bomber.yRotO, bomber.getYRot()) - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(90F));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, bomber.xRotO, bomber.getXRot())));
        // The slow roll that makes it look like it is actually flying rather than sliding.
        ps.mulPose(Axis.XP.rotationDegrees(
                (float) Math.sin((bomber.tickCount + partialTick) * 0.05) * 10F));

        for (List<float[]> mesh : obj.values()) {
            RBMKColumnRenderer.renderObjGroup(buffer.getBuffer(RenderType.entityCutoutNoCull(
                            InventoryMenu.BLOCK_ATLAS)), ps.last().pose(),
                    mesh, sprite, 1F, 1F, 1F, light, OverlayTexture.NO_OVERLAY);
        }

        ps.popPose();
        super.render(bomber, yaw, partialTick, ps, buffer, light);
    }

    private static String skinFor(int type) {
        return switch (type) {
            case 2 -> "dornier_2";
            case 4 -> "dornier_4";
            case 5 -> "b29_0";
            case 6 -> "b29_1";
            case 7 -> "b29_2";
            case 8 -> "b29_3";
            default -> "dornier_1";
        };
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityBomber bomber) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
