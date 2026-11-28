package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.block.machines.MachineRbmkConsoleBlock;
import com.hbm_m.block.machines.rbmk.RBMKCraneConsoleBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity;
import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity.RBMKColumnData;
import com.hbm_m.blockentity.machines.rbmk.RBMKAutoloaderBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKCraneConsoleBlockEntity;
import com.hbm_m.client.render.cache.RenderCacheManager;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;

/**
 * Nucleus renderers for RBMK Control Systems:
 * 1. RBMK Main Console: console body with live 15x15 reactor core status map,
 *    channel heat gradients, control rod markers, and 6 aggregate digital screens.
 * 2. RBMK Crane Console: control desk with dynamic joystick tilt, dial needles,
 *    and multi-block gantry hoist crane render hook traversing the reactor hall.
 * 3. RBMK Autoloader: static multiblock base with GPU translating fuel transfer
 *    sleeve/piston (0..4 blocks vertical travel).
 */
public final class MachineRbmkControlRenderer {

    private static final int GRID = MachineRbmkConsoleBlockEntity.GRID;
    private static final double SCREEN_X = -0.3725D;
    private static final double CELL = 0.125D;
    private static final double GRID_TOP = 3.625D;
    private static final double GRID_LEFT = CELL * 7;
    private static final double CELL_HALF = 0.0625D * 0.75D;

    private static TextureAtlasSprite whiteSprite;
    static {
        RenderCacheManager.register(reason -> whiteSprite = null);
    }

    private static TextureAtlasSprite whiteSprite() {
        if (whiteSprite == null) {
            whiteSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(ResourceLocation.withDefaultNamespace("block/white_concrete"));
        }
        return whiteSprite;
    }

