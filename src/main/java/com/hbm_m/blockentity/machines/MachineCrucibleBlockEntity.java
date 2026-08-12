package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.CrucibleSmeltingRecipe;
import com.hbm_m.recipe.MoltenAlloyRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
//?}

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MachineCrucibleBlockEntity extends BlockEntity {

    public static final int INPUT_SLOTS    = 9;
    public static final int LIQUID_CAPACITY = 18_000;
    public static final int PROCESS_TIME   = 200;
    private static final int MAX_HEAT      = 10_000;
    private static final int TU_PER_TICK   = 500;

    private final ModItemStackHandler itemHandler = new ModItemStackHandler(INPUT_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    //? if forge {
    private final LazyOptional<IItemHandler> itemHandlerOpt = LazyOptional.of(() -> itemHandler);
    //?}

    public  int   heat        = 0;
    private int   maxHeat     = MAX_HEAT;
    private int   progress    = 0;
    private int   processTime = PROCESS_TIME;

    /**
     * Molten material pool. Unlike a single-material tank, the crucible can hold
     * several different molten materials simultaneously (like the 1.7.10
     * TileEntityCrucible's recipeStack), which is what lets alloying recipes
     * (steel, dura steel, bronzes, ...) combine multiple inputs at once.
     */
    private final List<MaterialStack> molten = new ArrayList<>();

    private float fillLevel = 0f;
    private int   fillColor = 0xFFC18336;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return switch (i) {
            case 0 -> progress; case 1 -> processTime;
            case 2 -> heat;     case 3 -> maxHeat;
            case 4 -> totalMoltenAmount();
            case 5 -> LIQUID_CAPACITY;
            default -> 0; }; }
        @Override public void set(int i, int v) { switch (i) {
            case 0 -> progress    = v; case 1 -> processTime = v;
            case 2 -> heat        = v; case 3 -> maxHeat     = v; } }
        @Override public int getCount() { return 6; }
    };

    public MachineCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE_BE.get(), pos, state);
    }

    //? if forge {
    public IItemHandler          getItemHandler()        { return itemHandler; }
    //?}
    public ModItemStackHandler   getModItemStackHandler() { return itemHandler; }
    public ContainerData         getData()               { return data; }
    public float                 getFillLevel()          { return fillLevel; }
    public int                   getFillColor()          { return fillColor; }

    /** Returns the single largest molten material for display purposes, or null if empty. */
    public @Nullable MaterialStack getMaterialStack() {
        MaterialStack biggest = null;
        for (MaterialStack ms : molten) {
            if (biggest == null || ms.amount > biggest.amount) biggest = ms;
        }
        return biggest;
    }

    public List<MaterialStack> getMoltenPool() { return molten; }

    private int totalMoltenAmount() {
        int total = 0;
        for (MaterialStack ms : molten) total += ms.amount;
        return total;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineCrucibleBlockEntity be) {
        int oldHeat = be.heat, oldProg = be.progress;
        int oldAmt  = be.totalMoltenAmount();

        long pulled = 0L;
        for (int dy = 1; dy <= 2 && pulled <= 0; dy++)
            for (int dx = -2; dx <= 2 && pulled <= 0; dx++)
                for (int dz = -2; dz <= 2 && pulled <= 0; dz++)
                    pulled = tryPull(level, pos.offset(dx, -dy, dz), be);

        if (pulled <= 0) be.heat = Math.max(0, be.heat - 2);

        int slot = be.findSmeltSlot();
        boolean canSmelt = slot >= 0 && be.heat > 0;
        if (canSmelt) {
            ItemStack in = be.itemHandler.getStackInSlot(slot);
            // smelt() scales with stack size, but only ONE item is consumed per cycle.
            // Рецепты data-driven (JSON) — поиск через RecipeManager (RecipeHooks).
            MaterialStack ms = smeltCrucible(level, in.copyWithCount(1));
            if (ms != null && be.canAccept(ms)) {
                be.progress++;
                be.heat = Math.max(0, be.heat - 1);
                if (be.progress >= be.processTime) {
                    in.shrink(1);
                    be.addMaterial(ms);
                    be.progress = 0;
                }
            } else {
                be.progress = 0;
            }
        } else {
            be.progress = 0;
        }

        be.tryAlloy(level);
        be.tryPourFront(level, pos, state);

        int newAmt = be.totalMoltenAmount();
        MaterialStack primary = be.getMaterialStack();
        be.fillLevel = Math.min(1f, (float) newAmt / LIQUID_CAPACITY);
        be.fillColor = primary != null ? (0xFF000000 | primary.type.color) : 0xFFC18336;

        if (oldHeat != be.heat || oldProg != be.progress || oldAmt != newAmt) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private boolean canAccept(@Nullable MaterialStack ms) {
        if (ms == null) return false;
        return totalMoltenAmount() + ms.amount <= LIQUID_CAPACITY;
    }

    private void addMaterial(MaterialStack ms) {
        for (MaterialStack existing : molten) {
            if (existing.type == ms.type) {
                existing.amount += ms.amount;
                return;
            }
        }
        molten.add(ms.copy());
    }

    private int poolAmount(MaterialType type) {
        for (MaterialStack ms : molten) if (ms.type == type) return ms.amount;
        return 0;
    }

    private boolean poolHasAll(MaterialStack[] required) {
        for (MaterialStack req : required) if (poolAmount(req.type) < req.amount) return false;
        return true;
    }

    private void poolConsume(MaterialStack req) {
        for (int i = 0; i < molten.size(); i++) {
            MaterialStack ms = molten.get(i);
            if (ms.type == req.type) {
                ms.amount -= req.amount;
                if (ms.amount <= 0) molten.remove(i);
                return;
            }
        }
    }

    /**
     * Combines molten materials into alloys (steel, dura steel, bronzes, ...)
     * whenever the pool holds enough of every required input.
     * Port of the 1.7.10 TileEntityCrucible#tryRecipe, generalized over all
     * registered {@link MoltenAlloyRecipe}s instead of a single active recipe.
     */
    private void tryAlloy(Level level) {
        long time = level.getGameTime();
        // MoltenAlloy теперь data-driven (JSON) — итерируем MoltenAlloyRecipe из RecipeManager
        // через RecipeHooks (кросс-версионный мост). Статический MoltenAlloyRecipes удалён.
        for (MoltenAlloyRecipe recipe : RecipeHooks.getAllRecipes(level, MoltenAlloyRecipe.Type.INSTANCE)) {
            MaterialStack[] inputs  = recipe.getInputs();
            MaterialStack[] outputs = recipe.getOutputs();
            int frequency = recipe.getFrequency();
            if (frequency > 0 && time % frequency != 0) continue;
            if (!poolHasAll(inputs)) continue;

            int outputTotal = 0;
            for (MaterialStack out : outputs) outputTotal += out.amount;
            int inputTotal = 0;
            for (MaterialStack in : inputs) inputTotal += in.amount;
            // don't let the reaction overflow the tank
            if (totalMoltenAmount() - inputTotal + outputTotal > LIQUID_CAPACITY) continue;

            for (MaterialStack in : inputs) poolConsume(in);
            for (MaterialStack out : outputs) addMaterial(out.copy());
        }
    }

    private int findSmeltSlot() {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack s = itemHandler.getStackInSlot(i);
            if (s.isEmpty()) continue;
            // only one item is smelted per cycle, so check a single-item copy.
            // Рецепты data-driven (JSON) — поиск через RecipeManager (RecipeHooks); level из BlockEntity.
            MaterialStack ms = smeltCrucible(this.level, s.copyWithCount(1));
            if (ms != null && canAccept(ms)) return i;
        }
        return -1;
    }

    /**
     * Замена удалённому {@code CrucibleSmeltingRecipes.smelt(stack)}: итерация
     * data-driven {@link CrucibleSmeltingRecipe} из {@code RecipeManager} через {@link RecipeHooks};
     * первый {@code matchesInput(stack)} → {@code toMaterialStack(stack.count)}.
     */
    private static MaterialStack smeltCrucible(Level level, ItemStack stack) {
        if (level == null || stack == null || stack.isEmpty()) return null;
        for (CrucibleSmeltingRecipe recipe : RecipeHooks.getAllRecipes(level, CrucibleSmeltingRecipe.Type.INSTANCE)) {
            if (recipe.matchesInput(stack)) {
                return recipe.toMaterialStack(stack.getCount());
            }
        }
        return null;
    }

    /**
     * Original behavior (TileEntityCrucible): the crucible pours out the FRONT
     * (1 block in facing direction), casting a hitscan up to 6 blocks straight
     * down, in portions of 3 nuggets per tick, into any ICrucibleAcceptor
     * (channel, basin with mold, ...). Nothing is lost if no target is found.
     * Only the single largest molten material is poured per tick, mirroring the
     * original's "drain top of the stack" behavior.
     */
    private static final int POUR_PORTION = MaterialStack.MB_PER_INGOT / 3;
    private static final int POUR_RANGE   = 6;

    private void tryPourFront(Level level, BlockPos cruciblePos, BlockState state) {
        MaterialStack top = getMaterialStack();
        if (top == null || top.isEmpty()) return;

        Direction facing = state.hasProperty(com.hbm_m.block.machines.MachineCrucibleBlock.FACING)
                ? state.getValue(com.hbm_m.block.machines.MachineCrucibleBlock.FACING)
                : Direction.NORTH;

        // The OBJ model's green pouring spout sits 90° counterclockwise from FACING
        // (model space: spout at west for facing=north) — pour where the spout is.
        Direction pourDir = facing.getCounterClockWise();

        // Original: pour point is 1.875 blocks from the multiblock center → the
        // block column 2 in front. Distance 1 is tried as fallback since our
        // crucible is a single block and blocks can be placed inside the model.
        for (int dist = 2; dist >= 1; dist--) {
            BlockPos pourPos = cruciblePos.relative(pourDir, dist);

            MaterialStack portion = new MaterialStack(top.type, Math.min(top.amount, POUR_PORTION));
            int poured = com.hbm_m.util.CrucibleUtil.pourSingleStack(level, pourPos, POUR_RANGE, portion);

            if (poured > 0) {
                poolConsume(new MaterialStack(top.type, poured));
                return;
            }
        }
    }

    /** The crucible model spans 3×3 blocks — keep the molten surface visible (Forge render culling). */
    //? if forge {
    @Override
    //?}
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(
                worldPosition.getX() - 1, worldPosition.getY(),     worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 2, worldPosition.getZ() + 2);
    }

    private static long pullEnergy(IEnergyProvider p, MachineCrucibleBlockEntity be) {
        if (!p.canExtract() || be.heat >= be.maxHeat) return 0L;
        long req = Math.min((long)(be.maxHeat - be.heat), TU_PER_TICK);
        long got = p.extractEnergy(req, false);
        if (got > 0) be.heat = Math.min(be.maxHeat, be.heat + (int) got);
        return got;
    }

    private static long tryPull(Level level, BlockPos src, MachineCrucibleBlockEntity be) {
        BlockEntity te = level.getBlockEntity(src);
        if (te == null) return 0L;
        //? if forge {
        return te.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER, Direction.UP)
                .map(p -> pullEnergy(p, be))
                .orElse(0L);
        //?} elif neoforge {
        /*// NeoForge: BlockEntity.getCapability удалён — запрос через level.getCapability(cap, pos, state, be, side).
        IEnergyProvider p = level.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER, src, level.getBlockState(src), te, Direction.UP);
        return p != null ? pullEnergy(p, be) : 0L;
        *///?}
    }

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerOpt.cast();
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); itemHandlerOpt.invalidate(); }
    //?}

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("heat", heat); tag.putInt("progress", progress);
        tag.putFloat("fillLevel", fillLevel); tag.putInt("fillColor", fillColor);

        ListTag list = new ListTag();
        for (MaterialStack ms : molten) {
            CompoundTag entry = new CompoundTag();
            ms.writeToNBT(entry);
            list.add(entry);
        }
        tag.put("molten", list);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("heat", heat); tag.putInt("progress", progress);
        tag.putFloat("fillLevel", fillLevel); tag.putInt("fillColor", fillColor);

        ListTag list = new ListTag();
        for (MaterialStack ms : molten) {
            CompoundTag entry = new CompoundTag();
            ms.writeToNBT(entry);
            list.add(entry);
        }
        tag.put("molten", list);
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) itemHandler.deserializeNBT(tag.getCompound("inventory"));
        heat = tag.getInt("heat"); progress = tag.getInt("progress");
        fillLevel = tag.getFloat("fillLevel"); fillColor = tag.getInt("fillColor");

        molten.clear();
        if (tag.contains("molten")) {
            ListTag list = tag.getList("molten", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                MaterialStack ms = MaterialStack.readFromNBT(list.getCompound(i));
                if (ms != null) molten.add(ms);
            }
        } else if (tag.contains("material")) {
            // legacy single-material save format
            MaterialStack ms = MaterialStack.readFromNBT(tag.getCompound("material"));
            if (ms != null) molten.add(ms);
        }
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        heat = tag.getInt("heat"); progress = tag.getInt("progress");
        fillLevel = tag.getFloat("fillLevel"); fillColor = tag.getInt("fillColor");

        molten.clear();
        if (tag.contains("molten")) {
            ListTag list = tag.getList("molten", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                MaterialStack ms = MaterialStack.readFromNBT(list.getCompound(i));
                if (ms != null) molten.add(ms);
            }
        } else if (tag.contains("material")) {
            // legacy single-material save format
            MaterialStack ms = MaterialStack.readFromNBT(tag.getCompound("material"));
            if (ms != null) molten.add(ms);
        }
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }
    //?} else {
    /*@Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {

        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    
    }
    *///?}

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
