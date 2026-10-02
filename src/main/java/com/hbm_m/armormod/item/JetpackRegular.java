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

/** 1:1 {@code com.hbm.items.armor.JetpackRegular}. */
public class JetpackRegular extends JetpackFueledBase {

    public JetpackRegular(Properties properties, Supplier<Fluid> fuel, int maxFuel) {
        super(properties, fuel, maxFuel);
    }

    @Override
    public String getModelTexture() {
        return "hbm_m:textures/models/jetpackred.png";
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
                particles(world, player, false);
            }
        }

        if (getFuel(stack) > 0 && props.isJetpackActive()) {
            player.fallDistance = 0;

            Vec3 m = player.getDeltaMovement();
            if (m.y < 0.4D)
                player.setDeltaMovement(m.x, m.y + 0.1D, m.z);

            sound(world, player, 1.5F);
            this.useUpFuel(player, stack, 5);
            ArmorUtil.resetFlightTime(player);
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(Component.literal("Regular jetpack for simple upwards momentum."));
        super.addInformation(stack, level, list);
    }
}
