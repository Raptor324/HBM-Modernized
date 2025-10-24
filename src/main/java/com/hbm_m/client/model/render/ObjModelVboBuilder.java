package com.hbm_m.client.model.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.lwjgl.system.MemoryUtil;
import com.hbm_m.main.MainRegistry;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ObjModelVboBuilder {

    public static AbstractGpuVboRenderer.VboData buildSinglePart(BakedModel modelPart) {
        List<BakedQuad> quads = new ArrayList<>();
        
        // ✅ ИСПРАВЛЕНИЕ 1: Загружаем квады для ВСЕХ направлений + null
        quads.addAll(modelPart.getQuads(null, null, RandomSource.create(42), ModelData.EMPTY, RenderType.solid()));
        for (Direction direction : Direction.values()) {
            quads.addAll(modelPart.getQuads(null, direction, RandomSource.create(42), ModelData.EMPTY, RenderType.solid()));
        }
        
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int indexOffset = 0;
        
        MainRegistry.LOGGER.debug("=== Building VBO for model, quad count: {} ===", quads.size());
        
        for (BakedQuad quad : quads) {
            int[] raw = quad.getVertices();
            
            // ✅ ИСПРАВЛЕНИЕ 2: Извлекаем нормали из вершинных данных
            for (int i = 0; i < 4; i++) {
                int base = i * 8;
                
                // Position (offsets 0, 1, 2)
                float x = Float.intBitsToFloat(raw[base + 0]);
                float y = Float.intBitsToFloat(raw[base + 1]);
                float z = Float.intBitsToFloat(raw[base + 2]);
                
                // UV (offsets 4, 5)
                float u = Float.intBitsToFloat(raw[base + 4]);
                float v = Float.intBitsToFloat(raw[base + 5]);
                
                // ✅ Normal (offset 6) - декодируем из packed int
                int normalPacked = raw[base + 6];
                float nx = ((byte) (normalPacked & 0xFF)) / 127.0f;
                float ny = ((byte) ((normalPacked >> 8) & 0xFF)) / 127.0f;
                float nz = ((byte) ((normalPacked >> 16) & 0xFF)) / 127.0f;
                
                // Нормализуем вектор нормали
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0.001f) {
                    nx /= len;
                    ny /= len;
                    nz /= len;
                }
                
                // ✅ Отладка первой вершины
                if (indexOffset == 0 && i == 0) {
                    MainRegistry.LOGGER.debug("VBO layout: pos({},{},{}), norm({},{},{}), uv({},{})",
                        x, y, z, nx, ny, nz, u, v);
                }
                
                // Записываем в буфер: Position, Normal, UV
                vertices.add(x); vertices.add(y); vertices.add(z);       // 0,1,2
                vertices.add(nx); vertices.add(ny); vertices.add(nz);    // 3,4,5
                vertices.add(u); vertices.add(v);                        // 6,7
            }
            
            // ✅ Индексы для триангуляции квада (0-1-2, 2-3-0)
            indices.add(indexOffset + 0);
            indices.add(indexOffset + 1);
            indices.add(indexOffset + 2);
            indices.add(indexOffset + 2);
            indices.add(indexOffset + 3);
            indices.add(indexOffset + 0);
            
            indexOffset += 4;
        }
        
        // ✅ Аллоцируем буферы
        ByteBuffer vb = MemoryUtil.memAlloc(vertices.size() * 4);
        for (float f : vertices) vb.putFloat(f);
        vb.flip();
        
        IntBuffer ib = MemoryUtil.memAllocInt(indices.size());
        for (int idx : indices) ib.put(idx);
        ib.flip();
        
        return new AbstractGpuVboRenderer.VboData(vb, ib);
    }
}
