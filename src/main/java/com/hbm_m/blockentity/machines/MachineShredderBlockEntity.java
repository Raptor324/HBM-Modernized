package com.hbm_m.blockentity.machines;

import com.hbm_m.platform.PlatformHooks;


import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineShredderMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemBlades;
import com.hbm_m.recipe.ShredderRecipe;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;
import com.hbm_m.sound.ClientSoundBootstrap;

import dev.architectury.registry.registries.RegistrySupplier;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MachineShredderBlockEntity extends BaseMachineBlockEntity {

    private static final String SHREDDER_SOUND_INSTANCE = "com.hbm_m.sound.ShredderSoundInstance";

    private static final int BATTERY_SLOT = 29;
    private static final int TOTAL_SLOTS = 30;

    private static final int INPUT_START = 0;
    private static final int INPUT_END = 8;
    private static final int OUTPUT_START = 9;
    private static final int OUTPUT_END = 26;
    private static final int BLADE_LEFT = 27;
    private static final int BLADE_RIGHT = 28;

    public static final long MAX_POWER = 10_000L;
    private static final long MAX_RECEIVE = 1_000L;
    public static final int PROCESSING_SPEED = 60;
    private static final long ENERGY_PER_TICK = 5L; 

    private int progress = 0;
    private boolean isActive = false;
    private boolean clientIsActive = false;
    private int syncCounter = 0;
    private static final int SYNC_INTERVAL = 20;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> PROCESSING_SPEED;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // read-only
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public MachineShredderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHREDDER.get(), pos, state, TOTAL_SLOTS, MAX_POWER, MAX_RECEIVE);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.shredder");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= INPUT_START && slot <= INPUT_END) {
            return !(stack.getItem() instanceof ItemBlades);
        }
        if (slot >= OUTPUT_START && slot <= OUTPUT_END) {
            return false;
        }
        if (slot == BLADE_LEFT || slot == BLADE_RIGHT) {
            return stack.getItem() instanceof ItemBlades;
        }
        if (slot == BATTERY_SLOT) {
            return isEnergyProviderItem(stack);
        }
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineShredderMenu(containerId, playerInventory, this, containerData);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("progress", progress);
        tag.putBoolean("active", isActive);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        progress = tag.getInt("progress");
        if (tag.contains("active")) {
            isActive = tag.getBoolean("active");
            clientIsActive = isActive;
        } else {
            isActive = false;
            clientIsActive = false;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            ClientSoundBootstrap.stopSound(level, worldPosition);
        }
    }

    public void drops() {
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, container);
    }

    // ==================== TICK LOGIC ====================

    public static void tick(Level level, BlockPos pos, BlockState state, MachineShredderBlockEntity blockEntity) {
        if (level.isClientSide()) {
            blockEntity.clientTick();
        } else {
            blockEntity.serverTick(level, pos);
        }
    }
