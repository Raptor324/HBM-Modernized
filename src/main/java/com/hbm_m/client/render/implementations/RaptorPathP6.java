package com.hbm_m.client.render.implementations;

import static com.hbm_m.client.render.raptorpath.RaptorParts.idleStatic;
import static com.hbm_m.client.render.raptorpath.RaptorParts.mesh;
import static com.hbm_m.client.render.raptorpath.RaptorParts.rotY;
import static com.hbm_m.client.render.raptorpath.RaptorParts.select;
import static com.hbm_m.client.render.raptorpath.RaptorParts.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.api.fluids.IPipelineBase;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.MachineSolarBoilerBlock;
import com.hbm_m.block.machines.PistonInserterBlock;
import com.hbm_m.block.machines.RefuelerBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.BarrelAntimatterBlockEntity;
import com.hbm_m.blockentity.machines.BarrelCorrodedBlockEntity;
import com.hbm_m.blockentity.machines.BarrelIronBlockEntity;
import com.hbm_m.blockentity.machines.BarrelPlasticBlockEntity;
import com.hbm_m.blockentity.machines.BarrelSteelBlockEntity;
import com.hbm_m.blockentity.machines.BarrelTcalloyBlockEntity;
import com.hbm_m.blockentity.machines.MachineBigAssTankBlockEntity;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.blockentity.machines.MachineLiquefactorBlockEntity;
import com.hbm_m.blockentity.machines.MachineSolarBoilerBlockEntity;
import com.hbm_m.blockentity.machines.MachineSolidifierBlockEntity;
import com.hbm_m.blockentity.machines.MachineStrandCasterBlockEntity;
import com.hbm_m.blockentity.machines.MachineSuperComputerBlockEntity;
import com.hbm_m.blockentity.machines.OrbusBlockEntity;
import com.hbm_m.blockentity.machines.PistonInserterBlockEntity;
import com.hbm_m.blockentity.machines.RefuelerBlockEntity;
import com.hbm_m.blockentity.machines.TeslaBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity.ColumnType;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlAutoBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlManualBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKCraneConsoleBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKRodBlockEntity;
import com.hbm_m.blockentity.network.ExhaustPipeAnchorBlockEntity;
import com.hbm_m.blockentity.network.PipeAnchorBlockEntity;
import com.hbm_m.blockentity.network.PneumaticPipeAnchorBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpecBuilder;
import com.hbm_m.client.render.raptorpath.ObjAtlasMesh;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.DiamondPronter;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * Render-Migration Paket P6 (Kategorie B): RBMK-Saeulen und -Konsole, Tanks/Faesser, Leitungsanker und die
 * uebrigen Maschinen mit Fluessigkeit/Strahlen. Die Saeulenkoerper werden je Zustand (Typ, Hoehe, Deckel) als
 * ein Netz gebacken, damit grosse Reaktoren instanziert gezeichnet werden.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class RaptorPathP6 {

    private RaptorPathP6() {}

    private static ResourceLocation rl(String p) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p);
    }

    private static ObjAtlasMesh p(SimpleObjModel m, ResourceLocation t, String key, String... parts) {
        return ObjAtlasMesh.parts(m, t, "hbm_m/" + key, parts);
    }

    // ── RBMK-Saeulen ─────────────────────────────────────────────────────────────────────

    private static final Map<String, Map<Integer, ObjAtlasMesh>> RBMK_BODY = new HashMap<>();
    private static final Map<String, ObjAtlasMesh> RBMK_CAP = new HashMap<>();

    private static int rbmkHeight() {
        return RBMKDials.COLUMN_HEIGHT + 1;
    }

    /** Zustandsschluessel wie im alten BER: Brennstoff, Deckel (0 keiner, 1 Beton, 2 Glas), Rohrstutzen, Hoehe. */
    private static ObjAtlasMesh rbmkBody(RBMKColumnBlockEntity be) {
        boolean isFuel = be.getConsoleType() == ColumnType.FUEL;
        String prefix = be.getRenderTexturePrefix();
        ColumnType consoleType = be.getConsoleType();
        boolean stub = consoleType == ColumnType.HEATER && !be.hasLid();
        int lid = be.hasLid() && !be.hasOwnLid() ? (be.getLidState() == 2 ? 2 : 1) : 0;
        int height = rbmkHeight();
        int key = (isFuel ? 1 : 0) | (stub ? 2 : 0) | (lid << 2) | (height << 4);
        return RBMK_BODY.computeIfAbsent(prefix, k -> new HashMap<>())
                .computeIfAbsent(key, k -> rbmkBodyMesh(prefix, isFuel, stub, lid, height));
    }

    private static ObjAtlasMesh rbmkBodyMesh(String prefix, boolean isFuel, boolean stub, int lid, int height) {
        String key = "hbm_m/rbmk/" + prefix + "/" + (isFuel ? "f" : "") + (stub ? "s" : "") + lid + "h" + height;
        return ObjAtlasMesh.of(key, b -> {
            TextureAtlasSprite side = b.sprite(rl("block/rbmk/" + prefix + "_side"));
            TextureAtlasSprite top = b.sprite(rl("block/rbmk/" + prefix + "_top"));
            for (int y = 0; y < height; y++) {
                float y0 = y, y1 = y + 1;
                rq(b, side, 1, y0, 0, 0, y0, 0, 0, y1, 0, 1, y1, 0, 0, 0, -1);
                rq(b, side, 0, y0, 1, 1, y0, 1, 1, y1, 1, 0, y1, 1, 0, 0, 1);
                rq(b, side, 0, y0, 0, 0, y0, 1, 0, y1, 1, 0, y1, 0, -1, 0, 0);
                rq(b, side, 1, y0, 1, 1, y0, 0, 1, y1, 0, 1, y1, 1, 1, 0, 0);
            }
            if (!isFuel) rq(b, top, 0, height, 0, 0, height, 1, 1, height, 1, 1, height, 0, 0, 1, 0);
            rq(b, top, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, -1, 0);
            if (stub) {
                TextureAtlasSprite pipeTop = b.sprite(rl("block/rbmk/" + prefix + "_pipe_top"));
                TextureAtlasSprite pipeSide = b.sprite(rl("block/rbmk/" + prefix + "_pipe_side"));
                float y0 = height, y1 = height + 0.3f;
                float s0 = 0.15f, s1 = 0.85f;
                rq(b, pipeSide, s1, y0, s0, s0, y0, s0, s0, y1, s0, s1, y1, s0, 0, 0, -1);
                rq(b, pipeSide, s0, y0, s1, s1, y0, s1, s1, y1, s1, s0, y1, s1, 0, 0, 1);
                rq(b, pipeSide, s0, y0, s0, s0, y0, s1, s0, y1, s1, s0, y1, s0, -1, 0, 0);
                rq(b, pipeSide, s1, y0, s1, s1, y0, s0, s1, y1, s0, s1, y1, s1, 1, 0, 0);
                rq(b, pipeTop, s0, y1, s0, s0, y1, s1, s1, y1, s1, s1, y1, s0, 0, 1, 0);
            }
            if (lid != 0) {
                String suffix = lid == 2 ? "_glass" : "_cover";
                TextureAtlasSprite lidTop = b.sprite(rl("block/rbmk/" + prefix + suffix + "_top"));
                TextureAtlasSprite lidSide = b.sprite(rl("block/rbmk/" + prefix + suffix + "_side"));
                float ly0 = height, ly1 = height + 0.25f;
                rq(b, lidSide, 1, ly0, 0, 0, ly0, 0, 0, ly1, 0, 1, ly1, 0, 0, 0, -1);
                rq(b, lidSide, 0, ly0, 1, 1, ly0, 1, 1, ly1, 1, 0, ly1, 1, 0, 0, 1);
                rq(b, lidSide, 0, ly0, 0, 0, ly0, 1, 0, ly1, 1, 0, ly1, 0, -1, 0, 0);
                rq(b, lidSide, 1, ly0, 1, 1, ly0, 0, 1, ly1, 0, 1, ly1, 1, 1, 0, 0);
                rq(b, lidTop, 0, ly1, 0, 0, ly1, 1, 1, ly1, 1, 1, ly1, 0, 0, 1, 0);
            }
            if (isFuel) {
                ResourceLocation inner = rl("block/rbmk/rbmk_element_inner");
                ResourceLocation cap = rl("block/rbmk/" + prefix + "_top");
                b.pose.pushPose();
                b.pose.translate(0.5, 0, 0.5);
                for (int y = 0; y < height; y++) {
                    b.group("models/rbmk/models/rbmk_element.obj", "Inner", inner);
                    b.group("models/rbmk/models/rbmk_element.obj", "Cap", cap);
                    b.pose.translate(0, 1, 0);
                }
                b.pose.popPose();
            }
        });
    }

    /** {@code RBMKColumnRenderer.quad}: Ecken 0..3 mit UV (u0,v1) (u1,v1) (u1,v0) (u0,v0). */
    private static void rq(ObjAtlasMesh.Baker b, TextureAtlasSprite s,
                           float x0, float y0, float z0, float x1, float y1, float z1,
                           float x2, float y2, float z2, float x3, float y3, float z3,
                           float nx, float ny, float nz) {
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        b.atlasQuad(s, new float[][] {
                { x0, y0, z0, u0, v1 }, { x1, y1, z1, u1, v1 }, { x2, y2, z2, u1, v0 }, { x3, y3, z3, u0, v0 } }, nx, ny, nz);
    }

    private static ObjAtlasMesh rbmkCap(RBMKControlBlockEntity ctrl) {
        String capPrefix;
        if (ctrl instanceof RBMKControlManualBlockEntity manual) capPrefix = manual.getCapTexture();
        else if (ctrl instanceof RBMKControlAutoBlockEntity) capPrefix = "rbmk_control_auto";
        else capPrefix = "rbmk_control";
        return RBMK_CAP.computeIfAbsent(capPrefix, cp ->
                ObjAtlasMesh.groups("models/rbmk/models/rbmk_rods.obj", rl("block/rbmk/" + cp), "hbm_m/rbmk/cap_" + cp, "Lid"));
    }

    private static <T extends RBMKColumnBlockEntity> void registerColumn(String id, BlockEntityType<T> type, Class<T> cls) {
        MachineSpecBuilder<T> b = MachineRenderers.machine(id, type, cls)
                .blockTransform((be, a) -> { });
        select(b, "body", RaptorPathP6::rbmkBody);
        select(b, "cap", (be, f, t, ps) -> {
            if (!(be instanceof RBMKControlBlockEntity ctrl)) return false;
            double level = Mth.lerp(f, ctrl.lastLevel, ctrl.level);
            ps.translate(0.5, rbmkHeight() + level - 1.0, 0.5);
            return true;
        }, be -> be instanceof RBMKControlBlockEntity ctrl ? rbmkCap(ctrl) : null);
        b.hook((be, f, ps, buf, light, overlay, api) -> rbmkFuelHook(be, ps, buf, light, overlay));
        b.viewDistance(64).register();
    }

    /** Brennstaebe (Farbe des Stabs) und Tscherenkow-Leuchten aus {@code RBMKColumnRenderer}. */
    private static void rbmkFuelHook(RBMKColumnBlockEntity be, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (be.getConsoleType() != ColumnType.FUEL) return;
        if (!(be instanceof RBMKRodBlockEntity rod) || !rod.hasRod) return;
        int height = rbmkHeight();
        int color = rod.rodColor;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float bl = (color & 0xFF) / 255f;
        Map<String, List<float[]>> rodsObj = RBMKColumnRenderer.getObj("models/rbmk/models/rbmk_element_rods.obj");
        TextureAtlasSprite fuelSprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/rbmk_element_fuel");
        VertexConsumer vc = buf.getBuffer(RenderType.solid());
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        for (int y = 0; y < height; y++) {
            RBMKColumnRenderer.renderObjGroup(vc, ps.last().pose(), rodsObj.get("Rods"), fuelSprite, r, g, bl, light, overlay);
            ps.translate(0, 1, 0);
        }
        ps.popPose();
        if (rod.lastFluxQuantity > 5) {
            VertexConsumer glow = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.RBMK_CHERENKOV);
            ps.pushPose();
            ps.translate(0.5, 0.75, 0.5);
            Matrix4f m = ps.last().pose();
            for (float j = 0; j <= height; j += 0.25f) {
                RenderHooks.vertexColor(glow, m, -0.5f, j, -0.5f, 102, 230, 255, 25);
                RenderHooks.vertexColor(glow, m, -0.5f, j, 0.5f, 102, 230, 255, 25);
                RenderHooks.vertexColor(glow, m, 0.5f, j, 0.5f, 102, 230, 255, 25);
                RenderHooks.vertexColor(glow, m, 0.5f, j, -0.5f, 102, 230, 255, 25);
            }
            ps.popPose();
        }
    }

    // ── RBMK-Kranpult ────────────────────────────────────────────────────────────────────

    private static final String CRANE_CONSOLE_OBJ = "models/rbmk/models/crane_console.obj";
    private static final ResourceLocation DESK_SPRITE = rl("block/machine/crane_console");
    private static final ObjAtlasMesh CC_DESK = ObjAtlasMesh.groups(CRANE_CONSOLE_OBJ, DESK_SPRITE, "hbm_m/crane_console/desk", "Console_Coonsole");
    private static final ObjAtlasMesh CC_JOYSTICK = ObjAtlasMesh.groups(CRANE_CONSOLE_OBJ, DESK_SPRITE, "hbm_m/crane_console/joystick", "Joystick");
    private static final ObjAtlasMesh CC_METER1 = ObjAtlasMesh.groups(CRANE_CONSOLE_OBJ, DESK_SPRITE, "hbm_m/crane_console/meter1", "Meter1");
    private static final ObjAtlasMesh CC_METER2 = ObjAtlasMesh.groups(CRANE_CONSOLE_OBJ, DESK_SPRITE, "hbm_m/crane_console/meter2", "Meter2");

    private static float craneAngle(Direction facing) {
        return switch (facing) {
            case NORTH -> 90f;
            case WEST -> 180f;
            case SOUTH -> 270f;
            default -> 0f;
        };
    }

    private static boolean dial(PoseStack ps, double z, double value) {
        ps.translate(0, 1.25, z);
        double jitter = Math.sin(System.currentTimeMillis() * 0.01 % 360) * 180 / Math.PI * 0.05;
        ps.mulPose(Axis.XP.rotationDegrees((float) (jitter + 135 - 270 * value)));
        ps.translate(0, -1.25, -z);
        return true;
    }

    private static void registerCraneConsole() {
        MachineSpecBuilder<RBMKCraneConsoleBlockEntity> b = MachineRenderers.machine("hbm_m/rbmk_crane_console", ModBlockEntities.RBMK_CRANE_CONSOLE_BE.get(), RBMKCraneConsoleBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    rotY(a, craneAngle(be.facing));
                    a.translate(0.5, 0, 0);
                });
        mesh(b, "desk", CC_DESK);
        mesh(b, "joystick", (be, f, t, ps) -> {
            ps.translate(0.75, 1, 0);
            ps.mulPose(Axis.ZP.rotationDegrees((float) Mth.lerp(f, be.lastTiltFront, be.tiltFront)));
            ps.mulPose(Axis.XP.rotationDegrees((float) Mth.lerp(f, be.lastTiltLeft, be.tiltLeft)));
            ps.translate(-0.75, -1.015, 0);
            return true;
        }, CC_JOYSTICK);
        mesh(b, "meter1", (be, f, t, ps) -> dial(ps, 0.75, be.loadedHeat), CC_METER1);
        mesh(b, "meter2", (be, f, t, ps) -> dial(ps, 0.25, be.loadedEnrichment), CC_METER2);
        b.hook((be, pt, ps, buf, light, overlay, api) -> {
            Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(CRANE_CONSOLE_OBJ);
            TextureAtlasSprite deskTex = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/crane_console");
            TextureAtlasSprite craneTex = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/rbmk_crane");
            int full = LightTexture.FULL_BRIGHT;
            // Lampen (Farbe, vollhell) im Pultsystem
            float[] lamp1 = be.isCraneLoading() ? new float[] { 0.8f, 0.8f, 0f }
                    : be.hasItemLoaded() ? new float[] { 0f, 1f, 0f } : new float[] { 0f, 0.1f, 0f };
            cranePart(ps, buf, obj, "Lamp1", deskTex, lamp1[0], lamp1[1], lamp1[2], full, overlay);
            boolean valid = be.getLevel() != null && be.getLoadableAtPos(be.getLevel()) != null;
            cranePart(ps, buf, obj, "Lamp2", deskTex, valid ? 0f : 1f, valid ? 1f : 0f, 0f, full, overlay);
            if (!be.setUpCrane) return;
            // Kranbruecke: zurueck ins Blocksystem (Pultdrehung aufheben), dann 1:1 der alte BER
            float angle = craneAngle(be.facing);
            ps.pushPose();
            ps.translate(-0.5, 0, 0);
            ps.mulPose(Axis.YP.rotationDegrees(-angle));
            ps.translate(-0.5, 0, -0.5);
            ps.translate(0.5, -1, 0.5);
            var pos = be.getBlockPos();
            ps.translate(be.center.getX() - pos.getX(), be.center.getY() - pos.getY() + 1, be.center.getZ() - pos.getZ());
            ps.mulPose(Axis.YP.rotationDegrees(angle));
            double posX = Mth.lerp(pt, be.lastPosFront, be.posFront);
            double posZ = Mth.lerp(pt, be.lastPosLeft, be.posLeft);
            ps.translate(-posX, 0, posZ);
            ps.mulPose(Axis.YP.rotationDegrees(be.craneRotationOffset));
            Map<String, List<float[]>> crane = RBMKColumnRenderer.getObj("models/rbmk/models/crane.obj");
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(-be.craneRotationOffset));
            int girderSpan;
            switch (be.craneRotationOffset) {
                case 90 -> {
                    girderSpan = be.spanL + be.spanR + 1;
                    ps.translate(0, 0, -posZ - be.spanR);
                }
                case 180 -> {
                    girderSpan = be.spanF + be.spanB + 1;
                    ps.translate(posX - be.spanF, 0, 0);
                }
                case 270 -> {
                    girderSpan = be.spanL + be.spanR + 1;
                    ps.translate(0, 0, -posZ + be.spanL);
                }
                default -> {
                    girderSpan = be.spanF + be.spanB + 1;
                    ps.translate(posX + be.spanB, 0, 0);
                }
            }
            ps.mulPose(Axis.YP.rotationDegrees(be.craneRotationOffset));
            for (int i = 0; i < girderSpan; i++) {
                cranePart(ps, buf, crane, "Girder", craneTex, 1f, 1f, 1f, light, overlay);
                ps.translate(-1, 0, 0);
            }
            ps.popPose();
            cranePart(ps, buf, crane, "Main", craneTex, 1f, 1f, 1f, light, overlay);
            ps.pushPose();
            for (int i = 0; i < be.height - 6; i++) {
                cranePart(ps, buf, crane, "Tube", craneTex, 1f, 1f, 1f, light, overlay);
                ps.translate(0, 1, 0);
            }
            ps.translate(0, -1, 0);
            cranePart(ps, buf, crane, "Carriage", craneTex, 1f, 1f, 1f, light, overlay);
            ps.popPose();
            ps.translate(0, -3.25 * (1 - Mth.lerp(pt, be.lastProgress, be.progress)), 0);
            cranePart(ps, buf, crane, "Lift", craneTex, 1f, 1f, 1f, light, overlay);
            ps.popPose();
        }).viewDistance(128).register();
    }

    private static void cranePart(PoseStack ps, MultiBufferSource buf, Map<String, List<float[]>> obj, String group,
                                  TextureAtlasSprite sprite, float r, float g, float b, int light, int overlay) {
        List<float[]> mesh = obj.get(group);
        if (mesh == null) return;
        RBMKColumnRenderer.renderObjGroup(buf.getBuffer(RenderType.solid()), ps.last().pose(), mesh, sprite, r, g, b, light, overlay);
    }

    // ── weitere Maschinen ────────────────────────────────────────────────────────────────

    private static final ObjAtlasMesh REFUELER = p(RefuelerRenderer.MODEL, RefuelerRenderer.TEX, "refueler/fueler", "Fueler");
    private static final ObjAtlasMesh[] SOLAR_BOILER = new ObjAtlasMesh[4];
    private static final ObjAtlasMesh SOLIDIFIER = p(SolidifierRenderer.SOLIDIFIER, SolidifierRenderer.SOLIDIFIER_TEX, "solidifier/main", "Main");
    private static final ObjAtlasMesh LIQUEFACTOR = p(SolidifierRenderer.LIQUEFACTOR, SolidifierRenderer.LIQUEFACTOR_TEX, "liquefactor/main", "Main");
    private static final ObjAtlasMesh STRAND_CASTER = p(StrandCasterRenderer.MODEL, StrandCasterRenderer.TEX, "strand_caster/caster", "caster");
    private static final ObjAtlasMesh SUPERCOMPUTER = p(SuperComputerRenderer.MODEL, SuperComputerRenderer.TEX, "supercomputer/computer", "Computer");
    private static final ObjAtlasMesh TESLA = ObjAtlasMesh.all(TeslaRenderer.MODEL, TeslaRenderer.TEX, "hbm_m/tesla");
    private static final ObjAtlasMesh BIGASS = ObjAtlasMesh.all(TankRenderers.BigAssTank.MODEL, TankRenderers.BigAssTank.TEX, "hbm_m/bigasstank");
    private static final ObjAtlasMesh ORBUS = ObjAtlasMesh.all(TankRenderers.Orbus.MODEL, TankRenderers.Orbus.TEX, "hbm_m/orbus");
    private static final ObjAtlasMesh PISTON_FRAME = p(PistonInserterRenderer.MODEL, PistonInserterRenderer.TEX, "piston_inserter/frame", "Frame");
    private static final ObjAtlasMesh PISTON_PISTON = p(PistonInserterRenderer.MODEL, PistonInserterRenderer.TEX, "piston_inserter/piston", "Piston");
    /** Faesser: Anschlussstutzen je Seite (eingebackene Drehung) und Fasstextur (Blockatlas). */
    private static final Map<String, ObjAtlasMesh[]> BARREL_CONNECTORS = new HashMap<>();
    /** Leitungsanker: Anker je FACING (eingebackene Drehung), je Ankertextur. */
    private static final Map<ResourceLocation, ObjAtlasMesh[]> ANCHORS = new HashMap<>();

    static {
        for (int i = 0; i < 4; i++) {
            final int f = i;
            SOLAR_BOILER[i] = ObjAtlasMesh.of("hbm_m/solar_boiler/base" + i, b -> {
                b.pose.mulPose(Axis.YP.rotationDegrees(90));
                if (f > 0) b.pose.mulPose(Axis.YP.rotationDegrees(f * 90));
                b.obj(SolarBoilerRenderer.MODEL, SolarBoilerRenderer.TEX, "Base");
            });
        }
    }

    private static ObjAtlasMesh[] barrelConnectors(String tex) {
        return BARREL_CONNECTORS.computeIfAbsent(tex, t -> {
            ResourceLocation texture = rl("textures/block/" + t + ".png");
            float[] yaw = { 0F, 180F, 90F, -90F }; // Ost, West, Nord, Sued wie im alten BER
            ObjAtlasMesh[] out = new ObjAtlasMesh[4];
            for (int i = 0; i < 4; i++) {
                final float y = yaw[i];
                out[i] = ObjAtlasMesh.of("hbm_m/barrel/" + t + "/connector" + i, b -> {
                    if (y != 0F) b.pose.mulPose(Axis.YP.rotationDegrees(y));
                    b.pose.translate(0.0F, -0.5F, 0.0F);
                    b.obj(TankRenderers.Barrel.MODEL, texture, "Connector");
                });
            }
            return out;
        });
    }

    private static ObjAtlasMesh anchor(ResourceLocation texture, Direction facing) {
        ObjAtlasMesh[] set = ANCHORS.computeIfAbsent(texture, t -> {
            ObjAtlasMesh[] out = new ObjAtlasMesh[6];
            for (Direction d : Direction.values()) {
                out[d.ordinal()] = ObjAtlasMesh.of("hbm_m/pipe_anchor/" + ObjAtlasMesh.spriteIdOf(t).getPath().replace('/', '_') + "/" + d.getName(), b -> {
                    switch (d) {
                        case DOWN -> b.pose.mulPose(Axis.XP.rotationDegrees(180));
                        case UP -> { }
                        case NORTH -> { b.pose.mulPose(Axis.XP.rotationDegrees(90)); b.pose.mulPose(Axis.ZP.rotationDegrees(180)); }
                        case SOUTH -> b.pose.mulPose(Axis.XP.rotationDegrees(90));
                        case WEST -> { b.pose.mulPose(Axis.XP.rotationDegrees(90)); b.pose.mulPose(Axis.ZP.rotationDegrees(90)); }
                        case EAST -> { b.pose.mulPose(Axis.XP.rotationDegrees(90)); b.pose.mulPose(Axis.ZP.rotationDegrees(270)); }
                    }
                    b.pose.translate(0, -0.5F, 0);
                    b.obj(PipeAnchorRenderer.MODEL, t, "Anchor");
                });
            }
            return out;
        });
        return set[facing.ordinal()];
    }

    public static void register() {
        // RBMK-Saeulen (alle Typen, die den gemeinsamen Saeulen-BER nutzen)
        registerColumn("hbm_m/rbmk_rod", ModBlockEntities.RBMK_ROD_BE.get(), com.hbm_m.blockentity.machines.rbmk.RBMKRodBlockEntity.class);
        registerColumn("hbm_m/rbmk_blank", ModBlockEntities.RBMK_BLANK_BE.get(), com.hbm_m.blockentity.machines.rbmk.RBMKBlankBlockEntity.class);
        registerColumn("hbm_m/rbmk_absorber", ModBlockEntities.RBMK_ABSORBER_BE.get(), com.hbm_m.blockentity.machines.rbmk.RBMKAbsorberBlockEntity.class);
        registerColumn("hbm_m/rbmk_reflector", ModBlockEntities.RBMK_REFLECTOR_BE.get(), com.hbm_m.blockentity.machines.rbmk.RBMKReflectorBlockEntity.class);
        registerColumn("hbm_m/rbmk_cooler", ModBlockEntities.RBMK_COOLER_BE.get(), com.hbm_m.blockentity.machines.rbmk.RBMKCoolerBlockEntity.class);
        registerColumn("hbm_m/rbmk_boiler", ModBlockEntities.RBMK_BOILER_BE.get(), com.hbm_m.blockentity.machines.rbmk.RBMKBoilerBlockEntity.class);
        registerColumn("hbm_m/rbmk_heater", ModBlockEntities.RBMK_HEATER_BE.get(), com.hbm_m.blockentity.machines.rbmk.RBMKHeaterBlockEntity.class);
        registerColumn("hbm_m/rbmk_moderator", ModBlockEntities.RBMK_MODERATOR_BE.get(), com.hbm_m.blockentity.machines.rbmk.RBMKModeratorBlockEntity.class);
        registerColumn("hbm_m/rbmk_outgasser", ModBlockEntities.RBMK_OUTGASSER_BE.get(), com.hbm_m.blockentity.machines.rbmk.RBMKOutgasserBlockEntity.class);
        registerColumn("hbm_m/rbmk_storage", ModBlockEntities.RBMK_STORAGE_BE.get(), com.hbm_m.blockentity.machines.rbmk.RBMKStorageBlockEntity.class);
        registerColumn("hbm_m/rbmk_control", ModBlockEntities.RBMK_CONTROL_BE.get(), RBMKControlManualBlockEntity.class);
        registerColumn("hbm_m/rbmk_control_auto", ModBlockEntities.RBMK_CONTROL_AUTO_BE.get(), RBMKControlAutoBlockEntity.class);
        registerColumn("hbm_m/rbmk_panel", ModBlockEntities.RBMK_PANEL_BE.get(), com.hbm_m.blockentity.machines.rbmk.RBMKPanelBlockEntity.class);

        registerCraneConsole();

        // RenderRefueler: Fluessigkeit (additiv, abgeschnitten) immediate
        mesh(MachineRenderers.machine("hbm_m/refueler", ModBlockEntities.REFUELER_BE.get(), RefuelerBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    rotY(a, 90);
                    switch (be.getBlockState().getValue(RefuelerBlock.FACING)) {
                        case WEST -> rotY(a, 90);
                        case SOUTH -> rotY(a, 180);
                        case EAST -> rotY(a, 270);
                        default -> { }
                    }
                }), "fueler", REFUELER)
                .hook((refueler, interp, ps, buf, light, overlay, api) -> {
                    double fillLevel = refueler.prevFillLevel + (refueler.fillLevel - refueler.prevFillLevel) * interp;
                    int color = FluidType.forFluid(refueler.tank.getTankType()).getColor();
                    RefuelerRenderer.MODEL.renderPartColorClippedY("Fluid", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.ADDITIVE_TRIANGLES),
                            ((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 0.75F,
                            (float) ((1 - fillLevel) * -0.625), 0.125F);
                })
                .viewDistance(64).register();

        // RenderSolarBoiler: Gehaeuse mit eingebackener Drehung (die Strahlen liegen im ungedrehten System)
        select(MachineRenderers.machine("hbm_m/solar_boiler", ModBlockEntities.SOLAR_BOILER_BE.get(), MachineSolarBoilerBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5D, 0D, 0.5D)), "base", be -> SOLAR_BOILER[switch (be.getBlockState().getValue(MachineSolarBoilerBlock.FACING)) {
                    case WEST -> 1;
                    case SOUTH -> 2;
                    case EAST -> 3;
                    default -> 0;
                }])
                .hook((boiler, interp, ps, buf, light, overlay, api) -> solarBeams(boiler, ps, buf))
                .viewDistance(256).register();

        // RenderSolidifier / RenderLiquefactor: Fluessigkeit (Farbe, Pegel-Skalierung) und Glas immediate
        mesh(MachineRenderers.machine("hbm_m/solidifier", ModBlockEntities.SOLIDIFIER_BE.get(), MachineSolidifierBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    if (be.getBlockState().hasProperty(DummyableMachineBlock.FACING)) {
                        switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                            case NORTH -> rotY(a, 90);
                            case WEST -> rotY(a, 180);
                            case SOUTH -> rotY(a, 270);
                            default -> { }
                        }
                    }
                }), "main", SOLIDIFIER)
                .hook((te, f, ps, buf, light, overlay, api) -> solidifierFluid(SolidifierRenderer.SOLIDIFIER, 1.25, te.getTank(), ps, buf))
                .viewDistance(256).register();
        mesh(MachineRenderers.machine("hbm_m/liquefactor", ModBlockEntities.LIQUEFACTOR_BE.get(), MachineLiquefactorBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5D, 0D, 0.5D)), "main", LIQUEFACTOR)
                .hook((te, f, ps, buf, light, overlay, api) -> solidifierFluid(SolidifierRenderer.LIQUEFACTOR, 1, te.getTank(), ps, buf))
                .viewDistance(256).register();

        // RenderStrandCaster: Platte und Schmelze (Farbe, vollhell) immediate
        mesh(MachineRenderers.machine("hbm_m/strand_caster", ModBlockEntities.STRAND_CASTER_BE.get(), MachineStrandCasterBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case WEST -> rotY(a, 90);
                        case SOUTH -> rotY(a, 180);
                        case EAST -> rotY(a, 270);
                        default -> { }
                    }
                    a.translate(0.5D, 0D, 0.5D);
                    rotY(a, 180);
                }), "caster", STRAND_CASTER)
                .hook((caster, f, ps, buf, light, overlay, api) -> strandCasterMelt(caster, ps, buf))
                .viewDistance(256).register();

        // RenderSuperComputer: Lauflichter (UV-Lauf, vollhell) immediate
        mesh(MachineRenderers.machine("hbm_m/supercomputer", ModBlockEntities.SUPERCOMPUTER_BE.get(), MachineSuperComputerBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case NORTH -> rotY(a, 180);
                        case WEST -> rotY(a, 270);
                        case EAST -> rotY(a, 90);
                        default -> { }
                    }
                }), "computer", SUPERCOMPUTER)
                .hook((computer, interp, ps, buf, light, overlay, api) -> {
                    float c = computer.didProcess ? 1F : 0F;
                    float scroll = computer.getLevel() != null ? (computer.getLevel().getGameTime() % 20 + interp) / 20F : 0F;
                    SuperComputerRenderer.MODEL.renderPartShiftedU("Lights", ps, buf.getBuffer(RenderType.entityCutoutNoCull(SuperComputerRenderer.SCAN_TEX)),
                            LightTexture.FULL_BRIGHT, c, c, c, -scroll);
                })
                .viewDistance(256).register();

        // RenderTesla: Blitze immediate
        mesh(MachineRenderers.machine("hbm_m/tesla", ModBlockEntities.TESLA_BE.get(), TeslaBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    rotY(a, 180);
                }), "body", TESLA)
                .hook((tesla, f, ps, buf, light, overlay, api) -> {
                    double offset = 1.75D;
                    double sx = tesla.getBlockPos().getX() + 0.5D;
                    double sy = tesla.getBlockPos().getY() + offset;
                    double sz = tesla.getBlockPos().getZ() + 0.5D;
                    ps.pushPose();
                    ps.translate(0.0D, offset, 0.0D);
                    long time = tesla.getLevel() != null ? tesla.getLevel().getGameTime() : 0L;
                    for (Vec3 target : tesla.getTargets()) {
                        double length = Math.sqrt(Math.pow(target.x - sx, 2) + Math.pow(target.y - sy, 2) + Math.pow(target.z - sz, 2));
                        BeamPronter.prontBeam(ps, buf, new Vec3(-target.x + sx, target.y - sy, -target.z + sz),
                                BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x404040, 0x404040,
                                (int) time % 1000 + 1, (int) (length * 5), 0.125F, 2, 0.03125F, BeamPronter.forkAlpha());
                    }
                    ps.popPose();
                })
                .viewDistance(64).register();

        // RenderBigAssTank: gekippt, Rauten und Pegelfenster immediate
        mesh(MachineRenderers.machine("hbm_m/bigasstank", ModBlockEntities.BIGASSTANK_BE.get(), MachineBigAssTankBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    if (be.tilted) {
                        a.translate(0, -1, 0);
                        a.rotate(10, 0, 0, 1);
                        a.rotate(5, 0, 1, 0);
                    }
                    switch (ObjBerHelper.meta(be)) {
                        case 2 -> rotY(a, 270);
                        case 3 -> rotY(a, 90);
                        case 5 -> rotY(a, 180);
                        default -> { }
                    }
                }), "body", BIGASS)
                .hook((bat, interp, ps, buf, light, overlay, api) -> bigAssExtras(bat, interp, ps, buf, light))
                .viewDistance(256).register();

        // RenderOrbus: Versatz nach Metadate; Kugel (Farbe, Skalierung) und Strahlen immediate
        mesh(MachineRenderers.machine("hbm_m/orbus", ModBlockEntities.ORBUS_BE.get(), OrbusBlockEntity.class)
                .blockTransform((be, a) -> {
                    switch (ObjBerHelper.meta(be)) {
                        case 2 -> a.translate(1F, 0F, 1F);
                        case 4 -> a.translate(1F, 0F, 0F);
                        case 5 -> a.translate(0F, 0F, 1F);
                        default -> { }
                    }
                }), "body", ORBUS)
                .hook((orbus, interp, ps, buf, light, overlay, api) -> orbusExtras(orbus, interp, ps, buf))
                .viewDistance(256).register();

        // RenderFluidBarrel: Stutzen zu passenden Nachbarn (nur mit Sorte), Rauten immediate
        registerBarrel("hbm_m/barrel_iron", ModBlockEntities.BARREL_IRON_BE.get(), BarrelIronBlockEntity.class);
        registerBarrel("hbm_m/barrel_steel", ModBlockEntities.BARREL_STEEL_BE.get(), BarrelSteelBlockEntity.class);
        registerBarrel("hbm_m/barrel_tcalloy", ModBlockEntities.BARREL_TCALLOY_BE.get(), BarrelTcalloyBlockEntity.class);
        registerBarrel("hbm_m/barrel_corroded", ModBlockEntities.BARREL_CORRODED_BE.get(), BarrelCorrodedBlockEntity.class);
        registerBarrel("hbm_m/barrel_plastic", ModBlockEntities.BARREL_PLASTIC_BE.get(), BarrelPlasticBlockEntity.class);
        registerBarrel("hbm_m/barrel_antimatter", ModBlockEntities.BARREL_ANTIMATTER_BE.get(), BarrelAntimatterBlockEntity.class);

        // RenderPipeAnchor (Fluid / Abgas / Rohrpost): Anker; Leitungen (eingefaerbt, laengenskaliert) immediate
        registerAnchor("hbm_m/pipe_anchor", ModBlockEntities.PIPE_ANCHOR.get(), PipeAnchorBlockEntity.class, PipeAnchorRenderer.TEX);
        registerAnchor("hbm_m/pipe_anchor_exhaust", ModBlockEntities.PIPE_ANCHOR_EXHAUST.get(), ExhaustPipeAnchorBlockEntity.class, PipeAnchorRenderer.TEX_EXHAUST);
        registerAnchor("hbm_m/pipe_anchor_pneumatic", ModBlockEntities.PIPE_ANCHOR_PNEUMATIC.get(), PneumaticPipeAnchorBlockEntity.class, PipeAnchorRenderer.TEX_PNEUMATIC);

        // RenderPistonInserter: Kolben faehrt; Gegenstand immediate
        MachineSpecBuilder<PistonInserterBlockEntity> pi = MachineRenderers.machine("hbm_m/piston_inserter", ModBlockEntities.PISTON_INSERTER_BE.get(), PistonInserterBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0.5, 0.5);
                    switch (be.getBlockState().getValue(PistonInserterBlock.FACING)) {
                        case DOWN -> a.rotate(180, 1, 0, 0);
                        case UP -> { }
                        case NORTH -> { a.rotate(-90, 1, 0, 0); rotY(a, 180); }
                        case WEST -> { a.rotate(90, 0, 0, 1); rotY(a, -90); }
                        case SOUTH -> a.rotate(90, 1, 0, 0);
                        case EAST -> { a.rotate(-90, 0, 0, 1); rotY(a, 90); }
                    }
                    a.translate(0D, -0.5, 0D);
                });
        mesh(pi, "frame", PISTON_FRAME);
        idleStatic(pi, "piston", RaptorPathP6::pistonExtend, be -> be.lastExtend != 0 || be.renderExtend != 0, PISTON_PISTON);
        pi.hook((piston, interp, ps, buffers, light, overlay, api) -> {
            if (piston.slot.isEmpty()) return;
            ps.pushPose();
            pistonExtend(piston, interp, 0L, ps);
            ItemStack stack = piston.slot.copyWithCount(1);
            if (stack.getItem() instanceof BlockItem) {
                ps.translate(0.0D, 1.125D, 0.0D);
            } else {
                ps.translate(0.0D, 1.0625D, 0.1D);
                ps.mulPose(Axis.XN.rotationDegrees(90));
            }
            ps.scale(0.5F, 0.5F, 0.5F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, buffers, piston.getLevel(), 0);
            ps.popPose();
        }).viewDistance(64).register();
    }

    private static boolean pistonExtend(PistonInserterBlockEntity piston, float interp, long t, PoseStack ps) {
        double e = (piston.lastExtend + (piston.renderExtend - piston.lastExtend) * interp) / (double) PistonInserterBlockEntity.maxExtend;
        ps.translate(0, e * 0.9375D, 0);
        return true;
    }

    private static <T extends MachineFluidTankBlockEntity> void registerBarrel(String id, BlockEntityType<T> type, Class<T> cls) {
        MachineSpecBuilder<T> b = MachineRenderers.machine(id, type, cls)
                .blockTransform((be, a) -> a.translate(0.5, 0.5, 0.5));
        // Original-Reihenfolge Ost, West, Nord, Sued
        Direction[] dirs = { Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH };
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            final Direction dir = dirs[i];
            select(b, "connector" + i, be -> {
                Fluid fluid = be.getFluidTank().getTankType();
                Level level = be.getLevel();
                if (TankRenderers.isNone(fluid) || level == null) return null;
                if (!TankRenderers.Barrel.canConnectFluid(level, be.getBlockPos().relative(dir), dir, fluid)) return null;
                return barrelConnectors(barrelTexture(be.getBlockState().getBlock()))[idx];
            });
        }
        b.hook((barrel, f, ps, buf, light, overlay, api) -> {
            Fluid fluid = barrel.getFluidTank().getTankType();
            if (TankRenderers.isNone(fluid) || barrel.getLevel() == null) return;
            FluidType ft = FluidType.forFluid(fluid);
            ps.pushPose();
            for (int j = 0; j < 4; j++) {
                ps.pushPose();
                ps.translate(0.4, 0.30, -0.24);
                ps.scale(1.0F, 0.25F, 0.25F);
                DiamondPronter.pront(ps, buf, ft.poison, ft.flammability, ft.reactivity, ft.symbol, light, OverlayTexture.NO_OVERLAY);
                ps.popPose();
                ps.mulPose(Axis.YP.rotationDegrees(90));
            }
            ps.popPose();
        }).viewDistance(64).register();
    }

    private static String barrelTexture(Block block) {
        return block == ModBlocks.BARREL_PLASTIC.get() ? "barrel_plastic"
                : block == ModBlocks.BARREL_STEEL.get() ? "barrel_steel"
                : block == ModBlocks.BARREL_TCALLOY.get() ? "barrel_tcalloy"
                : block == ModBlocks.BARREL_ANTIMATTER.get() ? "barrel_antimatter"
                : block == ModBlocks.BARREL_CORRODED.get() ? "barrel_corroded" : "barrel_iron";
    }

    private static <T extends BlockEntity & IPipelineBase> void registerAnchor(String id, BlockEntityType<T> type, Class<T> cls, ResourceLocation tex) {
        select(MachineRenderers.machine(id, type, cls)
                .blockTransform((be, a) -> a.translate(0.5, 0.5, 0.5)), "anchor", be -> anchor(tex, be.getBlockState().getValue(BlockStateProperties.FACING)))
                .hook((te, pt, ps, buf, light, overlay, api) -> anchorPipes(te, tex, ps, buf, light))
                .viewDistance(256).register();
    }

    /** Leitungen zwischen Ankern aus {@code PipeAnchorRenderer} (laengenskaliert und eingefaerbt, unveraendert). */
    private static <T extends BlockEntity & IPipelineBase> void anchorPipes(T te, ResourceLocation texture, PoseStack ps, MultiBufferSource buf, int light) {
        if (te.getLevel() == null) return;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(texture));
        for (BlockPos p : te.getConnected()) {
            BlockEntity tile = te.getLevel().getBlockEntity(p);
            if (!(tile instanceof IPipelineBase other) || te.getNetworkType() != other.getNetworkType()) continue;
            Vec3 a = te.getConnectionPoint(), b = other.getConnectionPoint();
            if (!PipeAnchorRenderer.isDominant(a, b)) continue;
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
            Fluid f = te instanceof PipeAnchorBlockEntity fa ? (fa.getFluidType() == Fluids.EMPTY ? ModFluids.NONE.getSource() : fa.getFluidType()) : null;
            int c = f == null ? 0xFFFFFF : HbmFluidRegistry.getTintColor(f);
            int r = c >> 16 & 255, g = c >> 8 & 255, bl = c & 255;
            if (f != null) { r = (int) (r + (255 - r) * 0.25D); g = (int) (g + (255 - g) * 0.25D); bl = (int) (bl + (255 - bl) * 0.25D); }
            PipeAnchorRenderer.MODEL.renderPartTinted("Pipe", ps, vc, light, r / 255F, g / 255F, bl / 255F);
            ps.popPose();

            ps.pushPose();
            ps.translate(0, length / 2D - 1.5, 0);
            PipeAnchorRenderer.MODEL.renderPart("Ring", ps, vc, light);
            ps.popPose();

            ps.popPose();
        }
    }

    /** Heliostat-Strahlen aus {@code SolarBoilerRenderer} (unveraendert). */
    private static void solarBeams(MachineSolarBoilerBlockEntity boiler, PoseStack ps, MultiBufferSource buf) {
        if (Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST) return;
        VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.RBMK_CHERENKOV);
        BlockPos bp = boiler.getBlockPos();
        int beamCount = 0;
        for (BlockPos co : boiler.secondary) {
            beamCount++;
            if (beamCount > com.hbm_m.config.ClientConfig.renderHeliostatBeamLimit) break;
            int dx = bp.getX() - co.getX();
            int dy = bp.getY() - co.getY();
            int dz = bp.getZ() - co.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float min = 0.005F;
            float max = 0.01F;
            ps.pushPose();
            ps.translate(-dx, -dy, -dz);
            double pitch = Math.toDegrees(-Math.asin((dy + 0.5) / dist)) + 90;
            double yaw = Math.toDegrees(-Math.atan2(dz, dx)) + 180;
            ps.translate(0, 1, 0);
            ps.mulPose(Axis.YP.rotationDegrees((float) yaw));
            ps.mulPose(Axis.ZP.rotationDegrees((float) pitch));
            ps.translate(0, -1, 0);
            Matrix4f m = ps.last().pose();
            int aMax = (int) (max * 255F);
            int aMin = (int) (min * 255F);
            float d = (float) dist;
            sv(vc, m, 0.5F, 1.0625F, 0.5F, aMax);
            sv(vc, m, 0.5F, 1.0625F, -0.5F, aMax);
            sv(vc, m, 0.5F, d, -0.5F, aMin);
            sv(vc, m, 0.5F, d, 0.5F, aMin);
            sv(vc, m, -0.5F, 1.0625F, 0.5F, aMax);
            sv(vc, m, -0.5F, 1.0625F, -0.5F, aMax);
            sv(vc, m, -0.5F, d, -0.5F, aMin);
            sv(vc, m, -0.5F, d, 0.5F, aMin);
            sv(vc, m, 0.5F, 1.0625F, 0.5F, aMax);
            sv(vc, m, -0.5F, 1.0625F, 0.5F, aMax);
            sv(vc, m, -0.5F, d, 0.5F, aMin);
            sv(vc, m, 0.5F, d, 0.5F, aMin);
            sv(vc, m, 0.5F, 1.0625F, -0.5F, aMax);
            sv(vc, m, -0.5F, 1.0625F, -0.5F, aMax);
            sv(vc, m, -0.5F, d, -0.5F, aMin);
            sv(vc, m, 0.5F, d, -0.5F, aMin);
            ps.popPose();
        }
    }

    private static void sv(VertexConsumer vc, Matrix4f m, float x, float y, float z, int a) {
        RenderHooks.vertexColor(vc, m, x, y, z, 255, 255, 255, a);
    }

    /** Fluessigkeit und Glas aus {@code SolidifierRenderer} (unveraendert). */
    private static void solidifierFluid(SimpleObjModel model, double pivot, FluidTank t, PoseStack ps, MultiBufferSource buf) {
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
    }

    /** Platte und Schmelzspiegel aus {@code StrandCasterRenderer} (unveraendert). */
    private static void strandCasterMelt(MachineStrandCasterBlockEntity caster, PoseStack ps, MultiBufferSource buf) {
        if (caster.amount == 0 || caster.getInstalledMold() == null || caster.type == null) return;
        float level = (float) (((double) caster.amount / (double) caster.getCapacity()) * 0.675D);
        double offset = ((double) caster.amount / (double) caster.getInstalledMold().getCost()) * 0.375D;
        int color = caster.type.moltenColor;
        float r = (color >> 16 & 0xFF) / 255F;
        float g = (color >> 8 & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;
        ps.pushPose();
        ps.translate(0, 0, Math.max(-offset + 3.4, 0));
        StrandCasterRenderer.MODEL.renderPartColor("plate", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), r, g, b, 1F);
        ps.popPose();
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(StrandCasterRenderer.LAVA));
        Matrix4f m = ps.last().pose();
        org.joml.Matrix3f n = ps.last().normal();
        float y = 2.3F + level;
        int full = 0xF000F0;
        //? if < 1.21.1 {
        vc.vertex(m, -0.9F, y, -0.999F).color(r, g, b, 1F).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, -0.9F, y, 0.999F).color(r, g, b, 1F).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, 0.9F, y, 0.999F).color(r, g, b, 1F).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, 0.9F, y, -0.999F).color(r, g, b, 1F).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
        //?} else {
        /*RenderHooks.normal(vc.addVertex(m, -0.9F, y, -0.999F).setColor(r, g, b, 1F).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full), n, 0, 1, 0);
        RenderHooks.normal(vc.addVertex(m, -0.9F, y, 0.999F).setColor(r, g, b, 1F).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full), n, 0, 1, 0);
        RenderHooks.normal(vc.addVertex(m, 0.9F, y, 0.999F).setColor(r, g, b, 1F).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full), n, 0, 1, 0);
        RenderHooks.normal(vc.addVertex(m, 0.9F, y, -0.999F).setColor(r, g, b, 1F).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full), n, 0, 1, 0);
        *///?}
    }

    /** Gefahrenrauten und Pegelfenster aus {@code TankRenderers.BigAssTank} (unveraendert). */
    private static void bigAssExtras(MachineFluidTankBlockEntity bat, float interp, PoseStack ps, MultiBufferSource buf, int light) {
        Fluid fluid = bat.getFluidTank().getTankType();
        if (!TankRenderers.isNone(fluid)) {
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
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(TankRenderers.tankTexture(bat)));
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
        tv(vc, m, -off, 1.75F, -0.25F, minU, 0, light);
        tv(vc, m, -off, 1.75F + height, -0.25F, minU, topV, light);
        tv(vc, m, -off, 1.75F + height, 0.25F, maxU, topV, light);
        tv(vc, m, -off, 1.75F, 0.25F, maxU, 0, light);
        tv(vc, m, off, 1.75F, -0.25F, maxU, 0, light);
        tv(vc, m, off, 1.75F + height, -0.25F, maxU, topV, light);
        tv(vc, m, off, 1.75F + height, 0.25F, minU, topV, light);
        tv(vc, m, off, 1.75F, 0.25F, minU, 0, light);
    }

    private static void tv(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float vv, int light) {
        RenderHooks.vertexFull(vc, m, x, y, z, 255, 255, 255, 255, u, vv, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
    }

    /** Schwebekugel und Strahlen aus {@code TankRenderers.Orbus} (unveraendert). */
    private static void orbusExtras(MachineFluidTankBlockEntity orbus, float interp, PoseStack ps, MultiBufferSource buf) {
        int fill = orbus.getFluidTank().getFill();
        if (fill <= 0) return;
        double scale = (double) fill / (double) Math.max(1, orbus.getFluidTank().getMaxFill());
        long time = orbus.getLevel() != null ? orbus.getLevel().getGameTime() : 0L;
        Fluid fluid = orbus.getFluidTank().getTankType();
        int c = TankRenderers.isNone(fluid) ? 0xFFFFFF : FluidType.forFluid(fluid).getColor();
        ps.pushPose();
        ps.translate(0, 2.5D + Math.sin(((time + interp) * 0.1D) % (Math.PI * 2D)) * 0.125 * scale, 0);
        ps.scale((float) scale, (float) scale, (float) scale);
        TankRenderers.Orbus.SPHERE_UV.renderAllColor(ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL),
                ((c & 0xff0000) >> 16) / 255F, ((c & 0x00ff00) >> 8) / 255F, (c & 0x0000ff) / 255F, 1F);
        ps.popPose();
        ps.pushPose();
        ps.translate(0, 1, 0);
        BeamPronter.prontBeam(ps, buf, new Vec3(0, 3, 0), BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.SOLID, 0x101020, 0x101020, 0, 1, 0F, 6, (float) scale * 0.5F, BeamPronter.forkAlpha());
        BeamPronter.prontBeam(ps, buf, new Vec3(0, 3, 0), BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x202060, 0x202060, (int) (time / 2) % 1000, 6, (float) scale, 2, 0.0625F * (float) scale, BeamPronter.forkAlpha());
        BeamPronter.prontBeam(ps, buf, new Vec3(0, 3, 0), BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x202060, 0x202060, (int) (time / 4) % 1000, 6, (float) scale, 2, 0.0625F * (float) scale, BeamPronter.forkAlpha());
        ps.popPose();
    }
}
