package com.hbm_m.powerarmor;

import com.hbm_m.client.model.ModModelLayers;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.item.AbstractRadiationMeterItem;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.sounds.GeigerSoundPacket;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.radiation.PlayerHandler;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.EnergyFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegistryObject;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID)
public class ModPowerArmorItem extends ModArmorFSBPowered {

    private static final Random RANDOM = new Random();
    private int soundTickCounter = 0;

    private final PowerArmorSpecs specs;

    public ModPowerArmorItem(ArmorMaterial material, Type type, Properties properties, PowerArmorSpecs specs) {
        super(material, type, properties, MainRegistry.MOD_ID + ":textures/armor/" + getTextureName(type),
              specs.capacity, specs.maxReceive, specs.usagePerDamagePoint, specs.usagePerTick);
        this.specs = specs;

        // Apply FSB properties from specs
        this.enableVATS(specs.hasVats);
        this.enableThermalSight(specs.hasThermal);
        this.setHasGeigerSound(specs.hasGeigerSound);
        this.setHasCustomGeiger(specs.hasCustomGeiger);
        this.setHasHardLanding(specs.hasHardLanding);
        this.setDashCount(specs.dashCount);
        this.setStepSize(specs.stepSize);
        this.setStepSound(specs.stepSound);
        this.setJumpSound(specs.jumpSound);
        this.setFallSound(specs.fallSound);

        // Add effects from specs
        for (var effect : specs.passiveEffects) {
            this.addEffect(effect);
        }
    }

    private static String getTextureName(Type type) {
        return switch (type) {
            case HELMET -> "t51_helmet";
            case CHESTPLATE -> "t51_chest";
            case LEGGINGS, BOOTS -> "t51_leg";
        } + ".png";
    }

    public PowerArmorSpecs getSpecs() {
        return specs;
    }

