package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.machines.MachineFoundryChannelBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFoundryChannelBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Foundry channel on the {@link MachineRenderers} factory -- port of the legacy
 * {@code FoundryChannelRenderer} BER: the molten-metal surface inside the trough
 * (x/z 6..10 px, floor top at 2 px, walls up to 8 px) -- a center quad plus one
 * quad per connected direction (the NORTH/SOUTH/EAST/WEST blockstate flags).
 * The surface height follows {@code amount / getCapacity()}; the quad is
 * fullbright (240/240, as the original lightmap 0xF000F0) with the
 * {@code block/machine/lava_gray} atlas sprite tinted by the molten color
 * (brightened by 100/70, clamped -- the original {@code new Color(c).brighter()}).
 * Geometry depends on connectivity + fill level + color, so the melt is a
 * dynamic part with a matching cache key (VBO compiled once per distinct
 * surface, LRU-swept). The block transform is identity: the quads are in
 * block-corner coordinates exactly like the legacy BER drew them (no facing
 * rotation -- the channel is symmetric, connectivity comes from the flags).
 */
public final class MachineFoundryChannelRenderer {

    /** Atlas sprite for the melt surface (atlas copy of the legacy {@code lava_gray} texture). */
    private static final ResourceLocation LAVA_GRAY_SPRITE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machine/lava_gray");

    // inner trough: x/z 6..10 px, floor top at 2 px, max melt level 6 px
    private static final float IN_MIN_PX = 6f;
    private static final float IN_MAX_PX = 10f;
    private static final float FLOOR_PX = 2f;
    private static final float MAX_LEVEL_PX = 6f;

    /** Packed +Y vertex normal (the canonical 3-byte packing, as DaeQuadBaker.packNormal). */
    private static final int NORMAL_UP =
            ((int) (0f * 127) & 0xFF) | (((int) (1f * 127) & 0xFF) << 8) | (((int) (0f * 127) & 0xFF) << 16);

    private MachineFoundryChannelRenderer() {}

    public static void register() {
        MachineRenderers.machine("foundry_channel", ModBlockEntities.FOUNDRY_CHANNEL_BE.get(),
                MachineFoundryChannelBlockEntity.class)
            .blockTransform((be, animator) -> { /* identity: block-corner geometry, no facing rotation */ })
            .dynamicPart("Melt", MachineFoundryChannelRenderer::meltQuads,
                    MachineFoundryChannelRenderer::cacheKey)
            .register();
    }

    // -- Geometry ----------------------------------------------------------

    private static float surfaceYPx(MachineFoundryChannelBlockEntity be) {
        float fill = Math.min(1f, be.amount / (float) be.getCapacity());
        return FLOOR_PX + fill * (MAX_LEVEL_PX - FLOOR_PX);
    }

    /** Cache key: connectivity flags + quantized surface height + molten color. */
    private static String cacheKey(MachineFoundryChannelBlockEntity be) {
        if (be.type == null || be.amount <= 0) return "empty";
        BlockState state = be.getBlockState();
        int flags = (flag(state, MachineFoundryChannelBlock.NORTH) ? 1 : 0)
                | (flag(state, MachineFoundryChannelBlock.SOUTH) ? 2 : 0)
                | (flag(state, MachineFoundryChannelBlock.WEST) ? 4 : 0)
                | (flag(state, MachineFoundryChannelBlock.EAST) ? 8 : 0);
        return flags + "_" + (int) (surfaceYPx(be) * 16f) + "_" + moltenTintRgba(be);
    }

    private static boolean flag(BlockState state, net.minecraft.world.level.block.state.properties.BooleanProperty p) {
        return state.hasProperty(p) && state.getValue(p);
    }

