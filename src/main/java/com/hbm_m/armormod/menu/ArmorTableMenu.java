package com.hbm_m.armormod.menu;

import com.hbm_m.platform.PlatformHooks;

import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.block.ModBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import com.hbm_m.interfaces.IHasTooltip;
import com.hbm_m.inventory.menu.ModMenuTypes;
import com.hbm_m.sound.ModSounds;

import java.util.Map;

public class ArmorTableMenu extends AbstractContainerMenu {

    private final Container armorInventory = new SimpleContainer(1);
    private final Container modsInventory = new SimpleContainer(ArmorModificationHelper.MOD_SLOTS);
    private final ContainerLevelAccess access;
    private final Player player;

    /** РљР°Рє РІ 1.7.10 {@code ContainerArmorTable}: СЃР»РѕС‚С‹ РјРѕРґРѕРІ 0..8, С†РµРЅС‚СЂР°Р»СЊРЅР°СЏ Р±СЂРѕРЅСЏ вЂ” 9. */
    public static final int MOD_SLOTS = ArmorModificationHelper.MOD_SLOTS;
    public static final int SLOT_ARMOR_IN = MOD_SLOTS;

    public static final int SLOT_ARMOR_SIDE_HELMET = MOD_SLOTS + 1;
    public static final int SLOT_ARMOR_SIDE_CHEST = MOD_SLOTS + 2;
    public static final int SLOT_ARMOR_SIDE_LEGS = MOD_SLOTS + 3;
    public static final int SLOT_ARMOR_SIDE_BOOTS = MOD_SLOTS + 4;

    private static final int PLAYER_INVENTORY_START = MOD_SLOTS + 5;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 26;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END + 1;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 8;

    private static final int ARMOR_PANEL_START = SLOT_ARMOR_SIDE_HELMET;
    private static final int ARMOR_PANEL_END = SLOT_ARMOR_SIDE_BOOTS;

    public ArmorTableMenu(int pContainerId, Inventory pPlayerInventory, final BlockPos pPos) {
        super(ModMenuTypes.ARMOR_TABLE_MENU.get(), pContainerId);
        this.player = pPlayerInventory.player;
        this.access = ContainerLevelAccess.create(pPlayerInventory.player.level(), pPos);

        // РЎР»РѕС‚С‹ 0-8: РјРѕРґС‹ (РєР°Рє ContainerArmorTable.UpgradeSlot)
        this.addSlot(new ModificationSlot(modsInventory, ArmorModificationHelper.helmet_only, 26, 27));
        this.addSlot(new ModificationSlot(modsInventory, ArmorModificationHelper.plate_only, 62, 27));
        this.addSlot(new ModificationSlot(modsInventory, ArmorModificationHelper.legs_only, 98, 27));
        this.addSlot(new ModificationSlot(modsInventory, ArmorModificationHelper.boots_only, 134, 45));
        this.addSlot(new ModificationSlot(modsInventory, ArmorModificationHelper.servos, 134, 81));
        this.addSlot(new ModificationSlot(modsInventory, ArmorModificationHelper.cladding, 98, 99));
        this.addSlot(new ModificationSlot(modsInventory, ArmorModificationHelper.kevlar, 62, 99));
        this.addSlot(new ModificationSlot(modsInventory, ArmorModificationHelper.extra, 26, 99));
        this.addSlot(new ModificationSlot(modsInventory, ArmorModificationHelper.battery, 8, 63));

        // РЎР»РѕС‚ 9: С†РµРЅС‚СЂР°Р»СЊРЅР°СЏ Р±СЂРѕРЅСЏ
        this.addSlot(new CentralArmorSlot(armorInventory, 0, 44, 63));

        addPlayerArmorSlots(pPlayerInventory);
        addPlayerInventory(pPlayerInventory);
        addPlayerHotbar(pPlayerInventory);
    }

