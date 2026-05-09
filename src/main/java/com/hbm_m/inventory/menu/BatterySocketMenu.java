package com.hbm_m.inventory.menu;

import com.hbm_m.block.entity.machines.BatterySocketBlockEntity;
import com.hbm_m.block.machines.MachineBatterySocketBlock;
import com.hbm_m.interfaces.ILongEnergyMenu;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.packet.PacketSyncEnergy;
import com.hbm_m.platform.ModItemStackHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class BatterySocketMenu extends AbstractContainerMenu implements ILongEnergyMenu {

    public final BatterySocketBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private final Player player;

    private static final int PLAYER_START = 0;
    private static final int PLAYER_END = 36;
    private static final int SOCKET_SLOT = 36;

    private long clientEnergy;
    private long clientMaxEnergy;
    private long clientDelta;
    private long lastSyncedEnergy = -1;
    private long lastSyncedMaxEnergy = -1;
    private long lastSyncedDelta = -1;

    public BatterySocketMenu(int containerId, Inventory inv, BatterySocketBlockEntity be, ContainerData data) {
        super(ModMenuTypes.BATTERY_SOCKET_MENU.get(), containerId);
        this.blockEntity = be;
        this.level = inv.player.level();
        this.data = data;
        this.player = inv.player;

        addPlayerInventory(inv, 8, 99);
        addPlayerHotbar(inv, 8, 157);

        Container socketContainer = new HandlerContainer(be.getItemHandler());
        this.addSlot(new Slot(socketContainer, 0, 35, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BatterySocketBlockEntity.isAllowedPortableEnergyStack(stack);
            }
        });

        addDataSlots(data);
    }

    public BatterySocketMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv,
                (BatterySocketBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(3));
    }

    private void addPlayerInventory(Inventory inv, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inv, int x, int y) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, x + col * 18, y));
        }
    }

    @Override
    public void setEnergy(long energy, long maxEnergy, long delta) {
        this.clientEnergy = energy;
        this.clientMaxEnergy = maxEnergy;
        this.clientDelta = delta;
    }

    @Override
    public long getEnergyStatic() {
        return blockEntity.getEnergyStored();
    }

    @Override
    public long getMaxEnergyStatic() {
        return blockEntity.getMaxEnergyStored();
    }

    @Override
    public long getEnergyDeltaStatic() {
        return blockEntity.getEnergyDelta();
    }

    public long getEnergyLong() {
        if (blockEntity != null && !level.isClientSide) {
            return blockEntity.getEnergyStored();
        }
        return clientEnergy;
    }

    public long getMaxEnergyLong() {
        if (blockEntity != null && !level.isClientSide) {
            return blockEntity.getMaxEnergyStored();
        }
        return clientMaxEnergy;
    }

    public long getEnergyDelta() {
        if (blockEntity != null && !level.isClientSide) {
            return blockEntity.getEnergyDelta();
        }
        return clientDelta;
    }

    public long getEnergy() {
        return getEnergyLong();
    }

    public long getMaxEnergy() {
        return getMaxEnergyLong();
    }

    public int getModeOnNoSignal() {
        return data.get(0);
    }

    public int getModeOnSignal() {
        return data.get(1);
    }

    public int getPriorityOrdinal() {
        return data.get(2);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity != null && !level.isClientSide) {
            long curE = blockEntity.getEnergyStored();
            long curM = blockEntity.getMaxEnergyStored();
            long curD = blockEntity.getEnergyDelta();
            if (curE != lastSyncedEnergy || curM != lastSyncedMaxEnergy || curD != lastSyncedDelta) {
                ModPacketHandler.sendToPlayer((ServerPlayer) player, ModPacketHandler.SYNC_ENERGY,
                    new PacketSyncEnergy(containerId, curE, curM, curD));
                lastSyncedEnergy = curE;
                lastSyncedMaxEnergy = curM;
                lastSyncedDelta = curD;
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index == SOCKET_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else {
            if (!BatterySocketBlockEntity.isAllowedPortableEnergyStack(stack)) return ItemStack.EMPTY;
            if (!moveItemStackTo(stack, SOCKET_SLOT, SOCKET_SLOT + 1, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return ContainerLevelAccess.create(level, blockEntity.getBlockPos()).evaluate((lvl, pos) -> {
            Block b = lvl.getBlockState(pos).getBlock();
            return b instanceof MachineBatterySocketBlock
                    && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64 * 64;
        }, true);
    }

    /**
     * Ванильный Container-адаптер поверх {@link ModItemStackHandler}.
     * Нужен, чтобы меню не зависело от Forge `IItemHandler`/`SlotItemHandler`.
     */
    private static final class HandlerContainer implements Container {
        private final ModItemStackHandler handler;

        private HandlerContainer(ModItemStackHandler handler) {
            this.handler = handler;
        }

        @Override
        public int getContainerSize() {
            return handler.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return handler.extractItem(slot, amount, false);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack cur = handler.getStackInSlot(slot);
            if (cur.isEmpty()) return ItemStack.EMPTY;
            handler.setStackInSlot(slot, ItemStack.EMPTY);
            return cur;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            handler.setStackInSlot(slot, stack);
        }

        @Override
        public void setChanged() {
            // изменения трекаются в ModItemStackHandler.onContentsChanged()
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < handler.getSlots(); i++) {
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }
}
