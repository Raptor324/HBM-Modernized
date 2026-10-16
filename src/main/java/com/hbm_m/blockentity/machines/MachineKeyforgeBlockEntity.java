package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.ItemKeyPin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityMachineKeyForge} (1.7.10 Original) - no power required. Slot 0 = source
 * key, slot 1 = destination key that gets duplicated from slot 0's code every tick, slot 2 = a
 * separate key that gets randomized every tick it's present (a "blank cutter").
 */
public class MachineKeyforgeBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_SOURCE = 0;
    public static final int SLOT_DUPE = 1;
    public static final int SLOT_CUT = 2;
    public static final int INVENTORY_SIZE = 3;

    public MachineKeyforgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_KEYFORGE_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineKeyforgeBlockEntity be) {
        if (level.isClientSide) return;

        ItemStack source = be.inventory.getStackInSlot(SLOT_SOURCE);
        ItemStack dupe = be.inventory.getStackInSlot(SLOT_DUPE);
        if (ItemKeyPin.isTransferable(source) && ItemKeyPin.isTransferable(dupe)) {
            int code = ItemKeyPin.getCode(source);
            if (code >= 0 && ItemKeyPin.getCode(dupe) != code) {
                ItemKeyPin.setCode(dupe, code);
            }
        }

        ItemStack cut = be.inventory.getStackInSlot(SLOT_CUT);
        if (ItemKeyPin.isTransferable(cut)) {
            ItemKeyPin.setCode(cut, level.random.nextInt(900) + 100);
        }

        // Anti-sync-spam гейт: подпись = фингерпринты всех трёх слотов (то, что пишет writeNbtData).
        // Случайный код cutter'а меняет подпись почти каждый тик — by-design per-tick поведение
        // (fidelity-решение отложено, см. план миграции); гейт гарантирует, что спокойная машина
        // (нет ключей) не спамит setChanged/sendBlockUpdated.
        long fp = com.hbm_m.platform.PlatformHooks.stackFingerprint(source) * 31L
                + com.hbm_m.platform.PlatformHooks.stackFingerprint(dupe);
        fp = fp * 31L + com.hbm_m.platform.PlatformHooks.stackFingerprint(cut);
        be.syncIfStateChanged(fp, 0L);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // Паритет 1.7.10 (TileEntityMachineKeyForge.isItemValidForSlot = false):
        // автоматизация (воронки) не вставляет предметы — только вручную через GUI.
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_keyforge");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineKeyforgeMenu.create(id, inventory, this);
    }
}
