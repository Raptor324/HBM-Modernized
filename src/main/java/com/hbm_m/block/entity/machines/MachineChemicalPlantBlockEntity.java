package com.hbm_m.block.entity.machines;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.machines.MachineChemicalPlantBlock;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.inventory.menu.MachineChemicalPlantMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.hbm_m.recipe.ChemicalPlantRecipe.CountedIngredient;
import com.hbm_m.recipe.ChemicalPlantRecipe.FluidIngredient;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Chemical Plant BlockEntity - порт с 1.7.10.
 * 22 слота, 6 FluidTank (3 input, 3 output), энергия.
 * Логика крафтов - заглушка.
 */
public class MachineChemicalPlantBlockEntity extends BaseMachineBlockEntity {

    private static final int SLOT_COUNT = 22;
    private static final int SLOT_BATTERY = 0;
    private static final int SLOT_SCHEMATIC = 1;
    private static final int SLOT_UPGRADE_START = 2;
    private static final int SLOT_UPGRADE_END = 3;
    private static final int SLOT_SOLID_INPUT_START = 4;
    private static final int SLOT_SOLID_INPUT_END = 6;
    private static final int SLOT_SOLID_OUTPUT_START = 7;
    private static final int SLOT_SOLID_OUTPUT_END = 9;
    private static final int SLOT_FLUID_INPUT_START = 10;
    private static final int SLOT_FLUID_INPUT_END = 12;
    private static final int SLOT_FLUID_INPUT_EMPTY_START = 13;
    private static final int SLOT_FLUID_INPUT_EMPTY_END = 15;
    private static final int SLOT_FLUID_OUTPUT_START = 16;
    private static final int SLOT_FLUID_OUTPUT_END = 18;
    private static final int SLOT_FLUID_OUTPUT_EMPTY_START = 19;
    private static final int SLOT_FLUID_OUTPUT_EMPTY_END = 21;

    private static final int TANK_CAPACITY = 24_000;
    private static final long MAX_POWER = 100_000;

    private final FluidTank[] inputTanks = new FluidTank[3];
    private final FluidTank[] outputTanks = new FluidTank[3];
    private final LazyOptional<IFluidHandler>[] inputTankHandlers = new LazyOptional[3];
    private final LazyOptional<IFluidHandler>[] outputTankHandlers = new LazyOptional[3];

    private boolean didProcess = false;
    @Nullable private ResourceLocation selectedRecipeId = null;
    @Nullable private ChemicalPlantRecipe cachedRecipe = null;
    private boolean recipeCacheDirty = false;

    private int progress = 0;
    private int maxProgress = 100;

    private float anim = 0.0F;
    private float prevAnim = 0.0F;

