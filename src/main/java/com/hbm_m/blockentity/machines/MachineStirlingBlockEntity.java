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
 * <p><b>Geht er durch, fliegt das Zahnrad heraus</b>
 * ({@link com.hbm_m.entity.projectile.CogEntity}) - und das ist kein Effekt: es toetet, was es
 * trifft, sprengt beim Aufprall und laesst sich danach wieder aufsammeln und einbauen. Je heisser
 * der Motor beim Platzen war, desto weiter fliegt es.</p>
 *
 * <p>Wie im Original verlangt jede Bauart ihr eigenes Zahnrad ({@code getGeatMeta}): der normale Motor
 * {@code gear_large}, der Stahlmotor {@code gear_large_steel}. Meta 2 (kreativ) gibt es als Gegenstand nicht; hier
 * gilt dafuer {@code gear_large}.
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
    /** Original {@code warnCooldown}. */
    private int warnCooldown = 0;
    private boolean hasCog = true;
    /** Waerme des letzten Ticks fuer den Client (Original serialisiert {@code heat} vor dem Nullsetzen). */
    private int syncHeat = 0;

    /** Original {@code powerBuffer}: synchronisiert, treibt auf dem Client die Zahnraddrehung (RenderStirling). */
    private long powerBuffer = 0;
    /** Original {@code spin/lastSpin} - nur Client. */
    public float spin;
    public float lastSpin;

    public MachineStirlingBlockEntity(BlockPos pos, BlockState state) {
        // Abgabe unbegrenzt: Original-tryProvide liefert den ganzen Puffer (kreativ ohne Obergrenze)
        super(ModBlockEntities.STIRLING_BE.get(), pos, state, 0,
                capacityFor(state), 0L, Long.MAX_VALUE);

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
        if (level.isClientSide()) {
            be.clientTick();
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos);
    }

    /** Original updateEntity (isRemote-Zweig): momentum = powerBuffer * 50 / maxHeat, kreativ max. 45. */
    private void clientTick() {
        float momentum = powerBuffer * 50F / ((float) renderMaxHeat());
        if (isCreative) momentum = Math.min(momentum, 45F);

        this.lastSpin = this.spin;
        this.spin += momentum;

        if (this.spin >= 360F) {
            this.spin -= 360F;
            this.lastSpin -= 360F;
        }
    }

    /** Original {@code maxHeat()}: 300 fuer den normalen Motor, sonst 1500 (auch kreativ). */
    public int renderMaxHeat() {
        return getBlockState().is(ModBlocks.STIRLING.get()) ? 300 : 1500;
    }

    /**
     * 1:1 Original {@code updateEntity}: der Motor speichert nichts - {@code getPower() == getMaxPower() == powerBuffer},
     * der Puffer wird jeden Tick aus der Waerme dieses Ticks neu gesetzt ({@code heat * efficiency}), was das Netz
     * nicht abnimmt, verfaellt. Die Waerme selbst wird am Tickende auf 0 gesetzt.
     */
    private void serverTick(ServerLevel level, BlockPos pos) {
        ensureNetworkInitialized();

        if (hasCog) {
            this.powerBuffer = 0;
            pullOrDecayHeat(level, pos);
            this.powerBuffer = (long) (heat * (isCreative ? 1.0D : EFFICIENCY));

            if (warnCooldown > 0) warnCooldown--;

            handleOverspeed(level, pos);
        } else {
            this.overspeedTicks = 0;
            this.warnCooldown = 0;
        }

        // Original networkPackNT vor dem Nullsetzen: der Client sieht die Waerme dieses Ticks
        this.syncHeat = this.heat;
        setChanged();
        sendUpdateToClient();

        if (hasCog) {
            // Port-Energiemodell: Speicher == Puffer dieses Ticks
            setEnergyCapacity(Math.max(0L, powerBuffer));
            setEnergyStored(powerBuffer);
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                // Original getConPos: je 2 Bloecke vom Kern in alle vier Himmelsrichtungen
                BlockPos con = pos.relative(d, 2);
                this.tryProvide(level, con.getX(), con.getY(), con.getZ(), d);
            }
        } else {
            if (this.powerBuffer > 0) this.powerBuffer--;
            setEnergyCapacity(Math.max(0L, powerBuffer));
            setEnergyStored(powerBuffer);
        }

        this.heat = 0;
    }

    /** Original: nur mit Zahnrad wird an die Anschluesse geliefert. */
    @Override
    public long getProvideSpeed() {
        return hasCog ? powerBuffer : 0L;
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

    /** Original: ab 60 Ticks Ueberdrehzahl Warnton (alle 100 Ticks), ab 300 fliegt das Zahnrad heraus. */
    private void handleOverspeed(ServerLevel level, BlockPos pos) {
        if (heat > maxHeat && !isCreative) {
            overspeedTicks++;
            if (overspeedTicks > WARNING_TICKS && warnCooldown == 0) {
                warnCooldown = 100;
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                        com.hbm_m.sound.HbmSoundsNT.get("hbm:block.warnOverspeed"), SoundSource.BLOCKS, 2.0F, 1.0F);
            }
            if (overspeedTicks > OVERSPEED_LIMIT) {
                explode(level, pos);
            }
        } else {
            overspeedTicks = 0;
        }
    }

    private void explode(ServerLevel level, BlockPos pos) {
        // Original: newExplosion(..., 5F, false, false) - ohne Feuer und ohne Blockschaden
        level.explode(null, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                5.0F, Level.ExplosionInteraction.NONE);

        // 1:1: das Zahnrad fliegt seitlich heraus - je heisser, desto hoeher.
        net.minecraft.core.Direction facing =
                getBlockState().hasProperty(com.hbm_m.block.machines.DummyableMachineBlock.FACING)
                        ? getBlockState().getValue(com.hbm_m.block.machines.DummyableMachineBlock.FACING)
                        : net.minecraft.core.Direction.NORTH;
        net.minecraft.core.Direction sideways = facing.getCounterClockWise();

        com.hbm_m.entity.projectile.CogEntity cog = com.hbm_m.entity.projectile.CogEntity.create(level,
                pos.getX() + 0.5 + facing.getStepX(), pos.getY() + 1, pos.getZ() + 0.5 + facing.getStepZ(),
                facing).setMeta(getGeatMeta());
        cog.setDeltaMovement(sideways.getStepX(),
                1D + (heat - maxHeat) * 0.0001D,
                sideways.getStepZ());
        level.addFreshEntity(cog);

        hasCog = false;
    }

    /** Rechtsklick mit passendem Zahnrad repariert die Maschine (1:1 aus dem Original). */
    public boolean tryRepair(Player player, ItemStack held) {
        ItemStack gear = gearFor(getGeatMeta());
        if (hasCog || held.isEmpty() || gear.isEmpty() || held.getItem() != gear.getItem()) return false;
        held.shrink(1);
        hasCog = true;
        if (level != null) {
            level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    com.hbm_m.sound.HbmSoundsNT.get("hbm:item.upgradePlug"), SoundSource.BLOCKS, 1.5F, 0.75F);
        }
        setChanged();
        sendUpdateToClient();
        return true;
    }

    /** Original TileEntityStirling.getGeatMeta: 0 normal, 1 Stahl, 2 kreativ. */
    public int getGeatMeta() {
        BlockState state = getBlockState();
        return state.is(ModBlocks.STIRLING.get()) ? 0 : state.is(ModBlocks.STIRLING_CREATIVE.get()) ? 2 : 1;
    }

    /**
     * Das Zahnrad zur Bauart ({@code new ItemStack(gear_large, 1, meta)}). Meta 2 (kreativ) gibt es im Original
     * als Gegenstand nicht (ItemGear zeigt nur 0 und 1) - der kreative Motor ist also nicht nachruestbar.
     */
    public static ItemStack gearFor(int meta) {
        if (meta == 1) return new ItemStack(com.hbm_m.item.PartTabMetaItems.get("gear_large_steel").get());
        if (meta == 2) return ItemStack.EMPTY;
        return new ItemStack(ModItems.GEAR_LARGE.get());
    }

    public boolean hasCog() {
        return hasCog;
    }

    /** Original {@code MachineStirling.onBlockPlacedBy}: Meta-1-Item ohne Zahnrad. */
    public void setHasCog(boolean hasCog) {
        this.hasCog = hasCog;
        setChanged();
    }

    public int getHeat() {
        return heat;
    }

    /** Original {@code powerBuffer} (Blick-Overlay). */
    public long getPowerBuffer() {
        return powerBuffer;
    }

    public int getMaxHeat() {
        return maxHeat;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("heat", syncHeat);
        tag.putInt("overspeed_ticks", overspeedTicks);
        tag.putBoolean("has_cog", hasCog);
        tag.putLong("power_buffer", powerBuffer);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        overspeedTicks = tag.getInt("overspeed_ticks");
        hasCog = !tag.contains("has_cog") || tag.getBoolean("has_cog");
        powerBuffer = tag.getLong("power_buffer");
    }

    @Override
    protected void applyClientUpdate(CompoundTag tag) {
        super.applyClientUpdate(tag);
        this.heat = tag.getInt("heat");
    }

    private net.minecraft.world.phys.AABB bb = null;

    /** Original {@code getRenderBoundingBox}: x-1..x+2, y..y+2, z-1..z+2. */
    //? if forge {
    @Override
    //?}
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        if (bb == null) bb = new net.minecraft.world.phys.AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 2, worldPosition.getZ() + 2);
        return bb;
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
