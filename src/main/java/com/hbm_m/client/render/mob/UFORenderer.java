package com.hbm_m.client.render.mob;

import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.hbm_m.entity.mob.EntityUFO;
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
import net.minecraft.world.inventory.InventoryMenu;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * 1:1 port of {@code RenderUFO}. The disc spins constantly at five degrees a tick and tips over
 * once it is dead, which is the only animation it has.
 *
 * <p>The original also draws the abduction beam as a translucent column down to the ground using
 * its {@code BeamPronter} helper; that helper is not ported, so the beam is currently invisible
 * even though its effect is live. {@link EntityUFO#getBeam()} is synced and ready for it.</p>
 */
public class UFORenderer extends EntityRenderer<EntityUFO> {

    private static final String MODEL = "models/mobs/ufo.obj";
    private static final double SCALE = 2D;

    public UFORenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public void render(@NotNull EntityUFO ufo, float yaw, float partialTick,
                       @NotNull PoseStack ps, @NotNull MultiBufferSource buffer, int light) {
        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(MODEL);
        if (obj.isEmpty()) return;

        TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "entity_obj/ufo");

        ps.pushPose();
        ps.translate(0, 1, 0);

        if (!ufo.isAlive()) {
            // deathTime starts at -30, so the tilt only becomes visible once it has been falling.
            float tilt = ufo.deathTime + 30 + partialTick;
            ps.mulPose(Axis.XP.rotationDegrees(tilt));
            ps.mulPose(Axis.ZP.rotationDegrees(tilt));
        }

        ps.mulPose(Axis.YP.rotationDegrees((float) ((ufo.tickCount + partialTick) * 5 % 360D)));
        ps.scale((float) SCALE, (float) SCALE, (float) SCALE);

        for (List<float[]> mesh : obj.values()) {
            RBMKColumnRenderer.renderObjGroup(buffer.getBuffer(RenderType.entityCutoutNoCull(
                            InventoryMenu.BLOCK_ATLAS)), ps.last().pose(),
                    mesh, sprite, 1F, 1F, 1F, light, OverlayTexture.NO_OVERLAY);
        }

        ps.popPose();
        super.render(ufo, yaw, partialTick, ps, buffer, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityUFO ufo) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
