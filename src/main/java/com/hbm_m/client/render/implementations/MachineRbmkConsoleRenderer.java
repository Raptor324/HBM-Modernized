package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineRbmkConsoleBlock;
import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity;
import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity.RBMKColumnData;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity.ColumnType;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;

/**
 * Renders the console's full body mesh ({@code models/block/rbmk_console.obj} - a single large
 * hand-modeled ~2x4x5-block mesh, not a 1-block cube) and draws a live mini-map of the linked
 * reactor grid onto its screen face, one small colored quad per column - matches the original
 * 1.7.10 console's screen, which is not a static texture at all but a dynamically-drawn overview
 * of the reactor.
 * <p>
 * The mesh is loaded and rendered directly here (reusing {@link RBMKColumnRenderer}'s OBJ
 * loader/renderer helpers) rather than through a static block model: the previous setup routed
 * it through a Forge-only "forge:composite"/"forge:obj" custom model loader, which isn't
 * reliably available in this multi-loader (Forge+Fabric) build and silently baked down to a
 * bare cube - the console rendered as a featureless slab even though the correct mesh and
 * texture were both present as assets.
 * <p>
 * Screen quad position is the OBJ's own "screen" mesh island bounding box (X≈-0.5, Y:[1.75,3.75],
 * Z:[-1,1] in the model's raw coordinate space), verified directly against the OBJ's vertex data.
 */
public class MachineRbmkConsoleRenderer implements BlockEntityRenderer<MachineRbmkConsoleBlockEntity> {

    private static final int GRID = MachineRbmkConsoleBlockEntity.GRID;
    private static final String MODEL_PATH = "models/block/rbmk_console.obj";

    // Screen quad bounds in the OBJ's raw local space (see class javadoc).
    private static final float SCREEN_X = -0.505f;
    private static final float SCREEN_Y0 = 1.85f;
    private static final float SCREEN_Y1 = 3.65f;
    private static final float SCREEN_Z0 = -0.95f;
    private static final float SCREEN_Z1 = 0.95f;
    private static final float CELL_H = (SCREEN_Y1 - SCREEN_Y0) / GRID;
    private static final float CELL_W = (SCREEN_Z1 - SCREEN_Z0) / GRID;
    private static final float GAP = 0.006f;

    public MachineRbmkConsoleRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static TextureAtlasSprite whiteSprite;

    /** A plain, uniformly-light vanilla sprite used purely as a tinting base for the flat dots
     *  below - RenderType.solid() always samples a texture, so an arbitrary raw UV rect on
     *  whatever happens to be bound would sample garbage; a known flat sprite keeps the vertex
     *  color the dominant, predictable result. */
    private static TextureAtlasSprite whiteSprite() {
        if (whiteSprite == null) {
            whiteSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(ResourceLocation.withDefaultNamespace("block/white_concrete"));
        }
        return whiteSprite;
    }

    /** Renders the console's single-mesh body (OBJ group name is literally "Cube" in the source
     *  export, despite being the full detailed console - vents, buttons and all are baked into
     *  its ~4000 vertices, not split into named sub-parts). */
    private static void renderBody(PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(MODEL_PATH);
        List<float[]> mesh = obj.get("Cube");
        if (mesh == null) return;
        TextureAtlasSprite bodySprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/rbmk_console");
        VertexConsumer vc = buf.getBuffer(RenderType.solid());
        RBMKColumnRenderer.renderObjGroup(vc, ps.last().pose(), mesh, bodySprite, 1f, 1f, 1f, light, overlay);
    }

    @Override
    public void render(MachineRbmkConsoleBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
                        int packedLight, int packedOverlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5); // matches the original TE renderer's translate to block center

