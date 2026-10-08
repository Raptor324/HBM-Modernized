package com.hbm_m.blockentity.machines;

import com.hbm_m.platform.RenderBounds;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.inventory.menu.MissileAssemblyMenu;
import com.hbm_m.item.missile.ItemCustomMissile;
import com.hbm_m.item.missile.ItemCustomMissilePart;
import com.hbm_m.item.missile.ItemCustomMissilePart.FuelType;
import com.hbm_m.item.missile.ItemCustomMissilePart.PartType;
import com.hbm_m.item.missile.MissileStruct;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineMissileAssembly}: Chip (0), Sprengkopf (1), Rumpf (2), Leitwerk (3, optional) und
 * Triebwerk (4) werden auf Passung geprueft und auf Knopfdruck zu einer {@code missile_custom} (5) verbaut. Die
 * Anschlussgroessen muessen passen, das Triebwerk muss den Treibstoff des Rumpfs verbrennen und mindestens so viel
 * Schub liefern wie der Sprengkopf wiegt. Der Slotinhalt wird an die Clients gespiegelt (Original:
 * TEMissileMultipartPacket), damit der Renderer die halbfertige Rakete auf dem Geruest zeigt.
 */
public class MissileAssemblyBlockEntity extends BaseHbmBlockEntity implements MenuProvider {

    public static final int SLOT_CHIP = 0;
    public static final int SLOT_WARHEAD = 1;
    public static final int SLOT_FUSELAGE = 2;
    public static final int SLOT_FINS = 3;
    public static final int SLOT_THRUSTER = 4;
    public static final int SLOT_OUTPUT = 5;
    private static final int SLOT_COUNT = 6;

    private String customName;

    private final ModItemStackHandler inventory = new ModItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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

    private ItemStack slot(int i) {
        return inventory.getStackInSlot(i);
    }

    @Nullable
    private ItemCustomMissilePart part(int i) {
        return slot(i).getItem() instanceof ItemCustomMissilePart p ? p : null;
    }

    /** Der aktuelle Slotinhalt als Raketenverbund (Renderer, GUI-Vorschau). */
    public MissileStruct getStruct() {
        return new MissileStruct(slot(SLOT_WARHEAD), slot(SLOT_FUSELAGE), slot(SLOT_FINS), slot(SLOT_THRUSTER));
    }

    public int fuselageState() {
        ItemCustomMissilePart part = part(SLOT_FUSELAGE);
        return part != null && part.type == PartType.FUSELAGE ? 1 : 0;
    }

    public int chipState() {
        ItemCustomMissilePart part = part(SLOT_CHIP);
        return part != null && part.type == PartType.CHIP ? 1 : 0;
    }

    public int warheadState() {
        ItemCustomMissilePart part = part(SLOT_WARHEAD);
        ItemCustomMissilePart fuselage = part(SLOT_FUSELAGE);
        ItemCustomMissilePart thruster = part(SLOT_THRUSTER);

        if (part != null && fuselage != null && thruster != null) {
            if (part.type == PartType.WARHEAD && fuselage.type == PartType.FUSELAGE && thruster.type == PartType.THRUSTER) {
                float weight = (Float) part.attributes[2];
                float thrust = (Float) thruster.attributes[2];

                if (part.bottom == fuselage.top && weight <= thrust) return 1;
            }
        }
        return 0;
    }

    public int stabilityState() {
        if (slot(SLOT_FINS).isEmpty()) return -1;

        ItemCustomMissilePart part = part(SLOT_FINS);
        ItemCustomMissilePart fuselage = part(SLOT_FUSELAGE);
        if (part != null && fuselage != null) {
            if (part.top == fuselage.bottom && part.type == PartType.FINS) return 1;
        }
        return 0;
    }

    public int thrusterState() {
        ItemCustomMissilePart part = part(SLOT_THRUSTER);
        ItemCustomMissilePart fuselage = part(SLOT_FUSELAGE);
        if (part != null && fuselage != null) {
            if (part.type == PartType.THRUSTER && fuselage.type == PartType.FUSELAGE
                    && part.top == fuselage.bottom && (FuelType) part.attributes[0] == (FuelType) fuselage.attributes[0]) {
                return 1;
            }
        }
        return 0;
    }

    public boolean canBuild() {
        if (slot(SLOT_OUTPUT).isEmpty() && chipState() == 1 && warheadState() == 1 && fuselageState() == 1 && thrusterState() == 1) {
            return stabilityState() != 0;
        }
        return false;
    }

    public void construct() {
        if (!canBuild() || level == null) return;

        inventory.setStackInSlot(SLOT_OUTPUT, ItemCustomMissile.buildMissile(slot(SLOT_CHIP), slot(SLOT_WARHEAD), slot(SLOT_FUSELAGE), slot(SLOT_FINS), slot(SLOT_THRUSTER)).copy());

        if (stabilityState() == 1) inventory.setStackInSlot(SLOT_FINS, ItemStack.EMPTY);

        inventory.setStackInSlot(SLOT_CHIP, ItemStack.EMPTY);
        inventory.setStackInSlot(SLOT_WARHEAD, ItemStack.EMPTY);
        inventory.setStackInSlot(SLOT_FUSELAGE, ItemStack.EMPTY);
        inventory.setStackInSlot(SLOT_THRUSTER, ItemStack.EMPTY);

        level.playSound(null, worldPosition, ModSounds.MISSILE_ASSEMBLY2.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        setChanged();
    }

    public void dropInventoryContents() {
        if (level == null) return;
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            c.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, c);
    }

    public void setCustomName(String name) {
        this.customName = name;
        setChanged();
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(inventory, registries));
        if (customName != null) tag.putString("name", customName);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("inventory")) {
            com.hbm_m.platform.ItemStackSerialization.deserialize(inventory, tag.getCompound("inventory"), registries);
        }
        customName = tag.contains("name") ? tag.getString("name") : null;
    }

    // Phase D: auf NeoForge ueber RenderBoundsProvider (HbmBerBounds), daher Methode auf beiden Loadern
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return RenderBounds.INFINITE;
    }

    @Override
    public Component getDisplayName() {
        return customName != null && !customName.isEmpty() ? Component.literal(customName) : Component.translatable("container.missileAssembly");
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
