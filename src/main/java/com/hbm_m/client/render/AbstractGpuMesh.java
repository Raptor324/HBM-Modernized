package com.hbm_m.client.render;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractGpuMesh {

    protected int vaoId = -1;
    protected int vboId = -1;
    protected int eboId = -1;
    protected int indexCount = 0;
    protected boolean initialized = false;

    /**
     * Object-space AABB of this mesh: {minX, minY, minZ, maxX, maxY, maxZ}.
     * Populated from {@link SingleMeshVboRenderer.VboData} when the mesh is built.
     * Used for the 8-corner trilinear lightmap sampling so the shader can interpolate
     * per-vertex brightness from corner samples taken at the bbox' world-space corners.
     */
    protected final float[] objBbox = new float[] { 0f, 0f, 0f, 1f, 1f, 1f };

    public float[] getObjBbox() {
        return objBbox;
    }

    protected void setObjBboxFrom(SingleMeshVboRenderer.VboData data) {
        if (data == null) return;
        objBbox[0] = data.minX; objBbox[1] = data.minY; objBbox[2] = data.minZ;
        objBbox[3] = data.maxX; objBbox[4] = data.maxY; objBbox[5] = data.maxZ;
    }
    /**
     * При true - init уже провалился, не пытаемся снова и не логируем
     */
    protected boolean initFailed = false;

    /**
     * Освобождение GL-ресурсов (VAO/VBO/EBO) на render-треде.
     * Конкретные рендереры должны вызывать этот метод из своего cleanup().
     */
    public void cleanup() {
        if (!initialized) {
            return;
        }

        // Сохраняем текущие id в локальные переменные
        final int vaoToDelete = this.vaoId;
        final int vboToDelete = this.vboId;
        final int eboToDelete = this.eboId;

        // Локально помечаем как очищенные, чтобы больше не рендерить
        this.vaoId = -1;
        this.vboId = -1;
        this.eboId = -1;
        this.indexCount = 0;
        this.initialized = false;
        this.initFailed = false;

        // Планируем реальные GL-вызовы на рендер-тред
        RenderSystem.recordRenderCall(() -> {
            try {
                if (vboToDelete != -1) {
                    GL15.glDeleteBuffers(vboToDelete);
                }
                if (eboToDelete != -1) {
                    GL15.glDeleteBuffers(eboToDelete);
                }
                if (vaoToDelete != -1) {
                    GL30.glDeleteVertexArrays(vaoToDelete);
                }
            } catch (Exception e) {
                MainRegistry.LOGGER.error("Error during GPU mesh cleanup", e);
            }
        });
    }
}

