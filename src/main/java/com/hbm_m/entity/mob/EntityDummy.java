package com.hbm_m.entity.mob;

import org.jetbrains.annotations.NotNull;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityDummy}: Testpuppe. Zeigt dauerhaft "Leben / Maximum" als Namen, laesst sich per Rechtsklick mit
 * einer Ruestung (Kopie) einkleiden und wirft keine Ausruestung ab.
 */
public class EntityDummy extends Mob {

    public EntityDummy(EntityType<? extends EntityDummy> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes();
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && held.getItem() instanceof ArmorItem armor) {
            this.setItemSlot(armor.getEquipmentSlot(), held.copy());
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    public boolean isCustomNameVisible() {
        return true;
    }

    @Override
    public @NotNull Component getName() {
        return Component.literal((int) (this.getHealth() * 10) / 10F + " / " + (int) (this.getMaxHealth() * 10) / 10F);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getName();
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) { }
}
