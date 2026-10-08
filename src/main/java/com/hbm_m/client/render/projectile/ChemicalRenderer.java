package com.hbm_m.client.render.projectile;

import java.awt.Color;
import java.util.Random;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.entity.projectile.EntityChemical;
import com.hbm_m.entity.projectile.EntityChemical.ChemicalStyle;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code RenderChemical}: Antimaterie/Blitz als verblassender Strahl entlang der Flugrichtung, Gas als wachsende
 * Wolke in Fluidfarbe, brennendes Gas als Feuerkugel (Farbe von Gelb nach Rot). Fluessigkeiten nur als Partikel.
 */
public class ChemicalRenderer extends EntityRenderer<EntityChemical> {

    private static final ResourceLocation gas = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/particle_base.png");

    public ChemicalRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0F;
    }

    @Override
    public void render(EntityChemical chem, float yaw, float f1, PoseStack ps, MultiBufferSource buf, int light) {

        ps.pushPose();

        ChemicalStyle style = chem.getStyle();

        if (style == ChemicalStyle.AMAT || style == ChemicalStyle.LIGHTNING)
            renderAmatBeam(chem, f1, ps, buf);

        if (style == ChemicalStyle.GAS) {
            renderGasCloud(chem, f1, ps, buf);
        }

        if (style == ChemicalStyle.GASFLAME) {
            renderGasFire(chem, f1, ps, buf);
        }

        ps.popPose();
    }

    private void billboard(PoseStack ps) {
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - this.entityRenderDispatcher.camera.getYRot()));
        ps.mulPose(Axis.XP.rotationDegrees(-this.entityRenderDispatcher.camera.getXRot()));
    }

    private void quad(VertexConsumer vc, PoseStack ps, double size, int rgb, int alpha, float u0, float v0, float u1, float v1) {
        var m = ps.last().pose();
        var n = ps.last().normal();
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        int full = 0xF000F0;
        vc.vertex(m, (float) -size, (float) -size, 0).color(r, g, b, alpha).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, (float) size, (float) -size, 0).color(r, g, b, alpha).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, (float) size, (float) size, 0).color(r, g, b, alpha).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, (float) -size, (float) size, 0).color(r, g, b, alpha).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
    }

    private void renderGasFire(EntityChemical chem, float interp, PoseStack ps, MultiBufferSource buf) {

        float exp = (float) (chem.tickCount + interp) / (float) chem.getMaxAge();
        double size = 0.0 + exp * 2;
        Color color = Color.getHSBColor(Math.max((60 - exp * 100) / 360F, 0.0F), 1 - exp * 0.25F, 1 - exp * 0.5F);

        billboard(ps);
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(gas));
        quad(vc, ps, size, color.getRGB() & 0xFFFFFF, (int) Math.max(255 * (1 - exp), 0), 0, 0, 1, 1);
    }

    private void renderGasCloud(EntityChemical chem, float interp, PoseStack ps, MultiBufferSource buf) {

        double exp = (double) (chem.tickCount + interp) / (double) chem.getMaxAge();
        double size = 0.0 + exp * 10;
        int color = chem.getFluidType().getColor();

        billboard(ps);

        Random rand = new Random(chem.getId());
        int i = rand.nextInt(2);
        int j = rand.nextInt(2);

        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(gas));
        quad(vc, ps, size, color, (int) Math.max(127 * (1 - exp), 0), i, j, 1 - i, 1 - j);
    }

    private void renderAmatBeam(EntityChemical chem, float interp, PoseStack ps, MultiBufferSource buf) {

        float yaw = chem.yRotO + (chem.getYRot() - chem.yRotO) * interp;
        float pitch = chem.xRotO + (chem.getXRot() - chem.xRotO) * interp;
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.mulPose(Axis.XP.rotationDegrees(-pitch - 90));

        Vec3 motion = chem.getDeltaMovement();
        float length = (float) (motion.length() * (chem.tickCount + interp) * 0.75);
        float size = 0.0625F;
        float o = 0.2F;

        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        var m = ps.last().pose();

        // je Seite zwei Kanten bei y = 0 (Alpha o) und y = length (Alpha 0)
        vc.vertex(m, -size, 0, -size).color(1F, 1F, 1F, o).endVertex();
        vc.vertex(m, size, 0, -size).color(1F, 1F, 1F, o).endVertex();
        vc.vertex(m, size, length, -size).color(1F, 1F, 1F, 0F).endVertex();
        vc.vertex(m, -size, length, -size).color(1F, 1F, 1F, 0F).endVertex();

        vc.vertex(m, -size, 0, size).color(1F, 1F, 1F, o).endVertex();
        vc.vertex(m, size, 0, size).color(1F, 1F, 1F, o).endVertex();
        vc.vertex(m, size, length, size).color(1F, 1F, 1F, 0F).endVertex();
        vc.vertex(m, -size, length, size).color(1F, 1F, 1F, 0F).endVertex();

        vc.vertex(m, -size, 0, -size).color(1F, 1F, 1F, o).endVertex();
        vc.vertex(m, -size, 0, size).color(1F, 1F, 1F, o).endVertex();
        vc.vertex(m, -size, length, size).color(1F, 1F, 1F, 0F).endVertex();
        vc.vertex(m, -size, length, -size).color(1F, 1F, 1F, 0F).endVertex();

        vc.vertex(m, size, 0, -size).color(1F, 1F, 1F, o).endVertex();
        vc.vertex(m, size, 0, size).color(1F, 1F, 1F, o).endVertex();
        vc.vertex(m, size, length, size).color(1F, 1F, 1F, 0F).endVertex();
        vc.vertex(m, size, length, -size).color(1F, 1F, 1F, 0F).endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(EntityChemical entity) {
        return gas;
    }
}