    // --- САМОЕ ВАЖНОЕ: ПУТЬ К ТЕКСТУРЕ ---
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        String tex = switch (slot) {
            case HEAD -> "t51_helmet";
            case CHEST -> "t51_chest";
            case LEGS, FEET -> "t51_leg";
            default -> "t51_chest";
        };
        return MainRegistry.MOD_ID + ":textures/armor/" + tex + ".png";
    }

    @Override
    public void initializeClient(@Nonnull Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            private T51ArmorModel model;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.model == null) {
                    ModelPart layer = Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.T51_ARMOR);
                    this.model = new T51ArmorModel(layer);
                }

                this.model.setAllVisible(false);
                this.model.setRenderSlot(equipmentSlot);

                switch (equipmentSlot) {
                    case HEAD -> {
                        this.model.head.visible = true;
                    }
                    case CHEST -> {
                        this.model.body.visible = true;
                        this.model.rightArm.visible = true;
                        this.model.leftArm.visible = true;
                    }
                    case LEGS -> {
                        this.model.rightLeg.visible = true;
                        this.model.leftLeg.visible = true;
                    }
                    case FEET -> {
                        this.model.rightLeg.visible = true;
                        this.model.leftLeg.visible = true;
                    }
                    default -> {
                        // MAINHAND/OFFHAND are irrelevant for armor model rendering.
                    }
                }

                this.model.crouching = original.crouching;
                this.model.riding = original.riding;
                this.model.young = original.young;

                copyRotations(original, this.model);

                return this.model;
            }
        });
    }

    private void copyRotations(HumanoidModel<?> source, HumanoidModel<?> target) {
        target.head.copyFrom(source.head);
        target.body.copyFrom(source.body);
        target.rightArm.copyFrom(source.rightArm);
        target.leftArm.copyFrom(source.leftArm);
        target.rightLeg.copyFrom(source.rightLeg);
        target.leftLeg.copyFrom(source.leftLeg);
    }

    @Override
    public net.minecraft.sounds.SoundEvent getEquipSound() {
        return net.minecraft.sounds.SoundEvents.EMPTY;
    }


    /**
     * Переопределяем методы для корректного отображения энергии в тултипе с учетом модификаторов
     */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true; // Показываем полоску энергии
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long currentEnergy = getCharge(stack);
        long maxEnergy = getMaxCharge(stack);
        if (maxEnergy <= 0) return 0;
        return (int) Math.round(13.0 - (1.0 - (double) currentEnergy / maxEnergy) * 13.0);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        long currentEnergy = getCharge(stack);
        long maxEnergy = getMaxCharge(stack);
        if (maxEnergy <= 0) return 0xFFFFFF;

        double ratio = (double) currentEnergy / maxEnergy;

        if (ratio >= 0.5) {
            // Зеленый для высокой энергии
            return 0x00FF00;
        } else if (ratio >= 0.25) {
            // Желтый для средней энергии
            return 0xFFFF00;
        } else {
            // Красный для низкой энергии
            return 0xFF0000;
        }
    }

    @Override
    public void onArmorTick(ItemStack stack, Level world, Player player) {
        if (!world.isClientSide && this.getType() == Type.CHESTPLATE) {
            // Handle passive effects for CONSTANT_DRAIN mode (energy drain now handled in base class)
            if (hasFSBArmor(player) && specs.mode == PowerArmorSpecs.EnergyMode.CONSTANT_DRAIN) {
                long energy = getCharge(stack);
                if (energy >= specs.usagePerTick) {
                    // Apply passive effects without draining energy (drain handled in ModArmorFSBPowered)
                    for (var effect : specs.passiveEffects) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect.getEffect(), 40, effect.getAmplifier(), effect.isAmbient(), effect.isVisible()));
                    }
                }
            }

            // Handle geiger sound for power armor
            handlePowerArmorGeiger(stack, world, player);
        }

        // Call parent method for FSB functionality
        super.onArmorTick(stack, world, player);
    }

    private void handlePowerArmorGeiger(ItemStack stack, Level world, Player player) {
        // Check if geiger is enabled for this armor
        if (!specs.hasGeigerSound) return;

        // Check if player has full FSB armor set
        if (!hasFSBArmor(player)) return;

        // Don't play sounds if player has separate geiger counter or dosimeter
        if (playerHasGeigerDevice(player)) return;

        soundTickCounter++;
        final int SOUND_INTERVAL_TICKS = 5;

        if (soundTickCounter >= SOUND_INTERVAL_TICKS) {
            soundTickCounter = 0;

            if (player instanceof ServerPlayer serverPlayer) {
                playArmorGeigerSound(serverPlayer);
            }
        }
    }

    private boolean playerHasGeigerDevice(Player player) {
        return player.getInventory().contains(ModItems.GEIGER_COUNTER.get().getDefaultInstance()) ||
               player.getInventory().contains(ModItems.DOSIMETER.get().getDefaultInstance());
    }

    private void playArmorGeigerSound(ServerPlayer player) {
        if (!ModClothConfig.get().enableRadiation) return;

        // Measure radiation like the geiger counter does
        float chunkRad = ChunkRadiationManager.getRadiation(
            player.level(),
            player.getBlockX(),
            (int) Math.floor(player.getY() + player.getBbHeight() * 0.5),
            player.getBlockZ()
        );

        float invRad = PlayerHandler.getInventoryRadiation(player);
        float totalEnvironmentRads = chunkRad + invRad;

        if (ModClothConfig.get().enableDebugLogging) {
            MainRegistry.LOGGER.debug("PowerArmorGeiger: chunkRad = {}, invRad = {}, totalEnvironmentRads = {}",
                chunkRad, invRad, totalEnvironmentRads);
        }

        int soundIndex = 0;
        List<Integer> soundOptions = new ArrayList<>();

        if (totalEnvironmentRads > 0) {
            if (totalEnvironmentRads < 10) soundOptions.add(1);
            if (totalEnvironmentRads > 5 && totalEnvironmentRads < 15) soundOptions.add(2);
            if (totalEnvironmentRads > 10 && totalEnvironmentRads < 20) soundOptions.add(3);
            if (totalEnvironmentRads > 15 && totalEnvironmentRads < 25) soundOptions.add(4);
            if (totalEnvironmentRads > 20 && totalEnvironmentRads < 30) soundOptions.add(5);
            if (totalEnvironmentRads > 25) soundOptions.add(6);

            if (!soundOptions.isEmpty()) {
                soundIndex = soundOptions.get(RANDOM.nextInt(soundOptions.size()));
            }
        } else if (RANDOM.nextInt(50) == 0) {
            soundIndex = 1; // Rare background click
        }

        Optional<RegistryObject<SoundEvent>> soundRegistryObject = switch (soundIndex) {
            case 1 -> Optional.of(ModSounds.GEIGER_1);
            case 2 -> Optional.of(ModSounds.GEIGER_2);
            case 3 -> Optional.of(ModSounds.GEIGER_3);
            case 4 -> Optional.of(ModSounds.GEIGER_4);
            case 5 -> Optional.of(ModSounds.GEIGER_5);
            case 6 -> Optional.of(ModSounds.GEIGER_6);
            default -> Optional.empty();
        };

        soundRegistryObject.ifPresent(regObject -> {
            SoundEvent soundEvent = regObject.get();
            if (soundEvent != null) {
                ResourceLocation soundLocation = soundEvent.getLocation();
                ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new GeigerSoundPacket(soundLocation, 0.4F, 1.0F));
            }
        });
    }

    public static boolean hasFullSet(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        if (head.isEmpty() || chest.isEmpty() || legs.isEmpty() || feet.isEmpty()) return false;
        if (!(head.getItem() instanceof ModPowerArmorItem) || !(chest.getItem() instanceof ModPowerArmorItem) || !(legs.getItem() instanceof ModPowerArmorItem) || !(feet.getItem() instanceof ModPowerArmorItem)) return false;
        ModPowerArmorItem armorItem = (ModPowerArmorItem) chest.getItem();
        return ((ModPowerArmorItem) head.getItem()).getMaterial() == armorItem.getMaterial() && ((ModPowerArmorItem) legs.getItem()).getMaterial() == armorItem.getMaterial() && ((ModPowerArmorItem) feet.getItem()).getMaterial() == armorItem.getMaterial();
    }

    /**
     * Проверяет, носит ли игрок полный сет силовой брони (FSB).
     * Использует базовую реализацию из ModArmorFSB
     */
    public static boolean hasFSBArmor(Player player) {
        return ModArmorFSB.hasFSBArmor(player);
    }

    /**
     * Проверяет, носит ли игрок полный сет силовой брони, игнорируя заряд.
     * Использует базовую реализацию из ModArmorFSB
     */
    public static boolean hasFSBArmorIgnoreCharge(Player player) {
        return ModArmorFSB.hasFSBArmorIgnoreCharge(player);
    }

    public long getEnergy(ItemStack stack) {
        return getCharge(stack);
    }

    public void extractEnergy(ItemStack stack, long amount, boolean simulate) {
        if (!simulate) {
            dischargeBattery(stack, amount);
        }
    }

    public long getModifiedCapacity(ItemStack stack) {
        return getMaxCharge(stack);
    }

    public float getRadiationResistance(ItemStack stack) {
        long energy = getCharge(stack);
        if (energy <= 0) return 0.0f;
        return specs.resRadiation;
    }


    @Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID)
    public static class PowerArmorSoundHandler {
        @SubscribeEvent
        public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
            if (event.getEntity().level().isClientSide) return; // Только сервер
            if (event.getFrom().getItem() == event.getTo().getItem()) return; // Игнорируем смену NBT
            
            // Если надели или сняли нашу броню
            if (event.getTo().getItem() instanceof ModPowerArmorItem || event.getFrom().getItem() instanceof ModPowerArmorItem) {
                event.getEntity().playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 1.0F);
            }
        }
    }
}