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

    /** Menue-Slots wie Original ContainerTurretBase: 0 KI-Chip (98/27), 1-9 Munition, 10 Batterie.
     *  BE-Inventar bleibt: 0-8 Munition, 9 Batterie, 10 Chip. */
    private static final int CHIP_MENU_SLOT = 0;
    private static final int PLAYER_INV_START = 11;
    private static final int PLAYER_INV_END = 47;
    private static final int AMMO_MENU_START = 1;
    private static final int AMMO_SLOT_COUNT = 9;
    private static final int BATTERY_SLOT = 9;
    private static final int BATTERY_MENU_SLOT = 10;

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
                : new DummyItemStackHandler(TurretBaseBlockEntity.CHIP_SLOT + 1);
        var container = new ModItemStackHandlerContainer(handler,
                this.blockEntity != null ? this.blockEntity::setChanged : () -> {});

        // Original ContainerTurretBase: zuerst KI-Chip (98/27), dann 3x3 Munition, dann Batterie
        this.addSlot(new Slot(container, TurretBaseBlockEntity.CHIP_SLOT, 98, 27) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(com.hbm_m.item.ModItems.TURRET_CHIP.get());
            }
        });

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

        // Original openInventory(): "hbm:block.openC"
        if (this.blockEntity != null && !inv.player.level().isClientSide) playTurretSound("hbm:block.openC");
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Original closeInventory(): "hbm:block.closeC"
        if (this.blockEntity != null && !player.level().isClientSide) playTurretSound("hbm:block.closeC");
    }

    private void playTurretSound(String sound) {
        var pos = blockEntity.getBlockPos();
        blockEntity.getLevel().playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                com.hbm_m.sound.HbmSoundsNT.get(sound), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
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
                // Original: turret_chip geht in den KI-Slot
                if (slotStack.is(com.hbm_m.item.ModItems.TURRET_CHIP.get())) {
                    if (!this.moveItemStackTo(slotStack, CHIP_MENU_SLOT, CHIP_MENU_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                // тайл может отсутствовать на клиенте (реплей Flashback)
                } else if (blockEntity != null && blockEntity.isAcceptedAmmoPublic(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, AMMO_MENU_START, AMMO_MENU_START + AMMO_SLOT_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    boolean isEnergySource = ItemEnergyAccess.isEnergySource(slotStack);
                    if (isEnergySource && !this.moveItemStackTo(slotStack, BATTERY_MENU_SLOT, BATTERY_MENU_SLOT + 1, false)) {
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
        // audit13: Original isUseableByPlayer (<= 128 zur Kernmitte) oder Huelle <= 64; Vanilla 64 schloss die GUI an grossen Maschinen
        return MultiblockMenuReach.stillValidCore(blockEntity, pPlayer, 128.0D);
    }

    private void addPlayerInventory(Inventory i) {
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(i, x + y * 9 + 9, 8 + x * 18, 84 + y * 18 + (18 * 3) + 2));
            }
        }
    }

    private void addPlayerHotbar(Inventory i) {
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(i, x, 8 + x * 18, 142 + (18 * 3) + 2));
        }
    }
}