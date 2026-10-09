package com.hbm_m.item.tool;

import com.hbm_m.platform.PlatformHooks;

import com.hbm_m.platform.AttributeOps;

import com.hbm_m.platform.ItemHooks;

import com.hbm_m.platform.EffectHooks;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.hbm_m.advancement.ModAdvancements;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.entity.projectile.RubbleEntity;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.Polaroid;
import com.hbm_m.particle.helper.ExplosionCreator;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ArmorUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.items.tool.WeaponSpecial}: Schrabidium-Hammer (toetet sofort), Flaschenoeffner,
 * Ullapool Caber (Explosion), Shimmer-Vorschlaghammer/-Axt, Richterhaemmer, Stoppschilder, Meme-Loeffel
 * (Krit bei Raketensprung), umgedrehter Schraubenschluessel und pch. Treffer nutzen die Waffe nicht ab
 * (Original {@code hitEntity} ruft super nicht auf) - ausser dem Caber, der sich selbst verbraucht.
 */
public class WeaponSpecial extends SwordItem implements ITooltipProvider {

    private static final UUID WEAPON_MODIFIER = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");

    Random rand = new Random();

    public WeaponSpecial(HbmToolMaterial material, Properties properties) {
        // ItemSword (1.7.10): 4 + Schadensbonus des Materials
        //? if < 1.21.1 {
        super(material, 4, -2.4F, properties);
        //?} else {
        /*super(material, properties); // Attribute: getDefaultAttributeModifiers(ItemStack)
        com.hbm_m.platform.ItemComponentHooks.deferRarity(this, this::hbmRarity);
        *///?}
    }

    //? if < 1.21.1 {
    @Override
    public @NotNull Rarity getRarity(@NotNull ItemStack stack) {
    //?} else {
    /*private @NotNull Rarity hbmRarity() {
    *///?}
        if (this == ModItems.SCHRABIDIUM_HAMMER.get()) {
            return Rarity.RARE;
        }
        if (this == ModItems.ULLAPOOL_CABER.get()) {
            return Rarity.UNCOMMON;
        }
        if (this == ModItems.SHIMMER_SLEDGE.get() || this == ModItems.SHIMMER_AXE.get()) {
            return Rarity.EPIC;
        }
        return Rarity.COMMON;
    }

