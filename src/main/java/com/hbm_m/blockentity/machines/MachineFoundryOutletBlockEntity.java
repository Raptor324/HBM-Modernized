package com.hbm_m.blockentity.machines;

<<<<<<< HEAD:src/main/java/com/hbm_m/block/entity/machines/MachineFoundryOutletBlockEntity.java
import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.machines.MachineFoundryOutletBlock;
=======
import com.hbm_m.blockentity.ModBlockEntities;
>>>>>>> 910527b2d8c811b73b30c9dce85eea2396003997:src/main/java/com/hbm_m/blockentity/machines/MachineFoundryOutletBlockEntity.java
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import com.hbm_m.util.CrucibleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Port of the 1.7.10 TileEntityFoundryOutlet.
 * Holds no material itself: it accepts sideways flow from the channel behind it
 * (opposite of FACING) and immediately re-pours it straight down, up to 4 blocks,
 * into the first ICrucibleAcceptor below (basin, channel, crucible, ...).
 * Flow is only accepted if the target below can actually take the material.
 */
public class MachineFoundryOutletBlockEntity extends BlockEntity implements ICrucibleAcceptor {

    /** Raytrace depth of the original: y-0.125 down to y+0.125-4 → 4 blocks below. */
    private static final int POUR_RANGE = 3;

    /** Only let this material through (null = all materials allowed). */
    @Nullable public MaterialType filter = null;

    /** When true: let everything EXCEPT the filter material through. */
    public boolean invertFilter = false;

    /** When true: outlet is closed by default and OPENS with redstone.
     *  When false (default): outlet is open by default and CLOSES with redstone. */
    public boolean invertRedstone = false;

    public MachineFoundryOutletBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_OUTLET_BE.get(), pos, state);
    }

    public boolean isClosed() {
        if (level == null) return true;
        boolean powered = level.hasNeighborSignal(worldPosition);
        return invertRedstone ^ powered;
    }

    public boolean passesFilter(MaterialType type) {
        if (filter == null) return true;
        boolean matches = filter == type;
        return invertFilter ? !matches : matches;
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(MachineFoundryOutletBlock.FACING)
                ? state.getValue(MachineFoundryOutletBlock.FACING)
                : Direction.NORTH;
    }

    /* ── ICrucibleAcceptor ──────────────────────────────────────────────── */

    /* Outlets can't be poured into, only flowed through. */
    @Override public boolean canAcceptPartialPour(Level level, BlockPos pos, Direction side, MaterialStack stack) { return false; }
    @Override public @Nullable MaterialStack pour(Level level, BlockPos pos, Direction side, MaterialStack stack) { return stack; }

    @Override
    public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        if (!passesFilter(stack.type)) return false;
        if (isClosed()) return false;
        // material must arrive from behind (channel side), i.e. opposite of the pour direction
        if (side != getFacing().getOpposite()) return false;

        BlockPos target = CrucibleUtil.getPouringTarget(level, pos.below(), POUR_RANGE);
        if (target == null) return false;

        BlockEntity be = level.getBlockEntity(target);
        return be instanceof ICrucibleAcceptor acc
                && acc.canAcceptPartialPour(level, target, Direction.UP, stack);
    }

    @Override
    public @Nullable MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        BlockPos target = CrucibleUtil.getPouringTarget(level, pos.below(), POUR_RANGE);
        if (target == null) return stack;

        BlockEntity be = level.getBlockEntity(target);
        if (!(be instanceof ICrucibleAcceptor acc)) return stack;

        return acc.pour(level, target, Direction.UP, stack);
    }

    /* ── NBT / sync ─────────────────────────────────────────────────────── */

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (filter != null) tag.putString("filter", filter.name);
        tag.putBoolean("invertFilter",   invertFilter);
        tag.putBoolean("invertRedstone", invertRedstone);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("filter")) filter = MaterialType.byName(tag.getString("filter"));
        else filter = null;
        invertFilter   = tag.getBoolean("invertFilter");
        invertRedstone = tag.getBoolean("invertRedstone");
    }

    @Override
    public CompoundTag getUpdateTag() { CompoundTag t = super.getUpdateTag(); saveAdditional(t); return t; }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
