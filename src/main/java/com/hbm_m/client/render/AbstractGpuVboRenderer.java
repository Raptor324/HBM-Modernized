package com.hbm_m.client.render;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractGpuVboRenderer {

    protected int vaoId = -1;
    protected int vboId = -1;
    protected int eboId = -1;
    protected int indexCount = 0;
    protected boolean initialized = false;
    protected abstract VboData buildVboData();


    static final class TextureBinder {
        static void bindForModelIfNeeded(ShaderInstance shader) {
            var minecraft = Minecraft.getInstance();
            var textureManager = minecraft.getTextureManager();
            
            //  Только текстура атласа, без lightmap
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            var blockAtlas = textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS);
            int textureId = blockAtlas.getId();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }
    }

    /**
     * Проверка видимости перед рендерингом с использованием occlusion culling
     */
    private boolean shouldRenderWithCulling(BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return false;
        }
    
        // Используем продвинутый occlusion culling
        AABB renderBounds = blockEntity.getRenderBoundingBox();
        return OcclusionCullingHelper.shouldRender(blockPos, blockEntity.getLevel(), renderBounds);
    }

    protected void initVbo() {
        if (initialized) return;
    
        // Сохраняем текущее состояние OpenGL перед инициализацией
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int previousElementArrayBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
    
        VboData data = null;
        
        try {
            // Генерируем идентификаторы для VAO и VBO
            vaoId = GL30.glGenVertexArrays();
            vboId = GL15.glGenBuffers();
    
            // Билдим данные для VBO
            data = buildVboData();
            if (data == null) {
                MainRegistry.LOGGER.warn("VboData is null, cannot initialize VBO");
                throw new IllegalStateException("VboData is null");
            }
            indexCount = data.indices != null ? data.indices.remaining() : 0;
    
            // Конфигурируем VAO
            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data.byteBuffer, GL15.GL_STATIC_DRAW);
    
            // Атрибут 0: Position (3 float, offset 0)
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 32, 0);
    
            // Атрибут 1: Normal (3 float, offset 12)
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 32, 12);
    
            // Атрибут 2: TexCoord (2 float, offset 24)
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 32, 24);
    
            // Если есть индексы, создаем EBO
            if (data.indices != null && data.indices.remaining() > 0) {
                eboId = GL15.glGenBuffers();
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data.indices, GL15.GL_STATIC_DRAW);
                //  НЕ отвязываем EBO здесь - он должен остаться частью VAO!
            }
    
            //  КРИТИЧНО: Отвязываем VAO ПЕРЕД восстановлением EBO
            GL30.glBindVertexArray(0);
    
            // Освобождаем нативную память
            MemoryUtil.memFree(data.byteBuffer);
            if (data.indices != null) {
                MemoryUtil.memFree(data.indices);
            }
    
            initialized = true;
    
        } catch (Exception e) {
            // В случае ошибки освобождаем ресурсы
            if (data != null) {
                if (data.byteBuffer != null) {
                    MemoryUtil.memFree(data.byteBuffer);
                }
                if (data.indices != null) {
                    MemoryUtil.memFree(data.indices);
                }
            }
            
            // Удаляем созданные GL объекты при ошибке
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
            //  КРИТИЧНО: Сначала восстанавливаем VBO, потом VAO, потом EBO
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL30.glBindVertexArray(previousVao);
            
            //  EBO восстанавливаем ПОСЛЕ отвязки VAO
            // Если previousVao != 0, то его EBO восстановится автоматически
            // Если previousVao == 0, восстанавливаем явно
            if (previousVao == 0) {
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, previousElementArrayBuffer);
            }
        }
    }

    private void applyTextureFilterBasedOnDistance(BlockPos blockPos) {
        var minecraft = Minecraft.getInstance();
        var camera = minecraft.gameRenderer.getMainCamera();
        var cameraPos = camera.getPosition();
        
        // Вычисляем расстояние от камеры до блока
        double dx = blockPos.getX() + 0.5 - cameraPos.x;
        double dy = blockPos.getY() + 0.5 - cameraPos.y;
        double dz = blockPos.getZ() + 0.5 - cameraPos.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        //  Конвертируем чанки в блоки (1 чанк = 16 блоков)
        int thresholdChunks = ModClothConfig.get().modelUpdateDistance;
        double thresholdBlocks = thresholdChunks * 16.0;
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        
        if (distance > thresholdBlocks) {
            // На большом расстоянии - размытая текстура (LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        } else {
            // Вблизи - попиксельная текстура (NEAREST)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos blockPos) {
        render(poseStack, packedLight, blockPos, null);
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos blockPos, 
                    @Nullable BlockEntity blockEntity) {
        //  CULLING CHECK - ранний выход
        if (!shouldRenderWithCulling(blockPos, blockEntity)) {
            return;
        }
        if (!initialized) {
            try {
                initVbo();
            } catch (Exception e) {
                MainRegistry.LOGGER.error("Failed to initialize VBO", e);
                vaoId = -1;
                vboId = -1;
                eboId = -1;
                return;
            }
        }
        if (!initialized || vaoId <= 0 || vboId <= 0) {
            return;
        }

        if (eboId <= 0 || indexCount <= 0) {
            return;
        }
    
        ShaderInstance shader = ModShaders.getBlockLitShader();
        if (shader == null) {
            return;
        }
    
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int previousCullFace = GL11.glGetInteger(GL11.GL_CULL_FACE);
        
        //  КРИТИЧНО: Сохраняем ТЕКУЩИЕ texture parameters ПЕРЕД их изменением!
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int previousMinFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
        int previousMagFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);

        try {
            RenderSystem.setShader(() -> shader);
            if (shader.MODEL_VIEW_MATRIX != null)
                shader.MODEL_VIEW_MATRIX.set(poseStack.last().pose());
            if (shader.PROJECTION_MATRIX != null)
                shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
    
            float brightness = calculateBrightness(packedLight);
            var brightnessUniform = shader.getUniform("Brightness");
            if (brightnessUniform != null) {
                brightnessUniform.set(brightness);
            }
    
            var useInstancingUniform = shader.getUniform("UseInstancing");
            if (useInstancingUniform != null) {
                useInstancingUniform.set(0);
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
            
            //  Применяем фильтр на основе расстояния
            applyTextureFilterBasedOnDistance(blockPos);
            
            var sampler0 = shader.getUniform("Sampler0");
            if (sampler0 != null) sampler0.set(0);

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            GL11.glDisable(GL11.GL_CULL_FACE);
    
            GL30.glBindVertexArray(vaoId);
            GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
    
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during VBO render", e);
        } finally {

            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            GL30.glBindVertexArray(previousVao);
            
            // Восстанавливаем culling
            if (previousCullFace == GL11.GL_TRUE) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            }
            
            RenderSystem.setShader(() -> null);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, previousMinFilter);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, previousMagFilter);
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
        
        //  Применяем к sky light
        float effectiveSkyLight = skyLight * skyBrightness;
        
        //  Берём максимум из block и modified sky
        float maxLight = Math.max(blockLight, effectiveSkyLight);
        
        //  Нормализуем [0.05, 1.0]
        float brightness = 0.05f + (maxLight / 15.0f) * 0.95f;
        
        return brightness;
    }    

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
        this.initialized = false;

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
                MainRegistry.LOGGER.error("Error during VBO cleanup", e);
            }
        });
    }

    public static class VboData {
        public final ByteBuffer byteBuffer;
        public final IntBuffer indices;

        public VboData(ByteBuffer byteBuffer, IntBuffer indices) {
            this.byteBuffer = byteBuffer;
            this.indices = indices;
        }
    }
}
