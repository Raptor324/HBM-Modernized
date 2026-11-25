package com.hbm_m.client.render.effect;

import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.entity.effect.EntityFreakyUfo;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Renderer for {@link EntityFreakyUfo}: spinning UFO saucer with a green-cyan laser beam into the ground.
 */
public class FreakyUfoRenderer extends EntityRenderer<EntityFreakyUfo> {

    private static final String MODEL = "models/mobs/ufo.obj";
    private static final double SCALE = 2.0D;

    public FreakyUfoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public void render(@NotNull EntityFreakyUfo ufo, float yaw, float partialTick,
                       @NotNull PoseStack ps, @NotNull MultiBufferSource buffer, int light) {
        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(MODEL);
        if (obj.isEmpty()) return;

        TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "entity_obj/ufo");

        ps.pushPose();
        ps.translate(0, 1, 0);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) ((ufo.tickCount + partialTick) * 5 % 360D)));
        ps.scale((float) SCALE, (float) SCALE, (float) SCALE);

        for (List<float[]> mesh : obj.values()) {
            RBMKColumnRenderer.renderObjGroup(
                    buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)),
                    ps.last().pose(), mesh, sprite, 1F, 1F, 1F, light, OverlayTexture.NO_OVERLAY);
        }
        ps.popPose();

        if (ufo.getBeam()) {
            int ix = Mth.floor(ufo.getX());
            int iz = Mth.floor(ufo.getZ());
            int iy = ufo.level().getMinBuildHeight();

            for (int i = Mth.ceil(ufo.getY()); i >= ufo.level().getMinBuildHeight(); i--) {
                if (!ufo.level().getBlockState(new BlockPos(ix, i, iz)).isAir()) {
                    iy = i;
                    break;
                }
            }

            double length = ufo.getY() - iy;
            if (length > 0) {
                Vec3 down = new Vec3(0, -length, 0);
                BeamPronter.prontBeam(ps, buffer, down, BeamPronter.EnumWaveType.SPIRAL,
                        BeamPronter.EnumBeamType.SOLID, 0x10B020, 0x80FF90, 0, (int) (length + 1), 0F, 6, (float) SCALE * 0.55F);
                BeamPronter.prontBeam(ps, buffer, down, BeamPronter.EnumWaveType.RANDOM,
                        BeamPronter.EnumBeamType.SOLID, 0x20FF60, 0xCCFFDD, ufo.tickCount / 2, (int) (length / 2 + 1), (float) SCALE * 1.1F, 2, 0.08F);
            }
        }

        ps.popPose();
        super.render(ufo, yaw, partialTick, ps, buffer, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityFreakyUfo ufo) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
