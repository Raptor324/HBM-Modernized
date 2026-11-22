package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineFireboxBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 1:1 {@code ContainerFirebox} (Feuerbuechse): Layout und Logik im gemeinsamen
 * {@link FireboxBaseMenu}-Skelett, hier nur noch der eigene MenuType und die
 * Original-Reichweite (128 vom Kern, Multiblock-Huelle mitgerechnet).
 */
public class MachineFireboxMenu extends FireboxBaseMenu<MachineFireboxBlockEntity> {

    public MachineFireboxMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, lookup(inventory, extraData));
    }

    public MachineFireboxMenu(int id, Inventory inv, MachineFireboxBlockEntity furnace) {
        this(ModMenuTypes.MACHINE_FIREBOX_MENU.get(), id, inv, furnace, new SimpleContainerData(DATA_SLOTS));
    }

    public static MachineFireboxMenu create(int id, Inventory inventory, MachineFireboxBlockEntity blockEntity) {
        return new MachineFireboxMenu(ModMenuTypes.MACHINE_FIREBOX_MENU.get(), id, inventory, blockEntity, blockEntity.getData());
    }

    protected MachineFireboxMenu(net.minecraft.world.inventory.MenuType<?> type, int id, Inventory inv,
                                 MachineFireboxBlockEntity furnace, net.minecraft.world.inventory.ContainerData data) {
        super(type, id, inv, furnace, data);
    }

    protected static MachineFireboxBlockEntity lookup(Inventory inventory, FriendlyByteBuf buffer) {
        if (inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof MachineFireboxBlockEntity be) return be;
        throw new IllegalStateException("No firebox at expected position");
    }

    @Override
    public boolean stillValid(Player player) {
        // w16b: Original isUseableByPlayer (TileEntityMachineBase) = 128 vom Kern, dazu Huelle der Maschine (MultiblockMenuReach)
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }
}
