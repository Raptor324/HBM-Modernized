package com.hbm_m.item.weapon;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.hbm_m.item.IAnimatedItem;
import com.hbm_m.item.IEquipReceiver;
import com.hbm_m.item.tool.ItemSwordAbility;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.render.anim.AnimationEnums.ToolAnimation;
import com.hbm_m.render.anim.BusAnimation;
import com.hbm_m.render.anim.BusAnimationSequence;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ShadyUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * 1:1 {@code com.hbm.items.weapon.ItemCrucible} ("Crucible"): Nahkampfklinge mit drei Ladungen (Haltbarkeit 3).
 * Solange Ladung uebrig ist: 5000 Schaden, +100 % Laufgeschwindigkeit, Ausklapp-/Schwung-Animationen und
 * Blutfontaene beim Toeten; leer: kein Schaden, keine Modifikatoren, "Not enough energy.".
 * Port-ID {@code crucible_sword}, weil {@code crucible} im Port der Giesstiegel ist.
 */
public class ItemCrucible extends ItemSwordAbility implements IEquipReceiver, IAnimatedItem<ToolAnimation> {

    private static final java.util.Random itemRand = new java.util.Random();

    public ItemCrucible(float damage, double movement, Tier material) {
        super(damage, movement, material);
    }

    private static boolean isActive(ItemStack stack) {
        return stack.getDamageValue() < stack.getMaxDamage();
    }

    @Override
    public void onEquip(Player player, ItemStack stack) {

        if (!(player instanceof ServerPlayer))
            return;

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getDamageValue() < held.getMaxDamage()) {

            Level world = player.level();
            world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:weapon.cDeploy"), SoundSource.PLAYERS, 1.0F, 1.0F);

            playAnimation(player, ToolAnimation.EQUIP);
        }
    }

    /** Original {@code onEntitySwing}: Tankish laedt nach, sonst Schwung-Animation solange Ladung da ist. */
    public boolean onSwing(LivingEntity entityLiving, ItemStack stack) {

        if (!(entityLiving instanceof ServerPlayer player))
            return false;

        if (player.getUUID().toString().equals(ShadyUtil.Tankish)) {
            stack.setDamageValue(0);
        }

        if (stack.getDamageValue() >= stack.getMaxDamage())
            return false;

        playAnimation(player, ToolAnimation.SWING);

        return false;
    }

    //? if forge {
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        onSwing(entity, stack);
        return false;
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.weapon.GunItemRenderer.INSTANCE;
            }
        });
    }

    /** Original {@code getAttributeModifiers(ItemStack)}: Modifikatoren nur mit Ladung. */
    @Override
    @SuppressWarnings("deprecation")
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND || !isActive(stack)) return ImmutableMultimap.of();
        return super.getDefaultAttributeModifiers(slot);
    }
    //?} elif neoforge {
    /*@Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        onSwing(entity, stack);
        return false;
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.weapon.GunItemRenderer.INSTANCE;
            }
        });
    }

    /^* Original {@code getAttributeModifiers(ItemStack)}: Modifikatoren nur mit Ladung (ItemSwordAbility liefert nur MAINHAND). ^/
    @Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        if (!isActive(stack)) return net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY;
        return super.getDefaultAttributeModifiers(stack);
    }
    *///?}

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {

        boolean active = isActive(stack);

        if (active) {

            attacker.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.PLAYERS, 1.0F, 0.75F + victim.getRandom().nextFloat() * 0.2F);

            if (!attacker.level().isClientSide && !victim.isAlive() && attacker.level() instanceof ServerLevel server) {
                int count = Math.min((int) Math.ceil(victim.getMaxHealth() / 3D), 250);

                CompoundTag data = new CompoundTag();
                data.putString("type", "vanillaburst");
                data.putInt("count", count * 4);
                data.putDouble("motion", 0.1D);
                data.putString("mode", "blockdust");
                data.putInt("block", net.minecraft.world.level.block.Block.getId(Blocks.REDSTONE_BLOCK.defaultBlockState()));
                IParticleCreator.sendPacket(server, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 50, data);
            }

            if (attacker instanceof Player player && (player.getGameProfile().getName().equals("Tankish") || player.getGameProfile().getName().equals("Tankish020")))
                return true;

            // ItemSwordAbility#hitEntity: Faehigkeiten (keine) + stack.damageItem(1) - im Original zerbricht die Klinge bei
            // Haltbarkeit = Maximum nicht, sie ist dann nur leer
            if (!(attacker instanceof Player player && player.getAbilities().instabuild)) {
                stack.setDamageValue(Math.min(stack.getDamageValue() + 1, stack.getMaxDamage()));
            }
            return true;
        } else {

            if (!attacker.level().isClientSide && attacker instanceof Player player)
                player.sendSystemMessage(Component.literal("Not enough energy.").withStyle(ChatFormatting.RED));
            return false;
        }
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {

        String charge = ChatFormatting.RED + "Charge [";

        for (int i = 2; i >= 0; i--)
            if (stack.getDamageValue() <= i)
                charge += "||||||";
            else
                charge += "   ";

        charge += "]";

        list.add(Component.literal(charge));
    }

    @Override
    public BusAnimation getAnimation(ToolAnimation type, ItemStack stack) {
        /* crucible deploy */
        if (type == ToolAnimation.EQUIP) {

            return new BusAnimation()
                    .addBus("GUARD_ROT", new BusAnimationSequence()
                            .addPos(90, 0, 1, 0)
                            .addPos(90, 0, 1, 800)
                            .addPos(0, 0, 1, 50));
        }

        /* crucible swing */
        if (type == ToolAnimation.SWING) {

            if (com.hbm_m.render.anim.HbmAnimations.getRelevantTransformation("SWING_ROT")[0] == 0) {

                int offset = itemRand.nextInt(80) - 20;

                playSwing(0.8F + itemRand.nextFloat() * 0.2F);

                return new BusAnimation()
                        .addBus("SWING_ROT", new BusAnimationSequence()
                                .addPos(90 - offset, 90 - offset, 35, 75)
                                .addPos(90 + offset, 90 - offset, -45, 150)
                                .addPos(0, 0, 0, 500))
                        .addBus("SWING_TRANS", new BusAnimationSequence()
                                .addPos(-3, 0, 0, 75)
                                .addPos(8, 0, 0, 150)
                                .addPos(0, 0, 0, 500));
            }
        }

        return null;
    }

    /** Nur clientseitig aufgerufen (aus {@link #getAnimation} im Animationspaket-Handler). */
    private void playSwing(float pitch) {
        com.hbm_m.client.ClientAccess.playUiSound(HbmSoundsNT.get("hbm:weapon.cSwing"), pitch);
    }

    @Override
    public Class<ToolAnimation> getEnum() {
        return ToolAnimation.class;
    }

    @Override
    public boolean shouldPlayerModelAim(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return false;
    }

    /** Leer ist sie nicht kaputt: Haltbarkeitsbalken wie im Original. */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged();
    }
}
