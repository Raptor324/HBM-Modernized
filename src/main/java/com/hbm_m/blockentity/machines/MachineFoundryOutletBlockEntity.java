package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.block.machines.MachineFoundryOutletBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.util.CrucibleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code TileEntityFoundryOutlet}: Ausguss am Ende einer Rinne. Nimmt nur von hinten auf und giesst direkt
 * in den Abnehmer bis vier Bloecke darunter. Filtermaterial (auch umkehrbar) und Redstone-Sperre (umkehrbar).
 */
public class MachineFoundryOutletBlockEntity extends MachineFoundryBaseBlockEntity {

    public NTMMaterial filter = null;
    public NTMMaterial lastFilter = null;
    /* inverts filter behavior, will let everything but the filter material pass */
    public boolean invertFilter = false;
    /** inverts redstone behavior, i.e. when TRUE, the outlet will be blocked by default and only open with redstone */
    public boolean invertRedstone = false;
    public boolean lastClosed = false;

    public MachineFoundryOutletBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.FOUNDRY_OUTLET_BE.get(), pos, state);
    }

    protected MachineFoundryOutletBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** if TRUE, prevents all fluids from flowing through the outlet and renders a small barrier */
    public boolean isClosed() {
        return level != null && (invertRedstone ^ this.level.hasNeighborSignal(worldPosition));
    }

    protected Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(MachineFoundryOutletBlock.FACING) ? state.getValue(MachineFoundryOutletBlock.FACING) : Direction.NORTH;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (level != null && !level.isClientSide) {
            boolean closed = isClosed();
            if (this.lastClosed != closed || this.filter != this.lastFilter) {
                this.lastFilter = this.filter;
                this.lastClosed = closed;
                markForUpdate();
            }
        }
    }

    @Override public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, MaterialStack stack) { return false; }
    @Override @Nullable public MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, MaterialStack stack) { return stack; }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos pos, Direction side, MaterialStack stack) {

        if (filter != null && (filter != stack.material ^ invertFilter)) return false;
        if (isClosed()) return false;
        if (side != facing().getOpposite()) return false;

        Vec3 start = new Vec3(pos.getX() + 0.5, pos.getY() - 0.125, pos.getZ() + 0.5);
        Vec3 end = new Vec3(pos.getX() + 0.5, pos.getY() + 0.125 - 4, pos.getZ() + 0.5);

        BlockHitResult[] mop = new BlockHitResult[1];
        ICrucibleAcceptor acc = CrucibleUtil.getPouringTarget(world, start, end, mop);

        if (acc == null) {
            return false;
        }

        Vec3 hit = mop[0].getLocation();
        return acc.canAcceptPartialPour(world, mop[0].getBlockPos(), hit.x, hit.y, hit.z, Direction.UP, stack);
    }

    @Override
    @Nullable
    public MaterialStack flow(Level world, BlockPos pos, Direction side, MaterialStack stack) {

        Vec3 start = new Vec3(pos.getX() + 0.5, pos.getY() - 0.125, pos.getZ() + 0.5);
        Vec3 end = new Vec3(pos.getX() + 0.5, pos.getY() + 0.125 - 4, pos.getZ() + 0.5);

        BlockHitResult[] mop = new BlockHitResult[1];
        ICrucibleAcceptor acc = CrucibleUtil.getPouringTarget(world, start, end, mop);

        if (acc == null)
            return stack;

        Vec3 hit = mop[0].getLocation();
        MaterialStack didPour = acc.pour(world, mop[0].getBlockPos(), hit.x, hit.y, hit.z, Direction.UP, stack);

        if (stack != null && world instanceof ServerLevel server) {
            Direction dir = side.getOpposite();
            double hitY = mop[0].getBlockPos().getY() + 1;
            CompoundTag data = new CompoundTag();
            data.putString("type", "foundry");
            data.putInt("color", stack.material.moltenColor);
            data.putByte("dir", (byte) dir.get3DDataValue());
            data.putFloat("off", 0.375F);
            data.putFloat("base", 0F);
            data.putFloat("len", Math.max(1F, worldPosition.getY() - (float) (Math.ceil(hitY) - 0.875)));
            com.hbm_m.particle.helper.IParticleCreator.sendPacket(server, worldPosition.getX() + 0.5D - dir.getStepX() * 0.125, worldPosition.getY() + 0.125, worldPosition.getZ() + 0.5D - dir.getStepZ() * 0.125, 50, data);
        }

        return didPour;
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.invertRedstone = nbt.getBoolean("invert");
        this.invertFilter = nbt.getBoolean("invertFilter");
        this.filter = Mats.matById.get((int) nbt.getShort("filter"));
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putBoolean("invert", this.invertRedstone);
        nbt.putBoolean("invertFilter", this.invertFilter);
        nbt.putShort("filter", this.filter == null ? -1 : (short) this.filter.id);
    }
}
