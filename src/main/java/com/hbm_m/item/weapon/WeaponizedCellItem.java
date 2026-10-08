package com.hbm_m.item.weapon;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.EntityCloudFleijaRainbow;
import com.hbm_m.entity.logic.EntityNukeExplosionMK3;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * 1:1 {@code WeaponizedCell} (weaponized_starblaster_cell): liegt sie 50 s am Boden oder brennt sie, zuendet eine
 * FLEIJA-Explosion (Radius 100) samt Regenbogenwolke. Vorher Funken/Rauch, die letzten 5 s Lava-Partikel.
 * Ob sie zuendet, regelt wie im Original {@code WeaponConfig.dropStar}.
 */
public class WeaponizedCellItem extends Item {

    public WeaponizedCellItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity item) {
        Level world = item.level();

        if (item.tickCount > 50 * 20 || item.isOnFire()) {
            if (!world.isClientSide) {
                if (com.hbm_m.config.WeaponConfig.dropStar) {
                    EntityNukeExplosionMK3 ex = EntityNukeExplosionMK3.statFacFleija(world, item.getX(), item.getY(), item.getZ(), 100);
                    if (!ex.isRemoved()) {
                        world.playSound(null, item.getX(), item.getY(), item.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 100.0F, world.random.nextFloat() * 0.1F + 0.9F);
                        world.addFreshEntity(ex);
                        EntityCloudFleijaRainbow cloud = new EntityCloudFleijaRainbow(ModEntities.CLOUD_FLEIJA_RAINBOW.get(), world, 100);
                        cloud.setPos(item.getX(), item.getY(), item.getZ());
                        world.addFreshEntity(cloud);
                    }
                }
                item.discard();
            }
        }

        int randy = (50 * 20) - item.tickCount;
        if (randy < 1) randy = 1;

        double px = item.getX() + world.random.nextGaussian() * item.getBbWidth() / 2;
        double py = item.getY() + world.random.nextGaussian() * item.getBbHeight();
        double pz = item.getZ() + world.random.nextGaussian() * item.getBbWidth() / 2;
        if (world.random.nextInt(50 * 20) >= randy)
            world.addParticle(new DustParticleOptions(new Vector3f(1F, 0F, 0F), 1F), px, py, pz, 0.0, 0.0, 0.0);
        else
            world.addParticle(ParticleTypes.SMOKE, px, py, pz, 0.0, 0.0, 0.0);

        if (randy < 100)
            world.addParticle(ParticleTypes.LAVA, item.getX() + world.random.nextGaussian() * item.getBbWidth() / 2,
                    item.getY() + world.random.nextGaussian() * item.getBbHeight(), item.getZ() + world.random.nextGaussian() * item.getBbWidth() / 2, 0.0, 0.0, 0.0);

        return false;
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal("A charged energy cell, rigged to explode"));
        list.add(Component.literal("when left on the floor for too long."));
    }
}
