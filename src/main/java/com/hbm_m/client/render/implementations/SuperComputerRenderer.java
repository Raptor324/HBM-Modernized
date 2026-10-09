package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineSuperComputerBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderSuperComputer}: Gehaeuse ("Computer") und Lichtleisten ("Lights") mit einer Lauflicht-Textur, die
 * jede Sekunde einmal durchlaeuft; ohne laufendes Rezept bleiben die Lichter schwarz.
 */
public class SuperComputerRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineSuperComputerBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/supercomputer.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/supercomputer.png");
    public static final ResourceLocation SCAN_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/supercomputer_scan.png");

    public SuperComputerRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineSuperComputerBlockEntity computer, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 2/4/3/5 -> 180/270/0/90 Grad
        switch (computer.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            default -> { }
        }

        MODEL.renderPart("Computer", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);

        float c = computer.didProcess ? 1F : 0F;
        float scroll = computer.getLevel() != null ? (computer.getLevel().getGameTime() % 20 + interp) / 20F : 0F;
        MODEL.renderPartShiftedU("Lights", ps, buf.getBuffer(RenderType.entityCutoutNoCull(SCAN_TEX)), LightTexture.FULL_BRIGHT, c, c, c, -scroll);

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineSuperComputerBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
