package com.hbm_m.blockentity.machines;

import com.hbm_m.block.machines.MachineWoodBurnerBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineWoodBurnerMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1 {@code TileEntityMachineWoodBurner}: 100000 HE, Feststoff (Brenndauer ueber {@code ModuleBurnTime}, Holzstaemme x4,
 * Holz x2) liefert 100 HE/t, Fluessigmodus verbrennt bis 2 mB/t aus dem Holzoeltank (16000 mB) mit
 * {@code Brennwert * mB / 2000}. Asche je Sorte ab 2000 Brennticks. Ohne Einschalten ({@code isOn}) laeuft nichts.
 * Port-Slotlage (GUI): 0 Brennstoff, 1 Asche, 2 Ladeslot; 3 Fluid-ID, 4/5 Kanister ein/aus (Original 2, 3/4, Batterie 5).
 */
public class MachineWoodBurnerBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2, com.hbm_m.api.tile.IControlReceiver {

    private static final int FUEL_SLOT = 0;
    private static final int ASH_SLOT = 1;
    private static final int CHARGE_SLOT = 2;
    private static final int FLUID_ID_SLOT = 3;
    private static final int FLUID_IN_SLOT = 4;
    private static final int FLUID_OUT_SLOT = 5;
    private static final int INVENTORY_SIZE = 6;

    /** Original: {@code maxPower = 100_000}. */
    private static final long CAPACITY = 100_000L;

    /** Original: {@code new ModuleBurnTime().setLogTimeMod(4).setWoodTimeMod(2)}. */
    public static final com.hbm_m.module.ModuleBurnTime burnModule = new com.hbm_m.module.ModuleBurnTime().setLogTimeMod(4).setWoodTimeMod(2);

    // GUI данные
    protected final ContainerData data;

    // Состояние горения
    private int burnTime = 0;
    private int maxBurnTime = 0;
    /** Original {@code isOn}: startet ausgeschaltet. */
    private boolean enabled = false;
    public boolean liquidBurn = false;
    protected int powerGen = 0;

    public int ashLevelWood;
    public int ashLevelCoal;
    public int ashLevelMisc;

    public final com.hbm_m.inventory.fluid.tank.FluidTank tank =
            new com.hbm_m.inventory.fluid.tank.FluidTank(com.hbm_m.inventory.fluid.ModFluids.WOODOIL.getSource(), 16_000);

