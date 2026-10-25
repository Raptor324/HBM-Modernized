package com.hbm_m.blockentity.machines;

import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.LaunchPadLargeMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Mobile Startrampe - der Iskander-M TEL (9P78-1) auf MZKT-7930-Fahrgestell.
 *
 * <p>Technisch eine gewoehnliche Startrampe: dieselben Slots, dieselben Tanks, derselbe
 * Startvorgang ueber Designator, Redstone oder Radar. Zwei Dinge sind anders.
 *
 * <p><b>Die Geometrie.</b> Bei allen anderen Rampen steht die Rakete ueber der Blockmitte, hier
 * liegt beziehungsweise steht sie im Startrahmen am Heck - deshalb ist {@link #getLaunchOrigin()}
 * ueberschrieben.
 *
 * <p><b>Der Aufrichter.</b> Ein Startbefehl feuert nicht sofort, sondern richtet erst die Rakete
 * auf; abgefeuert wird, sobald sie steht. Der Fortschritt laeuft auf Server und Client mit
 * demselben Schritt, uebertragen wird nur die Richtung - und alle zehn Ticks ohnehin der ganze
 * Zustand, was einen auseinanderlaufenden Fortschritt wieder einfaengt.
 */
public class MobileLaunchPadBlockEntity extends LaunchPadBaseBlockEntity {

    /**
     * Abstand des Startrahmens von der Blockmitte, in Blickrichtung gemessen. Der Wert ist die
     * Achse, um die herum die Rakete aus dem Modell geschnitten wurde; -Z ist bei
     * {@code facing=north} genau die Blickrichtung.
     */
    public static final double ERECTOR_FORWARD = 6.33D;

    /**
     * Fusspunkt der aufgerichteten Rakete. Genau die Hoehe, in der sie im Ausgangsmodell stand -
     * der Zylinder, mit dem sie herausgeschnitten wurde, beginnt hier.
     */
    public static final double ERECTOR_BASE = 2.0D;

    /**
     * Hoehe der liegenden Rakete in der Ladewanne. Deren Boden liegt bei y = 2,8 und ihr Rand bei
     * y = 3,9; ein Rumpf von einem Block Durchmesser liegt auf 3,5 also sauber darin. Zwischen
     * diesem Wert und {@link #ERECTOR_BASE} wandert der Fusspunkt beim Aufrichten - so, wie das
     * Heck der echten Rakete beim Aufrichten in den Startschuh rutscht.
     */
    public static final double ERECTOR_REST = 3.5D;

    /** Dauer des Aufrichtens in Ticks (5 Sekunden). Das Einfahren dauert genauso lange. */
    public static final int ERECT_TICKS = 100;

    /** 0 = eingefahren, 1 = aufgerichtet. */
    private float erectorProgress;

    /** Stand des vorigen Ticks, damit der Renderer zwischen zwei Ticks interpolieren kann. */
    private float lastErectorProgress;

    /** Richtung, in die sich der Aufrichter gerade bewegt. */
    private boolean erecting;

    private boolean hasPendingLaunch;
    private int pendingTargetX;
    private int pendingTargetZ;

    public MobileLaunchPadBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MOBILE_LAUNCH_PAD_BE.get(), pos, state);
    }

    public MobileLaunchPadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MobileLaunchPadBlockEntity be) {
        be.advanceErector();
        if (level.isClientSide) {
            // Der Bodenqualm gehoert an den Startrahmen am Heck, nicht unter die Blockmitte -
            // sonst sucht er die startende Rakete sechs Bloecke neben ihrer tatsaechlichen Bahn.
            BlockPos erector = pos.relative(state.getValue(HorizontalDirectionalBlock.FACING), 6);
            clientLaunchPadSmokeTick(level, erector, state);
        } else {
            commonServerTick(level, pos, state, be);
            be.serverErectorTick();
        }
    }

    // -- Aufrichter ------------------------------------------------------------

    private void advanceErector() {
        this.lastErectorProgress = this.erectorProgress;
        float step = 1.0F / ERECT_TICKS;
        this.erectorProgress = this.erecting
                ? Math.min(1.0F, this.erectorProgress + step)
                : Math.max(0.0F, this.erectorProgress - step);
    }

    private void serverErectorTick() {
        if (!this.erecting) {
            return;
        }
        // Rakete entnommen, Treibstoff weg, Ziel verloren: wieder einfahren.
        if (!this.hasPendingLaunch || !canLaunch()) {
            this.hasPendingLaunch = false;
            setErecting(false);
            return;
        }
        if (this.erectorProgress >= 1.0F) {
            this.hasPendingLaunch = false;
            super.launchToCoordinate(this.pendingTargetX, this.pendingTargetZ);
            setErecting(false);
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

    /** Aufrichtfortschritt fuer die Darstellung, zwischen zwei Ticks interpoliert. */
    public float getErectorProgress(float partialTicks) {
        return Mth.lerp(Mth.clamp(partialTicks, 0.0F, 1.0F), this.lastErectorProgress, this.erectorProgress);
    }

    public boolean isErecting() {
        return this.erecting;
    }

    /**
     * Ein Startbefehl richtet zuerst auf. Der eigentliche Start passiert in
     * {@link #serverErectorTick()}, sobald die Rakete steht; steht sie schon, geht es direkt los.
     * Der Rueckgabewert meldet also "Startvorgang laeuft", nicht "Rakete ist weg".
     */
    @Override
    public boolean launchToCoordinate(int targetX, int targetZ) {
        if (level == null || level.isClientSide || !canLaunch()) {
            return false;
        }
        if (this.erecting && this.erectorProgress >= 1.0F) {
            return super.launchToCoordinate(targetX, targetZ);
        }
        this.pendingTargetX = targetX;
        this.pendingTargetZ = targetZ;
        this.hasPendingLaunch = true;
        setErecting(true);
        setChanged();
        return true;
    }

    /**
     * Ueber Koordinaten statt ueber die Zielentitaet: waehrend der fuenf Sekunden Aufrichtzeit
     * waere eine mitgefuehrte Verfolgung ohnehin hinfaellig.
     */
    @Override
    public boolean launchToEntity(Entity entity) {
        return launchToCoordinate(Mth.floor(entity.getX()), Mth.floor(entity.getZ()));
    }

    @Override
    protected Vec3 getLaunchOrigin() {
        Direction facing = Direction.NORTH;
        BlockState state = getBlockState();
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            facing = state.getValue(HorizontalDirectionalBlock.FACING);
        }
        return new Vec3(
                worldPosition.getX() + 0.5D + facing.getStepX() * ERECTOR_FORWARD,
                worldPosition.getY() + ERECTOR_BASE,
                worldPosition.getZ() + 0.5D + facing.getStepZ() * ERECTOR_FORWARD);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.mobileLaunchPad");
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
    }

    /**
     * Das Fahrzeug ist knapp 14 Bloecke lang und die Rakete steht am Heck - mit dem
     * Ein-Block-Standard wuerde beides weggekullt, sobald der Controllerblock aus dem Bild laeuft.
     */
    //? if forge {
    @Override
    //?}
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(9.0D, 0.0D, 9.0D)
                .expandTowards(0.0D, 16.0D, 0.0D);
    }
}
