package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IControlReceiver;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.network.MachineControlC2SPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * Combustion Engine — полный порт {@code TileEntityMachineCombustionEngine} (1.7.10).
 *
 * <p>Работает только при включённом зажигании ({@code isOn}, кнопка GUI "turnOn")
 * и ненулевом дросселе ({@code setting}, 0..30, драг-слайдер в GUI): расход
 * {@code setting * 2} mB/тик (в десятых долях mB — {@code tenth}), выработка
 * {@code toBurn * (combustionEnergy / 10000) * eff}, где eff — из полной матрицы
 * поршень × грейд топлива (1:1 с {@code ItemPistons.EnumPistonType}).
 *
 * <p>Слоты 1:1 с оригиналом: 0/1 — канистры (автозалив/автоопустошение),
 * 2 — комплект поршней, 3 — батарея, 4 — fluid identifier (смена типа топлива;
 * по умолчанию, как в оригинале, Diesel).
 *
 * <p>Установленные расхождения с оригиналом: {@code TileEntityMachinePolluting}
 * (выхлоп в дымовые баки) и Redstone over Radio/OC-мосты не портированы
 * (сквозной пробел порта); анимация двери и петля звука двигателя — чистый
 * визуал/акустика рендерера, не механика.
 */
public class MachineCombustionEngineBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2, IControlReceiver, com.hbm_m.interfaces.IFluidCopiable {

    public static final int SLOT_FLUID_IN = 0;
    public static final int SLOT_FLUID_OUT = 1;
    public static final int SLOT_PISTON = 2;
    public static final int SLOT_BATTERY = 3;
    public static final int SLOT_FLUID_ID = 4;
    public static final int INVENTORY_SIZE = 5;

    private static final int TANK_CAPACITY_MB = 24_000;
    private static final long MAX_POWER = 2_500_000L;

    /** Оригинал: isOn — зажигание из GUI. */
    public boolean isOn = false;
    /** Оригинал: setting — дроссель 0..30, расход setting * 2 mB/тик. */
    public int setting = 0;
    /** Оригинал: tenth — дробная часть mB (бак ведётся в десятых долях mB). */
    public int tenth = 0;

    private final FluidTank tank = new FluidTank(ModFluids.DIESEL.getSource(), TANK_CAPACITY_MB);

    public MachineCombustionEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBUSTION_ENGINE_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, 0L, MAX_POWER);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return tank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCombustionEngineBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        // Оригинал: loadTank(0, 1) + setType(4); смена типа сбрасывает десятые доли.
        ItemStack[] slots = getSlotsArray();
        boolean slotsChanged = false;
        if (tank.loadTank(SLOT_FLUID_IN, SLOT_FLUID_OUT, slots)) slotsChanged = true;
        if (tank.setType(SLOT_FLUID_ID, slots)) {
            this.tenth = 0;
            slotsChanged = true;
        }
        if (slotsChanged) applySlotsArray(slots);

        // Оригинал: работа только при isOn && setting > 0, наличии поршней, топлива
        // и FT_Combustible. Десятые доли mB: fill = getFill() * 10 + tenth.
        int fill = tank.getFill() * 10 + tenth;

        if (isOn && setting > 0 && isPistonSet(inventory.getStackInSlot(SLOT_PISTON).getItem()) && fill > 0) {
            FT_Combustible trait = FluidType.getTrait(tank.getTankType(), FT_Combustible.class);

            if (trait != null) {
                double eff = pistonEfficiency(inventory.getStackInSlot(SLOT_PISTON).getItem(), trait.getGrade());

                if (eff > 0) {
                    int speed = setting * 2;

                    int toBurn = Math.min(fill, speed);
                    long produced = (long) (toBurn * (trait.getCombustionEnergy() / 10_000D) * eff);
                    setEnergyStored(Math.min(getMaxEnergyStored(), getEnergyStored() + produced));
                    fill -= toBurn;

                    tank.setFill(fill / 10);
                    tenth = fill % 10;
                }
            }
        }

        // Заряд батареи в слоте 3 (оригинал chargeItemsFromTE(slots, 3, ...)).
        chargeItemInSlot(SLOT_BATTERY);

        setChanged();
        sendUpdateToClient();
    }

    // ─────────────────── Эффективность поршней (1:1 ItemPistons) ───────────────────

    /** Порядок грейдов 1:1 с FuelGrade: LOW, MEDIUM, HIGH, AERO, GAS. */
    private static final double[][] PISTON_EFF = {
            //          LOW    MEDIUM  HIGH   AERO   GAS
            { 1.00D, 0.75D, 0.25D, 0.00D, 0.00D },  // STEEL
            { 0.50D, 1.00D, 0.90D, 0.50D, 0.00D },  // DURA
            { 0.00D, 0.50D, 1.00D, 0.75D, 0.00D },  // DESH
            { 0.50D, 0.75D, 1.00D, 0.90D, 0.50D },  // STARMETAL
    };

    /** Индекс поршня: 0=STEEL, 1=DURA, 2=DESH, 3=STARMETAL; -1 = не поршень. */
    public static int pistonIndex(Item piston) {
        if (piston == ModItems.PISTON_SET_STEEL.get()) return 0;
        if (piston == ModItems.PISTON_SET_DURA.get()) return 1;
        if (piston == ModItems.PISTON_SET_DESH.get()) return 2;
        if (piston == ModItems.PISTON_SET_STARMETAL.get()) return 3;
        return -1;
    }

    public static boolean isPistonSet(Item piston) {
        return pistonIndex(piston) >= 0;
    }

    /** Оригинал {@code piston.eff[trait.getGrade().ordinal()]}. */
    public static double pistonEfficiency(Item piston, FT_Combustible.FuelGrade grade) {
        int idx = pistonIndex(piston);
        if (idx < 0) return 0.0D;
        return PISTON_EFF[idx][grade.ordinal()];
    }

    private ItemStack[] getSlotsArray() {
        ItemStack[] slots = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) slots[i] = inventory.getStackInSlot(i);
        return slots;
    }

    private void applySlotsArray(ItemStack[] slots) {
        for (int i = 0; i < INVENTORY_SIZE; i++) inventory.setStackInSlot(i, slots[i]);
    }

    // ==================== Управление (зажигание / дроссель) ====================

    /** Оригинал hasPermission: ближе 25 блоков (getDistance < 25 → distanceToSqr < 625). */
    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 625;
    }

    /** Оригинал receiveControl: "turnOn" → зажигание, "setting" → дроссель. */
    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("turnOn")) this.isOn = !this.isOn;
        if (data.contains("setting")) this.setting = data.getInt("setting");
        setChanged();
        sendUpdateToClient();
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("isOn", isOn);
        tag.putInt("setting", setting);
        tag.putInt("tenth", tenth);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        isOn = tag.getBoolean("isOn");
        setting = tag.getInt("setting");
        tenth = tag.getInt("tenth");
        tank.readFromNBT(tag, "tank");
    }

    // ==================== GETTERS / MENU ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.combustion_engine");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_IN -> true;
            case SLOT_PISTON -> isPistonSet(stack.getItem());
            case SLOT_BATTERY -> isEnergyProviderItem(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.hbm_m.inventory.menu.MachineCombustionEngineMenu(containerId, playerInventory, this);
    }

    public FluidTank getTank() {
        return tank;
    }

    /** Оригинальное состояние "engine running" (поршни + топливо + расход > 0). */
    public boolean isActive() {
        return isOn && setting > 0 && isPistonSet(inventory.getStackInSlot(SLOT_PISTON).getItem())
                && tank.getFluidAmountMb() >= 1;
    }
}
