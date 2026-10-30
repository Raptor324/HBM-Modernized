package com.hbm_m.client.render.implementations;

import java.util.Random;

import org.joml.Matrix4f;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.BatterySocketBlockEntity;
import com.hbm_m.blockentity.machines.ChargerBlockEntity;
import com.hbm_m.blockentity.machines.MachineRTGBlockEntity;
import com.hbm_m.blockentity.machines.TeslaBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.HorsePronter;
import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.item.fekal_electric.ItemBatteryPack;
import com.hbm_m.item.fekal_electric.ItemBatterySC;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Modern Nucleus renderer implementation for power storage, distribution, and generation installations:
 * <ul>
 *   <li>Charger: Industrial battery charging station with translating arms, fullbright status light, and slide;</li>
 *   <li>RTG: Radioisotope thermoelectric generator with dynamic face connector detection;</li>
 *   <li>Tesla Coil: High-voltage discharge tower with procedural stepped lightning arcs;</li>
 *   <li>Battery Socket: Heavy energy bank multiblock with animated inserted battery items and corner spark arcs.</li>
 * </ul>
 */
public final class MachinePowerRenderer {

    private static final SimpleObjModel RTG_MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/rtg.obj"));
    private static final ResourceLocation RTG_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/rtg.png");

    private static final SimpleObjModel BATTERY_MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/battery.obj"));
    private static final ResourceLocation BATTERY_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/battery_socket.png");
    private static final ResourceLocation HORSE_SUNBURST = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/horse/sunburst.png");

    private static final float TESLA_THICKNESS = 0.03125F;
    private static final double TESLA_JITTER = 0.125D;

    public static void register() {
        registerCharger();
        registerRTG();
        registerTesla();
        registerBatterySocket();
    }

    private static void registerCharger() {
        MachineRenderers.machine("charger", ModBlockEntities.CHARGER_BE.get(),
                ChargerBlockEntity.class)
            .part("Base")
            .lightOverride("Light", be -> LightTexture.FULL_BRIGHT)
            .part("Light")
            .part("Left", (be, pt, gt, ps) -> {
                double time = Math.min(1.0, Math.max(0.0, (be.getLastUsingTicks() + (be.getUsingTicks() - be.getLastUsingTicks()) * pt) / 20.0));
                double extend = Math.min(1.0, time * 2.0);
                double swivel = Math.max(0.0, (time - 0.5) * 2.0);
                armOffset(ps, extend);
                ps.translate(0.0, 0.28, 0.0);
                ps.mulPose(Axis.XP.rotationDegrees((float) (30.0 * swivel)));
                ps.translate(0.0, -0.28, 0.0);
                return true;
            })
            .part("Right", (be, pt, gt, ps) -> {
                double time = Math.min(1.0, Math.max(0.0, (be.getLastUsingTicks() + (be.getUsingTicks() - be.getLastUsingTicks()) * pt) / 20.0));
                double extend = Math.min(1.0, time * 2.0);
                double swivel = Math.max(0.0, (time - 0.5) * 2.0);
                armOffset(ps, extend);
                ps.translate(0.0, 0.28, 0.0);
                ps.mulPose(Axis.XP.rotationDegrees((float) (-30.0 * swivel)));
                ps.translate(0.0, -0.28, 0.0);
                return true;
            })
            .part("Slide", (be, pt, gt, ps) -> {
                double time = Math.min(1.0, Math.max(0.0, (be.getLastUsingTicks() + (be.getUsingTicks() - be.getLastUsingTicks()) * pt) / 20.0));
                double extend = Math.min(1.0, time * 2.0);
                armOffset(ps, extend);
                return true;
            })
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Base", "Light", "Left", "Right", "Slide")
            .register();
    }

    private static void armOffset(PoseStack pose, double extend) {
        pose.translate(-0.34375, 0.25, 0.0);
        pose.mulPose(Axis.ZP.rotationDegrees(10.0F));
        pose.translate(0.34375, -0.25, 0.0);
        pose.translate(0.0, -0.25 * extend, 0.0);
    }

