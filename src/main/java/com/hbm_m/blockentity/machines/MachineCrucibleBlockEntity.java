package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
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

public class MachineCrucibleBlockEntity extends BlockEntity {

    public static final int INPUT_SLOTS    = 9;
    public static final int LIQUID_CAPACITY = 18_000;
    public static final int PROCESS_TIME   = 200;
    private static final int MAX_HEAT      = 10_000;
    private static final int TU_PER_TICK   = 500;

    private final ModItemStackHandler itemHandler = new ModItemStackHandler(INPUT_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final LazyOptional<IItemHandler> itemHandlerOpt = LazyOptional.of(() -> itemHandler);

    public  int   heat        = 0;
    private int   maxHeat     = MAX_HEAT;
    private int   progress    = 0;
    private int   processTime = PROCESS_TIME;

    @Nullable
    private MaterialStack materialStack = null;

    private float fillLevel = 0f;
    private int   fillColor = 0xFFC18336;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return switch (i) {
            case 0 -> progress; case 1 -> processTime;
            case 2 -> heat;     case 3 -> maxHeat;
            case 4 -> materialStack != null ? materialStack.amount : 0;
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

    public IItemHandler          getItemHandler()        { return itemHandler; }
    public ModItemStackHandler   getModItemStackHandler() { return itemHandler; }
    public ContainerData         getData()               { return data; }
    public float                 getFillLevel()          { return fillLevel; }
    public int                   getFillColor()          { return fillColor; }
    public @Nullable MaterialStack getMaterialStack()    { return materialStack; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineCrucibleBlockEntity be) {
        int oldHeat = be.heat, oldProg = be.progress;
        int oldAmt  = be.materialStack != null ? be.materialStack.amount : -1;

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
            MaterialStack ms = CrucibleSmeltingRecipes.smelt(in);
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

        be.tryPourIntoBasin(level, pos);

        int newAmt = be.materialStack != null ? be.materialStack.amount : -1;
        be.fillLevel = be.materialStack != null
                ? Math.min(1f, (float) be.materialStack.amount / LIQUID_CAPACITY) : 0f;
        be.fillColor = be.materialStack != null ? (0xFF000000 | be.materialStack.type.color) : 0xFFC18336;

        if (oldHeat != be.heat || oldProg != be.progress || oldAmt != newAmt) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private boolean canAccept(@Nullable MaterialStack ms) {
        if (ms == null) return false;
        if (materialStack == null) return ms.amount <= LIQUID_CAPACITY;
        return materialStack.type == ms.type
                && materialStack.amount + ms.amount <= LIQUID_CAPACITY;
    }

    private void addMaterial(MaterialStack ms) {
        if (materialStack == null) {
            materialStack = ms.copy();
        } else {
            materialStack.amount += ms.amount;
        }
    }

    private int findSmeltSlot() {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack s = itemHandler.getStackInSlot(i);
            if (s.isEmpty()) continue;
            MaterialStack ms = CrucibleSmeltingRecipes.smelt(s);
            if (ms != null && canAccept(ms)) return i;
        }
        return -1;
    }

    private void tryPourIntoBasin(Level level, BlockPos cruciblePos) {
        if (materialStack == null || materialStack.isEmpty()) return;
        BlockEntity below = level.getBlockEntity(cruciblePos.below());
        int poured = 0;
        if (below instanceof MachineFoundryBasinBlockEntity basin) {
            poured = basin.receiveMaterial(materialStack.type, materialStack.amount);
        } else if (below instanceof MachineFoundryChannelBlockEntity channel) {
            poured = channel.receiveMaterial(materialStack.type, materialStack.amount);
        }
        if (poured > 0) {
            materialStack.amount -= poured;
            if (materialStack.isEmpty()) materialStack = null;
        }
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
        return te.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER, Direction.UP)
                .map(p -> pullEnergy(p, be))
                .orElseGet(() -> te.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER)
                        .map(p -> pullEnergy(p, be)).orElse(0L));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerOpt.cast();
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); itemHandlerOpt.invalidate(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("heat", heat); tag.putInt("progress", progress);
        tag.putFloat("fillLevel", fillLevel); tag.putInt("fillColor", fillColor);
        if (materialStack != null) { CompoundTag ms = new CompoundTag(); materialStack.writeToNBT(ms); tag.put("material", ms); }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) itemHandler.deserializeNBT(tag.getCompound("inventory"));
        heat = tag.getInt("heat"); progress = tag.getInt("progress");
        fillLevel = tag.getFloat("fillLevel"); fillColor = tag.getInt("fillColor");
        if (tag.contains("material")) materialStack = MaterialStack.readFromNBT(tag.getCompound("material"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
