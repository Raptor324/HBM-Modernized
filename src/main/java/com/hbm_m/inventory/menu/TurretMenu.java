package com.hbm_m.inventory.menu;

import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.blockentity.machines.TurretBaseBlockEntity;
import com.hbm_m.interfaces.ILongEnergyMenu;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Generisches Menu fuer alle Turret-Varianten - siehe {@link TurretBaseBlockEntity}. */
@SuppressWarnings("UnstableApiUsage")
public class TurretMenu extends AbstractContainerMenu implements ILongEnergyMenu {

    public final TurretBaseBlockEntity blockEntity;
    private final Player player;

    private long clientEnergy;
    private long clientMaxEnergy;

    private static final int PLAYER_INV_START = 10;
    private static final int PLAYER_INV_END = 46;
    private static final int AMMO_SLOT_COUNT = 9;
    private static final int BATTERY_SLOT = 9;

    public TurretMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    private static TurretBaseBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf extraData) {
        BlockEntity blockEntity = inv.player.level().getBlockEntity(extraData.readBlockPos());
        if (blockEntity instanceof TurretBaseBlockEntity turret) return turret;
        // На клиенте тайл может отсутствовать (реплей Flashback) — возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (inv.player.level().isClientSide) return null;
        throw new IllegalStateException("BlockEntity is not a TurretBaseBlockEntity");
    }

    public TurretMenu(int id, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.TURRET_MENU.get(), id);

        this.blockEntity = entity instanceof TurretBaseBlockEntity turret ? turret : null;
        this.player = inv.player;

        // тайл может отсутствовать на клиенте (реплей Flashback) — подставляем пустую заглушку
        var handler = this.blockEntity != null
                ? this.blockEntity.getInventory()
                : new DummyItemStackHandler(BATTERY_SLOT + 1);
        var container = new ModItemStackHandlerContainer(handler,
                this.blockEntity != null ? this.blockEntity::setChanged : () -> {});

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(container, row * 3 + col, 80 + col * 18, 63 + row * 18));
            }
        }

        this.addSlot(new Slot(container, BATTERY_SLOT, 152, 99) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ItemEnergyAccess.isEnergySource(stack);
            }
        });

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    @Override
    public void setEnergy(long energy, long maxEnergy, long delta) {
        this.clientEnergy = energy;
        this.clientMaxEnergy = maxEnergy;
    }

    public long getEnergyStatic() {
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            return blockEntity.getEnergyStored();
        }
        return clientEnergy;
    }

    public long getMaxEnergyStatic() {
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            return blockEntity.getMaxEnergyStored();
        }
        return clientMaxEnergy;
    }

    public long getEnergyDeltaStatic() {
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            return blockEntity.getEnergyDelta();
        }
        return 0;
    }

    public long getEnergyLong() {
        return getEnergyStatic();
    }

    public long getMaxEnergyLong() {
        return getMaxEnergyStatic();
    }

    public int getEnergyScaled(int scale) {
        long max = getMaxEnergyLong();
        return max > 0 ? (int) ((double) getEnergyLong() / max * scale) : 0;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            ModPacketHandler.sendToPlayer((net.minecraft.server.level.ServerPlayer) this.player, ModPacketHandler.SYNC_ENERGY,
                    new com.hbm_m.network.packet.PacketSyncEnergy(
                            this.containerId,
                            blockEntity.getEnergyStored(),
                            blockEntity.getMaxEnergyStored(),
                            blockEntity.getEnergyDelta()
                    ));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (pIndex < PLAYER_INV_START) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, itemstack);
            } else if (pIndex >= PLAYER_INV_START && pIndex < PLAYER_INV_END) {
                // тайл может отсутствовать на клиенте (реплей Flashback)
                if (blockEntity != null && blockEntity.isAcceptedAmmoPublic(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, 0, AMMO_SLOT_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    boolean isEnergySource = ItemEnergyAccess.isEnergySource(slotStack);
                    if (isEnergySource && !this.moveItemStackTo(slotStack, BATTERY_SLOT, BATTERY_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    } else if (!isEnergySource) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, slotStack);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        // тайл может отсутствовать на клиенте (реплей Flashback)
        if (blockEntity == null) {
            return false;
        }
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), pPlayer, blockEntity.getBlockState().getBlock());
    }

    private void addPlayerInventory(Inventory i) {
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(i, x + y * 9 + 9, 8 + x * 18, 122 + y * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory i) {
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(i, x, 8 + x * 18, 180));
        }
    }
}