package com.hbm_m.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

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
     * When {@code true}, init has already failed; do not retry or log again.
     */
    protected boolean initFailed = false;

    /**
     * Supplies the one-shot off-heap transfer data consumed (and freed) by
     * {@link #initVbo()}. Overridden by concrete mesh renderers that build
     * through {@link #initVbo()} (default {@code null}: {@code initVbo()} treats
     * it as an error, and mesh types that never call {@code initVbo()} - e.g.
     * {@code InstancedStaticPartRenderer}, which lives in the MDI atlas - are
     * not forced to implement it).
     */
    protected SingleMeshVboRenderer.VboData buildVboData() {
        return null;
    }

    /**
     * Uploads {@link #buildVboData()} to a fresh VAO/VBO(/EBO) and closes the
     * transfer data. Moved verbatim from {@code SingleMeshVboRenderer} so that
     * {@code WeaponPartVboRenderer} can extend {@code AbstractGpuMesh} directly
     * as a sibling of {@code SingleMeshVboRenderer}.
     */
    protected void initVbo() {
        if (initialized) return;

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int previousElementArrayBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);

        SingleMeshVboRenderer.VboData data = null;

        try {
            vaoId = GL30.glGenVertexArrays();
            vboId = GL15.glGenBuffers();

            data = buildVboData();
            if (data == null) {
                MainRegistry.LOGGER.warn("VboData is null, cannot initialize VBO");
                throw new IllegalStateException("VboData is null");
            }
            indexCount = data.indices != null ? data.indices.remaining() : 0;
            setObjBboxFrom(data);

            int vs = data.bytesPerVertex;

            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data.byteBuffer, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, vs, 0);

            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, vs, 12);

            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, vs, 24);

            if (data.indices != null && data.indices.remaining() > 0) {
                eboId = GL15.glGenBuffers();
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data.indices, GL15.GL_STATIC_DRAW);
            }

            GL30.glBindVertexArray(0);

            data.close();

            initialized = true;

        } catch (Exception e) {
            if (data != null) {
                data.close();
            }

            if (vaoId != -1) {
                GL30.glDeleteVertexArrays(vaoId);
                vaoId = -1;
            }
            if (vboId != -1) {
                GL15.glDeleteBuffers(vboId);
                vboId = -1;
            }
            if (eboId != -1) {
                GL15.glDeleteBuffers(eboId);
                eboId = -1;
            }

            throw e;

        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL30.glBindVertexArray(previousVao);

            if (previousVao == 0) {
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, previousElementArrayBuffer);
            }
        }
    }

    /**
     * Releases GL resources (VAO/VBO/EBO) on the render thread.
     * Concrete renderers must call this from their own {@code cleanup()}.
     */
    public void cleanup() {
        if (!initialized) {
            return;
        }

        // Save current ids into locals
        final int vaoToDelete = this.vaoId;
        final int vboToDelete = this.vboId;
        final int eboToDelete = this.eboId;

        // Locally mark as cleaned so nothing is rendered anymore
        this.vaoId = -1;
        this.vboId = -1;
        this.eboId = -1;
        this.indexCount = 0;
        this.initialized = false;
        this.initFailed = false;

        // Delete synchronously when already on the render thread: recordRenderCall
        // ALWAYS queues (both 1.20.1 and 1.21.1) and only replays at flipFrame, so a
        // cleanup during RESOURCE_RELOAD would park the delete until end of frame -
        // after the driver has re-issued the freed numeric IDs to freshly created
        // objects (e.g. the new MdiGeometryAtlas VAO), and the replayed delete kills
        // the live object (native crash in the next MDI draw).
        // Belt and braces: skip any name the live atlas owns RIGHT NOW - a stale
        // queued delete must never reach a re-issued ID.
        Runnable deleter = () -> {
            try {
                if (vboToDelete != -1 && !MdiGeometryAtlas.isLiveAtlasName(vboToDelete)) {
                    GL15.glDeleteBuffers(vboToDelete);
                }
                if (eboToDelete != -1 && !MdiGeometryAtlas.isLiveAtlasName(eboToDelete)) {
                    GL15.glDeleteBuffers(eboToDelete);
                }
                if (vaoToDelete != -1 && !MdiGeometryAtlas.isLiveAtlasName(vaoToDelete)) {
                    GL30.glDeleteVertexArrays(vaoToDelete);
                }
            } catch (Exception e) {
                MainRegistry.LOGGER.error("Error during GPU mesh cleanup", e);
            }
        };
        if (RenderSystem.isOnRenderThread()) {
            deleter.run();
        } else {
            RenderSystem.recordRenderCall(deleter::run);
        }
    }
}

