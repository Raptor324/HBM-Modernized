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

/** 1:1 {@code com.hbm.items.armor.JetpackBooster}. */
public class JetpackBooster extends JetpackFueledBase {

    public JetpackBooster(Properties properties, Supplier<Fluid> fuel, int maxFuel) {
        super(properties, fuel, maxFuel);
    }

    @Override
    public String getModelTexture() {
        return "hbm_m:textures/models/jetpack.png";
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

    @Override
    public void jetpackTick(Level world, Player player, ItemStack stack) {

        HbmPlayerProps props = HbmPlayerProps.getData(player);

        if (!world.isClientSide) {

            if (getFuel(stack) > 0 && props.isJetpackActive()) {
                particles(world, player, true);
            }
        }

        if (getFuel(stack) > 0 && props.isJetpackActive()) {

            Vec3 m = player.getDeltaMovement();
            double mx = m.x, my = m.y, mz = m.z;

            if (my < 0.6D)
                my += 0.1D;

            Vec3 look = player.getLookAngle();

            if (new Vec3(mx, my, mz).length() < 5) {
                mx += look.x * 0.25;
                my += look.y * 0.25;
                mz += look.z * 0.25;

                if (look.y > 0)
                    player.fallDistance = 0;
            }

            player.setDeltaMovement(mx, my, mz);
            sound(world, player, 1.0F);
            this.useUpFuel(player, stack, 1);
            ArmorUtil.resetFlightTime(player);
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(Component.literal("High-powered vectorized jetpack."));
        list.add(Component.literal("Highly increased fuel consumption."));
        super.addInformation(stack, level, list);
    }
}
