package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.block.machines.MachineRbmkConsoleBlock;
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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;

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
                            case FUEL -> {
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
                float angle = switch (be.facing) {
                    case NORTH -> 90f;
                    case WEST  -> 180f;
                    case SOUTH -> 270f;
                    default    -> 0f;
                };
                animator.rotate(angle, 0, 1, 0);
                animator.translate(0.5, 0, 0);
            })
            .part("Console_Coonsole")
            // Dynamic Joystick tilting with movement inputs
            .rotatingPart("Joystick", new Vector3f(0f, 0f, 1f), new Vector3f(0.75f, 1f, 0f),
                    (be, pt) -> Mth.lerp(pt, be.lastTiltFront, be.tiltFront))
            .part("Meter1")
            .part("Meter2")
            .hook(new MachineRenderHook<RBMKCraneConsoleBlockEntity>() {
                @Override
                public void render(RBMKCraneConsoleBlockEntity be, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    if (!be.setUpCrane) return;

                    Map<String, List<float[]>> craneObj = RBMKColumnRenderer.getObj("models/rbmk/models/crane.obj");
                    TextureAtlasSprite craneTex = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/rbmk_crane");

                    ps.pushPose();
                    ps.translate(0.5, -1, 0.5);
                    var pos = be.getBlockPos();
                    ps.translate(be.center.getX() - pos.getX(),
                                 be.center.getY() - pos.getY() + 1,
                                 be.center.getZ() - pos.getZ());

                    double posX = Mth.lerp(pt, be.lastPosFront, be.posFront);
                    double posZ = Mth.lerp(pt, be.lastPosLeft, be.posLeft);
                    ps.translate(-posX, 0, posZ);
                    ps.mulPose(Axis.YP.rotationDegrees(be.craneRotationOffset));

                    // Girder bridge
                    ps.pushPose();
                    ps.mulPose(Axis.YP.rotationDegrees(-be.craneRotationOffset));
                    int girderSpan = switch (be.craneRotationOffset) {
                        case 90 -> {
                            ps.translate(0, 0, -posZ - be.spanR);
                            yield be.spanL + be.spanR + 1;
                        }
                        case 180 -> {
                            ps.translate(posX - be.spanF, 0, 0);
                            yield be.spanF + be.spanB + 1;
                        }
                        case 270 -> {
                            ps.translate(0, 0, -posZ + be.spanL);
                            yield be.spanL + be.spanR + 1;
                        }
                        default -> {
                            ps.translate(posX + be.spanB, 0, 0);
                            yield be.spanF + be.spanB + 1;
                        }
                    };
                    ps.mulPose(Axis.YP.rotationDegrees(be.craneRotationOffset));
                    for (int i = 0; i < girderSpan; i++) {
                        renderCranePart(ps, buf, craneObj, "Girder", craneTex, light, overlay);
                        ps.translate(-1, 0, 0);
                    }
                    ps.popPose();

                    renderCranePart(ps, buf, craneObj, "Main", craneTex, light, overlay);

                    ps.pushPose();
                    for (int i = 0; i < be.height - 6; i++) {
                        renderCranePart(ps, buf, craneObj, "Tube", craneTex, light, overlay);
                        ps.translate(0, 1, 0);
                    }
                    ps.translate(0, -1, 0);
                    renderCranePart(ps, buf, craneObj, "Carriage", craneTex, light, overlay);
                    ps.popPose();

                    ps.translate(0, -3.25 * (1 - Mth.lerp(pt, be.lastProgress, be.progress)), 0);
                    renderCranePart(ps, buf, craneObj, "Lift", craneTex, light, overlay);

                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(RBMKCraneConsoleBlockEntity be) {
                    return be.setUpCrane;
                }
            })
            .viewDistance(128)
            .itemParts("Console_Coonsole")
            .register();

        // ── RBMK Autoloader ──────────────────────────────────────────────────
        MachineRenderers.machine("rbmk_autoloader",
                ModBlockEntities.RBMK_AUTOLOADER_BE.get(),
                RBMKAutoloaderBlockEntity.class)
            .part("Base")
            // Fuel transfer piston/sleeve traveling 4 blocks down into channel
            .translatingPart("Piston", new Vector3f(0f, 1f, 0f),
                    (be, pt) -> (float) (4.0 - (be.lastPiston + (be.piston - be.lastPiston) * pt) * 4.0))
            .itemParts("Base", "Piston")
            .register();
    }

    private static void renderCranePart(PoseStack ps, MultiBufferSource buf, Map<String, List<float[]>> obj,
                                        String group, TextureAtlasSprite sprite, int light, int overlay) {
        List<float[]> mesh = obj.get(group);
        if (mesh == null) return;
        RBMKColumnRenderer.renderObjGroup(buf.getBuffer(RenderType.solid()),
                ps.last().pose(), mesh, sprite, 1f, 1f, 1f, light, overlay);
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
