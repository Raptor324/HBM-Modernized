package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.client.render.machine.MachineBer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

@OnlyIn(Dist.CLIENT)
/**
 * <b>BlockEntity dispatcher bypass in the MAIN pass</b> for Nucleus engine
 * machines ({@code MachineRenderers}) — and ONLY for them; all other mod BEs
 * go through the normal path.
 * <p>
 * The shadow pass is intentionally NOT bypassed: the dispatcher costs ~3% of
 * the frame there and records work fine (profile 0914 04:04: shadows broke
 * when the shadow pass was bypassed). The entire win is in main (25.4%:
 * skipping the iteration of 600 machines by Sodium).
 * <p>
 * Scheme:
 * <ol>
 *   <li>{@code collectMain} is called explicitly at {@code AFTER_ENTITIES} —
 *       BEFORE Embeddium's BE iteration: flat loop over {@link #LIVE},
 *       pose = eventPose * T(be-cam); the {@code mainCollected} flag is set
 *       EARLY.</li>
 *   <li>Dispatcher mixin in main: managed BEs are cancelled (already drawn),
 *       newcomers are registered in {@link #LIVE} (drawn from the next frame;
 *       to avoid flicker a newcomer is rendered singly right away — see
 *       shouldBypass).</li>
 *   <li>Shadow pass: the mixin always returns {@code false} — normal path.</li>
 * </ol>
 * Unloading is handled by pruning via {@code be.isRemoved()/level} on every
 * collectMain. Kill-switch: {@code -Dhbm.dispatcherBypass=false}.
 */
public final class NucleusDispatcherBypass {

    /** Emergency JVM override -Dhbm.dispatcherBypass=false; primary source is the nucleusDispatcherBypass config. */
    private static final boolean KILL_SWITCH =
            !"false".equalsIgnoreCase(System.getProperty("hbm.dispatcherBypass", "true"));

    /** BE types managed by the MachineRenderers factory (registerManaged at spec registration). */
    private static final Set<BlockEntityType<?>> MANAGED_TYPES = new HashSet<>();

    /** Live machines on the client level (populated by dispatcher calls in the main pass). */
    private static final LinkedHashSet<BlockEntity> LIVE = new LinkedHashSet<>(256);

    private static boolean mainCollected = false;

    /** Scratch: normal matrix part of the basis for the shared PoseStack. */
    private static final Matrix3f NORMAL_SCRATCH = new Matrix3f();
    // collectMain scratch: basis + PoseStack are reused every frame
    // (previously 2 allocations per frame were wasted). Not reentrant: collectOne
    // sets pose/base at the start of each call, no nested collects occur.
    private static final Matrix4f BASE_SCRATCH = new Matrix4f();
    private static final PoseStack POSE_SCRATCH = new PoseStack();

    private NucleusDispatcherBypass() {}

    public static void registerManaged(BlockEntityType<?> type) {
        MANAGED_TYPES.add(type);
    }

    static boolean isEnabled() {
        return com.hbm_m.config.ModClothConfig.get().nucleusDispatcherBypass && KILL_SWITCH
                && !ClientRenderFlags.forceVanillaImmediate()
                && ClientRenderFlags.useInstancedBatching();
    }

    /** Resets the main-pass flag (called from IrisShadowBatchCollector.noteMainFrameStart). */
    public static void noteMainFrameStart() {
        mainCollected = false;
    }

