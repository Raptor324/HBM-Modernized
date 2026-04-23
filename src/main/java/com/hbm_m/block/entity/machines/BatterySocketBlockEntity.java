package com.hbm_m.block.entity.machines;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.machines.MachineBatterySocketBlock;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IEnergyModeHolder;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.inventory.menu.BatterySocketMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
/**
 * Battery socket: one portable battery slot, modes like machine battery, energy from item capabilities.
 */
public class BatterySocketBlockEntity extends BlockEntity implements MenuProvider, IEnergyProvider, IEnergyReceiver, IEnergyConnector, IEnergyModeHolder {

    public static final ModelProperty<Boolean> HAS_INSERT = new ModelProperty<>();
    public static final ModelProperty<Boolean> CREATIVE_INSERT = new ModelProperty<>();

    public int modeOnNoSignal = 0;
    public int modeOnSignal = 0;
    private IEnergyReceiver.Priority priority = IEnergyReceiver.Priority.NORMAL;

    public long energyDelta = 0;
    private long lastEnergySample = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            BatterySocketBlockEntity.this.setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isAllowedPortableEnergyStack(stack);
        }
    };

    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> itemHandler);

    private LazyOptional<IEnergyProvider> hbmProvider = LazyOptional.empty();
    private LazyOptional<IEnergyReceiver> hbmReceiver = LazyOptional.empty();
    private LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.empty();

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> modeOnNoSignal;
                case 1 -> modeOnSignal;
                case 2 -> priority.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> modeOnNoSignal = value;
                case 1 -> modeOnSignal = value;
                case 2 -> priority = IEnergyReceiver.Priority.values()[Math.max(0, Math.min(value, IEnergyReceiver.Priority.values().length - 1))];
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public BatterySocketBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_SOCKET_BE.get(), pos, state);
    }

    public static boolean isAllowedPortableEnergyStack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent()
                || stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).isPresent();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        hbmProvider = LazyOptional.of(() -> this);
        hbmReceiver = LazyOptional.of(() -> this);
        hbmConnector = LazyOptional.of(() -> this);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCap.invalidate();
        hbmProvider.invalidate();
        hbmReceiver.invalidate();
        hbmConnector.invalidate();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BatterySocketBlockEntity be) {
        if (level.isClientSide) return;

        EnergyNetworkManager manager = EnergyNetworkManager.get((ServerLevel) level);
        manager.addNode(pos);
        Direction facing = state.getValue(MachineBatterySocketBlock.FACING);
        var helper = MachineBatterySocketBlock.getStructureHelperStatic();
        for (BlockPos local : helper.getStructureMap().keySet()) {
            if (MachineBatterySocketBlock.getPartRoleStatic(local) == PartRole.ENERGY_CONNECTOR) {
                manager.addNode(helper.getRotatedPos(pos, local, facing));
            }
        }

        long gameTime = level.getGameTime();
        if (gameTime % 10 == 0) {
            long cur = be.getEnergyStoredFromStack();
            be.energyDelta = (cur - be.lastEnergySample) / 10;
            be.lastEnergySample = cur;
        }
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public long getEnergyDelta() {
        return energyDelta;
    }

    @Override
    public int getCurrentMode() {
        if (level == null) return modeOnNoSignal;
        return level.hasNeighborSignal(worldPosition) ? modeOnSignal : modeOnNoSignal;
    }

    private int getMode() {
        return getCurrentMode();
    }

    private Optional<IEnergyProvider> stackProvider() {
        ItemStack stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return Optional.empty();
        return stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).resolve();
    }

    private Optional<IEnergyReceiver> stackReceiver() {
        ItemStack stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return Optional.empty();
        return stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).resolve();
    }

    private long getEnergyStoredFromStack() {
        Optional<IEnergyReceiver> r = stackReceiver();
        if (r.isPresent()) return r.get().getEnergyStored();
        return stackProvider().map(IEnergyProvider::getEnergyStored).orElse(0L);
    }

    private long getMaxEnergyStoredFromStack() {
        Optional<IEnergyReceiver> r = stackReceiver();
        if (r.isPresent()) return Math.max(1, r.get().getMaxEnergyStored());
        return stackProvider().map(p -> Math.max(1, p.getMaxEnergyStored())).orElse(1L);
    }

    @Override
    public long getEnergyStored() {
        return getEnergyStoredFromStack();
    }

    @Override
    public long getMaxEnergyStored() {
        return getMaxEnergyStoredFromStack();
    }

    @Override
    public void setEnergyStored(long energy) {
        Optional<IEnergyReceiver> rec = stackReceiver();
        if (rec.isPresent()) {
            rec.get().setEnergyStored(energy);
        } else {
            stackProvider().ifPresent(p -> p.setEnergyStored(energy));
        }
    }

    @Override
    public long getReceiveSpeed() {
        return stackReceiver().map(IEnergyReceiver::getReceiveSpeed).orElse(0L);
    }

    @Override
    public IEnergyReceiver.Priority getPriority() {
        return priority;
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        return stackReceiver().map(r -> r.receiveEnergy(maxReceive, simulate)).orElse(0L);
    }

    @Override
    public boolean canReceive() {
        int mode = getMode();
        if (!(mode == 0 || mode == 1)) return false;
        return stackReceiver().map(IEnergyReceiver::canReceive).orElse(false);
    }

    @Override
    public long getProvideSpeed() {
        return stackProvider().map(IEnergyProvider::getProvideSpeed).orElse(0L);
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (!canExtract()) return 0;
        return stackProvider().map(p -> p.extractEnergy(maxExtract, simulate)).orElse(0L);
    }

    @Override
    public boolean canExtract() {
        int mode = getMode();
        if (!(mode == 0 || mode == 2)) return false;
        return stackProvider().map(IEnergyProvider::canExtract).orElse(false);
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return true;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerCap.cast();
        }
        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) {
            return hbmConnector.cast();
        }
        int mode = getMode();
        if (cap == ModCapabilities.HBM_ENERGY_PROVIDER && (mode == 0 || mode == 2) && stackProvider().isPresent()) {
            return hbmProvider.cast();
        }
        if (cap == ModCapabilities.HBM_ENERGY_RECEIVER && (mode == 0 || mode == 1) && stackReceiver().isPresent()) {
            return hbmReceiver.cast();
        }
        return super.getCapability(cap, side);
    }

    public void handleButtonPress(int buttonId) {
        switch (buttonId) {
            case 0 -> this.data.set(0, (this.modeOnNoSignal + 1) % 4);
            case 1 -> this.data.set(1, (this.modeOnSignal + 1) % 4);
            case 2 -> {
                IEnergyReceiver.Priority[] priorities = IEnergyReceiver.Priority.values();
                int next = (this.priority.ordinal() + 1) % priorities.length;
                this.data.set(2, next);
            }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public int getComparatorOutput() {
        long max = getMaxEnergyStoredFromStack();
        if (max <= 0) return 0;
        double frac = (double) getEnergyStoredFromStack() / (double) max * 15.0;
        return Math.min(15, Math.max(0, (int) Math.round(frac)));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("modeOnNoSignal", modeOnNoSignal);
        tag.putInt("modeOnSignal", modeOnSignal);
        tag.putInt("priority", priority.ordinal());
        tag.putLong("energyDelta", energyDelta);
        tag.putLong("lastEnergySample", lastEnergySample);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        }
        modeOnNoSignal = tag.getInt("modeOnNoSignal");
        modeOnSignal = tag.getInt("modeOnSignal");
        if (tag.contains("priority")) {
            int p = tag.getInt("priority");
            IEnergyReceiver.Priority[] vals = IEnergyReceiver.Priority.values();
            priority = vals[Math.max(0, Math.min(p, vals.length - 1))];
        }
        energyDelta = tag.getLong("energyDelta");
        lastEnergySample = tag.getLong("lastEnergySample");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("modeOnNoSignal", modeOnNoSignal);
        tag.putInt("modeOnSignal", modeOnSignal);
        tag.putInt("priority", priority.ordinal());
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @NotNull
    @Override
    public ModelData getModelData() {
        ItemStack stack = itemHandler.getStackInSlot(0);
        boolean has = !stack.isEmpty() && isAllowedPortableEnergyStack(stack);
        boolean creative = stack.getItem() instanceof ItemCreativeBattery;
        return ModelData.builder()
                .with(HAS_INSERT, has && !creative)
                .with(CREATIVE_INSERT, has && creative)
                .build();
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(3, 3, 3));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new BatterySocketMenu(containerId, inv, this, data);
    }
}
