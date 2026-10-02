package com.hbm_m.client.stress;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.block.entity.doors.DoorDecl;
import com.hbm_m.block.entity.doors.DoorDeclRegistry;
import com.hbm_m.client.model.variant.DoorModelRegistry;
import com.hbm_m.client.model.variant.DoorSkin;
import com.hbm_m.client.render.machine.MachineRenderRegistry;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

/**
 * State and logic behind {@code /nucleus stress} (port of CrankShaft's /flywheel
 * stress, adapted to multiblock machines processed by the Nucleus engine).
 *
 * <p>Spawn places REAL multiblock machines server-side (a controller {@code setBlock}
 * auto-builds its parts through {@code placeMultiblockStructure}) in a cube around
 * the player ({@code side = ceil(cbrt(count))}, CrankShaft packing), sized by the
 * machine's structure footprint so machines never intersect. Every touched cell is
 * snapshotted before placement; {@code clear} replays the snapshots in reverse and
 * restores the scene 1:1. Singleplayer only (integrated server), like CrankShaft's
 * chest rig.</p>
 *
 * <p><b>Persistence:</b> the snapshot list is mirrored into
 * {@code &lt;world&gt;/hbm_stress_nucleus.dat} after every spawn batch. If the game
 * exits (crash, alt-F4, plain logout) without {@code clear}, the next world join
 * replays the file - restores the original blocks and deletes it. Nothing stress
 * spawned ever survives into a session as an untracked machine.</p>
 *
 * <p>Animations run each machine's NATIVE working animation: {@link NucleusStressAnims}
 * (the single per-machine knowledge point) drives the machines' own working state
 * every client tick; doors use their real server-side state machine - ON mode opens
 * and closes ALL doors in an endless synchronous cycle, RANDOM mode periodically
 * toggles a random subset of doors. Block entities know nothing about the stress
 * command.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class NucleusStressManager {

    public enum AnimMode { OFF, ON, RANDOM }

    private static final String STORE_FILE = "hbm_stress_nucleus.dat";

    /** One placed stress machine: controller position, footprint radius and the pre-placement snapshot of the whole footprint cube. */
    private static final class StressMachine {
        final BlockPos controller;
        final int radius;
        final long[] snapshotPositions;    // packed BlockPos of every cube cell
        final BlockState[] snapshotStates; // state before placement (air included)
        final int controllerIndex;         // snapshot index of the controller cell (restored FIRST)

        StressMachine(BlockPos controller, int radius, long[] snapshotPositions, BlockState[] snapshotStates,
                      int controllerIndex) {
            this.controller = controller;
            this.radius = radius;
            this.snapshotPositions = snapshotPositions;
            this.snapshotStates = snapshotStates;
            this.controllerIndex = controllerIndex;
        }
    }

    /** Guards all mutable state below (written on the server thread during spawn/clear, read on the client thread). */
    private static final Object LOCK = new Object();
    private static final List<StressMachine> MACHINES = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private static volatile ResourceKey<Level> dimension;

    private static AnimMode animMode = AnimMode.OFF;
    /** RANDOM mode: per-machine window end and next window start. */
    private static final Map<BlockPos, Long> ANIM_UNTIL = new HashMap<>();
    private static final Map<BlockPos, Long> NEXT_START = new HashMap<>();
    /** Machines whose animation driver is currently engaged (for once-only shutdown). */
    private static final Set<BlockPos> ANIM_ACTIVE = new HashSet<>();

    /** ON mode: whether the synchronous door cycle is currently in its open phase. */
    private static boolean doorPhaseOpen = false;
    /** RANDOM mode: gameTime of the next random door toggle round (0 = schedule on first tick). */
    private static long doorsNextToggle = 0L;
    /** One-shot crash-recovery check on the first client tick of a session. */
    private static boolean pendingRestoreCheck = true;

    // Door timings, in ticks. ON mode: ALL doors open/close together in an endless
    // cycle (the open phase must comfortably exceed the slowest door's open time).
    // RANDOM mode: every gap a random SUBSET of doors toggles.
    private static final int DOOR_CYCLE_OPEN = 400;     // 20 s open
    private static final int DOOR_CYCLE_CLOSED = 100;   // 5 s closed
    private static final int DOOR_TOGGLE_GAP_MIN = 100; // 5 s between toggle rounds
    private static final int DOOR_TOGGLE_GAP_SPAN = 100; // .. 10 s

    // RANDOM mode (machines): a full native machine cycle (assembler slider
    // ping-pong, press cycle, ring swing) needs well over five seconds to read as
    // "the machine is working" - one-off windows look like glitches.
    private static final int MACHINE_WINDOW_MIN = 300;   // 15 s
    private static final int MACHINE_WINDOW_SPAN = 300;  // .. 30 s
    private static final int WINDOW_GAP_MIN = 40;        // 2 s off between windows
    private static final int WINDOW_GAP_SPAN = 160;      // .. 10 s

    private NucleusStressManager() {}

    // ==================== Spawn ====================

    /**
     * Places {@code count} machines of the spec {@code modelId} in a cube centered
     * on the player. Runs on the integrated server thread; returns the feedback
     * message (the placement itself is async). {@code skinArg} selects a door skin
     * ("skin=clean" or a bare skin id); it is ignored for non-door machines.
     */
    public static Component spawn(Minecraft mc, int count, String modelId, @Nullable String skinArg,
                                  @Nullable Double spacingOverride) {
        Block block = resolveStressBlock(modelId);
        if (block == null) {
            return Component.literal("Unknown Nucleus model '" + modelId + "'").withStyle(ChatFormatting.RED);
        }
        String skinId = parseSkin(skinArg);
        String declId = doorDeclIdOf(modelId);
        if (skinId != null && (declId == null || !isValidDoorSkin(declId, skinId))) {
            return Component.literal("Door '" + (declId == null ? modelId : declId) + "' has no skin '" + skinId + "'")
                    .withStyle(ChatFormatting.RED);
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            return Component.literal("Stress spawn requires a singleplayer world").withStyle(ChatFormatting.RED);
        }
        ResourceKey<Level> dim = mc.player.level().dimension();
        ServerLevel level = server.getLevel(dim);
        if (level == null) {
            return Component.literal("Dimension not found: " + dim.location()).withStyle(ChatFormatting.RED);
        }

        int radius = footprintRadius(block);
        double spacing = spacingOverride != null ? Math.max(spacingOverride, 1.0D) : 2 * radius + 1.0D;
        int side = (int) Math.ceil(Math.cbrt(count));
        int layerSize = side * side;
        double half = (side - 1) * spacing * 0.5D;
        double baseX = mc.player.getX();
        double baseZ = mc.player.getZ();
        int baseY = mc.player.blockPosition().getY();

        List<BlockPos> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            positions.add(BlockPos.containing(
                    baseX - half + (i % side) * spacing,
                    baseY + (i / layerSize) * spacing,
                    baseZ - half + (i / side % side) * spacing));
        }

        dimension = dim;
        List<StressMachine> placed = new ArrayList<>(count);
        server.execute(() -> {
            ServerLevel lvl = server.getLevel(dim);
            if (lvl == null) return;
            for (BlockPos pos : positions) {
                placed.add(snapshotAndPlace(lvl, block, pos, radius, skinId));
            }
            List<StressMachine> toStore;
            synchronized (LOCK) {
                MACHINES.addAll(placed);
                toStore = new ArrayList<>(MACHINES);
            }
            // Mirror the scene into the save folder: a crash/logout without clear
            // is rolled back on the next world join.
            saveStore(lvl, toStore, dim);
        });

        String grid = side + "x" + side + "x" + side + ", spacing " + trim(spacing);
        return Component.literal("Spawning " + count + " '" + modelId + "' stress machines (cube " + grid + ")");
    }

    /**
     * Resolves the model argument to a placeable controller block. The argument
     * is a REAL block registry id ("hbm_m:advanced_assembly_machine",
     * "hbm_m:fire_door", ...). Door declarations take priority (a door id maps to
     * its block through {@link DoorDecl}); everything else must be a block the
     * Nucleus engine actually renders ({@code isManagedBlock}).
     */
    @Nullable
    private static Block resolveStressBlock(String modelId) {
        String path = stripHbmNamespace(modelId);
        if (path == null) return null;
        if (DoorDeclRegistry.contains(path)) {
            DoorDecl decl = DoorDeclRegistry.getById(path);
            Block block = BuiltInRegistries.BLOCK.getOptional(decl.getBlockId()).orElse(null);
            if (block != null && !block.defaultBlockState().isAir()) {
                return block;
            }
        }
        Block block = BuiltInRegistries.BLOCK.getOptional(
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path)).orElse(null);
        if (block != null && !block.defaultBlockState().isAir() && MachineRenderRegistry.isManagedBlock(block)) {
            return block;
        }
        return null;
    }

    /** "hbm_m:x" -> "x"; bare "x" -> "x"; foreign namespace -> null. */
    @Nullable
    private static String stripHbmNamespace(String modelId) {
        int colon = modelId.indexOf(':');
        if (colon < 0) return modelId;
        return modelId.substring(0, colon).equals(RefStrings.MODID) ? modelId.substring(colon + 1) : null;
    }

    /** The door declaration id behind the model argument, or null (not a door). */
    @Nullable
    private static String doorDeclIdOf(@Nullable String modelId) {
        if (modelId == null) return null;
        String path = stripHbmNamespace(modelId);
        return path != null && DoorDeclRegistry.contains(path) ? path : null;
    }

    /**
     * Full model list for the Brigadier suggestions: real block registry ids of
     * every Nucleus-rendered machine plus every door declaration (the doors'
     * real block ids). "hbm_m:door" spec ids are not part of this - they were
     * never real block ids.
     */
    public static List<String> machineModelIds() {
        List<String> out = new ArrayList<>(MachineRenderRegistry.managedBlockIds());
        for (String id : doorModelIds()) {
            if (!out.contains(id)) {
                out.add(id);
            }
        }
        java.util.Collections.sort(out);
        return out;
    }

    /**
     * Door model ids for the Brigadier suggestions - one entry per registered
     * {@link DoorDecl} ("hbm_m:fire_door", ...), which is the door's REAL block id.
     */
    public static List<String> doorModelIds() {
        List<String> out = new ArrayList<>();
        for (String id : DoorDeclRegistry.getAll().keySet()) {
            out.add(RefStrings.MODID + ":" + id);
        }
        java.util.Collections.sort(out);
        return out;
    }

    /**
     * Skin suggestions for the CONCRETELY selected door model, formatted as
     * "skin=&lt;id&gt;". Empty for non-door models.
     */
    public static List<String> doorSkinSuggestions(@Nullable String modelArg) {
        List<String> out = new ArrayList<>();
        String declId = doorDeclIdOf(modelArg);
        if (declId == null) return out;
        for (DoorSkin skin : DoorModelRegistry.getInstance().getSkins(declId)) {
            out.add("skin=" + skin.getId());
        }
        return out;
    }

    private static boolean isValidDoorSkin(String declId, String skinId) {
        for (DoorSkin skin : DoorModelRegistry.getInstance().getSkins(declId)) {
            if (skin.getId().equals(skinId)) return true;
        }
        return "default".equals(skinId);
    }

    /** Strips the "skin=" prefix (any case) from the skin argument. */
    @Nullable
    private static String parseSkin(@Nullable String skinArg) {
        if (skinArg == null) return null;
        String s = skinArg.trim();
        if (s.regionMatches(true, 0, "skin=", 0, 5)) {
            s = s.substring(5).trim();
        }
        return s.isEmpty() ? null : s;
    }

    /** Snapshot of the footprint cube + controller setBlock (onPlace auto-builds the multiblock). */
    private static StressMachine snapshotAndPlace(ServerLevel level, Block block, BlockPos pos, int radius,
                                                  @Nullable String skinId) {
        BlockPos from = pos.offset(-radius, -radius, -radius);
        BlockPos to = pos.offset(radius, radius, radius);
        int cells = (2 * radius + 1) * (2 * radius + 1) * (2 * radius + 1);
        long[] positions = new long[cells];
        BlockState[] states = new BlockState[cells];
        int controllerIndex = -1;
        int k = 0;
        // betweenClosed hands out a MUTABLE position - only asLong()/getBlockState are safe here.
        for (BlockPos cell : BlockPos.betweenClosed(from, to)) {
            positions[k] = cell.asLong();
            states[k] = level.getBlockState(cell);
            if (cell.equals(pos)) {
                controllerIndex = k;
            }
            k++;
        }

        BlockState state = block.defaultBlockState();
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            state = state.setValue(HorizontalDirectionalBlock.FACING, HORIZONTALS[RANDOM.nextInt(HORIZONTALS.length)]);
        }
        // UPDATE_NEIGHBORS | UPDATE_CLIENTS: onPlace runs placeMultiblockStructure.
        level.setBlock(pos, state, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
        if (skinId != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DoorBlockEntity door) {
                door.setSkin(DoorSkin.of(skinId));
            }
        }
        return new StressMachine(pos.immutable(), radius, positions, states, controllerIndex);
    }

    /**
     * Max structure extent around the controller (structureMap keys are grid
     * positions, parts are {@code key - controllerOffset}; rotation permutes X/Z,
     * so one radius covers every facing). 0 for single-block machines.
     */
    private static int footprintRadius(Block block) {
        if (block instanceof IMultiblockController controller) {
            MultiblockStructureHelper helper = controller.getStructureHelper();
            if (helper != null) {
                BlockPos controllerOffset = helper.getControllerOffset();
                int r = 0;
                for (BlockPos key : helper.getStructureMap().keySet()) {
                    BlockPos off = key.subtract(controllerOffset);
                    r = Math.max(r, Math.max(Math.abs(off.getX()), Math.max(Math.abs(off.getY()), Math.abs(off.getZ()))));
                }
                return r;
            }
        }
        return 0;
    }

    // ==================== Clear ====================

    public static Component clear(Minecraft mc) {
        boolean hadAnims;
        List<StressMachine> machines;
        Set<BlockPos> active;
        ResourceKey<Level> dim;
        synchronized (LOCK) {
            hadAnims = animMode != AnimMode.OFF;
            machines = new ArrayList<>(MACHINES);
            MACHINES.clear();
            ANIM_UNTIL.clear();
            NEXT_START.clear();
            active = new HashSet<>(ANIM_ACTIVE);
            ANIM_ACTIVE.clear();
            animMode = AnimMode.OFF;
            dim = dimension;
            dimension = null;
        }
        if (hadAnims) {
            stopAnimations(mc, machines, active);
        }
        if (machines.isEmpty()) {
            return Component.literal("No stress machines to clear");
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || dim == null) {
            return Component.literal("Cleared stress tracking (world restore requires a singleplayer world)")
                    .withStyle(ChatFormatting.YELLOW);
        }
        ServerLevel level = server.getLevel(dim);
        if (level == null) {
            return Component.literal("Stress dimension no longer loaded - " + machines.size()
                    + " machines left in place").withStyle(ChatFormatting.YELLOW);
        }
        server.execute(() -> {
            restoreMachines(level, machines);
            deleteStore(server);
        });
        return Component.literal("Cleared " + machines.size() + " stress machines, scene restored");
    }

    /**
     * Replays the snapshots (server thread). Reverse order: snapshots of later
     * machines contain earlier machines' blocks (overlapping spacing overrides) -
     * LIFO restore is exact. Controller cell FIRST: its onRemove tears the
     * structure down quietly (destroyStructure clears the phantom parts with plain
     * setBlock - no drops/particles/sounds). Restoring a part cell while its
     * controller still stands would fire UniversalMachinePartBlock's "machine
     * broke" cascade (destroyBlock(controllerPos, true) = item drop + break
     * effects), which is exactly the noise a stress restore must not make.
     */
    private static void restoreMachines(ServerLevel level, List<StressMachine> machines) {
        for (int i = machines.size() - 1; i >= 0; i--) {
            StressMachine m = machines.get(i);
            if (m.controllerIndex >= 0) {
                restoreCell(level, m, m.controllerIndex);
            }
            for (int k = 0; k < m.snapshotPositions.length; k++) {
                if (k == m.controllerIndex) continue;
                restoreCell(level, m, k);
            }
        }
    }

    private static void restoreCell(ServerLevel level, StressMachine m, int k) {
        BlockPos cell = BlockPos.of(m.snapshotPositions[k]);
        // UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE: no neighbor cascades
        // (CrankShaft chest-restore flags).
        level.setBlock(cell, m.snapshotStates[k], Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    // ==================== Animations ====================

    public static Component animsOn(Minecraft mc) {
        synchronized (LOCK) {
            if (MACHINES.isEmpty()) {
                return Component.literal("No stress machines - spawn first with /nucleus stress spawn").withStyle(ChatFormatting.RED);
            }
            animMode = AnimMode.ON;
            ANIM_UNTIL.clear();
            NEXT_START.clear();
            ANIM_ACTIVE.clear();
            doorPhaseOpen = false;
            doorsNextToggle = 0L;
        }
        return Component.literal("Stress animations ON (native working animations, all machines; "
                + "doors cycle open/close together)");
    }

    public static Component animsRandom(Minecraft mc) {
        List<StressMachine> machines;
        Set<BlockPos> active;
        synchronized (LOCK) {
            if (MACHINES.isEmpty()) {
                return Component.literal("No stress machines - spawn first with /nucleus stress spawn").withStyle(ChatFormatting.RED);
            }
            machines = new ArrayList<>(MACHINES);
            animMode = AnimMode.RANDOM;
            ANIM_UNTIL.clear();
            NEXT_START.clear();
            active = new HashSet<>(ANIM_ACTIVE);
            ANIM_ACTIVE.clear();
            doorPhaseOpen = false;
            doorsNextToggle = 0L;
        }
        // Leaving ON: wind down everything that was running continuously.
        stopAnimations(mc, machines, active);
        return Component.literal("Stress animations RANDOM: random machines animate 15-30s, "
                + "random doors toggle - until /nucleus stress anims off");
    }

    public static Component animsOff(Minecraft mc) {
        boolean hadAnims;
        List<StressMachine> machines;
        Set<BlockPos> active;
        synchronized (LOCK) {
            hadAnims = animMode != AnimMode.OFF;
            animMode = AnimMode.OFF;
            machines = new ArrayList<>(MACHINES);
            ANIM_UNTIL.clear();
            NEXT_START.clear();
            active = new HashSet<>(ANIM_ACTIVE);
            ANIM_ACTIVE.clear();
            doorPhaseOpen = false;
            doorsNextToggle = 0L;
        }
        if (!hadAnims) {
            return Component.literal("Stress animations are already off");
        }
        stopAnimations(mc, machines, active);
        return Component.literal("Stress animations OFF");
    }

    /**
     * Client tick hook (ClientModEvents CLIENT_POST). Performs the one-shot
     * crash-recovery check, runs the RANDOM scheduler and drives the animation
     * state of every animating machine each tick (the per-machine drivers in
     * {@link NucleusStressAnims} are tick-based).
     */
    public static void clientTick(Minecraft mc) {
        if (mc.level == null || mc.getSingleplayerServer() == null) return;
        if (pendingRestoreCheck) {
            pendingRestoreCheck = false;
            restorePending(mc);
        }
        AnimMode mode;
        List<StressMachine> machines;
        synchronized (LOCK) {
            mode = animMode;
            if (mode == AnimMode.OFF || MACHINES.isEmpty()) return;
            machines = MACHINES;
        }
        long now = mc.level.getGameTime();
        synchronized (LOCK) {
            // Doors in ON mode move as ONE synchronous wave; in RANDOM mode a
            // random subset toggles every few seconds.
            boolean doorWaveOpen = false;
            boolean doorToggleRound = false;
            if (mode == AnimMode.ON) {
                doorWaveOpen = Math.floorMod(now, DOOR_CYCLE_OPEN + DOOR_CYCLE_CLOSED) < DOOR_CYCLE_OPEN;
            } else if (now >= doorsNextToggle) {
                doorsNextToggle = now + DOOR_TOGGLE_GAP_MIN + RANDOM.nextInt(DOOR_TOGGLE_GAP_SPAN);
                doorToggleRound = true;
            }

            for (int i = 0; i < machines.size(); i++) {
                StressMachine m = machines.get(i);
                BlockPos pos = m.controller;
                BlockEntity be = mc.level.getBlockEntity(pos);
                if (be == null) continue;

                if (be instanceof DoorBlockEntity) {
                    if (mode == AnimMode.ON) {
                        if (doorWaveOpen != doorPhaseOpen) {
                            setDoor(mc, pos, doorWaveOpen);
                        }
                    } else if (doorToggleRound && RANDOM.nextBoolean()) {
                        toggleDoor(mc, pos);
                    }
                    continue;
                }

                boolean should;
                if (mode == AnimMode.ON) {
                    should = true;
                } else {
                    should = driveRandomWindow(m, now);
                }
                if (should) {
                    NucleusStressAnims.apply(be, true, now);
                    ANIM_ACTIVE.add(pos);
                } else if (ANIM_ACTIVE.remove(pos)) {
                    NucleusStressAnims.apply(be, false, now);
                }
            }
            doorPhaseOpen = doorWaveOpen;
        }
    }

    /**
     * RANDOM window bookkeeping for one machine; true while the machine should
     * animate. Window start records the window END (until) - a window without an
     * end stamp would collapse into a single tick.
     */
    private static boolean driveRandomWindow(StressMachine m, long now) {
        BlockPos pos = m.controller;
        Long until = ANIM_UNTIL.get(pos);
        if (until != null) {
            if (now >= until) {
                ANIM_UNTIL.remove(pos);
                NEXT_START.put(pos, now + WINDOW_GAP_MIN + RANDOM.nextInt(WINDOW_GAP_SPAN));
                return false;
            }
            return true;
        }
        Long start = NEXT_START.get(pos);
        if (start == null) {
            NEXT_START.put(pos, now + RANDOM.nextInt(WINDOW_GAP_MIN + WINDOW_GAP_SPAN));
            return false;
        }
        if (now >= start) {
            NEXT_START.remove(pos);
            ANIM_UNTIL.put(pos, now + MACHINE_WINDOW_MIN + RANDOM.nextInt(MACHINE_WINDOW_SPAN));
            return true;
        }
        return false;
    }

    /** Wind down every animating machine once (drivers with on=false, doors closed). */
    private static void stopAnimations(Minecraft mc, List<StressMachine> machines, Set<BlockPos> active) {
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        for (int i = 0; i < machines.size(); i++) {
            StressMachine m = machines.get(i);
            BlockEntity be = mc.level.getBlockEntity(m.controller);
            if (be == null) continue;
            if (be instanceof DoorBlockEntity) {
                setDoor(mc, m.controller, false);
            } else if (active.contains(m.controller)) {
                NucleusStressAnims.apply(be, false, now);
            }
        }
    }

    /**
     * Doors animate through their real state machine (open()/close() on the
     * integrated server - native sounds, timings and part animation).
     */
    private static void setDoor(Minecraft mc, BlockPos pos, boolean open) {
        MinecraftServer server = mc.getSingleplayerServer();
        ResourceKey<Level> dim = mc.level != null ? mc.level.dimension() : null;
        if (server == null || dim == null) return;
        server.execute(() -> {
            ServerLevel level = server.getLevel(dim);
            if (level == null) return;
            if (level.getBlockEntity(pos) instanceof DoorBlockEntity door) {
                if (open) door.open();
                else if (door.isOpen()) door.close();
            }
        });
    }

    /** Flips a door's state server-side (skipped while the door is still moving). */
    private static void toggleDoor(Minecraft mc, BlockPos pos) {
        MinecraftServer server = mc.getSingleplayerServer();
        ResourceKey<Level> dim = mc.level != null ? mc.level.dimension() : null;
        if (server == null || dim == null) return;
        server.execute(() -> {
            ServerLevel level = server.getLevel(dim);
            if (level == null) return;
            if (level.getBlockEntity(pos) instanceof DoorBlockEntity door && !door.isMoving()) {
                if (door.isOpen()) {
                    door.close();
                } else {
                    door.open();
                }
            }
        });
    }

    // ==================== Crash recovery (scene persistence) ====================

    /**
     * One-shot check on the first client tick of a session: if a previous session
     * left a stress scene behind (crash, logout without clear), restore the world
     * and drop the store file. The store lives in the SAVE folder, so worlds are
     * independent.
     */
    public static void restorePending(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            synchronized (LOCK) {
                if (!MACHINES.isEmpty()) {
                    return; // session already has a live scene - nothing to recover
                }
            }
            StoredScene stored = loadStore(server);
            if (stored == null || stored.machines().isEmpty()) return;
            ServerLevel level = server.getLevel(stored.dimension());
            if (level != null) {
                restoreMachines(level, stored.machines());
            }
            deleteStore(server);
            String dimNote = level != null ? "" : " (dimension not loaded - machines kept)";
            MainRegistry.LOGGER.info("[NucleusStress] crash recovery: restored {} stress machines from a previous session{}",
                    stored.machines().size(), dimNote);
            mc.execute(() -> {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal(
                            "[NucleusStress] Rolled back " + stored.machines().size()
                                    + " stress machines left by a previous session"), false);
                }
            });
        });
    }

    private record StoredScene(ResourceKey<Level> dimension, List<StressMachine> machines) {}

    private static Path storePath(MinecraftServer server) {
        return server.getWorldPath(new LevelResource(STORE_FILE));
    }

    /** Mirrors the full scene snapshot into the save folder (server thread, tmp+move). */
    private static void saveStore(ServerLevel level, List<StressMachine> machines, ResourceKey<Level> dim) {
        try {
            CompoundTag root = new CompoundTag();
            ResourceLocation dimLoc = dim.location();
            root.putString("dimNs", dimLoc.getNamespace());
            root.putString("dimPath", dimLoc.getPath());

            // State palette: machines share their snapshot states (air, ground, ...).
            List<BlockState> palette = new ArrayList<>();
            Map<BlockState, Integer> paletteIndex = new IdentityHashMap<>();
            ListTag machineList = new ListTag();
            for (int i = 0; i < machines.size(); i++) {
                StressMachine m = machines.get(i);
                CompoundTag t = new CompoundTag();
                t.putLong("ctrl", m.controller.asLong());
                t.putInt("radius", m.radius);
                t.putInt("ctrlIdx", m.controllerIndex);
                t.putLongArray("cells", m.snapshotPositions);
                int[] idx = new int[m.snapshotStates.length];
                for (int k = 0; k < idx.length; k++) {
                    Integer existing = paletteIndex.get(m.snapshotStates[k]);
                    if (existing == null) {
                        existing = palette.size();
                        paletteIndex.put(m.snapshotStates[k], existing);
                        palette.add(m.snapshotStates[k]);
                    }
                    idx[k] = existing;
                }
                t.putIntArray("pal", idx);
                machineList.add(t);
            }
            root.put("machines", machineList);
            ListTag stateList = new ListTag();
            for (BlockState state : palette) {
                stateList.add(NbtUtils.writeBlockState(state));
            }
            root.put("states", stateList);

            Path path = storePath(level.getServer());
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                NbtIo.writeCompressed(root, out);
            }
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            MainRegistry.LOGGER.error("[NucleusStress] failed to store the stress scene", e);
        }
    }

    /** Reads the store file; null = absent or unreadable (treated as nothing to recover). */
    @Nullable
    private static StoredScene loadStore(MinecraftServer server) {
        Path path = storePath(server);
        if (!Files.exists(path)) return null;
        try (InputStream in = Files.newInputStream(path)) {
            CompoundTag root;
            //? if < 1.21.1 {
            root = NbtIo.readCompressed(in);
            //?} else {
            /*root = NbtIo.readCompressed(in, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            *///?}
            ResourceLocation dimLoc = ResourceLocation.fromNamespaceAndPath(
                    root.getString("dimNs"), root.getString("dimPath"));
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimLoc);

            // Any loaded level provides the block registries needed to parse states.
            ServerLevel parseLevel = server.getLevel(dim);
            if (parseLevel == null) parseLevel = server.overworld();
            if (parseLevel == null) return null;

            ListTag stateList = root.getList("states", CompoundTag.TAG_COMPOUND);
            List<BlockState> palette = new ArrayList<>(stateList.size());
            for (int i = 0; i < stateList.size(); i++) {
                palette.add(readStoredState(parseLevel, stateList.getCompound(i)));
            }

            ListTag machineList = root.getList("machines", CompoundTag.TAG_COMPOUND);
            List<StressMachine> machines = new ArrayList<>(machineList.size());
            for (int i = 0; i < machineList.size(); i++) {
                CompoundTag t = machineList.getCompound(i);
                long[] cells = t.getLongArray("cells");
                int[] pal = t.getIntArray("pal");
                BlockState[] states = new BlockState[cells.length];
                for (int k = 0; k < cells.length && k < pal.length; k++) {
                    states[k] = pal[k] >= 0 && pal[k] < palette.size() ? palette.get(pal[k]) : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                }
                machines.add(new StressMachine(
                        BlockPos.of(t.getLong("ctrl")),
                        t.getInt("radius"),
                        cells,
                        states,
                        t.getInt("ctrlIdx")));
            }
            return new StoredScene(dim, machines);
        } catch (Exception e) {
            MainRegistry.LOGGER.error("[NucleusStress] stress scene store is unreadable - skipping recovery", e);
            return null;
        }
    }

    /** BlockState NBT round-trip (same signature on both targets, see RedCablePaintableBlockEntity). */
    private static BlockState readStoredState(ServerLevel level, CompoundTag tag) {
        return NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), tag);
    }

    private static void deleteStore(MinecraftServer server) {
        try {
            Files.deleteIfExists(storePath(server));
        } catch (Exception e) {
            MainRegistry.LOGGER.error("[NucleusStress] failed to delete the stress scene store", e);
        }
    }

    // ==================== Lifecycle ====================

    /** Client disconnect: drop the in-memory scene (the store file keeps the recovery path alive). */
    public static void reset() {
        synchronized (LOCK) {
            MACHINES.clear();
            ANIM_UNTIL.clear();
            NEXT_START.clear();
            ANIM_ACTIVE.clear();
            animMode = AnimMode.OFF;
            doorPhaseOpen = false;
            doorsNextToggle = 0L;
            pendingRestoreCheck = true;
            dimension = null;
        }
    }

    private static String trim(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
