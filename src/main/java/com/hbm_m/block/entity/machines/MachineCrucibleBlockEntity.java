package com.hbm_m.block.entity.machines;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.recipe.CrucibleSmeltingRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity for the Crucible.
 *
 * Currently holds only the render-facing data exposed to {@link com.hbm_m.client.render.implementations.CrucibleRenderer}:
 *   - {@link #fillLevel}  (0.0 – 1.0) fraction of total capacity that is filled
 *   - {@link #fillColor}  packed ARGB tint for the molten surface (defaults to a warm orange)
 *
 * Once the MaterialStack / Mats system is ported, wire the actual recipeStack and wasteStack
 * here and compute fillLevel / fillColor from that data.
 */
public class MachineCrucibleBlockEntity extends BlockEntity {

    private static final int MAX_HEAT = 10_000;
    private static final int PROCESS_TIME = 200;
    private static final int TU_PULL_PER_TICK = 500;
    private static final int LIQUID_CAPACITY = 10_000;
    private static final int LIQUID_PER_SMELT = 1_000;

    /** 9-slot input grid — items placed into the crucible for smelting / casting. */
    public static final int INPUT_SLOTS = 9;

    private final ModItemStackHandler itemHandler = new ModItemStackHandler(INPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemHandlerOpt = LazyOptional.of(() -> itemHandler);

    // 0.0 = empty, 1.0 = full
    private float fillLevel = 0f;

    // Packed 0xAARRGGBB — default warm molten-metal orange
    private int fillColor = 0xFFC18336;

    private int progress = 0;
    private int processTime = PROCESS_TIME;
    private int heat = 0;
    private int maxHeat = MAX_HEAT;
    private int liquidStored = 0;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> processTime;
                case 2 -> heat;
                case 3 -> maxHeat;
                case 4 -> liquidStored;
                case 5 -> LIQUID_CAPACITY;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> processTime = value;
                case 2 -> heat = value;
                case 3 -> maxHeat = value;
                case 4 -> liquidStored = value;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public MachineCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE_BE.get(), pos, state);
    }

    public IItemHandler getItemHandler() { return itemHandler; }

    /** Для {@link com.hbm_m.inventory.ModItemStackHandlerContainer} в общем меню (Forge: {@link ModItemStackHandler}). */
    public ModItemStackHandler getModItemStackHandler() { return itemHandler; }
    public ContainerData getData() { return data; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineCrucibleBlockEntity be) {
        int oldHeat = be.heat;
        int oldProgress = be.progress;
        int oldLiquid = be.liquidStored;

        long pulled = 0L;
        // Probe two layers below and a wider footprint so multiblock heaters can be found.
        for (int dy = 1; dy <= 2 && pulled <= 0L; dy++) {
            for (int dx = -2; dx <= 2 && pulled <= 0L; dx++) {
                for (int dz = -2; dz <= 2 && pulled <= 0L; dz++) {
                    pulled = tryPullFromPos(level, pos.offset(dx, -dy, dz), be);
                }
            }
        }

        if (pulled <= 0L) {
            be.heat = Math.max(0, be.heat - 2);
        }

        int recipeSlot = be.findSmeltableInputSlot();
        boolean canProcess = recipeSlot >= 0 && be.heat > 0 && (be.liquidStored + LIQUID_PER_SMELT) <= LIQUID_CAPACITY;

        if (canProcess) {
            be.progress = Math.min(be.processTime, be.progress + 1);
            be.heat = Math.max(0, be.heat - 1);
            if (be.progress >= be.processTime) {
                ItemStack in = be.itemHandler.getStackInSlot(recipeSlot);
                if (!in.isEmpty()) {
                    in.shrink(1);
                    be.liquidStored = Math.min(LIQUID_CAPACITY, be.liquidStored + LIQUID_PER_SMELT);
                }
                be.progress = 0;
            }
        } else {
            be.progress = 0;
        }

        float newFill = (float) be.liquidStored / (float) LIQUID_CAPACITY;
        be.fillLevel = Math.max(0f, Math.min(1f, newFill));

        if (oldHeat != be.heat || oldProgress != be.progress || oldLiquid != be.liquidStored) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private static long pullEnergy(IEnergyProvider provider, MachineCrucibleBlockEntity be) {
        if (!provider.canExtract() || be.heat >= be.maxHeat) return 0L;
        int need = be.maxHeat - be.heat;
        long request = Math.min((long) need, TU_PULL_PER_TICK);
        long extracted = provider.extractEnergy(request, false);
        if (extracted > 0L) {
            be.heat = Math.min(be.maxHeat, be.heat + (int) extracted);
        }
        return extracted;
    }

    private static long tryPullFromPos(Level level, BlockPos sourcePos, MachineCrucibleBlockEntity be) {
        BlockEntity source = level.getBlockEntity(sourcePos);
        if (source == null) return 0L;
        return source.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER, Direction.UP)
                .map(provider -> pullEnergy(provider, be))
                .orElseGet(() -> source.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER)
                        .map(provider -> pullEnergy(provider, be))
                        .orElse(0L));
    }

    private int findSmeltableInputSlot() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack in = itemHandler.getStackInSlot(i);
            if (in.isEmpty()) continue;
            if (hasSmeltingRecipe(in)) return i;
        }
        return -1;
    }

    private boolean hasSmeltingRecipe(ItemStack input) {
        for (ItemStack key : CrucibleSmeltingRecipes.getRecipes().keySet()) {
            if (ItemStack.isSameItemSameTags(key, input) || ItemStack.isSameItem(key, input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerOpt.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerOpt.invalidate();
    }

    // -------------------------------------------------------------------------
    // Render data API (read by CrucibleRenderer on the client thread)
    // -------------------------------------------------------------------------

    public float getFillLevel() { return fillLevel; }
    public int getFillColor()   { return fillColor; }

    // -------------------------------------------------------------------------
    // Setters called once MaterialStack is ported
    // -------------------------------------------------------------------------

    public void setFillLevel(float level) {
        this.fillLevel = Math.max(0f, Math.min(1f, level));
        setChanged();
    }

    public void setFillColor(int argb) {
        this.fillColor = argb;
        setChanged();
    }

    // -------------------------------------------------------------------------
    // NBT persistence
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putFloat("fillLevel", fillLevel);
        tag.putInt("fillColor",  fillColor);
        tag.putInt("progress", progress);
        tag.putInt("processTime", processTime);
        tag.putInt("heat", heat);
        tag.putInt("maxHeat", maxHeat);
        tag.putInt("liquidStored", liquidStored);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) itemHandler.deserializeNBT(tag.getCompound("inventory"));
        if (tag.contains("fillLevel")) fillLevel = tag.getFloat("fillLevel");
        if (tag.contains("fillColor"))  fillColor  = tag.getInt("fillColor");
        if (tag.contains("progress")) progress = tag.getInt("progress");
        if (tag.contains("processTime")) processTime = tag.getInt("processTime");
        if (tag.contains("heat")) heat = tag.getInt("heat");
        if (tag.contains("maxHeat")) maxHeat = tag.getInt("maxHeat");
        if (tag.contains("liquidStored")) liquidStored = tag.getInt("liquidStored");
    }
}
