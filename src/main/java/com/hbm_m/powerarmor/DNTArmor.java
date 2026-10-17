package com.hbm_m.powerarmor;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ArmorUtil;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.items.armor.ArmorDNT}: der Dineutronium-Nanoanzug ({@code dns_*}). Raketenstiefel,
 * schneller Fall, Sprintschub, immun gegen alles ausser Explosionen (die auf 0,1 % gedrueckt werden).
 */
public class DNTArmor extends ModPowerArmorItem {

    public DNTArmor(ModArmorMaterials material, Type type, Properties properties, String texture,
                    long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, texture, maxPower, chargeRate, consumption, drain);
    }

    private static final UUID speed = UUID.fromString("6ab858ba-d712-485c-bae9-e5e765fc555a");

    @Override
    protected void armorTick(ItemStack stack, Level world, Player player) {
        super.armorTick(stack, world, player);

        if (this != ModItems.DNS_PLATE.get())
            return;

        HbmPlayerProps props = HbmPlayerProps.getData(player);

        /// SPEED ///
        ArmorAttributes.speed(player, speed, "DNT SPEED", 0.25, player.isSprinting());

        // E2 (Fork, nur mit Addon): Sprintschub/Reibung/Effekte 1:1 NTM Space ArmorDNT
        if (com.hbm_m.api.space.SpaceApi.forkActive()) {
            forkFlight(world, player, props);
            return;
        }

        if (!world.isClientSide) {

            /// JET ///
            if (hasFSBArmor(player) && (props.isJetpackActive() || (!player.onGround() && !player.isShiftKeyDown() && props.enableBackpack))) {

                CompoundTag data = new CompoundTag();
                data.putString("type", "jetpack_dns");
                data.putInt("player", player.getId());
                IParticleCreator.sendPacket((ServerLevel) world, player.getX(), player.getY(), player.getZ(), 100, data);
            }
        }

        if (hasFSBArmor(player)) {

            ArmorUtil.resetFlightTime(player);
            Vec3 m = player.getDeltaMovement();
            double mx = m.x, my = m.y, mz = m.z;

            if (props.isJetpackActive()) {

                if (my < 0.6D)
                    my += 0.2D;

                player.fallDistance = 0;

                world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:weapon.immolatorShoot"), SoundSource.PLAYERS, 0.125F, 1.5F);

            } else if (!player.isShiftKeyDown() && !player.onGround() && props.enableBackpack) {
                player.fallDistance = 0;

                if (my < -1)
                    my += 0.4D;
                else if (my < -0.1)
                    my += 0.2D;
                else if (my < 0)
                    my = 0;

                mx *= 1.05D;
                mz *= 1.05D;

                if (player.zza != 0) {
                    mx += player.getLookAngle().x * 0.25 * player.zza;
                    mz += player.getLookAngle().z * 0.25 * player.zza;
                }

                world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:weapon.immolatorShoot"), SoundSource.PLAYERS, 0.125F, 1.5F);
            }

            // NTM Space: in Schwerelosigkeit nur mit Rucksack absinken (CelestialBody.getGravity) - Erde: gravity > 0
            if (player.isShiftKeyDown() && !player.onGround() && (com.hbm_m.api.space.SpaceApi.gravity(player) > 0 || props.enableBackpack)) {
                my -= 0.1D;
            }

            player.setDeltaMovement(mx, my, mz);
        }
    }

    /** E2: 1:1 NTM Space {@code ArmorDNT.onArmorTick} (Flugteil). */
    private void forkFlight(Level world, Player player, HbmPlayerProps props) {
        float gravity = com.hbm_m.api.space.SpaceApi.gravity(player);

        if (hasFSBArmor(player)) {
            boolean playEffect = false;

            ArmorUtil.resetFlightTime(player);
            Vec3 m = player.getDeltaMovement();
            double mx = m.x, my = m.y, mz = m.z;

            if (props.isJetpackActive()) {

                if (my < 0.6D)
                    my += 0.2D;

                player.fallDistance = 0;

                playEffect = true;

            } else if (!player.isShiftKeyDown() && !player.onGround() && props.enableBackpack) {
                player.fallDistance = 0;

                if (my < -1)
                    my += 0.4D;
                else if (my < -0.1)
                    my += 0.2D;
                else if (my > 0.1)
                    my -= 0.2D;
                else if (my > 1)
                    my -= 0.4D;
                else if (my < 0)
                    my = 0;

                // Only reduce air friction if actively moving
                if (player.zza != 0 || player.xxa != 0) {
                    mx *= 1.05D;
                    mz *= 1.05D;

                    playEffect = true;
                }

                if (gravity > 0 || player.isSprinting()) playEffect = true;

                // Only apply flight boost when holding sprint key
                if (player.zza != 0 && player.isSprinting()) {
                    mx += player.getLookAngle().x * 0.25 * player.zza;
                    mz += player.getLookAngle().z * 0.25 * player.zza;
                }
            }

            if (player.isShiftKeyDown() && !player.onGround() && (gravity > 0 || props.enableBackpack)) {
                my -= 0.1D;

                if (gravity == 0 && props.enableBackpack) playEffect = true;
            }

            player.setDeltaMovement(mx, my, mz);

            /// JET ///
            if (playEffect) {
                if (!world.isClientSide) {
                    CompoundTag data = new CompoundTag();
                    data.putString("type", "jetpack_dns");
                    data.putInt("player", player.getId());
                    IParticleCreator.sendPacket((ServerLevel) world, player.getX(), player.getY(), player.getZ(), 100, data);
                }

                world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:weapon.immolatorShoot"), SoundSource.PLAYERS, 0.125F, 1.5F);
            }
        }
    }

    @Override
    public void handleAttack(LivingEntity e, ItemArmorMod.Hurt event) {

        if (e instanceof Player player) {

            if (ModArmorFSB.hasFSBArmor(player)) {

                if (event.source.is(DamageTypeTags.IS_EXPLOSION)) {
                    return;
                }

                // E2 (Fork, nur mit Addon): kein Klirren bei Erstickungsschaden (space_m:oxyprime)
                if (!(com.hbm_m.api.space.SpaceApi.forkActive() && event.source.typeHolder().unwrapKey()
                        .map(k -> k.location().getPath().equals("oxyprime") && !k.location().getNamespace().equals("minecraft")).orElse(false))) {
                    HbmPlayerProps.plink(player, SoundEvents.ITEM_BREAK, 0.5F, 1.0F + e.getRandom().nextFloat() * 0.5F);
                }

                event.canceled = true;
            }
        }
    }

    @Override
    public void handleHurt(LivingEntity e, ItemArmorMod.Hurt event) {

        if (e instanceof Player player) {

            if (ModArmorFSB.hasFSBArmor(player)) {

                if (event.source.is(DamageTypeTags.IS_EXPLOSION)) {
                    event.amount *= 0.001F;
                    return;
                }

                event.amount = 0;
            }
        }
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {

        list.add(Component.literal("Charge: " + BobMathUtil.getShortNumber(getCharge(stack)) + " / " + BobMathUtil.getShortNumber(this.getMaxCharge(stack))));

        // E2 (Fork, nur mit Addon): "Can be used as a spacesuit" (Lang aus dem Addon)
        if (com.hbm_m.api.space.SpaceApi.forkActive())
            list.add(Component.translatable("armor.canBreathe").withStyle(ChatFormatting.BLUE));

        list.add(Component.translatable("armor.fullSetBonus").withStyle(ChatFormatting.GOLD));

        if (!effects.isEmpty()) {

            for (MobEffectInstance effect : effects) {
                list.add(Component.literal("  ").append(Component.translatable(effect.getDescriptionId())).withStyle(ChatFormatting.AQUA));
            }
        }

        list.add(line(ChatFormatting.RED, "armor.vats"));
        list.add(line(ChatFormatting.RED, "armor.thermal"));
        list.add(line(ChatFormatting.RED, "armor.hardLanding"));
        list.add(line(ChatFormatting.AQUA, "armor.rocketBoots"));
        list.add(line(ChatFormatting.AQUA, "armor.fastFall"));
        list.add(line(ChatFormatting.AQUA, "armor.sprintBoost"));
    }
}