        // 1:1 with the original's per-facing rotation switch in RenderRBMKConsole - rotates the
        // whole mesh (and everything drawn after it, including the mini-map/screens below) to
        // match the block's stored facing, same y-degree convention as the blockstate JSON.
        Direction facing = be.getBlockState().getValue(MachineRbmkConsoleBlock.FACING);
        float rotY = switch (facing) {
            case SOUTH -> 180f;
            case WEST  -> 270f;
            case EAST  -> 90f;
            default    -> 0f; // NORTH
        };
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotY));

        renderBody(ps, buf, packedLight, packedOverlay);

        VertexConsumer vc = buf.getBuffer(RenderType.solid());
        Matrix4f m = ps.last().pose();
        TextureAtlasSprite sprite = whiteSprite();

        for (int i = 0; i < MachineRbmkConsoleBlockEntity.AREA; i++) {
            RBMKColumnData col = be.columns[i];
            int gx = i % GRID;
            int gz = i / GRID;

            float y0 = SCREEN_Y1 - (gz + 1) * CELL_H + GAP;
            float y1 = SCREEN_Y1 - gz * CELL_H - GAP;
            float z0 = SCREEN_Z0 + gx * CELL_W + GAP;
            float z1 = SCREEN_Z0 + (gx + 1) * CELL_W - GAP;

            float r, g, b;
            if (col == null) {
                r = 0.10f; g = 0.10f; b = 0.10f;
            } else if (col.data.contains("color") && col.data.getShort("color") >= 0) {
                // 1:1 with the original: a control rod assigned to a color group is tinted by
                // that group's color, overriding the heat gradient entirely.
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
                // 1:1 with the original: everything else is tinted by heat fraction (grayish at
                // rest, reddening as heat approaches maxHeat), with a faint per-cell checkerboard
                // variation (i%2) so adjacent same-heat cells remain visually distinguishable.
                double maxHeat = col.data.getDouble("maxHeat");
                double heat = maxHeat > 0 ? col.data.getDouble("heat") / maxHeat : 0;
                double base = 0.65 + (i % 2) * 0.05;
                r = (float) (base + (1 - base) * heat);
                g = (float) base;
                b = (float) base;
            }

            // 1:1 with the original: a column the crane is currently over flashes solid yellow,
            // overriding whatever color it would otherwise have.
            if (col != null && col.data.getInt("indicator") > 0) {
                r = 1f; g = 1f; b = 0f;
            }

            quad(vc, m, SCREEN_X, y0, z0, SCREEN_X, y0, z1, SCREEN_X, y1, z1, SCREEN_X, y1, z0,
                    -1, 0, 0, sprite, r, g, b, packedLight, packedOverlay);

            // Dot overlay: a small fixed-size marker whose BRIGHTNESS (not size) encodes
            // enrichment/insertion-level - 1:1 with the original's drawFuel/drawControl/
            // drawControlAuto (which always draw the same-size diamond, tinted green-by-
            // enrichment, yellow-by-level for manual rods, or purple-by-level for auto rods).
            if (col != null && (col.type == ColumnType.FUEL || col.type == ColumnType.CONTROL)) {
                float dr, dg, db;
                if (col.type == ColumnType.FUEL) {
                    double enrichment = Math.min(Math.max(col.data.getDouble("enrichment"), 0), 1);
                    dr = 0f; dg = (float) (0.25 + enrichment * 0.75); db = 0f;
                } else {
                    float level = (float) Math.min(Math.max(col.data.getDouble("level"), 0), 1);
                    boolean auto = col.data.getBoolean("auto");
                    dr = level; dg = auto ? 0f : level; db = auto ? level : 0f;
                }
                float half = Math.min(CELL_H, CELL_W) * 0.18f;
                float cyMid = (y0 + y1) * 0.5f;
                float czMid = (z0 + z1) * 0.5f;
                quad(vc, m, SCREEN_X - 0.001f, cyMid - half, czMid - half,
                        SCREEN_X - 0.001f, cyMid - half, czMid + half,
                        SCREEN_X - 0.001f, cyMid + half, czMid + half,
                        SCREEN_X - 0.001f, cyMid + half, czMid - half,
                        -1, 0, 0, sprite, dr, dg, db, packedLight, packedOverlay);
            }
        }

        renderScreenText(be, ps, buf, packedLight, packedOverlay);

        ps.popPose();
    }

    // ─── Text screens ────────────────────────────────────────────────────────

    private static final float TEXT_SCALE = 0.007f;

    /**
     * Draws the 6 per-screen aggregate readouts (see
     * {@code MachineRbmkConsoleBlockEntity#computeScreenText}) as flat text below the grid
     * mini-map, on the same plane. Orientation is a best-effort match to the model's screen
     * face (viewed from -X, columns run along +Z, rows down along -Y) - the exact placement may
     * need a small offset tweak once seen in-game.
     */
    private void renderScreenText(MachineRbmkConsoleBlockEntity be, PoseStack ps, MultiBufferSource buf,
                                   int packedLight, int packedOverlay) {
        Font font = Minecraft.getInstance().font;
        float y = SCREEN_Y0 - 0.08f;

        for (int i = 0; i < MachineRbmkConsoleBlockEntity.SCREENS; i++) {
            String text = be.screenText != null && be.screenText[i] != null ? be.screenText[i] : "";
            if (!text.isEmpty()) {
                ps.pushPose();
                ps.translate(SCREEN_X - 0.001f, y, SCREEN_Z0);
                ps.mulPose(new Quaternionf().rotateY((float) (-Math.PI / 2)));
                ps.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
                font.drawInBatch(text, 0, 0, 0x40FF60, false, ps.last().pose(), buf,
                        Font.DisplayMode.NORMAL, 0, packedLight);
                ps.popPose();
            }
            y -= 0.10f;
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                              float x0, float y0, float z0, float x1, float y1, float z1,
                              float x2, float y2, float z2, float x3, float y3, float z3,
                              float nx, float ny, float nz,
                              TextureAtlasSprite sprite, float r, float g, float b, int light, int overlay) {
        float u = (sprite.getU0() + sprite.getU1()) * 0.5f;
        float v = (sprite.getV0() + sprite.getV1()) * 0.5f;
        //? if < 1.21.1 {
        vc.vertex(m, x0, y0, z0).color(r, g, b, 1f).uv(u, v).overlayCoords(overlay).uv2(light).normal(nx, ny, nz).endVertex();
        vc.vertex(m, x1, y1, z1).color(r, g, b, 1f).uv(u, v).overlayCoords(overlay).uv2(light).normal(nx, ny, nz).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, 1f).uv(u, v).overlayCoords(overlay).uv2(light).normal(nx, ny, nz).endVertex();
        vc.vertex(m, x3, y3, z3).color(r, g, b, 1f).uv(u, v).overlayCoords(overlay).uv2(light).normal(nx, ny, nz).endVertex();
        //?} else {
        /*vc.addVertex(m, x0, y0, z0).setColor(r, g, b, 1f).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, 1f).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, 1f).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        vc.addVertex(m, x3, y3, z3).setColor(r, g, b, 1f).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        *///?}
    }

    @Override public boolean shouldRenderOffScreen(MachineRbmkConsoleBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 64; }
}