    private static void registerRTG() {
        MachineRenderers.machine("rtg", ModBlockEntities.MACHINE_RTG_BE.get(),
                MachineRTGBlockEntity.class)
            .part("Gen")
            .part("Body")
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Gen", "Body")
            .hook(new MachineRenderHook<MachineRTGBlockEntity>() {
                @Override
                public void render(MachineRTGBlockEntity te, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    Level level = te.getLevel();
                    if (level == null) return;
                    BlockPos p = te.getBlockPos();

                    ps.pushPose();
                    VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(RTG_TEX));

                    if (canConnect(level, p.east(), Direction.WEST)) RTG_MODEL.renderPart("Connector", ps, vc, light);

                    if (canConnect(level, p.west(), Direction.EAST)) {
                        ps.mulPose(Axis.YP.rotationDegrees(180.0F));
                        RTG_MODEL.renderPart("Connector", ps, vc, light);
                        ps.mulPose(Axis.YP.rotationDegrees(-180.0F));
                    }

                    if (canConnect(level, p.north(), Direction.SOUTH)) {
                        ps.mulPose(Axis.YP.rotationDegrees(90.0F));
                        RTG_MODEL.renderPart("Connector", ps, vc, light);
                        ps.mulPose(Axis.YP.rotationDegrees(-90.0F));
                    }

                    if (canConnect(level, p.south(), Direction.NORTH)) {
                        ps.mulPose(Axis.YP.rotationDegrees(-90.0F));
                        RTG_MODEL.renderPart("Connector", ps, vc, light);
                        ps.mulPose(Axis.YP.rotationDegrees(90.0F));
                    }

                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(MachineRTGBlockEntity be) {
                    return true;
                }
            })
            .register();
    }

    private static boolean canConnect(Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof IEnergyConnector c && c.canConnectEnergy(side);
    }

    private static void registerTesla() {
        // tesla.obj слит в одну группу "Cube.001"
        MachineRenderers.machine("tesla", ModBlockEntities.TESLA_BE.get(),
                TeslaBlockEntity.class)
            .part("Cube.001")
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Cube.001")
            .hook(new MachineRenderHook<TeslaBlockEntity>() {
                @Override
                public void render(TeslaBlockEntity be, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    if (be.getTargets().isEmpty() || be.getLevel() == null) return;
                    VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL);

                    ps.pushPose();
                    ps.translate(0.0, TeslaBlockEntity.ORIGIN_OFFSET_Y, 0.0);
                    Matrix4f m = ps.last().pose();
                    long seed = be.getLevel().getGameTime() % 1000L + 1L;
                    int index = 0;
                    for (Vec3 target : be.getTargets()) {
                        renderTeslaArc(vc, m, target, seed + index++ * 7919L);
                    }
                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(TeslaBlockEntity be) {
                    return !be.getTargets().isEmpty();
                }
            })
            .register();
    }

    private static void renderTeslaArc(VertexConsumer vc, Matrix4f m, Vec3 target, long seed) {
        double length = target.length();
        int segments = Math.max(1, (int) (length * 5.0));
        RandomSource rand = RandomSource.create(seed);
        Vec3 prev = Vec3.ZERO;
        for (int i = 1; i <= segments; i++) {
            Vec3 point = target.scale((double) i / segments);
            if (i < segments) {
                point = point.add((rand.nextDouble() - 0.5) * TESLA_JITTER,
                        (rand.nextDouble() - 0.5) * TESLA_JITTER,
                        (rand.nextDouble() - 0.5) * TESLA_JITTER);
            }
            renderArcSegment(vc, m, prev, point);
            prev = point;
        }
    }

