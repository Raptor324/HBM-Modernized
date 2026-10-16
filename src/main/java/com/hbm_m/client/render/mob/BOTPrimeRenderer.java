package com.hbm_m.client.render.mob;

import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.hbm_m.entity.mob.botprime.EntityWormBase;
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
 * 1:1 port of {@code RenderWormHead}, generalised to also draw the body segments - the two differ
 * only in which mesh and skin they use, and the original has a near-identical second class for it.
 *
 * <p>Both are drawn along the segment's own facing, yawed back 90 degrees and pitched back 90, the
 * offsets the model is authored with. Culling is off because the worm is routinely seen from
 * inside itself.</p>
 */
public class BOTPrimeRenderer<T extends EntityWormBase> extends EntityRenderer<T> {

    private final String model;
    private final String texture;

    private BOTPrimeRenderer(EntityRendererProvider.Context context, String model, String texture) {
        super(context);
        this.model = model;
        this.texture = texture;
        this.shadowRadius = 0F;
    }

    public static BOTPrimeRenderer<com.hbm_m.entity.mob.botprime.EntityBOTPrimeHead> head(
            EntityRendererProvider.Context context) {
        return new BOTPrimeRenderer<>(context, "models/mobs/bot_prime_head.obj", "entity_obj/mark_zero_head");
    }

    public static BOTPrimeRenderer<com.hbm_m.entity.mob.botprime.EntityBOTPrimeBody> body(
            EntityRendererProvider.Context context) {
        return new BOTPrimeRenderer<>(context, "models/mobs/bot_prime_body.obj", "entity_obj/mark_zero_body");
    }

    @Override
    public void render(@NotNull T worm, float yaw, float partialTick,
                       @NotNull PoseStack ps, @NotNull MultiBufferSource buffer, int light) {
        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(this.model);
        if (obj.isEmpty()) return;

        TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, this.texture);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(
                Mth.lerp(partialTick, worm.yRotO, worm.getYRot()) - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(
                Mth.lerp(partialTick, worm.xRotO, worm.getXRot()) - 90.0F));

        // renderAll(): every group in the file, not a selected part.
        for (List<float[]> mesh : obj.values()) {
            RBMKColumnRenderer.renderObjGroup(buffer.getBuffer(RenderType.entityCutoutNoCull(
                            InventoryMenu.BLOCK_ATLAS)), ps.last().pose(),
                    mesh, sprite, 1F, 1F, 1F, light, OverlayTexture.NO_OVERLAY);
        }

        ps.popPose();
        super.render(worm, yaw, partialTick, ps, buffer, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T worm) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
