package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineSolderingStationMenu;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.SolderingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MachineSolderingStationBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2 {

    /** Original {@code setInventorySlotContents}: Aufwertung einstecken macht das Steckgeraeusch. */
    @Override
    protected com.hbm_m.platform.ModItemStackHandler createInventoryHandler(int size) {
        return new com.hbm_m.platform.ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (isCriticalSlot(slot)) sendUpdateToClient();
                net.minecraft.world.item.ItemStack stack = getStackInSlot(slot);
                if (level != null && !level.isClientSide && slot >= 9 && slot <= 10
                        && stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade) {
                    level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            com.hbm_m.sound.HbmSoundsNT.get("hbm:item.upgradePlug"), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }

            @Override
            public boolean isItemValid(int slot, @org.jetbrains.annotations.NotNull net.minecraft.world.item.ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }
        };
    }


    // ─── Slot map (11 total) ──────────────────────────────────────────────────
    public static final int SLOTS      = 11;
    public static final int SLOT_TOP0  = 0, SLOT_TOP1 = 1, SLOT_TOP2 = 2; // toppings
    public static final int SLOT_PCB0  = 3, SLOT_PCB1 = 4;                // PCB (2 slots)
    public static final int SLOT_SOLDER = 5;                               // solder (1 slot)
    public static final int SLOT_OUT   = 6;  // output
    public static final int SLOT_BAT   = 7;  // battery
    public static final int SLOT_FLUID = 8;  // fluid-ID item
    public static final int SLOT_UPG1  = 9, SLOT_UPG2 = 10; // upgrades

    // ─── Energy constants ─────────────────────────────────────────────────────
    /** Original: {@code maxPower = 2_000}, ohne Rezept {@code consumption = 100}; mit Rezept {@code maxPower = consumption * 20}. */
    public static final long MAX_ENERGY  = 2_000L;
    public static final long CONSUMPTION =   100L;
    /** Original nimmt beliebig viel an (bis maxPower). */
    private static final long RECEIVE_RATE = 1_000_000_000_000L;

    private static final java.util.Map<com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType, Integer> VALID_UPGRADES = java.util.Map.of(
            com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.SPEED, 3,
            com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.POWER, 3,
            com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.OVERDRIVE, 3);
    private final com.hbm_m.inventory.UpgradeManager upgradeManager = new com.hbm_m.inventory.UpgradeManager();

    // ─── Processing state ─────────────────────────────────────────────────────
    /** Original {@code display}: Ergebnis des passenden Rezepts, nur zur Darstellung synchronisiert. */
    public ItemStack display = ItemStack.EMPTY;
    public int  progress    = 0;
    public int  processTime = 1;
    public long consumption = CONSUMPTION;

    // ─── Fluid tank (solder flux / coolant) ──────────────────────────────────
    /** Welding fluid (8,000 mb as in the original). */
    public final FluidTank tank = new FluidTank(8_000);

    // ─── Feature flags ────────────────────────────────────────────────────────
    /**
     * When true, prevents processing no-fluid recipes while a fluid is present
     * in the tank (avoids accidental dry-welding when flux is loaded).
     */
    public boolean collisionPrevention = false;

    // ─── Constructor ──────────────────────────────────────────────────────────

    public MachineSolderingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLDERING_STATION_BE.get(), pos, state, SLOTS, MAX_ENERGY, RECEIVE_RATE);
    }

    /** Exposes the machine inventory for SlotItemHandler in the menu. */
    public com.hbm_m.platform.ModItemStackHandler getItemHandler() { return inventory; }

    // ─── Collision Prevention toggle ──────────────────────────────────────────

    /** Called server-side by the GUI toggle packet. */
    public void toggleCollisionPrevention() {
        collisionPrevention = !collisionPrevention;
        setChanged();
        if (level != null)
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    // ─── Tick ─────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSolderingStationBlockEntity be) {
        if (level.isClientSide) return;

        // Original: Strom- und Fluessigkeitsanschluesse rund um den Sockel
        be.ensureNetworkInitialized();
        if (level.getGameTime() % 20 == 0) be.subscribeFluid(level);

        // Charge from battery slot
        com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(
                be.inventory.getStackInSlot(SLOT_BAT)).ifPresent(provider -> {
            long want = be.capacity - be.energy;
            long got  = provider.extractEnergy(want, false);
            if (got > 0) be.energy += got;
        });

        // Data-driven поиск рецепта: итерируем SolderingRecipe из RecipeManager (заменяет статику SolderingRecipes).
        // Original: tank.setType(8, slots)
        ItemStack[] slotArr = new ItemStack[SLOTS];
        for (int i = 0; i < SLOTS; i++) slotArr[i] = be.inventory.getStackInSlot(i);
        be.tank.setType(SLOT_FLUID, slotArr);

        SolderingRecipe recipe = findSolderingRecipe(level, be);
        // Original serialize: das Rezeptergebnis geht als display an den Client (Renderer zeigt es auf dem Tisch)
        be.display = recipe != null ? recipe.getOutput() : ItemStack.EMPTY;
        long intendedMaxPower;

        // 1:1 Original: Tempo (rot), Sparsamkeit (blau), Overdrive (schwarz)
        be.upgradeManager.checkSlots(be.inventory, SLOT_UPG1, SLOT_UPG2, VALID_UPGRADES);
        int redLevel = be.upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.SPEED);
        int blueLevel = be.upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.POWER);
        int blackLevel = be.upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.OVERDRIVE);

        if (recipe != null) {
            int duration = recipe.getDuration();
            long cons = recipe.getConsumption();
            be.processTime  = duration - (duration * redLevel / 6) + (duration * blueLevel / 3);
            be.consumption  = cons + (cons * redLevel) - (cons * blueLevel / 6);
            be.consumption *= (long) Math.pow(2, blackLevel);
            intendedMaxPower = be.consumption * 20;
        } else {
            be.consumption = CONSUMPTION;
            intendedMaxPower = MAX_ENERGY;
        }
        be.setEnergyCapacity(Math.max(intendedMaxPower, be.energy));

        // matchesFluid на SolderingRecipe заменяет прежний recipe.fluid.satisfiedBy(tank) (FluidStack-based).
        boolean hasRecipe  = recipe != null && recipe.matchesFluid(be.tank);
        boolean canProcess = hasRecipe && be.canProcess(recipe);

        if (canProcess) {
            be.progress += (1 + blackLevel);

            // Original: alle 20 Ticks drei Tau-Funken ueber der Loetstelle
            if (level.getGameTime() % 20 == 0 && level instanceof net.minecraft.server.level.ServerLevel serverLevel
                    && state.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
                net.minecraft.core.Direction dir = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
                net.minecraft.core.Direction rot = dir.getClockWise();
                CompoundTag dPart = new CompoundTag();
                dPart.putString("type", "tau");
                dPart.putByte("count", (byte) 3);
                com.hbm_m.particle.helper.IParticleCreator.sendPacket(serverLevel,
                        pos.getX() + 0.5 - dir.getStepX() * 0.5 + rot.getStepX() * 0.5, pos.getY() + 1.125,
                        pos.getZ() + 0.5 - dir.getStepZ() * 0.5 + rot.getStepZ() * 0.5, 25, dPart);
            }
            be.energy = Math.max(0, be.energy - be.consumption);
            if (be.progress >= be.processTime) {
                be.progress = 0;
                be.consumeItems(recipe);
                ItemStack out = be.inventory.getStackInSlot(SLOT_OUT);
                ItemStack recipeOut = recipe.getOutput();
                if (out.isEmpty()) {
                    be.inventory.setStackInSlot(SLOT_OUT, recipeOut.copy());
                } else {
                    out.grow(recipeOut.getCount());
                }
                recipe.consumeFluid(be.tank); // no-op, если требования по жидкости нет
                be.setChanged();
            }
        } else {
            be.progress = 0;
        }

        level.sendBlockUpdated(pos, state, state, 3);
    }

    private void consumeItems(SolderingRecipe recipe) {
        // Поглощаем по одной группе за раз: toppings (3 слота), pcb (2), solder (1).
        consumeGroup(recipe.getToppings(), recipe.getToppingCounts(), SLOT_TOP0, SLOT_TOP2 + 1);
        consumeGroup(recipe.getPcb(),      recipe.getPcbCounts(),      SLOT_PCB0, SLOT_PCB1 + 1);
        consumeGroup(recipe.getSolder(),   recipe.getSolderCounts(),   SLOT_SOLDER, SLOT_SOLDER + 1);
    }

    private void consumeGroup(Ingredient[] required, int[] counts, int from, int to) {
        for (int g = 0; g < required.length; g++) {
            Ingredient ing = required[g];
            int need = counts[g];
            // Перебираем слоты группы, ищем стак, проходящий ingredient.test() с нужным countом.
            for (int i = from; i < to; i++) {
                ItemStack s = inventory.getStackInSlot(i);
                if (ing.test(s) && s.getCount() >= need) { s.shrink(need); break; }
            }
        }
    }

    /** Data-driven поиск SolderingRecipe по 6 слотам машины (заменяет статический SolderingRecipes.getRecipe). */
    @org.jetbrains.annotations.Nullable
    private static SolderingRecipe findSolderingRecipe(Level level, MachineSolderingStationBlockEntity be) {
        for (SolderingRecipe recipe : RecipeHooks.getAllRecipes(level, SolderingRecipe.Type.INSTANCE)) {
            if (recipe.matches(be.inventory.getStackInSlot(SLOT_TOP0), be.inventory.getStackInSlot(SLOT_TOP1), be.inventory.getStackInSlot(SLOT_TOP2),
                              be.inventory.getStackInSlot(SLOT_PCB0), be.inventory.getStackInSlot(SLOT_PCB1),
                              be.inventory.getStackInSlot(SLOT_SOLDER))) {
                return recipe;
            }
        }
        return null;
    }

    /** 1:1 Original {@code canProcess(recipe)}. */
    public boolean canProcess(SolderingRecipe recipe) {
        if (energy < consumption) return false;
        // Original: Kollisionsschutz nur fuer Rezepte ohne Fluessigkeit
        if (collisionPrevention && recipe.getFluid() == null && tank.getFill() > 0) return false;
        ItemStack out = inventory.getStackInSlot(SLOT_OUT);
        if (!out.isEmpty()) {
            ItemStack result = recipe.getOutput();
            if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(out, result)) return false;
            if (out.getCount() + result.getCount() > out.getMaxStackSize()) return false;
        }
        return true;
    }

    // ─── Progress helpers (for GUI) ───────────────────────────────────────────

    public int getProgressScaled(int scale) {
        return processTime > 0 ? progress * scale / processTime : 0;
    }

    public int getProgress()    { return progress;    }
    public int getMaxProgress() { return processTime; }

    // ─── Slot validation ──────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_OUT) return false;
        Level level = getLevel();
        if (level == null) return true; // пока нет level — позволяем всё (поведение при отсутствии рецептов).
        Ingredient[] group;
        if (slot < 3) {
            group = collectGroup(level, SolderingRecipe::getToppings);
        } else if (slot < SLOT_SOLDER) {
            group = collectGroup(level, SolderingRecipe::getPcb);
        } else if (slot == SLOT_SOLDER) {
            group = collectGroup(level, SolderingRecipe::getSolder);
        } else {
            return true;
        }
        if (group.length == 0) return true; // нет рецептов на эту группу — позволяем всё (как в оригинале).
        for (Ingredient ing : group) {
            if (ing.test(stack)) return true;
        }
        return false;
    }

    /** Собирает все ingredient-ы заданной группы по всем data-driven рецептам (заменяет SolderingRecipes.toppings/pcb/solder). */
    private static Ingredient[] collectGroup(Level level, java.util.function.Function<SolderingRecipe, Ingredient[]> pick) {
        java.util.List<Ingredient> out = new java.util.ArrayList<>();
        for (SolderingRecipe recipe : RecipeHooks.getAllRecipes(level, SolderingRecipe.Type.INSTANCE)) {
            for (Ingredient ing : pick.apply(recipe)) out.add(ing);
        }
        return out.toArray(new Ingredient[0]);
    }

    // ─── MenuProvider ─────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() { return Component.translatable("block.hbm_m.soldering_station"); }
    @Override
    public Component getDisplayName()    { return getDefaultName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineSolderingStationMenu.create(id, inv, this);
    }

    // ─── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        if (!display.isEmpty()) tag.put("display", com.hbm_m.platform.PlatformHooks.saveItemStack(display, new CompoundTag(), registries));
        tag.putInt("progress",    progress);
        tag.putInt("processTime", processTime);
        tag.putBoolean("collision", collisionPrevention);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        display = tag.contains("display") ? com.hbm_m.platform.PlatformHooks.itemStackOf(tag.getCompound("display"), registries) : ItemStack.EMPTY;
        progress            = tag.getInt("progress");
        processTime         = tag.getInt("processTime");
        if (processTime <= 0) processTime = 1;
        collisionPrevention = tag.getBoolean("collision");
        tank.readFromNBT(tag, "tank");
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {0-6}; Bauteile nach isItemValidForSlot hinein, nur das Ergebnis heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0, 1, 2, 3, 4, 5, 6 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot < 6 && isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot == 6; }
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
    //?} elif neoforge {
    /*/^* Original {@code ISidedInventory}: Slots {0-6}; Bauteile nach isItemValidForSlot hinein, nur das Ergebnis heraus. ^/
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0, 1, 2, 3, 4, 5, 6 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot < 6 && isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot == 6; }
            });

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sidedItems.invalidate();
    }
    *///?}

    // ─── Anschluesse (Original getConPos: Strom und Fluessigkeit rund um den Sockel) ───────────────────────

    private net.minecraft.core.BlockPos[] structureCells = null;

    /** Alle Zellen der unteren Lage; Kabel und Rohre an jeder Seite davon zaehlen als Anschluss. */
    private net.minecraft.core.BlockPos[] baseCells() {
        if (structureCells != null) return structureCells;
        java.util.List<net.minecraft.core.BlockPos> cells = new java.util.ArrayList<>();
        cells.add(worldPosition);
        if (getBlockState().getBlock() instanceof com.hbm_m.interfaces.IMultiblockController ctrl
                && getBlockState().hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
            net.minecraft.core.Direction facing = getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
            var helper = ctrl.getStructureHelper();
            for (net.minecraft.core.BlockPos local : helper.getStructureMap().keySet()) {
                if (local.getY() == 0) cells.add(helper.getRotatedPos(worldPosition, local, facing));
            }
        }
        structureCells = cells.toArray(new net.minecraft.core.BlockPos[0]);
        return structureCells;
    }

    @Override
    protected net.minecraft.core.BlockPos[] getExtraEnergyPorts() {
        if (level == null || level.isClientSide) return new net.minecraft.core.BlockPos[0];
        return baseCells();
    }

    /** Original: {@code trySubscribe(tank.getTankType(), ...)} an allen Anschluessen, alle 20 Ticks. */
    private void subscribeFluid(Level level) {
        if (tank.getTankType() == com.hbm_m.inventory.fluid.ModFluids.NONE.getSource()
                || tank.getTankType() == net.minecraft.world.level.material.Fluids.EMPTY) return;
        java.util.Set<net.minecraft.core.BlockPos> cells = new java.util.HashSet<>(java.util.Arrays.asList(baseCells()));
        for (net.minecraft.core.BlockPos cell : cells) {
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                net.minecraft.core.BlockPos n = cell.relative(dir);
                if (!cells.contains(n)) trySubscribe(tank.getTankType(), level, n, dir);
            }
        }
    }

    @Override public com.hbm_m.inventory.fluid.tank.FluidTank[] getAllTanks() { return new com.hbm_m.inventory.fluid.tank.FluidTank[] { tank }; }
    @Override public com.hbm_m.inventory.fluid.tank.FluidTank[] getReceivingTanks() { return new com.hbm_m.inventory.fluid.tank.FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }
}
