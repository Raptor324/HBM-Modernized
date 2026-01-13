package com.hbm_m.powerarmor;

import com.hbm_m.sound.ModSounds;
import com.hbm_m.lib.RefStrings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Full Set Bonus armor base class
@Mod.EventBusSubscriber(modid = com.hbm_m.main.MainRegistry.MOD_ID)
public class ModArmorFSB extends ArmorItem {

    private String texture = "";
    private ResourceLocation overlay = null;
    public List<MobEffectInstance> effects = new ArrayList<>();
    public boolean noHelmet = false;
    public boolean vats = false;
    public boolean thermal = false;
    public boolean hasGeigerSound = false;
    public boolean customGeiger = false;
    public boolean hardLanding = false;
    public int dashCount = 0;
    public int stepSize = 0;
    public String stepSound;
    public String jumpSound;
    public String fallSound;

    public ModArmorFSB(ArmorMaterial material, Type type, Properties properties, String texture) {
        super(material, type, properties.stacksTo(1));
        this.texture = texture;
    }

    public ModArmorFSB addEffect(MobEffectInstance effect) {
        effects.add(effect);
        return this;
    }

    // @Override
    // public int getDefense() {
    //     return 0; // Отключаем ванильную защиту
    // }

    // @Override
    // public float getToughness() {
    //     return 0; // Отключаем toughness
    // }


    public ModArmorFSB setNoHelmet(boolean noHelmet) {
        this.noHelmet = noHelmet;
        return this;
    }

    public ModArmorFSB enableVATS(boolean vats) {
        this.vats = vats;
        return this;
    }

    public ModArmorFSB enableThermalSight(boolean thermal) {
        this.thermal = thermal;
        return this;
    }

    public ModArmorFSB setHasGeigerSound(boolean geiger) {
        this.hasGeigerSound = geiger;
        return this;
    }

    public ModArmorFSB setHasCustomGeiger(boolean geiger) {
        this.customGeiger = geiger;
        return this;
    }

    public ModArmorFSB setHasHardLanding(boolean hardLanding) {
        this.hardLanding = hardLanding;
        return this;
    }

    public ModArmorFSB setDashCount(int dashCount) {
        this.dashCount = dashCount;
        return this;
    }

    public ModArmorFSB setStepSize(int stepSize) {
        this.stepSize = stepSize;
        return this;
    }

    public ModArmorFSB setStepSound(String step) {
        this.stepSound = step;
        return this;
    }

    public ModArmorFSB setJumpSound(String jump) {
        this.jumpSound = jump;
        return this;
    }

    public ModArmorFSB setFallSound(String fall) {
        this.fallSound = fall;
        return this;
    }

    public ModArmorFSB setOverlay(String path) {
        this.overlay = new ResourceLocation(path);
        return this;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return texture;
    }


    public static boolean hasFSBArmor(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

        if (chest.getItem() instanceof ModArmorFSB) {
            ModArmorFSB chestplate = (ModArmorFSB) chest.getItem();
            boolean noHelmet = chestplate.noHelmet;

            int requiredSlots = noHelmet ? 3 : 4;
            for (int i = 0; i < requiredSlots; i++) {
                EquipmentSlot slot = switch (i) {
                    case 0 -> EquipmentSlot.HEAD;
                    case 1 -> EquipmentSlot.CHEST;
                    case 2 -> EquipmentSlot.LEGS;
                    case 3 -> EquipmentSlot.FEET;
                    default -> EquipmentSlot.CHEST;
                };

                ItemStack armor = player.getItemBySlot(slot);

                if (armor.isEmpty() || !(armor.getItem() instanceof ModArmorFSB))
                    return false;

                if (((ModArmorFSB) armor.getItem()).getMaterial() != chestplate.getMaterial())
                    return false;

                if (!((ModArmorFSB) armor.getItem()).isArmorEnabled(armor))
                    return false;
            }

            return true;
        }

        return false;
    }

    public static boolean hasFSBArmorIgnoreCharge(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

        if (chest.getItem() instanceof ModArmorFSB) {
            ModArmorFSB chestplate = (ModArmorFSB) chest.getItem();
            boolean noHelmet = chestplate.noHelmet;

            int requiredSlots = noHelmet ? 3 : 4;
            for (int i = 0; i < requiredSlots; i++) {
                EquipmentSlot slot = switch (i) {
                    case 0 -> EquipmentSlot.HEAD;
                    case 1 -> EquipmentSlot.CHEST;
                    case 2 -> EquipmentSlot.LEGS;
                    case 3 -> EquipmentSlot.FEET;
                    default -> EquipmentSlot.CHEST;
                };

                ItemStack armor = player.getItemBySlot(slot);

                if (armor.isEmpty() || !(armor.getItem() instanceof ModArmorFSB))
                    return false;

                if (((ModArmorFSB) armor.getItem()).getMaterial() != chestplate.getMaterial())
                    return false;
            }

            return true;
        }

        return false;
    }

    public void handleTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        boolean step = true;

        if (ModArmorFSB.hasFSBArmor(player)) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            ModArmorFSB chestplate = (ModArmorFSB) chest.getItem();

            if (!chestplate.effects.isEmpty()) {
                for (MobEffectInstance effect : chestplate.effects) {
                    player.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible()));
                }
            }

            if (step && chestplate.stepSound != null && player.level().isClientSide && player.onGround()) {
                steppy(player, chestplate.stepSound);
            }
        }
    }

    public static void steppy(Player player, String sound) {
        try {
            // Simplified stepping logic for 1.20.1
            if (player.level().isClientSide && player.onGround()) {
                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    ModSounds.STEP_METAL.get(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
            }
        } catch (Exception e) {
            // Handle reflection errors gracefully
        }
    }

    public void handleJump(Player player) {
        if (ModArmorFSB.hasFSBArmor(player)) {
            ModArmorFSB chestplate = (ModArmorFSB) player.getItemBySlot(EquipmentSlot.CHEST).getItem();

            if (chestplate.jumpSound != null) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.STEP_IRON_JUMP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    public void handleFall(Player player) {
        if (ModArmorFSB.hasFSBArmor(player)) {
            ModArmorFSB chestplate = (ModArmorFSB) player.getItemBySlot(EquipmentSlot.CHEST).getItem();

            if (chestplate.hardLanding && player.fallDistance > 10) {
                // AOE damage effect
                List<Entity> entities = player.level().getEntities(player, player.getBoundingBox().inflate(3, 0, 3));

                for (Entity entity : entities) {
                    if (entity == player) continue;

                    Vec3 vec = player.position().subtract(entity.position());
                    if (vec.length() < 3) {
                        double intensity = 3 - vec.length();
                        entity.setDeltaMovement(entity.getDeltaMovement().add(
                            vec.x * intensity * -2,
                            0.1D * intensity,
                            vec.z * intensity * -2
                        ));

                        entity.hurt(player.damageSources().playerAttack(player), (float) (intensity * 10));
                    }
                }
            }

            if (chestplate.fallSound != null) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.STEP_IRON_LAND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    @Override
    public void onArmorTick(@Nonnull ItemStack stack, @Nonnull Level world, @Nonnull Player player) {
        // FSB armor tick logic can be implemented here
        // For now, simplified version - geiger logic moved to ModPowerArmorItem
    }

    public boolean isArmorEnabled(ItemStack stack) {
        return true;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            // Basic implementation, can be extended for part hiding
        });
    }

    // Event handlers
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        // Handle attack events for FSB armor
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // Handle hurt events for FSB armor
    }
}
