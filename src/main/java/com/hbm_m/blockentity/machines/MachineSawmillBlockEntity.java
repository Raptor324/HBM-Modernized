package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sawmill: Direktport der Kernlogik aus {@code TileEntitySawmill} (1.7.10 Original) - ein rein
 * automatisierungsgesteuerter Block ohne Spieler-GUI (das Original hat keine
 * {@code provideContainer}/{@code provideGUI}-Implementierung, nur Hopper-Zugriff ueber
 * {@code getAccessibleSlotsFromSide}), daher entfaellt Menu/GUI hier ebenfalls vollstaendig.
 * <p>
 * Grosse Vereinfachung: das Original wird von einem {@code IHeatSource}-Block darunter gespeist
 * (Heiznetzwerk-Mechanik, kein eigener Brennstoff-/Batterie-Slot existiert im Original ueberhaupt).
 * Dieses Heiznetzwerk existiert in diesem Port nicht (siehe gleiche Feststellung bei
 * {@link MachineFurnaceSteelBlockEntity}). Da es im Original ohnehin keinen Slot gibt, der eine
 * andere Energiequelle simulieren koennte, verarbeitet dieser Port passiv ohne Energiebedarf
 * (immer "voll beheizt") statt eine im Original nicht vorhandene Stromquelle zu erfinden. Die
 * daran gekoppelten Original-Mechaniken Ueberhitzung/Explosion+Sagezahn-Projektil-Auswurf und die
 * Entitaeten-Schadens-AoE der rotierenden Klinge entfallen ersatzlos, da beide direkt von der
 * fehlenden Hitze-Eskalation abhaengen.
 */
public class MachineSawmillBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_BYPRODUCT = 2;
    private static final int SLOT_COUNT = 3;
    private static final int PROCESSING_TIME = 600;

    public int progress = 0;

    private final ModItemStackHandler inventory = new ModItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot != SLOT_INPUT) return false;
            return getStackInSlot(SLOT_INPUT).isEmpty() && getStackInSlot(SLOT_OUTPUT).isEmpty()
                    && getStackInSlot(SLOT_BYPRODUCT).isEmpty() && stack.getCount() == 1 && getOutput(stack) != null;
        }
    };

    public MachineSawmillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAWMILL_BE.get(), pos, state);
    }

    public ModItemStackHandler getInventory() { return inventory; }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSawmillBlockEntity be) {
        if (level.isClientSide()) return;

        ItemStack input = be.inventory.getStackInSlot(SLOT_INPUT);
        ItemStack result = be.getOutput(input);

        if (result != null) {
            be.progress++;
            if (be.progress >= PROCESSING_TIME) {
                be.progress = 0;
                be.inventory.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
                be.inventory.setStackInSlot(SLOT_OUTPUT, result);

                if (result.getItem() != ModItems.POWDER_SAWDUST.get()) {
                    float chance = result.getItem() == Items.STICK ? 0.1F : 0.5F;
                    if (level.getRandom().nextFloat() < chance) {
                        be.inventory.setStackInSlot(SLOT_BYPRODUCT, new ItemStack(ModItems.POWDER_SAWDUST.get()));
                    }
                }
                be.setChanged();
            }
        } else {
            be.progress = 0;
        }
    }

    /** Port of {@code TileEntitySawmill.getOutput}, using item tags instead of 1.7.10 OreDict names. */
    @org.jetbrains.annotations.Nullable
    private ItemStack getOutput(ItemStack input) {
        if (input.isEmpty()) return null;

        if (input.is(ItemTags.LOGS)) {
            // Original: any vanilla crafting recipe matching the log, output x 6/4 (4 planks -> 6).
            return new ItemStack(Items.OAK_PLANKS, 6);
        }
        if (input.is(ItemTags.PLANKS)) {
            return new ItemStack(Items.STICK, 6);
        }
        if (input.is(Items.STICK)) {
            return new ItemStack(ModItems.POWDER_SAWDUST.get());
        }
        if (input.is(ItemTags.SAPLINGS)) {
            return new ItemStack(Items.STICK, 1);
        }
        return null;
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, container);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(inventory, registries));
        tag.putInt("progress", progress);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        com.hbm_m.platform.ItemStackSerialization.deserialize(inventory, tag.getCompound("inventory"), registries);
        progress = tag.getInt("progress");
    }
}
