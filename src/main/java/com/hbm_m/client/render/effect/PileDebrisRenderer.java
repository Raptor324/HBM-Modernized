package com.hbm_m.client.render.effect;

import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.hbm_m.entity.projectile.PileDebrisEntity;
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

import java.util.List;
import java.util.Map;

/**
 * 1:1-Port von {@code LegoClient.RENDER_GRAPHITE}: der Graphitbrocken des Uranmeilers benutzt
 * dieselbe Form wie der RBMK-Trueummerbrocken, nur doppelt so gross ({@code glScalef(2F, 2F, 2F)}).
 */
public class PileDebrisRenderer extends EntityRenderer<PileDebrisEntity> {

    private static final String MODEL = "models/rbmk/models/deb_graphite.obj";
    private static final String TEXTURE = "block/block_graphite";

    public PileDebrisRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(PileDebrisEntity entity, float yaw, float pt, PoseStack ps,
                       MultiBufferSource buf, int light) {

        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(MODEL);
        if (obj.isEmpty()) return;

        List<float[]> mesh = obj.values().iterator().next();
        TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, TEXTURE);

        ps.pushPose();
        ps.scale(2F, 2F, 2F);

        // Wie beim RBMK-Schutt: ein fester Versatz je Entitaet, damit nicht alle gleich taumeln.
        ps.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));
        float spin = Mth.lerp(pt, entity.lastRot, entity.rot);
        ps.mulPose(Axis.XP.rotationDegrees(spin));
        ps.mulPose(Axis.YP.rotationDegrees(spin));
        ps.mulPose(Axis.ZP.rotationDegrees(spin));

        RBMKColumnRenderer.renderObjGroup(buf.getBuffer(RenderType.solid()), ps.last().pose(),
                mesh, sprite, 1F, 1F, 1F, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(PileDebrisEntity entity) {
        return net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
    }
}