    private static void renderArcSegment(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b) {
        Vec3 dir = b.subtract(a);
        if (dir.lengthSqr() < 1.0E-8) return;
        Vec3 up = Math.abs(dir.normalize().y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 side1 = dir.cross(up).normalize().scale(TESLA_THICKNESS);
        Vec3 side2 = dir.cross(side1).normalize().scale(TESLA_THICKNESS);
        drawTeslaQuad(vc, m, a.add(side1), b.add(side1), b.subtract(side1), a.subtract(side1));
        drawTeslaQuad(vc, m, a.add(side2), b.add(side2), b.subtract(side2), a.subtract(side2));
    }

    private static void drawTeslaQuad(VertexConsumer vc, Matrix4f m, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
        for (Vec3 p : new Vec3[] {p0, p1, p2, p3}) {
            RenderHooks.vertexColor(vc, m, (float) p.x, (float) p.y, (float) p.z, 0x40, 0x40, 0x40, 255);
        }
        for (Vec3 p : new Vec3[] {p3, p2, p1, p0}) {
            RenderHooks.vertexColor(vc, m, (float) p.x, (float) p.y, (float) p.z, 0x40, 0x40, 0x40, 255);
        }
    }

    private static void registerBatterySocket() {
        MachineRenderers.machine("battery_socket", ModBlockEntities.BATTERY_SOCKET_BE.get(),
                BatterySocketBlockEntity.class)
            .part("Base")
            .conditionalPart("Supports", be -> be.getLevel() != null && !be.getLevel().getBlockState(be.getBlockPos().above(2)).isAir())
            .facing(ObjBerHelper::facing)
            .viewDistance(256)
            .itemParts("Base", "Supports")
            .hook(new MachineRenderHook<BatterySocketBlockEntity>() {
                @Override
                public void render(BatterySocketBlockEntity socket, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    Level level = socket.getLevel();
                    if (level == null) return;

                    ItemStack render = socket.getItemHandler().getStackInSlot(0);
                    if (render.isEmpty()) return;
                    if (render.getItem() instanceof ItemBatteryPack || render.getItem() instanceof ItemBatterySC) return;

                    long time = level.getGameTime();
                    if (render.getItem() instanceof ItemCreativeBattery) {
                        ps.pushPose();
                        ps.scale(0.75F, 0.75F, 0.75F);
                        ps.mulPose(Axis.YN.rotationDegrees((float) ((time % 360 + pt) * 25.0)));
                        HorsePronter.reset();
                        HorsePronter.enableHorn();
                        HorsePronter.pront(ps, buf.getBuffer(RenderType.entityCutoutNoCull(HORSE_SUNBURST)), light);
                        ps.popPose();
                    } else {
                        ps.pushPose();
                        ps.translate(0.0, 0.5, 0.0);
                        ps.scale(1.5F, 1.5F, 1.5F);
                        ps.mulPose(Axis.YN.rotationDegrees((float) ((time % 360 + pt) * 2.5)));
                        ItemStack one = render.copy();
                        one.setCount(1);
                        ps.scale(0.5128205F, 0.5128205F, 0.5128205F);
                        ps.translate(0.0F, -0.05F, 0.0F);
                        ps.mulPose(Axis.YP.rotationDegrees(180.0F));
                        ps.scale(0.5F, 0.5F, 0.5F);
                        ps.translate(0.0F, 0.25F, 0.0F);
                        Minecraft.getInstance().getItemRenderer().renderStatic(one, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, ps, buf, level, 0);
                        ps.popPose();
                    }

                    Random rand = new Random(time / 5L);
                    for (int i = -1; i <= 1; i += 2) {
                        for (int j = -1; j <= 1; j += 2) {
                            if (rand.nextInt(4) == 0) {
                                ps.pushPose();
                                ps.translate(0.0, 0.75, 0.0);
                                int start = (int) (System.currentTimeMillis() % 1000L) / 50;
                                BeamPronter.prontBeam(ps, buf, new Vec3(0.4375 * i, 1.1875, 0.4375 * j),
                                        BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID,
                                        0x404040, 0x002040, start, 15, 0.0625F, 3, 0.025F);
                                BeamPronter.prontBeam(ps, buf, new Vec3(0.4375 * i, 1.1875, 0.4375 * j),
                                        BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID,
                                        0x404040, 0x002040, start, 1, 0, 3, 0.025F);
                                ps.popPose();
                            }
                        }
                    }
                }

                @Override
                public boolean mayDrawContent(BatterySocketBlockEntity be) {
                    ItemStack s = be.getItemHandler().getStackInSlot(0);
                    return !s.isEmpty() && !(s.getItem() instanceof ItemBatteryPack) && !(s.getItem() instanceof ItemBatterySC);
                }
            })
            .register();
    }

    private MachinePowerRenderer() {}
}
