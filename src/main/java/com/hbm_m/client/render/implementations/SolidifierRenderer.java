package com.hbm_m.client.render.implementations;

import java.util.function.Function;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineLiquefactorBlockEntity;
import com.hbm_m.blockentity.machines.MachineSolidifierBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code RenderSolidifier}/{@code RenderLiquefactor}: Gehaeuse, eine mit dem Tank steigende Fluidsaeule in der
 * Fluidfarbe (unbeleuchtet) und das zart getoente Glas. Der Verfluessiger dreht sich im Original nicht mit.
 */
public class SolidifierRenderer<T extends BlockEntity> implements com.hbm_m.client.render.HbmBerBounds<T> {

    public static final SimpleObjModel SOLIDIFIER = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/solidifier.obj"));
    public static final ResourceLocation SOLIDIFIER_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/solidifier.png");
    public static final SimpleObjModel LIQUEFACTOR = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/liquefactor.obj"));
    public static final ResourceLocation LIQUEFACTOR_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/liquefactor.png");

    private final SimpleObjModel model;
    private final ResourceLocation tex;
    private final double pivot;
    private final boolean rotate;
    private final Function<T, FluidTank> tank;

    private SolidifierRenderer(SimpleObjModel model, ResourceLocation tex, double pivot, boolean rotate, Function<T, FluidTank> tank) {
        this.model = model;
        this.tex = tex;
        this.pivot = pivot;
        this.rotate = rotate;
        this.tank = tank;
    }

    public static SolidifierRenderer<MachineSolidifierBlockEntity> solidifier(BlockEntityRendererProvider.Context ctx) {
        return new SolidifierRenderer<>(SOLIDIFIER, SOLIDIFIER_TEX, 1.25, true, MachineSolidifierBlockEntity::getTank);
    }

    public static SolidifierRenderer<MachineLiquefactorBlockEntity> liquefactor(BlockEntityRendererProvider.Context ctx) {
        return new SolidifierRenderer<>(LIQUEFACTOR, LIQUEFACTOR_TEX, 1, false, MachineLiquefactorBlockEntity::getTank);
    }

    @Override
    public void render(T te, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original (nur Verfestiger): Metadaten 2/4/3/5 -> 90/180/270/0 Grad
        if (rotate && te.getBlockState().hasProperty(DummyableMachineBlock.FACING)) {
            switch (te.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
                case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
                case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
                default -> { }
            }
        }

        model.renderPart("Main", ps, buf.getBuffer(RenderType.entityCutoutNoCull(tex)), light);

        FluidTank t = tank.apply(te);
        if (t.getFill() > 0) {
            int color = FluidType.forFluid(t.getTankType()).getColor();
            double height = (double) t.getFill() / (double) t.getMaxFill();
            ps.pushPose();
            ps.translate(0, pivot, 0);
            ps.scale(1, (float) height, 1);
            ps.translate(0, -pivot, 0);
            model.renderPartColor("Fluid", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL),
                    ((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, 1F);
            ps.popPose();
        }

        model.renderPartColor("Glass", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.TRANSLUCENT_COLOR_NOCULL), 0.75F, 1.0F, 1.0F, 0.15F);

        ps.popPose();
    }

    @Override public int getViewDistance() { return 256; }
}