    public static void register() {
        // ── RBMK Main Console ────────────────────────────────────────────────
        MachineRenderers.machine("rbmk_console",
                ModBlockEntities.RBMK_CONSOLE_BE.get(),
                MachineRbmkConsoleBlockEntity.class)
            .blockTransform((be, animator) -> {
                animator.translate(0.5, 0, 0.5);
                Direction facing = be.getBlockState().getValue(MachineRbmkConsoleBlock.FACING);
                float rotY = switch (facing) {
                    case SOUTH -> 270f;
                    case WEST  -> 180f;
                    case EAST  -> 0f;
                    default    -> 90f;
                };
                animator.rotate(rotY, 0, 1, 0);
                animator.translate(0.5, 0, 0);
                animator.translate(-0.5, 0, -0.5);
            })
            .part("Cube")
            .hook(new MachineRenderHook<MachineRbmkConsoleBlockEntity>() {
                @Override
                public void render(MachineRbmkConsoleBlockEntity be, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    VertexConsumer vc = buf.getBuffer(RenderType.solid());
                    Matrix4f m = ps.last().pose();
                    TextureAtlasSprite sprite = whiteSprite();

                    for (int i = 0; i < MachineRbmkConsoleBlockEntity.AREA; i++) {
                        RBMKColumnData col = be.columns[i];
                        if (col == null) continue;

                        double kx = SCREEN_X;
                        double ky = -(i / GRID) * CELL + GRID_TOP;
                        double kz = -(i % GRID) * CELL + GRID_LEFT;

                        float r, g, b;
                        if (col.data.contains("color") && col.data.getShort("color") >= 0) {
                            int rgb = switch (col.data.getShort("color")) {
                                case 0  -> 0xFF0000;
                                case 1  -> 0xFFFF00;
                                case 2  -> 0x008000;
                                case 3  -> 0x0000FF;
                                case 4  -> 0x8000FF;
                                default -> 0xFFFFFF;
                            };
                            r = ((rgb >> 16) & 0xFF) / 255f;
                            g = ((rgb >> 8) & 0xFF) / 255f;
                            b = (rgb & 0xFF) / 255f;
                        } else {
                            double maxHeat = col.data.getDouble("maxHeat");
                            double heat = maxHeat > 0 ? col.data.getDouble("heat") / maxHeat : 0;
                            double base = 0.65 + (i % 2) * 0.05;
                            r = (float) (base + (1 - base) * heat);
                            g = (float) base;
                            b = (float) base;
                        }

                        if (col.data.getInt("indicator") > 0) {
                            r = 1f; g = 1f; b = 0f;
                        }

                        quad(vc, m, (float) kx, (float) (ky + CELL_HALF), (float) (kz - CELL_HALF),
                                    (float) kx, (float) (ky + CELL_HALF), (float) (kz + CELL_HALF),
                                    (float) kx, (float) (ky - CELL_HALF), (float) (kz + CELL_HALF),
                                    (float) kx, (float) (ky - CELL_HALF), (float) (kz - CELL_HALF),
                                1, 0, 0, sprite, r, g, b, light, overlay);

                        switch (col.type) {
                            // The original's console renderer shares the fuel dot between FUEL and
                            // FUEL_SIM (RenderRBMKConsole :82-83).
                            case FUEL, FUEL_SIM -> {
                                float e = (float) Math.min(Math.max(col.data.getDouble("enrichment"), 0), 1);
                                drawDot(vc, m, kx + 0.01, ky, kz, 0F, 0.25F + e * 0.75F, 0F, sprite, light, overlay);
                            }
                            case CONTROL -> {
                                float l = (float) Math.min(Math.max(col.data.getDouble("level"), 0), 1);
                                boolean auto = col.data.getBoolean("auto");
                                drawDot(vc, m, kx + 0.01, ky, kz, l, auto ? 0F : l, auto ? l : 0F, sprite, light, overlay);
                            }
                            default -> { }
                        }
                    }

                    renderScreenText(be, ps, buf, light);
                }

                @Override
                public boolean mayDrawContent(MachineRbmkConsoleBlockEntity be) {
                    return true;
                }
            })
            .viewDistance(64)
            .itemParts("Cube")
            .register();

        // ── RBMK Crane Console ───────────────────────────────────────────────
        MachineRenderers.machine("rbmk_crane_console",
                ModBlockEntities.RBMK_CRANE_CONSOLE_BE.get(),
                RBMKCraneConsoleBlockEntity.class)
            .blockTransform((be, animator) -> {
                animator.translate(0.5, 0, 0.5);
                Direction facing = be.getBlockState().hasProperty(RBMKCraneConsoleBlock.FACING)
                        ? be.getBlockState().getValue(RBMKCraneConsoleBlock.FACING)
                        : be.facing;
                float angle = switch (facing) {
                    case NORTH -> 90f;
                    case WEST  -> 180f;
                    case SOUTH -> 270f;
                    default    -> 0f;
                };
                animator.rotate(angle, 0, 1, 0);
                animator.translate(0.5, 0, 0);
                animator.translate(-0.5, 0, -0.5);
            })
            .part("Console_Coonsole")
            // Desk widgets moved off baked parts onto OBJ-quad dynamic parts so their
            // animator chains can replicate the original exactly (all groups of the same
            // crane_console.obj the baked body uses, so coordinates line up):
            // - Joystick: two-axis tilt T(0.75,1,0)·Rz(tiltFront)·Rx(tiltLeft)·T(-0.75,-1.015,0)
            //   (the baked rotatingPart only had the Z axis and dropped the tail nudge);
            // - Meter1/2: needle at wobble + 135 - 270*loadedHeat/loadedEnrichment;
            // - Lamp1/2: untextured state lamps - white sprite + per-instance tint with
            //   full emission (the original drew them with lighting disabled, lightmap 240/240).
            .dynamicPart("Joystick", MachineRbmkControlRenderer::joystickAnim,
                    MachineRbmkControlRenderer::joystickQuads, be -> "joy")
            .dynamicPart("Meter1", MachineRbmkControlRenderer::meter1Anim,
                    MachineRbmkControlRenderer::meter1Quads, be -> "m1")
            .dynamicPart("Meter2", MachineRbmkControlRenderer::meter2Anim,
                    MachineRbmkControlRenderer::meter2Quads, be -> "m2")
            .tintOverride("Lamp1", MachineRbmkControlRenderer::lamp1Tint)
            .dynamicPart("Lamp1", MachineRbmkControlRenderer::identityAnim,
                    MachineRbmkControlRenderer::lamp1Quads, be -> "l1")
            .tintOverride("Lamp2", MachineRbmkControlRenderer::lamp2Tint)
            .dynamicPart("Lamp2", MachineRbmkControlRenderer::identityAnim,
                    MachineRbmkControlRenderer::lamp2Quads, be -> "l2")
            // The gantry hoist crane traversing the reactor hall, migrated off the immediate
            // hook onto dynamic parts: the girder repetitions / tube stack / carriage compile
            // into cached VBOs keyed by crane size, and the per-frame transforms (hall anchor,
            // gantry travel, hall rotation, hoist progress) ride the animators - the exact
            // coordinate chain of the hook they replace, split across three parts because the
            // girder sits one counter-rotate + offset deeper than the rest of the assembly.
            .dynamicPart("CraneGirder", MachineRbmkControlRenderer::craneGirder,
                    MachineRbmkControlRenderer::craneGirderQuads, MachineRbmkControlRenderer::craneGirderKey)
            .dynamicPart("CraneFrame", MachineRbmkControlRenderer::craneFrame,
                    MachineRbmkControlRenderer::craneFrameQuads, MachineRbmkControlRenderer::craneFrameKey)
            .dynamicPart("CraneLift", MachineRbmkControlRenderer::craneLift,
                    MachineRbmkControlRenderer::craneLiftQuads, MachineRbmkControlRenderer::craneLiftKey)
            .viewDistance(128)
            .itemParts("Console_Coonsole")
            .register();

        // ── RBMK Autoloader ──────────────────────────────────────────────────
        MachineRenderers.machine("rbmk_autoloader",
                ModBlockEntities.RBMK_AUTOLOADER_BE.get(),
                RBMKAutoloaderBlockEntity.class)
            .blockTransform(ObjBerHelper::setupBakedBlockTransform)
            .part("Base")
            // Fuel transfer piston/sleeve traveling 4 blocks down into channel
            .translatingPart("Piston", new Vector3f(0f, 1f, 0f),
                    (be, pt) -> (float) (4.0 - (be.lastPiston + (be.piston - be.lastPiston) * pt) * 4.0))
            // Sole rbmk_autoloader spec now: an uncommitted working-tree state had a second
            // hook spec in MachineRBMKColumnRenderer, which (registered later) overwrote this
            // one in MachineRenderRegistry.BY_TYPE; removed in the MDI migration. 64 matches
            // the legacy BER default that overwritten spec ran at.
            .viewDistance(64)
            .itemParts("Base", "Piston")
            .register();
    }

