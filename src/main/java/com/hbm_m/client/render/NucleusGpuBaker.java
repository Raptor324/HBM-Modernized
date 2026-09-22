package com.hbm_m.client.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
/**
 * <b>Tier 1: GPU Compute Bake</b> — запекание инстансов частей в вершинный буфер
 * BLOCK-формата compute-шейдером и отрисовка ОДНИМ {@code glDrawElements}
 * РОДНОЙ программой пака (pack shadow / gbuffers). Пак сам применяет свою
 * тене-дисторсию и кодирует свой gbuffer — никакой репликации схем, 100%
 * совместимость (BSL/Photon/Complementary/…).
 * <p>
 * Контракт (выверен по companion-пути и разбору shadow-vsh паков): паковая
 * shadow-программа содержит round-trip
 * {@code P·M·(P⁻¹·M⁻¹·ftransform)} — математически тождества, итоговый клип
 * всегда {@code ProjMat · ModelViewMat · vertex (+дисторсия)}. Для теней:
 * вершины = shadow-space (записи коллектора), ModelViewMat = identity,
 * ProjMat = P_shadow — тот же контракт, что у Tier-2 drawShadowInstances.
 * <p>
 * Входы compute: SSBO 0 = меш части (companion VBO — УЖЕ в BLOCK-формате,
 * биндится как SSBO без копии), SSBO 1 = записи инстансов (30 флоатов,
 * layout {@link InstancedStaticPartRenderer#INSTANCE_DATA_SIZE}), SSBO 2 =
 * выход verts×instances. Индексы — CPU-расширение базовых индексов
 * {@code baseIdx + inst*vertCount} (строится один раз на достигнутое число
 * инстансов, дальше кэш). Барьер {@code GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT} → draw.
 * <p>
 * Фолбэк: GL < 4.3 / любая ошибка → {@link #isEnabled()} false → вызывающий
 * код остаётся на Tier 2 (ExtendedShader) / Tier 3 (vanilla instancing).
 * Kill-switch: {@code -Dhbm.gpuBake=false}. Вызов только с render thread.
 */
public final class NucleusGpuBaker {

    private static final int LOCAL_SIZE = 64;
    private static final boolean KILL_SWITCH =
            !"false".equalsIgnoreCase(System.getProperty("hbm.gpuBake", "true"));

    private static boolean checked = false;
    private static boolean available = false;
    private static boolean logged = false;
    private static int programId = -1;
    private static int uMeshStride, uInstStride, uOutStride, uVertCount, uInstCount;
    private static int uOffPos, uOffColor, uOffNormal, uOffUv2;
    private static int uOffUv0, uOffUv1, uOffMidTex, uOffTangent;
    private static int uBakeMode, uCamPos, uOffLight;

    // ── Плотный ВЫХОДНОЙ формат bake (52 байта, кратен 4) ──────────────
    // Входной companion-меш лежит в формате IrisVertexFormats.ENTITY, чей
    // stride на 1.21.1 = 54 байта (НЕ кратен 4: невидимый padding после
    // Normal) — адресовать его флоатами, как раньше, нельзя: 54/4=13.5
    // обрезалось до 13, и каждая вершина после первой читалась со сдвигом
    // 2 байта → мусорные позиции → «взорванные» треугольники от машин
    // (только 1.21.1/Iris; на 1.20.1/Oculus stride 56 кратен 4, потому
    // старый float-путь там работал). Compute читает ВХОД словами по
    // байтовым офсетам (любой stride), а пишет в НАШ плотный layout —
    // порядок полей как у Iris ENTITY (локации 0..5 программ прибиндены
    // ShaderInstance по порядку атрибутов), но все офсеты выровнены.
    private static final int OUT_STRIDE_BYTES = 52;
    private static final int OUT_OFF_POS = 0;      // 3F
    private static final int OUT_OFF_COLOR = 12;   // 4UB
    private static final int OUT_OFF_UV0 = 16;     // 2F
    private static final int OUT_OFF_UV1 = 24;     // 2S (integer pipeline)
    private static final int OUT_OFF_UV2 = 28;     // 2S (integer pipeline)
    private static final int OUT_OFF_NORMAL = 32;  // 3B + 1 pad
    private static final int OUT_OFF_ENTITY = 36;  // 2US
    private static final int OUT_OFF_MIDTEX = 40;  // 2F
    private static final int OUT_OFF_TANGENT = 48; // 4B

    private static int instanceSsbo = -1;
    private static long instanceSsboBytes = -1;
    private static int outputSsbo = -1;
    private static long outputSsboBytes = -1;

    private static final java.util.ArrayList<BakedMesh> BAKED = new java.util.ArrayList<>(64);

    /** GPU-состояние bake одного part-renderer'а (по его companion-мешу). */
    private static final class BakedMesh {
        final IrisCompanionMesh mesh;
        final int vaoId;
        final int eboId;
        final int[] baseIndices;
        final int vertCount;
        final int idxCount;
        int filledInstances = -1;

