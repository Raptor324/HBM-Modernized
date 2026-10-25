package com.hbm_m.client.render.entity;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.train.EntityRailCarBase;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 1:1 {@code RenderTrainCargoTram}: Modell an der Renderposition (Mitte der Drehgestelle), gedreht nach Gier- und Nickwinkel. */
public class RenderTrainCargoTram<T extends EntityRailCarBase> extends EntityRenderer<T> {

    public static final SimpleObjModel train_cargo_tram = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/vehicles/tram.obj"));
    public static final ResourceLocation train_tram = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/trains/tram.png");

    public RenderTrainCargoTram(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(@NotNull T train, float swing, float interp, @NotNull PoseStack ps, @NotNull MultiBufferSource buffers, int light) {
        ps.pushPose();
        applyTrainTransform(train, interp, ps);
        // GL_CULL_FACE aus
        train_cargo_tram.renderAll(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(train_tram)), light);
        ps.popPose();
    }

    /** Gemeinsame Verschiebung/Drehung beider Tram-Renderer. */
    static void applyTrainTransform(EntityRailCarBase train, float interp, PoseStack ps) {
        double iX = Mth.lerp(interp, train.xo, train.getX());
        double iY = Mth.lerp(interp, train.yo, train.getY());
        double iZ = Mth.lerp(interp, train.zo, train.getZ());
        double rX = train.lastRenderX + (train.renderX - train.lastRenderX) * interp;
        double rY = train.lastRenderY + (train.renderY - train.lastRenderY) * interp;
        double rZ = train.lastRenderZ + (train.renderZ - train.lastRenderZ) * interp;
        ps.translate(-(iX - rX), -(iY - rY), -(iZ - rZ));

        float yaw = train.getYRot();
        float prevYaw = train.yRotO;

        if (yaw - prevYaw > 180) yaw -= 360;
        if (prevYaw - yaw > 180) prevYaw -= 360;

        float yawInterp = prevYaw + (yaw - prevYaw) * interp - 720;

        ps.mulPose(Axis.YP.rotationDegrees(-yawInterp));

        float pitch = train.getXRot();
        float prevPitch = train.xRotO;
        float pitchInterp = prevPitch + (pitch - prevPitch) * interp;
        ps.mulPose(Axis.XP.rotationDegrees(-pitchInterp));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T entity) {
        return train_tram;
    }
}