    private static List<BakedQuad> meltQuads(MachineFoundryChannelBlockEntity be) {
        if (be.type == null || be.amount <= 0) return List.of();

        float yPx = surfaceYPx(be);
        int tint = moltenTintRgba(be);
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(LAVA_GRAY_SPRITE);
        BlockState state = be.getBlockState();

        List<BakedQuad> quads = new ArrayList<>(5);
        // center
        quads.add(meltQuad(sprite, IN_MIN_PX, IN_MAX_PX, IN_MIN_PX, IN_MAX_PX, yPx, tint));
        // arms
        if (flag(state, MachineFoundryChannelBlock.NORTH))
            quads.add(meltQuad(sprite, IN_MIN_PX, IN_MAX_PX, 0f, IN_MIN_PX, yPx, tint));
        if (flag(state, MachineFoundryChannelBlock.SOUTH))
            quads.add(meltQuad(sprite, IN_MIN_PX, IN_MAX_PX, IN_MAX_PX, 16f, yPx, tint));
        if (flag(state, MachineFoundryChannelBlock.WEST))
            quads.add(meltQuad(sprite, 0f, IN_MIN_PX, IN_MIN_PX, IN_MAX_PX, yPx, tint));
        if (flag(state, MachineFoundryChannelBlock.EAST))
            quads.add(meltQuad(sprite, IN_MAX_PX, 16f, IN_MIN_PX, IN_MAX_PX, yPx, tint));
        return quads;
    }

    /**
     * One horizontal surface quad (CCW seen from above, normal +Y), BLOCK vertex
     * format (8 ints per vertex: position x3, color, uv x2, light, normal) --
     * the same synthesis as the strand caster's "Surface" part. Coordinates are
     * in pixels/16 (block units); the sprite is stretched over each quad
     * (UV 0..1, as the legacy vertex() calls).
     */
    private static BakedQuad meltQuad(TextureAtlasSprite sprite,
                                      float x0Px, float x1Px, float z0Px, float z1Px,
                                      float yPx, int tint) {
        float x0 = x0Px / 16f, x1 = x1Px / 16f;
        float z0 = z0Px / 16f, z1 = z1Px / 16f;
        float y = yPx / 16f;
        // Atlas UVs stored as raw floats (getU0/getU1 semantics are identical on 1.20.1/1.21.1,
        // as in DaeQuadBaker); the sprite is stretched over each quad (UV 0..1, as the legacy
        // vertex() calls with 0/1 texcoords).
        float uA = sprite.getU0(), uB = sprite.getU1();
        float vA = sprite.getV0(), vB = sprite.getV1();

        int[] data = new int[4 * 8];
        // v0(-x,-z) uv(0,0), v1(-x,+z) uv(0,1), v2(+x,+z) uv(1,1), v3(+x,-z) uv(1,0)
        putVertex(data, 0, x0, y, z0, uA, vA, tint);
        putVertex(data, 1, x0, y, z1, uA, vB, tint);
        putVertex(data, 2, x1, y, z1, uB, vB, tint);
        putVertex(data, 3, x1, y, z0, uB, vA, tint);
        return new BakedQuad(data, -1, Direction.UP, sprite, false);
    }

    private static void putVertex(int[] data, int i, float x, float y, float z,
                                  float u, float v, int tint) {
        int off = i * 8;
        data[off] = Float.floatToRawIntBits(x);
        data[off + 1] = Float.floatToRawIntBits(y);
        data[off + 2] = Float.floatToRawIntBits(z);
        data[off + 3] = tint;
        data[off + 4] = Float.floatToRawIntBits(u);
        data[off + 5] = Float.floatToRawIntBits(v);
        data[off + 6] = LightTexture.FULL_BRIGHT; // fullbright 240/240, as the legacy 0xF000F0
        data[off + 7] = NORMAL_UP;
    }

    /** Brightened molten color as a packed RGBA vertex color (r in the lowest byte). */
    private static int moltenTintRgba(MachineFoundryChannelBlockEntity be) {
        // Original: new Color(moltenColor).brighter() -- brightening by 1.43x, clamped.
        int raw = be.type.moltenColor;
        int br = Math.min(255, (raw >> 16 & 0xFF) * 100 / 70);
        int bg = Math.min(255, (raw >> 8 & 0xFF) * 100 / 70);
        int bb = Math.min(255, (raw & 0xFF) * 100 / 70);
        return (255 << 24) | (bb << 16) | (bg << 8) | br;
    }
}
