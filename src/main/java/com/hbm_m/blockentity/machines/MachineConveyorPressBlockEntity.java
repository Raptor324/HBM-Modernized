package com.hbm_m.blockentity.machines;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.recipe.PressRecipe;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Conveyor Press - Port von {@code TileEntityConveyorPress} (1.7.10 Original), das dritte
 * Mitglied der Press-Familie (siehe {@link MachinePressBlockEntity}/{@link MachineEPressBlockEntity}
 * fuer Stempel+Material -&gt; Ausgabe ueber dieselbe {@link PressRecipe}-Tabelle). Anders als die
 * beiden anderen hat das Original KEIN GUI - nur ein Stempel-Slot, per Rechtsklick befuellt
 * (Original: {@code onBlockActivated}/{@code onScrew}), und zieht sein Material automatisch von
 * ueber dem Block schwebenden Foerderband-Item-Entities statt aus einem manuellen Material-Slot;
 * die Ausgabe wird wieder als Foerderband-Item-Entity an derselben Position erzeugt.
 * <p>
 * SCOPE-Entscheidung: Das Original ist zusaetzlich selbst ein {@code IConveyorBelt} (Items koennen
 * ueber die Oberseite laufen wie auf einem normalen Band) und ein echtes 1x1x2-Multiblock. Dieser
 * Port vereinfacht auf einen Einzelblock (kein Multiblock, konsistent mit anderen stark
 * vereinfachten "grosses Modell, aber im Kern 1 Block"-Maschinen dieser Session, siehe
 * {@code MachineAmmoPressBlockEntity}) und implementiert NICHT selbst {@code IConveyorBelt} - der
 * Block sucht stattdessen aktiv nach {@link MovingConveyorItemEntity}s in einer AABB direkt ueber
 * und an seiner eigenen Position (die von einem angrenzenden echten Foerderband dorthin
 * transportiert wurden) und verarbeitet sie. Praktisch: ein Foerderband muss ueber/an den Block
 * heranfuehren, nicht zwingend direkt "durch" ihn hindurch wie im Original.
 */
public class MachineConveyorPressBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_STAMP = 0;
    private static final int SLOT_COUNT = 1;

    private static final long MAX_POWER = 50_000L;
    private static final long POWER_PER_OPERATION = 100L;

    public MachineConveyorPressBlockEntity(BlockPos pos, BlockState state) {
        super(com.hbm_m.blockentity.ModBlockEntities.CONVEYOR_PRESS_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineConveyorPressBlockEntity be) {
        if (level.isClientSide()) return;
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        ItemStack stamp = inventory.getStackInSlot(SLOT_STAMP);
        if (stamp.isEmpty() || getEnergyStored() < POWER_PER_OPERATION) return;

        AABB scanArea = new AABB(pos).inflate(0.1, 0.0, 0.1).expandTowards(0, 1.5, 0);
        List<MovingConveyorItemEntity> items = level.getEntitiesOfClass(MovingConveyorItemEntity.class, scanArea);

        for (MovingConveyorItemEntity itemEntity : items) {
            ItemStack material = itemEntity.getItem();
            if (material.isEmpty() || material.getCount() != 1) continue;

            Optional<PressRecipe> recipe = findRecipe(level, stamp, material);
            if (recipe.isEmpty()) continue;

            ItemStack output = recipe.get().getResultItem(level.registryAccess());
            itemEntity.discard();
            MovingConveyorItemEntity spawned = MovingConveyorItemEntity.create(level,
                    itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), output.copy());
            level.addFreshEntity(spawned);

            setEnergyStored(getEnergyStored() - POWER_PER_OPERATION);
            damageStamp(level, stamp);

            level.playSound(null, worldPosition, ModSounds.PRESS_OPERATE.get(), SoundSource.BLOCKS, 1.5f, 1.0f);
            setChanged();
            sendUpdateToClient();
            break; // ein Item pro Tick, analog zum Press-Extend/Retract-Rhythmus des Originals.
        }
    }

    private void damageStamp(Level level, ItemStack stamp) {
        if (!stamp.isDamageableItem()) return;
        stamp.setDamageValue(stamp.getDamageValue() + 1);
        if (stamp.getDamageValue() >= stamp.getMaxDamage()) {
            inventory.setStackInSlot(SLOT_STAMP, ItemStack.EMPTY);
            level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.5f, 0.8f);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Optional<PressRecipe> findRecipe(Level level, ItemStack stamp, ItemStack material) {
        SimpleContainer container = new SimpleContainer(3);
        container.setItem(1, stamp);
        container.setItem(2, material);

        RecipeType type = (RecipeType) PressRecipe.Type.INSTANCE;
        for (Object holderObj : level.getRecipeManager().getAllRecipesFor(type)) {
            if (holderObj instanceof PressRecipe recipe && recipe.matches(container, level)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    /** Rechtsklick mit Stempel installiert ihn (Original: {@code onBlockActivated}, kein GUI). */
    public boolean tryInsertStamp(Player player, ItemStack held) {
        if (!inventory.getStackInSlot(SLOT_STAMP).isEmpty() || held.isEmpty()) return false;
        inventory.setStackInSlot(SLOT_STAMP, held.split(1));
        setChanged();
        sendUpdateToClient();
        return true;
    }

    /** Schraubenzieher entfernt den Stempel (Original: {@code onScrew}). */
    public ItemStack removeStamp() {
        ItemStack stamp = inventory.getStackInSlot(SLOT_STAMP);
        if (!stamp.isEmpty()) {
            inventory.setStackInSlot(SLOT_STAMP, ItemStack.EMPTY);
            setChanged();
            sendUpdateToClient();
        }
        return stamp;
    }

    public ItemStack getStampStack() {
        return inventory.getStackInSlot(SLOT_STAMP);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.conveyor_press");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_STAMP;
    }

    @Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
            net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        return null; // Kein GUI im Original - reine Rechtsklick/Schraubenzieher-Bedienung.
    }
}
