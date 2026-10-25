package com.hbm_m.entity.train;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.rail.IRailNTM.TrackGauge;
import com.hbm_m.interfaces.ITrainGuiProvider;
import com.hbm_m.inventory.menu.TrainCargoTramTrailerMenu;
import com.hbm_m.util.Vec3NT;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code TrainCargoTramTrailer}: antriebsloser Anhaenger der Tram mit 45 Ladeplaetzen; Rechtsklick oeffnet das GUI.
 *
 * <pre>
 *                         &lt;--
 *
 * _O\____________________________________________/O_
 * |____|                                      |____|
 *    \__________________________________________/
 *        '( + )'                      '( + )'
 * </pre>
 */
public class TrainCargoTramTrailer extends EntityRailCarCargo implements ITrainGuiProvider {

    public TrainCargoTramTrailer(EntityType<? extends TrainCargoTramTrailer> type, Level world) {
        super(type, world);
    }

    @Override public double getMaxRailSpeed() { return 1; }
    @Override public TrackGauge getGauge() { return TrackGauge.STANDARD; }
    @Override public double getLengthSpan() { return 1.5; }
    @Override public double getCollisionSpan() { return 2.5; }
    @Override public int getContainerSize() { return 45; }
    @Override public String getInventoryName() { return "container.trainTramTrailer"; }
    @Override public double getCouplingDist(TrainCoupling coupling) { return coupling != null ? 2.75 : 0; }
    @Override public double getCurrentSpeed() { return 0; }

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
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        return interactFirst(player, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public boolean interactFirst(Player player, InteractionHand hand) {
        if (super.interactFirst(player, hand)) return false;

        if (!this.level().isClientSide && player instanceof ServerPlayer sp) {
            this.openTrainGui(sp);
        }

        return true;
    }

    public Component getGuiTitle() {
        return this.hasCustomName() ? this.getCustomName() : Component.translatable(this.getInventoryName());
    }

    @Override
    public void openTrainGui(ServerPlayer player) {
        MenuRegistry.openExtendedMenu(player,
                new SimpleMenuProvider((id, inv, p) -> new TrainCargoTramTrailerMenu(id, inv, this), getGuiTitle()),
                buf -> buf.writeVarInt(this.getId()));
    }
}
