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
 * <p>Der Entfuehrungsstrahl wird wie im Original mit {@code BeamPronter} bis zum ersten Block darunter gezeichnet.</p>
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
            // Original glRotatef(tilt, 1, 0, 1): eine Drehung um die Diagonale
            float inv = (float) (1D / Math.sqrt(2D));
            ps.mulPose(new org.joml.Quaternionf().rotationAxis((float) Math.toRadians(tilt), inv, 0F, inv));
        }

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) ((ufo.tickCount + partialTick) * 5 % 360D)));
        ps.scale((float) SCALE, (float) SCALE, (float) SCALE);

        for (List<float[]> mesh : obj.values()) {
            RBMKColumnRenderer.renderObjGroup(buffer.getBuffer(RenderType.entityCutoutNoCull(
                            InventoryMenu.BLOCK_ATLAS)), ps.last().pose(),
                    mesh, sprite, 1F, 1F, 1F, light, OverlayTexture.NO_OVERLAY);
        }
        ps.popPose();

        if (ufo.getBeam()) {
            int ix = (int) Math.floor(ufo.getX());
            int iz = (int) Math.floor(ufo.getZ());
            int iy = 0;

            for (int i = (int) Math.ceil(ufo.getY()); i >= ufo.level().getMinBuildHeight(); i--) {
                if (!ufo.level().getBlockState(new net.minecraft.core.BlockPos(ix, i, iz)).isAir()) {
                    iy = i;
                    break;
                }
            }

            double length = ufo.getY() - iy;

            if (length > 0) {
                net.minecraft.world.phys.Vec3 down = new net.minecraft.world.phys.Vec3(0, -length, 0);
                com.hbm_m.client.render.util.BeamPronter.prontBeam(ps, buffer, down, com.hbm_m.client.render.util.BeamPronter.EnumWaveType.SPIRAL,
                        com.hbm_m.client.render.util.BeamPronter.EnumBeamType.SOLID, 0x101020, 0x101020, 0, (int) (length + 1), 0F, 6, (float) SCALE * 0.75F);
                com.hbm_m.client.render.util.BeamPronter.prontBeam(ps, buffer, down, com.hbm_m.client.render.util.BeamPronter.EnumWaveType.RANDOM,
                        com.hbm_m.client.render.util.BeamPronter.EnumBeamType.SOLID, 0x202060, 0x202060, ufo.tickCount / 2, (int) (length / 2 + 1), (float) SCALE * 1.5F, 2, 0.0625F);
                com.hbm_m.client.render.util.BeamPronter.prontBeam(ps, buffer, down, com.hbm_m.client.render.util.BeamPronter.EnumWaveType.RANDOM,
                        com.hbm_m.client.render.util.BeamPronter.EnumBeamType.SOLID, 0x202060, 0x202060, ufo.tickCount / 4, (int) (length / 2 + 1), (float) SCALE * 1.5F, 2, 0.0625F);
            }
        }

        ps.popPose();
        super.render(ufo, yaw, partialTick, ps, buffer, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityUFO ufo) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
