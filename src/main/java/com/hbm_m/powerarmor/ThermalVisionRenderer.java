package com.hbm_m.powerarmor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.joml.Matrix4f;

import com.hbm_m.lib.RefStrings;
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
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
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

/**
 * Handles all thermal vision rendering effects.
 * Optimized for performance and compatibility with shader mods (Oculus/Embeddium).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ThermalVisionRenderer {

    private static final Random RANDOM = new Random();
    private static boolean isActive = false;
    
    /**
     * Public method to check if thermal vision is currently active.
     */
    public static boolean isThermalVisionActive() {
        return isActive;
    }
    private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
    // Use entityCutoutNoCull for uniform white rendering
    // NoCull ensures all faces are visible, Cutout ensures solid rendering
    private static final RenderType THERMAL_ENTITY_WHITE = RenderType.entityCutoutNoCull(WHITE_TEXTURE);
    private static boolean isRenderingThermalEntityOverlay = false;
    
    // Caches for optimization
    private static final Map<Integer, Boolean> entityVisibilityCache = new HashMap<>();
    private static final java.util.Set<Integer> spectralHighlighted = new java.util.HashSet<>();
    private static final java.util.Set<Integer> spectralHighlightedThisFrame = new java.util.HashSet<>();
    private static final Set<Block> forcedOpaqueTranslucentBlocks = new HashSet<>();
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
        boolean wasActive = isActive;
        if (newActive != isActive) {
            isActive = newActive;
            
            // Apply/remove translucent override only when state changes
            // Note: This feature may not work due to checkClientLoading restriction
            // The try-catch in applyOpaqueTranslucentOverride will handle failures gracefully
            if (isActive && !isSpectralFallbackMode()) {
                applyOpaqueTranslucentOverride(true);
            } else if (wasActive) {
                applyOpaqueTranslucentOverride(false);
            }
        }
    }

    /**
     * Intercepts translucent block rendering to make them opaque.
     * NOTE: This approach is currently not implemented due to complexity.
     * The reflection-based approach should be used instead.
     */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGH)
    public static void onRenderTranslucentBlocks(RenderLevelStageEvent event) {
        // Disabled: Direct interception of translucent rendering is complex and may cause performance issues
        // Using reflection-based render layer override instead
        return;
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
            reconcileSpectralHighlights();
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
        // Disable legacy aura/white overlay rendering to avoid duplicate models.
        // Entity highlighting should be handled either by the shader or by spectral fallback.
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
            handleSpectralHighlight(mc.player, entity);
            return;
        }

        // FULL_SHADER: force solid-white render, skip vanilla render to avoid duplicates
        event.setCanceled(true);
        float partialTick = event.getPartialTick();
        float entityYaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        MultiBufferSource whiteSource = new ThermalWhiteBufferSource(bufferSource);
        int fullBright = 0xF000F0;

        @SuppressWarnings("unchecked")
        net.minecraft.client.renderer.entity.LivingEntityRenderer<LivingEntity, ?> renderer =
            (net.minecraft.client.renderer.entity.LivingEntityRenderer<LivingEntity, ?>) event.getRenderer();

        isRenderingThermalEntityOverlay = true;
        try {
            renderer.render(entity, entityYaw, partialTick, poseStack, whiteSource, fullBright);
        } catch (Exception e) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[ThermalVision] Error rendering white entity for {}", entity.getType(), e);
        } finally {
            isRenderingThermalEntityOverlay = false;
            poseStack.popPose();
            try {
                bufferSource.endBatch(THERMAL_ENTITY_WHITE);
            } catch (Exception e) {
                com.hbm_m.main.MainRegistry.LOGGER.error("[ThermalVision] Error ending white entity batch", e);
            }
        }
    }

    /**
     * BufferSource wrapper for rendering entity aura (glowing outline).
     * Uses reduced alpha and additive blending for glow effect.
     */
    private static final class ThermalAuraBufferSource implements MultiBufferSource {
        private final MultiBufferSource delegate;

        private ThermalAuraBufferSource(MultiBufferSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            VertexFormat format = renderType.format();
            String name = renderType != null ? renderType.toString() : "null";
            String lower = name.toLowerCase(Locale.ROOT);
            
            // Exclude problematic RenderTypes
            if (lower.contains("text") || lower.contains("font") || 
                lower.contains("gui") || lower.contains("overlay") || 
                lower.contains("line") || lower.contains("particle")) {
                return delegate.getBuffer(renderType);
            }
            
            // Wrap entity RenderTypes with aura consumer (reduced alpha for glow)
            if (format == DefaultVertexFormat.NEW_ENTITY || lower.contains("entity")) {
                try {
                    VertexConsumer originalBuffer = delegate.getBuffer(THERMAL_ENTITY_WHITE);
                    return new ThermalAuraVertexConsumer(originalBuffer);
                } catch (Exception e) {
                    return delegate.getBuffer(renderType);
                }
            }
            
            return delegate.getBuffer(renderType);
        }
    }
    
    /**
     * VertexConsumer wrapper for aura effect - white color with reduced alpha for glow.
     */
    private static final class ThermalAuraVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        
        private ThermalAuraVertexConsumer(VertexConsumer delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            return delegate.vertex(x, y, z);
        }
        
        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            // White color with full alpha for maximum glow visibility on dark textures
            return delegate.color(255, 255, 255, 255);
        }
        
        @Override
        public VertexConsumer uv(float u, float v) {
            // Fixed UV to eliminate texture details
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
            // Full alpha for brighter aura
            delegate.defaultColor(255, 255, 255, 255);
        }
        
        @Override
        public void unsetDefaultColor() {
            // Keep white as default
        }
    }

    private static final class ThermalWhiteBufferSource implements MultiBufferSource {
        private final MultiBufferSource delegate;

        private ThermalWhiteBufferSource(MultiBufferSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            // Override ALL entity-related RenderTypes to force white rendering
            // This includes base model, cape, armor layers, held items, etc.
            VertexFormat format = renderType.format();
            String name = renderType != null ? renderType.toString() : "null";
            String lower = name.toLowerCase(Locale.ROOT);
            
            // CRITICAL: Exclude problematic RenderTypes that cause crashes
            // These have different vertex formats or are not entity-related
            if (lower.contains("text") || lower.contains("font") || 
                lower.contains("gui") || lower.contains("overlay") || 
                lower.contains("line") || lower.contains("particle")) {
                return delegate.getBuffer(renderType);
            }
            
            // Override ALL RenderTypes with NEW_ENTITY format (all entity parts use this)
            // This includes: base model, cape, armor, held items, etc.
            if (format == DefaultVertexFormat.NEW_ENTITY) {
                try {
                    // Wrap the buffer with a white color consumer to force solid white
                    // This ensures ALL parts of the mob (body, cape, armor, items) are white
                    VertexConsumer originalBuffer = delegate.getBuffer(THERMAL_ENTITY_WHITE);
                    return new WhiteColorVertexConsumer(originalBuffer);
                } catch (Exception e) {
                    // If white buffer fails, fall back to original to avoid crash
                    return delegate.getBuffer(renderType);
                }
            }
            
            // ALSO check for entity/armor/item-related RenderTypes by name
            // This catches edge cases like cow spots, villager capes, armor layers, glints, held items, etc.
            if (lower.contains("entity") || lower.contains("armor") || lower.contains("glint") || lower.contains("item")) {
                try {
                    // Try to wrap with white consumer for entity-related types
                    VertexConsumer originalBuffer = delegate.getBuffer(THERMAL_ENTITY_WHITE);
                    return new WhiteColorVertexConsumer(originalBuffer);
                } catch (Exception e) {
                    // If white buffer fails, fall back to original
                    return delegate.getBuffer(renderType);
                }
            }

            // For non-entity formats, return original buffer to avoid crashes
            return delegate.getBuffer(renderType);
        }
    }
    
    /**
     * VertexConsumer wrapper that forces all vertices to be white (fullbright).
     * This ensures entities are rendered as solid white silhouettes without any
     * texture detail variations - completely uniform white color.
     * 
     * NOTE: This class is lightweight and creates minimal overhead.
     * Each entity render creates one instance per RenderType, which is acceptable.
     */
    private static final class WhiteColorVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        
        private WhiteColorVertexConsumer(VertexConsumer delegate) {
            this.delegate = delegate;
            // NOTE: Do NOT call defaultColor() here - BufferBuilder may not be ready yet
            // We'll set it when color() is first called, or rely on color() override
        }
        
        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            // CRITICAL: Call vertex first, then color will be applied via defaultColor
            // This ensures proper order and avoids IllegalStateException
            return delegate.vertex(x, y, z);
        }
        
        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            // CRITICAL: Force white color with full alpha - completely ignore original color
            // This eliminates any texture-based color variations
            // NOTE: Do NOT log here - this is called thousands of times per frame!
            // Always return white color regardless of input
            // This ensures all vertices are white, making mobs uniformly visible
            return delegate.color(255, 255, 255, 255);
        }
        
        @Override
        public VertexConsumer uv(float u, float v) {
            // CRITICAL: Set UV to fixed value (center of texture) to eliminate texture detail variations
            // This makes the entire mob appear as a solid white silhouette without texture details
            // Using 0.5, 0.5 ensures we sample a consistent pixel from the white texture
            // All vertices will use the same texture pixel, making the mob uniformly white
            return delegate.uv(0.5f, 0.5f);
        }
        
        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            // Force no overlay (no hurt/flash tinting) to keep uniform white tone
            return delegate.overlayCoords(0, 0);
        }
        
        @Override
        public VertexConsumer uv2(int u, int v) {
            // CRITICAL: Set lightmap to fullbright (0xF0, 0xF0 = sky light 15, block light 15)
            // This ensures uniform brightness regardless of time of day
            return delegate.uv2(0xF0, 0xF0);
        }
        
        @Override
        public VertexConsumer normal(float x, float y, float z) {
            // CRITICAL: Set normal to face camera (0, 0, 1) to eliminate lighting variations
            // This ensures uniform brightness regardless of surface orientation
            // Without this, different parts of the mob may appear darker/lighter based on normals
            // Setting all normals to the same value ensures completely uniform lighting
            return delegate.normal(0.0f, 0.0f, 1.0f);
        }
        
        @Override
        public void endVertex() {
            delegate.endVertex();
        }
        
        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            // Always set default color to white
            delegate.defaultColor(255, 255, 255, 255);
        }
        
        @Override
        public void unsetDefaultColor() {
            // Don't unset - keep white as default
            // delegate.unsetDefaultColor();
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
        
        ClipContext ctx = new ClipContext(start, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player);
        BlockHitResult hit = player.level().clip(ctx);
        
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockState state = player.level().getBlockState(hit.getBlockPos());
            // Check for glass or other transparent but solid blocks
            // If it can occlude, it blocks thermal vision (so you can't see through walls)
            // But we also want to block vision through glass
            
            // Standard check: if it's not air and not something passable like grass
            // We consider it blocking for thermal vision
            return state.isAir() || !state.canOcclude();
        }
        
        return true;
    }

    private static boolean isSpectralFallbackMode() {
        return ModClothConfig.get().thermalRenderMode == ModClothConfig.ThermalRenderMode.SPECTRAL_FALLBACK;
    }

    private static void handleSpectralHighlight(LocalPlayer player, LivingEntity entity) {
        if (player == null || player.level() == null) {
            return;
        }
        if (!isEntityVisibleForThermal(player, entity)) {
            return;
        }
        entity.setGlowingTag(true);
        spectralHighlightedThisFrame.add(entity.getId());
    }

    /**
     * Attempts to make translucent blocks (water, glass, etc.) render as opaque solid blocks.
     * Uses try-catch to handle IllegalStateException from checkClientLoading check.
     * 
     * NOTE: This approach has limitations:
     * - Already-rendered chunks may not update immediately (may require chunk reload)
     * - The checkClientLoading check will fail during runtime, so this feature may not work
     * - This is a known limitation of Minecraft 1.20.1 - render layers can only be set during client loading
     * 
     * @param enable If true, force translucent blocks to solid; if false, restore original render types
     */
    private static void applyOpaqueTranslucentOverride(boolean enable) {
        // Store original render types and override translucent -> solid
        // Use reflection to bypass the "client loading only" check
        if (enable) {
            if (!forcedOpaqueTranslucentBlocks.isEmpty()) {
                return; // Already applied
            }
            int count = 0;
            
            // Mixin bypasses checkClientLoading when thermal vision is active
            // We can now call setRenderLayer directly without reflection
            try {
                for (Block block : BuiltInRegistries.BLOCK) {
                    try {
                        BlockState defaultState = block.defaultBlockState();
                        RenderType currentType = ItemBlockRenderTypes.getChunkRenderType(defaultState);
                        
                        // Check if block is translucent by multiple methods
                        boolean isTranslucent = false;
                        
                        // Method 1: Direct comparison
                        if (currentType == RenderType.translucent() || currentType == RenderType.tripwire()) {
                            isTranslucent = true;
                        }
                        // Method 2: Check by render type name
                        else {
                            String typeName = currentType.toString().toLowerCase(Locale.ROOT);
                            if (typeName.contains("translucent") || typeName.contains("tripwire")) {
                                isTranslucent = true;
                            }
                        }
                        // Method 3: Check common translucent blocks by ID
                        if (!isTranslucent) {
                            var blockId = BuiltInRegistries.BLOCK.getKey(block);
                            if (blockId != null) {
                                String idStr = blockId.toString().toLowerCase(Locale.ROOT);
                                // Common translucent blocks: water, ice, glass, stained glass, etc.
                                if (idStr.contains("water") || idStr.contains("glass") || 
                                    idStr.contains("ice") || idStr.contains("slime") ||
                                    idStr.contains("honey") || idStr.contains("bubble")) {
                                    // Check if it's actually translucent
                                    RenderType checkType = ItemBlockRenderTypes.getChunkRenderType(defaultState);
                                    String checkName = checkType.toString().toLowerCase(Locale.ROOT);
                                    if (checkName.contains("translucent") || checkName.contains("cutout")) {
                                        isTranslucent = true;
                                    }
                                }
                            }
                        }
                        
                        if (isTranslucent) {
                            // Force to solid render type
                            // Try-catch fallback: catch IllegalStateException if checkClientLoading fails
                            try {
                                ItemBlockRenderTypes.setRenderLayer(block, RenderType.solid());
                                forcedOpaqueTranslucentBlocks.add(block);
                                count++;
                            } catch (IllegalStateException e) {
                                // checkClientLoading check failed - this is expected during runtime
                                // Log only first few errors to avoid spam
                                if (count < 3 && com.hbm_m.main.MainRegistry.LOGGER.isDebugEnabled()) {
                                    com.hbm_m.main.MainRegistry.LOGGER.debug("[ThermalVision] Cannot change render layer for {} at runtime: {}", block, e.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Log first few errors for debugging
                        if (count < 5 && com.hbm_m.main.MainRegistry.LOGGER.isDebugEnabled()) {
                            com.hbm_m.main.MainRegistry.LOGGER.debug("[ThermalVision] Error checking block {}: {}", block, e.getMessage());
                        }
                    }
                }
                
                if (count > 0) {
                    com.hbm_m.main.MainRegistry.LOGGER.info("[ThermalVision] Successfully forced {} translucent blocks to solid", count);
                } else {
                    com.hbm_m.main.MainRegistry.LOGGER.warn("[ThermalVision] No translucent blocks were converted to solid. This feature may not work due to runtime restrictions.");
                }
            } catch (Exception e) {
                com.hbm_m.main.MainRegistry.LOGGER.error("[ThermalVision] Failed to apply opaque override: {}", e.getMessage());
                com.hbm_m.main.MainRegistry.LOGGER.debug("[ThermalVision] Error details: ", e);
            }
        } else {
            if (forcedOpaqueTranslucentBlocks.isEmpty()) {
                return;
            }
            int count = forcedOpaqueTranslucentBlocks.size();
            
            // Try-catch fallback: catch IllegalStateException if checkClientLoading fails
            try {
                // Restore original translucent render type
                for (Block block : forcedOpaqueTranslucentBlocks) {
                    try {
                        ItemBlockRenderTypes.setRenderLayer(block, RenderType.translucent());
                    } catch (IllegalStateException e) {
                        // checkClientLoading check failed - ignore restoration errors
                        // This is expected during runtime
                    } catch (Exception ignored) {
                        // ignore other restoration errors
                    }
                }
                
                forcedOpaqueTranslucentBlocks.clear();
                com.hbm_m.main.MainRegistry.LOGGER.info("[ThermalVision] Restored {} blocks to translucent", count);
            } catch (Exception e) {
                com.hbm_m.main.MainRegistry.LOGGER.error("[ThermalVision] Failed to restore translucent blocks: {}", e.getMessage());
                forcedOpaqueTranslucentBlocks.clear();
            }
        }
    }

    private static void reconcileSpectralHighlights() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            clearSpectralHighlights();
            return;
        }

        for (Integer id : spectralHighlighted) {
            if (!spectralHighlightedThisFrame.contains(id)) {
                net.minecraft.world.entity.Entity entity = mc.level.getEntity(id);
                if (entity instanceof LivingEntity living) {
                    living.setGlowingTag(false);
                }
            }
        }

        spectralHighlighted.clear();
        spectralHighlighted.addAll(spectralHighlightedThisFrame);
        spectralHighlightedThisFrame.clear();
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
