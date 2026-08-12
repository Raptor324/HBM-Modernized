package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineFoundryChannelBlockEntity;
import com.hbm_m.block.machines.MachineFoundryChannelBlock;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Matrix4f;

/**
 * Renders the molten metal flowing through a foundry channel:
 * a center quad plus one quad per connected direction.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class FoundryChannelRenderer implements BlockEntityRenderer<MachineFoundryChannelBlockEntity> {

    private static final ResourceLocation LAVA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/fluids/lava.png");

    // inner trough: x/z 6..10 px, floor top at 2 px, walls up to 8 px
    private static final float IN_MIN = 6f / 16f;
    private static final float IN_MAX = 10f / 16f;
    private static final float FLOOR  = 2f / 16f;
    private static final float MAX_LEVEL = 6f / 16f;

    public FoundryChannelRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(MachineFoundryChannelBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (be.type == null || be.amount <= 0) return;

        float fill = Math.min(1f, (float) be.amount / MachineFoundryChannelBlockEntity.CAPACITY);
        float surfaceY = FLOOR + fill * (MAX_LEVEL - FLOOR);

        int color = 0xFF000000 | be.type.color;
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(LAVA_TEXTURE));
        Matrix4f m = poseStack.last().pose();

        BlockState state = be.getBlockState();
        boolean north = state.hasProperty(MachineFoundryChannelBlock.NORTH) && state.getValue(MachineFoundryChannelBlock.NORTH);
        boolean south = state.hasProperty(MachineFoundryChannelBlock.SOUTH) && state.getValue(MachineFoundryChannelBlock.SOUTH);
        boolean east  = state.hasProperty(MachineFoundryChannelBlock.EAST)  && state.getValue(MachineFoundryChannelBlock.EAST);
        boolean west  = state.hasProperty(MachineFoundryChannelBlock.WEST)  && state.getValue(MachineFoundryChannelBlock.WEST);

        // center
        quad(vc, m, IN_MIN, IN_MAX, IN_MIN, IN_MAX, surfaceY, r, g, b, a);
        // arms
        if (north) quad(vc, m, IN_MIN, IN_MAX, 0f,     IN_MIN, surfaceY, r, g, b, a);
        if (south) quad(vc, m, IN_MIN, IN_MAX, IN_MAX, 1f,     surfaceY, r, g, b, a);
        if (west)  quad(vc, m, 0f,     IN_MIN, IN_MIN, IN_MAX, surfaceY, r, g, b, a);
        if (east)  quad(vc, m, IN_MAX, 1f,     IN_MIN, IN_MAX, surfaceY, r, g, b, a);
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x0, float x1, float z0, float z1, float y,
                             float r, float g, float b, float a) {
        int fullbright = 0xF000F0;
        int rI = (int)(r * 255);
        int gI = (int)(g * 255);
        int bI = (int)(b * 255);
        int aI = (int)(a * 255);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x0, y, z0, rI, gI, bI, aI, 0, 0, OverlayTexture.NO_OVERLAY, fullbright, 0, 1, 0);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x0, y, z1, rI, gI, bI, aI, 0, 1, OverlayTexture.NO_OVERLAY, fullbright, 0, 1, 0);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x1, y, z1, rI, gI, bI, aI, 1, 1, OverlayTexture.NO_OVERLAY, fullbright, 0, 1, 0);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x1, y, z0, rI, gI, bI, aI, 1, 0, OverlayTexture.NO_OVERLAY, fullbright, 0, 1, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(MachineFoundryChannelBlockEntity blockEntity) {
        return ShaderCompatibilityDetector.shouldRenderBlockEntityOffScreen();
    }
}
