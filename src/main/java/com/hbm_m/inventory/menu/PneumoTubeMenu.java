package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.network.pneumatic.PneumoTubeBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code ContainerPneumoTube} (1.7.10). Die fuenfzehn Filterplaetze liegen als 5×3
 * bei (35, 17), das Spielerinventar bei (8, 103) und die Schnellleiste auf 161.
 *
 * <p>Die Filterplaetze sind <b>Schaufenster</b>: der Gegenstand bleibt in der Hand, es wird nur
 * eine Vorlage hinterlegt. Ein Rechtsklick auf eine belegte Vorlage schaltet ihre Vergleichsart
 * weiter (genau / Platzhalter / Marke), ein Linksklick setzt oder loescht sie.</p>
 */
public class PneumoTubeMenu extends AbstractContainerMenu {

    private static final int FILTER_SLOTS = PneumoTubeBlockEntity.FILTER_SLOTS;

    private final PneumoTubeBlockEntity blockEntity;

    public PneumoTubeMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public PneumoTubeMenu(int id, Inventory inv, PneumoTubeBlockEntity blockEntity) {
        super(ModMenuTypes.PNEUMO_TUBE_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                int index = row * 5 + col;
                this.addSlot(new Slot(container, index, 35 + col * 18, 17 + row * 18) {
                    @Override
                    public void set(ItemStack stack) {
                        super.set(stack);
                        blockEntity.getMatcher().initPattern(index, stack);
                    }

                    /** Vorlagenplatz: der Gegenstand bleibt beim Spieler. */
                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                });
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 103 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 161));
        }
    }

    private static PneumoTubeBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof PneumoTubeBlockEntity tube) return tube;
        throw new IllegalStateException("No PneumoTubeBlockEntity at " + pos);
    }

    public PneumoTubeBlockEntity getBlockEntity() { return blockEntity; }

    /** 1:1-Port von {@code slotClick} fuer die Vorlagenplaetze. */
    @Override
    public void clicked(int index, int button, ClickType clickType, Player player) {
        if (index < 0 || index >= FILTER_SLOTS) {
            super.clicked(index, button, clickType, player);
            return;
        }

        Slot slot = this.slots.get(index);

        // Rechtsklick auf eine belegte Vorlage: Vergleichsart weiterschalten.
        if (button == 1 && clickType == ClickType.PICKUP && slot.hasItem()) {
            blockEntity.nextFilterMode(index);
            return;
        }

        // Sonst: die Vorlage auf das setzen, was der Spieler in der Hand haelt (Abbild, nicht das
        // Stueck selbst) - oder leeren, wenn die Hand leer ist.
        ItemStack held = getCarried();
        ItemStack template = held.isEmpty() ? ItemStack.EMPTY : held.copyWithCount(1);
        slot.set(template);
        blockEntity.setChanged();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Original: {@code transferStackInSlot} gibt immer null zurueck - Umlagern gibt es nicht.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) return false;
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }
}
