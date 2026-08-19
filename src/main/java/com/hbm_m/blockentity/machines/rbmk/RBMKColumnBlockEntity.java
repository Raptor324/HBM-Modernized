package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.handler.rbmk.NeutronNodeWorld;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.item.ModItems;
import com.hbm_m.platform.PlatformHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class RBMKColumnBlockEntity extends BaseHbmBlockEntity {

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
    public static void meltdownReactor(Level level, RBMKColumnBlockEntity origin) {
        if (level.isClientSide) return;

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
        digamma = false;
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
     * Flings a piece of debris outward from the top of the column, matching the original's
     * {@code EntityRBMKDebris} arc (gaussian horizontal spread, strong upward kick). Reuses vanilla
     * {@link ItemEntity} physics rather than a bespoke entity class - visually equivalent (tumbling,
     * bouncing, gravity) since the debris items themselves are wrapped as ItemStacks.
     */
    protected void spawnDebris(Level level, String type) {
        if (level.isClientSide) return;
        Item item = debrisItem(type);
        if (item == null) return;

        BlockPos base = getBlockPos();
        ItemEntity debris = new ItemEntity(level,
                base.getX() + 0.5, base.getY() + 4.0, base.getZ() + 0.5, new ItemStack(item));
        double vx = level.random.nextGaussian() * 0.25;
        double vz = level.random.nextGaussian() * 0.25;
        double vy = 0.25 + level.random.nextDouble() * 1.25;
        if (type.equals("lid")) { vx *= 0.5; vz *= 0.5; vy += 0.5; }
        debris.setDeltaMovement(vx, vy, vz);
        debris.setPickUpDelay(100);
        level.addFreshEntity(debris);
    }

    /**
     * 1:1 with the original's {@code EntityRBMKDebris} pickup mapping: BLANK/ELEMENT/ROD debris
     * all drop the same plain {@code debris_metal} beam item (not a dedicated "element" item -
     * that registration exists but the original never actually uses it for this), and LID debris
     * drops the real, placeable {@code rbmk_lid} item (recoverable even after a meltdown),
     * not a decorative stand-in.
     */
    private static Item debrisItem(String type) {
        return switch (type) {
            case "fuel"                      -> ModItems.DEBRIS_FUEL.get();
            case "graphite"                  -> ModItems.DEBRIS_GRAPHITE.get();
            case "blank", "element", "rod"    -> ModItems.DEBRIS_METAL.get();
            case "lid"                        -> ModItems.RBMK_LID.get();
            default -> null;
        };
    }

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

    public RBMKType getRBMKType()  { return RBMKType.OTHER; }
    public boolean  isModerated()  { return false; }

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

    /**
     * Версионно-независимое сохранение ItemStack в новый CompoundTag.
     * Делегирует в {@link PlatformHooks#safeItemSave} (наследники RBMK используют в своём NBT).
     */
    protected static CompoundTag safeItemSave(ItemStack stack) {
        return PlatformHooks.safeItemSave(stack, null);
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putDouble("heat", heat);
        tag.putInt("reasimWater", reasimWater);
        tag.putInt("reasimSteam", reasimSteam);
        tag.putInt("lidState", lidState);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        heat        = tag.getDouble("heat");
        reasimWater = tag.getInt("reasimWater");
        reasimSteam = tag.getInt("reasimSteam");
        lidState    = tag.contains("lidState") ? tag.getInt("lidState") : 1;
    }

}

