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

//? if forge {
import net.minecraftforge.client.model.data.ModelData;
//?} elif neoforge {
/*import net.neoforged.neoforge.client.model.data.ModelData;
*///?}


/**
 * Один проход {@link BakedModel#getQuads} для multipart-части: общий список квадов (Iris / putBulkData)
 * и построение {@link com.hbm_m.client.render.SingleMeshVboRenderer.VboData} из тех же квадов.
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
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
        // Разворачиваем FRAPI-обёртки (Continuity, Emissive и т.д.) если они есть на данной части
        modelPart = AbstractPartBasedRenderer.unwrapFabricForwardingModels(modelPart);
        List<BakedQuad> quads = collectSolidQuads(modelPart, partName);
        if (quads.isEmpty()) {
            MainRegistry.LOGGER.debug("PartGeometry: Part '{}' has NO QUADS, skipping", partName);
            return EMPTY;
        }
        MainRegistry.LOGGER.debug("PartGeometry: Part '{}' - {} quads", partName, quads.size());
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
        //? if forge || neoforge {
        quads.addAll(modelPart.getQuads(null, null, random, ModelData.EMPTY, RenderType.solid()));
        //?}
        //? if fabric {
        /*quads.addAll(modelPart.getQuads(null, null, random));
        *///?}

        for (Direction direction : Direction.values()) {
            random.setSeed(BAKE_SEED);
            //? if forge || neoforge {
            quads.addAll(modelPart.getQuads(null, direction, random, ModelData.EMPTY, RenderType.solid()));
            //?}
        }

        return quads.isEmpty() ? List.of() : Collections.unmodifiableList(quads);
    }

    /**
     * VBO из уже собранных квадов (без повторного getQuads).
     * {@code bone_id} в вершинах = 0 (статика / кольцо).
     */
    public SingleMeshVboRenderer.VboData toVboData(String partName) {
        return buildVboDataFromQuads(solidQuads, partName, 0);
    }

    /**
     * @param perVertexBoneId индекс кости для instanced GPU skinning (0=base, 1=lower arm, …);
     *                        см. {@code MachineAdvancedAssemblerRenderer} и {@code block_lit.vsh}.
     */
    public SingleMeshVboRenderer.VboData toVboData(String partName, int perVertexBoneId) {
        return buildVboDataFromQuads(solidQuads, partName, perVertexBoneId);
    }

    public static SingleMeshVboRenderer.VboData buildVboDataFromQuads(List<BakedQuad> quads, String partName) {
        return buildVboDataFromQuads(quads, partName, 0);
    }

    public static SingleMeshVboRenderer.VboData buildVboDataFromQuads(List<BakedQuad> quads, String partName, int perVertexBoneId) {
        if (quads == null || quads.isEmpty()) {
            return null;
        }

        final int quadCount = quads.size();
        final int vertexCount = quadCount * 4;
        final int indexCapacity = quadCount * 6;
        final int vertexStrideBytes = SingleMeshVboRenderer.MACHINE_PART_VERTEX_STRIDE_BYTES;

        ByteBuffer vb = null;
        IntBuffer ib = null;
        int indexOffset = 0;

        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

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

                    if (x < minX) minX = x; if (x > maxX) maxX = x;
                    if (y < minY) minY = y; if (y > maxY) maxY = y;
                    if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;

                    float u = Float.intBitsToFloat(raw[base + 4]);
                    float v = Float.intBitsToFloat(raw[base + 5]);

                    int normalPacked = raw[base + 7];
                    float nx = ((byte) (normalPacked & 0xFF)) / 127.0f;
                    float ny = ((byte) ((normalPacked >> 8) & 0xFF)) / 127.0f;
                    float nz = ((byte) ((normalPacked >> 16) & 0xFF)) / 127.0f;

                    // ОПТИМИЗАЦИЯ: Math.sqrt вызывается только если вектор нормали денормализован
                    float lenSq = nx * nx + ny * ny + nz * nz;
                    if (lenSq > 1e-6f && Math.abs(lenSq - 1.0f) > 1e-3f) {
                        float invLen = (float) (1.0 / Math.sqrt(lenSq));
                        nx *= invLen;
                        ny *= invLen;
                        nz *= invLen;
                    }

                    vb.putFloat(x).putFloat(y).putFloat(z);
                    vb.putFloat(nx).putFloat(ny).putFloat(nz);
                    vb.putFloat(u).putFloat(v);
                    vb.putInt(perVertexBoneId);
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
                MemoryUtil.memFree(vb);
                MemoryUtil.memFree(ib);
                return null;
            }

            vb.flip();
            ib.flip();

            if (!Float.isFinite(minX)) {
                minX = minY = minZ = 0f;
                maxX = maxY = maxZ = 0f;
            }

            MainRegistry.LOGGER.debug("PartGeometry VBO: {} vertices, {} indices, bbox min({},{},{}) max({},{},{})",
                    indexOffset, ib.remaining(), minX, minY, minZ, maxX, maxY, maxZ);
            return new SingleMeshVboRenderer.VboData(vb, ib, minX, minY, minZ, maxX, maxY, maxZ, vertexStrideBytes);

        } catch (Exception | OutOfMemoryError e) {
            if (vb != null) MemoryUtil.memFree(vb);
            if (ib != null) MemoryUtil.memFree(ib);
            throw e;
        }
    }
}
