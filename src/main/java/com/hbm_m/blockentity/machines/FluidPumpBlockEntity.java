package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.ConnectionPriority;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.block.machines.FluidPumpBlock;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * BlockEntity насоса (порт TileEntityFluidPump из 1.7.10).
 *
 * Поведение:
 *  - Входная сторона: подписывается как receiver в сети.
 *  - Выходная сторона: если нет сигнала ред.камня — регистрируется как provider.
 *  - Имеет буфер (bufferSize мБ) и configurable ConnectionPriority.
 *  - При переполнении буфера (fill > bufferSize) getReceivingTanks() возвращает пустой массив.
 *
 * Ориентация: хранится в BlockState направление «вперёд» (direction.name() в NBT),
 * входная сторона — повёрнутая на 90° по часовой CW, выходная — её противоположность.
 */
public class FluidPumpBlockEntity extends BaseHbmBlockEntity implements IFluidStandardTransceiverMK2, com.hbm_m.api.tile.IControlReceiver {

    /** Размер буфера по умолчанию (мБ). */
    public int bufferSize = 100;
    /** Тип жидкости. */
    private Fluid fluidType = Fluids.EMPTY;
    /** Основной бак. */
    private FluidTank tank;
    /** Приоритет в сети. */
    public ConnectionPriority priority = ConnectionPriority.NORMAL;
    /** true если есть сигнал редстоуна. */
    public boolean redstonePowered = false;
    /** Направление «вперёд» (от входа к выходу). */
    private Direction facing = Direction.NORTH;

    public FluidPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_PUMP_BE.get(), pos, state);
        tank = new FluidTank(Fluids.EMPTY, bufferSize);
    }

    // =====================================================================================
    // IFluidUserMK2
    // =====================================================================================

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[]{ tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && VanillaFluidEquivalence.sameSubstance(fluid, this.fluidType);
    }

    // =====================================================================================
    // IFluidStandardTransceiverMK2
    // =====================================================================================

    /** Отдаёт только если нет сигнала ред.камня. */
    @Override
    public FluidTank[] getSendingTanks() {
        return redstonePowered ? new FluidTank[0] : new FluidTank[]{ tank };
    }

    /** Принимает только если буфер не переполнен. */
    @Override
    public FluidTank[] getReceivingTanks() {
        return tank.getFill() < bufferSize ? new FluidTank[]{ tank } : new FluidTank[0];
    }

    @Override
    public ConnectionPriority getFluidPriority() { return priority; }

    // =====================================================================================
    // Tick
    // =====================================================================================

    public static void tick(Level level, BlockPos pos, BlockState state, FluidPumpBlockEntity entity) {
        if (level.isClientSide || !(level instanceof ServerLevel)) return;

        Direction fromState = state.getValue(FluidPumpBlock.FACING);
        if (entity.facing != fromState) {
            entity.facing = fromState;
        }

        // Синхронизируем размер бака с bufferSize (не уничтожаем буферизованный флюид)
        if (entity.bufferSize != entity.tank.getMaxFill()) {
            int nextSize = Math.max(entity.tank.getFill(), entity.bufferSize);
            entity.tank.changeTankSize(nextSize);
        }

        entity.redstonePowered = level.hasNeighborSignal(pos);

        // Входная сторона (CW от facing): подписка как receiver
        Direction inDir  = rotateClockwise(entity.facing);
        // Выходная сторона: противоположная входной
        Direction outDir = inDir.getOpposite();

        entity.trySubscribe(entity.fluidType, level, pos.relative(inDir), inDir);
        if (!entity.redstonePowered) {
            entity.tryProvide(entity.fluidType, level, pos.relative(outDir), outDir);
        }
    }

    /** CW поворот горизонтальных направлений (имитирует ForgeDirection.getRotation(UP)). */
    private static Direction rotateClockwise(Direction dir) {
        return switch (dir) {
            case NORTH -> Direction.EAST;
            case EAST  -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST  -> Direction.NORTH;
            default    -> Direction.EAST;
        };
    }

    // =====================================================================================
    // Fluid type / facing setters
    // =====================================================================================

    public Fluid getFluidType() { return fluidType; }

    public void setFluidType(Fluid fluid) {
        this.fluidType = fluid != null ? fluid : Fluids.EMPTY;
        tank.setTankType(this.fluidType);
        setChanged();
    }

    public Direction getFacing() { return facing; }

    public void setFacing(Direction facing) {
        this.facing = facing != null && facing.getAxis() != Direction.Axis.Y ? facing : Direction.NORTH;
        setChanged();
    }

    /** Original {@code receiveControl} (GUIPump): Durchsatz 0-10.000 mB, Druck 0-5 PU, Prioritaet. */
    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("capacity")) {
            this.bufferSize = net.minecraft.util.Mth.clamp(data.getInt("capacity"), 0, 10_000);
        }
        if (data.contains("pressure")) {
            this.tank.withPressure(net.minecraft.util.Mth.clamp(data.getByte("pressure"), 0, 5));
        }
        if (data.contains("priority")) {
            this.priority = fromOrdinal(ConnectionPriority.class, data.getByte("priority"));
        }
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public boolean hasPermission(net.minecraft.world.entity.player.Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 128.0D;
    }

    // =====================================================================================
    // NBT
    // =====================================================================================

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        tank.writeToNBT(tag, "t");
        tag.putByte("priority", (byte) priority.ordinal());
        tag.putInt("bufferSize", bufferSize);
        tag.putString("facing", facing.getName());
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        tank.readFromNBT(tag, "t");
        fluidType = tank.getTankType();
        priority = fromOrdinal(ConnectionPriority.class, tag.getByte("priority"));
        bufferSize = tag.getInt("bufferSize");
        if (bufferSize <= 0) bufferSize = 100;
        if (tag.contains("facing")) {
            facing = Direction.byName(tag.getString("facing"));
            if (facing == null || facing.getAxis() == Direction.Axis.Y) facing = Direction.NORTH;
        }
    }

    private static <E extends Enum<E>> E fromOrdinal(Class<E> clazz, byte ordinal) {
        E[] values = clazz.getEnumConstants();
        if (ordinal < 0 || ordinal >= values.length) return values[2];
        return values[ordinal];
    }
}