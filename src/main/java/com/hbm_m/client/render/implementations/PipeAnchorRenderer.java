package com.hbm_m.client.render.implementations;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.api.fluids.IPipelineBase;
import com.hbm_m.blockentity.network.PipeAnchorBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code RenderPipeAnchor}: Anker nach Anbringseite gedreht, Leitung (in aufgehellter Fluessigkeitsfarbe) nur vom "dominanten" Anker. */
<<<<<<< HEAD
public class PipeAnchorRenderer<T extends BlockEntity & IPipelineBase> implements BlockEntityRenderer<T> {
=======
public class PipeAnchorRenderer implements com.hbm_m.client.render.HbmBerBounds<PipeAnchorBlockEntity> {
>>>>>>> 5cf60b6108d271636c3d48b21b60db45caf65f6b

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/network/pipe_anchor.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/network/pipe_anchor.png");
    public static final ResourceLocation TEX_EXHAUST = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/network/pipe_anchor_exhaust.png");
    public static final ResourceLocation TEX_PNEUMATIC = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/network/pipe_anchor_pneumatic.png");

    private final ResourceLocation texture;

    public PipeAnchorRenderer(BlockEntityRendererProvider.Context ctx) { this(ctx, TEX); }

    public PipeAnchorRenderer(BlockEntityRendererProvider.Context ctx, ResourceLocation texture) { this.texture = texture; }

    @Override
    public void render(T te, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(texture));
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);

        ps.pushPose();
        switch (te.getBlockState().getValue(BlockStateProperties.FACING)) {
            case DOWN -> ps.mulPose(Axis.XP.rotationDegrees(180));
            case UP -> { }
            case NORTH -> { ps.mulPose(Axis.XP.rotationDegrees(90)); ps.mulPose(Axis.ZP.rotationDegrees(180)); }
            case SOUTH -> ps.mulPose(Axis.XP.rotationDegrees(90));
            case WEST -> { ps.mulPose(Axis.XP.rotationDegrees(90)); ps.mulPose(Axis.ZP.rotationDegrees(90)); }
            case EAST -> { ps.mulPose(Axis.XP.rotationDegrees(90)); ps.mulPose(Axis.ZP.rotationDegrees(270)); }
        }
        ps.translate(0, -0.5F, 0);
        MODEL.renderPart("Anchor", ps, vc, light);
        ps.popPose();

        for (BlockPos p : te.getConnected()) {
            BlockEntity tile = te.getLevel().getBlockEntity(p);
            if (!(tile instanceof IPipelineBase other) || te.getNetworkType() != other.getNetworkType()) continue;
            Vec3 a = te.getConnectionPoint(), b = other.getConnectionPoint();
            if (!isDominant(a, b)) continue;
            double dX = b.x - a.x, dY = b.y - a.y, dZ = b.z - a.z;
            double hyp = Math.sqrt(dX * dX + dZ * dZ);
            double yaw = Math.toDegrees(Math.atan2(dX, dZ));
            double pitch = Math.toDegrees(Math.atan2(dY, hyp));
            double length = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees((float) yaw));
            ps.mulPose(Axis.XP.rotationDegrees((float) (90 - pitch)));

            ps.pushPose();
            ps.scale(1, (float) length, 1);
            ps.translate(0, -0.5, 0);
            // Original: nur der Fluessigkeits-Anker faerbt die Leitung, alle anderen bleiben weiss
            Fluid f = te instanceof PipeAnchorBlockEntity fa ? (fa.getFluidType() == Fluids.EMPTY ? ModFluids.NONE.getSource() : fa.getFluidType()) : null;
            int c = f == null ? 0xFFFFFF : HbmFluidRegistry.getTintColor(f);
            int r = c >> 16 & 255, g = c >> 8 & 255, bl = c & 255;
            if (f != null) { r = (int) (r + (255 - r) * 0.25D); g = (int) (g + (255 - g) * 0.25D); bl = (int) (bl + (255 - bl) * 0.25D); }
            MODEL.renderPartTinted("Pipe", ps, vc, light, r / 255F, g / 255F, bl / 255F);
            ps.popPose();

            ps.pushPose();
            ps.translate(0, length / 2D - 1.5, 0);
            MODEL.renderPart("Ring", ps, vc, light);
            ps.popPose();

            ps.popPose();
        }
        ps.popPose();
    }

    /** {@code isDominant}: nur einer der beiden Anker zeichnet die Leitung. */
    public static boolean isDominant(Vec3 first, Vec3 second) {
        if (first.x < second.x) return true;
        if (first.x > second.x) return false;
        if (first.y < second.y) return true;
        if (first.y > second.y) return false;
        if (first.z < second.z) return true;
        return false;
    }

    @Override public boolean shouldRenderOffScreen(T te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