    public ArmorTableMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf extraData) {
        this(pContainerId, pPlayerInventory, extraData.readBlockPos());
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        if (!pPlayer.level().isClientSide) {
            ItemStack armor = this.armorInventory.getItem(0);
            if (!armor.isEmpty()) {
                ArmorModificationHelper.flushTableToArmor(armor, this.modsInventory, pPlayer);
                if (!pPlayer.getInventory().add(armor)) {
                    pPlayer.drop(armor, false);
                }
                this.armorInventory.setItem(0, ItemStack.EMPTY);
                this.clearModSlots();
            } else {
                for (int i = 0; i < this.modsInventory.getContainerSize(); i++) {
                    ItemStack mod = this.modsInventory.getItem(i);
                    if (!mod.isEmpty()) {
                        if (!pPlayer.getInventory().add(mod)) {
                            pPlayer.drop(mod, false);
                        }
                        this.modsInventory.setItem(i, ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    private void syncCenterArmorStack(ItemStack armorStack) {
        if (armorStack.isEmpty() || player.level().isClientSide) {
            return;
        }
        this.armorInventory.setItem(0, armorStack.copy());
        this.broadcastChanges();
    }

    private void clearModSlots() {
        for (int i = 0; i < this.modsInventory.getContainerSize(); i++) {
            this.modsInventory.setItem(i, ItemStack.EMPTY);
        }
    }

    /** РљР°Рє РІ 1.7.10 onPickupFromSlot С†РµРЅС‚СЂР°Р»СЊРЅРѕР№ Р±СЂРѕРЅРё: СѓР±СЂР°С‚СЊ РїСЂРµРґРјРµС‚С‹ РјРѕРґРѕРІ РёР· СЃС‚РѕР»Р°, NBT Р±СЂРѕРЅРё РЅРµ С‚СЂРѕРіР°РµРј. */
    private void absorbModItemsFromTable(ItemStack armorStack) {
        if (armorStack.isEmpty()) {
            return;
        }
        for (int i = 0; i < MOD_SLOTS; i++) {
            ItemStack mod = this.modsInventory.getItem(i);
            if (!mod.isEmpty() && ArmorModificationHelper.isApplicable(armorStack, mod)) {
                this.modsInventory.setItem(i, ItemStack.EMPTY);
            }
        }
        this.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack sourceStack = slot.getItem();
            itemstack = sourceStack.copy();

            if (pIndex < MOD_SLOTS) {
                // РњРѕРґ РёР· СЃС‚РѕР»Р° -> РёРЅРІРµРЅС‚Р°СЂСЊ РёРіСЂРѕРєР°
                if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END + 1, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (pIndex == SLOT_ARMOR_IN) {
                if (!pPlayer.level().isClientSide && !sourceStack.isEmpty()) {
                    ArmorModificationHelper.flushTableToArmor(sourceStack, this.modsInventory, pPlayer);
                }

                int targetSlotIndex = -1;
                if (sourceStack.getItem() instanceof ArmorItem armorItem) {
                    EquipmentSlot slotType = armorItem.getEquipmentSlot();
                    if (slotType == EquipmentSlot.HEAD) targetSlotIndex = SLOT_ARMOR_SIDE_HELMET;
                    else if (slotType == EquipmentSlot.CHEST) targetSlotIndex = SLOT_ARMOR_SIDE_CHEST;
                    else if (slotType == EquipmentSlot.LEGS) targetSlotIndex = SLOT_ARMOR_SIDE_LEGS;
                    else if (slotType == EquipmentSlot.FEET) targetSlotIndex = SLOT_ARMOR_SIDE_BOOTS;
                }

                boolean moved = targetSlotIndex != -1
                        && this.moveItemStackTo(sourceStack, targetSlotIndex, targetSlotIndex + 1, false);
                if (!moved && !this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END + 1, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (pIndex >= ARMOR_PANEL_START && pIndex <= ARMOR_PANEL_END) {
                if (!this.moveItemStackTo(sourceStack, SLOT_ARMOR_IN, SLOT_ARMOR_IN + 1, false)
                        && !this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END + 1, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (pIndex >= PLAYER_INVENTORY_START && pIndex <= PLAYER_HOTBAR_END) {
                if (sourceStack.getItem() instanceof ArmorItem) {
                    if (!this.moveItemStackTo(sourceStack, SLOT_ARMOR_IN, SLOT_ARMOR_IN + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (sourceStack.getItem() instanceof ItemArmorMod modItem) {
                    int modMenuSlot = modItem.type;
                    if (modMenuSlot < 0 || modMenuSlot >= MOD_SLOTS) {
                        return ItemStack.EMPTY;
                    }
                    Slot targetSlot = this.slots.get(modMenuSlot);
                    if (!targetSlot.hasItem() && targetSlot.mayPlace(sourceStack)) {
                        if (!this.moveItemStackTo(sourceStack, modMenuSlot, modMenuSlot + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                        pPlayer.level().playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                                ModSounds.REPAIR_RANDOM.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
                    } else if (pIndex >= PLAYER_INVENTORY_START && pIndex <= PLAYER_INVENTORY_END) {
                        if (!this.moveItemStackTo(sourceStack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (pIndex >= PLAYER_INVENTORY_START && pIndex <= PLAYER_INVENTORY_END) {
                    if (!this.moveItemStackTo(sourceStack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (sourceStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
                if (pIndex == SLOT_ARMOR_IN && !pPlayer.level().isClientSide) {
                    clearModSlots();
                }
            } else {
                slot.setChanged();
            }

            if (sourceStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(pPlayer, sourceStack);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(access, pPlayer, ModBlocks.ARMOR_TABLE.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 198));
        }
    }

    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private void addPlayerArmorSlots(Inventory playerInventory) {
        for (int i = 0; i < ARMOR_SLOTS.length; ++i) {
            final EquipmentSlot slotType = ARMOR_SLOTS[i];
            this.addSlot(new ArmorSidePanelSlot(
                    playerInventory,
                    39 - i,
                    -17, 36 + i * 18,
                    player,
                    slotType
            ));
        }
    }

    private class CentralArmorSlot extends Slot implements IHasTooltip {

        public CentralArmorSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public Component getEmptyTooltip() {
            return Component.translatable("armorMod.insertHere").withStyle(ChatFormatting.YELLOW);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ArmorItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void set(ItemStack pStack) {
            ItemStack oldStack = this.getItem();
            if (!ArmorTableMenu.this.player.level().isClientSide && !oldStack.isEmpty()
                    && (pStack.isEmpty() || !PlatformHooks.isSameItemSameTags(oldStack, pStack))) {
                ArmorModificationHelper.flushTableToArmor(oldStack, ArmorTableMenu.this.modsInventory, ArmorTableMenu.this.player);
            }

            // РљР°Рє 1.7.10 putStack: СЃРЅР°С‡Р°Р»Р° Р·Р°РіСЂСѓР·РёС‚СЊ РјРѕРґС‹ РІ СЃС‚РѕР», РїРѕС‚РѕРј РїРѕР»РѕР¶РёС‚СЊ Р±СЂРѕРЅСЋ
            if (!pStack.isEmpty()) {
                ArmorModificationHelper.loadModsIntoTable(pStack, ArmorTableMenu.this.modsInventory);
            }
            super.set(pStack);
        }

        @Override
        public void onTake(Player pPlayer, ItemStack pStack) {
            super.onTake(pPlayer, pStack);
            if (!pPlayer.level().isClientSide && !pStack.isEmpty()) {
                ArmorTableMenu.this.absorbModItemsFromTable(pStack);
            }
        }
    }

    private class ModificationSlot extends Slot implements IHasTooltip {

        private static final Map<Integer, String> TOOLTIP_KEYS = Map.of(
                ArmorModificationHelper.helmet_only, "armorMod.type.helmet",
                ArmorModificationHelper.plate_only, "armorMod.type.chestplate",
                ArmorModificationHelper.legs_only, "armorMod.type.leggings",
                ArmorModificationHelper.boots_only, "armorMod.type.boots",
                ArmorModificationHelper.servos, "armorMod.type.servo",
                ArmorModificationHelper.cladding, "armorMod.type.cladding",
                ArmorModificationHelper.kevlar, "armorMod.type.insert",
                ArmorModificationHelper.extra, "armorMod.type.special",
                ArmorModificationHelper.battery, "armorMod.type.battery"
        );

        private final int modType;

        public ModificationSlot(Container container, int modType, int x, int y) {
            super(container, modType, x, y);
            this.modType = modType;
        }

        @Override
        public Component getEmptyTooltip() {
            String key = TOOLTIP_KEYS.getOrDefault(modType, "");
            return Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!(stack.getItem() instanceof ItemArmorMod mod)) {
                return false;
            }
            ItemStack armorStack = ArmorTableMenu.this.armorInventory.getItem(0);
            if (armorStack.isEmpty()) {
                return false;
            }
            return mod.type == this.modType && ArmorModificationHelper.isApplicable(armorStack, stack);
        }

        @Override
        public void set(ItemStack pStack) {
            ItemStack oldStack = this.getItem();
            super.set(pStack);

            if (!pStack.isEmpty() && !ItemStack.matches(oldStack, pStack)) {
                ArmorTableMenu.this.player.level().playSound(null,
                        ArmorTableMenu.this.player.getX(), ArmorTableMenu.this.player.getY(), ArmorTableMenu.this.player.getZ(),
                        ModSounds.REPAIR_RANDOM.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
            }

            ItemStack armor = ArmorTableMenu.this.armorInventory.getItem(0);
            if (!armor.isEmpty() && !ArmorTableMenu.this.player.level().isClientSide) {
                if (!pStack.isEmpty() && ArmorModificationHelper.isApplicable(armor, pStack)) {
                    ArmorModificationHelper.applyMod(armor, pStack);
                } else if (pStack.isEmpty() && !oldStack.isEmpty()) {
                    ArmorModificationHelper.removeMod(armor, this.modType);
                }
                ArmorTableMenu.this.syncCenterArmorStack(armor);
            }
        }

        @Override
        public void onTake(Player pPlayer, ItemStack pStack) {
            super.onTake(pPlayer, pStack);
            pPlayer.level().playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                    ModSounds.EXTRACT_RANDOM.get(), SoundSource.PLAYERS, 0.4F, 1.0F);
            ItemStack armor = ArmorTableMenu.this.armorInventory.getItem(0);
            if (!armor.isEmpty() && !pPlayer.level().isClientSide) {
                ArmorModificationHelper.removeMod(armor, this.modType);
                ArmorTableMenu.this.syncCenterArmorStack(armor);
            }
        }
    }
}
