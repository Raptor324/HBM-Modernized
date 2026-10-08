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
 * <p>Sie steht wie im Original <b>drei Felder hoch</b> ({@code getDimensions {2,0,0,0,0,0}}), und
 * ihre Oberseite ist selbst ein Foerderband: Gegenstaende laufen oben durch und werden im
 * Vorbeigehen gepresst. Man haengt sie also mitten in eine Bandstrecke, statt sie zu befuellen -
 * genau dafuer ist sie da.
 */
public class MachineConveyorPressBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_STAMP = 0;
    private static final int SLOT_COUNT = 1;

    /** Original: {@code maxPower = 50000}, {@code usage = 100} HE je Hubtick, {@code speed = 0.125} je Tick. */
    private static final long MAX_POWER = 50_000L;
    private static final long POWER_PER_OPERATION = 100L;
    private static final double SPEED = 0.125;

    /** Original: Kolbenstellung 0..1, Richtung und Pause zwischen den Richtungswechseln. */
    public double press;
    public boolean isRetracting = false;
    private int delay;
    // Client: Annaeherungs-Interpolation wie im Original
    public double renderPress;
    public double lastPress;
    private double syncPress;
    private int turnProgress;

    public MachineConveyorPressBlockEntity(BlockPos pos, BlockState state) {
        super(com.hbm_m.blockentity.ModBlockEntities.CONVEYOR_PRESS_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineConveyorPressBlockEntity be) {
        if (level.isClientSide()) {
            be.lastPress = be.renderPress;
            if (be.turnProgress > 0) {
                be.renderPress = be.renderPress + ((be.syncPress - be.renderPress) / (double) be.turnProgress);
                --be.turnProgress;
            } else {
                be.renderPress = be.syncPress;
            }
            return;
        }
        be.serverTick(level, pos);
    }

    /** 1:1 Original {@code updateEntity}: ausfahren bis 1, pressen, 5 Ticks Pause, einfahren bis 0. */
    private void serverTick(Level level, BlockPos pos) {
        ensureNetworkInitialized();
        double prev = press;

        if (delay <= 0) {
            if (isRetracting) {
                if (getEnergyStored() >= POWER_PER_OPERATION) {
                    press -= SPEED;
                    setEnergyStored(getEnergyStored() - POWER_PER_OPERATION);
                    if (press <= 0) {
                        press = 0;
                        isRetracting = false;
                        delay = 0;
                    }
                }
            } else {
                if (canExtend(level, pos)) {
                    press += SPEED;
                    setEnergyStored(getEnergyStored() - POWER_PER_OPERATION);
                    if (press >= 1) {
                        press = 1;
                        isRetracting = true;
                        delay = 5;
                        process(level, pos);
                    }
                }
            }
        } else {
            delay--;
        }

        if (prev != press) {
            setChanged();
            sendUpdateToClient();
        }
    }

    private List<MovingConveyorItemEntity> itemsOnTop(Level level, BlockPos pos) {
        AABB area = new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1.5, pos.getZ() + 1);
        return level.getEntitiesOfClass(MovingConveyorItemEntity.class, area);
    }

    /** Original {@code canExtend}: Strom, Stempel und ein passendes Einzelstueck oben; mittig liegende werden zentriert. */
    private boolean canExtend(Level level, BlockPos pos) {
        if (getEnergyStored() < POWER_PER_OPERATION) return false;
        ItemStack stamp = inventory.getStackInSlot(SLOT_STAMP);
        if (stamp.isEmpty()) return false;

        for (MovingConveyorItemEntity item : itemsOnTop(level, pos)) {
            ItemStack stack = item.getItem();
            if (stack.getCount() == 1 && findRecipe(level, stamp, stack).isPresent()) {
                double d0 = 0.35;
                double d1 = 0.65;
                if (item.getX() > pos.getX() + d0 && item.getX() < pos.getX() + d1
                        && item.getZ() > pos.getZ() + d0 && item.getZ() < pos.getZ() + d1) {
                    item.setPos(pos.getX() + 0.5, item.getY(), pos.getZ() + 0.5);
                }
                return true;
            }
        }
        return false;
    }

    /** Original {@code process}: alle passenden Einzelstuecke oben werden gepresst, Stempel nutzt sich ab. */
    private void process(Level level, BlockPos pos) {
        ItemStack stamp = inventory.getStackInSlot(SLOT_STAMP);
        if (stamp.isEmpty()) return;

        for (MovingConveyorItemEntity item : itemsOnTop(level, pos)) {
            ItemStack stack = item.getItem();
            if (stack.getCount() != 1) continue;
            Optional<PressRecipe> recipe = findRecipe(level, stamp, stack);
            if (recipe.isEmpty()) continue;
            ItemStack output = recipe.get().getResultItem(level.registryAccess());
            item.discard();
            MovingConveyorItemEntity spawned = MovingConveyorItemEntity.create(level, item.getX(), item.getY(), item.getZ(), output.copy());
            level.addFreshEntity(spawned);
        }

        level.playSound(null, worldPosition, ModSounds.PRESS_OPERATE.get(), SoundSource.BLOCKS, 1.5f, 1.0f);

        // Original: Stempel verschleisst, ohne eigenes Bruchgeraeusch
        if (stamp.isDamageableItem()) {
            stamp.setDamageValue(stamp.getDamageValue() + 1);
            if (stamp.getDamageValue() >= stamp.getMaxDamage()) {
                inventory.setStackInSlot(SLOT_STAMP, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void writeNbtData(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putDouble("press", press);
        tag.putBoolean("ret", isRetracting);
        tag.putInt("delay", delay);
    }

    @Override
    protected void readNbtData(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        double p = tag.getDouble("press");
        isRetracting = tag.getBoolean("ret");
        delay = tag.getInt("delay");
        if (level != null && level.isClientSide) {
            syncPress = p;
            turnProgress = 2;
        } else {
            press = p;
        }
    }

    public double getRenderPress(float interp) {
        return lastPress + (renderPress - lastPress) * interp;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Optional<PressRecipe> findRecipe(Level level, ItemStack stamp, ItemStack material) {
        SimpleContainer container = new SimpleContainer(3);
        container.setItem(1, stamp);
        container.setItem(2, material);

        // 1.21.1: Recipe.matches требует RecipeInput, а рецепты завёрнуты в RecipeHolder —
        // используем RecipeHooks.getAllRecipes + matchesRecipe(RecipeInputWrapper).
        for (PressRecipe recipe : com.hbm_m.platform.recipe.RecipeHooks.getAllRecipes(level, (RecipeType<PressRecipe>) (RecipeType<?>) PressRecipe.Type.INSTANCE)) {
            if (recipe.matchesRecipe(new com.hbm_m.platform.recipe.RecipeInputWrapper(container), level)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    /** Rechtsklick mit Stempel installiert ihn (Original: {@code onBlockActivated}, kein GUI). */
    public boolean tryInsertStamp(Player player, ItemStack held) {
        if (!inventory.getStackInSlot(SLOT_STAMP).isEmpty() || held.isEmpty()) return false;
        inventory.setStackInSlot(SLOT_STAMP, held.split(1));
        // Original MachineConveyorPress.onBlockActivated: Steckgeraeusch beim Einsetzen des Stempels
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    com.hbm_m.sound.HbmSoundsNT.get("hbm:item.upgradePlug"), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
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

    //? if forge {
    /** Original {@code ISidedInventory}: Slot {0}; nur Stempel hinein, nichts heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return stack.getItem() instanceof com.hbm_m.item.industrial.ItemStamp; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
            });

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?} elif neoforge {
    /*/^* Original {@code ISidedInventory}: Slot {0}; nur Stempel hinein, nichts heraus. ^/
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return stack.getItem() instanceof com.hbm_m.item.industrial.ItemStamp; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
            });

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sidedItems.invalidate();
    }
    *///?}
}
