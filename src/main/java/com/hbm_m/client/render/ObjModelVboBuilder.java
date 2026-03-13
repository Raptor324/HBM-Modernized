package com.hbm_m.client.render;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
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

@OnlyIn(Dist.CLIENT)
public class ObjModelVboBuilder {

    public static SingleMeshVboRenderer.VboData buildSinglePart(BakedModel modelPart) {
        return buildSinglePart(modelPart, "unknown");
    }
    
    public static SingleMeshVboRenderer.VboData buildSinglePart(BakedModel modelPart, String partName) {
        List<BakedQuad> quads = new ArrayList<>();
        
        // Загружаем квады для ВСЕХ направлений + null
        // Важно: не создаём новый RandomSource на каждый вызов. Чтобы сохранить детерминизм как от create(42),
        // сбрасываем seed перед каждым getQuads.
        RandomSource random = RandomSource.create(42);
        random.setSeed(42L);
        List<BakedQuad> nullQuads = modelPart.getQuads(null, null, random, ModelData.EMPTY, RenderType.solid());
        quads.addAll(nullQuads);
        
        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            List<BakedQuad> dirQuads = modelPart.getQuads(null, direction, random, ModelData.EMPTY, RenderType.solid());
            quads.addAll(dirQuads);
        }

        // Если квадов нет - возвращаем null (часть без геометрии, пропускаем без спама)
        if (quads.isEmpty()) {
            MainRegistry.LOGGER.debug("VBO Builder: Part '{}' has NO QUADS, skipping", partName);
            return null;
        }
        
        MainRegistry.LOGGER.debug("VBO Builder: Part '{}' has {} quads (null-face: {}, directional: {})", 
            partName, quads.size(), nullQuads.size(), quads.size() - nullQuads.size());

        final int quadCount = quads.size();
        final int vertexCount = quadCount * 4;
        final int indexCount = quadCount * 6;
        final int floatsPerVertex = 8; // pos(3) + normal(3) + uv(2)
        final int vertexStrideBytes = floatsPerVertex * 4; // 32 bytes

        MainRegistry.LOGGER.debug("=== Building VBO for model, quad count: {} ===", quads.size());

        ByteBuffer vb = null;
        IntBuffer ib = null;
        int indexOffset = 0;

        try {
            vb = MemoryUtil.memAlloc(vertexCount * vertexStrideBytes);
            ib = MemoryUtil.memAllocInt(indexCount);

            for (BakedQuad quad : quads) {
                int[] raw = quad.getVertices();
                if (raw == null || raw.length < 32) {
                    MainRegistry.LOGGER.warn("VBO Builder: Skipping malformed quad in part '{}' (vertex array length = {})",
                            partName, raw == null ? -1 : raw.length);
                    continue;
                }

                // Извлекаем данные из вершинных int-ов
                for (int i = 0; i < 4; i++) {
                    int base = i * 8;

                    // Position (offsets 0, 1, 2)
                    float x = Float.intBitsToFloat(raw[base + 0]);
                    float y = Float.intBitsToFloat(raw[base + 1]);
                    float z = Float.intBitsToFloat(raw[base + 2]);

                    // UV (offsets 4, 5)
                    float u = Float.intBitsToFloat(raw[base + 4]);
                    float v = Float.intBitsToFloat(raw[base + 5]);

                    // Normal (offset 7) - декодируем из packed int
                    // Layout BakedQuad (1.20.1): x,y,z,color,u,v,light,normal
                    int normalPacked = raw[base + 7];
                    float nx = ((byte) (normalPacked & 0xFF)) / 127.0f;
                    float ny = ((byte) ((normalPacked >> 8) & 0xFF)) / 127.0f;
                    float nz = ((byte) ((normalPacked >> 16) & 0xFF)) / 127.0f;

                    // Нормализуем вектор нормали (перестраховка)
                    float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (len > 0.001f) {
                        nx /= len;
                        ny /= len;
                        nz /= len;
                    }

                    // Отладка первой вершины
                    if (indexOffset == 0 && i == 0) {
                        MainRegistry.LOGGER.debug("VBO layout: pos({},{},{}), norm({},{},{}), uv({},{})",
                                x, y, z, nx, ny, nz, u, v);
                    }

                    // Записываем в буфер: Position, Normal, UV
                    vb.putFloat(x).putFloat(y).putFloat(z);
                    vb.putFloat(nx).putFloat(ny).putFloat(nz);
                    vb.putFloat(u).putFloat(v);
                }

                // Индексы для триангуляции квада (0-1-2, 2-3-0)
                ib.put(indexOffset + 0);
                ib.put(indexOffset + 1);
                ib.put(indexOffset + 2);
                ib.put(indexOffset + 2);
                ib.put(indexOffset + 3);
                ib.put(indexOffset + 0);
                indexOffset += 4;
            }

            if (indexOffset == 0) {
                MainRegistry.LOGGER.debug("VBO Builder: Part '{}' produced no valid quads, skipping", partName);
                return null;
            }

            vb.flip();
            ib.flip();

            MainRegistry.LOGGER.debug("VBO allocated: {} vertices, {} indices", vertexCount, indexCount);

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
            MainRegistry.LOGGER.error("Out of memory while building VBO! Vertices: {}, Indices: {}",
                    vertexCount, indexCount, e);
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
