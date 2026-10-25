package com.hbm_m.client.render.projectile;

import java.util.Random;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.projectile.EntityBurningFOEQ;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 {@code RenderFOEQ}: brennender Satellit 75 Bloecke unter der Entity, darueber zehn additive Flammenstapel
 * (vier Farbstufen, je Bild zufaellig gedreht, aufsummierte Verschiebungen wie im Original).
 */
public class BurningFOEQRenderer extends EntityRenderer<EntityBurningFOEQ> {

    private static final SimpleObjModel SAT = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/projectiles/sat_foeq_burning.obj"));
    private static final SimpleObjModel FIRE = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/projectiles/sat_foeq_fire.obj"));
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/sat_foeq_burning.png");

    public BurningFOEQRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(@NotNull EntityBurningFOEQ e, float yaw, float f1, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        ps.translate(0, -75, 0);
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(f1, e.yRotO, e.getYRot()) - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(f1, e.xRotO, e.getXRot())));

        SAT.renderAll(ps, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light);

        VertexConsumer vc = buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.ADDITIVE_TRIANGLES);
        Random rand = new Random(System.currentTimeMillis() / 50);

        ps.scale(1.15F, 0.75F, 1.15F);
        ps.translate(0, -0.5, 0.3);
        for (int i = 0; i < 10; i++) {
            ps.mulPose(Axis.YP.rotationDegrees(rand.nextInt(360)));
            FIRE.renderAllColor(ps, vc, 1F, 0.75F, 0.25F, 1F);
            ps.translate(0, 2, 0);
            ps.mulPose(Axis.YP.rotationDegrees(rand.nextInt(360)));
            FIRE.renderAllColor(ps, vc, 1F, 0.5F, 0F, 1F);
            ps.translate(0, 2, 0);
            ps.mulPose(Axis.YP.rotationDegrees(rand.nextInt(360)));
            FIRE.renderAllColor(ps, vc, 1F, 0.25F, 0F, 1F);
            ps.translate(0, 2, 0);
            ps.mulPose(Axis.YP.rotationDegrees(rand.nextInt(360)));
            FIRE.renderAllColor(ps, vc, 1F, 0.15F, 0F, 1F);

            ps.translate(0, -3.8, 0);
            ps.scale(0.95F, 1.2F, 0.95F);
        }

        ps.popPose();
        super.render(e, yaw, f1, ps, buffers, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityBurningFOEQ entity) {
        return TEXTURE;
    }
}