    // ─── Crane console desk widgets (1:1 with RenderCraneConsole) ────────────

    private static final String CRANE_CONSOLE_OBJ = "models/rbmk/models/crane_console.obj";
    private static final String CRANE_CONSOLE_TEX = "block/machine/crane_console";

    /**
     * Two-axis joystick tilt, 1:1 with the original's transform chain
     * {@code T(0.75,1,0)·Rz(tiltFront)·Rx(tiltLeft)·T(-0.75,-1.015,0)}. Tilt state
     * (incl. tiltLeft) is ticked and synced by the BE - the previous baked
     * rotatingPart could only ever express the Z axis.
     */
    private static boolean joystickAnim(RBMKCraneConsoleBlockEntity be, float pt, long gt, PoseStack ps) {
        ps.translate(0.75, 1, 0);
        ps.mulPose(Axis.ZP.rotationDegrees((float) Mth.lerp(pt, be.lastTiltFront, be.tiltFront)));
        ps.mulPose(Axis.XP.rotationDegrees((float) Mth.lerp(pt, be.lastTiltLeft, be.tiltLeft)));
        ps.translate(-0.75, -1.015, 0);
        return true;
    }

    /** Idle needle jitter, 1:1 with the original's {@code sin(now*0.01 % 360)·180/PI·0.05}. */
    private static double meterWobble() {
        return Math.sin(System.currentTimeMillis() * 0.01 % 360) * 180 / Math.PI * 0.05;
    }

    /** Heat gauge needle: {@code wobble + 135 - 270*loadedHeat} around X at (0, 1.25, 0.75). */
    private static boolean meter1Anim(RBMKCraneConsoleBlockEntity be, float pt, long gt, PoseStack ps) {
        ps.translate(0, 1.25, 0.75);
        ps.mulPose(Axis.XP.rotationDegrees((float) (meterWobble() + 135 - 270 * be.loadedHeat)));
        ps.translate(0, -1.25, -0.75);
        return true;
    }