    private static void sound(Level world, Entity at, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity entity, @NotNull LivingEntity entityPlayer) {
        Level world = entity.level();

        if (this == ModItems.SCHRABIDIUM_HAMMER.get()) {
            if (!world.isClientSide) {
                entity.setHealth(0.0F);
            }
            sound(world, entity, HbmSoundsNT.get("hbm:weapon.bonk"), 3.0F, 1.0F);
        }

        if (this == ModItems.BOTTLE_OPENER.get()) {
            if (!world.isClientSide) {
                int i = rand.nextInt(7);
                if (i == 0)
                    entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 60 * 20, 0));
                if (i == 1)
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5 * 60 * 20, 2));
                if (i == 2)
                    entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 5 * 60 * 20, 2));
                if (i == 3)
                    entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 1 * 60 * 20, 0));
            }
            sound(world, entity, SoundEvents.ANVIL_LAND, 3.0F, 1.F);
        }

        if (this == ModItems.ULLAPOOL_CABER.get()) {
            if (!world.isClientSide) {
                world.explode(null, entity.getX(), entity.getY(), entity.getZ(), 7.5F, true, Level.ExplosionInteraction.TNT);
            }
            ItemHooks.hurtAndBreak(stack, 505, entityPlayer, EquipmentSlot.MAINHAND);
        }

        if (this == ModItems.SHIMMER_SLEDGE.get()) {
            Vec3 vec = entityPlayer.getLookAngle();
            entity.setDeltaMovement(entity.getDeltaMovement().add(vec.x * 5, vec.y * 5, vec.z * 5));
            entity.hurtMarked = true;
            sound(world, entity, HbmSoundsNT.get("hbm:weapon.bang"), 3.0F, 1.F);
        }

        if (this == ModItems.SHIMMER_AXE.get()) {
            entity.setHealth(entity.getHealth() / 2);
            sound(world, entity, HbmSoundsNT.get("hbm:weapon.slice"), 3.0F, 1.F);
        }

        if (this == ModItems.WOOD_GAVEL.get()) {
            sound(world, entity, HbmSoundsNT.get("hbm:weapon.whack"), 3.0F, 1.F);
        }

        if (this == ModItems.LEAD_GAVEL.get()) {
            sound(world, entity, HbmSoundsNT.get("hbm:weapon.whack"), 3.0F, 1.F);
            entity.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.LEAD), 15 * 20, 4));
        }

        if (this == ModItems.DIAMOND_GAVEL.get()) {
            float ded = entity.getMaxHealth() / 3;
            entity.setHealth(entity.getHealth() - ded);
            sound(world, entity, HbmSoundsNT.get("hbm:weapon.whack"), 3.0F, 1.F);
        }

        if (this == ModItems.MEMESPOON.get() && !world.isClientSide) {

            if (!(entityPlayer instanceof Player player))
                return false;

            if (entityPlayer.fallDistance >= 2) {
                sound(world, entity, HbmSoundsNT.get("hbm:weapon.bang"), 3.0F, 0.75F);
                entity.hurt(world.damageSources().playerAttack(player), 50F);
            }

            if (entityPlayer.fallDistance >= 20 && !player.getAbilities().instabuild) {
                ExplosionVNT vnt = new ExplosionVNT(world, entity.getX(), entity.getY() + entity.getBbHeight() / 2D, entity.getZ(), 15, entityPlayer);
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 150).setupPiercing(25, 0.5F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                ExplosionCreator.composeEffectSmall((ServerLevel) world, entity.getX(), entity.getY() + entity.getBbHeight() / 2D, entity.getZ());
                vnt.explode();
            }
        }

        if (this == ModItems.STOPSIGN.get() || this == ModItems.SOPSIGN.get())
            sound(world, entity, HbmSoundsNT.get("hbm:weapon.stop"), 1.0F, 1.0F);

        return false;
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        if (this == ModItems.SHIMMER_SLEDGE.get()) {

            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && state.getBlock().getExplosionResistance() < 6000) {

                RubbleEntity rubble = RubbleEntity.create(world, pos.getX() + 0.5F, pos.getY(), pos.getZ() + 0.5F, state);
                Vec3 vec = player != null ? player.getLookAngle() : Vec3.ZERO;
                rubble.setDeltaMovement(rubble.getDeltaMovement().add(vec.x * 5, vec.y * 5, vec.z * 5));

                world.playSound(null, rubble.getX(), rubble.getY(), rubble.getZ(), HbmSoundsNT.get("hbm:weapon.bang"), SoundSource.PLAYERS, 3.0F, 1.0F);

                if (!world.isClientSide) {
                    world.addFreshEntity(rubble);
                    world.destroyBlock(pos, false);
                }
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        if (this == ModItems.SHIMMER_AXE.get()) {

            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HbmSoundsNT.get("hbm:weapon.kapeng"), SoundSource.PLAYERS, 3.0F, 1.0F);

            if (!world.isClientSide) {
                for (BlockPos p : new BlockPos[] {pos, pos.above(), pos.below()}) {
                    BlockState state = world.getBlockState(p);
                    if (!state.isAir() && state.getBlock().getExplosionResistance() < 6000) {
                        world.destroyBlock(p, false);
                    }
                }
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }

    //? if < 1.21.1 {
    @Override
    public @NotNull Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@NotNull EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> base = super.getDefaultAttributeModifiers(slot);
    //?} else {
    /*@Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        return com.hbm_m.platform.AttributeHooks.fromSlots(this::hbmSlotModifiers);
    }

    private Multimap<net.minecraft.core.Holder<Attribute>, AttributeModifier> hbmSlotModifiers(EquipmentSlot slot) {
        // 1.20.1 SwordItem(material, 4, -2.4F): Grundwerte des Schwerts
        Multimap<net.minecraft.core.Holder<Attribute>, AttributeModifier> base = com.hbm_m.platform.AttributeHooks.forSlot(
                SwordItem.createAttributes(getTier(), 4, -2.4F), slot);
    *///?}
        if (slot != EquipmentSlot.MAINHAND) return base;

        double speed = 0;
        if (this == ModItems.SCHRABIDIUM_HAMMER.get()) speed = -0.5;
        if (this == ModItems.SHIMMER_SLEDGE.get() || this == ModItems.SHIMMER_AXE.get()) speed = -0.2;
        if (this == ModItems.WRENCH_FLIPPED.get()) speed = -0.1;
        if (speed == 0) return base;

        //? if < 1.21.1 {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        //?} else {
        /*ImmutableMultimap.Builder<net.minecraft.core.Holder<Attribute>, AttributeModifier> builder = ImmutableMultimap.builder();
        *///?}
        builder.putAll(base);
        builder.put(Attributes.MOVEMENT_SPEED, PlatformHooks.attributeModifier(WEAPON_MODIFIER, "Weapon modifier", speed, AttributeOps.MULTIPLY_BASE));
        return builder.build();
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int i, boolean b) {
        if (!world.isClientSide && entity instanceof Player player) {
            if (ArmorUtil.checkForFiend(player)) {
                ModAdvancements.grant(player, ModAdvancements.FIEND);
            } else if (ArmorUtil.checkForFiend2(player)) {
                ModAdvancements.grant(player, ModAdvancements.FIEND2);
            }
        }
    }

    private static void add(List<Component> list, String s) {
        list.add(Component.literal(s).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void appendHbmTooltip(ItemStack itemstack, @Nullable Level level, List<Component> list, TooltipFlag flag) {

        if (this == ModItems.SCHRABIDIUM_HAMMER.get()) {
            add(list, "Even though it says \"+1000000000");
            add(list, "damage\", it's actually \"onehit anything\"");
        }
        if (this == ModItems.ULLAPOOL_CABER.get()) {
            add(list, "High-yield Scottish face removal.");
            add(list, "A sober person would throw it...");
        }
        if (this == ModItems.BOTTLE_OPENER.get()) {
            add(list, "My very own bottle opener.");
            add(list, "Use with caution!");
        }
        if (this == ModItems.SHIMMER_SLEDGE.get()) {
            if (Polaroid.id() == 11) {
                add(list, "shimmer no");
                add(list, "drop that hammer");
                add(list, "you're going to hurt somebody");
                add(list, "shimmer no");
                add(list, "shimmer pls");
            } else {
                add(list, "Breaks everything, even portals.");
            }
        }
        if (this == ModItems.SHIMMER_AXE.get()) {
            if (Polaroid.id() == 11) {
                add(list, "shim's toolbox does an e-x-p-a-n-d");
            } else {
                add(list, "Timber!");
            }
        }
        if (this == ModItems.WRENCH_FLIPPED.get()) {
            add(list, "Wrench 2: The Wrenchening");
        }
        if (this == ModItems.MEMESPOON.get()) {
            list.add(Component.literal("Level 10 Shovel").withStyle(ChatFormatting.DARK_GRAY));
            list.add(Component.literal("Deals crits while the wielder is rocket jumping").withStyle(ChatFormatting.AQUA));
            list.add(Component.literal("20% slower firing speed").withStyle(ChatFormatting.RED));
            list.add(Component.literal("No random critical hits").withStyle(ChatFormatting.RED));
        }
        if (this == ModItems.WOOD_GAVEL.get()) {
            add(list, "Thunk!");
        }
        if (this == ModItems.LEAD_GAVEL.get()) {
            add(list, "You are hereby sentenced to lead poisoning.");
        }
        if (this == ModItems.DIAMOND_GAVEL.get()) {
            add(list, "The joke! It makes sense now!!");
            add(list, "");
            list.add(Component.literal("Deals as much damage as it needs to.").withStyle(ChatFormatting.BLUE));
        }
    }

    /** BEWLR fuer die 1.7-IItemRenderer (ItemRenderGavel/Shim/RedstoneSword); greift nur bei builtin/entity-Itemmodellen. */
    //? if forge {
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
    /*@Override
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