    /**
     * Dispatcher mixin decision point. MAIN: cancel collected machines + register
     * newcomers (singly rendered newcomer prevents flicker). SHADOW: always false.
     */
    public static boolean shouldBypass(BlockEntity be, float partialTick,
                                       PoseStack poseStack, MultiBufferSource buffers) {
        if (!isEnabled() || !isManaged(be)) {
            return false;
        }
        if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return false; // shadows take the normal path (cheap and working)
        }
        // Sable sublevel / Create contraption: the dispatcher (Sable) supplies a
        // ready transform, while collectOne cannot translate the BE position
        // correctly (beyond 20M+ blocks the distance cap and float precision
        // destroy it). Never intercept such a render - it goes through the
        // normal dispatcher path.
        if (com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(be)) {
            return false;
        }
        boolean known = LIVE.contains(be);
        if (!known) {
            LIVE.add(be);
        }
        if (!mainCollected) {
            return false; // collect has not run yet - draw normally and accumulate the list
        }
        if (known) {
            return true; // already drawn by the collect loop
        }
        // Newcomer of the frame: render singly so it does not flicker until the next frame.
        collectOne(be, partialTick, new PoseStack(), buffers, bypassBase(poseStack));
        return true;
    }

    /**
     * Flat loop over live machines of the main pass. Called at AFTER_ENTITIES —
     * BEFORE the BE iteration; {@code levelPose} is the event poseStack (= the
     * dispatcher basis).
     */
    public static void collectMain(PoseStack levelPose, MultiBufferSource buffers,
                                   float partialTick) {
        if (!isEnabled() || mainCollected) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        LIVE.removeIf(be -> be.isRemoved() || be.getLevel() != mc.level || !isManaged(be));

        Matrix4f base = BASE_SCRATCH.set(levelPose.last().pose());
        PoseStack pose = POSE_SCRATCH;
        for (BlockEntity be : LIVE) {
            collectOne(be, partialTick, pose, buffers, base, cam, mc);
        }
        mainCollected = true;
    }

    /**
     * Basis for the newcomer's single render: in a dispatcher call the poseStack
     * already carries T(be-cam) — so the basis is the pose without translation.
     */
    private static Matrix4f bypassBase(PoseStack dispatcherPose) {
        Matrix4f base = new Matrix4f(dispatcherPose.last().pose());
        base.m30(0f).m31(0f).m32(0f);
        return base;
    }

    private static boolean isManaged(BlockEntity be) {
        return be != null && be.getLevel() != null
                && be.getLevel() == Minecraft.getInstance().level
                && MANAGED_TYPES.contains(be.getType());
    }

    /** One machine: distance check -> renderer -> pose = base * T(be-cam) -> MachineBer.collectRender. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void collectOne(BlockEntity be, float partialTick, PoseStack pose,
                                   MultiBufferSource buffers, Matrix4f base) {
        collectOne(be, partialTick, pose, buffers, base,
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(),
                Minecraft.getInstance());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void collectOne(BlockEntity be, float partialTick, PoseStack pose,
                                   MultiBufferSource buffers, Matrix4f base,
                                   Vec3 cam, Minecraft mc) {
        BlockPos pos = be.getBlockPos();
        // Safety: sublevel/contraption BEs must not be collected by the bypass
        // (see shouldBypass) - their transform comes from the external dispatcher.
        if (com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(be)) {
            return;
        }
        double dx = pos.getX() - cam.x;
        double dy = pos.getY() - cam.y;
        double dz = pos.getZ() - cam.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        // Safety distance cap: slightly above the maximum CONFIG distance
        // (static/animated draw sliders) - the real soft fade lives inside
        // renderParts anyway; this is just a cheap early exit.
        double maxDist = Math.max(RenderDistanceHelper.getStaticDistanceBlocks(),
                RenderDistanceHelper.getAnimatedDistanceBlocks()) + 8.0;
        if (distSq > maxDist * maxDist) {
            return;
        }

        BlockEntityRenderer<?> ber = mc.getBlockEntityRenderDispatcher().getRenderer(be);
        if (!(ber instanceof MachineBer<?> machineBer)) {
            return;
        }
        // Fast-path dirty skip: a clean machine (no dirty flag, no worldGen
        // change, no expired light/fade TTL) confirms its presence with a
        // roster assert instead of a full rebuild (matrices/light/comparison
        // of 46 floats per part).
        if (((MachineBer<?>) machineBer).tryFastAssertRender(be, mc.level.getGameTime())) {
            return;
        }
        pose.last().pose().set(base);
        // Normal matrix part of the basis - otherwise hook icon normals ignore camera rotation.
        NORMAL_SCRATCH.set(base);
        pose.last().normal().set(NORMAL_SCRATCH);
        pose.translate((float) dx, (float) dy, (float) dz);
        // Light goes through the tick cache (LightSampleCache): vanilla getLightColor
        // per machine per frame cost ~1.5-4% of the frame on farms.
        int packedLight = com.hbm_m.client.render.LightSampleCache.getOrSamplePacked(be, 0);
        try {
            ((MachineBer<BlockEntity>) machineBer).collectRender(
                    be, partialTick, pose, buffers, packedLight, true);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error(
                    "[HBM-M] NucleusDispatcherBypass: collect failed for {}", pos, t);
        }
    }
}
