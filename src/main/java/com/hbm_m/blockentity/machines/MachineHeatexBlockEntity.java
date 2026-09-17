package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.block.machines.MachineHeatexBlock;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Coolable;
import com.hbm_m.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm_m.inventory.menu.MachineHeatexMenu;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.item.liquids.FluidIdentifierItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * Port of {@code TileEntityHeaterHeatex} (1.7.10 Original) - converts a hot fluid into its cooled
 * form (via the already-ported {@link FT_Coolable}/{@code CoolingType.HEATEXCHANGER} fluid-trait
 * data, e.g. hot reactor coolant -> coolant) and, as a byproduct, generates heat exposed via
 * {@link IHeatSource}.
 * <p>
 * Slot 0 accepts a fluid identifier item which re-types the HOT tank every tick
 * (original: {@code tanks[0].setType(0, slots)}). If the hot type has no HEATEXCHANGER-coolable
 * trait, BOTH tanks are invalidated to NONE (original: {@code setupTanks()}).
 * Fluid connections are restricted to the front/back faces along {@code FACING}
 * (original: {@code canConnect = dir == facing || dir == facing.getOpposite()}), with the four
 * diagonal subscription positions at ±2 in facing/rotated space (original: {@code getConPos()}).
 */
public class MachineHeatexBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2, IHeatSource, com.hbm_m.interfaces.IFluidCopiable {

    private static final int TANK_CAPACITY = 24_000;

    /** Original default: {@code amountToCool = 24_000}. */
    public static final int DEFAULT_AMOUNT_TO_COOL = 24_000;
    /** Original default: {@code tickDelay = 1}. */
    public static final int DEFAULT_TICK_DELAY = 1;

    private final FluidTank hotTank = new FluidTank(ModFluids.COOLANT_HOT.getSource(), TANK_CAPACITY);
    private final FluidTank coldTank = new FluidTank(ModFluids.COOLANT.getSource(), TANK_CAPACITY);

    private int amountToCool = DEFAULT_AMOUNT_TO_COOL;
    private int tickDelay = DEFAULT_TICK_DELAY;
    private int heatEnergy = 0;

