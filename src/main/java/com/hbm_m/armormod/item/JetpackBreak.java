package com.hbm_m.armormod.item;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ArmorUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code com.hbm.items.armor.JetpackBreak}. */
public class JetpackBreak extends JetpackFueledBase {

    public JetpackBreak(Properties properties, Supplier<Fluid> fuel, int maxFuel) {
        super(properties, fuel, maxFuel);
    }

    @Override
    public String getModelTexture() {
        return "hbm_m:textures/models/jetpackblue.png";
    }

    protected static void particles(Level world, Player player, boolean mode) {
        CompoundTag data = new CompoundTag();
        data.putString("type", "jetpack");
        data.putInt("player", player.getId());
        if (mode) data.putInt("mode", 1);
        IParticleCreator.sendPacket((ServerLevel) world, player.getX(), player.getY(), player.getZ(), 100, data);
    }

    protected static void sound(Level world, Player player, float pitch) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:weapon.flamethrowerShoot"), SoundSource.PLAYERS, 0.25F, pitch);
    }

    public static int maxFuel = 1200;

    @Override
    public void jetpackTick(Level world, Player player, ItemStack stack) {

        HbmPlayerProps props = HbmPlayerProps.getData(player);
        // NTM Space: Schwerkraft des Koerpers (CelestialBody.getGravity) - Erde: STANDARD_GRAVITY, alles unveraendert
        float gravity = com.hbm_m.api.space.SpaceApi.gravity(player);

        if (!world.isClientSide) {

            if (getFuel(stack) > 0 && (props.isJetpackActive() || (!player.onGround() && !player.isShiftKeyDown() && props.enableBackpack && gravity > 0))) {
                particles(world, player, false);
            }
        }

        if (getFuel(stack) > 0) {
            boolean fork = com.hbm_m.api.space.SpaceApi.forkActive();

            boolean playerTriesToHover = player.isShiftKeyDown() && props.isJetpackActive();
            boolean playerShouldHover = playerTriesToHover || !player.isShiftKeyDown();
            Vec3 m = player.getDeltaMovement();
            double mx = m.x, my = m.y, mz = m.z;

            if (props.isJetpackActive() && !playerTriesToHover) {
                player.fallDistance = 0;

                if (gravity == 0) {
                    Vec3 look = player.getLookAngle();

                    mx += look.x * 0.05;
                    my += look.y * 0.05;
                    mz += look.z * 0.05;
                } else if (my < 0.4D) {
                    my += 0.1D * Math.max(gravity / com.hbm_m.api.space.SpaceEnvironment.STANDARD_GRAVITY, 1);
                }

                sound(world, player, 1.5F);
                this.useUpFuel(player, stack, 5);
                // E2 (Fork, nur mit Addon): Flugzeit nur beim Schub/Schweben zuruecksetzen
                if (fork) ArmorUtil.resetFlightTime(player);

            } else if (playerShouldHover && !player.onGround() && props.enableBackpack && gravity > 0) {
                player.fallDistance = 0;

                float thrustMultiplier = Math.max(gravity / com.hbm_m.api.space.SpaceEnvironment.STANDARD_GRAVITY, 1);

                if (my < -1 * thrustMultiplier)
                    my += 0.2D * thrustMultiplier;
                else if (my < -0.1 * thrustMultiplier)
                    my += 0.1D * thrustMultiplier;
                else if (my < 0)
                    my = 0;

                mx *= 1.025D;
                mz *= 1.025D;

                sound(world, player, 1.5F);
                this.useUpFuel(player, stack, 10);
                if (fork) ArmorUtil.resetFlightTime(player);
            }

            player.setDeltaMovement(mx, my, mz);
            if (!fork) ArmorUtil.resetFlightTime(player);
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(Component.literal("Regular jetpack that will automatically hover mid-air."));
        list.add(Component.literal("Sneaking will stop hover mode."));
        list.add(Component.literal("Hover mode will consume less fuel and increase air-mobility."));
        super.addInformation(stack, level, list);
    }
}
