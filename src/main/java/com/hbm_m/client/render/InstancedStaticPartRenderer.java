package com.hbm_m.client.render;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Instanced Renderer для статических частей (Base/Frame).
 * Рендерит все машины одного типа одним draw call через glDrawElementsInstanced.
 */
@OnlyIn(Dist.CLIENT)
public class InstancedStaticPartRenderer extends AbstractGpuVboRenderer {
    private static final int MAX_INSTANCES = 1024;

    private final List<Matrix4f> nearInstanceTransforms = new ArrayList<>();
    private final List<Integer> nearInstanceLights = new ArrayList<>();
    
    private final List<Matrix4f> farInstanceTransforms = new ArrayList<>();
    private final List<Integer> farInstanceLights = new ArrayList<>();
    
    private int instanceVboId = -1; // VBO для instance attributes
    private FloatBuffer instanceBuffer;

    public InstancedStaticPartRenderer(VboData data) {
        if (data == null) {
            MainRegistry.LOGGER.error("InstancedStaticPartRenderer received null VboData!");
            initialized = false;
            return;
        }

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

        try {
            vaoId = GL30.glGenVertexArrays();
            vboId = GL15.glGenBuffers();

            if (vaoId == 0 || vboId == 0) {
                throw new IllegalStateException("Failed to generate VAO/VBO!");
            }

            indexCount = data.indices != null ? data.indices.remaining() : 0;

            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data.byteBuffer, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 32, 0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 32, 12);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 32, 24);