    public MachineHeatexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATEX_BE.get(), pos, state, 1, 0L, 0L, 0L);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return hotTank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    // ==================== Тики ====================

    public static void tick(Level level, BlockPos pos, BlockState state, MachineHeatexBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {

        // Оригинал: tanks[0].setType(0, slots) — предмет-идентификатор ретипит ГОРЯЧИЙ бак каждый тик
        this.hotTank.setType(0, new ItemStack[] { inventory.getStackInSlot(0) });
        this.setupTanks();

        // Подписки/провайд на сеть труб — раз в 20 тиков (конвенция MK2 этого репозитория)
        if (level.getGameTime() % 20 == 0) {
            for (ConPos con : getConPos()) {
                trySubscribe(hotTank.getTankType(), level, con.pos, con.dir);
            }
        }

        // Оригинал: this.heatEnergy *= 0.999; — каждый тик, ПЕРЕД tryConvert
        this.heatEnergy = (int) (this.heatEnergy * 0.999D);

        this.tryConvert(level);

        // Оригинал: sendFluid(tanks[1], ...) каждый тик по всем conPos
        for (ConPos con : getConPos()) {
            if (this.coldTank.getFill() > 0) tryProvide(coldTank, level, con.pos, con.dir);
        }

        // Оригинал: networkPackNT(25)
        if (level.getGameTime() % 25 == 0) sendUpdateToClient();
    }

    /** Оригинал {@code setupTanks()}: валидный HEATEXCHANGER-трит задаёт тип холодного бака, иначе ОБА бака -> NONE. */
    protected void setupTanks() {
        FT_Coolable trait = FluidType.getTrait(hotTank.getTankType(), FT_Coolable.class);
        if (trait != null) {
            double eff = trait.getEfficiency(CoolingType.HEATEXCHANGER);
            if (eff > 0) {
                coldTank.setTankType(trait.coolsTo);
                return;
            }
        }
        hotTank.setTankType(ModFluids.NONE.getSource());
        coldTank.setTankType(ModFluids.NONE.getSource());
    }

    /** Оригинал {@code tryConvert()}: конвертация раз в {@code tickDelay} тиков, лимит операций {@code amountToCool}. */
    protected void tryConvert(ServerLevel level) {
        FT_Coolable trait = FluidType.getTrait(hotTank.getTankType(), FT_Coolable.class);
        if (trait == null) return;
        if (tickDelay < 1) tickDelay = 1;
        if (level.getGameTime() % tickDelay != 0) return;
        if (trait.amountReq <= 0 || trait.amountProduced <= 0) return;

        double eff = trait.getEfficiency(CoolingType.HEATEXCHANGER);
        if (eff <= 0) return;

        int inputOps = hotTank.getFill() / trait.amountReq;
        int outputOps = (coldTank.getMaxFill() - coldTank.getFill()) / trait.amountProduced;
        int opCap = this.amountToCool;

        int ops = Math.min(inputOps, Math.min(outputOps, opCap));
        hotTank.setFill(hotTank.getFill() - trait.amountReq * ops);
        coldTank.setFill(coldTank.getFill() + trait.amountProduced * ops);
        this.heatEnergy += (int) (trait.heatEnergy * ops * eff);
        setChanged();
    }

    /** Контроль из GUI. Оригинал {@code receiveControl}: toCool clamp 1..maxFill, delay min 1. */
    public void receiveControl(int toCool, int tickDelay) {
        // Сентинел NOT_SET = аналог отсутствующего NBT-ключа в оригинальном receiveControl
        if (toCool != com.hbm_m.network.SetHeatexControlC2SPacket.NOT_SET)
            this.amountToCool = Mth.clamp(toCool, 1, hotTank.getMaxFill());
        if (tickDelay != com.hbm_m.network.SetHeatexControlC2SPacket.NOT_SET)
            this.tickDelay = Math.max(tickDelay, 1);
        setChanged();
        sendUpdateToClient();
    }

    // ==================== Подключения (оригинал getConPos / canConnect) ====================

    private record ConPos(BlockPos pos, Direction dir) {}

    /** Оригинал {@code getConPos()}: 4 диагональные позиции на ±2 по facing/rot. */
    private ConPos[] getConPos() {
        Direction dir = getBlockState().getValue(MachineHeatexBlock.FACING);
        Direction rot = dir.getClockWise();

        BlockPos p = getBlockPos();
        return new ConPos[] {
                new ConPos(p.offset(dir.getNormal().getX() * 2 + rot.getNormal().getX(),
                                   0,
                                   dir.getNormal().getZ() * 2 + rot.getNormal().getZ()), dir),
                new ConPos(p.offset(dir.getNormal().getX() * 2 - rot.getNormal().getX(),
                                   0,
                                   dir.getNormal().getZ() * 2 - rot.getNormal().getZ()), dir),
                new ConPos(p.offset(-dir.getNormal().getX() * 2 + rot.getNormal().getX(),
                                   0,
                                   -dir.getNormal().getZ() * 2 + rot.getNormal().getZ()), dir.getOpposite()),
                new ConPos(p.offset(-dir.getNormal().getX() * 2 - rot.getNormal().getX(),
                                   0,
                                   -dir.getNormal().getZ() * 2 - rot.getNormal().getZ()), dir.getOpposite())
        };
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        // Оригинал: dir == facing || dir == facing.getOpposite()
        Direction facing = getBlockState().getValue(MachineHeatexBlock.FACING);
        return fromDir == facing || fromDir == facing.getOpposite();
    }

    // ==================== IFluidUserMK2 ====================

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { hotTank };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { coldTank };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { hotTank, coldTank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public FluidTank getHotTank() { return hotTank; }
    public FluidTank getColdTank() { return coldTank; }
    public int getAmountToCool() { return amountToCool; }
    public int getTickDelay() { return tickDelay; }
    public int getHeatEnergy() { return heatEnergy; }

    // ==================== Устройство настройки (оригинал) ====================
    // Копия: тип горячей жидкости + amountToCool; вставка по индексу выбирает,
    // какой из скопированных типов назначить горячему баку.

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = com.hbm_m.interfaces.IFluidCopiable.super.getSettings(level, pos);
        nbt.putInt("toCool", amountToCool);
        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        net.minecraft.nbt.ListTag list = nbt.getList("fluids", net.minecraft.nbt.Tag.TAG_STRING);
        if (!list.isEmpty() && index < list.size()) {
            hotTank.setTankType(com.hbm_m.interfaces.IFluidCopiable.fluidFromString(list.getString(index)));
        }
        if (nbt.contains("toCool")) {
            amountToCool = Mth.clamp(nbt.getInt("toCool"), 1, hotTank.getMaxFill());
        }
        setChanged();
        sendUpdateToClient();
    }

    // ==================== IHeatSource ====================

    @Override
    public int getHeatStored() {
        return heatEnergy;
    }

    /**
     * Оригинал не имеет верхнего предела heatEnergy (int, затухает на 0.999/тик).
     * Интерфейс этого репозитория требует max — возвращаем "без ограничения".
     */
    @Override
    public int getMaxHeatStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void useUpHeat(int amount) {
        heatEnergy = Math.max(0, heatEnergy - amount);
        setChanged();
    }

    // ==================== Инвентарь / меню ====================

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // Оригинал: единственный слот принимает только fluid identifier
        return slot == 0 && stack.getItem() instanceof FluidIdentifierItem;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.heatex");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineHeatexMenu.create(id, inventory, this);
    }

    // ==================== NBT (ключи как в оригинале) ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        hotTank.writeToNBT(tag, "0");
        coldTank.writeToNBT(tag, "1");
        tag.putInt("heatEnergy", heatEnergy);
        tag.putInt("toCool", amountToCool);
        tag.putInt("delay", tickDelay);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        hotTank.readFromNBT(tag, "0");
        coldTank.readFromNBT(tag, "1");
        heatEnergy = tag.getInt("heatEnergy");
        amountToCool = tag.getInt("toCool");
        tickDelay = tag.getInt("delay");
    }
}