    /** Enrichment gauge needle: same formula over {@code loadedEnrichment} at (0, 1.25, 0.25). */
    private static boolean meter2Anim(RBMKCraneConsoleBlockEntity be, float pt, long gt, PoseStack ps) {
        ps.translate(0, 1.25, 0.25);
        ps.mulPose(Axis.XP.rotationDegrees((float) (meterWobble() + 135 - 270 * be.loadedEnrichment)));
        ps.translate(0, -1.25, -0.25);
        return true;
    }

    /**
     * Lamps draw untransformed; the animator exists purely so the part counts as
     * animated and its {@code tintOverride} refreshes every frame (static parts
     * only refresh tint on a full VBO rebuild).
     */
    private static boolean identityAnim(RBMKCraneConsoleBlockEntity be, float pt, long gt, PoseStack ps) {
        return true;
    }

    private static List<BakedQuad> deskPartQuads(RBMKCraneConsoleBlockEntity be, String group,
                                                 TextureAtlasSprite sprite) {
        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(CRANE_CONSOLE_OBJ);
        return RBMKColumnRenderer.objGroupQuads(obj.get(group), sprite, 0, 0, 0);
    }

    private static List<BakedQuad> joystickQuads(RBMKCraneConsoleBlockEntity be) {
        return deskPartQuads(be, "Joystick", RBMKColumnRenderer.sprite(RefStrings.MODID, CRANE_CONSOLE_TEX));
    }

    private static List<BakedQuad> meter1Quads(RBMKCraneConsoleBlockEntity be) {
        return deskPartQuads(be, "Meter1", RBMKColumnRenderer.sprite(RefStrings.MODID, CRANE_CONSOLE_TEX));
    }

    private static List<BakedQuad> meter2Quads(RBMKCraneConsoleBlockEntity be) {
        return deskPartQuads(be, "Meter2", RBMKColumnRenderer.sprite(RefStrings.MODID, CRANE_CONSOLE_TEX));
    }

    private static List<BakedQuad> lamp1Quads(RBMKCraneConsoleBlockEntity be) {
        return deskPartQuads(be, "Lamp1", whiteSprite());
    }

    private static List<BakedQuad> lamp2Quads(RBMKCraneConsoleBlockEntity be) {
        return deskPartQuads(be, "Lamp2", whiteSprite());
    }

    /** Loading indicator: crane busy yellow / rod loaded green / unloaded dark. */
    private static float[] lamp1Tint(RBMKCraneConsoleBlockEntity be) {
        if (be.isCraneLoading()) return new float[] {0.8f, 0.8f, 0f, 1f};
        if (be.hasItemLoaded())  return new float[] {0f, 1f, 0f, 1f};
        return new float[] {0f, 0.1f, 0f, 1f};
    }

    /** Target indicator: valid load/unload column under the claw green, otherwise red. */
    private static float[] lamp2Tint(RBMKCraneConsoleBlockEntity be) {
        Level level = be.getLevel();
        boolean valid = level != null && be.getLoadableAtPos(level) != null;
        return valid ? new float[] {0f, 1f, 0f, 1f} : new float[] {1f, 0f, 0f, 1f};
    }

    // ─── Crane console: dynamic parts (former immediate hook) ────────────────

    private static final String CRANE_OBJ = "models/rbmk/models/crane.obj";
    private static final String CRANE_TEX = "block/machine/rbmk_crane";

    /**
     * Shared frame transform: hall anchor (center of the crane grid), gantry
     * travel and hall rotation - the head of the legacy hook's transform chain,
     * identical for every crane part.
     */
    private static boolean craneFrame(RBMKCraneConsoleBlockEntity be, float pt, long gt, PoseStack ps) {
        if (!be.setUpCrane) return false;
        var pos = be.getBlockPos();
        ps.translate(0.5, -1, 0.5);
        ps.translate(be.center.getX() - pos.getX(),
                     be.center.getY() - pos.getY() + 1,
                     be.center.getZ() - pos.getZ());
        double posX = Mth.lerp(pt, be.lastPosFront, be.posFront);
        double posZ = Mth.lerp(pt, be.lastPosLeft, be.posLeft);
        ps.translate(-posX, 0, posZ);
        ps.mulPose(Axis.YP.rotationDegrees(be.craneRotationOffset));
        return true;
    }

