package com.hbm_m.client.render.mob;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.mob.siege.EntitySiegeTunneler;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 {@code RenderSiegeTunneler}: Bohrerkoerper ({@code tunneler.obj}) entlang der Bewegung ausgerichtet, der
 * Bohrkopf dreht sich nach Systemzeit. Kein Schatten.
 *
 * <p>Das Original sucht {@code siege_drill_<stufe>.png}, die es nicht gibt; im Port wird die vorhandene
 * {@code siege_drill.png} fuer alle Stufen genommen.</p>
 */
public class SiegeTunnelerRenderer extends EntityRenderer<EntitySiegeTunneler> {

    public static final SimpleObjModel BODY = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/mobs/tunneler.obj"));
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/siege_drill.png");

    public SiegeTunnelerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowStrength = 0.0F;
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull EntitySiegeTunneler entity, float yaw, float f1, @NotNull PoseStack ps, @NotNull MultiBufferSource buffers, int light) {

        ps.pushPose();
        // 1.7.10: yOffset 0,5 - die Position lag in der Mitte des Wesens
        ps.translate(0, 0.5, 0);

        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(f1, entity.yRotO, entity.getYRot()) - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(f1, entity.xRotO, entity.getXRot()) - 90));

        var vc = buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        BODY.renderPartEntity("Body", ps, vc, light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        ps.mulPose(Axis.YN.rotationDegrees(System.currentTimeMillis() / 3L % 360));
        BODY.renderPartEntity("Drill", ps, vc, light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);

        ps.popPose();
        super.render(entity, yaw, f1, ps, buffers, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntitySiegeTunneler entity) {
        return TEXTURE;
    }
}
