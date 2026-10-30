package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCatalyticReformerBlockEntity;
import com.hbm_m.blockentity.machines.MachineCrackingTowerBlockEntity;
import com.hbm_m.blockentity.machines.MachineFractionTowerBlockEntity;
import com.hbm_m.blockentity.machines.MachineVacuumDistillBlockEntity;
import com.hbm_m.blockentity.machines.RefuelerBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.util.HorsePronter;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.Polaroid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Modern modular Nucleus renderers for petrochemical processing installations:
 * <ul>
 *   <li>Catalytic Reformer: High-pressure reforming column with facing transform and Polaroid-11 horse hook;</li>
 *   <li>Cracking Tower: Multi-stage catalytic cracking distillation column;</li>
 *   <li>Fractionation Tower: Multi-product oil fractionation column;</li>
 *   <li>Vacuum Distillation Tower: Heavy vacuum distillation unit;</li>
 *   <li>Refueler: Industrial fuel loading station with dynamic fluid volume indicator.</li>
 * </ul>
 */
public final class MachinePetrochemRenderer {

    private static final ResourceLocation HORSE_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/horse/dyx.png");
    private static final SimpleObjModel REFUELER_MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/refueler.obj"));

    public static void register() {
        registerReformer();
        registerCrackingTower();
        registerFractionTower();
        registerVacuumDistill();
        registerRefueler();
    }

    private static void registerReformer() {
        // catalytic_reformer.obj слит в одну группу "Plane"
        MachineRenderers.machine("catalytic_reformer", ModBlockEntities.CATALYTIC_REFORMER_BE.get(),
                MachineCatalyticReformerBlockEntity.class)
            .part("Plane")
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Plane")
            .hook(new MachineRenderHook<MachineCatalyticReformerBlockEntity>() {
                @Override
                public void render(MachineCatalyticReformerBlockEntity be, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    if (Polaroid.id() == 11) {
                        ps.pushPose();
                        ps.translate(-1.125, 1.375, 1);
                        float s = 0.125F;
                        ps.scale(s, s, s);
                        ps.mulPose(Axis.YN.rotationDegrees((float) (System.currentTimeMillis() / 5D % 360D)));
                        ps.translate(0, 0.1, -0.5);

                        HorsePronter.reset();
                        double r = 60;
                        HorsePronter.pose(HorsePronter.id_body, 0, -r, 0);
                        HorsePronter.pose(HorsePronter.id_tail, 0, 45, 90);
                        HorsePronter.pose(HorsePronter.id_lbl, 0, -90 + r, 35);
                        HorsePronter.pose(HorsePronter.id_rbl, 0, -90 + r, -35);
                        HorsePronter.pose(HorsePronter.id_lfl, 0, r - 10, 5);
                        HorsePronter.pose(HorsePronter.id_rfl, 0, r - 10, -5);
                        HorsePronter.pose(HorsePronter.id_head, 0, r, 0);
                        HorsePronter.enableHorn();
                        HorsePronter.enableWings();
                        HorsePronter.pront(ps, buf.getBuffer(RenderType.entityCutoutNoCull(HORSE_TEX)), light);

                        double scale = 0.25;
                        ps.translate(0.02, 1.13, -0.42);
                        ps.scale((float) scale, (float) scale, (float) scale);
                        ps.mulPose(Axis.YN.rotationDegrees(90));
                        ps.mulPose(Axis.ZN.rotationDegrees(60));
                        TrinketRenderer.renderItem2D(ps, buf, new ItemStack(ModItems.CIGARETTE.get()), light);
                        ps.popPose();
                    }
                }

                @Override
                public boolean mayDrawContent(MachineCatalyticReformerBlockEntity be) {
                    return Polaroid.id() == 11;
                }
            })
            .register();
    }

    private static void registerCrackingTower() {
        // cracking_tower.obj слит в одну группу "Cube_Cube.001"
        MachineRenderers.machine("cracking_tower", ModBlockEntities.CRACKING_TOWER_BE.get(),
                MachineCrackingTowerBlockEntity.class)
            .part("Cube_Cube.001")
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Cube_Cube.001")
            .register();
    }

    private static void registerFractionTower() {
        // fraction_tower.obj слит в одну группу "Plane"
        MachineRenderers.machine("fraction_tower", ModBlockEntities.FRACTION_TOWER_BE.get(),
                MachineFractionTowerBlockEntity.class)
            .part("Plane")
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Plane")
            .register();
    }

    private static void registerVacuumDistill() {
        // vacuum_distill.obj слит в одну группу "Cylinder"
        MachineRenderers.machine("vacuum_distill", ModBlockEntities.VACUUM_DISTILL_BE.get(),
                MachineVacuumDistillBlockEntity.class)
            .part("Cylinder")
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Cylinder")
            .register();
    }

    private static void registerRefueler() {
        MachineRenderers.machine("refueler", ModBlockEntities.REFUELER_BE.get(),
                RefuelerBlockEntity.class)
            .part("Fueler")
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Fueler")
            .hook(new MachineRenderHook<RefuelerBlockEntity>() {
                @Override
                public void render(RefuelerBlockEntity refueler, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    if (refueler.tank == null || refueler.tank.getFill() <= 0) return;
                    double fillLevel = refueler.prevFillLevel + (refueler.fillLevel - refueler.prevFillLevel) * pt;
                    int color = FluidType.forFluid(refueler.tank.getTankType()).getColor();
                    ps.pushPose();
                    REFUELER_MODEL.renderPartColorClippedY("Fluid", ps,
                            buf.getBuffer(ClientRenderHandler.CustomRenderTypes.ADDITIVE_TRIANGLES),
                            ((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 0.75F,
                            (float) ((1.0 - fillLevel) * -0.625), 0.125F);
                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(RefuelerBlockEntity be) {
                    return be.tank != null && be.tank.getFill() > 0;
                }
            })
            .register();
    }

    private MachinePetrochemRenderer() {}
}