    /** Girder copy count from the rotation-facing switch of the legacy hook. */
    private static int girderSpan(RBMKCraneConsoleBlockEntity be) {
        return switch (be.craneRotationOffset) {
            case 90, 270 -> be.spanL + be.spanR + 1;
            default      -> be.spanF + be.spanB + 1;
        };
    }

    /**
     * Girder bridge: the hook's counter-rotate → offset-switch → re-rotate
     * sandwich (the offset itself travels with the gantry, hence per-frame),
     * with the span repetitions baked into the cached VBO at 1-block spacing.
     */
    private static boolean craneGirder(RBMKCraneConsoleBlockEntity be, float pt, long gt, PoseStack ps) {
        if (!craneFrame(be, pt, gt, ps)) return false;
        double posX = Mth.lerp(pt, be.lastPosFront, be.posFront);
        double posZ = Mth.lerp(pt, be.lastPosLeft, be.posLeft);
        ps.mulPose(Axis.YP.rotationDegrees(-be.craneRotationOffset));
        switch (be.craneRotationOffset) {
            case 90  -> ps.translate(0, 0, -posZ - be.spanR);
            case 180 -> ps.translate(posX - be.spanF, 0, 0);
            case 270 -> ps.translate(0, 0, -posZ + be.spanL);
            default  -> ps.translate(posX + be.spanB, 0, 0);
        }
        ps.mulPose(Axis.YP.rotationDegrees(be.craneRotationOffset));
        return true;
    }

    private static List<BakedQuad> craneGirderQuads(RBMKCraneConsoleBlockEntity be) {
        if (!be.setUpCrane) return List.of();
        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(CRANE_OBJ);
        TextureAtlasSprite tex = RBMKColumnRenderer.sprite(RefStrings.MODID, CRANE_TEX);
        List<BakedQuad> quads = new ArrayList<>();
        for (int i = 0; i < girderSpan(be); i++)
            quads.addAll(RBMKColumnRenderer.objGroupQuads(obj.get("Girder"), tex, -i, 0, 0));
        return quads;
    }

    private static String craneGirderKey(RBMKCraneConsoleBlockEntity be) {
        // setUpCrane in the key: the resolver runs once per key, a VBO built while the
        // crane was unset would stay empty forever.
        return be.setUpCrane ? "g" + be.craneRotationOffset + "|" + girderSpan(be) : "off";
    }

    /** Main mast + tube stack up the hall height + carriage (static within the crane frame). */
    private static List<BakedQuad> craneFrameQuads(RBMKCraneConsoleBlockEntity be) {
        if (!be.setUpCrane) return List.of();
        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(CRANE_OBJ);
        TextureAtlasSprite tex = RBMKColumnRenderer.sprite(RefStrings.MODID, CRANE_TEX);
        List<BakedQuad> quads = new ArrayList<>();
        quads.addAll(RBMKColumnRenderer.objGroupQuads(obj.get("Main"), tex, 0, 0, 0));
        // The hook's arithmetic: N tubes at y = 0..N-1, then the stack translates +N and
        // back -1, so the carriage sits at y = height - 7, co-located with the top tube.
        for (int i = 0; i < be.height - 6; i++)
            quads.addAll(RBMKColumnRenderer.objGroupQuads(obj.get("Tube"), tex, 0, i, 0));
        quads.addAll(RBMKColumnRenderer.objGroupQuads(obj.get("Carriage"), tex, 0, be.height - 7, 0));
        return quads;
    }

    private static String craneFrameKey(RBMKCraneConsoleBlockEntity be) {
        return be.setUpCrane ? "f" + be.height : "off";
    }

    /** Hoist hook, riding the crane frame plus the progress-driven vertical travel. */
    private static boolean craneLift(RBMKCraneConsoleBlockEntity be, float pt, long gt, PoseStack ps) {
        if (!craneFrame(be, pt, gt, ps)) return false;
        ps.translate(0, -3.25 * (1 - Mth.lerp(pt, be.lastProgress, be.progress)), 0);
        return true;
    }

