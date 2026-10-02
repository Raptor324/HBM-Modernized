package com.hbm_m.blockentity.machines;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.multiblock.PartRole;
import com.hbm_m.block.machines.MachineBatterySocketBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyModeHolder;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.inventory.menu.BatterySocketMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.fekal_electric.ModBatteryItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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

/**
 * Battery socket: one portable battery slot, modes like machine battery, energy from item capabilities.
 */
@SuppressWarnings("UnstableApiUsage")
public class BatterySocketBlockEntity extends BaseMachineBlockEntity implements IEnergyModeHolder, com.hbm_m.api.energy.PowerBuffer {

    private static final int SLOT_BATTERY = 0;

    public int modeOnNoSignal = 0;
    public int modeOnSignal = 0;
    private IEnergyReceiver.Priority priority = IEnergyReceiver.Priority.NORMAL;

    public long energyDelta = 0;
    private long lastEnergySample = 0;
    private int lastComparator = -1;

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
        return isEnergyProviderItem(stack) || isEnergyReceiverItem(stack);
    }

    @Override
    protected Component getDefaultName() { return Component.translatable("container.hbm_m.battery_socket"); }

    // Энергопорты мультиблока: сама батарея (ядро, подписывается базовым классом)
    // плюс все ENERGY_CONNECTOR-фантомы структуры.
    @Override
    protected net.minecraft.core.BlockPos[] getExtraEnergyPorts() {
        if (level == null || level.isClientSide) return new BlockPos[0];
        if (!(getBlockState().getBlock() instanceof MachineBatterySocketBlock controller)) {
            return new BlockPos[0];
        }
        Direction facing = getBlockState().getValue(MachineBatterySocketBlock.FACING);
        var helper = controller.getStructureHelper();
        java.util.List<BlockPos> ports = new java.util.ArrayList<>();
        for (BlockPos local : helper.getStructureMap().keySet()) {
            if (helper.resolvePartRole(local, controller) == PartRole.ENERGY_CONNECTOR) {
                ports.add(helper.getRotatedPos(worldPosition, local, facing));
            }
        }
        return ports.toArray(new BlockPos[0]);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BatterySocketBlockEntity be) {
        if (level.isClientSide) return;

        be.ensureNetworkInitialized();

        // Creative-батарея в сокете сама себя не дозаряжает (inventoryTick ходит
        // только по инвентарям игроков) — добираем заряд здесь.
        ItemStack battery = be.inventory.getStackInSlot(SLOT_BATTERY);
        if (battery.getItem() instanceof ItemCreativeBattery creative) {
            if (ModBatteryItem.getEnergy(battery) < creative.getCapacity()) {
                ModBatteryItem.setEnergy(battery, creative.getCapacity());
            }
        }

        long gameTime = level.getGameTime();
        if (gameTime % 10 == 0) {
            long cur = be.getEnergyStoredFromStack();
            be.energyDelta = (cur - be.lastEnergySample) / 10;
            be.lastEnergySample = cur;
            // Заряд меняется сетью в обход onContentsChanged — без периодического
            // sync'а HUD и getUpdatePacket показывают устаревший заряд.
            if (be.energyDelta != 0) {
                be.sendUpdateToClient();
            }
        }

        // Компаратор читает заряд стека, который сам block update не триггерит.
        int comparator = be.getComparatorOutput();
        if (comparator != be.lastComparator) {
            be.lastComparator = comparator;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
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


    public long getEnergyDelta() {
        return energyDelta;
    }

    /** Вставленная батарея — для HUD прицела (ориг. socket.syncStack). */
    public net.minecraft.world.item.ItemStack getBatteryStack() {
        return inventory.getStackInSlot(SLOT_BATTERY);
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
        return com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(stack);
    }

    private Optional<IEnergyReceiver> stackReceiver() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty()) return Optional.empty();
        return com.hbm_m.api.energy.ItemEnergyAccess.getHbmReceiver(stack);
    }

    /**
     * Чужой FE-предмет (нет HBM-капабилити, но есть loader-FE): слот валидации его
     * пропускает (isEnergyProviderItem/isEnergyReceiverItem), поэтому делегаты
     * обязаны уметь работать и с ним — через конверсионные хелперы ItemEnergyAccess.
     */
    private boolean useForgeFallback() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty()) return false;
        return stackProvider().isEmpty() && stackReceiver().isEmpty() && ItemEnergyAccess.hasLoaderEnergy(stack);
    }

    private long getEnergyStoredFromStack() {
        Optional<IEnergyReceiver> r = stackReceiver();
        if (r.isPresent()) return r.get().getEnergyStored();
        Optional<IEnergyProvider> p = stackProvider();
        if (p.isPresent()) return p.get().getEnergyStored();
        if (useForgeFallback()) return ItemEnergyAccess.feStoredAsHe(inventory.getStackInSlot(SLOT_BATTERY));
        return 0L;
    }

    private long getMaxEnergyStoredFromStack() {
        Optional<IEnergyReceiver> r = stackReceiver();
        if (r.isPresent()) return Math.max(1L, r.get().getMaxEnergyStored());
        Optional<IEnergyProvider> p = stackProvider();
        if (p.isPresent()) return Math.max(1L, p.get().getMaxEnergyStored());
        if (useForgeFallback()) return ItemEnergyAccess.feMaxAsHe(inventory.getStackInSlot(SLOT_BATTERY));
        return 1L;
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
        } else if (stackProvider().isPresent()) {
            stackProvider().get().setEnergyStored(energy);
        } else if (useForgeFallback()) {
            // У FE-предмета нет абсолютного set: долить/снять разницу.
            long cur = getEnergyStoredFromStack();
            if (energy > cur) receiveEnergy(energy - cur, false);
            else if (energy < cur) extractEnergy(cur - energy, false);
        }
        // Энергия пишется прямо в NBT стека в обход инвентаря — без этого заряд
        // теряется при автосейве/краше (onContentsChanged не вызывается).
        setChanged();
    }

    // Скорости, как и приём/отдача, обязаны гейтиться режимом: сеть буферизации
    // заходит через setEnergyStored/extractEnergy в обход canReceive/canExtract.
    @Override
    public long getReceiveSpeed() {
        int mode = getMode();
        if (!(mode == 0 || mode == 1)) return 0L;
        Optional<IEnergyReceiver> r = stackReceiver();
        if (r.isPresent()) return r.get().getReceiveSpeed();
        // FE не раскрывает лимит скорости — полная производительность.
        if (useForgeFallback()) return canReceive() ? getMaxEnergyStored() : 0L;
        return 0L;
    }

    @Override
    public IEnergyReceiver.Priority getPriority() {
        return priority;
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        Optional<IEnergyReceiver> r = stackReceiver();
        if (r.isPresent()) return r.get().receiveEnergy(maxReceive, simulate);
        if (useForgeFallback()) {
            long moved = ItemEnergyAccess.receiveHeIntoFe(inventory.getStackInSlot(SLOT_BATTERY), maxReceive, simulate);
            if (moved > 0 && !simulate) setChanged();
            return moved;
        }
        return 0L;
    }

    @Override
    public boolean canReceive() {
        int mode = getMode();
        if (!(mode == 0 || mode == 1)) return false;
        Optional<IEnergyReceiver> r = stackReceiver();
        if (r.isPresent()) return r.get().canReceive();
        if (useForgeFallback()) return ItemEnergyAccess.canForgeReceive(inventory.getStackInSlot(SLOT_BATTERY));
        return false;
    }

    @Override
    public long getProvideSpeed() {
        int mode = getMode();
        if (!(mode == 0 || mode == 2)) return 0L;
        Optional<IEnergyProvider> p = stackProvider();
        if (p.isPresent()) return p.get().getProvideSpeed();
        if (useForgeFallback()) return canExtract() ? getMaxEnergyStored() : 0L;
        return 0L;
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (!canExtract()) return 0;
        Optional<IEnergyProvider> p = stackProvider();
        if (p.isPresent()) return p.get().extractEnergy(maxExtract, simulate);
        if (useForgeFallback()) {
            long moved = ItemEnergyAccess.extractHeFromFe(inventory.getStackInSlot(SLOT_BATTERY), maxExtract, simulate);
            if (moved > 0 && !simulate) setChanged();
            return moved;
        }
        return 0L;
    }

    @Override
    public boolean canExtract() {
        int mode = getMode();
        if (!(mode == 0 || mode == 2)) return false;
        Optional<IEnergyProvider> p = stackProvider();
        if (p.isPresent()) return p.get().canExtract();
        if (useForgeFallback()) return ItemEnergyAccess.canForgeExtract(inventory.getStackInSlot(SLOT_BATTERY));
        return false;
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
                // Паритет с оригиналом: батареи ограничены приоритетами LOW..HIGH (без LOWEST/HIGHEST)
                IEnergyReceiver.Priority[] priorities = IEnergyReceiver.Priority.values();
                int current = Math.max(1, Math.min(this.priority.ordinal(), priorities.length - 2));
                int next = (current >= priorities.length - 2) ? 1 : current + 1;
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

    
    // (кастомный лёгкий getUpdateTag ниже сохранён как есть)
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("modeOnNoSignal", modeOnNoSignal);
        tag.putInt("modeOnSignal", modeOnSignal);
        tag.putInt("priorityV2", priority.ordinal());
        tag.putLong("energyDelta", energyDelta);
        tag.putLong("lastEnergySample", lastEnergySample);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        modeOnNoSignal = tag.getInt("modeOnNoSignal");
        modeOnSignal = tag.getInt("modeOnSignal");
        // priorityV2 — ординал нового 5-уровневого enum'а; старый "priority" (3 уровня) сдвигаем на +1
        if (tag.contains("priorityV2")) {
            int p = tag.getInt("priorityV2");
            IEnergyReceiver.Priority[] vals = IEnergyReceiver.Priority.values();
            priority = vals[Math.max(0, Math.min(p, vals.length - 1))];
        } else if (tag.contains("priority")) {
            int p = tag.getInt("priority") + 1;
            IEnergyReceiver.Priority[] vals = IEnergyReceiver.Priority.values();
            priority = vals[Math.max(0, Math.min(p, vals.length - 1))];
        }
        energyDelta = tag.getLong("energyDelta");
        lastEnergySample = tag.getLong("lastEnergySample");

        // Тело батареи рисует BER прямо из инвентаря — пересборка чанка не нужна.
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(
            worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
            worldPosition.getX() + 3, worldPosition.getY() + 3, worldPosition.getZ() + 3
        );
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
