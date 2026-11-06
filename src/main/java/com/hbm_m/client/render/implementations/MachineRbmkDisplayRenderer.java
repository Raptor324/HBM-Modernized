package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity.RBMKColumnData;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity.ColumnType;
import com.hbm_m.blockentity.machines.rbmk.RBMKDisplayBlockEntity;
import com.hbm_m.client.render.cache.RenderCacheManager;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Nucleus renderer for the RBMK Display panel - 1:1 port of {@code RenderRBMKDisplay}: the
 * 7x7 reactor mini-map drawn onto the panel face.
 *
 * <p>The panel body itself is the block's static model; this spec declares no parts and only
 * hooks the dynamic mini-map. Each column is a small square tinted by its heat ratio, or by its
 * assigned color group when it has one, or flashing yellow while the crane hovers over it. Fuel,
 * control and auto-control columns additionally get an octagonal dot on top encoding enrichment
 * or rod insertion. The default Nucleus block transform (T(0.5,0,0.5) + 90° + legacy facing
 * rotation: NORTH 90°, WEST 180°, SOUTH 270°, EAST 0°) reproduces the legacy BER's
 * {@code begin()} transform exactly, so the hook draws at the original's local coordinates.</p>
 */
public final class MachineRbmkDisplayRenderer {

    private static final int GRID = RBMKDisplayBlockEntity.GRID;

    /** The original's five color-group swatches, in RBMKColor order. */
    private static final int[] GROUP_COLORS = {0xFF0000, 0xFFFF00, 0x008000, 0x0000FF, 0x8000FF};

    /** {@code RenderRBMKDisplay.fullbright(true)} - lightmap pinned to maximum. */
    private static final int FULLBRIGHT = LightTexture.FULL_BRIGHT;

    private static TextureAtlasSprite white;

    // A re-stitch replaces the atlas sprites wholesale (u0/v0 are final on TextureAtlasSprite),
    // so the latch must be dropped on reload/session end - white() re-fetches lazily.
    static {
        RenderCacheManager.register(reason -> white = null);
    }

    /** A flat sprite used purely as a tint base, same trick the console mini-map uses. */
    private static TextureAtlasSprite white() {
        if (white == null) {
            white = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(ResourceLocation.withDefaultNamespace("block/white_concrete"));
        }
        return white;
    }

    public static void register() {
        MachineRenderers.machine("rbmk_display",
                ModBlockEntities.RBMK_DISPLAY_BE.get(),
                RBMKDisplayBlockEntity.class)
            .viewDistance(64)
            .hook(new MachineRenderHook<RBMKDisplayBlockEntity>() {
                @Override
                public void render(RBMKDisplayBlockEntity be, float pt, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    VertexConsumer vc = buf.getBuffer(RenderType.solid());

                    // The original scales the whole map up by 8/7 about the panel's mid height.
                    ps.pushPose();
                    ps.translate(0, 0.5, 0);
                    ps.scale(1f, 8f / 7f, 8f / 7f);
                    ps.translate(0, -0.5, 0);

                    for (int i = 0; i < RBMKDisplayBlockEntity.AREA; i++) {
                        RBMKColumnData col = be.columns[i];
                        if (col == null) continue;

                        float kx = 0.28125f;
                        float ky = (float) (-(i / GRID) * 0.125 + 0.875);
                        float kz = (float) (-(i % GRID) * 0.125 + 0.125 * 3);

                        CompoundTag d = col.data;
                        float r, g, b;
                        if (d.contains("color") && d.getInt("color") >= 0 && d.getInt("color") < GROUP_COLORS.length) {
                            int rgb = GROUP_COLORS[d.getInt("color")];
                            r = red(rgb); g = green(rgb); b = blue(rgb);
                        } else {
                            double maxHeat = d.getDouble("maxHeat");
                            double heat = maxHeat > 0 ? d.getDouble("heat") / maxHeat : 0;
                            float base = (float) (0.65D + (i % 2) * 0.05D);
                            r = (float) (base + (1 - base) * heat);
                            g = base;
                            b = base;
                        }
                        if (d.getInt("indicator") > 0) { r = 1f; g = 1f; b = 0f; }

                        square(vc, ps, kx, ky, kz, r, g, b, overlay);

                        // The original splits CONTROL and CONTROL_AUTO into two ColumnType constants; this
                        // port keeps one CONTROL type and flags auto rods with an "auto" bool in the console
                        // payload (see RBMKControlAutoBlockEntity#getNBTForConsole), so the dot color is
                        // chosen from that flag instead - same yellow-vs-purple result as the original.
                        switch (col.type) {
                            case FUEL -> dot(vc, ps, kx + 0.01f, ky, kz, 0f,
                                    (float) (0.25D + d.getDouble("enrichment") * 0.75D), 0f, overlay);
                            case CONTROL -> {
                                float level = (float) d.getDouble("level");
                                if (d.getBoolean("auto")) dot(vc, ps, kx + 0.01f, ky, kz, level, 0f, level, overlay);
                                else                      dot(vc, ps, kx + 0.01f, ky, kz, level, level, 0f, overlay);
                            }
                            default -> { }
                        }
                    }

                    ps.popPose();
                }

                @Override
                public boolean mayDrawContent(RBMKDisplayBlockEntity be) {
                    for (int i = 0; i < RBMKDisplayBlockEntity.AREA; i++)
                        if (be.columns[i] != null) return true;
                    return false;
                }
            })
            .register();
    }

