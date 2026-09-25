package com.hbm_m.blockentity.machines;

import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.LaunchPadLargeMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Topol-M-Werfer - der MZKT-79221 mit dem Startbehaelter der RT-2PM2.
 *
 * <p>Technisch eine Startrampe wie jede andere: dieselben Slots, dieselben Tanks, ausgeloest ueber
 * Designator, Redstone oder Radar. Zwei Dinge sind eigen.
 *
 * <p><b>Der Aufrichter.</b> Der Behaelter liegt laengs auf dem Fahrzeug und wird auf Startbefehl
 * in {@link #ERECT_TICKS} Ticks senkrecht gestellt. Gerechnet wird der Fortschritt auf Server und
 * Client mit demselben Schritt, uebertragen wird nur die Richtung.
 *
 * <p><b>Der Kaltstart.</b> Anders als bei jeder anderen Rampe der Mod zuendet die Rakete nicht im
 * Rohr. Sobald der Behaelter steht, vergehen {@link #EJECT_TICKS}, in denen der Deckel abgeht und
 * ein Gasgenerator die Rakete aus dem Rohr wirft - erst danach entsteht die Raketenentitaet, und
 * zwar oberhalb der Muendung. Genau so macht es das Vorbild, und deshalb sieht man auf Aufnahmen
 * eines Topol-Starts den Feuerball immer ueber dem Fahrzeug und nie daran.
 */
public class TopolLaunchPadBlockEntity extends LaunchPadBaseBlockEntity {

    /** Abstand des Aufrichtgelenks von der Blockmitte, in Blickrichtung. Aus dem Modell gemessen. */
    public static final double PIVOT_FORWARD = 11.877D;

    /**
     * Seitlicher Versatz der Rohrachse gegenueber der Blockmitte. Das Fahrzeug ist nicht ganz
     * symmetrisch modelliert, der Behaelter sitzt ein gutes Zehntel neben der Mitte.
     */
    public static final double PIVOT_LATERAL = 0.126D;

    /**
     * Halbmesser des Behaelters. Der Drehpunkt liegt an seiner *Unterkante*, die Rohrachse also um
     * diesen Betrag darueber - aufgerichtet heisst das: um diesen Betrag weiter vorn.
     */
    public static final double CANISTER_RADIUS = 1.188D;

    /** Hoehe des Aufrichtgelenks ueber dem Boden. */
    public static final double PIVOT_HEIGHT = 2.275D;

    /**
     * Versatz des Drehpunkts gegenueber dem hinteren Ende des Behaelters, in Blickrichtung.
     *
     * <p>Ohne diesen Versatz dreht das Rohr um seine eigene hintere Kante und schwenkt dabei
     * vollstaendig hinter das Fahrzeug - aufgerichtet stuende es zwischen z = -14,3 und -11,9,
     * also komplett hinter dem Heck bei -11,8 und ohne alles darunter. Mit 1,6 Bloecken Versatz
     * sinkt das Rohr beim Aufrichten stattdessen auf die Startplatte herunter (Unterkante y=0,7)
     * und kommt ueber dem letzten Achspaar zu stehen, so wie beim Vorbild.
     */
    public static final double HINGE_OFFSET = 1.6D;

    /** Laenge des Startbehaelters. Aufgerichtet liegt die Muendung entsprechend hoch. */
    public static final double CANISTER_LENGTH = 23.75D;

    /** Dauer des Aufrichtens in Ticks - sieben Sekunden fuer ein Rohr von 24 Metern. */
    public static final int ERECT_TICKS = 140;

    /** Zeit zwischen "Behaelter steht" und dem Verlassen der Rakete - zweieinhalb Sekunden. */
    public static final int EJECT_TICKS = 50;

    /**
     * Dauer der Rauchentwicklung nach dem Kaltstart.
     *
     * <p>Beim Vorbild treibt ein Gasgenerator die Rakete aus dem Rohr, bevor die erste Stufe
     * zuendet. Das Treibgas und der aufgewirbelte Staub liegen danach sekundenlang ueber dem
     * ganzen Platz, nicht nur an der Muendung.
     */
    private static final int SMOKE_TICKS = 110;

    /** Wie weit sich der Rauch ausbreitet, in Bloecken - beim Vorbild rund 50 m. */
    private static final double SMOKE_RADIUS = 50.0D;

    /** 0 = liegend, 1 = senkrecht. */
    private float erectorProgress;
    private float lastErectorProgress;
    private boolean erecting;

    private boolean hasPendingLaunch;
    private int pendingTargetX;
    private int pendingTargetZ;

    /** Laeuft nach dem Aufrichten herunter; bei 0 verlaesst die Rakete das Rohr. */
    private int ejectCountdown = -1;

    /** Stand des vorigen Ticks, damit der Renderer den Auswurf zwischen zwei Ticks interpoliert. */
    private int lastEjectCountdown = -1;

    /** Laeuft nach dem Auswurf herunter und speist die Rauchwolke. Nur auf dem Server. */
    private int smokeTicks = -1;

    public TopolLaunchPadBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TOPOL_LAUNCH_PAD_BE.get(), pos, state);
    }

    public TopolLaunchPadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TopolLaunchPadBlockEntity be) {
        be.advanceErector();
        if (level.isClientSide) {
            return;
        }
        commonServerTick(level, pos, state, be);
        be.serverErectorTick();
        be.serverSmokeTick();
    }

    // -- Aufrichter und Auswurf -------------------------------------------------

    private void advanceErector() {
        this.lastErectorProgress = this.erectorProgress;
        float step = 1.0F / ERECT_TICKS;
        this.erectorProgress = this.erecting
                ? Math.min(1.0F, this.erectorProgress + step)
                : Math.max(0.0F, this.erectorProgress - step);

        // Der Auswurf zaehlt auf beiden Seiten herunter. Der Server startet ihn und schickt den
        // Startwert mit; der Client laeuft dann von selbst weiter, sonst waere die Bewegung an
        // den Zehn-Tick-Takt der Zustandspakete gebunden und wuerde ruckeln.
        this.lastEjectCountdown = this.ejectCountdown;
        if (this.ejectCountdown > 0) {
            this.ejectCountdown--;
        }
    }

    /**
     * Fortschritt des Auswurfs, 0 = Rakete steckt noch im Rohr, 1 = sie ist frei. Zwischen zwei
     * Ticks interpoliert; -1 bedeutet: kein Auswurf im Gange.
     */
    public float getEjectProgress(float partialTicks) {
        if (this.ejectCountdown < 0) {
            return -1.0F;
        }
        float now = 1.0F - this.ejectCountdown / (float) EJECT_TICKS;
        float before = 1.0F - Math.max(0, this.lastEjectCountdown) / (float) EJECT_TICKS;
        return Mth.lerp(Mth.clamp(partialTicks, 0.0F, 1.0F), before, now);
    }

    private void serverErectorTick() {
        if (this.ejectCountdown == 0) {
            this.ejectCountdown = -1;
            this.hasPendingLaunch = false;
            super.launchToCoordinate(this.pendingTargetX, this.pendingTargetZ);
            setErecting(false);
            return;
        }
        if (this.ejectCountdown > 0) {
            return;
        }

        if (!this.erecting) {
            return;
        }
        if (!this.hasPendingLaunch || !canLaunch()) {
            this.hasPendingLaunch = false;
            setErecting(false);
            return;
        }
        if (this.erectorProgress >= 1.0F) {
            this.ejectCountdown = EJECT_TICKS;
            this.lastEjectCountdown = EJECT_TICKS;
            spawnEjectionEffect();
            setChanged();
            sendUpdateToClient();
        }
    }

    /** Deckel weg, Gasgenerator: eine Rauchwolke an der Muendung, bevor die Rakete kommt. */
    private void spawnEjectionEffect() {
        if (!(this.level instanceof ServerLevel server)) {
            return;
        }
        Vec3 mouth = getCanisterMouth();
        server.sendParticles(ParticleTypes.LARGE_SMOKE, mouth.x, mouth.y, mouth.z, 120,
                1.4D, 1.4D, 1.4D, 0.12D);
        this.smokeTicks = SMOKE_TICKS;
        server.playSound(null, mouth.x, mouth.y, mouth.z,
                com.hbm_m.sound.ModSounds.MISSILE_TAKEOFF.get(), SoundSource.BLOCKS, 6.0F, 1.6F);
    }

    /**
     * Die Wolke nach dem Kaltstart: weisser Treibgas- und Staubschleier, der vom Fahrzeug aus
     * nach aussen laeuft und dabei eine Kuppel bildet - in der Mitte hoch, zum Rand hin flach.
     *
     * <p>Schwarzer Qualm passt nicht: was beim Kaltstart aufsteigt, ist Gasgeneratorgas und
     * aufgewirbelter Staub, und das ist hell.
     */
    private void serverSmokeTick() {
        if (this.smokeTicks <= 0) {
            if (this.smokeTicks == 0) {
                this.smokeTicks = -1;
            }
            return;
        }
        if (!(this.level instanceof ServerLevel server)) {
            return;
        }
        this.smokeTicks--;

        float progress = 1.0F - this.smokeTicks / (float) SMOKE_TICKS;
        // Anfangs schnell, dann auslaufend - so breitet sich eine Druckwelle aus.
        double front = SMOKE_RADIUS * (1.0D - Math.pow(1.0D - progress, 2.2D));
        // Gegen Ende weniger Nachschub, sonst steht die Wolke bis in die Nacht.
        int puffs = (int) Math.max(10, 34 * (1.0D - progress * 0.6D));
        double cx = this.worldPosition.getX() + 0.5D;
        double cz = this.worldPosition.getZ() + 0.5D;

        for (int i = 0; i < puffs; i++) {
            double angle = server.random.nextDouble() * Math.PI * 2.0D;
            // Wurzel, damit die Punkte gleichmaessig auf der Flaeche liegen und sich nicht
            // alle am Rand sammeln.
            double r = 2.0D + (front - 2.0D) * Math.sqrt(server.random.nextDouble());
            double px = cx + Math.cos(angle) * r;
            double pz = cz + Math.sin(angle) * r;
            double ground = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (int) Math.floor(px), (int) Math.floor(pz));
            // Kuppel: ueber dem Werfer reicht der Schleier hoch, weit draussen liegt er flach.
            double dome = 1.0D + 16.0D * Math.pow(Math.max(0.0D, 1.0D - r / SMOKE_RADIUS), 1.6D);
            double py = ground + 0.4D + dome * server.random.nextDouble();

            server.sendParticles(ParticleTypes.CLOUD, px, py, pz, 5,
                    1.8D, 0.9D, 1.8D, 0.012D);
            if (server.random.nextFloat() < 0.4F) {
                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, ground + 0.3D, pz, 2,
                        1.0D, 0.3D, 1.0D, 0.008D);
            }
        }

        // Dichter Kern ueber der Startplatte, solange die Stufe noch brennt - in drei Lagen,
        // damit die Saeule Hoehe hat und nicht als Teppich am Boden klebt.
        if (progress < 0.55F) {
            for (int layer = 0; layer < 3; layer++) {
                double py = this.worldPosition.getY() + 1.0D + layer * 4.5D;
                server.sendParticles(ParticleTypes.CLOUD, cx, py, cz, 12,
                        3.5D - layer * 0.6D, 1.4D, 3.5D - layer * 0.6D, 0.035D);
            }
        }
    }

    private void setErecting(boolean value) {
        if (this.erecting == value) {
            return;
        }
        this.erecting = value;
        setChanged();
        sendUpdateToClient();
    }

    public float getErectorProgress(float partialTicks) {
        return Mth.lerp(Mth.clamp(partialTicks, 0.0F, 1.0F), this.lastErectorProgress, this.erectorProgress);
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
    }

    /**
     * Muendung des aufgerichteten Behaelters in Weltkoordinaten. Das Rohr sitzt aufgerichtet um
     * {@link #HINGE_OFFSET} tiefer und ebenso viel weiter vorn als das Gelenk - siehe dort.
     */
    private Vec3 getCanisterMouth() {
        Direction f = facing();
        Direction side = f.getClockWise();
        // Aufgerichtet liegt die Rohrachse um den Halbmesser vor dem Gelenk, nicht darauf.
        double forward = PIVOT_FORWARD - HINGE_OFFSET + CANISTER_RADIUS;
        return new Vec3(
                worldPosition.getX() + 0.5D + f.getStepX() * forward + side.getStepX() * PIVOT_LATERAL,
                worldPosition.getY() + PIVOT_HEIGHT - HINGE_OFFSET + CANISTER_LENGTH,
                worldPosition.getZ() + 0.5D + f.getStepZ() * forward + side.getStepZ() * PIVOT_LATERAL);
    }

    // -- Startvorgang -----------------------------------------------------------

    /**
     * Ein Startbefehl richtet erst auf. Gestartet wird in {@link #serverErectorTick()}, wenn der
     * Behaelter steht und der Auswurf durch ist.
     */
    @Override
    public boolean launchToCoordinate(int targetX, int targetZ) {
        if (level == null || level.isClientSide || !canLaunch()) {
            return false;
        }
        if (this.ejectCountdown > 0) {
            return false;   // Auswurf laeuft bereits
        }
        this.pendingTargetX = targetX;
        this.pendingTargetZ = targetZ;
        this.hasPendingLaunch = true;
        setErecting(true);
        setChanged();
        return true;
    }

    @Override
    public boolean launchToEntity(Entity entity) {
        return launchToCoordinate(Mth.floor(entity.getX()), Mth.floor(entity.getZ()));
    }

    /**
     * Die Rakete entsteht an der Muendung, nicht im Rohr - siehe Klassenkommentar. Der halbe Block
     * Abstand ist genau der Punkt, an dem der gezeichnete Auswurf aufhoert; so geht die Darstellung
     * ohne Sprung in die echte Entitaet ueber.
     */
    @Override
    protected Vec3 getLaunchOrigin() {
        return getCanisterMouth().add(0.0D, 0.5D, 0.0D);
    }

    // -- Rahmenkram -------------------------------------------------------------

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.topolLaunchPad");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new LaunchPadLargeMenu(containerId, inv, this);
    }

    @Override
    protected boolean isReadyForLaunch() {
        return delay <= 0;
    }

    @Override
    public NodeDirPos[] getConPos() {
        return buildStandardConPos(worldPosition);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putFloat("erector_progress", this.erectorProgress);
        tag.putBoolean("erector_raising", this.erecting);
        tag.putBoolean("erector_pending", this.hasPendingLaunch);
        tag.putInt("erector_target_x", this.pendingTargetX);
        tag.putInt("erector_target_z", this.pendingTargetZ);
        tag.putInt("erector_eject", this.ejectCountdown);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.erectorProgress = tag.getFloat("erector_progress");
        this.lastErectorProgress = this.erectorProgress;
        this.erecting = tag.getBoolean("erector_raising");
        this.hasPendingLaunch = tag.getBoolean("erector_pending");
        this.pendingTargetX = tag.getInt("erector_target_x");
        this.pendingTargetZ = tag.getInt("erector_target_z");
        this.ejectCountdown = tag.contains("erector_eject") ? tag.getInt("erector_eject") : -1;
        this.lastEjectCountdown = this.ejectCountdown;
    }

    /**
     * Das Fahrzeug ist 24 Bloecke lang und ragt aufgerichtet 26 hoch - mit dem Ein-Block-Standard
     * waere es weg, sobald der Controllerblock aus dem Bild laeuft.
     */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(14.0D, 0.0D, 14.0D).expandTowards(0.0D, 30.0D, 0.0D);
    }
}
