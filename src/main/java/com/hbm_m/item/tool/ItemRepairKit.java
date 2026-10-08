package com.hbm_m.item.tool;

import com.hbm_m.platform.ItemHooks;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ItemRepairKit} (gun_kit_1 / gun_kit_2): senkt den Verschleiss aller Waffen in der Schnellleiste
 * um je 25 % der Maximalhaltbarkeit pro Konfiguration; ein Einsatz kostet einen Haltbarkeitspunkt.
 */
public class ItemRepairKit extends Item {

    public ItemRepairKit(int dura, Properties properties) {
        // setMaxStackSize(1) + setMaxDamage(dura - 1)
        super(properties.durability(dura - 1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide) return InteractionResultHolder.pass(stack);

        boolean didSomething = false;

        for (int i = 0; i < 9; i++) {

            ItemStack item = player.getInventory().items.get(i);

            if (!item.isEmpty() && item.getItem() instanceof ItemGunBaseNT gun) {
                int configs = gun.getConfigCount();

                for (int j = 0; j < configs; j++) {
                    GunConfig cfg = gun.getConfig(item, j);
                    float maxDura = cfg.getDurability(item);
                    float wear = Math.min(ItemGunBaseNT.getWear(item, j), maxDura);
                    if (wear > 0) {
                        ItemGunBaseNT.setWear(item, j, Math.max(0F, ItemGunBaseNT.getWear(item, j) - maxDura * 0.25F));
                        didSomething = true;
                    }
                }
            }
        }

        if (didSomething) {
            if (this == ModItems.GUN_KIT_1.get()) world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:item.spray"), SoundSource.PLAYERS, 1.0F, 1.0F);
            if (this == ModItems.GUN_KIT_2.get()) world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:item.repair"), SoundSource.PLAYERS, 1.0F, 1.0F);

            ItemHooks.hurtAndBreak(stack, 1, player, hand);
        }

        return InteractionResultHolder.pass(stack);
    }
}