//? if forge || neoforge {
@OnlyIn(Dist.CLIENT)
//?}
    private void clientTick() {
        ClientSoundBootstrap.updateSound(this, getIsActive(), () -> newShredderSoundInstance());
    }

    private Object newShredderSoundInstance() {
        try {
            return Class.forName(SHREDDER_SOUND_INSTANCE).getConstructor(BlockPos.class).newInstance(this.getBlockPos());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
    
    public boolean getIsActive() {
        if (level != null && level.isClientSide) {
            return clientIsActive;
        }
        return isActive;
    }    

    private void serverTick(Level level, BlockPos pos) {
        ensureNetworkInitialized();

        boolean dirty = false;

        chargeFromBattery();

        if (level.getGameTime() % 10L == 0L) {
            updateEnergyDelta(this.getEnergyStored());
        }

        boolean canProcess = canProcess();
        boolean wasActive = isActive;

        boolean canWork = false;
        if (canProcess) {
            long currentEnergy = this.getEnergyStored();
            if (currentEnergy >= ENERGY_PER_TICK) {
                this.setEnergyStored(currentEnergy - ENERGY_PER_TICK);
                canWork = true;
                dirty = true;
            }
        }

        if (canWork) {
            progress++;
            if (progress >= PROCESSING_SPEED) {
                for (int i = BLADE_LEFT; i <= BLADE_RIGHT; i++) {
                    ItemStack blade = inventory.getStackInSlot(i);
                    if (!blade.isEmpty() && blade.getItem() instanceof ItemBlades bladeItem) {
                        int maxDamage = bladeItem.getMaxDamage(blade);
                        if (maxDamage > 0) {
                            int oldDamage = blade.getDamageValue();
                            int newDamage = Math.min(maxDamage, oldDamage + 1);
                            if (newDamage != oldDamage) {
                                blade.setDamageValue(newDamage);
                                dirty = true;
                                if (oldDamage < maxDamage && newDamage >= maxDamage) {
                                    level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
                                }
                            }
                        }
                    }
                }
                progress = 0;
                processItem();
                dirty = true;
            } else {
                dirty = true;
            }
        } else {
            if (progress > 0) {
                progress = 0;
                dirty = true;
            }
        }

        isActive = canWork;
        if (wasActive != isActive) {
            dirty = true;
        }
        
        if (isActive || wasActive) {
            dirty = true;
        }

        syncCounter++;
        if (syncCounter >= SYNC_INTERVAL) {
            syncCounter = 0;
            dirty = true;
        }

        if (dirty) {
            setChanged();
            sendUpdateToClient();
        }
    }

    // ==================== ENERGY ====================

    private void chargeFromBattery() {
        chargeFromBatterySlot(BATTERY_SLOT);
    }

    public boolean hasPower() {
        return this.getEnergyStored() > 0;
    }

    public long getPower() {
        return this.getEnergyStored();
    }

    public long getMaxPower() {
        return this.getMaxEnergyStored();
    }

    public long getPowerScaled(long i) {
        return (this.getEnergyStored() * i) / MAX_POWER;
    }

    // ==================== PROCESSING ====================

    public boolean canProcess() {
        int gearLeft = getGearLeft();
        int gearRight = getGearRight();
        
        if (gearLeft == 0 || gearLeft == 3 || gearRight == 0 || gearRight == 3) {
            return false;
        }

        for (int i = INPUT_START; i <= INPUT_END; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getCount() > 0 && hasSpace(stack)) {
                return true;
            }
        }

        return false;
    }

    private void processItem() {
        for (int inpSlot = INPUT_START; inpSlot <= INPUT_END; inpSlot++) {
            ItemStack inputStack = inventory.getStackInSlot(inpSlot);
            if (!inputStack.isEmpty() && hasSpace(inputStack)) {
                ItemStack result = getRecipeResult(inputStack);
                if (result == null || result.isEmpty()) {
                    continue;
                }

                boolean flag = false;

                for (int outSlot = OUTPUT_START; outSlot <= OUTPUT_END; outSlot++) {
                    ItemStack outputStack = inventory.getStackInSlot(outSlot);
                    if (!outputStack.isEmpty() && 
                        PlatformHooks.isSameItemSameTags(outputStack, result) &&
                        outputStack.getCount() + result.getCount() <= outputStack.getMaxStackSize()) {
                        
                        outputStack.grow(result.getCount());
                        inputStack.shrink(1);
                        flag = true;
                        break;
                    }
                }

                if (!flag) {
                    for (int outSlot = OUTPUT_START; outSlot <= OUTPUT_END; outSlot++) {
                        ItemStack outputStack = inventory.getStackInSlot(outSlot);
                        if (outputStack.isEmpty()) {
                            inventory.setStackInSlot(outSlot, result.copy());
                            inputStack.shrink(1);
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean hasSpace(ItemStack stack) {
        ItemStack result = getRecipeResult(stack);
        if (result == null || result.isEmpty()) {
            return false;
        }

        for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                return true;
            }
            if (PlatformHooks.isSameItemSameTags(slotStack, result) &&
                slotStack.getCount() + result.getCount() <= result.getMaxStackSize()) {
                return true;
            }
        }

        return false;
    }

    private ItemStack getRecipeResult(ItemStack input) {
        if (input.isEmpty() || level == null) {
            return new ItemStack(ModItems.SCRAP.get(), 1);
        }

        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, input);
        RecipeInputWrapper wrapper = new RecipeInputWrapper(container);
        Optional<ShredderRecipe> recipe = RecipeHooks.getAllRecipes(level, ShredderRecipe.Type.INSTANCE).stream()
                .filter(r -> r.matchesRecipe(wrapper, level))
                .findFirst();
        
        if (recipe.isPresent()) {
            return recipe.get().getResultItem(level.registryAccess()).copy();
        }
        
        if (isDustLike(input)) {
            ItemStack copy = input.copy();
            copy.setCount(1);
            return copy;
        }
        
        return new ItemStack(ModItems.SCRAP.get(), 1);
    }


    // ==================== BLADES ====================

    public int getGearLeft() {
        ItemStack blade = inventory.getStackInSlot(BLADE_LEFT);
        if (blade.isEmpty() || !(blade.getItem() instanceof ItemBlades)) {
            return 0;
        }

        ItemBlades bladeItem = (ItemBlades) blade.getItem();
        int maxDamage = bladeItem.getMaxDamage(blade);
        
        if (maxDamage == 0) {
            return 1; 
        }

        int currentDamage = blade.getDamageValue();
        if (currentDamage < maxDamage / 2) {
            return 1; 
        } else if (currentDamage < maxDamage) {
            return 2;
        } else {
            return 3;
        }
    }

    public int getGearRight() {
        ItemStack blade = inventory.getStackInSlot(BLADE_RIGHT);
        if (blade.isEmpty() || !(blade.getItem() instanceof ItemBlades)) {
            return 0;
        }

        ItemBlades bladeItem = (ItemBlades) blade.getItem();
        int maxDamage = bladeItem.getMaxDamage(blade);
        
        if (maxDamage == 0) {
            return 1;
        }

        int currentDamage = blade.getDamageValue();
        if (currentDamage < maxDamage / 2) {
            return 1;
        } else if (currentDamage < maxDamage) {
            return 2;
        } else {
            return 3;
        }
    }

    // ==================== UTILITY ====================

    public int getDiFurnaceProgressScaled(int i) {
        return (progress * i) / PROCESSING_SPEED;
    }

    public boolean isProcessing() {
        return this.progress > 0;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return PROCESSING_SPEED;
    }


    private boolean isDustLike(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item == ModItems.DUST.get() || item == ModItems.DUST_TINY.get()) {
            return true;
        }
        for (RegistrySupplier<Item> powder : ModItems.POWDERS.values()) {
            if (powder != null && powder.isPresent() && powder.get() == item) {
                return true;
            }
        }
        for (RegistrySupplier<Item> powder : ModItems.INGOT_POWDERS.values()) {
            if (powder != null && powder.isPresent() && powder.get() == item) {
                return true;
            }
        }
        for (RegistrySupplier<Item> powder : ModItems.INGOT_POWDERS_TINY.values()) {
            if (powder != null && powder.isPresent() && powder.get() == item) {
                return true;
            }
        }
        return false;
    }

    public ContainerData getContainerData() {
        return containerData;
    }
}
