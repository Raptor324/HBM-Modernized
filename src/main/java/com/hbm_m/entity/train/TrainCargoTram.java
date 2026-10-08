package com.hbm_m.entity.train;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.rail.IRailNTM.TrackGauge;
import com.hbm_m.interfaces.ITrainGuiProvider;
import com.hbm_m.inventory.menu.TrainCargoTramMenu;
import com.hbm_m.util.Vec3NT;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code TrainCargoTram}: elektrische Flachbett-Tram (Normalspur), Fahrer + zwei Passagiersitze, 28 Ladeplaetze
 * und ein Batterieplatz. Das GUI oeffnet die Zug-Taste ({@link ITrainGuiProvider}).
 *
 * <pre>
 *     _________
 *    | |       \          &lt;--
 *    | |       |___
 *    | |       |  |                             |
 * _O\|_|_______|__|_____________________________|/O_
 * |____|                                      |____|
 *    \__________________________________________/
 *        '( + )'                      '( + )'
 * </pre>
 */
public class TrainCargoTram extends EntityRailCarElectric implements ITrainGuiProvider {

    public TrainCargoTram(EntityType<? extends TrainCargoTram> type, Level world) {
        super(type, world);
    }

    @Override public double getPoweredAcceleration() { return 0.01; }
    @Override public double getPassivBrake() { return 0.95; }
    @Override public boolean shouldUseEngineBrake(Player player) { return Math.abs(this.engineSpeed) < 0.1; }
    @Override public double getMaxPoweredSpeed() { return 0.5; }
    @Override public double getMaxRailSpeed() { return 1; }

    @Override public TrackGauge getGauge() { return TrackGauge.STANDARD; }
    @Override public double getLengthSpan() { return 1.5; }
    @Override public double getCollisionSpan() { return 2.5; }
    @Override public Vec3NT getRiderSeatPosition() { return Vec3NT.createVectorHelper(0.375, 2.375, 0.5); }
    //? if forge || neoforge {
    @Override public boolean shouldRiderSit() { return false; }
    //?}
    @Override public int getContainerSize() { return 29; }
    @Override public String getInventoryName() { return "container.trainTram"; }
    @Override public double getCouplingDist(TrainCoupling coupling) { return coupling != null ? 2.75 : 0; }

    @Override public int getMaxPower() { return this.getPowerConsumption() * 100; }
    @Override public int getPowerConsumption() { return 10; }
    @Override public boolean hasChargeSlot() { return true; }
    @Override public int getChargeSlot() { return 28; }

    @Override
    public DummyConfig[] getDummies() {
        return new DummyConfig[] {
                new DummyConfig(2F, 1F, Vec3NT.createVectorHelper(0, 0, 1.5)),
                new DummyConfig(2F, 1F, Vec3NT.createVectorHelper(0, 0, 0)),
                new DummyConfig(2F, 1F, Vec3NT.createVectorHelper(0, 0, -1.5))
        };
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            this.discard();
        }

        return true;
    }

    @Override
    public Vec3NT[] getPassengerSeats() {
        return new Vec3NT[] {
                Vec3NT.createVectorHelper(0.5, 1.75, -1.5),
                Vec3NT.createVectorHelper(-0.5, 1.75, -1.5)
        };
    }

    /** Titel: eigener Name oder der uebersetzte Inventarname. */
    public Component getGuiTitle() {
        return this.hasCustomName() ? this.getCustomName() : Component.translatable(this.getInventoryName());
    }

    @Override
    public void openTrainGui(ServerPlayer player) {
        MenuRegistry.openExtendedMenu(player,
                new SimpleMenuProvider((id, inv, p) -> new TrainCargoTramMenu(id, inv, this), getGuiTitle()),
                buf -> buf.writeVarInt(this.getId()));
    }
}