    private int renderCooldownTimer = 0;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 2;
        }
    };

    /** Как 1.7.10: не воздух на три блока выше контроллера → видимая «рама». */
    private void updateFrameBlockState() {
        if (level == null) return;
        BlockState st = getBlockState();
        if (!st.hasProperty(MachineChemicalPlantBlock.FRAME)) return;
        boolean frame = !level.getBlockState(worldPosition.above(3)).isAir();
        if (st.getValue(MachineChemicalPlantBlock.FRAME) != frame) {
            level.setBlock(worldPosition, st.setValue(MachineChemicalPlantBlock.FRAME, frame), 3);
        }
    }

    /**
     * Заглушка под логику крафта: когда появится обработка рецептов - выставлять
     * {@link MachineChemicalPlantBlock#RENDER_ACTIVE} (и сбрасывать по таймеру как у advanced assembler).
     */
    private void syncRenderActiveStub() {
        // TODO: crafting progress → RENDER_ACTIVE + chunk rebuild
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof MachineChemicalPlantBlock block)) {
            return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 3, 2));
        }
        var structureHelper = block.getStructureHelper();
        var structureMap = structureHelper.getStructureMap();
        if (structureMap == null || structureMap.isEmpty()) {
            return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 3, 2));
        }
        int minX = 0, minY = 0, minZ = 0;
        int maxX = 0, maxY = 0, maxZ = 0;
        for (BlockPos offset : structureMap.keySet()) {
            minX = Math.min(minX, offset.getX());
            minY = Math.min(minY, offset.getY());
            minZ = Math.min(minZ, offset.getZ());
            maxX = Math.max(maxX, offset.getX());
            maxY = Math.max(maxY, offset.getY());
            maxZ = Math.max(maxZ, offset.getZ());
        }
        double margin = 1.5;
        return new AABB(
            worldPosition.getX() + minX - margin,
            worldPosition.getY() + minY - margin,
            worldPosition.getZ() + minZ - margin,
            worldPosition.getX() + maxX + 1 + margin,
            worldPosition.getY() + maxY + 1 + margin,
            worldPosition.getZ() + maxZ + 1 + margin
        );
    }

    public MachineChemicalPlantBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMICAL_PLANT_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            inputTanks[i] = new FluidTank(TANK_CAPACITY) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                    if (level != null && !level.isClientSide) {
                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    }
                }
            };
            outputTanks[i] = new FluidTank(TANK_CAPACITY) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                    if (level != null && !level.isClientSide) {
                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    }
                }
            };
            inputTankHandlers[i] = LazyOptional.of(() -> inputTanks[idx]);
            outputTankHandlers[i] = LazyOptional.of(() -> outputTanks[idx]);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineChemicalPlantBlockEntity entity) {
        entity.prevAnim = entity.anim;

        if (level.isClientSide) {
            if (entity.didProcess) {
                entity.anim += 0.05F;
            }
            if (entity.anim > (float) (Math.PI * 2.0)) {
                entity.anim -= (float) (Math.PI * 2.0);
            }
            entity.clientTick();
            return;
        }

        entity.updateFrameBlockState();
        entity.syncRenderActiveStub();

        entity.updateFrameBlockState();
        entity.syncRenderActiveStub();

        entity.updateFrameBlockState();
        entity.syncRenderActiveStub();

        entity.ensureNetworkInitialized();
        entity.chargeFromBattery();
        entity.transferFluidsFromItems();
        entity.transferFluidsToItems();

        if (level.getGameTime() % 10L == 0L) {
            entity.updateEnergyDelta(entity.getEnergyStored());
        }

        boolean dirty = false;

        ChemicalPlantRecipe currentRecipe = entity.getCachedRecipe();

        // Blueprint pool guard: если рецепт требует пул и установленная папка не совпадает - сбрасываем.
        ItemStack blueprint = entity.inventory.getStackInSlot(SLOT_SCHEMATIC);
        if (currentRecipe != null && !entity.isRecipeValidForBlueprint(currentRecipe, blueprint)) {
            entity.selectedRecipeId = null;
            entity.cachedRecipe = null;
            entity.recipeCacheDirty = false;
            currentRecipe = null;
            entity.progress = 0;
            entity.didProcess = false;
            dirty = true;
        }

        if (currentRecipe != null) {
            entity.maxProgress = currentRecipe.getDuration();
            long powerPerTick = currentRecipe.getPowerConsumption();

            if (entity.canProcess(currentRecipe) && entity.getEnergyStored() >= powerPerTick) {
                entity.setEnergyStored(entity.getEnergyStored() - powerPerTick);
                entity.progress++;
                entity.didProcess = true;
                dirty = true;

                if (entity.progress >= entity.maxProgress) {
                    entity.progress = 0;
                    entity.finishRecipe(currentRecipe);
                    dirty = true;
                }
            } else {
                if (entity.progress != 0 || entity.didProcess) {
                    entity.progress = 0;
                    entity.didProcess = false;
                    dirty = true;
                }
            }
        } else {
            if (entity.progress != 0 || entity.didProcess) {
                entity.progress = 0;
                entity.didProcess = false;
                dirty = true;
            }
        }

        // RENDER_ACTIVE cooldown как у ассемблера: когда крафт идёт - держим 5 секунд после.
        if (entity.didProcess) {
            entity.renderCooldownTimer = 100;
        } else if (entity.renderCooldownTimer > 0) {
            entity.renderCooldownTimer--;
        }
        boolean shouldRenderActive = entity.renderCooldownTimer > 0;
        if (state.hasProperty(MachineChemicalPlantBlock.RENDER_ACTIVE)) {
            boolean active = state.getValue(MachineChemicalPlantBlock.RENDER_ACTIVE);
            if (active != shouldRenderActive) {
                level.setBlock(pos, state.setValue(MachineChemicalPlantBlock.RENDER_ACTIVE, shouldRenderActive), 3);
            }
        }

        if (dirty) {
            entity.setChanged();
            entity.sendUpdateToClient();
        }
    }

    private boolean canProcess(ChemicalPlantRecipe recipe) {
        // Item inputs: positional (0..2) → slots 4..6
        List<CountedIngredient> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            int slot = SLOT_SOLID_INPUT_START + i;
            ItemStack slotStack = inventory.getStackInSlot(slot);
            CountedIngredient req = itemInputs.get(i);
            if (!req.ingredient().test(slotStack) || slotStack.getCount() < req.count()) return false;
        }

        // Fluid inputs: positional (0..2) → inputTanks[0..2]
        List<FluidIngredient> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            FluidIngredient req = fluidInputs.get(i);
            var fluid = ForgeRegistries.FLUIDS.getValue(req.fluidId());
            if (fluid == null) return false;
            FluidTank tank = inputTanks[i];
            if (tank.getFluid().isEmpty()
                || tank.getFluid().getFluid() != fluid
                || tank.getFluidAmount() < req.amount()) {
                return false;
            }
        }

        // Item outputs: positional (0..2) → slots 7..9
        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        for (int i = 0; i < itemOutputs.size(); i++) {
            ItemStack output = itemOutputs.get(i);
            if (output.isEmpty()) continue;
            int slot = SLOT_SOLID_OUTPUT_START + i;
            ItemStack slotStack = inventory.getStackInSlot(slot);
            if (!slotStack.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(slotStack, output)) return false;
                if (slotStack.getCount() + output.getCount() > slotStack.getMaxStackSize()) return false;
            }
        }

        // Fluid outputs: positional (0..2) → outputTanks[0..2]
        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < fluidOutputs.size(); i++) {
            FluidStack output = fluidOutputs.get(i);
            if (output.isEmpty()) continue;
            FluidTank tank = outputTanks[i];
            if (!tank.getFluid().isEmpty() && tank.getFluid().getFluid() != output.getFluid()) return false;
            if (tank.getFluidAmount() + output.getAmount() > tank.getCapacity()) return false;
        }

        return true;
    }

    private void finishRecipe(ChemicalPlantRecipe recipe) {
        // Consume item inputs
        List<CountedIngredient> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            int slot = SLOT_SOLID_INPUT_START + i;
            inventory.getStackInSlot(slot).shrink(itemInputs.get(i).count());
        }

        // Consume fluid inputs
        List<FluidIngredient> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            inputTanks[i].drain(fluidInputs.get(i).amount(), IFluidHandler.FluidAction.EXECUTE);
        }

        // Produce item outputs
        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        for (int i = 0; i < itemOutputs.size(); i++) {
            ItemStack output = itemOutputs.get(i);
            if (output.isEmpty()) continue;
            int slot = SLOT_SOLID_OUTPUT_START + i;
            ItemStack slotStack = inventory.getStackInSlot(slot);
            if (slotStack.isEmpty()) {
                inventory.setStackInSlot(slot, output.copy());
            } else {
                slotStack.grow(output.getCount());
            }
        }

        // Produce fluid outputs
        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < fluidOutputs.size(); i++) {
            FluidStack output = fluidOutputs.get(i);
            if (output.isEmpty()) continue;
            outputTanks[i].fill(output.copy(), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private boolean isRecipeValidForBlueprint(ChemicalPlantRecipe recipe, ItemStack blueprintFolder) {
        String pool = recipe.getBlueprintPool();
        if (pool == null || pool.isEmpty()) return true;
        String installed = ItemBlueprintFolder.getBlueprintPool(blueprintFolder);
        return installed != null && !installed.isEmpty() && installed.equals(pool);
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    private void clientTick() {
        com.hbm_m.sound.ClientSoundManager.updateSound(this, this.didProcess,
                () -> new com.hbm_m.sound.ChemicalPlantSoundInstance(this.getBlockPos()));
    }

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty()) return;

        if (stack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }

        stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).ifPresent(provider -> {
            long needed = getMaxEnergyStored() - getEnergyStored();
            if (needed <= 0) return;
            long extracted = provider.extractEnergy(Math.min(needed, getReceiveSpeed()), false);
            if (extracted > 0) {
                setEnergyStored(getEnergyStored() + extracted);
                setChanged();
            }
        });

        if (!stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent()) {
            stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(provider -> {
                long needed = getMaxEnergyStored() - getEnergyStored();
                if (needed <= 0) return;
                int extracted = provider.extractEnergy((int) Math.min(needed, getReceiveSpeed()), false);
                if (extracted > 0) {
                    setEnergyStored(getEnergyStored() + extracted);
                    setChanged();
                }
            });
        }
    }

    private void transferFluidsFromItems() {
        for (int i = 0; i < 3; i++) {
            int fullContainerSlot = SLOT_FLUID_INPUT_START + i;
            int emptyContainerSlot = SLOT_FLUID_INPUT_EMPTY_START + i;
            ItemStack fullContainer = inventory.getStackInSlot(fullContainerSlot);
            if (fullContainer.isEmpty()) continue;
            if (!inventory.getStackInSlot(emptyContainerSlot).isEmpty()) continue;

            var result = FluidUtil.tryEmptyContainer(fullContainer, inputTanks[i], TANK_CAPACITY, null, true);
            if (result.isSuccess()) {
                fullContainer.shrink(1);
                inventory.setStackInSlot(emptyContainerSlot, result.getResult());
                setChanged();
            }
        }
    }

    private void transferFluidsToItems() {
        for (int i = 0; i < 3; i++) {
            int emptyContainerSlot = SLOT_FLUID_OUTPUT_START + i;
            int filledContainerSlot = SLOT_FLUID_OUTPUT_EMPTY_START + i;
            ItemStack emptyContainer = inventory.getStackInSlot(emptyContainerSlot);
            if (emptyContainer.isEmpty()) continue;
            if (!inventory.getStackInSlot(filledContainerSlot).isEmpty()) continue;

            var result = FluidUtil.tryFillContainer(emptyContainer, outputTanks[i], TANK_CAPACITY, null, true);
            if (result.isSuccess()) {
                emptyContainer.shrink(1);
                inventory.setStackInSlot(filledContainerSlot, result.getResult());
                setChanged();
            }
        }
    }

    public void drops() {
        if (level == null) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.chemical_plant");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) {
            return stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                || stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent();
        }
        if (slot == SLOT_SCHEMATIC) {
            return stack.getItem() instanceof ItemBlueprintFolder;
        }
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END) {
            return true;
        }
        if (slot >= SLOT_SOLID_OUTPUT_START && slot <= SLOT_SOLID_OUTPUT_END) {
            return false;
        }
        if (slot >= SLOT_FLUID_INPUT_START && slot <= SLOT_FLUID_INPUT_END) {
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        }
        if (slot >= SLOT_FLUID_OUTPUT_START && slot <= SLOT_FLUID_OUTPUT_END) {
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        }
        if (slot >= SLOT_FLUID_INPUT_EMPTY_START && slot <= SLOT_FLUID_INPUT_EMPTY_END) {
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        }
        if (slot >= SLOT_FLUID_OUTPUT_EMPTY_START && slot <= SLOT_FLUID_OUTPUT_EMPTY_END) {
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        }
        return true;
    }

    @Override
    protected void setupFluidCapability() {
        // Экспонируем первый input tank по умолчанию
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineChemicalPlantMenu(containerId, playerInventory, this, data);
    }

    public FluidTank[] getInputTanks() {
        return inputTanks;
    }

    public FluidTank[] getOutputTanks() {
        return outputTanks;
    }

    public List<ChemicalPlantRecipe> getAvailableRecipes() {
        if (level == null) return List.of();
        ItemStack folder = inventory.getStackInSlot(SLOT_SCHEMATIC);
        String installedPool = ItemBlueprintFolder.getBlueprintPool(folder);
        List<ChemicalPlantRecipe> all = level.getRecipeManager().getAllRecipesFor(ChemicalPlantRecipe.Type.INSTANCE);
        return all.stream().filter(r -> {
            String pool = r.getBlueprintPool();
            if (pool == null || pool.isEmpty()) return true;
            return installedPool != null && !installedPool.isEmpty() && installedPool.equals(pool);
        }).toList();
    }

    public ItemStack getBlueprintFolder() {
        return inventory.getStackInSlot(SLOT_SCHEMATIC);
    }

    public boolean getDidProcess() {
        return didProcess;
    }

    @Nullable
    public ResourceLocation getSelectedRecipeId() {
        return selectedRecipeId;
    }

    public void setSelectedRecipe(@Nullable ResourceLocation recipeId) {
        this.selectedRecipeId = recipeId;
        this.cachedRecipe = null;
        this.recipeCacheDirty = true;
        this.progress = 0;
        this.didProcess = false;
        setChanged();
        if (level != null && !level.isClientSide) {
            sendUpdateToClient();
        }
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public float getAnim(float partialTicks) {
        return prevAnim + (anim - prevAnim) * partialTicks;
    }

    public FluidStack getFluid() {
        return inputTanks[0].getFluid();
    }

    public float getFluidFillFraction() {
        if (inputTanks[0].getCapacity() <= 0) return 0.0F;
        return Math.min(1.0F, Math.max(0.0F, inputTanks[0].getFluidAmount() / (float) inputTanks[0].getCapacity()));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < 3; i++) {
            tag.put("inputTank" + i, inputTanks[i].writeToNBT(new CompoundTag()));
            tag.put("outputTank" + i, outputTanks[i].writeToNBT(new CompoundTag()));
        }
        tag.putBoolean("didProcess", didProcess);
        tag.putBoolean("HasRecipe", selectedRecipeId != null);
        if (selectedRecipeId != null) {
            tag.putString("SelectedRecipe", selectedRecipeId.toString());
        }
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tag.putFloat("anim", anim);
        tag.putFloat("prevAnim", prevAnim);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < 3; i++) {
            if (tag.contains("inputTank" + i)) {
                inputTanks[i].readFromNBT(tag.getCompound("inputTank" + i));
            }
            if (tag.contains("outputTank" + i)) {
                outputTanks[i].readFromNBT(tag.getCompound("outputTank" + i));
            }
        }
        didProcess = tag.getBoolean("didProcess");
        if (tag.contains("HasRecipe") && tag.getBoolean("HasRecipe")) {
            selectedRecipeId = ResourceLocation.tryParse(tag.getString("SelectedRecipe"));
            recipeCacheDirty = true;
        } else {
            selectedRecipeId = null;
            cachedRecipe = null;
            recipeCacheDirty = false;
        }
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        if (maxProgress <= 0) maxProgress = 100;
        anim = tag.getFloat("anim");
        prevAnim = tag.getFloat("prevAnim");
    }

    @Nullable
    private ChemicalPlantRecipe getCachedRecipe() {
        if (selectedRecipeId == null || level == null) {
            cachedRecipe = null;
            return null;
        }
        if (cachedRecipe == null || recipeCacheDirty) {
            cachedRecipe = level.getRecipeManager()
                .byKey(selectedRecipeId)
                .filter(r -> r instanceof ChemicalPlantRecipe)
                .map(r -> (ChemicalPlantRecipe) r)
                .orElse(null);
            recipeCacheDirty = false;
        }
        return cachedRecipe;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return inputTankHandlers[0].cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (int i = 0; i < 3; i++) {
            inputTankHandlers[i].invalidate();
            outputTankHandlers[i].invalidate();
        }
    }
}
