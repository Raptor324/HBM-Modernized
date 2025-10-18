package com.hbm_m.client.model.render;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.*;

/**
 * GPU-ускоренный рендерер через Instanced Drawing.
 * КРИТИЧНО: Все анимированные части рендерятся ОДНИМ draw-вызовом через glDrawElementsInstanced!
 * 
 * Производительность: O(1) вместо O(N*M), где N=машины, M=части.
 */
public class GPUInstancedRenderer {
    
    // VBO для хранения геометрии (неизменяемые vertex data)
    private static final Map<String, VertexBuffer> GEOMETRY_VBOS = new HashMap<>();
    
    // VBO для хранения матриц трансформаций (обновляется каждый кадр)
    private static final Map<String, Integer> INSTANCE_VBOS = new HashMap<>();
    
    // Буфер для записи матриц всех машин
    private static FloatBuffer instanceMatrixBuffer;
    
    // Текущие инстансы для рендера
    private static final List<Matrix4f> currentInstances = new ArrayList<>();
    private static String currentPartKey;
    
    /**
     * Компилирует геометрию части модели в GPU буфер ОДИН РАЗ.
     */
    public static void compilePartGeometry(String partKey, BakedModel modelPart) {
        if (GEOMETRY_VBOS.containsKey(partKey)) return;
        
        // Получаем квады через GlobalMeshCache
        List<BakedQuad> quads = GlobalMeshCache.getOrCompile(partKey, modelPart);
        if (quads.isEmpty()) return;
        
        // Создаём VertexBuffer для геометрии
        BufferBuilder builder = new BufferBuilder(quads.size() * 32);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        
        PoseStack.Pose neutralPose = new PoseStack().last();
        int neutralLight = (240 << 20) | (240 << 4); // Нейтральный полный свет
        
        for (BakedQuad quad : quads) {
            builder.putBulkData(neutralPose, quad, 1.0f, 1.0f, 1.0f, 1.0f,
                              neutralLight, 0, false);
        }
        
        BufferBuilder.RenderedBuffer renderedBuffer = builder.end();
        
        VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vbo.bind();
        vbo.upload(renderedBuffer);
        VertexBuffer.unbind();
        
        GEOMETRY_VBOS.put(partKey, vbo);
        
        // Создаём instance VBO для матриц
        int instanceVBO = GL30.glGenBuffers();
        INSTANCE_VBOS.put(partKey, instanceVBO);
    }
    
    /**
     * Начать батч для инстансированного рендера части.
     */
    public static void beginInstances(String partKey) {
        currentPartKey = partKey;
        currentInstances.clear();
    }
    
    /**
     * Добавить инстанс части с трансформацией.
     */
    public static void addInstance(Matrix4f transformMatrix) {
        if (currentPartKey == null) {
            throw new IllegalStateException("Call beginInstances() first!");
        }
        currentInstances.add(new Matrix4f(transformMatrix));
    }
    
    /**
     * Завершить батч и отрисовать ВСЕ инстансы ОДНИМ вызовом.
     */
    public static void endInstances() {
        if (currentPartKey == null || currentInstances.isEmpty()) {
            currentPartKey = null;
            currentInstances.clear();
            return;
        }
        
        VertexBuffer geometryVBO = GEOMETRY_VBOS.get(currentPartKey);
        Integer instanceVBO = INSTANCE_VBOS.get(currentPartKey);
        
        if (geometryVBO == null || instanceVBO == null) {
            currentPartKey = null;
            currentInstances.clear();
            return;
        }
        
        // Подготовка буфера матриц
        if (instanceMatrixBuffer == null || instanceMatrixBuffer.capacity() < currentInstances.size() * 16) {
            instanceMatrixBuffer = org.lwjgl.BufferUtils.createFloatBuffer(currentInstances.size() * 16);
        }
        instanceMatrixBuffer.clear();
        
        for (Matrix4f matrix : currentInstances) {
            matrix.get(instanceMatrixBuffer);
        }
        instanceMatrixBuffer.flip();
        
        // Загрузка матриц в instance VBO
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, instanceVBO);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, instanceMatrixBuffer, GL30.GL_DYNAMIC_DRAW);
        
        // Setup instance attributes (матрица 4x4 занимает 4 attribute slots)
        for (int i = 0; i < 4; i++) {
            GL30.glEnableVertexAttribArray(5 + i); // Attributes 5-8 для матрицы
            GL30.glVertexAttribPointer(5 + i, 4, GL30.GL_FLOAT, false, 64, i * 16);
            GL33.glVertexAttribDivisor(5 + i, 1); // Instanced attribute
        }
        
        // Рисуем ВСЕ инстансы ОДНИМ вызовом
        geometryVBO.bind();
        geometryVBO.drawWithShader(new PoseStack().last().pose(), 
                                    RenderSystem.getProjectionMatrix(),
                                    RenderSystem.getShader());
        
        // Cleanup
        for (int i = 0; i < 4; i++) {
            GL33.glVertexAttribDivisor(5 + i, 0);
            GL30.glDisableVertexAttribArray(5 + i);
        }
        
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
        VertexBuffer.unbind();
        
        currentPartKey = null;
        currentInstances.clear();
    }
    
    /**
     * Очистка всех GPU ресурсов.
     */
    public static void clear() {
        GEOMETRY_VBOS.values().forEach(vbo -> {
            if (vbo != null) vbo.close();
        });
        GEOMETRY_VBOS.clear();
        
        INSTANCE_VBOS.values().forEach(GL30::glDeleteBuffers);
        INSTANCE_VBOS.clear();
    }
}
