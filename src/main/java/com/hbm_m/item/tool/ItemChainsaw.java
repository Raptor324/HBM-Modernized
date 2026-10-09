package com.hbm_m.item.tool;

import java.util.function.Supplier;

import com.hbm_m.item.IAnimatedItem;
import com.hbm_m.render.anim.AnimationEnums.ToolAnimation;
import com.hbm_m.render.anim.BusAnimation;
import com.hbm_m.render.anim.BusAnimationSequence;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.items.tool.ItemChainsaw}: treibstoffbetriebene Axt mit Kettensaegen-Faehigkeiten und
 * Schwung-Animation ({@code SWING_ROT}/{@code SWING_TRANS}) bei jedem Schlag.
 * {@code IHeldSoundProvider} ist im Original eine leere Markierung ohne Verwendung.
 */
public class ItemChainsaw extends ItemToolAbilityFueled implements IAnimatedItem<ToolAnimation> {

    @SafeVarargs
    public ItemChainsaw(float damage, double movement, Tier material, EnumToolType type, int maxFuel, int consumption, int fillRate, Supplier<Fluid>... acceptedFuels) {
        super(damage, movement, material, type, maxFuel, consumption, fillRate, acceptedFuels);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entityLiving) {

        if (!(entityLiving instanceof ServerPlayer player))
            return false;

        if (stack.getDamageValue() >= stack.getMaxDamage())
            return false;

        playAnimation(player, ToolAnimation.SWING);

        return false;
    }

    @Override
    public BusAnimation getAnimation(ToolAnimation type, ItemStack stack) {
        int forward = 150;
        int sideways = 100;
        int retire = 200;

        if (HbmAnimations.getRelevantAnim() == null) {

            return new BusAnimation()
                    .addBus("SWING_ROT", new BusAnimationSequence()
                            .addPos(0, 0, 90, forward)
                            .addPos(45, 0, 90, sideways)
                            .addPos(0, 0, 0, retire))
                    .addBus("SWING_TRANS", new BusAnimationSequence()
                            .addPos(0, 0, 3, forward)
                            .addPos(2, 0, 2, sideways)
                            .addPos(0, 0, 0, retire));
        } else {

            double[] rot = HbmAnimations.getRelevantTransformation("SWING_ROT");
            double[] trans = HbmAnimations.getRelevantTransformation("SWING_TRANS");

            if (System.currentTimeMillis() - HbmAnimations.getRelevantAnim().startMillis < 50) return null;

            return new BusAnimation()
                    .addBus("SWING_ROT", new BusAnimationSequence()
                            .addPos(rot[0], rot[1], rot[2], 0)
                            .addPos(0, 0, 90, forward)
                            .addPos(45, 0, 90, sideways)
                            .addPos(0, 0, 0, retire))
                    .addBus("SWING_TRANS", new BusAnimationSequence()
                            .addPos(trans[0], trans[1], trans[2], 0)
                            .addPos(0, 0, 3, forward)
                            .addPos(2, 0, 2, sideways)
                            .addPos(0, 0, 0, retire));
        }
    }

    @Override
    public Class<ToolAnimation> getEnum() {
        return ToolAnimation.class;
    }

    @Override
    public boolean shouldPlayerModelAim(ItemStack stack) {
        return false;
    }

    //? if forge {
    /** Original ClientProxy: eigener Itemrenderer (ItemRenderChainsaw) ueber den Waffen-BEWLR. */
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.weapon.GunItemRenderer.INSTANCE;
            }
        });
    }
    //?} elif neoforge {
    /*/^* Original ClientProxy: eigener Itemrenderer (ItemRenderChainsaw) ueber den Waffen-BEWLR. ^/
    @Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.weapon.GunItemRenderer.INSTANCE;
            }
        });
    }
    *///?}
}