            if (data.indices != null && data.indices.remaining() > 0) {
                eboId = GL15.glGenBuffers();
                if (eboId == 0) {
                    throw new IllegalStateException("Failed to generate EBO!");
                }
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data.indices, GL15.GL_STATIC_DRAW);
            }

            instanceVboId = GL15.glGenBuffers();
            if (instanceVboId == 0) {
                throw new IllegalStateException("Failed to generate instance VBO!");
            }

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) MAX_INSTANCES * 17 * 4, GL15.GL_STREAM_DRAW);

            int stride = 17 * 4;
            for (int i = 0; i < 4; i++) {
                int loc = 3 + i;
                GL20.glEnableVertexAttribArray(loc);
                GL20.glVertexAttribPointer(loc, 4, GL11.GL_FLOAT, false, stride, (long) i * 16);
                GL33.glVertexAttribDivisor(loc, 1);
            }

            GL20.glEnableVertexAttribArray(7);
            GL20.glVertexAttribPointer(7, 1, GL11.GL_FLOAT, false, stride, 16 * 4);
            GL33.glVertexAttribDivisor(7, 1);

            //  ОТВЯЗЫВАЕМ VAO ВНУТРИ TRY
            GL30.glBindVertexArray(0);

            instanceBuffer = MemoryUtil.memAllocFloat(MAX_INSTANCES * 17);

            MemoryUtil.memFree(data.byteBuffer);
            if (data.indices != null) {
                MemoryUtil.memFree(data.indices);
            }

            initialized = true;

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to initialize InstancedStaticPartRenderer", e);

            if (instanceBuffer != null) {
                MemoryUtil.memFree(instanceBuffer);
                instanceBuffer = null;
            }

            if (instanceVboId != -1) {
                GL15.glDeleteBuffers(instanceVboId);
                instanceVboId = -1;
            }

            if (eboId != -1) {
                GL15.glDeleteBuffers(eboId);
                eboId = -1;
            }

            if (vboId != -1) {
                GL15.glDeleteBuffers(vboId);
                vboId = -1;
            }

            if (vaoId != -1) {
                GL30.glDeleteVertexArrays(vaoId);
                vaoId = -1;
            }

            initialized = false;

        } finally {
            //  ТОЛЬКО восстанавливаем VBO и VAO, не трогаем EBO
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL30.glBindVertexArray(previousVao);
        }
    }

    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos) {
        addInstance(poseStack, packedLight, blockPos, null);
    }

    /**
     * Добавить инстанс для батча
     */
    public void addInstance(PoseStack poseStack, int packedLight, BlockPos blockPos,
                        @Nullable BlockEntity blockEntity) {
        var minecraft = Minecraft.getInstance();
        var camera = minecraft.gameRenderer.getMainCamera();
        var cameraPos = camera.getPosition();
        
        double dx = blockPos.getX() + 0.5 - cameraPos.x;
        double dy = blockPos.getY() + 0.5 - cameraPos.y;
        double dz = blockPos.getZ() + 0.5 - cameraPos.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        int thresholdChunks = ModClothConfig.get().modelUpdateDistance;
        double thresholdBlocks = thresholdChunks * 16.0;
        
        Matrix4f transform = new Matrix4f(poseStack.last().pose());
        
        if (distance > thresholdBlocks) {
            if (farInstanceTransforms.size() >= MAX_INSTANCES) {
                flushFarBatch();
            }
            farInstanceTransforms.add(transform);
            farInstanceLights.add(packedLight);
        } else {
            if (nearInstanceTransforms.size() >= MAX_INSTANCES) {
                flushNearBatch();
            }
            nearInstanceTransforms.add(transform);
            nearInstanceLights.add(packedLight);
        }
    }

    /**
     * Отрендерить все накопленные инстансы одним draw call
     */
    public void flush() {
        flushNearBatch();
        flushFarBatch();
    }
    
    private void flushNearBatch() {
        if (nearInstanceTransforms.isEmpty() || !initialized || vaoId == -1 || eboId == -1) {
            nearInstanceTransforms.clear();
            nearInstanceLights.clear();
            return;
        }
        
        flushBatchWithFilter(nearInstanceTransforms, nearInstanceLights, false);
        nearInstanceTransforms.clear();
        nearInstanceLights.clear();
    }
    
    private void flushFarBatch() {
        if (farInstanceTransforms.isEmpty() || !initialized || vaoId == -1 || eboId == -1) {
            farInstanceTransforms.clear();
            farInstanceLights.clear();
            return;
        }
        
        flushBatchWithFilter(farInstanceTransforms, farInstanceLights, true);
        farInstanceTransforms.clear();
        farInstanceLights.clear();
    }
    
    private void flushBatchWithFilter(List<Matrix4f> transforms, List<Integer> lights, boolean useLinearFilter) {
        ShaderInstance shader = ModShaders.getBlockLitShader();
        if (shader == null) {
            return;
        }
        
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int previousCullFace = GL11.glGetInteger(GL11.GL_CULL_FACE);
        
        try {
            instanceBuffer.clear();
            for (int i = 0; i < transforms.size(); i++) {
                Matrix4f mat = transforms.get(i);
                int light = lights.get(i);
                mat.get(instanceBuffer);
                instanceBuffer.position(instanceBuffer.position() + 16);
                float brightness = calculateBrightness(light);
                instanceBuffer.put(brightness);
            }
            instanceBuffer.flip();
            
            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceBuffer);
            
            RenderSystem.setShader(() -> shader);
            if (shader.PROJECTION_MATRIX != null) {
                shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
            }
            
            var useInstancingUniform = shader.getUniform("UseInstancing");
            if (useInstancingUniform != null) {
                useInstancingUniform.set(1);
            }
            
            var fogStartUniform = shader.getUniform("FogStart");
            if (fogStartUniform != null) fogStartUniform.set(RenderSystem.getShaderFogStart());
            var fogEndUniform = shader.getUniform("FogEnd");
            if (fogEndUniform != null) fogEndUniform.set(RenderSystem.getShaderFogEnd());
            var fogColorUniform = shader.getUniform("FogColor");
            if (fogColorUniform != null) {
                float[] fogColor = RenderSystem.getShaderFogColor();
                fogColorUniform.set(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
            }
            
            shader.apply();
            TextureBinder.bindForModelIfNeeded(shader);
            var sampler0 = shader.getUniform("Sampler0");
            if (sampler0 != null) sampler0.set(0);
            var sampler2 = shader.getUniform("Sampler2");
            if (sampler2 != null) sampler2.set(2);
            
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            
            //  Применяем нужный фильтр для этого батча
            if (useLinearFilter) {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            } else {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            }
            
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LESS);
            RenderSystem.depthMask(true);
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(1.0f, 1.0f);

            GL11.glDisable(GL11.GL_CULL_FACE);
            
            if (indexCount > 0) {
                GL31.glDrawElementsInstanced(
                    GL11.GL_TRIANGLES,
                    indexCount,
                    GL11.GL_UNSIGNED_INT,
                    0,
                    transforms.size()
                );
            }
            
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during instanced flush", e);
        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL30.glBindVertexArray(previousVao);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glEnable(GL11.GL_CULL_FACE);
            if (previousCullFace == GL11.GL_TRUE) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            }
        }
    }

    private float calculateBrightness(int packedLight) {
        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return Math.max(0.05f, Math.max(blockLight, skyLight) / 15.0f);
        }

        float skyDarken = level.getSkyDarken(1.0f);
        
        float skyBrightness = 0.05f + (skyDarken * 0.95f);
        
        // Применяем к sky light
        float effectiveSkyLight = skyLight * skyBrightness;
        
        // Берём максимум из block и modified sky
        float maxLight = Math.max(blockLight, effectiveSkyLight);
        
        // Нормализуем [0.05, 1.0]
        float brightness = 0.05f + (maxLight / 15.0f) * 0.95f;
        
        return brightness;
    }    

    @Override
    protected VboData buildVboData() {
        return null; // Не используется
    }

    @Override
    public void cleanup() {
        super.cleanup();
        if (instanceVboId != -1) {
            GL15.glDeleteBuffers(instanceVboId);
            instanceVboId = -1;
        }
        if (instanceBuffer != null) {
            MemoryUtil.memFree(instanceBuffer);
            instanceBuffer = null;
        }
    }
}