    /** {@code drawColumn} - the flat cell behind each column. */
    private static void square(VertexConsumer vc, PoseStack ps, float x, float y, float z,
                                float r, float g, float b, int overlay) {
        float w = 0.0625f * 0.75f;
        quad(vc, ps, x, y + w, z - w, x, y + w, z + w, x, y - w, z + w, x, y - w, z - w, r, g, b, overlay);
    }

    /**
     * {@code drawDot} - the original builds the octagon from three overlapping quads; the same
     * three are emitted here so the silhouette matches exactly.
     */
    private static void dot(VertexConsumer vc, PoseStack ps, float x, float y, float z,
                             float r, float g, float b, int overlay) {
        float w = 0.03125f;
        float e = 0.022097f;
        quad(vc, ps, x, y + w, z,     x, y + e, z + e, x, y,     z + w, x, y - e, z + e, r, g, b, overlay);
        quad(vc, ps, x, y + e, z - e, x, y + w, z,     x, y - e, z - e, x, y,     z - w, r, g, b, overlay);
        quad(vc, ps, x, y + w, z,     x, y - e, z + e, x, y - w, z,     x, y - e, z - e, r, g, b, overlay);
    }

    private static void quad(VertexConsumer vc, PoseStack ps,
                              float x0, float y0, float z0, float x1, float y1, float z1,
                              float x2, float y2, float z2, float x3, float y3, float z3,
                              float r, float g, float b, int overlay) {
        var m = ps.last().pose();
        TextureAtlasSprite s = white();
        float u = (s.getU0() + s.getU1()) / 2f;
        float v = (s.getV0() + s.getV1()) / 2f;
        RenderHooks.vertexFull(vc, m, x0, y0, z0, (int)(r * 255f), (int)(g * 255f), (int)(b * 255f), (int)(1f * 255f), u, v, overlay, FULLBRIGHT, 1, 0, 0);
        RenderHooks.vertexFull(vc, m, x1, y1, z1, (int)(r * 255f), (int)(g * 255f), (int)(b * 255f), (int)(1f * 255f), u, v, overlay, FULLBRIGHT, 1, 0, 0);
        RenderHooks.vertexFull(vc, m, x2, y2, z2, (int)(r * 255f), (int)(g * 255f), (int)(b * 255f), (int)(1f * 255f), u, v, overlay, FULLBRIGHT, 1, 0, 0);
        RenderHooks.vertexFull(vc, m, x3, y3, z3, (int)(r * 255f), (int)(g * 255f), (int)(b * 255f), (int)(1f * 255f), u, v, overlay, FULLBRIGHT, 1, 0, 0);
    }

    private static float red(int rgb)   { return ((rgb >> 16) & 0xFF) / 255f; }
    private static float green(int rgb) { return ((rgb >>  8) & 0xFF) / 255f; }
    private static float blue(int rgb)  { return ( rgb        & 0xFF) / 255f; }

    private MachineRbmkDisplayRenderer() {}
}
