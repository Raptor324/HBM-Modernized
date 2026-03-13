package com.hbm_m.client.render;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
import java.util.List;

@OnlyIn(Dist.CLIENT)
public abstract class SingleMeshVboRenderer extends AbstractGpuMesh {

    protected abstract VboData buildVboData();

    /**
     * Квады для Iris-совместимого пути (BufferBuilder + GameRenderer shader).
     * Переопределяется в рендерерах, созданных через GlobalMeshCache.
     */
    protected List<BakedQuad> getQuadsForIrisPath() {
        return null;
    }

    static final class TextureBinder {
        static void bindForModelIfNeeded(ShaderInstance shader) {
            var minecraft = Minecraft.getInstance();
            var textureManager = minecraft.getTextureManager();

            //  Только текстура атласа, без lightmap
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            var blockAtlas = textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS);
            RenderSystem.bindTexture(blockAtlas.getId());
        }
    }

    /**
     * Проверка видимости перед рендерингом с использованием occlusion culling.
     * Если BlockEntity недоступен, считаем, что объект нужно рендерить
     * (нельзя корректно проверить видимость без информации о мире и границах).
     */
    private boolean shouldRenderWithCulling(BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.getLevel() == null) {
            // Нет данных для продвинутого occlusion culling → не отбрасываем меш
            return true;
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

            // Освобождаем нативную память VboData через единый ownership-метод
            data.close();

            initialized = true;

        } catch (Exception e) {
            // В случае ошибки освобождаем нативную память VboData
            if (data != null) {
                data.close();
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

    private void renderToBufferSource(PoseStack poseStack, int packedLight, List<BakedQuad> quads, MultiBufferSource bufferSource) {
        if (quads == null || quads.isEmpty() || bufferSource == null) return;
        var consumer = bufferSource.getBuffer(RenderType.solid());
        var pose = poseStack.last();
        // При использовании bufferSource мы НЕ вызываем endBatch(), движок сам отрисует когда надо
        for (BakedQuad quad : quads) {
            // Используем стандартный putBulkData, он корректно работает с Iris
            consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, packedLight, OverlayTexture.NO_OVERLAY, false);
        }
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos blockPos) {
        render(poseStack, packedLight, blockPos, null, null);
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos blockPos,
                       @Nullable BlockEntity blockEntity) {
        render(poseStack, packedLight, blockPos, blockEntity, null);
    }

    public void render(PoseStack poseStack, int packedLight, BlockPos blockPos,
                       @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        if (!shouldRenderWithCulling(blockPos, blockEntity)) return;

        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            List<BakedQuad> irisQuads = getQuadsForIrisPath();
            // Если у нас есть квады и буфер - рисуем через ванильный пайплайн
            if (irisQuads != null && bufferSource != null) {
                renderToBufferSource(poseStack, packedLight, irisQuads, bufferSource);
            }
            // И ВЫХОДИМ, чтобы не трогать GL напрямую
            return;
        }

        if (!initialized && !initFailed) {
            try {
                initVbo();
            } catch (Exception e) {
                initFailed = true;
                MainRegistry.LOGGER.debug("VBO init failed (part has no geometry or other error), skipping: {}", e.getMessage());
                vaoId = -1;
                vboId = -1;
                eboId = -1;
                return;
            }
        }
        if (initFailed) return;
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
        boolean previousCullFaceEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

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

            var sampler0 = shader.getUniform("Sampler0");
            if (sampler0 != null) sampler0.set(0);

            // Биндим текстуры и только потом применяем шейдер (apply загружает uniform-ы)
            TextureBinder.bindForModelIfNeeded(shader);
            shader.apply();

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();

            GL30.glBindVertexArray(vaoId);
            GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error during VBO render", e);
        } finally {
            // Восстанавливаем состояние безопасным порядком:
            // 1) возвращаем предыдущий VAO
            // 2) возвращаем глобальный GL_ARRAY_BUFFER без промежуточного VAO=0
            GL30.glBindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);

            // Восстанавливаем culling
            if (previousCullFaceEnabled) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }

            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
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

    public static class VboData implements AutoCloseable {
        public final ByteBuffer byteBuffer;
        public final IntBuffer indices;
        private boolean consumed = false;

        public VboData(ByteBuffer byteBuffer, IntBuffer indices) {
            this.byteBuffer = byteBuffer;
            this.indices = indices;
        }

        /**
         * Освобождает связанную с VboData off-heap память.
         * Метод идемпотентен: повторные вызовы безопасны.
         */
        @Override
        public void close() {
            if (consumed) {
                return;
            }
            consumed = true;
            if (byteBuffer != null) {
                MemoryUtil.memFree(byteBuffer);
            }
            if (indices != null) {
                MemoryUtil.memFree(indices);
            }
        }
    }
}

