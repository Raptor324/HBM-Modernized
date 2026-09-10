package com.hbm_m.blockentity.machines;

import java.util.List;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1-Port von {@code TileEntityCharger} (1.7.10): eine Ladeplatte, die die Ausruestung der
 * darauf stehenden Spieler auflaedt.
 *
 * <p>Der Ablauf des Originals ist uebernommen: die Platte faehrt erst ueber {@link #DELAY} Ticks
 * aus, bevor sie Energie abgibt, und faehrt ebenso wieder ein, wenn niemand mehr etwas zu laden
 * hat. Geladen werden Haupthand und die vier Ruestungsteile - im Original sind das die
 * Ausruestungsplaetze 0 bis 4.</p>
 *
 * <p><b>Abweichung:</b> Das Original spricht Gegenstaende ueber {@code IBatteryItem} an; dieser
 * Port nutzt durchweg Energie-Faehigkeiten, darum laeuft das Laden ueber
 * {@link BaseMachineBlockEntity#chargeStack}. Wirkung und Reihenfolge bleiben gleich.</p>
 */
public class ChargerBlockEntity extends BaseMachineBlockEntity {

    private static final long MAX_POWER = 100_000L;
    /** Original: {@code delay = 20} - so lange braucht die Platte zum Ausfahren. */
    private static final int DELAY = 20;
    /** Original: {@code Math.max(power / 5, 1)} je Gegenstand und Tick. */
    private static final int SPLIT = 5;

    private int usingTicks = 0;
    private int lastUsingTicks = 0;
    /** Original: {@code lastOp} - haelt die Partikel/Klaenge noch ein paar Ticks am Laufen. */
    private int lastOp = 0;

    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGER_BE.get(), pos, state, 0, MAX_POWER, MAX_POWER, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ChargerBlockEntity be) {
        if (level.isClientSide()) return;

        be.ensureNetworkInitialized();

        List<Player> players = level.getEntitiesOfClass(Player.class, chargeArea(pos));

        // Original: erst zaehlen, wieviel ueberhaupt gebraucht wird - daran haengt das Ausfahren.
        boolean anythingToCharge = false;
        for (Player player : players) {
            for (ItemStack stack : chargeables(player)) {
                if (!stack.isEmpty()) { anythingToCharge = true; break; }
            }
            if (anythingToCharge) break;
        }

        boolean particles = be.lastOp > 0;
        if (particles) {
            be.lastOp--;
            if (level.getGameTime() % 20 == 0) {
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 0.5F);
            }
        }

        be.lastUsingTicks = be.usingTicks;

        // Original: Ausfahren, solange etwas zu laden ist; sonst wieder einfahren.
        if ((anythingToCharge || particles) && be.usingTicks < DELAY) {
            be.usingTicks++;
            if (be.usingTicks == 2) {
                level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.5F);
            }
        }
        if (!anythingToCharge && !particles && be.usingTicks > 0) {
            be.usingTicks--;
            if (be.usingTicks == 4) {
                level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, 0.5F);
            }
        }

        // Original: erst bei voll ausgefahrener Platte fliesst Strom.
        if (be.usingTicks >= DELAY && be.getEnergyStored() > 0) {
            be.doCharge(players);
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    /** Original: {@code transferPower} - jeder Gegenstand bekommt hoechstens ein Fuenftel. */
    private void doCharge(List<Player> players) {
        for (Player player : players) {
            for (ItemStack stack : chargeables(player)) {
                if (stack.isEmpty()) continue;
                if (getEnergyStored() <= 0) return;

                long portion = Math.max(getEnergyStored() / SPLIT, 1L);
                if (chargeStack(stack, portion) > 0) {
                    lastOp = 4;
                }
            }
        }
    }

    /** Original: Ausruestungsplaetze 0 bis 4 - Haupthand und die vier Ruestungsteile. */
    private static Iterable<ItemStack> chargeables(Player player) {
        List<ItemStack> stacks = new java.util.ArrayList<>();
        stacks.add(player.getMainHandItem());
        player.getArmorSlots().forEach(stacks::add);
        return stacks;
    }

    /** Original: eine flache Box auf der Platte, waagerecht um einen halben Block erweitert. */
    private static AABB chargeArea(BlockPos pos) {
        return new AABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                .inflate(0.5D, 0.0D, 0.5D);
    }

    public int getUsingTicks()     { return usingTicks; }
    public int getLastUsingTicks() { return lastUsingTicks; }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false; // Original: kein Inventar.
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("usingTicks", usingTicks);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        usingTicks = tag.getInt("usingTicks");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.charger");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
            int id, net.minecraft.world.entity.player.Inventory inv, Player player) {
        return null; // Original: kein GUI.
    }
}
