package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.inventory.menu.MissileAssemblyMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.missile.MissileItem;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * MVP-Port der 1.7.10 Missile-Assembly-Maschine ({@code TileEntityMachineMissileAssembly}).
 * <p>
 * Das Original prueft physische "Connector"-IDs zwischen Fuselage/Warhead/Fins/Thruster
 * (Top/Bottom-Pins + Treibstoff-Typ-Abgleich). Diese Attribute existieren in diesem Port
 * nicht (Teile sind einfache Items ohne NBT-Attribute), daher wird hier stattdessen anhand
 * der Item-Namen eine Groessen-Klasse (SMALL/MEDIUM/LARGE/NUCLEAR) abgeleitet und Warhead-
 * gegen Thruster-Klasse verglichen. Das Ergebnis ist eines der bereits registrierten
 * {@link MissileItem}-Presets statt einer frei kombinierten NBT-Rakete.
 */
public class MissileAssemblyBlockEntity extends BaseHbmBlockEntity implements MenuProvider {

    public static final int SLOT_CHIP = 0;
    public static final int SLOT_WARHEAD = 1;
    public static final int SLOT_FUSELAGE = 2;
    public static final int SLOT_FINS = 3;
    public static final int SLOT_THRUSTER = 4;
    public static final int SLOT_OUTPUT = 5;
    private static final int SLOT_COUNT = 6;

    private final ModItemStackHandler inventory = new ModItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot != SLOT_OUTPUT;
        }
    };

    public MissileAssemblyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MISSILE_ASSEMBLY_BE.get(), pos, state);
    }

    public ModItemStackHandler getInventory() {
        return inventory;
    }

    private enum Size { SMALL, MEDIUM, LARGE, NUCLEAR, UNKNOWN }

    private static Size sizeOf(ItemStack stack) {
        if (stack.isEmpty()) return Size.UNKNOWN;
        String id = stack.getItem().builtInRegistryHolder().key().location().getPath();
        if (id.contains("nuclear") || id.contains("mirv") || id.contains("volcano")) return Size.NUCLEAR;
        if (id.contains("_small")) return Size.SMALL;
        if (id.contains("_medium")) return Size.MEDIUM;
        if (id.contains("_large")) return Size.LARGE;
        return Size.UNKNOWN;
    }

    private static boolean isWarhead(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().builtInRegistryHolder().key().location().getPath().startsWith("warhead_");
    }

    private static boolean isThruster(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().builtInRegistryHolder().key().location().getPath().startsWith("thruster_");
    }

    private static boolean isFins(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().builtInRegistryHolder().key().location().getPath().startsWith("fins_");
    }

    public int chipState() {
        return inventory.getStackInSlot(SLOT_CHIP).is(ModItems.MISSILE_CHIP.get()) ? 1 : 0;
    }

    public int fuselageState() {
        return inventory.getStackInSlot(SLOT_FUSELAGE).is(ModItems.MISSILE_FUSELAGE.get()) ? 1 : 0;
    }

    public int thrusterState() {
        return (fuselageState() == 1 && isThruster(inventory.getStackInSlot(SLOT_THRUSTER))) ? 1 : 0;
    }

    /** -1 = leer (kein Fins-Teil erforderlich), 0 = ungueltiges Teil, 1 = gueltig. */
    public int stabilityState() {
        ItemStack fins = inventory.getStackInSlot(SLOT_FINS);
        if (fins.isEmpty()) return -1;
        return isFins(fins) ? 1 : 0;
    }

    public int warheadState() {
        ItemStack warhead = inventory.getStackInSlot(SLOT_WARHEAD);
        ItemStack thruster = inventory.getStackInSlot(SLOT_THRUSTER);
        if (!isWarhead(warhead) || !isThruster(thruster)) return 0;
        Size warheadSize = sizeOf(warhead);
        Size thrusterSize = sizeOf(thruster);
        return (warheadSize != Size.UNKNOWN && warheadSize == thrusterSize) ? 1 : 0;
    }

    public boolean canBuild() {
        if (!inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) return false;
        return chipState() == 1 && warheadState() == 1 && fuselageState() == 1
                && thrusterState() == 1 && stabilityState() != 0;
    }

    public void construct() {
        if (!canBuild() || level == null) return;

        ItemStack warhead = inventory.getStackInSlot(SLOT_WARHEAD);
        Item resultMissile = resolveMissile(warhead);

        inventory.setStackInSlot(SLOT_OUTPUT, new ItemStack(resultMissile));
        inventory.extractItem(SLOT_CHIP, 1, false);
        inventory.extractItem(SLOT_WARHEAD, 1, false);
        inventory.extractItem(SLOT_FUSELAGE, 1, false);
        inventory.extractItem(SLOT_THRUSTER, 1, false);
        if (stabilityState() == 1) {
            inventory.extractItem(SLOT_FINS, 1, false);
        }

        level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        setChanged();
    }

    private static Item resolveMissile(ItemStack warhead) {
        String id = warhead.getItem().builtInRegistryHolder().key().location().getPath();
        Size size = sizeOf(warhead);

        if (size == Size.NUCLEAR) {
            if (id.contains("mirv")) return ModItems.MISSILE_NUCLEAR_CLUSTER.get();
            if (id.contains("volcano")) return ModItems.MISSILE_VOLCANO.get();
            return ModItems.MISSILE_NUCLEAR.get();
        }
        if (size == Size.LARGE) {
            if (id.contains("incendiary")) return ModItems.MISSILE_INCENDIARY_STRONG.get();
            if (id.contains("cluster")) return ModItems.MISSILE_CLUSTER_STRONG.get();
            if (id.contains("buster")) return ModItems.MISSILE_BUSTER_STRONG.get();
            return ModItems.MISSILE_STRONG.get();
        }
        if (size == Size.MEDIUM) {
            if (id.contains("incendiary")) return ModItems.MISSILE_INCENDIARY.get();
            if (id.contains("cluster")) return ModItems.MISSILE_CLUSTER.get();
            if (id.contains("buster")) return ModItems.MISSILE_BUSTER.get();
            return ModItems.MISSILE_GENERIC.get();
        }
        return ModItems.MISSILE_MICRO.get();
    }

    public void dropInventoryContents() {
        if (level == null) return;
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            c.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, c);
    }

    
    // (устраняет вложенный stonecutter-баг в load())
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(inventory, registries));
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("inventory")) {
            com.hbm_m.platform.ItemStackSerialization.deserialize(inventory, tag.getCompound("inventory"), registries);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.missile_assembly");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MissileAssemblyMenu(id, inv, this);
    }

    public ModItemStackHandlerContainer asContainer() {
        return new ModItemStackHandlerContainer(inventory, this::setChanged);
    }
}
