package com.hbm_m.hazard.type;

import java.util.List;

import com.hbm_m.config.RadiationConfig;
import com.hbm_m.hazard.modifier.HazardModifier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HazardTypeExplosive extends HazardTypeBase {

    @Override
    public void onUpdate(LivingEntity target, float level, ItemStack stack) {
        if (RadiationConfig.disableExplosive) {
            return;
        }

        Level levelWorld = target.level();
        if (!levelWorld.isClientSide && target.isOnFire() && stack.getCount() > 0) {
            stack.shrink(stack.getCount());
            levelWorld.explode(target, target.getX(), target.getY() + target.getEyeHeight() - com.hbm_m.platform.PlatformHooks.getMyRidingOffset(target),
                    target.getZ(), level, Level.ExplosionInteraction.TNT);
        }
    }

    @Override
    public void updateEntity(ItemEntity item, float level) {
        if (RadiationConfig.disableExplosive) {
            return;
        }

        if (item.isOnFire()) {
            item.discard();
            item.level().explode(item, item.getX(), item.getY() + item.getBbHeight() * 0.5, item.getZ(), level,
                    Level.ExplosionInteraction.TNT);
        }
    }

    @Override
    public void addHazardInformation(Player player, List<Component> list, float level, ItemStack stack,
            List<HazardModifier> modifiers) {
        addBracketedTrait(list, "trait.explosive", ChatFormatting.RED);
    }
}
