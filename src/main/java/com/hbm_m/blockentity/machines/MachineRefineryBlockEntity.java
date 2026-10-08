package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.FluidItemAccess;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.recipes.RefineryRecipes;
import com.hbm_m.inventory.recipes.RefineryRecipes.RefineryRecipe;
import com.hbm_m.inventory.menu.MachineRefineryMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.FluidIdentifierItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

//? if forge {
import org.jetbrains.annotations.Nullable;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?} else {
/*import org.jetbrains.annotations.Nullable;
*///?}
import org.jetbrains.annotations.NotNull;

/**
 * Refinery BlockEntity - processes crude oil into petroleum products.
 */
public class MachineRefineryBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.block.IPersistentNBT, IFluidStandardTransceiverMK2, com.hbm_m.api.tile.IRepairable {

    public static final int INVENTORY_SIZE = 13;
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_CANISTER_IN = 1;
    public static final int SLOT_CANISTER_OUT = 2;
    public static final int SLOT_HEAVY_IN = 3;
    public static final int SLOT_HEAVY_OUT = 4;
    public static final int SLOT_NAPHTHA_IN = 5;
    public static final int SLOT_NAPHTHA_OUT = 6;
    public static final int SLOT_LIGHT_IN = 7;
    public static final int SLOT_LIGHT_OUT = 8;
    public static final int SLOT_PETROLEUM_IN = 9;
    public static final int SLOT_PETROLEUM_OUT = 10;
    public static final int SLOT_SULFUR_OUT = 11;
    public static final int SLOT_FLUID_ID = 12;

    public static final int TANK_INPUT = 0;
    public static final int TANK_HEAVY = 1;
    public static final int TANK_NAPHTHA = 2;
    public static final int TANK_LIGHT = 3;
    public static final int TANK_PETROLEUM = 4;

    public static final int MAX_SULFUR = 10;
    public static final long ENERGY_CAPACITY = 1_000L;
    private static final long ENERGY_RECEIVE_RATE = 1_000L;
    public static final long ENERGY_PER_TICK = 5L;
    /** Original {@code RefineryRecipes}: Eingang immer 100 mB. */
    public static final int INPUT_CONSUMPTION_MB = 100;

    private final FluidTank[] tanks = new FluidTank[] {
        // Original: new FluidTank(Fluids.HOTOIL, 64_000) - nur die per Kennung gesetzte Sorte, kein kaltes Oel.
        new FluidTank(ModFluids.HOTOIL.getSource(), 64_000),
            new FluidTank(ModFluids.HEAVYOIL.getSource(), 24_000),
            new FluidTank(ModFluids.NAPHTHA.getSource(), 24_000),
            new FluidTank(ModFluids.LIGHTOIL.getSource(), 24_000),
        new FluidTank(ModFluids.PETROLEUM.getSource(), 24_000)
    };

    private int sulfurProgress = 0;
    private boolean isOn = false;

    /**
     * {@code hasExploded} / {@code onFire}: a refinery that has been blown up stops working and
     * burns until someone puts it out. The port had neither - a bomb landing on a refinery simply
     * broke the block like any other, which is also why {@code inferno} had no trigger.
     */
    public boolean hasExploded = false;
    public boolean onFire = false;
    /** Guards against one blast calling into every block of the multiblock in turn. */
    public Object lastExplosion = null;

    public void explode() {
        if (this.hasExploded) return;
        this.hasExploded = true;
        this.onFire = true;
        this.isOn = false;
        this.setChanged();
        syncExplodedState();
    }

    /** Original {@code repair}: nur die Beschaedigung, ein Brand brennt weiter. */
    @Override
    public void repair() {
        this.hasExploded = false;
        this.setChanged();
        syncExplodedState();
    }

    @Override
    public boolean isDamaged() {
        return this.hasExploded;
    }

    private final java.util.List<RepairStack> repairList = new java.util.ArrayList<>();

    @Override
    public java.util.List<RepairStack> getRepairMaterials() {
        if (!repairList.isEmpty()) return repairList;
        repairList.add(new RepairStack(net.minecraft.world.item.crafting.Ingredient.of(com.hbm_m.item.material.ModMaterialItems.item(com.hbm_m.item.material.ModMaterials.STEEL, com.hbm_m.item.material.MaterialShape.PLATE)), 8));
        repairList.add(new RepairStack(net.minecraft.world.item.crafting.Ingredient.of(com.hbm_m.item.ModItems.DUCTTAPE.get()), 4));
        return repairList;
    }

    /** 1:1 {@code tryExtinguish}: Schaum/CO2 loeschen, Wasser auf gefuellte Tanks laesst sie explodieren. */
    @Override
    public void tryExtinguish(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, EnumExtinguishType type) {
        if (!this.hasExploded || !this.onFire) return;

        if (type == EnumExtinguishType.FOAM || type == EnumExtinguishType.CO2) {
            this.onFire = false;
            this.setChanged();
            return;
        }

        if (type == EnumExtinguishType.WATER) {
            for (FluidTank tank : tanks) {
                if (tank.getFill() > 0) {
                    world.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, 5F, true, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
                    return;
                }
            }
        }
    }
    /**
     * Pushes {@code hasExploded} into the blockstate so the wrecked model is used. The state is
     * the single source of truth for rendering; the field stays authoritative for behaviour.
     */
    private void syncExplodedState() {
        if (level == null || level.isClientSide) return;
        net.minecraft.world.level.block.state.BlockState state = getBlockState();
        if (!state.hasProperty(com.hbm_m.block.machines.MachineRefineryBlock.EXPLODED)) return;
        if (state.getValue(com.hbm_m.block.machines.MachineRefineryBlock.EXPLODED) == this.hasExploded) return;
        level.setBlock(worldPosition, state.setValue(com.hbm_m.block.machines.MachineRefineryBlock.EXPLODED, this.hasExploded), 3);
    }


    //? if forge {
    private LazyOptional<IFluidHandler> fluidHandler = LazyOptional.empty();
    //?}

    public MachineRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REFINERY_BE.get(), pos, state,
              INVENTORY_SIZE, ENERGY_CAPACITY, ENERGY_RECEIVE_RATE);
    }

    @Override public int getFloorCount() { return 2 * 2; }
    @Override public BlockPos getFloorPosFromIndex(int index) { return this.standardFloor3x3(index); }

    /** Original: Nachlauf des Betriebsgeraeuschs in Ticks. */
    private int audioTime;

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRefineryBlockEntity be) {
        if (level.isClientSide()) {
            // Original: getLoopedSound("hbm:block.boiler", 0.25F, 15F, 1.0F, 20), 20 Ticks Nachlauf
            if (be.isOn) be.audioTime = 20;
            boolean play = be.audioTime > 0;
            if (play) be.audioTime--;
            com.hbm_m.client.sound.MachineLoopSoundClient.tick(be, "hbm:block.boiler", play, 0.25F, 1.0F, 15);
            return;
        }

        be.checkTilt(TiltType.CONFIG, false);

        boolean changed = false;

        if (!be.hasExploded) {
            be.chargeFromBatterySlot(SLOT_BATTERY);
            if (be.processFluidContainers()) {
                changed = true;
            }
            if (be.refine()) {
                changed = true;
            }
        } else {
            if (be.isOn) {
                be.isOn = false;
                changed = true;
            }
            if (be.onFire && be.burn()) changed = true;
        }

        if (changed) {
            be.setChanged();
            be.sendUpdateToClient();
        }

        be.ensureNetworkInitialized();
    }

    public FluidTank[] getTanks() {
        return tanks;
    }

    public FluidTank getTank(int index) {
        return (index >= 0 && index < tanks.length) ? tanks[index] : tanks[0];
    }

    // ═══════════════════════════ IFluidStandardTransceiverMK2 ════════════════════════════════
    // Делает контроллер видимым для MK2-сети жидкостных труб (как у химической установки):
    // без этого UniversalMachinePartBlockEntity не подписывает рефайнери ни как receiver, ни
    // как provider, и трубы не могут залить сырую нефть / забрать продукты переработки.

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tanks[TANK_INPUT] };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tanks[TANK_HEAVY], tanks[TANK_NAPHTHA], tanks[TANK_LIGHT], tanks[TANK_PETROLEUM] };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public boolean hasDisplayRecipe() {
        return RefineryRecipes.getRefinery(getTank(TANK_INPUT).getTankType()) != null;
    }

    public Fluid[] getDisplayPipeFluids() {
        RefineryRecipe recipe = RefineryRecipes.getRefinery(getTank(TANK_INPUT).getTankType());
        if (recipe == null) {
            return new Fluid[] {
                    ModFluids.NONE.getSource(),
                    ModFluids.NONE.getSource(),
                    ModFluids.NONE.getSource(),
                    ModFluids.NONE.getSource()
            };
        }
        Fluid[] out = new Fluid[4];
        for (int i = 0; i < 4; i++) out[i] = recipe.outputs[i].type();
        return out;
    }

    /** Original: Rohre abonnieren nur {@code tanks[0].getTankType()} - also genau die per Kennung gesetzte Sorte. */
    public boolean canAcceptInputFluid(Fluid fluid) {
        if (fluid == null) return false;
        FluidTank input = tanks[TANK_INPUT];
        return input.getPressure() == 0 && VanillaFluidEquivalence.sameSubstance(input.getTankType(), fluid);
    }

    private boolean processFluidContainers() {
        ItemStack[] slots = getSlotsArray();
        boolean changed = false;

        if (tanks[TANK_INPUT].setType(SLOT_FLUID_ID, slots)) {
            changed = true;
        }
        if (tanks[TANK_INPUT].loadTank(SLOT_CANISTER_IN, SLOT_CANISTER_OUT, slots)) {
            changed = true;
        }
        if (tanks[TANK_HEAVY].unloadTank(SLOT_HEAVY_IN, SLOT_HEAVY_OUT, slots)) {
            changed = true;
        }
        if (tanks[TANK_NAPHTHA].unloadTank(SLOT_NAPHTHA_IN, SLOT_NAPHTHA_OUT, slots)) {
            changed = true;
        }
        if (tanks[TANK_LIGHT].unloadTank(SLOT_LIGHT_IN, SLOT_LIGHT_OUT, slots)) {
            changed = true;
        }
        if (tanks[TANK_PETROLEUM].unloadTank(SLOT_PETROLEUM_IN, SLOT_PETROLEUM_OUT, slots)) {
            changed = true;
        }

        if (changed) {
            applySlotsArray(slots);
        }

        return changed;
    }

    private boolean refine() {
        RefineryRecipe recipe = RefineryRecipes.getRefinery(getTank(TANK_INPUT).getTankType());
        if (recipe == null) {
            isOn = false;
            return clearEmptyOutputTankTypes();
        }

        Fluid[] outFluids = new Fluid[4];
        int[] outAmounts = new int[4];
        for (int i = 0; i < 4; i++) {
            outFluids[i] = recipe.outputs[i].type();
            outAmounts[i] = recipe.outputs[i].fill();
        }

        boolean changed = prepareOutputTypes(outFluids);

        if (this.energy < ENERGY_PER_TICK || getTank(TANK_INPUT).getFill() < INPUT_CONSUMPTION_MB) {
            isOn = false;
            return changed;
        }

        if (!hasOutputSpace(outFluids, outAmounts)) {
            isOn = false;
            return changed;
        }

        isOn = true;

        getTank(TANK_INPUT).setFill(getTank(TANK_INPUT).getFill() - INPUT_CONSUMPTION_MB);

        for (int i = 0; i < 4; i++) {
            FluidTank out = getTank(i + 1);
            out.fillMb(outFluids[i], outAmounts[i]);
        }

        sulfurProgress++;
        if (sulfurProgress >= MAX_SULFUR) {
            sulfurProgress -= MAX_SULFUR;
            changed |= emitByproduct(recipe.solid);
        }

        // Original: im Betrieb SOOT_PER_SECOND * 5; die 70 gelten nur fuer die brennende Ruine.
        if (level.getGameTime() % 20 == 0) {
            PollutionHandler.incrementPollution(level, worldPosition, PollutionType.SOOT,
                    PollutionHandler.SOOT_PER_SECOND * 5);
        }
        this.energy -= ENERGY_PER_TICK;
        return true;
    }

    /** Original-Zweig {@code else if(onFire)}: Tanks brennen je 10 mB ab, Umgebung faengt Feuer. */
    private boolean burn() {
        boolean hasFuel = false;
        for (int i = 0; i < 5; i++) {
            if (tanks[i].getFill() > 0) {
                tanks[i].setFill(Math.max(tanks[i].getFill() - 10, 0));
                hasFuel = true;
            }
        }

        if (hasFuel) {
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            java.util.List<net.minecraft.world.entity.Entity> affected = level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
                    new net.minecraft.world.phys.AABB(x - 1.5, y, z - 1.5, x + 2.5, y + 8, z + 2.5));
            for (net.minecraft.world.entity.Entity e : affected) e.setSecondsOnFire(5);
            net.minecraft.util.RandomSource rand = level.random;
            com.hbm_m.util.ParticleUtil.spawnGasFlame(level, x + rand.nextDouble(), y + 1.5 + rand.nextDouble() * 3, z + rand.nextDouble(),
                    rand.nextGaussian() * 0.05, 0.1, rand.nextGaussian() * 0.05);

            if (level.getGameTime() % 20 == 0) {
                PollutionHandler.incrementPollution(level, worldPosition, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 70);
            }
        }
        return hasFuel;
    }

    private boolean clearEmptyOutputTankTypes() {
        boolean changed = false;
        for (int i = 1; i < 5; i++) {
            FluidTank out = getTank(i);
            if (out.getFill() <= 0 && !VanillaFluidEquivalence.sameSubstance(out.getTankType(), ModFluids.NONE.getSource())) {
                out.setTankType(ModFluids.NONE.getSource());
                changed = true;
            }
        }
        return changed;
    }

    private boolean hasOutputSpace(Fluid[] outputFluids, int[] amounts) {
        for (int i = 0; i < 4; i++) {
            FluidTank out = getTank(i + 1);
            if (out.getFill() > 0 && !VanillaFluidEquivalence.sameSubstance(out.getTankType(), outputFluids[i])) {
                return false;
            }
            if (out.getFill() + amounts[i] > out.getMaxFill()) {
                return false;
            }
        }
        return true;
    }

    private boolean prepareOutputTypes(Fluid[] outputFluids) {
        boolean changed = false;
        for (int i = 0; i < 4; i++) {
            FluidTank out = getTank(i + 1);
            if (out.getFill() <= 0 && !VanillaFluidEquivalence.sameSubstance(out.getTankType(), outputFluids[i])) {
                out.setTankType(outputFluids[i]);
                changed = true;
            }
        }
        return changed;
    }

    private boolean emitByproduct(ItemStack byproduct) {
        if (byproduct.isEmpty()) return false;

        ItemStack slotStack = inventory.getStackInSlot(SLOT_SULFUR_OUT);
        if (slotStack.isEmpty()) {
            inventory.setStackInSlot(SLOT_SULFUR_OUT, byproduct.copy());
            return true;
        }

        if (slotStack.is(byproduct.getItem())
            && slotStack.getCount() + byproduct.getCount() <= slotStack.getMaxStackSize()) {
            slotStack.grow(byproduct.getCount());
            inventory.setStackInSlot(SLOT_SULFUR_OUT, slotStack);
            return true;
        }

        return false;
    }

    private ItemStack[] getSlotsArray() {
        ItemStack[] slots = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            slots[i] = inventory.getStackInSlot(i);
        }
        return slots;
    }

    private void applySlotsArray(ItemStack[] slots) {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setStackInSlot(i, slots[i]);
        }
    }

    // Persisted through writeNbtData/readNbtData, NOT saveAdditional/load:
    // BaseHbmBlockEntity builds the CLIENT update tag from writeNbtData alone, so a
    // subclass overriding saveAdditional saves to disk correctly yet sends the client
    // nothing - which is why these readouts stayed blank in world.
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("hasExploded", hasExploded);
        tag.putBoolean("onFire", onFire);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].writeToNBT(tag, "tank_" + i);
        }
        tag.putInt("sulfurProgress", sulfurProgress);
        tag.putBoolean("isOn", isOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        hasExploded = tag.getBoolean("hasExploded");
        onFire = tag.getBoolean("onFire");
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(tag, "tank_" + i);
        }
        sulfurProgress = tag.getInt("sulfurProgress");
        isOn = tag.getBoolean("isOn");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.refinery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return MachineRefineryMenu.create(containerId, playerInventory, this);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> isEnergyProviderItem(stack);
            case SLOT_CANISTER_IN, SLOT_HEAVY_IN, SLOT_NAPHTHA_IN, SLOT_LIGHT_IN, SLOT_PETROLEUM_IN ->
                    FluidItemAccess.hasFluidHandler(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.refinery");
    }

    //? if forge {
    @Override
    protected void setupFluidCapability() {
        setFluidHandler(new RefineryFluidHandler(this));
    }
    //?}

    public boolean isOn() {
        return isOn;
    }

    public int getSulfurProgress() {
        return sulfurProgress;
    }

    // Энергопорты мультиблока: позиции фантомов структуры, ранее регистрировавшиеся блоком.
    // Ядро (worldPosition) подписывается в BaseMachineBlockEntity.ensureNetworkInitialized().
    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        if (level == null || level.isClientSide) return new BlockPos[0];
        if (!(getBlockState().getBlock() instanceof com.hbm_m.block.machines.MachineRefineryBlock block)) return new BlockPos[0];

        var helper = block.getStructureHelper();
        Direction facing = getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);

        java.util.List<BlockPos> ports = new java.util.ArrayList<>();
        for (BlockPos localPos : helper.getStructureMap().keySet()) {
            if (helper.resolvePartRole(localPos, block).canReceiveEnergy()) {
                ports.add(helper.getRotatedPos(worldPosition, localPos, facing));
            }
        }
        return ports.toArray(new BlockPos[0]);
    }
    /** Original {@code TileEntityMachineRefinery.writeNBT}: Tanks und Explosions-/Brandzustand. */
    @Override
    public void writeNBT(CompoundTag nbt) {
        boolean empty = !this.hasExploded;
        for (var tank : tanks) if (tank.getFill() > 0) empty = false;
        if (empty) return;
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "tank_" + i);
        nbt.putBoolean("hasExploded", hasExploded);
        nbt.putBoolean("onFire", onFire);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slot {11}; nichts hinein, Schwefel heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 11 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot == 11; }
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
