package com.hbm_m.client.render.entity;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.item.EntityParachuteCrate;
import com.hbm_m.entity.logic.EntityC130;
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

/** 1:1 {@code RenderC130} und {@code RenderParachuteCrate}. */
public final class C130Renderers {

    private C130Renderers() { }

    private static ResourceLocation rl(String p) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p);
    }

    public static final SimpleObjModel C130 = new SimpleObjModel(rl("models/weapons/c130.obj"));
    public static final ResourceLocation C130_TEX = rl("textures/models/weapons/c130_0.png");
    public static final SimpleObjModel CONSERVECRATE = new SimpleObjModel(rl("models/blocks/conservecrate.obj"));
    public static final SimpleObjModel SOYUZ_LANDER = new SimpleObjModel(rl("models/soyuz_lander.obj"));
    public static final ResourceLocation SUPPLY_CRATE_TEX = rl("textures/block/crate_can.png");
    public static final ResourceLocation CHUTE_TEX = rl("textures/models/soyuz_capsule/soyuz_chute.png");

    public static class C130Renderer extends EntityRenderer<EntityC130> {

        public C130Renderer(EntityRendererProvider.Context ctx) { super(ctx); }

        @Override
        public void render(@NotNull EntityC130 entity, float yaw, float interp, PoseStack ps, MultiBufferSource buffers, int light) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(interp, entity.yRotO, entity.getYRot()) - 90.0F));
            ps.mulPose(Axis.ZP.rotationDegrees(90));
            ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(interp, entity.xRotO, entity.getXRot())));

            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(C130_TEX));
            C130.renderPart("Plane", ps, vc, light);

            double spin = System.currentTimeMillis() * 15D % 360D;

            prop(ps, vc, light, -20.5, spin, "Prop1");
            prop(ps, vc, light, -11.16, spin, "Prop2");
            prop(ps, vc, light, 11.16, spin, "Prop3");
            prop(ps, vc, light, 20.5, spin, "Prop4");

            ps.popPose();
            super.render(entity, yaw, interp, ps, buffers, light);
        }

        private static void prop(PoseStack ps, VertexConsumer vc, int light, double z, double spin, String part) {
            ps.pushPose();
            ps.translate(10, 4.2, z);
            ps.mulPose(Axis.XP.rotationDegrees((float) spin));
            ps.translate(-10, -4.2, -z);
            C130.renderPart(part, ps, vc, light);
            ps.popPose();
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull EntityC130 entity) { return C130_TEX; }
    }

    public static class ParachuteCrateRenderer extends EntityRenderer<EntityParachuteCrate> {

        public ParachuteCrateRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

        @Override
        public void render(@NotNull EntityParachuteCrate entity, float yaw, float interp, PoseStack ps, MultiBufferSource buffers, int light) {
            ps.pushPose();

            double time = entity.level().getGameTime();
            double sine = Math.sin(time * 0.05) * 5;
            double sin3 = Math.sin(time * 0.05 + Math.PI * 0.5) * 5;

            int height = 7;

            ps.translate(0.0F, height, 0.0F);
            ps.mulPose(Axis.ZP.rotationDegrees((float) sine));
            ps.mulPose(Axis.XP.rotationDegrees((float) sin3));
            ps.translate(0.0F, -height, 0.0F);

            CONSERVECRATE.renderAll(ps, buffers.getBuffer(RenderType.entityCutout(SUPPLY_CRATE_TEX)), light);

            ps.translate(0, -1, 0);

            SOYUZ_LANDER.renderPart("Chute", ps, buffers.getBuffer(RenderType.entityCutout(CHUTE_TEX)), light);

            ps.popPose();
            super.render(entity, yaw, interp, ps, buffers, light);
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull EntityParachuteCrate entity) { return CHUTE_TEX; }
    }
}