        BakedMesh(IrisCompanionMesh mesh, int vaoId, int eboId, int[] baseIndices,
                  int vertCount, int idxCount) {
            this.mesh = mesh;
            this.vaoId = vaoId;
            this.eboId = eboId;
            this.baseIndices = baseIndices;
            this.vertCount = vertCount;
            this.idxCount = idxCount;
        }
    }

    private NucleusGpuBaker() {}

    /** Доступен ли Tier-1 (capabilities + kill-switch). Дёшево после первого вызова. */
    public static boolean isEnabled() {
        if (!KILL_SWITCH) {
            return false;
        }
        if (!checked) {
            checked = true;
            try {
                var caps = org.lwjgl.opengl.GL.getCapabilities();
                available = caps.OpenGL43 || caps.GL_ARB_compute_shader;
                if (!available && !logged) {
                    logged = true;
                    com.hbm_m.main.MainRegistry.LOGGER.info(
                            "[HBM-M] NucleusGpuBaker: no compute shaders (GL<4.3, no ARB) - "
                                    + "Tier 1 disabled, Tier 2/3 stays active");
                }
            } catch (Throwable t) {
                available = false;
            }
        }
        return available;
    }

    /**
     * Tier-1 теневой флаш: запечь записи коллектора и нарисовать паковой
     * SHADOW-программой по одному draw на part-renderer. Программа {@code shader}
     * должна быть уже применена (tryApply — FB забинджен).
     *
     * @return true — флаш отработал через Tier-1 целиком; false — вызывающий
     *         продолжает на Tier 2/3.
     */
    public static boolean bakeAndDrawShadow(List<IrisShadowBatchCollector.Entry> entries,
                                            ShaderInstance shader, Matrix4f shadowProj) {
        if (!isEnabled() || !RenderSystem.isOnRenderThread() || shader == null) {
            return false;
        }
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            if (programId == -1 && !initProgram()) {
                return false;
            }
            ensureSharedBuffers();
            // Стейт дроука — как у Tier-2 drawShadowInstances: геометрия машин
            // со смешанным winding, без disableCull половина треугольников
            // вырезается. Snapshot восстанавливает всё на выходе.
            try (RenderStateGuard ignored = RenderStateGuard.snapshot()) {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.disableCull();

                for (int i = 0; i < entries.size(); i++) {
                IrisShadowBatchCollector.Entry e = entries.get(i);
                if (e.count <= 0) {
                    continue;
                }
                IrisCompanionMesh mesh = e.renderer.getOrBuildCompanionForShadowBatch();
                if (mesh == null || !mesh.isBuilt()) {
                    bakeSkipLog(e, "companion unbuilt/failed");
                    continue;
                }
                if (mesh.getMeshVertexCount() <= 0 || mesh.getMeshStrideBytes() <= 0
                        || mesh.getMeshIndices() == null || mesh.getMeshIndices().length == 0) {
                    bakeSkipLog(e, "companion empty mesh (verts=" + mesh.getMeshVertexCount() + ")");
                    continue;
                }
                BakedMesh baked = getOrCreate(mesh);
                if (baked == null) {
                    continue;
                }
                FloatBuffer records = e.data.duplicate();
                records.flip();
                if (!dispatchBake(baked, records, e.count, MODE_SHADOW, 0f, 0f, 0f)) {
                    continue;
                }

                // ── Draw: АКТИВНА ПАКОВАЯ программа — её матрицы и её дроук.
                // Иначе glUniformMatrix4fv улетает в compute-программу
                // («Uniform must be a matrix type», тени отсутствуют).
                GL20.glUseProgram(shader.getId());
                setMatrices(shader, shadowProj, IDENTITY_MV);
                RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
                GL30.glBindVertexArray(baked.vaoId);
                GL11.glDrawElements(GL11.GL_TRIANGLES, baked.idxCount * e.count,
                        GL11.GL_UNSIGNED_INT, 0L);
                NucleusDebug.recordDraw(1, e.count, "GPU bake shadow");
            }

                // Паковая программа уже активна — трекинг Iris остаётся консистентным.
                GL20.glUseProgram(shader.getId());
                com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            }
            return true;
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[HBM-M] NucleusGpuBaker shadow bake failed - "
                    + "falling back to Tier 2", t);
            available = false;
            releaseResourcesInternal();
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            return false;
        }
    }

    private static final int MODE_SHADOW = 0;
    private static final int MODE_MAIN = 1;

    /**
     * Ядро bake: upload записей → dispatch compute → барьеры.
     * После успешного возврата outputSsbo содержит verts×count вершин BLOCK-лейаута
     * (main — camera-relative), bake-VAO рендерера готов к одному glDrawElements.
     */
    private static boolean dispatchBake(BakedMesh baked, FloatBuffer records, int count,
                                        int mode, float camX, float camY, float camZ) {
        if (!uploadInstances(records, count)) {
            return false;
        }
        // Все байтовые офсеты входа обязаны резолвиться: ldWord с -1 читал бы
        // out-of-bounds (мусор вместо отказа).
        int offPos = baked.mesh.getMeshOffset("position", -1);
        int offColor = baked.mesh.getMeshOffset("color", -1);
        int offNormal = baked.mesh.getMeshOffset("normal", -1);
        int offUv0 = baked.mesh.getMeshOffset("uv", 0);
        int offUv1 = baked.mesh.getMeshOffset("uv", 1);
        int offUv2 = baked.mesh.getMeshOffset("uv", 2);
        int offMidTex = baked.mesh.getMeshAttribByteOffsetOr("mc_midTexCoord",
                baked.mesh.getMeshOffset("genericFloat2", -1));
        int offTangent = baked.mesh.getMeshAttribByteOffsetOr("at_tangent",
                baked.mesh.getMeshOffset("genericByte4", -1));
        if ((offPos | offColor | offNormal | offUv0 | offUv1 | offUv2 | offMidTex | offTangent) < 0) {
            return false;
        }
        expandIndices(baked, count);

        // ── Dispatch: АКТИВНА compute-программа (её glUniform1i/SSBO) ──
        GL20.glUseProgram(programId);
        // uMeshStride — БАЙТЫ входного меша (54 на 1.21.1 — не кратен 4,
        // потому вход адресуется словами с байтовой сборкой, см. ldWord).
        GL20.glUniform1i(uMeshStride, baked.mesh.getMeshStrideBytes());
        GL20.glUniform1i(uInstStride, InstancedStaticPartRenderer.INSTANCE_DATA_SIZE);
        GL20.glUniform1i(uOutStride, OUT_STRIDE_BYTES / 4);
        GL20.glUniform1i(uVertCount, baked.vertCount);
        GL20.glUniform1i(uInstCount, count);
        GL20.glUniform1i(uOffPos, offPos);
        GL20.glUniform1i(uOffColor, offColor);
        GL20.glUniform1i(uOffNormal, offNormal);
        GL20.glUniform1i(uOffUv0, offUv0);
        GL20.glUniform1i(uOffUv1, offUv1);
        GL20.glUniform1i(uOffUv2, offUv2);
        GL20.glUniform1i(uOffMidTex, offMidTex);
        GL20.glUniform1i(uOffTangent, offTangent);
        GL20.glUniform1i(uBakeMode, mode);
        GL20.glUniform1i(uOffLight, InstancedStaticPartRenderer.LIGHT_FLOAT_OFFSET);
        if (uCamPos >= 0) {
            GL20.glUniform3f(uCamPos, camX, camY, camZ);
        }

        ensureOutputBuffer((long) baked.vertCount * count * OUT_STRIDE_BYTES);
        // WAR-синхронизация с ПРЕДЫДУЩИМ draw: он ещё читает outputSsbo
        // вершинными атрибутами, а этот dispatch пишет туда же с нуля (раунд 27).
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT
                | GL43.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, baked.mesh.getMeshVboId());
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, instanceSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, outputSsbo);
        int total = baked.vertCount * count;
        GL43.glDispatchCompute((total + LOCAL_SIZE - 1) / LOCAL_SIZE, 1, 1);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, 0);
        GL42.glMemoryBarrier(GL42.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);
        return true;
    }

    /**
     * Tier-1 MAIN-проход: запечь инстансы рендерера в camera-relative вершины и
     * нарисовать ОДНИМ glDrawElements РОДНОЙ gbuffers-программой пака — пак сам
     * кодирует свой gbuffer (замена per-instance companion-пути для нераспознанных
     * схем: BSL 1618 дкоуков → ~27). Записи — мировые (FrameViewState), свет —
     * трилинейный из 8-corner полей записи (как наш инстансный VSH).
     *
     * @param records буфер записей (flip уже сделан вызывающим), count инстансов
     * @param packShader применённая pack-программа (getBlockShader(false))
     * @param proj     проекция события (чистая P на обеих версиях)
     * @param viewRot  R_cam (только ротация, translation=0)
     * @return true — нарисовано; false — вызывающий уходит на прежний путь
     */
    public static boolean bakeAndDrawMain(FloatBuffer records, int count, IrisCompanionMesh mesh,
                                          ShaderInstance packShader, Matrix4f proj, Matrix4f viewRot,
                                          float camX, float camY, float camZ) {
        if (!isEnabled() || !RenderSystem.isOnRenderThread() || packShader == null
                || mesh == null || !mesh.isBuilt() || count <= 0 || records == null) {
            return false;
        }
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            if (programId == -1 && !initProgram()) {
                return false;
            }
            BakedMesh baked = getOrCreate(mesh);
            if (baked == null || !dispatchBake(baked, records, count, MODE_MAIN, camX, camY, camZ)) {
                return false;
            }
            try (RenderStateGuard ignored = RenderStateGuard.snapshot()) {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.disableCull();

                GL20.glUseProgram(packShader.getId());
                setMatrices(packShader, proj, viewRot);
                RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
                GL30.glBindVertexArray(baked.vaoId);
                bindIrisExtendedAttributes(packShader, baked);
                GL11.glDrawElements(GL11.GL_TRIANGLES, baked.idxCount * count,
                        GL11.GL_UNSIGNED_INT, 0L);
                NucleusDebug.recordDraw(1, count, "GPU bake main");
            }
            // Паковая программа уже активна — трекинг Iris остаётся консистентным.
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            return true;
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[HBM-M] NucleusGpuBaker main bake failed - "
                    + "falling back to companion path", t);
            available = false;
            releaseResourcesInternal();
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
            return false;
        }
    }

    /**
     * Iris-расширенные атрибуты (mc_midTexCoord / at_tangent / iris_Entity) на
     * линкер-локациях ПАКОВОЙ программы. Читаем из НАШЕГО плотного выходного
     * layout (OUT_OFF_MIDTEX/OUT_OFF_TANGENT): per-vertex статические поля
     * compute уже скопировал туда из меша, entity-id — константный 0.
     * Вызывается при забинженном bake-VAO.
     */
    private static void bindIrisExtendedAttributes(ShaderInstance shader, BakedMesh baked) {
        int program = shader.getId();
        int entLoc = GL20.glGetAttribLocation(program, "iris_Entity");
        if (entLoc >= 0) {
            // Константа вместо массива: наши машины не entityId-специфичны.
            // Ставим ВСЕГДА (раньше — только если слот был занят массивом:
            // на 1.21.1 локация 6 никем не занята, и атрибут читал мусор из
            // «current value bank» чужих дроуков).
            if (attribBound(entLoc)) {
                GL20.glDisableVertexAttribArray(entLoc);
            }
            GL30.glVertexAttribI2i(entLoc, 0, 0);
        }
        int midTexLoc = GL20.glGetAttribLocation(program, "mc_midTexCoord");
        if (midTexLoc >= 0 && !attribBound(midTexLoc)) {
            GL20.glEnableVertexAttribArray(midTexLoc);
            GL20.glVertexAttribPointer(midTexLoc, 2, GL11.GL_FLOAT, false,
                    OUT_STRIDE_BYTES, OUT_OFF_MIDTEX);
        }
        int tangentLoc = GL20.glGetAttribLocation(program, "at_tangent");
        if (tangentLoc >= 0 && !attribBound(tangentLoc)) {
            GL20.glEnableVertexAttribArray(tangentLoc);
            GL20.glVertexAttribPointer(tangentLoc, 4, GL11.GL_BYTE, true,
                    OUT_STRIDE_BYTES, OUT_OFF_TANGENT);
        }
    }

    private static boolean attribBound(int loc) {
        return GL20.glGetVertexAttribi(loc, GL20.GL_VERTEX_ATTRIB_ARRAY_ENABLED) == GL11.GL_TRUE;
    }

    /**
     * ModelViewMat / ProjMat на паковой программе.
     * ТОЛЬКО сырые GL-локации: shader.getUniform() на pack-программе возвращает
     * Uniform-объект, чей upload() бьёт в не-матричный юниформ — 900k
     * GL_INVALID_OPERATION «Uniform must be a matrix type» и НИКОГДА не
     * установленные матрицы (тени пропадали целиком, debug.log 0914 00:44).
     * IrisDerivedMatrixUniforms — тот же механизм, что у companion-флаша.
     */
    private static final Matrix4f IDENTITY_MV = new Matrix4f();
    private static final float[] MV_FLOATS = new float[16];
    private static final float[] PROJ_FLOATS = new float[16];
    private static ShaderInstance matrixShader;
    private static com.hbm_m.client.render.shader.IrisDerivedMatrixUniforms.Locations matrixLocs =
            com.hbm_m.client.render.shader.IrisDerivedMatrixUniforms.Locations.NONE;
    private static int projLoc = -2;

    private static void setMatrices(ShaderInstance shader, Matrix4f proj, Matrix4f modelView) {
        if (matrixShader != shader) {
            matrixShader = shader;
            matrixLocs = com.hbm_m.client.render.shader.IrisDerivedMatrixUniforms.resolve(shader);
            int program = shader.getId();
            projLoc = GL20.glGetUniformLocation(program, "iris_ProjMat");
            if (projLoc < 0) {
                projLoc = GL20.glGetUniformLocation(program, "ProjMat");
            }
        }
        modelView.get(MV_FLOATS);
        int locModelView = matrixLocs.modelView();
        if (locModelView >= 0) {
            GL20.glUniformMatrix4fv(locModelView, false, MV_FLOATS);
        }
        if (projLoc >= 0) {
            proj.get(PROJ_FLOATS);
            GL20.glUniformMatrix4fv(projLoc, false, PROJ_FLOATS);
        }
        // ПРОИЗВОДНЫЕ матрицы — ОБЯЗАТЕЛЬНО: паки (Photon) считают мировые
        // нормали через mat3(ModelViewMatInverse)·n_view и/или NormalMat.
        // Без них свет «не с того угла» (инверсия остаётся от прошлого стейта).
        int locInverse = matrixLocs.modelViewInverse();
        if (locInverse >= 0) {
            INVERSE_SCRATCH.set(modelView).invertAffine();
            INVERSE_SCRATCH.get(INV_FLOATS);
            GL20.glUniformMatrix4fv(locInverse, false, INV_FLOATS);
        }
        int locNormalMat = matrixLocs.normalMat();
        if (locNormalMat >= 0) {
            NORMAL_TMP.set(modelView);
            NORMAL_TMP.get(NORM_FLOATS);
            GL20.glUniformMatrix3fv(locNormalMat, false, NORM_FLOATS);
        }
    }

    private static final Matrix4f INVERSE_SCRATCH = new Matrix4f();
    private static final float[] INV_FLOATS = new float[16];
    private static final org.joml.Matrix3f NORMAL_TMP = new org.joml.Matrix3f();
    private static final float[] NORM_FLOATS = new float[9];

    // ── Init / ресурсы ─────────────────────────────────────────────────

    private static boolean initProgram() {
        try {
            int p = GL20.glCreateProgram();
            int cs = GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
            GL20.glShaderSource(cs, COMPUTE_SOURCE);
            GL20.glCompileShader(cs);
            if (GL20.glGetShaderi(cs, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException("compute compile: " + GL20.glGetShaderInfoLog(cs, 4096));
            }
            GL20.glAttachShader(p, cs);
            GL20.glLinkProgram(p);
            GL20.glDeleteShader(cs);
            if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException("compute link: " + GL20.glGetProgramInfoLog(p, 4096));
            }
            programId = p;
            uMeshStride = GL20.glGetUniformLocation(p, "uMeshStride");
            uInstStride = GL20.glGetUniformLocation(p, "uInstStride");
            uOutStride = GL20.glGetUniformLocation(p, "uOutStride");
            uVertCount = GL20.glGetUniformLocation(p, "uVertCount");
            uInstCount = GL20.glGetUniformLocation(p, "uInstCount");
            uOffPos = GL20.glGetUniformLocation(p, "uOffPos");
            uOffColor = GL20.glGetUniformLocation(p, "uOffColor");
            uOffNormal = GL20.glGetUniformLocation(p, "uOffNormal");
            uOffUv2 = GL20.glGetUniformLocation(p, "uOffUv2");
            uOffUv0 = GL20.glGetUniformLocation(p, "uOffUv0");
            uOffUv1 = GL20.glGetUniformLocation(p, "uOffUv1");
            uOffMidTex = GL20.glGetUniformLocation(p, "uOffMidTex");
            uOffTangent = GL20.glGetUniformLocation(p, "uOffTangent");
            uBakeMode = GL20.glGetUniformLocation(p, "uBakeMode");
            uCamPos = GL20.glGetUniformLocation(p, "uCamPos");
            uOffLight = GL20.glGetUniformLocation(p, "uOffLight");
            instanceSsbo = GL15.glGenBuffers();
            outputSsbo = GL15.glGenBuffers();
            if (!logged) {
                logged = true;
                com.hbm_m.main.MainRegistry.LOGGER.info(
                        "[HBM-M] NucleusGpuBaker: Tier 1 ACTIVE (programId={})", programId);
            }
            return true;
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn("[HBM-M] NucleusGpuBaker init failed - Tier 1 disabled", t);
            return false;
        }
    }

    private static void ensureSharedBuffers() {
        if (instanceSsboBytes == -1) {
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, instanceSsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, 4L << 20, GL15.GL_DYNAMIC_COPY);
            instanceSsboBytes = 4L << 20;
        }
        if (outputSsboBytes == -1) {
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, outputSsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, 16L << 20, GL15.GL_DYNAMIC_COPY);
            outputSsboBytes = 16L << 20;
        }
    }

    private static void ensureOutputBuffer(long bytes) {
        if (bytes > outputSsboBytes) {
            long newBytes = Math.max(bytes, outputSsboBytes * 2);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, outputSsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, newBytes, GL15.GL_DYNAMIC_COPY);
            outputSsboBytes = newBytes;
        }
    }

    private static boolean uploadInstances(FloatBuffer records, int count) {
        int floats = count * InstancedStaticPartRenderer.INSTANCE_DATA_SIZE;
        if (records == null || records.remaining() < floats) {
            return false;
        }
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, instanceSsbo);
        long bytes = (long) floats * 4L;
        if (bytes > instanceSsboBytes) {
            long newBytes = Math.max(bytes, instanceSsboBytes * 2);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, newBytes, GL15.GL_DYNAMIC_COPY);
            instanceSsboBytes = newBytes;
        }
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, records);
        return true;
    }

    /** CPU-расширение индексов до {@code count} инстансов (кэш по достигнутому). */
    private static void expandIndices(BakedMesh baked, int count) {
        if (baked.filledInstances >= count) {
            return;
        }
        // GL_ELEMENT_ARRAY_BUFFER — состояние ТЕКУЩЕГО VAO: бинд без нашего VAO
        // перезаписывал бы EBO чужого (Iris/ванильного) VAO.
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        GL30.glBindVertexArray(baked.vaoId);
        IntBuffer data = MemoryUtil.memAllocInt(baked.idxCount * count);
        try {
            for (int i = 0; i < count; i++) {
                int base = i * baked.vertCount;
                for (int k = 0; k < baked.idxCount; k++) {
                    data.put(baked.baseIndices[k] + base);
                }
            }
            data.flip();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, baked.eboId);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data, GL15.GL_DYNAMIC_DRAW);
            baked.filledInstances = count;
        } finally {
            MemoryUtil.memFree(data);
            com.hbm_m.client.render.GlVaoSafety.bindVertexArray(prevVao);
        }
    }

    private static BakedMesh getOrCreate(IrisCompanionMesh mesh) {
        for (int i = 0; i < BAKED.size(); i++) {
            if (BAKED.get(i).mesh == mesh) {
                return BAKED.get(i);
            }
        }
        try {
            int vertCount = mesh.getMeshVertexCount();
            int stride = mesh.getMeshStrideBytes();
            int[] base = mesh.getMeshIndices();
            var format = mesh.getMeshFormat();
            if (vertCount <= 0 || stride == 0 || base == null || base.length == 0 || format == null) {
                return null;
            }
            int vao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vao);
            // Атрибуты указывают на outputSsbo: данные туда пишет compute
            // (один буфер, два таргета — SSBO-запись + ATTRIB-чтение через барьер).
            // Указатели — по НАШЕМУ плотному 52-байтовому выходному layout
            // (см. OUT_* константы): локации 0..5 = Position/Color/UV0/UV1/UV2/
            // Normal — ровно тот порядок, в котором vanilla ShaderInstance
            // биндит атрибуты Iris-ENTITY-форматы на ОБЕИХ версиях. Типы
            // зафиксированы (layout входного Iris-формата совпадает по полям,
            // отличается только паддингами/stride, которые compute уже учёл).
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, outputSsbo);
            // ОБЯЗАТЕЛЬНО: glVertexAttribPointer массив НЕ включает — без
            // enable все атрибуты читают константу, все вершины в нуле,
            // тени пустые при валидных дроуках (лог 0914 01:07).
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, OUT_STRIDE_BYTES, OUT_OFF_POS);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 4, GL11.GL_UNSIGNED_BYTE, true, OUT_STRIDE_BYTES, OUT_OFF_COLOR);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, OUT_STRIDE_BYTES, OUT_OFF_UV0);
            // UV1/UV2 — SHORT-целые: паковые шейдеры читают их как ivec2,
            // Mojang биндит через glVertexAttribIPointer (integer pipeline).
            GL20.glEnableVertexAttribArray(3);
            GL30.glVertexAttribIPointer(3, 2, GL11.GL_SHORT, OUT_STRIDE_BYTES, OUT_OFF_UV1);
            GL20.glEnableVertexAttribArray(4);
            GL30.glVertexAttribIPointer(4, 2, GL11.GL_SHORT, OUT_STRIDE_BYTES, OUT_OFF_UV2);
            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(5, 3, GL11.GL_BYTE, true, OUT_STRIDE_BYTES, OUT_OFF_NORMAL);
            int ebo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
            GL30.glBindVertexArray(0);
            BakedMesh baked = new BakedMesh(mesh, vao, ebo, base, vertCount, base.length);
            BAKED.add(baked);
            return baked;
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn("[HBM-M] NucleusGpuBaker: baked mesh init failed", t);
            return null;
        }
    }

    /** Одноразовый на рендерер лог скипнутого bake-участника (диагностика пустых теней). */
    private static final java.util.HashSet<Integer> SKIP_LOGGED = new java.util.HashSet<>();

    private static void bakeSkipLog(IrisShadowBatchCollector.Entry e, String reason) {
        Integer key = System.identityHashCode(e.renderer);
        if (SKIP_LOGGED.add(key)) {
            com.hbm_m.main.MainRegistry.LOGGER.info(
                    "[HBM-M] NucleusGpuBaker: renderer {} skipped in shadow bake ({}), instances={}",
                    key, reason, e.count);
        }
    }

    private static void releaseResourcesInternal() {
        for (BakedMesh bm : BAKED) {
            GL30.glDeleteVertexArrays(bm.vaoId);
            GL15.glDeleteBuffers(bm.eboId);
        }
        BAKED.clear();
        if (programId != -1) {
            GL20.glDeleteProgram(programId);
            programId = -1;
        }
        if (instanceSsbo != -1) {
            GL15.glDeleteBuffers(instanceSsbo);
            instanceSsbo = -1;
            instanceSsboBytes = -1;
        }
        if (outputSsbo != -1) {
            GL15.glDeleteBuffers(outputSsbo);
            outputSsbo = -1;
            outputSsboBytes = -1;
        }
    }

    // ── Compute-шейдер: один инвок на выходную вершину ─────────────────
    // MODE_SHADOW (0): вершины в shadow-space, UV2 = полный свет.
    // MODE_MAIN (1): вершины camera-relative (world - uCamPos), UV2 =
    // трилинейная интерполяция 8-corner света записи.
    //
    // ВХОД (SSBO 0) — companion-меш в формате IrisVertexFormats.ENTITY,
    // чей stride МОЖЕТ БЫТЬ НЕ КРАТЕН 4 (1.21.1 = 54 байта: невидимый
    // padding после Normal; 1.20.1 = 56 — кратен). Поэтому вход адресуется
    // СЛОВАМИ с побайтовой сборкой (ldWord/ldFloat по байтовым офсетам),
    // а не float[]-индексацией, как в ранней версии (54/4=13.5 → 13 —
    // сдвиг всех вершин после первой → мусорные позиции, «взорванные»
    // треугольники под Iris 1.21.1).
    // ВЫХОД (SSBO 2) — НАШ плотный 52-байтовый layout (кратен 4):
    // pos 3f@0, color 4ub@12, uv0 2f@16, uv1 2s@24, uv2 2s@28,
    // normal 4b@32, iris_Entity 2us@36, mc_midTexCoord 2f@40, at_tangent 4b@48.

    private static final String COMPUTE_SOURCE = """
            #version 430 core
            layout(local_size_x = 64) in;

            layout(std430, binding = 0) readonly restrict buffer MeshBuf { uint meshW[]; };
            layout(std430, binding = 1) readonly restrict buffer InstBuf { float instData[]; };
            layout(std430, binding = 2) restrict buffer OutBuf { float outData[]; };

            uniform int uMeshStride; // БАЙТЫ входного меша (может быть не кратен 4)
            uniform int uInstStride; // флоаты (30)
            uniform int uOutStride;  // флоаты выхода (13 = 52 байта)
            uniform int uVertCount;
            uniform int uInstCount;
            uniform int uOffPos;     // байтовые офсеты ВО ВХОДЕ
            uniform int uOffColor;
            uniform int uOffNormal;
            uniform int uOffUv0;
            uniform int uOffUv1;
            uniform int uOffUv2;
            uniform int uOffMidTex;
            uniform int uOffTangent;
            uniform int uBakeMode;   // 0 shadow, 1 main
            uniform int uOffLight;   // float-офсет 8-corner света в записи (14)
            uniform vec3 uCamPos;

            uint ldWord(uint base, int byteOff) {
                uint a = base + uint(byteOff);
                uint w0 = meshW[a >> 2u];
                uint sh = (a & 3u) * 8u;
                if (sh == 0u) return w0;
                uint w1 = meshW[(a >> 2u) + 1u];
                return (w0 >> sh) | (w1 << (32u - sh));
            }

            float ldFloat(uint base, int byteOff) {
                return uintBitsToFloat(ldWord(base, byteOff));
            }

            vec3 quatRotate(vec4 q, vec3 v) {
                return v + 2.0 * cross(q.xyz, cross(q.xyz, v) + q.w * v);
            }

            void main() {
                uint gid = gl_GlobalInvocationID.x;
                uint total = uint(uVertCount) * uint(uInstCount);
                if (gid >= total) return;
                uint inst = gid / uint(uVertCount);
                uint lv = gid - inst * uint(uVertCount);

                uint mBase = lv * uint(uMeshStride);   // байты входа
                uint iBase = uint(inst * uInstStride); // флоаты записи
                uint o = gid * uint(uOutStride);       // флоаты выхода

                // Позиция: R(quat)·pos + instPos; main — camera-relative.
                vec3 pos = vec3(ldFloat(mBase, uOffPos),
                                ldFloat(mBase, uOffPos + 4),
                                ldFloat(mBase, uOffPos + 8));
                vec4 rot = vec4(instData[iBase + 3u], instData[iBase + 4u],
                                instData[iBase + 5u], instData[iBase + 6u]);
                vec3 outPos = quatRotate(rot, pos)
                        + vec3(instData[iBase + 0u], instData[iBase + 1u], instData[iBase + 2u]);
                if (uBakeMode == 1) {
                    outPos -= uCamPos;
                }
                outData[o + 0u] = outPos.x;
                outData[o + 1u] = outPos.y;
                outData[o + 2u] = outPos.z;

                // Альфа цвета × fade инстанса (fade квантован 1/255).
                uint cRaw = ldWord(mBase, uOffColor);
                float fade = clamp(instData[iBase + 13u], 0.0, 1.0);
                uint a = uint(float((cRaw >> 24) & 0xFFu) * fade);
                outData[o + 3u] = uintBitsToFloat((cRaw & 0x00FFFFFFu) | ((a & 0xFFu) << 24));

                // UV0 — 2 float (сырая копия).
                outData[o + 4u] = ldFloat(mBase, uOffUv0);
                outData[o + 5u] = ldFloat(mBase, uOffUv0 + 4);

                // UV1 (overlay) — 2 short одним словом (сырая копия).
                outData[o + 6u] = uintBitsToFloat(ldWord(mBase, uOffUv1));

                // UV2 (lightmap) — два ushort в одном слове.
                uint uv2Bits;
                if (uBakeMode == 0) {
                    // shadow: полный свет (для теневой глубины/цвета не критичен).
                    uv2Bits = uint(240 | (240 << 16));
                } else {
                    // Трилинейный свет по 8 углам bbox записи (как инстансный VSH).
                    vec3 bmin = vec3(instData[iBase + 7u], instData[iBase + 8u], instData[iBase + 9u]);
                    vec3 bsize = max(vec3(instData[iBase + 10u], instData[iBase + 11u],
                                          instData[iBase + 12u]), vec3(1e-4));
                    vec3 w = clamp((pos - bmin) / bsize, 0.0, 1.0);
                    uint lBase = iBase + uint(uOffLight);
                    vec2 c0 = vec2(instData[lBase + 0u], instData[lBase + 1u]);
                    vec2 c1 = vec2(instData[lBase + 2u], instData[lBase + 3u]);
                    vec2 c2 = vec2(instData[lBase + 4u], instData[lBase + 5u]);
                    vec2 c3 = vec2(instData[lBase + 6u], instData[lBase + 7u]);
                    vec2 c4 = vec2(instData[lBase + 8u], instData[lBase + 9u]);
                    vec2 c5 = vec2(instData[lBase + 10u], instData[lBase + 11u]);
                    vec2 c6 = vec2(instData[lBase + 12u], instData[lBase + 13u]);
                    vec2 c7 = vec2(instData[lBase + 14u], instData[lBase + 15u]);
                    vec2 x00 = mix(c0, c1, w.x);
                    vec2 x10 = mix(c2, c3, w.x);
                    vec2 x01 = mix(c4, c5, w.x);
                    vec2 x11 = mix(c6, c7, w.x);
                    vec2 y0 = mix(x00, x10, w.y);
                    vec2 y1 = mix(x01, x11, w.y);
                    vec2 lm = mix(y0, y1, w.z);
                    int bu = int(clamp(lm.x, 0.0, 240.0));
                    int sv = int(clamp(lm.y, 0.0, 240.0));
                    uv2Bits = uint(bu | (sv << 16));
                }
                outData[o + 7u] = uintBitsToFloat(uv2Bits);

                // Нормаль: 4 GLbyte — декод (sign-extend), вращение, кодек назад.
                uint nRaw = ldWord(mBase, uOffNormal);
                vec3 nrm = vec3(float(bitfieldExtract(int(nRaw), 0, 8)) / 127.0,
                                float(bitfieldExtract(int(nRaw), 8, 8)) / 127.0,
                                float(bitfieldExtract(int(nRaw), 16, 8)) / 127.0);
                vec3 nOut = normalize(quatRotate(rot, nrm));
                uint nPacked = uint(int(round(clamp(nOut.x, -1.0, 1.0) * 127.0)) & 255)
                        | (uint(int(round(clamp(nOut.y, -1.0, 1.0) * 127.0)) & 255) << 8)
                        | (uint(int(round(clamp(nOut.z, -1.0, 1.0) * 127.0)) & 255) << 16)
                        | (nRaw & 0xFF000000u);
                outData[o + 8u] = uintBitsToFloat(nPacked);

                // iris_Entity — константа 0 (машины не entityId-специфичны).
                outData[o + 9u] = uintBitsToFloat(0u);

                // mc_midTexCoord — 2 float (сырая копия).
                outData[o + 10u] = ldFloat(mBase, uOffMidTex);
                outData[o + 11u] = ldFloat(mBase, uOffMidTex + 4);

                // at_tangent — 4 байта одним словом (сырая копия).
                outData[o + 12u] = uintBitsToFloat(ldWord(mBase, uOffTangent));
            }
            """;
}
