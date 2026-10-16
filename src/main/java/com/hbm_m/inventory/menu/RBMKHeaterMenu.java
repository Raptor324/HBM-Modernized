package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.rbmk.RBMKHeaterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code ContainerRBMKHeater}: ein Fluid-Identifier-Slot bei (41,45), der den
 * Kuehlmitteltyp des Zulauftanks umschaltet ({@code FluidTank.setType}), plus das Spieler-Inventar
 * (xSize/ySize 176x186, wie im Original). Der Slot fehlte bisher, weil die Block-Entity gar kein
 * Item-Inventar hatte - damit war der Waermetauscher fest auf ein Kuehlmittel verdrahtet.
 */
public class RBMKHeaterMenu extends AbstractContainerMenu {

    private final RBMKHeaterBlockEntity blockEntity;

    public RBMKHeaterMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public RBMKHeaterMenu(int id, Inventory inv, RBMKHeaterBlockEntity be) {
        super(ModMenuTypes.RBMK_HEATER_MENU.get(), id);
        this.blockEntity = be;

        addSlot(new net.minecraftforge.items.SlotItemHandler(be.inventory, RBMKHeaterBlockEntity.SLOT_FLUID_ID, 41, 45));

        // Original (ContainerRBMKHeater): 8+j*18, 84+i*18+20 / 8+i*18, 142+20 -- image is 176x186,
        // 20px taller than the standard 166px machine GUI.
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 162));
    }

    private static RBMKHeaterBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof RBMKHeaterBlockEntity h) return h;
        // На клиенте тайл может отсутствовать (реплей Flashback) — возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (inv.player.level().isClientSide) return null;
        throw new IllegalStateException("No RBMKHeaterBlockEntity at " + pos);
    }

    public RBMKHeaterBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        // тайл может отсутствовать на клиенте (реплей Flashback)
        if (blockEntity == null) {
            return false;
        }
        return blockEntity.getLevel() == player.level()
            && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
