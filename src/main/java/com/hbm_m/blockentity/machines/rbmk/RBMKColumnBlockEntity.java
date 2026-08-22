package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.handler.rbmk.NeutronNodeWorld;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class RBMKColumnBlockEntity extends BlockEntity {

    public double heat        = 20.0;
    public int reasimWater    = 0;
    public int reasimSteam    = 0;
    public static final int MAX_WATER = 16_000;
    public static final int MAX_STEAM = 16_000;
    public int craneIndicator = 0;
    /** 0 = no lid, 1 = concrete lid, 2 = glass lid */
    protected int lidState = 1;

    private static final Direction[] NEIGHBOR_DIRS = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    protected RBMKColumnBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Matches the original's node-invalidation on tile removal/unload (see
     * {@code TileEntity.invalidate()}/{@code onChunkUnload()} in the source mod): whenever a
     * column block entity is removed - block broken, replaced with a different RBMK type, or
     * the block otherwise swapped out - its cached {@link RBMKNeutronHandler.RBMKNeutronNode}
     * must be evicted immediately. Without this, {@code RBMKNeutronNode.checkNode()}'s periodic
     * sweep only ever evicts nodes downstream of a dead fuel rod, leaving stale type/hasLid data
     * behind whenever a moderator/reflector/absorber/control column is swapped for another type.
     */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            NeutronNodeWorld.removeNode(level, getBlockPos());
        }
    }

    // â"€â"€â"€ Base Tick â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    protected static void baseTick(Level level, BlockPos pos, BlockState state, RBMKColumnBlockEntity be) {
        if (level.isClientSide) return;
        if (be.craneIndicator > 0) be.craneIndicator--;
        if (be.participatesInHeatNetwork()) {
            be.moveHeat(level);
            if (RBMKDials.getReasimBoilers(level)) be.boilWater(level);
        }
        level.sendBlockUpdated(pos, state, state, 3);
    }

    /**
     * Whether this column takes part in the reactor's column-to-column heat equalization
     * network (see {@link #moveHeat}). True for every real reactor column (fuel/moderator/
     * cooler/etc); false for control-room devices like the RTTY panels or crane console that
     * happen to share this base class for placement/registration convenience but aren't
     * physically part of the fuel-channel grid, so they must never siphon or donate heat just
     * because a player placed one next to a reactor.
     */
    protected boolean participatesInHeatNetwork() { return true; }

    // â"€â"€â"€ Heat â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    private void moveHeat(Level level) {
        boolean reasim = RBMKDials.getReasimBoilers(level);
        List<RBMKColumnBlockEntity> rec = new ArrayList<>();
        rec.add(this);
        double heatTot = heat;
        int waterTot = reasimWater, steamTot = reasimSteam;

        for (Direction dir : NEIGHBOR_DIRS) {
            BlockPos np = getBlockPos().offset(dir.getStepX(), 0, dir.getStepZ());
            if (level.getBlockEntity(np) instanceof RBMKColumnBlockEntity n && n.participatesInHeatNetwork()) {
                rec.add(n);
                heatTot += n.heat;
                if (reasim) { waterTot += n.reasimWater; steamTot += n.reasimSteam; }
            }
        }

        int members = rec.size();
        if (members > 1) {
            double targetHeat = heatTot / members;
            double step = RBMKDials.getColumnHeatFlow(level);
            int tWater = waterTot / members, rWater = waterTot % members;
            int tSteam  = steamTot  / members, rSteam  = steamTot  % members;
            for (RBMKColumnBlockEntity c : rec) {
                c.heat += (targetHeat - c.heat) * step;
                if (reasim) { c.reasimWater = tWater; c.reasimSteam = tSteam; }
            }
            if (reasim) { reasimWater += rWater; reasimSteam += rSteam; }
            setChanged();
        }
        coolPassively(level, members - 1);
    }

    private void boilWater(Level level) {
        if (heat < 100.0) return;
        double hc = RBMKDials.getBoilerHeatConsumption(level);
        double available = Math.min(Math.min((heat - 100) / hc, reasimWater), MAX_STEAM - reasimSteam);
        int processed = (int) Math.floor(available * RBMKDials.getReaSimBoilerSpeed(level));
        if (processed <= 0) return;
        reasimWater -= processed;
        reasimSteam += processed;
        heat -= processed * hc;
    }

    protected void coolPassively(Level level, int neighbors) {
        double min = RBMKDials.getPassiveCoolingInner(level);
        double max = RBMKDials.getPassiveCooling(level);
        heat -= min + (max - min) * ((4 - Math.min(neighbors, 4)) / 4.0);
        if (heat < 20) heat = 20.0;
    }

    // â"€â"€â"€ Melt â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    public double maxHeat() { return 1500.0; }

    /**
     * 1:1 port of the original's static {@code RBMKBase.digamma} flag: set when a Digamma-fuel
     * (rbmk_fuel_drx) rod melts down, checked/reset once per {@link #meltdownReactor} call to
     * decide whether corium-adjacent debris becomes {@code pribris_digamma} (severe) or the
     * ordinary {@code pribris_radiating}.
     */
    public static boolean digamma = false;

    /**
     * 1:1 port of the original's {@code TileEntityRBMKBase.meltdown()}: a meltdown is never
     * confined to the single overheated column. It flood-fills every RBMK column block entity
     * connected (via the 4 cardinal neighbors) to {@code origin}, forming the "reactor" as one
     * contiguous group, then melts every column in that group together. Severity scales with
     * distance from the group's bounding-box edge - columns near the edge get a small
     * {@code reduce} (less destruction), columns near the center get a larger one (more
     * destruction), matching the original's {@code minDist+1} computation.
     */
    /**
     * {@code TileEntityRBMKBase.pipes}: every steam network a melting boiler channel was feeding.
     * Collected during the onMelt pass and consumed once at the end of the meltdown, so a network
     * shared by several channels is only torn apart once.
     */
    public static final Set<com.hbm_m.api.fluids.FluidNet> overpressureNets = new HashSet<>();

    public static void meltdownReactor(Level level, RBMKColumnBlockEntity origin) {
        if (level.isClientSide) return;

        // The original brackets the whole meltdown with RBMKBase.dropLids = false/true. Without
        // it every column that still had a lid dropped it as an item *and* launched it as debris,
        // so a meltdown quietly duplicated the entire reactor's lids across the crater floor.
        com.hbm_m.block.machines.rbmk.RBMKColumnBlock.dropLids = false;
        overpressureNets.clear();

        BlockPos originPos = origin.getBlockPos();
        Set<BlockPos> visited = new HashSet<>();
        Deque<RBMKColumnBlockEntity> queue = new ArrayDeque<>();
        List<RBMKColumnBlockEntity> columns = new ArrayList<>();

        visited.add(originPos);
        queue.add(origin);

        int minX = originPos.getX(), maxX = originPos.getX();
        int minZ = originPos.getZ(), maxZ = originPos.getZ();

        while (!queue.isEmpty()) {
            RBMKColumnBlockEntity col = queue.poll();
            columns.add(col);
            BlockPos p = col.getBlockPos();
            minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
            minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());

            for (Direction dir : NEIGHBOR_DIRS) {
                BlockPos np = p.relative(dir);
                if (visited.contains(np)) continue;
                if (level.getBlockEntity(np) instanceof RBMKColumnBlockEntity n) {
                    visited.add(np);
                    queue.add(n);
                }
            }
        }

        for (RBMKColumnBlockEntity col : columns) {
            BlockPos p = col.getBlockPos();
            int minDist = Math.min(
                    Math.min(p.getX() - minX, maxX - p.getX()),
                    Math.min(p.getZ() - minZ, maxZ - p.getZ()));
            col.onMelt(level, minDist + 1);
        }

        // Corium infection pass: every column that fully melted down to corium "infects" its
        // 3x3x3 neighborhood with a 1-in-3 chance of turning ordinary/burning debris into the
        // more severe digamma or radiating variant, matching the original's post-meltdown sweep.
        for (RBMKColumnBlockEntity col : columns) {
            BlockPos p = col.getBlockPos();
            if (!level.getBlockState(p).is(com.hbm_m.block.ModBlocks.RBMK_CORIUM.get())) continue;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos np = p.offset(dx, dy, dz);
                        BlockState bs = level.getBlockState(np);
                        if (level.random.nextInt(3) != 0) continue;
                        if (bs.is(com.hbm_m.block.ModBlocks.RBMK_DEBRIS.get())
                                || bs.is(com.hbm_m.block.ModBlocks.RBMK_DEBRIS_BURNING.get())) {
                            level.setBlock(np, digamma
                                    ? com.hbm_m.block.ModBlocks.RBMK_DEBRIS_DIGAMMA.get().defaultBlockState()
                                    : com.hbm_m.block.ModBlocks.RBMK_DEBRIS_RADIATING.get().defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        handleOverpressure(level);

        // The meltdown's own effect: one mushroom cloud scaled to the reactor's smaller footprint,
        // centred on the group rather than on whichever column happened to trigger it, plus the
        // explosion report. Both were missing - the RBMK_MUSH particle was registered but never
        // spawned by anything, so a meltdown was silent and left no cloud at all.
        int smallDim = Math.min(maxX - minX, maxZ - minZ);
        double avgX = minX + (maxX - minX) / 2 + 0.5;
        double avgZ = minZ + (maxZ - minZ) / 2 + 0.5;
        double cloudY = originPos.getY() + 1;

        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            // Count 0 makes the client take xd/yd/zd as literal values instead of a random
            // spread, which is how the provider receives the cloud's scale.
            server.sendParticles(com.hbm_m.particle.ModParticleTypes.RBMK_MUSH.get(),
                    avgX, cloudY, avgZ, 0, smallDim, 0, 0, 1);
        }
        level.playSound(null, avgX, cloudY, avgZ,
                com.hbm_m.sound.ModSounds.RBMK_EXPLOSION.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 50.0F, 1.0F);

        // A meltdown carrying digamma fuel calls down the lance, a hundred blocks above the
        // reactor's centre. Note the original reads the flag here, *after* onMelt has run - the
        // reset below is what stops it firing again on the next, ordinary meltdown.
        if (digamma) {
            com.hbm_m.entity.effect.SpearEntity spear =
                    com.hbm_m.entity.ModEntities.DIGAMMA_SPEAR.get().create(level);
            if (spear != null) {
                spear.setPos(avgX, originPos.getY() + 100, avgZ);
                level.addFreshEntity(spear);
            }
        }

        com.hbm_m.block.machines.rbmk.RBMKColumnBlock.dropLids = true;
        digamma = false;
    }

    /**
     * 1:1 port of the original's overpressure event. A meltdown does not stop at the reactor: the
     * steam still in the pipework has to go somewhere, so the networks the melting channels were
     * feeding rupture too.
     *
     * <p>Pipes go first, but only a fraction of them - {@code min(count / 5, 100)} - so a long
     * run is left mangled rather than erased, and a huge network cannot stall the server. Every
     * receiver on those networks is then destroyed: machines that implement
     * {@link com.hbm_m.api.tile.IOverpressurable} decide for themselves what that looks like,
     * anything else is removed and replaced with a plain five-power explosion.</p>
     */
    private static void handleOverpressure(Level level) {
        if (!RBMKDials.getOverpressure(level) || overpressureNets.isEmpty()) {
            overpressureNets.clear();
            return;
        }

        // Unify first: two channels feeding one network must not process it twice.
        Set<com.hbm_m.api.network.GenNode<?>> pipeNodes = new java.util.LinkedHashSet<>();
        Set<com.hbm_m.api.fluids.IFluidReceiverMK2> receivers = new java.util.LinkedHashSet<>();
        for (com.hbm_m.api.fluids.FluidNet net : overpressureNets) {
            if (net == null) continue;
            pipeNodes.addAll(net.links);
            receivers.addAll(net.receiverEntries.keySet());
        }

        int max = Math.min(pipeNodes.size() / 5, 100);
        int count = 0;
        for (com.hbm_m.api.network.GenNode<?> node : pipeNodes) {
            if (count >= max) break;
            for (BlockPos pos : node.positions) {
                if (level.getBlockEntity(pos) != null) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            count++;
        }

        for (com.hbm_m.api.fluids.IFluidReceiverMK2 receiver : receivers) {
            if (!(receiver instanceof BlockEntity be)) continue;
            BlockPos pos = be.getBlockPos();
            if (receiver instanceof com.hbm_m.api.tile.IOverpressurable overpressurable) {
                overpressurable.explode(level, pos);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        5F, Level.ExplosionInteraction.NONE);
            }
        }

        overpressureNets.clear();
    }

    public void onMelt(Level level, int reduce) {
        standardMelt(level, reduce);
        if (lidState == 1) spawnDebris(level, "lid");
    }

    protected void standardMelt(Level level, int reduce) {
        BlockPos base = getBlockPos();
        int h = RBMKDials.getColumnHeight(level);
        reduce = Math.max(1, Math.min(reduce, h));
        if (level.random.nextInt(3) == 0) reduce++;
        int burningLayer = h + 1 - reduce;
        for (int i = h; i >= 0; i--) {
            if (i <= burningLayer) {
                // 1:1 with the original: the boundary layer becomes the glowing "burning" rubble
                // variant, everything below it plain rubble - both real pribris blocks, not a
                // vanilla gravel/fire stand-in.
                BlockState debris = (reduce > 1 && i == burningLayer)
                        ? com.hbm_m.block.ModBlocks.RBMK_DEBRIS_BURNING.get().defaultBlockState()
                        : com.hbm_m.block.ModBlocks.RBMK_DEBRIS.get().defaultBlockState();
                level.setBlock(base.above(i), debris, 3);
            } else {
                level.setBlock(base.above(i), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        // Cosmetic-only blast (matches the original's newExplosion(...,5F,false,false)): sound and
        // particles to sell the meltdown without double-damaging terrain we already rewrote above.
        level.explode(null, base.getX() + 0.5, base.getY() + 0.5, base.getZ() + 0.5, 5F, Level.ExplosionInteraction.NONE);
    }

    /**
     * Flings a piece of debris outward from the top of the column, 1:1 with the original's
     * {@code TileEntityRBMKBase.spawnDebris}: gaussian horizontal spread, strong upward kick, and
     * a lid that gets a softer sideways push but a harder one upward so it clears the building.
     *
     * <p>This used to drop a plain vanilla {@link ItemEntity} as a stand-in, which looked roughly
     * right but lost everything that makes the debris matter: the lid no longer punched through
     * the ceiling, fuel and graphite chunks stopped irradiating anyone near them, and the
     * per-type lifetimes and models were gone. It now spawns the real
     * {@link com.hbm_m.entity.rbmk.RBMKDebrisEntity}.</p>
     */
    protected void spawnDebris(Level level, String type) {
        if (level.isClientSide) return;

        com.hbm_m.entity.rbmk.RBMKDebrisEntity.DebrisType debrisType = debrisType(type);
        if (debrisType == null) return;

        BlockPos base = getBlockPos();
        com.hbm_m.entity.rbmk.RBMKDebrisEntity debris = com.hbm_m.entity.rbmk.RBMKDebrisEntity.create(
                level, base.getX() + 0.5, base.getY() + 4.0, base.getZ() + 0.5, debrisType);

        double vx = level.random.nextGaussian() * 0.25;
        double vz = level.random.nextGaussian() * 0.25;
        double vy = 0.25 + level.random.nextDouble() * 1.25;
        if (debrisType == com.hbm_m.entity.rbmk.RBMKDebrisEntity.DebrisType.LID) {
            vx *= 0.5;
            vz *= 0.5;
            vy += 0.5;
        }
        debris.setDeltaMovement(vx, vy, vz);
        level.addFreshEntity(debris);
    }

    private static com.hbm_m.entity.rbmk.RBMKDebrisEntity.DebrisType debrisType(String type) {
        return switch (type) {
            case "fuel"     -> com.hbm_m.entity.rbmk.RBMKDebrisEntity.DebrisType.FUEL;
            case "graphite" -> com.hbm_m.entity.rbmk.RBMKDebrisEntity.DebrisType.GRAPHITE;
            case "element"  -> com.hbm_m.entity.rbmk.RBMKDebrisEntity.DebrisType.ELEMENT;
            case "rod"      -> com.hbm_m.entity.rbmk.RBMKDebrisEntity.DebrisType.ROD;
            case "lid"      -> com.hbm_m.entity.rbmk.RBMKDebrisEntity.DebrisType.LID;
            case "blank"    -> com.hbm_m.entity.rbmk.RBMKDebrisEntity.DebrisType.BLANK;
            default -> null;
        };
    }

    // â"€â"€â"€ Lid â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    public boolean hasLid()         { return lidState != 0; }
    public int    getLidState()     { return lidState; }
    public boolean isLidRemovable() { return true; }

    public void setLidState(int state) {
        lidState = state;
        if (level != null) {
            RBMKNeutronHandler.RBMKNeutronNode node =
                NeutronNodeWorld.getOrAddWorld(level).getNode(getBlockPos());
            if (node != null) node.hasLid = (state != 0);
        }
        setChanged();
    }

    // â"€â"€â"€ Neutron â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    public RBMKType getRBMKType()  { return RBMKType.OTHER; }
    public boolean  isModerated()  { return false; }

    // â"€â"€â"€ Console â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    public enum ColumnType {
        BLANK, FUEL, CONTROL, MODERATOR, ABSORBER, REFLECTOR, COOLER, BOILER, HEATER, OUTGASSER, STORAGE
    }
    public abstract ColumnType getConsoleType();

    /** Returns NBT data for the RBMK console panel display. Override in subclasses with relevant data. */
    public CompoundTag getNBTForConsole() { return new CompoundTag(); }

    //? if forge {
    /** Expands the render bounding box to cover the full column height so the BESR isn't culled early. */
    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public AABB getRenderBoundingBox() {
        BlockPos p = getBlockPos();
        return new AABB(p.getX(), p.getY(), p.getZ(),
                        p.getX() + 1, p.getY() + com.hbm_m.handler.rbmk.RBMKDials.COLUMN_HEIGHT + 1, p.getZ() + 1);
    }
    //?}

    /**
     * Returns the texture-name prefix used by the BESR to look up
     * {@code block/rbmk/<prefix>_side} and {@code block/rbmk/<prefix>_top}.
     * Matches the texture names in the block-model JSON. Override where the
     * block entity type doesn't map 1:1 to the ColumnType name.
     */
    public String getRenderTexturePrefix() {
        return switch (getConsoleType()) {
            case FUEL      -> "rbmk_element";
            case BLANK     -> "rbmk_blank";
            case ABSORBER  -> "rbmk_absorber";
            case REFLECTOR -> "rbmk_reflector";
            case COOLER    -> "rbmk_cooler";
            case BOILER    -> "rbmk_boiler";
            case HEATER    -> "rbmk_heater";
            case MODERATOR -> "rbmk_moderator";
            case OUTGASSER -> "rbmk_outgasser";
            case STORAGE   -> "rbmk_storage";
            case CONTROL   -> "rbmk_control";
        };
    }

    // ─── NBT ─────────────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    protected static CompoundTag safeItemSave(net.minecraft.world.item.ItemStack stack) {
        CompoundTag t = new CompoundTag();
        stack.save(t);
        return t;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("heat", heat);
        tag.putInt("reasimWater", reasimWater);
        tag.putInt("reasimSteam", reasimSteam);
        tag.putInt("lidState", lidState);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        heat        = tag.getDouble("heat");
        reasimWater = tag.getInt("reasimWater");
        reasimSteam = tag.getInt("reasimSteam");
        lidState    = tag.contains("lidState") ? tag.getInt("lidState") : 1;
    }
    //?} else {
    /*protected static CompoundTag safeItemSave(net.minecraft.world.item.ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        return (CompoundTag) stack.save(registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("heat", heat);
        tag.putInt("reasimWater", reasimWater);
        tag.putInt("reasimSteam", reasimSteam);
        tag.putInt("lidState", lidState);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heat        = tag.getDouble("heat");
        reasimWater = tag.getInt("reasimWater");
        reasimSteam = tag.getInt("reasimSteam");
        lidState    = tag.contains("lidState") ? tag.getInt("lidState") : 1;
    }
    *///?}

    // ─── Sync ─────────────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }
    //?} else {
    /*@Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
    *///?}

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

