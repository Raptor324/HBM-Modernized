package com.hbm_m.powerarmor.overlay;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.joml.Matrix4f;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.powerarmor.ModEventHandlerClient;
import com.hbm_m.config.ModClothConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


// Handles all thermal vision rendering effects.

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ThermalVisionRenderer {

    private static final Random RANDOM = new Random();
    private static boolean isActive = false;
    

    // Public method to check if thermal vision is currently active.

    public static boolean isThermalVisionActive() {
        return isActive;
    }
    private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
    // Solid entity render type used for thermal entity passes (outline + white silhouette).
    private static final RenderType THERMAL_ENTITY_SOLID = RenderType.entityCutoutNoCull(WHITE_TEXTURE);
    // Outline-only render type for spectral fallback (draws only outline, not a filled second model).
    private static final RenderType THERMAL_ENTITY_OUTLINE = RenderType.outline(WHITE_TEXTURE);
    private static boolean isRenderingThermalEntityOverlay = false;
    
    // Caches for optimization
    private static final Map<Integer, Boolean> entityVisibilityCache = new HashMap<>();
    private static final java.util.Set<Integer> spectralHighlighted = new java.util.HashSet<>();
    private static final java.util.Set<Integer> spectralHighlightedThisFrame = new java.util.HashSet<>();
    private static long lastCacheClearTime = 0;
    private static long lastInterferenceUpdate = 0;
    private static int interferenceOffset = 0;
    
    // Performance settings
    private static final int CACHE_LIFETIME_MS = 100; // Clear visibility cache every 100ms
    private static final double MAX_VISION_DISTANCE = 64.0;
    private static final double MAX_VISION_DISTANCE_SQR = MAX_VISION_DISTANCE * MAX_VISION_DISTANCE;
    
    /**
     * Updates the thermal vision state. Called from client tick.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        // Update interference animation
        long time = System.currentTimeMillis();
        if (time - lastInterferenceUpdate > 50) { // 20 FPS animation
            lastInterferenceUpdate = time;
            interferenceOffset = RANDOM.nextInt(100);
        }
        
        // Clear caches periodically
        if (time - lastCacheClearTime > CACHE_LIFETIME_MS) {
            entityVisibilityCache.clear();
            lastCacheClearTime = time;
        }
        
        // Update active state from ModEventHandlerClient
        boolean newActive = ModEventHandlerClient.isThermalActive();
        if (newActive != isActive) {
            isActive = newActive;
        }
    }

    /**
     * Renders the desaturated world overlay.
     * Uses post-processing overlay with multiply blend mode for grayscale effect.
     */
    @SubscribeEvent
    public static void onRenderWorldPost(RenderLevelStageEvent event) {
        // Use AFTER_LEVEL to render after all world elements but before GUI
        // AFTER_LEVEL is the correct stage for post-processing effects
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        
        if (!isActive) {
            return;
        }

        if (isSpectralFallbackMode()) {
            // Spectral fallback intentionally does not alter world rendering.
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        
        // Use main render target dimensions to match the world buffer
        var mainTarget = mc.getMainRenderTarget();
        int width = mainTarget.width;
        int height = mainTarget.height;

        // Render thermal vision effect
        renderWorldDesaturationOverlay(width, height);
    }

    /**
     * Render a white fullbright pass for living entities during thermal vision.
     * Uses Post event to render AFTER the main entity render, so our white overlay
     * appears on top and is visible. Uses separate PoseStack to avoid stack imbalance.
     * 
     * Priority LOWEST ensures this runs after all other entity rendering handlers.
     */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (!isActive) return;
        if (isRenderingThermalEntityOverlay) return;

        LivingEntity entity = event.getEntity();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity == mc.player) return;
        if (entity.isInvisible()) return;
        if (!(event.getRenderer() instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?>)) {
            return;
        }

        if (isSpectralFallbackMode()) {
            // Render ONLY outline (no second filled model), and only if visible (no through walls).
            if (entity instanceof net.minecraft.world.entity.player.Player) return;
            if (!isEntityVisibleForThermal(mc.player, entity)) return;

            float partialTick = event.getPartialTick();
            float entityYaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());

            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();

            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
            MultiBufferSource outlineSource = new ThermalOutlineOnlyBufferSource(bufferSource);
            int packedLight = 0xF000F0;

            @SuppressWarnings("unchecked")
            net.minecraft.client.renderer.entity.LivingEntityRenderer<LivingEntity, ?> renderer =
                (net.minecraft.client.renderer.entity.LivingEntityRenderer<LivingEntity, ?>) event.getRenderer();

            isRenderingThermalEntityOverlay = true;
            try {
                renderer.render(entity, entityYaw, partialTick, poseStack, outlineSource, packedLight);
            } catch (Exception e) {
                com.hbm_m.main.MainRegistry.LOGGER.error("[ThermalVision] Error rendering fallback outline for {}", entity.getType(), e);
            } finally {
                isRenderingThermalEntityOverlay = false;
                poseStack.popPose();
                try {
                    bufferSource.endBatch(THERMAL_ENTITY_OUTLINE);
                } catch (Exception e) {
                    com.hbm_m.main.MainRegistry.LOGGER.error("[ThermalVision] Error ending fallback outline batch", e);
                }
            }
            return;
        }

        // FULL_SHADER is handled in Pre (cancel + custom render)
        return;

    }

    /**
     * Entity handling before render:
     * - FULL_SHADER: render entity in solid white and cancel original render
     * - SPECTRAL_FALLBACK: apply glowing outline (not through walls)
     */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onRenderLivingPreSpectral(RenderLivingEvent.Pre<?, ?> event) {
        if (!isActive) return;
        if (isRenderingThermalEntityOverlay) return;

        LivingEntity entity = event.getEntity();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity == mc.player) return;
        if (entity.isInvisible()) return;
        if (entity instanceof net.minecraft.world.entity.player.Player) return;

        if (isSpectralFallbackMode()) {
            // Fallback handled in Post (outline-only pass).
            return;
        }

        // FULL_SHADER: force solid-white render, skip vanilla render to avoid duplicates
        event.setCanceled(true);
        float partialTick = event.getPartialTick();
        float entityYaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        MultiBufferSource whiteSource = new ThermalSolidColorBufferSource(bufferSource, 255, 255, 255, 255);
        int fullBright = 0xF000F0;

        @SuppressWarnings("unchecked")
        net.minecraft.client.renderer.entity.LivingEntityRenderer<LivingEntity, ?> renderer =
            (net.minecraft.client.renderer.entity.LivingEntityRenderer<LivingEntity, ?>) event.getRenderer();

        isRenderingThermalEntityOverlay = true;
        try {
            // Single-pass solid white (no scaled duplicate model).
            renderer.render(entity, entityYaw, partialTick, poseStack, whiteSource, fullBright);
        } catch (Exception e) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[ThermalVision] Error rendering white entity for {}", entity.getType(), e);
        } finally {
            isRenderingThermalEntityOverlay = false;
            poseStack.popPose();
            try {
                bufferSource.endBatch(THERMAL_ENTITY_SOLID);
            } catch (Exception e) {
                com.hbm_m.main.MainRegistry.LOGGER.error("[ThermalVision] Error ending white entity batch", e);
            }
        }
    }

    /**
     * MultiBufferSource which forces a single solid color for all entity vertex data,
     * while routing all entity-related RenderTypes into our own solid RenderType.
     */
    private static final class ThermalSolidColorBufferSource implements MultiBufferSource {
        private final MultiBufferSource delegate;
        private final int r;
        private final int g;
        private final int b;
        private final int a;

        private ThermalSolidColorBufferSource(MultiBufferSource delegate, int r, int g, int b, int a) {
            this.delegate = delegate;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            VertexFormat format = renderType.format();
            String name = renderType != null ? renderType.toString() : "null";
            String lower = name.toLowerCase(Locale.ROOT);

            // Exclude non-entity types to avoid crashes
            if (lower.contains("text") || lower.contains("font") ||
                lower.contains("gui") || lower.contains("overlay") ||
                lower.contains("line") || lower.contains("particle")) {
                return delegate.getBuffer(renderType);
            }

            // Route entity-ish render types into our solid pass render type
            if (format == DefaultVertexFormat.NEW_ENTITY || lower.contains("entity") || lower.contains("armor") || lower.contains("glint") || lower.contains("item")) {
                VertexConsumer originalBuffer = delegate.getBuffer(THERMAL_ENTITY_SOLID);
                return new SolidColorVertexConsumer(originalBuffer, r, g, b, a);
            }

            return delegate.getBuffer(renderType);
        }
    }

    /**
     * VertexConsumer wrapper forcing a constant RGBA + fullbright + fixed UV.
     * Used for both outline (black) and white-hot pass.
     */
    private static final class SolidColorVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int r;
        private final int g;
        private final int b;
        private final int a;

        private SolidColorVertexConsumer(VertexConsumer delegate, int r, int g, int b, int a) {
            this.delegate = delegate;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            return delegate.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return delegate.color(r, g, b, a);
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return delegate.uv(0.5f, 0.5f);
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return delegate.overlayCoords(0, 0);
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return delegate.uv2(0xF0, 0xF0);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return delegate.normal(0.0f, 0.0f, 1.0f);
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            delegate.defaultColor(r, g, b, a);
        }

        @Override
        public void unsetDefaultColor() {
            // keep forced default
        }
    }



    private static final class NoopVertexConsumer implements VertexConsumer {
        @Override
        public VertexConsumer vertex(double x, double y, double z) { return this; }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) { return this; }

        @Override
        public VertexConsumer uv(float u, float v) { return this; }

        @Override
        public VertexConsumer overlayCoords(int u, int v) { return this; }

        @Override
        public VertexConsumer uv2(int u, int v) { return this; }

        @Override
        public VertexConsumer normal(float x, float y, float z) { return this; }

        @Override
        public void endVertex() { }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) { }

        @Override
        public void unsetDefaultColor() { }
    }

    /**
     * Renders the HUD overlay (subtle filter, vignette, interference, text).
     * Called from ModEventHandlerClient.
     */
    public static void renderThermalOverlay(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!isActive) return;

        if (!isSpectralFallbackMode()) {
            // Render subtle world filter (light tint, not aggressive darkening)
            renderWorldFilterGUI(guiGraphics, screenWidth, screenHeight);
            
            // Render vignette and interference
            renderVignetteAndInterference(guiGraphics, screenWidth, screenHeight);
        }
        
        // Render text
        Minecraft mc = Minecraft.getInstance();
        String thermalText = "THERMAL VISION";
        guiGraphics.drawString(mc.font, thermalText, 10, 10, 0x00FF00);
    }

    public static void clearSpectralHighlights() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            spectralHighlighted.clear();
            spectralHighlightedThisFrame.clear();
            return;
        }
        for (Integer id : spectralHighlighted) {
            net.minecraft.world.entity.Entity entity = mc.level.getEntity(id);
            if (entity instanceof LivingEntity living) {
                living.setGlowingTag(false);
            }
        }
        spectralHighlighted.clear();
        spectralHighlightedThisFrame.clear();
    }
    
    /**
     * Public API for checking entity visibility with thermal vision logic.
     * Uses caching for performance.
     * 
     * NOTE: Entity rendering (white color) is handled by Body Camera Shader
     * through gbuffers_entities.fsh when TI == 1. This method is kept for
     * potential future use but is not currently used for entity rendering.
     */
    public static boolean isEntityVisibleForThermal(LocalPlayer player, LivingEntity entity) {
        if (!isActive) return false;
        
        // Distance check
        if (player.distanceToSqr(entity) > MAX_VISION_DISTANCE_SQR) return false;
        
        return isEntityVisible(player, entity);
    }
    
    // --- Implementation Details ---
    
    private static boolean isEntityVisible(LocalPlayer player, LivingEntity entity) {
        int entityId = entity.getId();
        if (entityVisibilityCache.containsKey(entityId)) {
            return entityVisibilityCache.get(entityId);
        }
        
        boolean visible = checkVisibilityRaycast(player, entity);
        entityVisibilityCache.put(entityId, visible);
        return visible;
    }
    
    private static boolean checkVisibilityRaycast(LocalPlayer player, LivingEntity entity) {
        Vec3 start = player.getEyePosition();
        Vec3 end = entity.getEyePosition();
        
        // COLLIDER ignores non-solid visuals (tall grass etc), but still blocks on glass/walls.
        ClipContext ctx = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult hit = player.level().clip(ctx);
        
        // If anything solid is hit, entity is NOT visible (no through walls/glass).
        return hit.getType() != HitResult.Type.BLOCK;
    }

    private static boolean isSpectralFallbackMode() {
        return ModClothConfig.get().thermalRenderMode == ModClothConfig.ThermalRenderMode.SPECTRAL_FALLBACK;
    }



    /**
     * MultiBufferSource which routes entity render types into an OUTLINE RenderType,
     * forcing a constant white color. This produces a glowing-style outline without
     * drawing a second filled model.
     */
    private static final class ThermalOutlineOnlyBufferSource implements MultiBufferSource {
        private final MultiBufferSource delegate;

        private ThermalOutlineOnlyBufferSource(MultiBufferSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            VertexFormat format = renderType.format();
            String name = renderType != null ? renderType.toString() : "null";
            String lower = name.toLowerCase(Locale.ROOT);

            // Exclude non-entity types
            if (lower.contains("text") || lower.contains("font") ||
                lower.contains("gui") || lower.contains("overlay") ||
                lower.contains("line") || lower.contains("particle")) {
                return delegate.getBuffer(renderType);
            }

            // Route entity-ish render types into our outline-only RenderType.
            if (format == DefaultVertexFormat.NEW_ENTITY || lower.contains("entity") || lower.contains("armor") || lower.contains("glint") || lower.contains("item")) {
                VertexConsumer originalBuffer = delegate.getBuffer(THERMAL_ENTITY_OUTLINE);
                return new SolidColorVertexConsumer(originalBuffer, 255, 255, 255, 255);
            }

            return delegate.getBuffer(renderType);
        }
    }
    
    /**
     * Renders thermal vision effect using the built-in shader.
     * Applies thermal color tint, interference effects, and scanlines.
     */
    private static void renderWorldDesaturationOverlay(int width, int height) {
        // Get the thermal vision shader
        net.minecraft.client.renderer.ShaderInstance shader = com.hbm_m.client.render.ModShaders.getThermalVisionShader();
        
        if (shader == null) {
            // Fallback: use simple overlay if shader is not available
            renderFallbackOverlay(width, height);
            return;
        }
        
        // Bind the main render target as the source texture for Sampler0.
        // We sample the already-rendered scene and apply the thermal effect in shader.
        var mainTarget = Minecraft.getInstance().getMainRenderTarget();
        mainTarget.bindRead();
        RenderSystem.setShaderTexture(0, mainTarget.getColorTextureId());
        
        // Save current projection matrix
        Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
        
        // Set orthographic projection for screen space (0,0 to width,height)
        Matrix4f orthoMatrix = new Matrix4f().ortho(0.0f, width, height, 0.0f, -1000.0f, 3000.0f);
        RenderSystem.setProjectionMatrix(orthoMatrix, RenderSystem.getVertexSorting());
        
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        
        // Use thermal vision shader
        RenderSystem.setShader(() -> shader);
        
        // Set shader uniforms
        try {
            // Set WorldTime uniform (day/night transitions)
            var worldTimeUniform = shader.getUniform("WorldTime");
            if (worldTimeUniform != null) {
                Minecraft mc = Minecraft.getInstance();
                float worldTime = mc.level != null ? (float) (mc.level.getDayTime() % 24000L) : 0.0f;
                worldTimeUniform.set(worldTime);
            }
            
            // Set ProjMat uniform (projection matrix)
            var projMatUniform = shader.getUniform("ProjMat");
            if (projMatUniform != null) {
                projMatUniform.set(orthoMatrix);
            }
            
            // Note: Sampler0 (screen texture) is not available in vanilla Minecraft
            // The shader works as an overlay effect without screen texture
        } catch (Exception e) {
            // Fallback to simple overlay if shader fails
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            renderFallbackOverlay(width, height);
            return;
        }
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        
        // Render fullscreen quad with shader
        // Using POSITION_TEX format (shader expects Position and UV0)
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        
        // Fullscreen quad with UV coordinates (0,0 to 1,1)
        // Flip V to match framebuffer texture orientation
        buffer.vertex(0, height, 0).uv(0.0f, 0.0f).endVertex();
        buffer.vertex(width, height, 0).uv(1.0f, 0.0f).endVertex();
        buffer.vertex(width, 0, 0).uv(1.0f, 1.0f).endVertex();
        buffer.vertex(0, 0, 0).uv(0.0f, 1.0f).endVertex();
        
        tesselator.end();
        
        // Restore OpenGL state
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        
        // Restore original projection matrix
        RenderSystem.setProjectionMatrix(projMatrix, RenderSystem.getVertexSorting());
    }
    
    /**
     * Renders thermal vision effect as a very subtle overlay.
     * Uses multiple passes with low intensity to tint the screen without covering it.
     */
    private static void renderFallbackOverlay(int width, int height) {
        // Save current projection matrix
        Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
        
        // Set orthographic projection for screen space
        Matrix4f orthoMatrix = new Matrix4f().ortho(0.0f, width, height, 0.0f, -1000.0f, 3000.0f);
        RenderSystem.setProjectionMatrix(orthoMatrix, RenderSystem.getVertexSorting());
        
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        
        // Pass 1: Very subtle thermal vision color tint (green)
        // Use multiply blend to tint existing colors, not cover them
        RenderSystem.blendFunc(
            GlStateManager.SourceFactor.DST_COLOR,
            GlStateManager.DestFactor.ZERO
        );
        
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // Thermal color: R=0.29, G=0.75, B=0.39
        // Use medium-high gray values to tint towards thermal color without darkening
        float r = 0.85f, g = 0.92f, b = 0.88f; // Slightly green-tinted gray
        float alpha = 0.15f; // Low alpha for subtle effect
        
        buffer.vertex(0, height, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(width, height, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(width, 0, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(0, 0, 0).color(r, g, b, alpha).endVertex();
        
        tesselator.end();
        
        // Pass 2: Very subtle desaturation (grayscale overlay)
        // Use multiply blend to slightly desaturate without darkening
        RenderSystem.blendFunc(
            GlStateManager.SourceFactor.DST_COLOR,
            GlStateManager.DestFactor.ZERO
        );
        
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // High gray value for minimal darkening, just slight desaturation
        float gray = 0.95f; // Very high gray value = minimal darkening
        float alpha2 = 0.08f; // Very low alpha for subtle effect
        
        buffer.vertex(0, height, 0).color(gray, gray, gray, alpha2).endVertex();
        buffer.vertex(width, height, 0).color(gray, gray, gray, alpha2).endVertex();
        buffer.vertex(width, 0, 0).color(gray, gray, gray, alpha2).endVertex();
        buffer.vertex(0, 0, 0).color(gray, gray, gray, alpha2).endVertex();
        
        tesselator.end();
        
        // Restore OpenGL state
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        
        // Restore original projection matrix
        RenderSystem.setProjectionMatrix(projMatrix, RenderSystem.getVertexSorting());
    }
    
    /**
     * Renders world filter in GUI space (for overlay system).

     * Now only vignette and scanlines are rendered here.
     */
    private static void renderWorldFilterGUI(GuiGraphics gfx, int width, int height) {
        // Removed black overlay - it was covering the world desaturation
        // World desaturation is handled in renderWorldDesaturationOverlay()
    }
    
    private static void renderVignetteAndInterference(GuiGraphics gfx, int width, int height) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        
        // 1. Vignette (Dark corners)
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        // We draw 4 quads around the edges or use a gradient approach
        // Simple approach: Full screen quad with gradient alpha would require shaders or texture
        // Here we'll draw a border to darken edges
        
        float vR = 0.0f, vG = 0.0f, vB = 0.0f, vA = 0.4f; // Reduced vignette opacity
        int border = Math.min(width, height) / 4;
        
        // Top
        drawGradientRect(buffer, 0, 0, width, border, vR, vG, vB, vA, vR, vG, vB, 0.0f);
        // Bottom
        drawGradientRect(buffer, 0, height - border, width, height, vR, vG, vB, 0.0f, vR, vG, vB, vA);
        // Left
        drawGradientRectVertical(buffer, 0, 0, border, height, vR, vG, vB, vA, vR, vG, vB, 0.0f);
        // Right
        drawGradientRectVertical(buffer, width - border, 0, width, height, vR, vG, vB, 0.0f, vR, vG, vB, vA);
        
        tesselator.end();
        
        // 2. Scanlines / Interference
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        float iR = 0.7f, iG = 0.7f, iB = 0.7f, iA = 0.1f;
        
        // Only draw a few random scanlines to simulate noise without killing performance
        for (int i = 0; i < 5; i++) {
            int lineY = (interferenceOffset * 2 + i * 50) % height;
            int lineHeight = 2;
            
            buffer.vertex(0, lineY + lineHeight, 0).color(iR, iG, iB, iA).endVertex();
            buffer.vertex(width, lineY + lineHeight, 0).color(iR, iG, iB, iA).endVertex();
            buffer.vertex(width, lineY, 0).color(iR, iG, iB, iA).endVertex();
            buffer.vertex(0, lineY, 0).color(iR, iG, iB, iA).endVertex();
        }
        
        tesselator.end();
        RenderSystem.disableBlend();
    }
    
    private static void drawGradientRect(BufferBuilder buffer, int x1, int y1, int x2, int y2, 
                                        float r1, float g1, float b1, float a1, 
                                        float r2, float g2, float b2, float a2) {
        buffer.vertex(x1, y2, 0).color(r2, g2, b2, a2).endVertex();
        buffer.vertex(x2, y2, 0).color(r2, g2, b2, a2).endVertex();
        buffer.vertex(x2, y1, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(x1, y1, 0).color(r1, g1, b1, a1).endVertex();
    }
    
    private static void drawGradientRectVertical(BufferBuilder buffer, int x1, int y1, int x2, int y2, 
                                                float r1, float g1, float b1, float a1, 
                                                float r2, float g2, float b2, float a2) {
        buffer.vertex(x1, y2, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(x2, y2, 0).color(r2, g2, b2, a2).endVertex();
        buffer.vertex(x2, y1, 0).color(r2, g2, b2, a2).endVertex();
        buffer.vertex(x1, y1, 0).color(r1, g1, b1, a1).endVertex();
    }
}
