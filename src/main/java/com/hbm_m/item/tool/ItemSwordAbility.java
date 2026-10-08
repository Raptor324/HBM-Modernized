package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.hbm_m.handler.ability.AvailableAbilities;
import com.hbm_m.handler.ability.IWeaponAbility;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.tool.ItemSwordAbility}: Schwert mit festem Schaden, Bewegungsmodifikator und
 * Waffenfaehigkeiten. Angriffsschaden = 1 + {@code damage}; die Angriffsgeschwindigkeit ist die des
 * 1.20-Schwerts (-2.4), wie bei allen portierten Schwertern.
 */
public class ItemSwordAbility extends SwordItem implements ITooltipProvider {

    private Rarity rarity = Rarity.COMMON;
    protected float damage;
    protected double movement;
    private final AvailableAbilities abilities = new AvailableAbilities();

    public ItemSwordAbility(float damage, double movement, Tier material) {
        this(damage, movement, material, new Properties());
    }

    public ItemSwordAbility(float damage, double movement, Tier material, Properties properties) {
        // unzerstoerbare Materialien (Haltbarkeit 0) wuerden sonst stapelbar
        this(damage, movement, material, material.getUses() == 0 ? properties.stacksTo(1) : properties, true);
    }

    /** Fuer Unterklassen, die ihre Haltbarkeit selbst setzen (Akku, unzerstoerbar). */
    protected ItemSwordAbility(float damage, double movement, Tier material, Properties prepared, boolean preparedMarker) {
        super(material, 0, -2.4F, prepared);
        this.damage = damage;
        this.movement = movement;
    }

    public ItemSwordAbility addAbility(IWeaponAbility weaponAbility, int level) {
        this.abilities.addAbility(weaponAbility, level);
        return this;
    }

    // <insert obvious Rarity joke here>
    public ItemSwordAbility setRarity(Rarity rarity) {
        this.rarity = rarity;
        return this;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return this.rarity != Rarity.COMMON ? this.rarity : super.getRarity(stack);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {

        if (!attacker.level().isClientSide && attacker instanceof Player player && canOperate(stack)) {

            // hacky hacky hack
            if (this == ModItems.MESE_GAVEL.get())
                attacker.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), HbmSoundsNT.get("weapon.whack"), SoundSource.PLAYERS, 3.0F, 1.F);

            this.abilities.getWeaponAbilities().forEach((ability, level) ->
                    ability.onHit(level, attacker.level(), player, victim, this));
        }

        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));

        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) return super.getDefaultAttributeModifiers(slot);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", this.damage, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.4F, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", movement, AttributeModifier.Operation.MULTIPLY_BASE));
        return builder.build();
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        abilities.addInformation(list);
    }

    protected boolean canOperate(ItemStack stack) {
        return true;
    }

    /** BEWLR fuer die 1.7-IItemRenderer (ItemRenderGavel/Shim/RedstoneSword); greift nur bei builtin/entity-Itemmodellen. */
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.weapon.GunItemRenderer.INSTANCE;
            }
        });
    }
}
