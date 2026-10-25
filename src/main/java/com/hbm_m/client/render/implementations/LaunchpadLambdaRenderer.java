package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.LaunchpadLambdaBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.entity.LambdaRocketRenderer;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderLaunchpadLambda}: Silo mit zwei Schiebetueren, darunter der Aufrichter mit Rotor, vier
 * Klammerarmen und Kolben. Die {@code GL_CLIP_PLANE0}-Schnitte des Originals (Tueren bei |z| = 6, alles Bewegliche
 * unterhalb y = 0,25) uebernimmt {@link SimpleObjModel#renderPartClipped}.
 */
public class LaunchpadLambdaRenderer implements com.hbm_m.client.render.HbmBerBounds<LaunchpadLambdaBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/launchpad_lambda.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/launchpad_lambda.png");

    public LaunchpadLambdaRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(LaunchpadLambdaBlockEntity pad, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 2/4/3/5 -> 90/180/270/0 Grad
        float rotation = switch (pad.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case NORTH -> 90F;
            case WEST -> 180F;
            case SOUTH -> 270F;
            default -> 0F;
        };
        ps.mulPose(Axis.YP.rotationDegrees(rotation));

        float doors = pad.getInterpPos(LaunchpadLambdaBlockEntity.INDEX_DOORS, interp);
        float erector = pad.getInterpPos(LaunchpadLambdaBlockEntity.INDEX_ERECTOR, interp) - 25F;
        float rotor = pad.getInterpPos(LaunchpadLambdaBlockEntity.INDEX_ROTOR, interp);
        float clamps = pad.getInterpPos(LaunchpadLambdaBlockEntity.INDEX_CLAMPS, interp);
        float pistons = pad.getInterpPos(LaunchpadLambdaBlockEntity.INDEX_PISTONS, interp);

        boolean renderRocket = pad.erecting || pad.erected;
        boolean rocketFixed = pad.erected;

        if (rocketFixed) {
            ps.pushPose();
            ps.translate(0, 2, 0);
            ps.mulPose(Axis.YP.rotationDegrees(-rotation));
            LambdaRocketRenderer.MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(LambdaRocketRenderer.TEX)), light);
            ps.popPose();
        }

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Silo", ps, vc, light);

        MODEL.renderPartClipped("DoorLeft", ps, new Matrix4f().translate(0, 0, doors), vc, light, 0, 0, -1, 6);
        MODEL.renderPartClipped("DoorRight", ps, new Matrix4f().translate(0, 0, -doors), vc, light, 0, 0, 1, 6);

        // ab hier alles unterhalb y = 0,25 abschneiden
        Matrix4f base = new Matrix4f().translate(0, erector, 0);
        clipped("Erector", ps, base, vc, light);

        base.translate(0, 13.25F, 0).rotateX(rad(rotor)).translate(0, -13.25F, 0);
        clipped("Rotor", ps, base, vc, light);

        arm(ps, vc, light, base, 3.5F, 19.75F, -clamps, pistons, "PivotUpper1", "ClampUpper1");
        arm(ps, vc, light, base, 3.5F, 6.75F, clamps, pistons, "PivotLower1", "ClampLower1");
        arm(ps, vc, light, base, -3.5F, 19.75F, clamps, -pistons, "PivotUpper2", "ClampUpper2");
        arm(ps, vc, light, base, -3.5F, 6.75F, -clamps, -pistons, "PivotLower2", "ClampLower2");

        if (renderRocket && !rocketFixed) {
            Matrix4f rocket = new Matrix4f(base).translate(0, 2, 0).rotateY(rad(-rotation));
            VertexConsumer rvc = buf.getBuffer(RenderType.entityCutout(LambdaRocketRenderer.TEX));
            for (String part : LambdaRocketRenderer.MODEL.getPartNames()) {
                LambdaRocketRenderer.MODEL.renderPartClipped(part, ps, rocket, rvc, light, 0, 1, 0, -0.25F);
            }
        }

        ps.popPose();
    }

    /** Klammerarm: Drehpunkt (px, py) um Z gedreht, Klammer zusaetzlich um {@code piston} entlang X verschoben. */
    private static void arm(PoseStack ps, VertexConsumer vc, int light, Matrix4f base, float px, float py, float angle, float piston, String pivot, String clamp) {
        Matrix4f m = new Matrix4f(base).translate(px, py, 0).rotateZ(rad(angle)).translate(-px, -py, 0);
        clipped(pivot, ps, m, vc, light);
        m.translate(piston, 0, 0);
        clipped(clamp, ps, m, vc, light);
    }

    private static void clipped(String part, PoseStack ps, Matrix4f local, VertexConsumer vc, int light) {
        MODEL.renderPartClipped(part, ps, local, vc, light, 0, 1, 0, -0.25F);
    }

    private static float rad(float deg) {
        return (float) Math.toRadians(deg);
    }

    @Override public boolean shouldRenderOffScreen(LaunchpadLambdaBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
