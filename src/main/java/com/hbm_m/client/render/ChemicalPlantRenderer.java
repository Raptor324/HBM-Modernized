package com.hbm_m.client.render;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.block.entity.custom.machines.MachineChemicalPlantBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

public class ChemicalPlantRenderer implements BlockEntityRenderer<MachineChemicalPlantBlockEntity> {

    public ChemicalPlantRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MachineChemicalPlantBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // Placeholder fluid overlay.
        FluidStack fluid = blockEntity.getFluid();
        if (!fluid.isEmpty()) {
            renderFluid(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay, fluid);
        }

        // Placeholder spinner marker (helps verify animation is ticking)
        poseStack.pushPose();
        poseStack.translate(0.5, 0.75, 0.5);
        poseStack.mulPose(Axis.YP.rotation(blockEntity.getAnim(partialTick)));
        poseStack.translate(-0.5, -0.75, -0.5);
        poseStack.popPose();
    }

    private static void renderFluid(MachineChemicalPlantBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, FluidStack fluid) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation stillTexture = ext.getStillTexture(fluid);
        if (stillTexture == null) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        int tintColor = ext.getTintColor(fluid);

        float red = ((tintColor >> 16) & 0xFF) / 255.0F;
        float green = ((tintColor >> 8) & 0xFF) / 255.0F;
        float blue = (tintColor & 0xFF) / 255.0F;
        float alpha = 0.85F;

        float fill = blockEntity.getFluidFillFraction();
        if (fill <= 0.001F) {
            return;
        }

        // Inset to avoid z-fighting with the block model.
        float x0 = 0.20F;
        float z0 = 0.20F;
        float x1 = 0.80F;
        float z1 = 0.80F;

        float y0 = 0.10F;
        float y1 = y0 + 0.60F * fill;

        long time = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;
        float t = (time + partialTick) * 0.02F;
        float scroll = t - (float) Math.floor(t);

        // Map the sprite once; we'll scroll UVs by offsetting within [u0,u1].
        float uMin = sprite.getU0();
        float uMax = sprite.getU1();
        float vMin = sprite.getV0();
        float vMax = sprite.getV1();

        float du = (uMax - uMin) * scroll;
        float dv = (vMax - vMin) * scroll;

        VertexConsumer vc = buffer.getBuffer(RenderType.translucent());

        poseStack.pushPose();
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMat = pose.pose();
        Matrix3f normalMat = pose.normal();

        // Top face
        addVertex(vc, poseMat, normalMat, x0, y1, z0, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, 0, 1, 0);
        addVertex(vc, poseMat, normalMat, x0, y1, z1, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, 0, 1, 0);
        addVertex(vc, poseMat, normalMat, x1, y1, z1, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, 0, 1, 0);
        addVertex(vc, poseMat, normalMat, x1, y1, z0, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, 0, 1, 0);

        // Four sides
        // North (-Z)
        addVertex(vc, poseMat, normalMat, x0, y0, z0, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, 0, 0, -1);
        addVertex(vc, poseMat, normalMat, x1, y0, z0, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, 0, 0, -1);
        addVertex(vc, poseMat, normalMat, x1, y1, z0, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, 0, 0, -1);
        addVertex(vc, poseMat, normalMat, x0, y1, z0, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, 0, 0, -1);

        // South (+Z)
        addVertex(vc, poseMat, normalMat, x0, y0, z1, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, 0, 0, 1);
        addVertex(vc, poseMat, normalMat, x0, y1, z1, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, 0, 0, 1);
        addVertex(vc, poseMat, normalMat, x1, y1, z1, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, 0, 0, 1);
        addVertex(vc, poseMat, normalMat, x1, y0, z1, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, 0, 0, 1);

        // West (-X)
        addVertex(vc, poseMat, normalMat, x0, y0, z0, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, -1, 0, 0);
        addVertex(vc, poseMat, normalMat, x0, y1, z0, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, -1, 0, 0);
        addVertex(vc, poseMat, normalMat, x0, y1, z1, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, -1, 0, 0);
        addVertex(vc, poseMat, normalMat, x0, y0, z1, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, -1, 0, 0);

        // East (+X)
        addVertex(vc, poseMat, normalMat, x1, y0, z0, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, 1, 0, 0);
        addVertex(vc, poseMat, normalMat, x1, y0, z1, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, 1, 0, 0);
        addVertex(vc, poseMat, normalMat, x1, y1, z1, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, 1, 0, 0);
        addVertex(vc, poseMat, normalMat, x1, y1, z0, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, 1, 0, 0);

        poseStack.popPose();
    }

    private static void addVertex(VertexConsumer vc, Matrix4f poseMat, Matrix3f normalMat,
                                  float x, float y, float z,
                                  float r, float g, float b, float a,
                                  float u, float v,
                                  int overlay, int light,
                                  float nx, float ny, float nz) {
        vc.vertex(poseMat, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMat, nx, ny, nz)
                .endVertex();
    }
}
