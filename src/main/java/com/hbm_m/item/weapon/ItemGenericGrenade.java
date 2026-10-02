package com.hbm_m.item.weapon;

import com.hbm_m.entity.grenades.EntityGrenadeBouncyGeneric;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.weapon.ItemGenericGrenade}: wirft eine {@link EntityGrenadeBouncyGeneric}. */
public class ItemGenericGrenade extends ItemGrenade {

    public ItemGenericGrenade(int fuse, Properties properties) {
        super(fuse, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5F, 0.4F / (world.random.nextFloat() * 0.4F + 0.8F));

        if (!world.isClientSide) {
            world.addFreshEntity(new EntityGrenadeBouncyGeneric(world, player).setType(this));
        }

        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }

    public void explode(Entity grenade, LivingEntity thrower, Level world, double x, double y, double z) { }

    public int getMaxTimer() {
        return this.fuse * 20;
    }

    public double getBounceMod() {
        return 0.5D;
    }
}
