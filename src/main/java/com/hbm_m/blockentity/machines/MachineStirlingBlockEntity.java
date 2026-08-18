package com.hbm_m.blockentity.machines;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stirling Engine - Port von {@code TileEntityStirling}/{@code MachineStirling} (1.7.10 Original).
 * Eine einzelne Klasse fuer alle 3 Varianten (regulaer/Stahl/kreativ), unterschieden per Block-
 * Identitaet im Konstruktor - 1:1 wie im Original ("differentiated only by identity checks").
 * <p>
 * KEIN Brennstoff-Slot, KEIN GUI: die Original-Maschine zieht passiv Waerme vom Block direkt
 * darunter (sofern dieser {@link IHeatSource} implementiert, z.B. der Basic Boiler), wandelt sie
 * in Energie um und speist sie automatisch ins Energienetz ein (ueber
 * {@link BaseMachineBlockEntity}s eingebauten Provider-Mechanismus - {@code maxExtract&gt;0} macht die
 * Maschine zum reinen Energie-Erzeuger, kein manueller Push-Loop noetig).
 * <p>
 * Overspeed-Mechanik 1:1 aus dem Original uebernommen: haelt sich die gespeicherte Waerme &gt;60
 * Ticks ueber {@link #maxHeat}, ertoent eine Warnung; nach &gt;300 Ticks explodiert die Maschine und
 * schaltet sich ab ({@code hasCog=false}), bis sie mit einem {@code ModItems.GEAR_LARGE} per
 * Rechtsklick repariert wird. Die kreative Variante hat keine Obergrenze/Explosion.
 * <p>
 * SCOPE-Entscheidung: Das Original wirft bei der Explosion ein fliegendes {@code EntityCog}-Entity
 * aus (rein optisch) - dieser Port verzichtet auf ein eigenes Entity dafuer, die Explosion und die
 * Abschaltung/Reparatur-Mechanik selbst sind vollstaendig uebernommen. Ebenso hatte das Original
 * pro Variante ein anderes Zahnrad-Metadaten-Item als Reparatur-Voraussetzung - da dieser Port nur
 * ein generisches {@code gear_large}-Item kennt, repariert dieses alle 3 Varianten.
 */
public class MachineStirlingBlockEntity extends BaseMachineBlockEntity {

    private static final double DIFFUSION = 0.1D;
    private static final double EFFICIENCY = 0.5D;
    private static final int WARNING_TICKS = 60;
    private static final int OVERSPEED_LIMIT = 300;

    private static final int MAX_HEAT_NORMAL = 300;
    private static final int MAX_HEAT_STEEL = 1500;

    private final int maxHeat;
    private final boolean isCreative;

    private int heat = 0;
    private int overspeedTicks = 0;
    private boolean hasCog = true;

    public MachineStirlingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STIRLING_BE.get(), pos, state, 0,
                capacityFor(state), 0L, capacityFor(state));

        if (state.is(ModBlocks.STIRLING_CREATIVE.get())) {
            this.isCreative = true;
            this.maxHeat = Integer.MAX_VALUE;
        } else if (state.is(ModBlocks.STIRLING_STEEL.get())) {
            this.isCreative = false;
            this.maxHeat = MAX_HEAT_STEEL;
        } else {
            this.isCreative = false;
            this.maxHeat = MAX_HEAT_NORMAL;
        }
    }

    private static long capacityFor(BlockState state) {
        return 200_000L;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineStirlingBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        ensureNetworkInitialized();

        pullOrDecayHeat(level, pos);

        if (hasCog) {
            long gain = (long) (heat * (isCreative ? 1.0D : EFFICIENCY));
            if (gain > 0) {
                setEnergyStored(Math.min(getMaxEnergyStored(), getEnergyStored() + gain));
            }
        }

        handleOverspeed(level, pos);

        setChanged();
        sendUpdateToClient();
    }

    private void pullOrDecayHeat(Level level, BlockPos pos) {
        BlockEntity below = level.getBlockEntity(pos.below());
        if (below instanceof IHeatSource source && source.getHeatStored() > 0) {
            int pulled = (int) (source.getHeatStored() * DIFFUSION);
            if (pulled > 0) {
                source.useUpHeat(pulled);
                heat += pulled;
                return;
            }
        }
        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    private void handleOverspeed(ServerLevel level, BlockPos pos) {
        if (isCreative || !hasCog) return;

        if (heat > maxHeat) {
            overspeedTicks++;
            if (overspeedTicks == WARNING_TICKS) {
                level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 0.7F);
            }
            if (overspeedTicks > OVERSPEED_LIMIT) {
                explode(level, pos);
            }
        } else {
            overspeedTicks = 0;
        }
    }

    private void explode(ServerLevel level, BlockPos pos) {
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                5.0F, Level.ExplosionInteraction.BLOCK);
        hasCog = false;
        heat = 0;
        overspeedTicks = 0;
        setEnergyStored(0);
    }

    /** Rechtsklick mit passendem Zahnrad repariert die Maschine (1:1 aus dem Original). */
    public boolean tryRepair(Player player, ItemStack held) {
        if (hasCog || held.getItem() != ModItems.GEAR_LARGE.get()) return false;
        held.shrink(1);
        hasCog = true;
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        setChanged();
        sendUpdateToClient();
        return true;
    }

    public boolean hasCog() {
        return hasCog;
    }

    public int getHeat() {
        return heat;
    }

    public int getMaxHeat() {
        return maxHeat;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putInt("heat", heat);
        tag.putInt("overspeed_ticks", overspeedTicks);
        tag.putBoolean("has_cog", hasCog);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        heat = tag.getInt("heat");
        overspeedTicks = tag.getInt("overspeed_ticks");
        hasCog = !tag.contains("has_cog") || tag.getBoolean("has_cog");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.stirling");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false; // Kein Inventar - siehe Klassenkommentar.
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
            net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        return null; // Kein GUI im Original.
    }
}
