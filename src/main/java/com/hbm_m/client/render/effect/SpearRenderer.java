package com.hbm_m.client.render.effect;

import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.hbm_m.entity.effect.SpearEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.List;
import java.util.Map;

/**
 * 1:1 port of {@code RenderSpear}. The lance is drawn from the {@code Spear} group of
 * {@code lance.obj}, offset fifteen blocks up from the entity's own position and flipped, so the
 * point sits at the entity while the shaft towers into the sky.
 *
 * <p>Once it plants itself the original overlays a second, untextured pass that fades in over a
 * hundred ticks - that white-out is what sells the discharge building up.</p>
 */
public class SpearRenderer extends EntityRenderer<SpearEntity> {

    private static final String MODEL = "models/weapons/lance.obj";

    public SpearRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(SpearEntity entity, float yaw, float pt, PoseStack ps,
                       MultiBufferSource buf, int light) {
        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(MODEL);
        List<float[]> mesh = obj.get("Spear");
        if (mesh == null) return;

        TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/lance");

        ps.pushPose();
        ps.translate(0, 15, 0);
        ps.mulPose(Axis.XP.rotationDegrees(180));
        ps.scale(2F, 2F, 2F);

        RBMKColumnRenderer.renderObjGroup(buf.getBuffer(RenderType.solid()), ps.last().pose(),
                mesh, sprite, 1F, 1F, 1F, light, OverlayTexture.NO_OVERLAY);

        // The charge-up overlay: the same mesh again, pure white and fully lit, its opacity
        // tracking how long the lance has been standing.
        int ticks = entity.getTicksInGround();
        if (ticks > 0) {
            float occupancy = Math.min((ticks + pt) / 100F, 1F);
            RBMKColumnRenderer.renderObjGroup(
                    buf.getBuffer(RenderType.entityTranslucentEmissive(InventoryMenu.BLOCK_ATLAS)),
                    ps.last().pose(), mesh, sprite,
                    1F, 1F, 1F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, occupancy);
        }

        ps.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SpearEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
