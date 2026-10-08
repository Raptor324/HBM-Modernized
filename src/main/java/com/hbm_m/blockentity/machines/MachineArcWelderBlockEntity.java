package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineArcWelderMenu;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.ArcWelderRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MachineArcWelderBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2, com.hbm_m.interfaces.IConditionalInvAccess {

    /** Original {@code setInventorySlotContents}: Aufwertung einstecken macht das Steckgeraeusch. */
    @Override
    protected com.hbm_m.platform.ModItemStackHandler createInventoryHandler(int size) {
        return new com.hbm_m.platform.ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (isCriticalSlot(slot)) sendUpdateToClient();
                net.minecraft.world.item.ItemStack stack = getStackInSlot(slot);
                if (level != null && !level.isClientSide && slot >= 6 && slot <= 7
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


    // ─── Slot map ─────────────────────────────────────────────────────────────
    public static final int SLOTS      = 8;
    public static final int SLOT_IN1   = 0, SLOT_IN2 = 1, SLOT_IN3 = 2;
    public static final int SLOT_OUT   = 3;
    public static final int SLOT_BAT   = 4;
    public static final int SLOT_FLUID = 5;   // Fluid-ID item (identifies accepted fluid)
    public static final int SLOT_UPG1  = 6, SLOT_UPG2 = 7;

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
    public int  processTime = 1;        // Original-Startwert; wird vom Rezept gesetzt
    public long consumption = CONSUMPTION;

    // ─── Fluid tank ───────────────────────────────────────────────────────────
    /** Welding fluid (e.g. water, coolant). Capacity 24,000 mb as in the original. */
    public final FluidTank tank = new FluidTank(24_000);

    // ─── Constructor ──────────────────────────────────────────────────────────

    public MachineArcWelderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARC_WELDER_BE.get(), pos, state, SLOTS, MAX_ENERGY, RECEIVE_RATE);
    }

    /** Exposes the machine inventory for SlotItemHandler in the menu. */
    public com.hbm_m.platform.ModItemStackHandler getItemHandler() { return inventory; }

    // ─── Tick ─────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, MachineArcWelderBlockEntity be) {
        if (level.isClientSide) return;

        // Original: Strom- und Fluessigkeitsanschluesse rund um den Sockel
        be.ensureNetworkInitialized();
        if (level.getGameTime() % 20 == 0) be.subscribeFluid(level);

        // Charge from battery slot (extract energy from battery item in SLOT_BAT)
        com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(
                be.inventory.getStackInSlot(SLOT_BAT)).ifPresent(provider -> {
            long want = be.capacity - be.energy;
            long got  = provider.extractEnergy(want, false);
            if (got > 0) be.energy += got;
        });

        // Original: tank.setType(5, slots) - Fluid-ID im Slot 5 legt die Sorte fest
        ItemStack[] slotArr = new ItemStack[SLOTS];
        for (int i = 0; i < SLOTS; i++) slotArr[i] = be.inventory.getStackInSlot(i);
        be.tank.setType(SLOT_FLUID, slotArr);

        // Data-driven поиск рецепта: итерируем ArcWelderRecipe из RecipeManager (заменяет статику ArcWelderRecipes).
        ArcWelderRecipe recipe = findArcWelderRecipe(level, be);
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
            be.progress = 0;
            be.consumption = CONSUMPTION;
            intendedMaxPower = MAX_ENERGY;
        }
        // Original: maxPower = max(intendedMaxPower, power)
        be.setEnergyCapacity(Math.max(intendedMaxPower, be.energy));

        // matchesFluid на ArcWelderRecipe заменяет прежний recipe.fluid.satisfiedBy(tank) (теперь FluidStack-based).
        boolean hasRecipe  = recipe != null && recipe.matchesFluid(be.tank);
        boolean canProcess = hasRecipe && be.energy >= be.consumption
                && be.canOutput(recipe.getOutput());

        if (canProcess) {
            be.progress += (1 + blackLevel);
            be.energy = Math.max(0, be.energy - be.consumption);

            // 1:1-Port: alle zwei Ticks ein Schwall Funken ueber der Schweissstelle, jeden
            // zwanzigsten davon als voller Lichtbogen.
            if (level.getGameTime() % 2 == 0 && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                net.minecraft.core.Direction dir = state.hasProperty(
                        com.hbm_m.block.machines.MachineArcWelderBlock.FACING)
                        ? state.getValue(com.hbm_m.block.machines.MachineArcWelderBlock.FACING)
                        : net.minecraft.core.Direction.NORTH;

                net.minecraft.nbt.CompoundTag dPart = new net.minecraft.nbt.CompoundTag();
                dPart.putString("type", level.getGameTime() % 20 == 0 ? "tau" : "hadron");
                dPart.putByte("count", (byte) 5);

                com.hbm_m.particle.helper.IParticleCreator.sendPacket(serverLevel,
                        pos.getX() + 0.5 - dir.getStepX() * 0.5,
                        pos.getY() + 1.25,
                        pos.getZ() + 0.5 - dir.getStepZ() * 0.5,
                        25, dPart);
            }

            if (be.progress >= be.processTime) {
                be.progress = 0;
                be.processRecipe(recipe);
                be.setChanged();
            }
        } else {
            // Original: progress = 0, sobald das Rezept nicht laufen kann
            be.progress = 0;
        }

        level.sendBlockUpdated(pos, state, state, 3);
    }

    /** Verbraucht die passenden Eingangs-Slots + ggf. Fluid und legt das Ergebnis in SLOT_OUT ab. */
    private void processRecipe(ArcWelderRecipe recipe) {
        int[] inputSlots = { SLOT_IN1, SLOT_IN2, SLOT_IN3 };
        // Поглощение зеркалит matchesInputs: каждый требуемый ингредиент снимается со своего слота.
        boolean[] consumed = new boolean[recipe.getInputs().length];

        for (int slot : inputSlots) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            for (int i = 0; i < recipe.getInputs().length; i++) {
                if (consumed[i]) continue;
                if (recipe.getInputs()[i].test(stack) && stack.getCount() >= recipe.getInputCount(i)) {
                    inventory.extractItem(slot, recipe.getInputCount(i), false);
                    consumed[i] = true;
                    break;
                }
            }
        }

        recipe.consumeFluid(tank);

        ItemStack result = recipe.getOutput();
        ItemStack existing = inventory.getStackInSlot(SLOT_OUT);
        if (existing.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUT, result);
        } else if (com.hbm_m.platform.PlatformHooks.isSameItemSameTags(existing, result)) {
            existing.grow(result.getCount());
        }
    }

    public boolean canOutput() {
        return canOutput(null);
    }

    /** Empty output slot is always fine; a filled one must match the pending recipe's result and have room to grow. */
    private boolean canOutput(ItemStack result) {
        ItemStack out = inventory.getStackInSlot(SLOT_OUT);
        if (out.isEmpty()) return true;
        if (result == null) return false;
        return com.hbm_m.platform.PlatformHooks.isSameItemSameTags(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    /** Data-driven поиск ArcWelderRecipe по 3 входным слотам (заменяет статический ArcWelderRecipes.getRecipe). */
    @org.jetbrains.annotations.Nullable
    private static ArcWelderRecipe findArcWelderRecipe(Level level, MachineArcWelderBlockEntity be) {
        ItemStack s0 = be.inventory.getStackInSlot(SLOT_IN1);
        ItemStack s1 = be.inventory.getStackInSlot(SLOT_IN2);
        ItemStack s2 = be.inventory.getStackInSlot(SLOT_IN3);
        for (ArcWelderRecipe recipe : RecipeHooks.getAllRecipes(level, ArcWelderRecipe.Type.INSTANCE)) {
            if (recipe.matchesInputs(s0, s1, s2)) return recipe;
        }
        return null;
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
        return slot != SLOT_OUT; // output is extraction-only
    }

    // ─── MenuProvider ─────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() { return Component.translatable("block.hbm_m.arc_welder"); }
    @Override
    public Component getDisplayName()    { return getDefaultName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineArcWelderMenu.create(id, inv, this);
    }

    // ─── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        if (!display.isEmpty()) tag.put("display", com.hbm_m.platform.PlatformHooks.saveItemStack(display, new CompoundTag(), registries));
        tag.putInt("progress",    progress);
        tag.putInt("processTime", processTime);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        display = tag.contains("display") ? com.hbm_m.platform.PlatformHooks.itemStackOf(tag.getCompound("display"), registries) : ItemStack.EMPTY;
        progress    = tag.getInt("progress");
        processTime = tag.getInt("processTime");
        if (processTime <= 0) processTime = 1;
        tank.readFromNBT(tag, "tank");
    }

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

    // ─── Original IConditionalInvAccess: rote/gelbe/gruene Anschlusszellen am Sockel ────────────────────

    //? if forge {
    /** Original: rot Slot 0, gelb Slot 1, gruen Slot 2 (je plus Ausgang 3); alle anderen Zellen ohne Zugriff. */
    @Override
    public net.minecraftforge.items.IItemHandler getConditionalItemHandler(net.minecraft.core.BlockPos part, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        net.minecraft.world.level.block.state.BlockState state = getBlockState();
        net.minecraft.core.Direction dir = state.hasProperty(com.hbm_m.block.machines.MachineArcWelderBlock.FACING)
                ? state.getValue(com.hbm_m.block.machines.MachineArcWelderBlock.FACING) : net.minecraft.core.Direction.NORTH;
        net.minecraft.core.Direction rot = dir.getClockWise(); // ForgeDirection.getRotation(UP)
        net.minecraft.core.BlockPos c = worldPosition;

        // Rot
        if (part.equals(c.relative(rot)) || part.equals(c.relative(rot.getOpposite()).relative(dir.getOpposite())))
            return itemAccess(new int[] {0, 3});
        // Gelb
        if (part.equals(c.relative(dir.getOpposite())))
            return itemAccess(new int[] {1, 3});
        // Gruen
        if (part.equals(c.relative(rot.getOpposite())) || part.equals(c.relative(rot).relative(dir.getOpposite())))
            return itemAccess(new int[] {2, 3});

        return itemAccess(new int[] { });
    }

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) {
            net.minecraftforge.items.IItemHandler h = itemAccess(new int[] {1, 3});
            return net.minecraftforge.common.util.LazyOptional.of(() -> h).cast();
        }
        return super.getCapability(cap, side);
    }

    private net.minecraftforge.items.IItemHandler itemAccess(int[] slots) {
        return com.hbm_m.blockentity.SidedItemAccess.fixed(() -> inventory, slots, (slot, stack) -> slot < 3, (slot, stack) -> slot == SLOT_OUT);
    }
    //?} elif neoforge {
    /*/^* Original: rot Slot 0, gelb Slot 1, gruen Slot 2 (je plus Ausgang 3); alle anderen Zellen ohne Zugriff. ^/
    @Override
    public net.neoforged.neoforge.items.IItemHandler getConditionalItemHandler(net.minecraft.core.BlockPos part, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        net.minecraft.world.level.block.state.BlockState state = getBlockState();
        net.minecraft.core.Direction dir = state.hasProperty(com.hbm_m.block.machines.MachineArcWelderBlock.FACING)
                ? state.getValue(com.hbm_m.block.machines.MachineArcWelderBlock.FACING) : net.minecraft.core.Direction.NORTH;
        net.minecraft.core.Direction rot = dir.getClockWise(); // ForgeDirection.getRotation(UP)
        net.minecraft.core.BlockPos c = worldPosition;

        // Rot
        if (part.equals(c.relative(rot)) || part.equals(c.relative(rot.getOpposite()).relative(dir.getOpposite())))
            return itemAccess(new int[] {0, 3});
        // Gelb
        if (part.equals(c.relative(dir.getOpposite())))
            return itemAccess(new int[] {1, 3});
        // Gruen
        if (part.equals(c.relative(rot.getOpposite())) || part.equals(c.relative(rot).relative(dir.getOpposite())))
            return itemAccess(new int[] {2, 3});

        return itemAccess(new int[] { });
    }

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) {
            net.neoforged.neoforge.items.IItemHandler h = itemAccess(new int[] {1, 3});
            return com.hbm_m.platform.LazyCap.of(() -> h).cast();
        }
        return super.getHbmCapability(cap, side);
    }

    private net.neoforged.neoforge.items.IItemHandler itemAccess(int[] slots) {
        return com.hbm_m.blockentity.SidedItemAccess.fixed(() -> inventory, slots, (slot, stack) -> slot < 3, (slot, stack) -> slot == SLOT_OUT);
    }
    *///?}
}
