package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;

import com.hbm_m.api.fluids.IFluidConnectorBlock;
import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.DiamondPronter;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code RenderBigAssTank}, {@code RenderOrbus} und {@code RenderFluidBarrel}. */
public final class TankRenderers {

    private TankRenderers() {}

    static boolean isNone(Fluid fluid) {
        return fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource();
    }

    /** Original {@code FluidType.getTexture()}: Tanktextur der Sorte (Port: textures/block/tank/tank_*.png). */
    static ResourceLocation tankTexture(MachineFluidTankBlockEntity be) {
        ResourceLocation loc = be.getTankTextureLocation();
        return ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), "textures/" + loc.getPath() + ".png");
    }

    // ── RenderBigAssTank ───────────────────────────────────────────────────────────────

    /**
     * Grosstank: gekippt wie {@code tilted}, Gefahrenrauten an beiden Enden und zwei Sichtfenster, deren Pegel
     * ({@code fill * 1.5 / maxFill}) mit der Tanktextur seitlich durchlaeuft.
     */
    public static class BigAssTank implements com.hbm_m.client.render.HbmBerBounds<MachineFluidTankBlockEntity> {

        static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/bigasstank.obj"));
        static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/bigasstank.png");

        public BigAssTank(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(MachineFluidTankBlockEntity bat, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            ps.translate(0.5D, 0D, 0.5D);
            if (bat.tilted) {
                ps.translate(0, -1, 0);
                ps.mulPose(Axis.ZP.rotationDegrees(10));
                ps.mulPose(Axis.YP.rotationDegrees(5));
            }
            switch (ObjBerHelper.meta(bat)) {
                case 2 -> ObjBerHelper.rotY(ps, 270);
                case 4 -> ObjBerHelper.rotY(ps, 0);
                case 3 -> ObjBerHelper.rotY(ps, 90);
                case 5 -> ObjBerHelper.rotY(ps, 180);
            }

            MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

            Fluid fluid = bat.getFluidTank().getTankType();
            if (!isNone(fluid)) {
                FluidType type = FluidType.forFluid(fluid);
                ps.pushPose();
                ps.mulPose(Axis.YP.rotationDegrees(22.5F));
                for (int j = 0; j < 2; j++) {
                    ps.pushPose();
                    ps.translate(5.5, 2, 0);
                    DiamondPronter.pront(ps, buf, type.poison, type.flammability, type.reactivity, type.symbol, light, OverlayTexture.NO_OVERLAY);
                    ps.popPose();
                    ps.mulPose(Axis.YP.rotationDegrees(180));
                }
                ps.popPose();
            }

            // Pegelfenster: ohne Culling, Blend 770/771, Farbe weiss
            VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(tankTexture(bat)));
            Matrix4f m = ps.last().pose();
            int fill = bat.getFluidTank().getFill(), max = Math.max(1, bat.getFluidTank().getMaxFill());
            float height = (float) (fill * 1.5D / max);
            float off = 5.9375F;
            double speed = 250D;
            double scaleFactor = 0.5D;
            long time = bat.getLevel() != null ? bat.getLevel().getGameTime() : 0L;
            float minU = (float) (-((time % speed + interp) / speed) % 1D);
            float maxU = (float) (minU + 1 * scaleFactor);
            float topV = (float) (-height * 2 * scaleFactor);

            v(vc, m, -off, 1.75F, -0.25F, minU, 0, light);
            v(vc, m, -off, 1.75F + height, -0.25F, minU, topV, light);
            v(vc, m, -off, 1.75F + height, 0.25F, maxU, topV, light);
            v(vc, m, -off, 1.75F, 0.25F, maxU, 0, light);

            v(vc, m, off, 1.75F, -0.25F, maxU, 0, light);
            v(vc, m, off, 1.75F + height, -0.25F, maxU, topV, light);
            v(vc, m, off, 1.75F + height, 0.25F, minU, topV, light);
            v(vc, m, off, 1.75F, 0.25F, minU, 0, light);

            ps.popPose();
        }

        private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float vv, int light) {
            com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x, y, z, 255, 255, 255, 255, u, vv, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
        }

        @Override public boolean shouldRenderOffScreen(MachineFluidTankBlockEntity be) { return true; }
        @Override public int getViewDistance() { return 256; }
    }

    // ── RenderOrbus ────────────────────────────────────────────────────────────────────

    /**
     * Orbus: schwebende Kugel in Fluidfarbe ({@code scale = fill / maxFill}, Hoehe 2.5 mit Sinuswippen), Gehaeuse und
     * drei Strahlen durch die Mitte, solange etwas im Tank ist.
     */
    public static class Orbus implements com.hbm_m.client.render.HbmBerBounds<MachineFluidTankBlockEntity> {

        static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/orbus.obj"));
        static final SimpleObjModel SPHERE_UV = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/sphere_uv.obj"));
        static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/orbus.png");

        public Orbus(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(MachineFluidTankBlockEntity orbus, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            switch (ObjBerHelper.meta(orbus)) {
                case 2 -> ps.translate(1F, 0F, 1F);
                case 4 -> ps.translate(1F, 0F, 0F);
                case 3 -> ps.translate(0F, 0F, 0F);
                case 5 -> ps.translate(0F, 0F, 1F);
            }

            int fill = orbus.getFluidTank().getFill();
            double scale = (double) fill / (double) Math.max(1, orbus.getFluidTank().getMaxFill());
            long time = orbus.getLevel() != null ? orbus.getLevel().getGameTime() : 0L;

            if (fill > 0) {
                Fluid fluid = orbus.getFluidTank().getTankType();
                int c = isNone(fluid) ? 0xFFFFFF : FluidType.forFluid(fluid).getColor();
                ps.pushPose();
                ps.translate(0, 2.5D + Math.sin(((time + interp) * 0.1D) % (Math.PI * 2D)) * 0.125 * scale, 0);
                ps.scale((float) scale, (float) scale, (float) scale);
                SPHERE_UV.renderAllColor(ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL),
                        ((c & 0xff0000) >> 16) / 255F, ((c & 0x00ff00) >> 8) / 255F, (c & 0x0000ff) / 255F, 1F);
                ps.popPose();
            }

            MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

            if (fill > 0) {
                ps.translate(0, 1, 0);
                BeamPronter.prontBeam(ps, buf, new Vec3(0, 3, 0), BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.SOLID, 0x101020, 0x101020, 0, 1, 0F, 6, (float) scale * 0.5F);
                BeamPronter.prontBeam(ps, buf, new Vec3(0, 3, 0), BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x202060, 0x202060, (int) (time / 2) % 1000, 6, (float) scale, 2, 0.0625F * (float) scale);
                BeamPronter.prontBeam(ps, buf, new Vec3(0, 3, 0), BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x202060, 0x202060, (int) (time / 4) % 1000, 6, (float) scale, 2, 0.0625F * (float) scale);
            }

            ps.popPose();
        }

        @Override public boolean shouldRenderOffScreen(MachineFluidTankBlockEntity be) { return true; }
        @Override public int getViewDistance() { return 256; }
    }

    // ── RenderFluidBarrel ──────────────────────────────────────────────────────────────

    /**
     * Fass: das Fass selbst ist das Blockmodell; hier nur die Anschlussstutzen zu passenden Nachbarn
     * ({@code Library.canConnectFluid}) und vier Gefahrenrauten rundum, solange eine Sorte gesetzt ist.
     */
    public static class Barrel implements com.hbm_m.client.render.HbmBerBounds<MachineFluidTankBlockEntity> {

        static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/blocks/barrel.obj"));

        public Barrel(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(MachineFluidTankBlockEntity barrel, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            Fluid fluid = barrel.getFluidTank().getTankType();
            if (isNone(fluid)) return;
            FluidType type = FluidType.forFluid(fluid);
            Level level = barrel.getLevel();
            if (level == null) return;

            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);

            // Original: nur Kunststoff/Stahl/TCA/Antimaterie binden eine eigene Textur
            Block block = barrel.getBlockState().getBlock();
            String tex = block == ModBlocks.BARREL_PLASTIC.get() ? "barrel_plastic"
                    : block == ModBlocks.BARREL_STEEL.get() ? "barrel_steel"
                    : block == ModBlocks.BARREL_TCALLOY.get() ? "barrel_tcalloy"
                    : block == ModBlocks.BARREL_ANTIMATTER.get() ? "barrel_antimatter"
                    : block == ModBlocks.BARREL_CORRODED.get() ? "barrel_corroded" : "barrel_iron";
            VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/" + tex + ".png")));
            BlockPos pos = barrel.getBlockPos();

            if (canConnectFluid(level, pos.east(), Direction.EAST, fluid)) {
                ps.translate(0.0F, -0.5F, 0.0F);
                MODEL.renderPart("Connector", ps, vc, light);
                ps.translate(0.0F, 0.5F, 0.0F);
            }
            if (canConnectFluid(level, pos.west(), Direction.WEST, fluid)) {
                ps.mulPose(Axis.YP.rotationDegrees(180));
                ps.translate(0.0F, -0.5F, 0.0F);
                MODEL.renderPart("Connector", ps, vc, light);
                ps.translate(0.0F, 0.5F, 0.0F);
                ps.mulPose(Axis.YP.rotationDegrees(-180));
            }
            if (canConnectFluid(level, pos.north(), Direction.NORTH, fluid)) {
                ps.mulPose(Axis.YP.rotationDegrees(90));
                ps.translate(0.0F, -0.5F, 0.0F);
                MODEL.renderPart("Connector", ps, vc, light);
                ps.translate(0.0F, 0.5F, 0.0F);
                ps.mulPose(Axis.YP.rotationDegrees(-90));
            }
            if (canConnectFluid(level, pos.south(), Direction.SOUTH, fluid)) {
                ps.mulPose(Axis.YP.rotationDegrees(-90));
                ps.translate(0.0F, -0.5F, 0.0F);
                MODEL.renderPart("Connector", ps, vc, light);
                ps.translate(0.0F, 0.5F, 0.0F);
                ps.mulPose(Axis.YP.rotationDegrees(90));
            }

            for (int j = 0; j < 4; j++) {
                ps.pushPose();
                ps.translate(0.4, 0.30, -0.24);
                ps.scale(1.0F, 0.25F, 0.25F);
                DiamondPronter.pront(ps, buf, type.poison, type.flammability, type.reactivity, type.symbol, light, OverlayTexture.NO_OVERLAY);
                ps.popPose();
                ps.mulPose(Axis.YP.rotationDegrees(90));
            }

            ps.popPose();
        }

        /** 1:1 {@code Library.canConnectFluid}: Nachbarblock oder -tile muss die Seite zum Fass annehmen. */
        static boolean canConnectFluid(Level level, BlockPos pos, Direction dir, Fluid fluid) {
            if (pos.getY() > level.getMaxBuildHeight() - 1 || pos.getY() < level.getMinBuildHeight()) return false;
            if (level.getBlockState(pos).getBlock() instanceof IFluidConnectorBlock con
                    && con.canConnect(fluid, level, pos, dir.getOpposite())) return true;
            BlockEntity te = level.getBlockEntity(pos);
            return te instanceof IFluidConnectorMK2 con && con.canConnect(fluid, dir.getOpposite());
        }
    }
}
