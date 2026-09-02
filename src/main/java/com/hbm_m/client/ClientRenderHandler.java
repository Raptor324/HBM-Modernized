package com.hbm_m.client;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.joml.Matrix4f;

import com.hbm_m.block.ModBlocks;
// Этот класс отвечает за подсветку блоков, если те мешают установке многоблочной структуры
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.interfaces.IMultiblockPart;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ClientRenderHandler {

    private static final Map<BlockPos, Long> highlightedBlocks = new HashMap<>();
    // Постоянная подсветка для осиротевших фантомных блоков (потерявших связь с контроллером)
    private static final Map<BlockPos, Boolean> orphanedPhantomBlocks = new HashMap<>();
    
    // Счетчик тиков для периодической проверки осиротевших блоков (проверяем раз в 60 тиков = 1 раз в 3 секунды)
    private static int tickCounter = 0;
    
    // Кэш известных фантомных блоков для оптимизации (чтобы не проверять их каждый раз)
    private static final Map<BlockPos, Long> knownPhantomBlocks = new HashMap<>();

    /** Shared with NukeTorex; must extend RenderType to access protected RenderStateShard members. */
    // ВАЖНО: у ВСЕХ create(...) ниже sortOnUpload=false — НЕ ВКЛЮЧАТЬ.
    // Сортировка квадов средствами BufferBuilder (sortOnUpload=true) в этом
    // модпаке нестабильно падает «Sorting state uninitialized» на наших
    // батчах (в т.ч. в чисто ванильном BufferSource — 4 краш-репорта
    // 2026-08-28). Порядок отрисовки вместо этого обеспечивает САМА
    // ParticleEngineNT: частицы каждого батча сортируются от дальних к
    // ближним относительно камеры перед записью (renderBatches). Новые
    // RenderType добавлять только с sortOnUpload=false.
    public static final class CustomRenderTypes extends RenderType {
        private static final RenderStateShard.ShaderStateShard BHOLE_TEX_COLOR_SHADER =
                new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader);
        /** Самосветящиеся облака/вспышки взрыва — без lightmap (как RenderTorex 1.7.10).
         *  Без Iris — кастомный шейдер nuke_cloud (попиксельная окклюзия против LOD).
         *  Под активным паком Iris — ExtendedShader ключа TEXTURED_COLOR: неизвестные
         *  Iris'у модовые ShaderInstance маскируются disableDepthColor и рисуются
         *  «в никуда» (см. FarContentShaders). */
        private static final RenderStateShard.ShaderStateShard NUKE_TEX_COLOR_SHADER =
                new RenderStateShard.ShaderStateShard(
                        com.hbm_m.client.render.shader.FarContentShaders::resolveTexColor);

        private static final RenderStateShard.ShaderStateShard NUKE_ADD_TEX_COLOR_SHADER =
                new RenderStateShard.ShaderStateShard(
                        com.hbm_m.client.render.shader.FarContentShaders::resolveAddTexColor);

        /** Флаг «идёт отрисовка дальнего контента в DH FBO» (мост).
         *  Раньше переключал nukeClouds() на DH-вариант рендертайпа; теперь
         *  используется только диагностикой (NukeTorex.render). */
        private static volatile boolean FAR_PASS_ACTIVE = false;

        public static void setFarPassActive(boolean active) {
            FAR_PASS_ACTIVE = active;
        }

        public static boolean isFarPassActive() {
            return FAR_PASS_ACTIVE;
        }

        /** Селектор рендертайпа для NT-частиц.
         *  Исторически выбирал между NUKE_CLOUDS и удалённым DH-вариантом
         *  NUKE_CLOUDS_DH по флагу FAR_PASS_ACTIVE; с перехода на «ПЛАН Б»
         *  частицы в DH FBO не рисуются, поэтому флаг остаётся только для
         *  диагностики и селектор всегда возвращает базовый тип. */
        public static RenderType nukeClouds(ResourceLocation texture) {
            return NUKE_CLOUDS.apply(texture);
        }

        private CustomRenderTypes(String s, VertexFormat v, VertexFormat.Mode m, int i, boolean b, boolean b2, Runnable r, Runnable r2) { super(s, v, m, i, b, b2, r, r2); }

        /** Жёлтая синусоида на лазерном детонаторе — additive + depth test (не HIGHLIGHT_BOX_FILL). */
        public static final RenderType DETONATOR_LASER_GLOW = create("hbm_m_detonator_laser_glow",
                DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));

        /** Translucent world overlay; TRANSLUCENT_TARGET required for Iris/Embeddium. */
        public static final RenderType HIGHLIGHT_BOX_FILL = create("highlight_box_fill",
                DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 131072, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setWriteMaskState(COLOR_WRITE)
                        .setOutputState(TRANSLUCENT_TARGET)
                        .createCompositeState(true));

        /** Nuke cloud particles (NukeTorex) — vertex color × texture, без lightmap. */
        public static final Function<ResourceLocation, RenderType> NUKE_CLOUDS = Util.memoize(
                texture -> create("nuke_clouds", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 239120, true, false,
                        RenderType.CompositeState.builder()
                                .setShaderState(NUKE_TEX_COLOR_SHADER)
                                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                                .setCullState(NO_CULL)
                                .setLightmapState(NO_LIGHTMAP)
                                .setWriteMaskState(COLOR_WRITE)
                                .setOutputState(TRANSLUCENT_TARGET)
                                .createCompositeState(false)));

        /** Nuke flash (NukeTorex) - без depth test, чтобы рендерился поверх всего. */
        public static final Function<ResourceLocation, RenderType> NUKE_FLASH = Util.memoize(
                texture -> create("nuke_flash", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 545234, true, false,
                        RenderType.CompositeState.builder()
                                .setShaderState(NUKE_ADD_TEX_COLOR_SHADER)
                                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, true))
                                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                                .setDepthTestState(NO_DEPTH_TEST)
                                .setCullState(NO_CULL)
                                .setLightmapState(NO_LIGHTMAP)
                                .setWriteMaskState(COLOR_WRITE)
                                .setOutputState(TRANSLUCENT_TARGET)
                                .createCompositeState(false)));

        /**
         * Блик сопла ракеты, выровненный по entity-рендеру: в entity-пути
         * flare рисуется LongRangeParticleRenderType с ОБЫЧНЫМ альфа-блендингом
         * (defaultBlendFunc) и без записи глубины. Аддитив (LIGHTNING) давал
         * другой вид в виртуальном рендере — «flare вместе с мешем», а не
         * поверх, как у сущности. Текстура — атлас частиц (спрайты flash/flare).
         * ОДИН экземпляр: ParticleEngineNT батчит по идентичности RenderType.
         */
        private static final RenderType NUKE_GLOW_ADD = create("hbm_m_nuke_glow_add", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 545234, true, false,
                RenderType.CompositeState.builder()
                        .setShaderState(NUKE_ADD_TEX_COLOR_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(
                                net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_PARTICLES, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setWriteMaskState(COLOR_WRITE)
                        .setOutputState(TRANSLUCENT_TARGET)
                        .createCompositeState(false));

        public static RenderType nukeGlowAdd() {
            return NUKE_GLOW_ADD;
        }

        /**
         * локаои 1.7.10-рендера костей/пепла (ParticleSkeleton/ParticleAshes).
         * В 1.7.10: GL_BLEND(770/771) + GL_ALPHA_TEST GREATER 0 + наследуемый depth (LEQUAL + depth write) + cull face.
         * 1.7.10 использовал OpenGlHelper.setLightmapTextureCoords; в 1.20.1 -> COLOR_TEX_LIGHTMAP shader
         * который семплирует lightmap по UV2. Ранее использовался PARTICLE_SHADER без привязки lightmap –
         * все частицы рендерились чёрными.
         */
        /**
         * Скелет-кости: TRIANGLES, формат NEW_ENTITY (с нормалями и overlay UV1).
         * NEW_ENTITY + RENDERTYPE_ENTITY_TRANSLUCENT_SHADER — это тот же путь, что и у
         * RenderType.entityTranslucent(): полноценная запись глубины и back-face culling.
         * Старый POSITION_COLOR_TEX_LIGHTMAP шейдер на TRIANGLES не писал глубину —
         * отсюда «дырявый череп» и выигрыш пепла в depth-тесте.
         */
        public static final Function<ResourceLocation, RenderType> SKELETON_PARTICLES = Util.memoize(
                texture -> create("skeleton_particles", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 1536, false, false,
                        RenderType.CompositeState.builder()
                                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                                .setCullState(CULL)
                                .setLightmapState(LIGHTMAP)
                                .setOverlayState(OVERLAY)
                                .setDepthTestState(LEQUAL_DEPTH_TEST)
                                .setWriteMaskState(COLOR_DEPTH_WRITE)
                                .createCompositeState(false)));

        /**
         * Пепел (ParticleAshesNT): QUADS, тот же lightmap-шейдер что и у скелета.
         * Тоже пишет глубину (COLOR_DEPTH_WRITE), как кости — тогда в зонах, где другие
         * частицы (облака гриба) оставили NO_DEPTH_TEST, пепел, нарисованный последним,
         * корректно проигрывает depth-тест против глубины костей. Порядок «кости раньше,
         * пепел позже» гарантирован сортировкой в ParticleEngineNT.
         */
        public static final Function<ResourceLocation, RenderType> ASHES_PARTICLES = Util.memoize(
                texture -> create("ashes_particles", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 1536, false, false,
                        RenderType.CompositeState.builder()
                                .setShaderState(POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                                .setCullState(NO_CULL)
                                .setLightmapState(LIGHTMAP)
                                .setDepthTestState(LEQUAL_DEPTH_TEST)
                                .setWriteMaskState(COLOR_DEPTH_WRITE)
                                .createCompositeState(false)));

        /** Вуаль камуфляжа (RedCablePaintableRenderer): спрайт из блочного атласа, прозрачный. */
        public static final RenderType PYLON_OVERLAY = makePylonOverlay();

        private static RenderType makePylonOverlay() {
            //? if < 1.21.1 {
            return create("hbm_m_pylon_overlay", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 4096, false, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(POSITION_COLOR_TEX_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(NO_LIGHTMAP)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false));
            //?} else {
            /*return create("hbm_m_pylon_overlay", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_TEXT_BACKGROUND_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(NO_LIGHTMAP)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false));
            *///?}
        }

        /** Кабели ЛЭП (RedPylonWireRenderer): текстурированные квады wire.png, UV-тайлинг 1/8 блока. */
        public static final Function<ResourceLocation, RenderType> PYLON_WIRE = Util.memoize(
                texture -> {
                    //? if < 1.21.1 {
                    return create("hbm_m_pylon_wire", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 262144, false, false,
                            RenderType.CompositeState.builder()
                                    .setShaderState(POSITION_COLOR_TEX_SHADER)
                                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                                    .setTransparencyState(NO_TRANSPARENCY)
                                    .setCullState(NO_CULL)
                                    .setLightmapState(NO_LIGHTMAP)
                                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                                    .createCompositeState(false));
                    //?} else {
                    /*return create("hbm_m_pylon_wire", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 262144, false, false,
                            RenderType.CompositeState.builder()
                                    .setShaderState(RENDERTYPE_TEXT_BACKGROUND_SHADER)
                                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                                    .setTransparencyState(NO_TRANSPARENCY)
                                    .setCullState(NO_CULL)
                                    .setLightmapState(NO_LIGHTMAP)
                                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                                    .createCompositeState(false));
                    *///?}
                });

        /** Fleija cloud — untextured sphere, full-bright color (порт RenderCloudFleija). */
        public static final RenderType FLEIJA_SPHERE = create("fleija_sphere",                DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 262144, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setWriteMaskState(COLOR_WRITE)
                        .setOutputState(TRANSLUCENT_TARGET)
                        .createCompositeState(false));

        /** Fleija outer shells / shockwave — additive glow. */
        public static final RenderType FLEIJA_SPHERE_ADDITIVE = FLEIJA_SPHERE;

        /** Black hole event horizon — opaque black, writes depth (порт RenderBlackHole sphere). */
        public static final RenderType BHOLE_SPHERE = create("bhole_sphere",
                DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 262144, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(NO_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(false));

        /** Accretion disc / swirl — vertex color × texture (1.7.10 glColor × bindTexture). */
        public static final Function<ResourceLocation, RenderType> BHOLE_DISC = Util.memoize(
                texture -> create("bhole_disc", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 8192, false, false,
                        RenderType.CompositeState.builder()
                                .setShaderState(BHOLE_TEX_COLOR_SHADER)
                                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                                .setDepthTestState(LEQUAL_DEPTH_TEST)
                                .setCullState(NO_CULL)
                                .setLightmapState(NO_LIGHTMAP)
                                .setWriteMaskState(COLOR_WRITE)
                                .setOutputState(TRANSLUCENT_TARGET)
                                .createCompositeState(false)));

        /** Second pass of disc/swirl — additive white glow (1.7.10 GL_SRC_ALPHA, GL_ONE). */
        public static final Function<ResourceLocation, RenderType> BHOLE_DISC_ADDITIVE = Util.memoize(
                texture -> create("bhole_disc_add", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 8192, false, false,
                        RenderType.CompositeState.builder()
                                .setShaderState(BHOLE_TEX_COLOR_SHADER)
                                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                                .setDepthTestState(LEQUAL_DEPTH_TEST)
                                .setCullState(NO_CULL)
                                .setLightmapState(NO_LIGHTMAP)
                                .setWriteMaskState(COLOR_WRITE)
                                .setOutputState(TRANSLUCENT_TARGET)
                                .createCompositeState(false)));

        /** Polar jets — additive, no depth write. */
        public static final RenderType BHOLE_JETS = create("bhole_jets",
                DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 512, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setWriteMaskState(COLOR_WRITE)
                        .setOutputState(TRANSLUCENT_TARGET)
                        .createCompositeState(false));

        /** Fallout rain (RenderFallout): tex × vertex color, без lightmap. */
        public static final Function<ResourceLocation, RenderType> ENTITY_SMOOTH = Util.memoize(
                texture -> create("entity_smooth", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, true, false,
                        RenderType.CompositeState.builder()
                                .setShaderState(NUKE_TEX_COLOR_SHADER)
                                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, true))
                                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                                .setCullState(NO_CULL)
                                .setLightmapState(NO_LIGHTMAP)
                                .setWriteMaskState(COLOR_DEPTH_WRITE)
                                .setOutputState(CLOUDS_TARGET)
                                .createCompositeState(false)));
    }

    public static void highlightBlocks(List<BlockPos> positions) {
        long duration = ModClothConfig.get().obstructionHighlight.obstructionHighlightDuration * 1000L;
        long expiryTime = System.currentTimeMillis() + duration;
        highlightedBlocks.clear(); // Очищаем старые, чтобы не было дубликатов
        for (BlockPos pos : positions) {
            highlightedBlocks.put(pos, expiryTime);
        }
    }

    /**
     * Добавляет блок в список осиротевших (потерявших связь с контроллером) для постоянной подсветки фиолетовым.
     */
    public static void addOrphanedPhantomBlock(BlockPos pos) {
        orphanedPhantomBlocks.put(pos, true);
    }

    /**
     * Удаляет блок из списка осиротевших.
     */
    public static void removeOrphanedPhantomBlock(BlockPos pos) {
        orphanedPhantomBlocks.remove(pos);
    }

    /**
     * Проверяет, осиротел ли фантомный блок (потерял связь с контроллером).
     */
    private static boolean isOrphaned(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(ModBlocks.UNIVERSAL_MACHINE_PART.get())) {
            return false;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof IMultiblockPart part)) {
            return false;
        }

        BlockPos controllerPos = part.getControllerPos();
        if (controllerPos == null) {
            return true; // Нет контроллера = осиротел
        }

        // Проверяем, существует ли контроллер и является ли он валидным
        if (!level.isLoaded(controllerPos)) {
            return false; // Чанк не загружен, не можем проверить
        }

        var controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof IMultiblockController)) {
            return true; // Контроллер не существует или не является контроллером = осиротел
        }

        return false; // Контроллер валиден
    }

    /**
     * Автоматически сканирует область вокруг игрока и находит осиротевшие фантомные блоки.
     * Вызывается из клиентского тика (platform hook).
     * Оптимизировано: проверяет только загруженные чанки в небольшом радиусе.
     */
    public static void onClientTickEnd() {

        // Проверяем раз в 3 секунды (60 тиков) для оптимизации производительности
        tickCounter++;
        if (tickCounter < 60) {
            return;
        }
        tickCounter = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        var level = mc.level;
        BlockPos playerPos = mc.player.blockPosition();
        
        // Оптимизированный радиус сканирования: 16 блоков во все стороны (32x32x32 область)
        // Это покрывает область видимости игрока без излишней нагрузки
        int scanRadius = 16;
        
        // Очищаем старые записи из кэша (старше 5 секунд)
        long currentTime = System.currentTimeMillis();
        knownPhantomBlocks.entrySet().removeIf(entry -> currentTime - entry.getValue() > 5000);
        
        // Сканируем только загруженные чанки вокруг игрока
        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;
        int chunkRadius = (scanRadius + 15) >> 4; // Радиус в чанках
        
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int currentChunkX = chunkX + dx;
                int currentChunkZ = chunkZ + dz;
                
                // Проверяем, загружен ли чанк
                var chunk = level.getChunkSource().getChunk(currentChunkX, currentChunkZ, false);
                if (chunk == null || chunk.isEmpty()) {
                    continue;
                }
                
                // Сканируем только блоки в этом чанке в пределах радиуса
                int minX = Math.max(playerPos.getX() - scanRadius, currentChunkX << 4);
                int maxX = Math.min(playerPos.getX() + scanRadius, (currentChunkX << 4) + 15);
                int minZ = Math.max(playerPos.getZ() - scanRadius, currentChunkZ << 4);
                int maxZ = Math.min(playerPos.getZ() + scanRadius, (currentChunkZ << 4) + 15);
                int minY = Math.max(playerPos.getY() - scanRadius, level.getMinBuildHeight());
                int maxY = Math.min(playerPos.getY() + scanRadius, level.getMaxBuildHeight());
                
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            BlockPos checkPos = new BlockPos(x, y, z);
                            
                            // Быстрая проверка: является ли блок фантомной частью
                            if (!level.getBlockState(checkPos).is(ModBlocks.UNIVERSAL_MACHINE_PART.get())) {
                                continue;
                            }
                            
                            // Добавляем в кэш известных фантомных блоков
                            knownPhantomBlocks.put(checkPos, currentTime);
                            
                            // Проверяем, осиротел ли блок
                            if (isOrphaned(level, checkPos)) {
                                // Блок осиротел - добавляем в подсветку
                                orphanedPhantomBlocks.put(checkPos, true);
                            } else {
                                // Блок валиден - удаляем из подсветки (если был там)
                                orphanedPhantomBlocks.remove(checkPos);
                            }
                        }
                    }
                }
            }
        }
        
        // Также проверяем уже известные фантомные блоки (на случай, если они изменили статус)
        knownPhantomBlocks.keySet().removeIf(pos -> {
            if (!level.isLoaded(pos) || !level.getBlockState(pos).is(ModBlocks.UNIVERSAL_MACHINE_PART.get())) {
                orphanedPhantomBlocks.remove(pos);
                return true; // Удаляем из кэша
            }
            
            // Проверяем статус известного блока
            if (isOrphaned(level, pos)) {
                orphanedPhantomBlocks.put(pos, true);
            } else {
                orphanedPhantomBlocks.remove(pos);
            }
            return false; // Оставляем в кэше
        });
    }

    /**
     * Dedicated, isolated {@link net.minecraft.client.renderer.MultiBufferSource} for
     * highlight overlays. MUST NOT be the shared {@link Minecraft#renderBuffers()}
     * bufferSource: poking the shared source here (getBuffer/endBatch) flushes
     * whatever it currently holds. {@code RenderType.translucentMovingBlock()} — used
     * by Copycats' Sliding/Folding door BER — is NOT a fixed buffer layer, so its
     * quads live in the shared builder; an unconditional {@code getBuffer} +
     * {@code endBatch} here at AFTER_BLOCK_ENTITIES flushes those quads mid-frame
     * instead of at the normal end-of-frame flush, corrupting their translucent
     * compositing on the main render target under Fast/Fancy graphics (where the
     * door renders to the main FB — under Fabulous it is isolated on the
     * item-entity target, so the disturbance is invisible). Keep highlight rendering
     * on a private source so it can never disturb other mods' pending shared-builder
     * geometry. See {@link #onRenderWorldLate}.
     */
    private static volatile net.minecraft.client.renderer.MultiBufferSource.BufferSource highlightBufferSource;

    private static net.minecraft.client.renderer.MultiBufferSource.BufferSource highlightBufferSource() {
        net.minecraft.client.renderer.MultiBufferSource.BufferSource bs = highlightBufferSource;
        if (bs == null) {
            // PlainBufferSource, а НЕ MultiBufferSource.immediate(): под
            // ImmediatelyFast фабрика подменяет источник на BatchableBufferSource,
            // падающий на пустых sortOnUpload-батчах — а HIGHLIGHT_BOX_FILL
            // сортируемый (см. PlainBufferSource).
            //? if < 1.21.1 {
            bs = new com.hbm_m.client.render.PlainBufferSource(
                    new com.mojang.blaze3d.vertex.BufferBuilder(CustomRenderTypes.HIGHLIGHT_BOX_FILL.bufferSize()));
            //?} else {
            /*bs = new com.hbm_m.client.render.PlainBufferSource(
                    new com.mojang.blaze3d.vertex.ByteBufferBuilder(CustomRenderTypes.HIGHLIGHT_BOX_FILL.bufferSize()));
            *///?}
            highlightBufferSource = bs;
        }
        return bs;
    }

    /**
     * Platform hook: render highlight boxes in world.
     *
     * Forge: call from RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES
     * Fabric: call from a late WorldRenderEvents stage (e.g. AFTER_ENTITIES).
     */
    public static void onRenderWorldLate(net.minecraft.client.renderer.MultiBufferSource.BufferSource ignored, com.mojang.blaze3d.vertex.PoseStack poseStack, Vec3 cameraPos) {
        Minecraft mc = Minecraft.getInstance();
        long currentTime = System.currentTimeMillis();
        float alpha = ModClothConfig.get().obstructionHighlight.obstructionHighlightAlpha / 100.0f;

        boolean hasHighlights = !highlightedBlocks.isEmpty() && alpha > 0;
        boolean hasOrphans = !orphanedPhantomBlocks.isEmpty();
        if (!hasHighlights && !hasOrphans) {
            // Nothing to draw — do NOT touch any bufferSource. The old code
            // unconditionally called getBuffer/endBatch(HIGHLIGHT_BOX_FILL) on the
            // shared global bufferSource even with zero HBM blocks present, which
            // flushed other mods' pending shared-builder geometry mid-frame (notably
            // Copycats' sliding/folding doors on a Create train) and broke their
            // translucent compositing under Fast/Fancy graphics.
            return;
        }

        // Private source — never the shared global one (see highlightBufferSource()).
        net.minecraft.client.renderer.MultiBufferSource.BufferSource buf = highlightBufferSource();
        VertexConsumer fillConsumer = buf.getBuffer(CustomRenderTypes.HIGHLIGHT_BOX_FILL);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f effectiveMatrix = poseStack.last().pose();

        // 1. Рендерим временные красные подсветки (препятствия при установке)
        if (!highlightedBlocks.isEmpty() && alpha > 0) {
            Color redColor = Color.RED;
            highlightedBlocks.entrySet().removeIf(entry -> {
                BlockPos pos = entry.getKey();
                if (currentTime > entry.getValue()) {
                    return true;
                }

                boolean drawDown = !highlightedBlocks.containsKey(pos.below());
                boolean drawUp = !highlightedBlocks.containsKey(pos.above());
                boolean drawNorth = !highlightedBlocks.containsKey(pos.north());
                boolean drawSouth = !highlightedBlocks.containsKey(pos.south());
                boolean drawWest = !highlightedBlocks.containsKey(pos.west());
                boolean drawEast = !highlightedBlocks.containsKey(pos.east());

                AABB boundingBox = new AABB(pos).inflate(0.002D);
                renderFilledBox(effectiveMatrix, fillConsumer, boundingBox, cameraPos, redColor, alpha,
                        drawDown, drawUp, drawNorth, drawSouth, drawWest, drawEast, false);
                return false;
            });
        }

        // 2. Рендерим постоянные фиолетовые подсветки (осиротевшие фантомные блоки)
        if (!orphanedPhantomBlocks.isEmpty()) {
            Color purpleColor = new Color(128, 0, 128); // Фиолетовый цвет
            float purpleAlpha = 0.6f; // Полупрозрачность для постоянной подсветки
            var level = mc.level;
            if (level != null) {
                orphanedPhantomBlocks.entrySet().removeIf(entry -> {
                    BlockPos pos = entry.getKey();
                    if (!level.getBlockState(pos).is(com.hbm_m.block.ModBlocks.UNIVERSAL_MACHINE_PART.get())) {
                        return true;
                    }

                    boolean drawDown = !orphanedPhantomBlocks.containsKey(pos.below());
                    boolean drawUp = !orphanedPhantomBlocks.containsKey(pos.above());
                    boolean drawNorth = !orphanedPhantomBlocks.containsKey(pos.north());
                    boolean drawSouth = !orphanedPhantomBlocks.containsKey(pos.south());
                    boolean drawWest = !orphanedPhantomBlocks.containsKey(pos.west());
                    boolean drawEast = !orphanedPhantomBlocks.containsKey(pos.east());

                    AABB boundingBox = new AABB(pos).inflate(0.002D);
                    renderFilledBox(effectiveMatrix, fillConsumer, boundingBox, cameraPos, purpleColor, purpleAlpha,
                            drawDown, drawUp, drawNorth, drawSouth, drawWest, drawEast, false);
                    return false;
                });
            }
        }

        buf.endBatch(CustomRenderTypes.HIGHLIGHT_BOX_FILL);
        poseStack.popPose();
    }

    // Рендерим только те грани куба, которые не примыкают к другим подсвеченным блокам.
    private static void renderFilledBox(Matrix4f matrix, VertexConsumer consumer, AABB box, Vec3 cameraPos, Color color, float alpha,
                                        boolean drawDown, boolean drawUp, boolean drawNorth, boolean drawSouth, boolean drawWest, boolean drawEast, boolean cameraRelative) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;
        if (cameraRelative) {
            minX -= (float) cameraPos.x; minY -= (float) cameraPos.y; minZ -= (float) cameraPos.z;
            maxX -= (float) cameraPos.x; maxY -= (float) cameraPos.y; maxZ -= (float) cameraPos.z;
        }
        
        if (drawDown) {
            //? if < 1.21.1 {
            consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).endVertex();
            //?} else {
            /*consumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, alpha);
            *///?}
        }
        if (drawUp) {
            //? if < 1.21.1 {
            consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).endVertex();
            //?} else {
            /*consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, alpha);
            *///?}
        }
        if (drawNorth) {
            //? if < 1.21.1 {
            consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).endVertex();
            //?} else {
            /*consumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, alpha);
            *///?}
        }
        if (drawSouth) {
            //? if < 1.21.1 {
            consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).endVertex();
            //?} else {
            /*consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, alpha);
            *///?}
        }
        if (drawWest) {
            //? if < 1.21.1 {
            consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).endVertex();
            //?} else {
            /*consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, alpha);
            *///?}
        }
        if (drawEast) {
            //? if < 1.21.1 {
            consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).endVertex();
            //?} else {
            /*consumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, alpha);
            consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, alpha);
            *///?}
        }
    }


}