    public MachineWoodBurnerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.WOOD_BURNER_BE.get(), pPos, pBlockState,
              INVENTORY_SIZE, CAPACITY, 0L, CAPACITY); // Original: nur Erzeuger, gibt alles ab

        this.data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> burnTime;
                    case 1 -> maxBurnTime;
                    case 2 -> isBurning() ? 1 : 0;
                    case 3 -> enabled ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int v) {
                if (i == 3) enabled = v != 0;
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineWoodBurnerBlockEntity be) {
        if (level.isClientSide()) return;

        // Инициализация сети через базовый класс
        be.ensureNetworkInitialized();

        boolean wasBurning = be.isBurning();

        be.powerGen = 0;

        ItemStack[] slots = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) slots[i] = be.inventory.getStackInSlot(i);
        boolean changed = be.tank.setType(FLUID_ID_SLOT, slots);
        changed |= be.tank.loadTank(FLUID_IN_SLOT, FLUID_OUT_SLOT, slots);
        if (changed) {
            for (int i = 0; i < INVENTORY_SIZE; i++) be.inventory.setStackInSlot(i, slots[i] == null ? ItemStack.EMPTY : slots[i]);
        }
        be.chargeItem();

        if (level.getGameTime() % 20 == 0) {
            for (BlockPos port : be.getExtraEnergyPorts()) {
                for (Direction dir : Direction.values()) be.trySubscribe(be.tank.getTankType(), level, port.relative(dir), dir);
            }
        }

        if (!be.liquidBurn) {

            if (be.burnTime <= 0) {

                ItemStack fuel = be.inventory.getStackInSlot(FUEL_SLOT);
                if (!fuel.isEmpty()) {
                    int burn = burnModule.getBurnTime(fuel);
                    if (burn > 0) {
                        MachineAshpitBlockEntity.AshType type = MachineFireboxBaseBlockEntity.getAshFromFuel(fuel);
                        if (type == MachineAshpitBlockEntity.AshType.WOOD) be.ashLevelWood += burn;
                        if (type == MachineAshpitBlockEntity.AshType.COAL) be.ashLevelCoal += burn;
                        if (type == MachineAshpitBlockEntity.AshType.MISC) be.ashLevelMisc += burn;
                        int threshold = 2000;
                        while (be.processAsh(be.ashLevelWood, ModItems.ASH_WOOD.get(), threshold)) be.ashLevelWood -= threshold;
                        while (be.processAsh(be.ashLevelCoal, ModItems.ASH_COAL.get(), threshold)) be.ashLevelCoal -= threshold;
                        while (be.processAsh(be.ashLevelMisc, ModItems.ASH_MISC.get(), threshold)) be.ashLevelMisc -= threshold;

                        be.maxBurnTime = be.burnTime = burn;
                        net.minecraft.world.item.Item container = fuel.getItem().getCraftingRemainingItem();
                        ItemStack rest = fuel.copy();
                        rest.shrink(1);
                        if (rest.isEmpty() && container != null) be.inventory.setStackInSlot(FUEL_SLOT, new ItemStack(container));
                        else be.inventory.setStackInSlot(FUEL_SLOT, rest);
                    }
                }

            } else if (be.energy < CAPACITY && be.enabled) {
                be.burnTime--;
                be.powerGen += 100;
                if (level.getGameTime() % 20 == 0) {
                    PollutionHandler.incrementPollution(level, pos, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND);
                }
            }

        } else {

            if (be.energy < CAPACITY && be.tank.getFill() > 0 && be.enabled) {
                com.hbm_m.inventory.fluid.trait.FT_Flammable trait =
                        com.hbm_m.inventory.fluid.FluidType.getTrait(be.tank.getTankType(), com.hbm_m.inventory.fluid.trait.FT_Flammable.class);

                if (trait != null) {
                    int toBurn = Math.min(be.tank.getFill(), 2);

                    if (toBurn > 0) {
                        be.powerGen += (int) (trait.getHeatEnergy() * toBurn / 2_000L);
                        be.tank.setFill(be.tank.getFill() - toBurn);
                        if (level.getGameTime() % 20 == 0) {
                            PollutionHandler.incrementPollution(level, pos, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * toBurn / 2F);
                        }
                    }
                }
            }
        }

        be.setEnergyStored(Math.min(CAPACITY, be.energy + be.powerGen));

        // Обновляем визуальное состояние блока
        if (wasBurning != be.isBurning()) {
            level.setBlock(pos, state.setValue(MachineWoodBurnerBlock.LIT, be.isBurning()), 3);
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    /** Original {@code processAsh}: legt bei erreichter Schwelle ein Aschehaeufchen in den Ascheplatz, falls es passt. */
    protected boolean processAsh(int level, net.minecraft.world.item.Item ash, int threshold) {
        if (level >= threshold) {
            ItemStack slot = inventory.getStackInSlot(ASH_SLOT);
            if (slot.isEmpty()) {
                inventory.setStackInSlot(ASH_SLOT, new ItemStack(ash));
                return true;
            } else if (slot.getCount() < slot.getMaxStackSize() && slot.is(ash)) {
                ItemStack grown = slot.copy();
                grown.grow(1);
                inventory.setStackInSlot(ASH_SLOT, grown);
                return true;
            }
        }
        return false;
    }

    /** Original {@code receiveControl}: "toggle" schaltet ein/aus, "switch" wechselt Fest-/Fluessigbetrieb. */
    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) setEnabled(!enabled);
        if (data.contains("switch")) {
            toggleLiquidBurn();
            sendUpdateToClient();
        }
    }

    /** Original {@code hasPermission}: Spieler in Reichweite (16 Bloecke). */
    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 256.0D;
    }

    /** Original {@code receiveControl("switch")}: Fest-/Fluessigbetrieb umschalten. */
    public void toggleLiquidBurn() {
        this.liquidBurn = !this.liquidBurn;
        setChanged();
    }

    public int getPowerGen() { return powerGen; }

    @Override public com.hbm_m.inventory.fluid.tank.FluidTank[] getAllTanks() { return new com.hbm_m.inventory.fluid.tank.FluidTank[] { tank }; }
    @Override public com.hbm_m.inventory.fluid.tank.FluidTank[] getReceivingTanks() { return new com.hbm_m.inventory.fluid.tank.FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public boolean isBurning() {
        return this.burnTime > 0;
    }


    // --- NBT ---
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries); // Сохраняет inventory и energy из базового класса
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("liquidBurn", liquidBurn);
        tag.putInt("powerGen", powerGen);
        tag.putInt("ashWood", ashLevelWood);
        tag.putInt("ashCoal", ashLevelCoal);
        tag.putInt("ashMisc", ashLevelMisc);
        tank.writeToNBT(tag, "t");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries); // Загружает inventory и energy из базового класса
        burnTime = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
        enabled = tag.getBoolean("enabled");
        liquidBurn = tag.getBoolean("liquidBurn");
        powerGen = tag.getInt("powerGen");
        ashLevelWood = tag.getInt("ashWood");
        ashLevelCoal = tag.getInt("ashCoal");
        ashLevelMisc = tag.getInt("ashMisc");
        tank.readFromNBT(tag, "t");
    }

    // --- GUI ---
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.wood_burner");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new MachineWoodBurnerMenu(id, inv, this, this.data);
    }

    public void drops() {
        if (level != null) {
            SimpleContainer simpleContainer = new SimpleContainer(inventory.getSlots());
            for (int i = 0; i < inventory.getSlots(); i++) {
                simpleContainer.setItem(i, inventory.getStackInSlot(i));
            }
            Containers.dropContents(this.level, this.worldPosition, simpleContainer);
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }



    private void chargeItem() {
        chargeItemInSlot(CHARGE_SLOT);
    }

    // --- Реализация абстрактных методов ---
    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.wood_burner");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == FUEL_SLOT) {
            // Original: alles mit Brennwert
            return burnModule.getBurnTime(stack) > 0;
        }
        if (slot == FLUID_ID_SLOT || slot == FLUID_IN_SLOT) return true;
        if (slot == ASH_SLOT) {
            // Ничего нельзя положить в слот пепла
            return false;
        }
        if (slot == CHARGE_SLOT) {
            return isEnergyReceiverItem(stack);
        }
        return false;
    }

    // Энергопорты мультиблока: позиции фантомов структуры, ранее регистрировавшиеся блоком.
    // Ядро (worldPosition) подписывается в BaseMachineBlockEntity.ensureNetworkInitialized().
    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        if (level == null || level.isClientSide) return new BlockPos[0];
        if (!(getBlockState().getBlock() instanceof com.hbm_m.block.machines.MachineWoodBurnerBlock block)) return new BlockPos[0];

        var helper = block.getStructureHelper();
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);

        java.util.List<BlockPos> ports = new java.util.ArrayList<>();
        for (BlockPos localPos : helper.getStructureMap().keySet()) {
            if (block.getPartRole(localPos) == com.hbm_m.multiblock.PartRole.ENERGY_CONNECTOR) {
                ports.add(helper.getRotatedPos(worldPosition, localPos, facing));
            }
        }
        return ports.toArray(new BlockPos[0]);
    }
    //? if forge {
    /** Original {@code ISidedInventory}: Slots {0, 1}; Brennstoff hinein, Asche heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0, 1 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot == 0 && burnModule.getBurnTime(stack) > 0; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot == 1; }
            });

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?}
}
