package com.hbm_m.client.render.effect;

import com.hbm_m.entity.projectile.CogEntity;
import com.hbm_m.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Das fliegende Zahnrad: es zeigt schlicht den Gegenstand, den man danach aufheben kann - im Flug
 * trudelnd, liegend flach auf dem Boden.
 *
 * <p>Das Original hat dafuer ein eigenes Modell; dieser Port nimmt die Gegenstandsgrafik, damit
 * sofort erkennbar ist, was da herumliegt.</p>
 */
public class CogRenderer extends EntityRenderer<CogEntity> {

    private final ItemStack gear = new ItemStack(ModItems.GEAR_LARGE.get());

    public CogRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(CogEntity entity, float yaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int light) {

        ps.pushPose();
        ps.scale(1.5F, 1.5F, 1.5F);

        if (entity.isResting()) {
            // Ausgerollt: es liegt flach.
            ps.mulPose(Axis.XP.rotationDegrees(90F));
        } else {
            // Im Flug trudelt es um die eigene Achse.
            float spin = (entity.tickCount + partialTick) * 20F;
            ps.mulPose(Axis.YP.rotationDegrees(spin));
            ps.mulPose(Axis.ZP.rotationDegrees(spin * 0.5F));
        }

        Minecraft.getInstance().getItemRenderer().renderStatic(
                gear, net.minecraft.world.item.ItemDisplayContext.FIXED, light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                ps, buffer, entity.level(), entity.getId());

        ps.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(CogEntity entity) {
        return net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
    }
}
