package com.hbm_m.block.entity.machines;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.machines.MachineBatterySocketBlock;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

//? if forge {
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import com.hbm_m.capability.ModCapabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?}

//? if fabric {
/*import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import team.reborn.energy.api.EnergyStorage;
*///?}
/**
 * Battery socket: one portable battery slot, modes like machine battery, energy from item capabilities.
 */
public class BatterySocketBlockEntity extends BaseMachineBlockEntity implements IEnergyModeHolder {

    //? if forge {
    public static final ModelProperty<Boolean> HAS_INSERT = new ModelProperty<>();
    //?}

    private static final int SLOT_BATTERY = 0;

    public int modeOnNoSignal = 0;
    public int modeOnSignal = 0;
    private IEnergyReceiver.Priority priority = IEnergyReceiver.Priority.NORMAL;

    public long energyDelta = 0;
    private long lastEnergySample = 0;

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
        super(ModBlockEntities.BATTERY_SOCKET_BE.get(), pos, state, 1, 0L, 0L, 0L);
    }

    public static boolean isAllowedPortableEnergyStack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemCreativeBattery) return true;
        //? if forge {
        return stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                || stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent()
                || stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).isPresent();
        //?}
        //? if fabric {
        /*return EnergyStorage.ITEM.find(stack, null) != null;
        *///?}
    }

    @Override
    protected Component getDefaultName() { return Component.translatable("container.hbm_m.battery_socket"); }

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

    /** Backwards-compatible accessor for renderers/menus. */
    public com.hbm_m.platform.ModItemStackHandler getItemHandler() {
        return this.inventory;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY && isAllowedPortableEnergyStack(stack);
    }

    @Override
    protected boolean isCriticalSlot(int slot) {
        return slot == SLOT_BATTERY;
    }

    //? if forge {
    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(HAS_INSERT, !inventory.getStackInSlot(0).isEmpty())
                .build();
    }
    //?}

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
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty()) return Optional.empty();
        //? if forge {
        return stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).resolve();
        //?}
        //? if fabric {
        /*return Optional.empty();
        *///?}
    }

    private Optional<IEnergyReceiver> stackReceiver() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty()) return Optional.empty();
        //? if forge {
        return stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).resolve();
        //?}
        //? if fabric {
        /*return Optional.empty();
        *///?}
    }

    private long getEnergyStoredFromStack() {
        //? if fabric {
        /*ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty()) return 0L;
        var es = EnergyStorage.ITEM.find(stack, null);
        return es != null ? es.getAmount() : 0L;
        *///?}
        Optional<IEnergyReceiver> r = stackReceiver();
        if (r.isPresent()) return r.get().getEnergyStored();
        return stackProvider().map(IEnergyProvider::getEnergyStored).orElse(0L);
    }

    private long getMaxEnergyStoredFromStack() {
        //? if fabric {
        /*ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty()) return 1L;
        var es = EnergyStorage.ITEM.find(stack, null);
        return es != null ? Math.max(1L, es.getCapacity()) : 1L;
        *///?}
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
        //? if fabric {
        /*return getMaxEnergyStoredFromStack(); // ограничим реально капом предмета
        *///?}
        return stackReceiver().map(IEnergyReceiver::getReceiveSpeed).orElse(0L);
    }

    @Override
    public IEnergyReceiver.Priority getPriority() {
        return priority;
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        //? if fabric {
        /*ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        var es = EnergyStorage.ITEM.find(stack, null);
        if (es == null || !es.supportsInsertion()) return 0;
        if (simulate) {
            try (Transaction tx = Transaction.openOuter()) {
                long accepted = es.insert(maxReceive, tx);
                return accepted;
            }
        }
        try (Transaction tx = Transaction.openOuter()) {
            long accepted = es.insert(maxReceive, tx);
            if (accepted > 0) tx.commit();
            return accepted;
        }
        *///?}
        return stackReceiver().map(r -> r.receiveEnergy(maxReceive, simulate)).orElse(0L);
    }

    @Override
    public boolean canReceive() {
        int mode = getMode();
        if (!(mode == 0 || mode == 1)) return false;
        //? if fabric {
        /*ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        var es = EnergyStorage.ITEM.find(stack, null);
        return es != null && es.supportsInsertion();
        *///?}
        return stackReceiver().map(IEnergyReceiver::canReceive).orElse(false);
    }

    @Override
    public long getProvideSpeed() {
        //? if fabric {
        /*return getEnergyStoredFromStack();
        *///?}
        return stackProvider().map(IEnergyProvider::getProvideSpeed).orElse(0L);
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (!canExtract()) return 0;
        //? if fabric {
        /*ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        var es = EnergyStorage.ITEM.find(stack, null);
        if (es == null || !es.supportsExtraction()) return 0;
        if (simulate) {
            try (Transaction tx = Transaction.openOuter()) {
                long extracted = es.extract(maxExtract, tx);
                return extracted;
            }
        }
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = es.extract(maxExtract, tx);
            if (extracted > 0) tx.commit();
            return extracted;
        }
        *///?}
        return stackProvider().map(p -> p.extractEnergy(maxExtract, simulate)).orElse(0L);
    }

    @Override
    public boolean canExtract() {
        int mode = getMode();
        if (!(mode == 0 || mode == 2)) return false;
        //? if fabric {
        /*ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        var es = EnergyStorage.ITEM.find(stack, null);
        return es != null && es.supportsExtraction();
        *///?}
        return stackProvider().map(IEnergyProvider::canExtract).orElse(false);
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return true;
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
        tag.putInt("modeOnNoSignal", modeOnNoSignal);
        tag.putInt("modeOnSignal", modeOnSignal);
        tag.putInt("priority", priority.ordinal());
        tag.putLong("energyDelta", energyDelta);
        tag.putLong("lastEnergySample", lastEnergySample);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
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
        tag.putInt("modeOnNoSignal", modeOnNoSignal);
        tag.putInt("modeOnSignal", modeOnSignal);
        tag.putInt("priority", priority.ordinal());
        return tag;
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
