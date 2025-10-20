package com.hbm_m.client.model.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class ObjModelVboBuilder {
    public static AbstractGpuVboRenderer.VboData buildSinglePart(BakedModel modelPart) {
        List<BakedQuad> quads = modelPart.getQuads(null, null, RandomSource.create(42), ModelData.EMPTY, RenderType.solid());
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int indexOffset = 0;

        for (BakedQuad quad : quads) {
            int[] raw = quad.getVertices();
            Direction face = quad.getDirection();
            float nx = face.getStepX();
            float ny = face.getStepY();
            float nz = face.getStepZ();

            for (int i = 0; i < 4; i++) {
                int base = i * 8;
                float x = Float.intBitsToFloat(raw[base + 0]);
                float y = Float.intBitsToFloat(raw[base + 1]);
                float z = Float.intBitsToFloat(raw[base + 2]);
                float u = Float.intBitsToFloat(raw[base + 4]); // UV0.u
                float v = Float.intBitsToFloat(raw[base + 5]); // UV0.v

                vertices.add(x); vertices.add(y); vertices.add(z);      // Position
                vertices.add(nx); vertices.add(ny); vertices.add(nz);   // Normal (by face)
                vertices.add(u); vertices.add(v);                       // UV0
            }

            indices.add(indexOffset + 0);
            indices.add(indexOffset + 1);
            indices.add(indexOffset + 2);
            indices.add(indexOffset + 2);
            indices.add(indexOffset + 3);
            indices.add(indexOffset + 0);
            indexOffset += 4;
        }

        ByteBuffer vb = MemoryUtil.memAlloc(vertices.size() * 4);
        for (float f : vertices) vb.putFloat(f);
        vb.flip();

        IntBuffer ib = MemoryUtil.memAllocInt(indices.size());
        for (int idx : indices) ib.put(idx);
        ib.flip();

        return new AbstractGpuVboRenderer.VboData(vb, ib);
    }
}
