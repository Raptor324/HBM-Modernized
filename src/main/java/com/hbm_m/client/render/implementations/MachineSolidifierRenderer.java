package com.hbm_m.client.render.implementations;

import java.util.function.Function;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineLiquefactorBlockEntity;
import com.hbm_m.blockentity.machines.MachineSolidifierBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Modern Nucleus renderer implementation for thermal phase-change basin machines:
 * <ul>
 *   <li>Solidifier: Solidification chamber with dynamic fluid column scaling and glass enclosure;</li>
 *   <li>Liquefactor: Melting/liquefaction vessel with dynamic fluid level basin.</li>
 * </ul>
 */
public final class MachineSolidifierRenderer {

    private static final SimpleObjModel SOLIDIFIER_MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/solidifier.obj"));
    private static final SimpleObjModel LIQUEFACTOR_MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/liquefactor.obj"));

    public static void register() {
        registerSolidifier();
        registerLiquefactor();
    }

    private static void registerSolidifier() {
        MachineRenderers.machine("solidifier", ModBlockEntities.SOLIDIFIER_BE.get(),
                MachineSolidifierBlockEntity.class)
            .part("Main")
            .part("Glass")
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Main", "Glass")
            .hook(new PhaseChangeFluidHook<>(SOLIDIFIER_MODEL, 1.25, MachineSolidifierBlockEntity::getTank))
            .register();
    }

    private static void registerLiquefactor() {
        MachineRenderers.machine("liquefactor", ModBlockEntities.LIQUEFACTOR_BE.get(),
                MachineLiquefactorBlockEntity.class)
            .part("Main")
            .part("Glass")
            .viewDistance(256)
            .itemParts("Main", "Glass")
            .hook(new PhaseChangeFluidHook<>(LIQUEFACTOR_MODEL, 1.0, MachineLiquefactorBlockEntity::getTank))
            .register();
    }

    private static class PhaseChangeFluidHook<T extends BlockEntity> implements MachineRenderHook<T> {
        private final SimpleObjModel model;
        private final double pivot;
        private final Function<T, FluidTank> tankGetter;

        PhaseChangeFluidHook(SimpleObjModel model, double pivot, Function<T, FluidTank> tankGetter) {
            this.model = model;
            this.pivot = pivot;
            this.tankGetter = tankGetter;
        }

        @Override
        public void render(T be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
            FluidTank tank = tankGetter.apply(be);
            if (tank != null && tank.getFill() > 0) {
                int color = FluidType.forFluid(tank.getTankType()).getColor();
                double height = (double) tank.getFill() / (double) Math.max(1, tank.getMaxFill());
                ps.pushPose();
                ps.translate(0, pivot, 0);
                ps.scale(1.0F, (float) height, 1.0F);
                ps.translate(0, -pivot, 0);
                model.renderPartColor("Fluid", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL),
                        ((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, 1.0F);
                ps.popPose();
            }

            model.renderPartColor("Glass", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.TRANSLUCENT_COLOR_NOCULL),
                    0.75F, 1.0F, 1.0F, 0.15F);
        }

        @Override
        public boolean mayDrawContent(T be) {
            return true;
        }
    }

    private MachineSolidifierRenderer() {}
}
