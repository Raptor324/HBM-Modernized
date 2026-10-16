package com.hbm_m.client.render.rbmk;

import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.hbm_m.entity.rbmk.RBMKDebrisEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Map;

/**
 * 1:1 port of {@code RenderRBMKDebris}. Each debris type draws its own group from
 * {@code debris.obj} with the matching column texture, spun around by the entity's tumble angle
 * plus a fixed per-entity offset derived from its id, so a shower of debris doesn't rotate in
 * lockstep - exactly the trick the original uses.
 */
public class RBMKDebrisRenderer extends EntityRenderer<RBMKDebrisEntity> {

    /** The original keeps one mesh per debris type under {@code models/projectiles/}. */
    private static final String MODEL_DIR = "models/rbmk/models/";

    public RBMKDebrisRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(RBMKDebrisEntity entity, float yaw, float pt, PoseStack ps,
                       MultiBufferSource buf, int light) {
        // Model file and skin per debris type, mirroring the original's switch.
        String model;
        String texture;
        switch (entity.getDebrisType()) {
            case ELEMENT  -> { model = "deb_element";  texture = "block/rbmk/rbmk_element_side"; }
            case FUEL     -> { model = "deb_fuel";     texture = "block/rbmk/rbmk_element_inner"; }
            case GRAPHITE -> { model = "deb_graphite"; texture = "block/block_graphite"; }
            case LID      -> { model = "deb_lid";      texture = "block/rbmk/rbmk_blank_cover_top"; }
            case ROD      -> { model = "deb_rod";      texture = "block/rbmk/rbmk_control"; }
            default       -> { model = "deb_blank";    texture = "block/rbmk/rbmk_blank_side"; }
        }

        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(MODEL_DIR + model + ".obj");
        if (obj.isEmpty()) return;

        // Each of these files is a single-group export, so whatever group it has is the mesh.
        List<float[]> mesh = obj.values().iterator().next();
        TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, texture);

        ps.pushPose();
        ps.translate(0, 0.125, 0);
        ps.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));
        float spin = Mth.lerp(pt, entity.lastRot, entity.rot);
        ps.mulPose(Axis.XP.rotationDegrees(spin));
        ps.mulPose(Axis.YP.rotationDegrees(spin));
        ps.mulPose(Axis.ZP.rotationDegrees(spin));

        RBMKColumnRenderer.renderObjGroup(buf.getBuffer(RenderType.solid()), ps.last().pose(),
                mesh, sprite, 1f, 1f, 1f, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(RBMKDebrisEntity entity) {
        return net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
    }
}
