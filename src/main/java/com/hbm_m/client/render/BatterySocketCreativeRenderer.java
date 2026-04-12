package com.hbm_m.client.render;

import java.util.Random;

import com.hbm_m.block.entity.machines.BatterySocketBlockEntity;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class BatterySocketCreativeRenderer implements BlockEntityRenderer<BatterySocketBlockEntity> {

    private static final ResourceLocation MOD_SKIN =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/battery_socket/creative_avatar.png");
    private static final ResourceLocation STEVE =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    private final PlayerModel<?> playerModel;

    public BatterySocketCreativeRenderer(BlockEntityRendererProvider.Context ctx) {
        this.playerModel = new PlayerModel<>(ctx.getModelSet().bakeLayer(ModelLayers.PLAYER), false);
    }

    @Override
    public void render(BatterySocketBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (be.getItemHandler().getStackInSlot(0).getItem() instanceof ItemCreativeBattery) {
            renderFigure(be.getLevel(), be.getBlockPos(), partialTicks, poseStack, buffer);
        }
    }

    private void renderFigure(Level level, BlockPos pos, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.15, 0.5);
        float spin = (level.getGameTime() + partialTicks) * 25f;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.scale(0.45f, 0.45f, 0.45f);
        poseStack.translate(0, 1.65, 0);

        ResourceLocation skin = MOD_SKIN;
        if (Minecraft.getInstance().getResourceManager().getResource(skin).isEmpty()) {
            skin = STEVE;
        }

        int light = LevelRenderer.getLightColor(level, pos.above());
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(skin));
        this.playerModel.young = false;
        this.playerModel.setAllVisible(true);
        this.playerModel.renderToBuffer(poseStack, vc, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);

        renderJaggedBolts(level, poseStack, buffer, pos);

        poseStack.popPose();
    }

    /** Jagged polylines toward corners (client-only decoration). */
    private static void renderJaggedBolts(Level level, PoseStack poseStack, MultiBufferSource buffer, BlockPos pos) {
        Random rand = new Random(level.getGameTime() / 5 + pos.asLong());
        VertexConsumer lines = buffer.getBuffer(RenderType.lines());
        float w = 0.4375f;
        for (int i = -1; i <= 1; i += 2) {
            for (int j = -1; j <= 1; j += 2) {
                if (rand.nextInt(4) != 0) continue;
                poseStack.pushPose();
                poseStack.translate(0, 0.75, 0);
                drawPolyline(poseStack, lines, w * i, 1.1875f, w * j, rand.nextInt(4096));
                poseStack.popPose();
            }
        }
    }

    private static void drawPolyline(PoseStack poseStack, VertexConsumer lines, float tx, float ty, float tz, int seed) {
        Random r = new Random(seed);
        float ox = 0f, oy = 0.5f, oz = 0f;
        int segments = 10;
        for (int s = 0; s < segments; s++) {
            float t = (s + 1) / (float) segments;
            float nx = tx * t + (r.nextFloat() - 0.5f) * 0.1f;
            float ny = oy + ty * t + (r.nextFloat() - 0.5f) * 0.08f;
            float nz = tz * t + (r.nextFloat() - 0.5f) * 0.1f;
            double minX = Math.min(ox, nx) - 0.012;
            double minY = Math.min(oy, ny) - 0.012;
            double minZ = Math.min(oz, nz) - 0.012;
            double maxX = Math.max(ox, nx) + 0.012;
            double maxY = Math.max(oy, ny) + 0.012;
            double maxZ = Math.max(oz, nz) + 0.012;
            LevelRenderer.renderLineBox(poseStack, lines, minX, minY, minZ, maxX, maxY, maxZ, 0.35f, 0.35f, 0.95f, 0.9f);
            ox = nx;
            oy = ny;
            oz = nz;
        }
    }

    @Override
    public boolean shouldRenderOffScreen(BatterySocketBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
