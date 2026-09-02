package com.hbm_m.item.tool;

import com.hbm_m.advancement.ModAdvancements;
import com.hbm_m.item.ModItems;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 1:1 port of {@code ItemBoltgun} - a rivet gun that fires whatever bolts you happen to be
 * carrying for a flat ten damage that ignores armour entirely.
 *
 * <p>The original's {@code achGoFish} fires on the <em>victim</em>, not the shooter, and only when
 * the victim is a player. It is a joke at the expense of whoever got riveted, which is why it
 * reads "Go Fish" - so this deliberately awards {@code entity}, not {@code player}.</p>
 */
public class ItemBoltgun extends Item {

    /** Damage bypasses armour, matching {@code setDamageBypassesArmor()}. */
    private static final float DAMAGE = 10F;

    public ItemBoltgun(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** {@code bolt_spike} first, then the material bolts, in the original's order. */
    private static List<Item> boltTypes() {
        return List.of(
                ModItems.BOLT_SPIKE.get(),
                ModItems.BOLT_STEEL.get(),
                ModItems.BOLT_TUNGSTEN.get(),
                ModItems.BOLT_HIGHSPEED_STEEL.get());
    }

    @Override
    public boolean onLeftClickEntity(@NotNull ItemStack stack, @NotNull Player player,
                                     @NotNull Entity entity) {
        Level level = player.level();
        if (!entity.isAlive()) return false;

        for (Item boltType : boltTypes()) {
            int slot = findBolt(player, boltType);
            if (slot < 0) continue;

            if (!level.isClientSide) {
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.getInventory().removeItem(slot, 1);

                // attackEntityFromIgnoreIFrame: the original deliberately bypasses the
                // invulnerability window so repeated rivets all land.
                entity.invulnerableTime = 0;
                entity.hurt(com.hbm_m.damagesource.ModDamageSources.boltgun(player), DAMAGE);

                if (!entity.isAlive() && entity instanceof Player victim) {
                    ModAdvancements.grant(victim, ModAdvancements.GO_FISH);
                }

                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.EXPLOSION,
                            entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                            1, 0, 0, 0, 0);
                }
            }
            // True cancels the normal melee swing, exactly as the original returns.
            return true;
        }

        return false;
    }

    private static int findBolt(Player player, Item bolt) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(bolt)) return i;
        }
        return -1;
    }

    /** The gun itself never damages anything by swinging - only the rivets do. */
    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target,
                             @NotNull LivingEntity attacker) {
        return false;
    }
}
