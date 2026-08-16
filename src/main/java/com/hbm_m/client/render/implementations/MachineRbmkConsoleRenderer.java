package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity;
import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity.RBMKColumnData;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity.ColumnType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

/**
 * Draws a live mini-map of the linked reactor grid onto the console's central screen face, one
 * small colored quad per column (dark/unlit for empty cells, tinted by {@link ColumnType} for
 * populated ones) - matches the original 1.7.10 console's screen, which is not a static texture at
 * all but a dynamically-drawn overview of the reactor (see reference screenshots: the "screen" area
 * on {@code gui_rbmk_console.png}'s base OBJ is otherwise blank/untextured on purpose).
 * <p>
 * Screen quad position is the OBJ's own "screen" mesh island bounding box (X≈-0.5, Y:[1.75,3.75],
 * Z:[-1,1] in the model's raw coordinate space; the model.json applies a static (0.5,0,0.5)
 * translation to center that space on the block, replicated here via {@code ps.translate}).
 */
public class MachineRbmkConsoleRenderer implements BlockEntityRenderer<MachineRbmkConsoleBlockEntity> {

    private static final int GRID = MachineRbmkConsoleBlockEntity.GRID;

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

    @Override
    public void render(MachineRbmkConsoleBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
                        int packedLight, int packedOverlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5); // matches the model.json's static transform

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
            } else {
                int rgb = tint(col.type);
                r = ((rgb >> 16) & 0xFF) / 255f;
                g = ((rgb >> 8) & 0xFF) / 255f;
                b = (rgb & 0xFF) / 255f;
            }

            quad(vc, m, SCREEN_X, y0, z0, SCREEN_X, y0, z1, SCREEN_X, y1, z1, SCREEN_X, y1, z0,
                    -1, 0, 0, sprite, r, g, b, packedLight, packedOverlay);
        }

        ps.popPose();
    }

    private static int tint(ColumnType type) {
        return switch (type) {
            case FUEL -> 0x30D030;
            case CONTROL -> 0xD0D030;
            case MODERATOR -> 0x3080D0;
            case ABSORBER -> 0xA030A0;
            case REFLECTOR -> 0x808080;
            case COOLER -> 0x30C0D0;
            case BOILER, HEATER -> 0xD08030;
            case OUTGASSER -> 0xD03030;
            case STORAGE -> 0x606060;
            default -> 0x303030;
        };
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                              float x0, float y0, float z0, float x1, float y1, float z1,
                              float x2, float y2, float z2, float x3, float y3, float z3,
                              float nx, float ny, float nz,
                              TextureAtlasSprite sprite, float r, float g, float b, int light, int overlay) {
        // Samples a single point near the sprite's center rather than its full UV range, so every
        // dot reads as a flat, evenly-tinted color regardless of the source sprite's own pattern.
        float u = (sprite.getU0() + sprite.getU1()) * 0.5f;
        float v = (sprite.getV0() + sprite.getV1()) * 0.5f;
        vc.vertex(m, x0, y0, z0).color(r, g, b, 1f).uv(u, v).overlayCoords(overlay).uv2(light).normal(nx, ny, nz).endVertex();
        vc.vertex(m, x1, y1, z1).color(r, g, b, 1f).uv(u, v).overlayCoords(overlay).uv2(light).normal(nx, ny, nz).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, 1f).uv(u, v).overlayCoords(overlay).uv2(light).normal(nx, ny, nz).endVertex();
        vc.vertex(m, x3, y3, z3).color(r, g, b, 1f).uv(u, v).overlayCoords(overlay).uv2(light).normal(nx, ny, nz).endVertex();
    }

    @Override public boolean shouldRenderOffScreen(MachineRbmkConsoleBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 64; }
}
