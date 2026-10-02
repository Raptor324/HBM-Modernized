package com.hbm_m.powerarmor;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ArmorUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code com.hbm.items.armor.ArmorBJJetpack}: Mondbrustplatte mit Ionen-Jetpack und Gleiter. */
public class ArmorBJJetpack extends ArmorBJ {

    public ArmorBJJetpack(ModArmorMaterials material, Type type, Properties properties, String texture,
                          long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, texture, maxPower, chargeRate, consumption, drain);
    }

    @Override
    protected void armorTick(ItemStack stack, Level world, Player player) {
        super.armorTick(stack, world, player);

        HbmPlayerProps props = HbmPlayerProps.getData(player);

        if (!world.isClientSide) {

            if (hasFSBArmor(player) && props.isJetpackActive()) {

                CompoundTag data = new CompoundTag();
                data.putString("type", "jetpack_bj");
                data.putInt("player", player.getId());
                IParticleCreator.sendPacket((ServerLevel) world, player.getX(), player.getY(), player.getZ(), 100, data);
            }
        }

        if (hasFSBArmor(player)) {

            ArmorUtil.resetFlightTime(player);

            if (props.isJetpackActive()) {

                Vec3 m = player.getDeltaMovement();
                if (m.y < 0.4D)
                    player.setDeltaMovement(m.x, m.y + 0.1D, m.z);

                player.fallDistance = 0;

                world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:weapon.immolatorShoot"), SoundSource.PLAYERS, 0.125F, 1.5F);

            } else if (player.isShiftKeyDown()) {

                Vec3 m = player.getDeltaMovement();
                if (m.y < -0.08) {

                    double mo = m.y * -0.4;
                    double mx = m.x, my = m.y + mo, mz = m.z;

                    Vec3 vec = player.getLookAngle().scale(mo);

                    mx += vec.x;
                    my += vec.y;
                    mz += vec.z;
                    player.setDeltaMovement(mx, my, mz);
                }
            }
        }
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHbmTooltip(stack, level, list, flag);

        list.add(Component.literal("  + ").append(Component.translatable("armor.electricJetpack")).withStyle(ChatFormatting.RED));
        list.add(Component.literal("  + ").append(Component.translatable("armor.glider")).withStyle(ChatFormatting.GRAY));
    }
}