    private static List<BakedQuad> craneLiftQuads(RBMKCraneConsoleBlockEntity be) {
        if (!be.setUpCrane) return List.of();
        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(CRANE_OBJ);
        TextureAtlasSprite tex = RBMKColumnRenderer.sprite(RefStrings.MODID, CRANE_TEX);
        return RBMKColumnRenderer.objGroupQuads(obj.get("Lift"), tex, 0, 0, 0);
    }

    private static String craneLiftKey(RBMKCraneConsoleBlockEntity be) {
        return be.setUpCrane ? "lift" : "off";
    }

    private static void drawDot(VertexConsumer vc, Matrix4f m, double x, double y, double z,
                                float r, float g, float b, TextureAtlasSprite sprite, int light, int overlay) {
        double w = 0.03125D;
        double e = 0.022097D;
        float fx = (float) x;

        quad(vc, m, fx, (float) (y + w), (float) z,
                    fx, (float) (y + e), (float) (z + e),
                    fx, (float) y,       (float) (z + w),
                    fx, (float) (y - e), (float) (z + e),
                1, 0, 0, sprite, r, g, b, light, overlay);

        quad(vc, m, fx, (float) (y + e), (float) (z - e),
                    fx, (float) (y + w), (float) z,
                    fx, (float) (y - e), (float) (z - e),
                    fx, (float) y,       (float) (z - w),
                1, 0, 0, sprite, r, g, b, light, overlay);

        quad(vc, m, fx, (float) (y + w), (float) z,
                    fx, (float) (y - e), (float) (z + e),
                    fx, (float) (y - w), (float) z,
                    fx, (float) (y - e), (float) (z - e),
                1, 0, 0, sprite, r, g, b, light, overlay);
    }

    private static void renderScreenText(MachineRbmkConsoleBlockEntity be, PoseStack ps,
                                         MultiBufferSource buf, int light) {
        Font font = Minecraft.getInstance().font;
        ps.pushPose();
        ps.translate(-0.42F, 3.5F, 1.75F);

        for (int i = 0; i < MachineRbmkConsoleBlockEntity.SCREENS; i++) {
            ps.pushPose();
            if (i % 2 == 1) ps.translate(0, 0, 1.75F * -2);
            ps.translate(0, -0.75F * (i / 2), 0);

            String text = be.screenText != null && be.screenText[i] != null ? be.screenText[i] : "";
            if (!text.isEmpty()) {
                // 1:1 with RenderRBMKConsole: the stored readout is "<lang key>=<value>" and is
                // resolved at draw time, so the physical panel reads "Temp: 123.4°C" instead of
                // the raw "rbmk.screen.temp=123.4°C".
                String[] parts = text.split("=");
                if (parts.length == 2) {
                    text = Component.translatable(parts[0], parts[1]).getString();
                }
                int width = font.width(text);
                int height = 9;
                float f3 = Math.min(0.03F, 0.8F / Math.max(width, 1));
                ps.scale(f3, -f3, f3);
                ps.mulPose(Axis.YP.rotationDegrees(90));
                font.drawInBatch(text, -width / 2f, -height / 2f, 0x00FF00, false,
                        ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, light);
            }
            ps.popPose();
        }
        ps.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float nx, float ny, float nz,
                             TextureAtlasSprite sprite, float r, float g, float b, int light, int overlay) {
        float u = (sprite.getU0() + sprite.getU1()) * 0.5f;
        float v = (sprite.getV0() + sprite.getV1()) * 0.5f;
        int ir = (int) (r * 255f + 0.5f);
        int ig = (int) (g * 255f + 0.5f);
        int ib = (int) (b * 255f + 0.5f);
        RenderHooks.vertexFull(vc, m, x0, y0, z0, ir, ig, ib, 0xFF, u, v, overlay, light, nx, ny, nz);
        RenderHooks.vertexFull(vc, m, x1, y1, z1, ir, ig, ib, 0xFF, u, v, overlay, light, nx, ny, nz);
        RenderHooks.vertexFull(vc, m, x2, y2, z2, ir, ig, ib, 0xFF, u, v, overlay, light, nx, ny, nz);
        RenderHooks.vertexFull(vc, m, x3, y3, z3, ir, ig, ib, 0xFF, u, v, overlay, light, nx, ny, nz);
    }

    private MachineRbmkControlRenderer() {}
}
