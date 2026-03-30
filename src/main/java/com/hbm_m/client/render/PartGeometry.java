package com.hbm_m.client.render;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.system.MemoryUtil;

import com.hbm_m.main.MainRegistry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Один проход {@link BakedModel#getQuads} для multipart-части: общий список квадов (Iris / putBulkData)
 * и построение {@link com.hbm_m.client.render.SingleMeshVboRenderer.VboData} из тех же квадов.
 */
@OnlyIn(Dist.CLIENT)
public record PartGeometry(List<BakedQuad> solidQuads) {

    public static final long BAKE_SEED = 42L;

    public static final PartGeometry EMPTY = new PartGeometry(List.of());

    public PartGeometry {
        solidQuads = List.copyOf(solidQuads);
    }

    public boolean isEmpty() {
        return solidQuads.isEmpty();
    }

    /**
     * Собирает solid-квады; при отсутствии геометрии возвращает {@link #EMPTY}.
     */
    public static PartGeometry compile(BakedModel modelPart, String partName) {
        if (modelPart == null) {
            return EMPTY;
        }
        List<BakedQuad> quads = collectSolidQuads(modelPart, partName);
        if (quads.isEmpty()) {
            MainRegistry.LOGGER.debug("PartGeometry: Part '{}' has NO QUADS, skipping", partName);
            return EMPTY;
        }
        MainRegistry.LOGGER.debug("PartGeometry: Part '{}' — {} quads", partName, quads.size());
        return new PartGeometry(quads);
    }

    /**
     * Детерминированный обход граней (тот же RNG/seed на каждый вызов getQuads).
     */
    public static List<BakedQuad> collectSolidQuads(BakedModel modelPart, String partName) {
        if (modelPart == null) {
            return List.of();
        }
        List<BakedQuad> quads = new ArrayList<>();
        RandomSource random = RandomSource.create(BAKE_SEED);

        random.setSeed(BAKE_SEED);
        quads.addAll(modelPart.getQuads(null, null, random, ModelData.EMPTY, RenderType.solid()));

        for (Direction direction : Direction.values()) {
            random.setSeed(BAKE_SEED);
            quads.addAll(modelPart.getQuads(null, direction, random, ModelData.EMPTY, RenderType.solid()));
        }

        return quads.isEmpty() ? List.of() : Collections.unmodifiableList(quads);
    }

    /**
     * VBO из уже собранных квадов (без повторного getQuads).
     */
    public SingleMeshVboRenderer.VboData toVboData(String partName) {
        return buildVboDataFromQuads(solidQuads, partName);
    }

    public static SingleMeshVboRenderer.VboData buildVboDataFromQuads(List<BakedQuad> quads, String partName) {
        if (quads == null || quads.isEmpty()) {
            return null;
        }

        final int quadCount = quads.size();
        final int vertexCount = quadCount * 4;
        final int indexCapacity = quadCount * 6;
        final int vertexStrideBytes = 32;

        ByteBuffer vb = null;
        IntBuffer ib = null;
        int indexOffset = 0;

        try {
            vb = MemoryUtil.memAlloc(vertexCount * vertexStrideBytes);
            ib = MemoryUtil.memAllocInt(indexCapacity);

            for (BakedQuad quad : quads) {
                int[] raw = quad.getVertices();
                if (raw == null || raw.length < 32) {
                    MainRegistry.LOGGER.warn("PartGeometry: Skipping malformed quad in part '{}' (vertex array length = {})",
                            partName, raw == null ? -1 : raw.length);
                    continue;
                }

                for (int i = 0; i < 4; i++) {
                    int base = i * 8;

                    float x = Float.intBitsToFloat(raw[base + 0]);
                    float y = Float.intBitsToFloat(raw[base + 1]);
                    float z = Float.intBitsToFloat(raw[base + 2]);

                    float u = Float.intBitsToFloat(raw[base + 4]);
                    float v = Float.intBitsToFloat(raw[base + 5]);

                    int normalPacked = raw[base + 7];
                    float nx = ((byte) (normalPacked & 0xFF)) / 127.0f;
                    float ny = ((byte) ((normalPacked >> 8) & 0xFF)) / 127.0f;
                    float nz = ((byte) ((normalPacked >> 16) & 0xFF)) / 127.0f;

                    float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (len > 0.001f) {
                        nx /= len;
                        ny /= len;
                        nz /= len;
                    }

                    if (indexOffset == 0 && i == 0) {
                        MainRegistry.LOGGER.debug("PartGeometry VBO: pos({},{},{}), norm({},{},{}), uv({},{})",
                                x, y, z, nx, ny, nz, u, v);
                    }

                    vb.putFloat(x).putFloat(y).putFloat(z);
                    vb.putFloat(nx).putFloat(ny).putFloat(nz);
                    vb.putFloat(u).putFloat(v);
                }

                ib.put(indexOffset + 0);
                ib.put(indexOffset + 1);
                ib.put(indexOffset + 2);
                ib.put(indexOffset + 2);
                ib.put(indexOffset + 3);
                ib.put(indexOffset + 0);
                indexOffset += 4;
            }

            if (indexOffset == 0) {
                MainRegistry.LOGGER.debug("PartGeometry: Part '{}' produced no valid quads for VBO", partName);
                return null;
            }

            vb.flip();
            ib.flip();

            MainRegistry.LOGGER.debug("PartGeometry VBO: {} vertices, {} indices", indexOffset, ib.remaining());
            return new SingleMeshVboRenderer.VboData(vb, ib);

        } catch (Exception e) {
            if (vb != null) {
                MemoryUtil.memFree(vb);
            }
            if (ib != null) {
                MemoryUtil.memFree(ib);
            }
            throw e;
        } catch (OutOfMemoryError e) {
            MainRegistry.LOGGER.error("PartGeometry: OOM building VBO, vertices ~ {}", vertexCount, e);
            if (vb != null) {
                MemoryUtil.memFree(vb);
            }
            if (ib != null) {
                MemoryUtil.memFree(ib);
            }
            throw e;
        }
    }
}
