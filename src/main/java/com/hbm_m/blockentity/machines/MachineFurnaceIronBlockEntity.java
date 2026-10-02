package com.hbm_m.blockentity.machines;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineFurnaceIronBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.inventory.menu.MachineFurnaceIronMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.module.ModuleBurnTime;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityFurnaceIron}: Eisenofen, 2x2x2 ({@code getDimensions {1,0,1,0,1,0}}). Zwei Brennstoffplaetze,
 * Brennzeit nach {@link ModuleBurnTime} (Kohle/Braunkohle x1,25, Koks x1,5, Fest-/Raketen-/Balefirebrennstoff x2);
 * gebrannt wird nur, solange geschmolzen wird. Geschwindigkeitsupgrade: {@code 160 - 80 * Stufe / 3} Ticks. Russ in die
 * Umwelt, Knistern, Rauch und Flammen am Schornstein.
 */
public class MachineFurnaceIronBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements MenuProvider, com.hbm_m.interfaces.IUpgradeInfoProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL_1 = 1;
    public static final int SLOT_FUEL_2 = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_UPGRADE = 4;
    private static final int SLOT_COUNT = 5;

    public static final int baseTime = 160;

    private static final java.util.Map<UpgradeType, Integer> VALID_UPGRADES = java.util.Map.of(UpgradeType.SPEED, 3);

    public final com.hbm_m.inventory.UpgradeManager upgradeManager = new com.hbm_m.inventory.UpgradeManager();

    public static final ModuleBurnTime burnModule = new ModuleBurnTime()
            .setLigniteTimeMod(1.25)
            .setCoalTimeMod(1.25)
            .setCokeTimeMod(1.5)
            .setSolidTimeMod(2)
            .setRocketTimeMod(2)
            .setBalefireTimeMod(2);

    private final ModItemStackHandler inventory = new ModItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isItemValidForSlot(slot, stack);
        }
    };

    public int maxBurnTime;
    public int burnTime;
    public boolean wasOn = false;
    public int progress;
    public int processingTime;

    private static final int DATA_BURN = 0;
    private static final int DATA_MAX_BURN = 1;
    private static final int DATA_PROGRESS = 2;
    private static final int DATA_PROCESSING = 3;
    private static final int DATA_CAN_SMELT = 4;
    public static final int DATA_COUNT = 5;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_BURN -> burnTime;
                case DATA_MAX_BURN -> maxBurnTime;
                case DATA_PROGRESS -> progress;
                case DATA_PROCESSING -> processingTime;
                case DATA_CAN_SMELT -> level != null && canSmelt(level) ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_BURN -> burnTime = value;
                case DATA_MAX_BURN -> maxBurnTime = value;
                case DATA_PROGRESS -> progress = value;
                case DATA_PROCESSING -> processingTime = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MachineFurnaceIronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_IRON_BE.get(), pos, state);
        this.processingTime = baseTime;
    }

    public ModItemStackHandler getInventory() {
        return inventory;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFurnaceIronBlockEntity be) {
        if (!level.isClientSide()) be.serverTick(level, pos, state);
        else be.clientTick(level, pos, state);
    }

    private void serverTick(Level world, BlockPos pos, BlockState state) {

        upgradeManager.checkSlots(inventory, SLOT_UPGRADE, SLOT_UPGRADE, VALID_UPGRADES);
        this.processingTime = baseTime - ((baseTime / 2) * upgradeManager.getLevel(UpgradeType.SPEED) / 3);

        boolean prevOn = wasOn;
        wasOn = false;

        if (burnTime <= 0) {

            for (int i = 1; i < 3; i++) {
                ItemStack fuelStack = inventory.getStackInSlot(i);
                if (!fuelStack.isEmpty()) {

                    int fuel = burnModule.getBurnTime(fuelStack);

                    if (fuel > 0) {
                        this.maxBurnTime = this.burnTime = fuel;
                        ItemStack container = fuelStack.getCraftingRemainingItem();
                        fuelStack.shrink(1);

                        if (fuelStack.isEmpty()) {
                            inventory.setStackInSlot(i, container);
                        } else {
                            inventory.setStackInSlot(i, fuelStack);
                        }

                        break;
                    }
                }
            }
        }

        if (canSmelt(world)) {
            wasOn = true;
            this.progress++;
            this.burnTime--;

            if (this.progress % 15 == 0) {
                world.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.0F, 0.5F + world.random.nextFloat() * 0.5F);
            }

            if (this.progress >= this.processingTime) {
                ItemStack result = getRecipe(world, inventory.getStackInSlot(SLOT_INPUT)).get().getResultItem(world.registryAccess());

                ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
                if (out.isEmpty()) {
                    inventory.setStackInSlot(SLOT_OUTPUT, result.copy());
                } else {
                    out.grow(result.getCount());
                    inventory.setStackInSlot(SLOT_OUTPUT, out);
                }

                inventory.extractItem(SLOT_INPUT, 1, false);

                this.progress = 0;
                this.setChanged();
            }
            if (world.getGameTime() % 20 == 0) PollutionHandler.incrementPollution(world, pos, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND);
        } else {
            this.progress = 0;
        }

        if (prevOn != wasOn && state.hasProperty(MachineFurnaceIronBlock.LIT)) {
            world.setBlock(pos, state.setValue(MachineFurnaceIronBlock.LIT, wasOn), 3);
        }

        setChanged();
        world.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
    }

    private void clientTick(Level world, BlockPos pos, BlockState state) {

        if (this.progress > 0) {
            Direction dir = state.hasProperty(MachineFurnaceIronBlock.FACING) ? state.getValue(MachineFurnaceIronBlock.FACING) : Direction.NORTH;
            Direction rot = dir.getClockWise(); // Original: dir.getRotation(UP)

            double offset = this.progress % 2 == 0 ? 1 : 0.5;
            world.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 - dir.getStepX() * offset - rot.getStepX() * 0.1875, pos.getY() + 2,
                    pos.getZ() + 0.5 - dir.getStepZ() * offset - rot.getStepZ() * 0.1875, 0.0, 0.01, 0.0);

            if (this.progress % 5 == 0) {
                double rand = world.random.nextDouble();
                world.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5 + dir.getStepX() * 0.25 + rot.getStepX() * rand, pos.getY() + 0.25 + world.random.nextDouble() * 0.25,
                        pos.getZ() + 0.5 + dir.getStepZ() * 0.25 + rot.getStepZ() * rand, 0.0, 0.0, 0.0);
            }
        }
    }

    public boolean canSmelt(Level level) {

        if (this.burnTime <= 0) return false;
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return false;

        var recipe = getRecipe(level, input);
        if (recipe.isEmpty()) return false;
        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        if (result.isEmpty()) return false;

        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) return true;

        if (!ItemStack.isSameItemSameTags(result, out)) return false;
        if (result.getCount() + out.getCount() > out.getMaxStackSize()) return false;

        return true;
    }

    private java.util.Optional<SmeltingRecipe> getRecipe(Level level, ItemStack input) {
        return com.hbm_m.platform.recipe.RecipeHooks.getRecipeFor(level, RecipeType.SMELTING, input);
    }

    /** Original: Slot 0 nimmt Schmelzbares, 1-2 Brennstoff, sonst nichts. */
    public boolean isItemValidForSlot(int i, ItemStack itemStack) {

        if (i == 0)
            return level != null && getRecipe(level, itemStack).isPresent();

        if (i < 3)
            return burnModule.getBurnTime(itemStack) > 0;

        return i == SLOT_UPGRADE; // Menue-Slot (SlotUpgrade)
    }

    /** Fuer die Brennstoffslots des Menues. */
    public static boolean isFuel(ItemStack stack) {
        return com.hbm_m.handler.FuelHandler.getBurnTimeFromCache(stack) > 0;
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, container);
    }

    // ==================== Upgrades ====================

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(com.hbm_m.interfaces.IUpgradeInfoProvider.getStandardLabel(getBlockState().getBlock()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 50 / 3) + "%").withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public java.util.Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(inventory, registries));
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putInt("burnTime", burnTime);
        tag.putInt("progress", progress);
        tag.putInt("processingTime", processingTime);
        tag.putBoolean("wasOn", wasOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        com.hbm_m.platform.ItemStackSerialization.deserialize(inventory, tag.getCompound("inventory"), registries);
        maxBurnTime = tag.getInt("maxBurnTime");
        burnTime = tag.getInt("burnTime");
        progress = tag.getInt("progress");
        processingTime = tag.contains("processingTime") ? tag.getInt("processingTime") : baseTime;
        wasOn = tag.getBoolean("wasOn");
    }

    // ==================== GUI ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.hbm_m.furnace_iron");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineFurnaceIronMenu(id, inv, this, data);
    }
}
