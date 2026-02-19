package com.hbm_m.powerarmor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.sounds.GeigerSoundPacket;
import com.hbm_m.powerarmor.layer.ModModelLayers;
import com.hbm_m.powerarmor.layer.PowerArmorEmptyModel;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.radiation.PlayerHandler;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModPowerArmorItem extends ModArmorFSBPowered {
    private static final Random RANDOM = new Random();
    private static final String TAG_HAS_GEIGER_DEVICE = "hbm_has_geiger_device";
    private static final String TAG_GEIGER_CHECK_TICK = "hbm_geiger_check_tick";
    private final PowerArmorSpecs specs;

    public ModPowerArmorItem(ArmorMaterial material, Type type, Properties properties, PowerArmorSpecs specs) {
        // NOTE: the texture passed to the base class is not used for rendering because we override getArmorTexture()
        // below. Keep this value generic to avoid hard-coding a specific armor set (e.g. T51) into the base class.
        super(material, type, properties, MainRegistry.MOD_ID + ":textures/block/armor/power_armor.png",
                specs.capacity, specs.maxReceive, specs.consumption, specs.drain);
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

    // Отключаем ванильную защиту
    @Override
    public int getDefense() {
        return 0; // Отключаем ванильные Armor Points
    }

    @Override
    public float getToughness() {
        return 0.0f; // Отключаем ванильную Toughness
    }

    public PowerArmorSpecs getSpecs() {
        return specs;
    }

    // ПУТЬ К ТЕКСТУРЕ
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        String tex = resolveArmorTextureName(stack, slot);
        return MainRegistry.MOD_ID + ":textures/block/armor/" + tex + ".png";
    }

    /**
     * Resolves the armor texture name (without extension) based on the item's registry id.
     *
     * Examples:
     * - hbm_m:t51_helmet     -> t51_helmet / t51_chest / t51_leg (depending on slot)
     * - hbm_m:ajr_chestplate -> ajr_helmet / ajr_chest / ajr_leg (depending on slot)
     */
    private static String resolveArmorTextureName(ItemStack stack, EquipmentSlot slot) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String path = id != null ? id.getPath() : "";

        String prefix = stripKnownArmorSuffix(path);
        return switch (slot) {
            case HEAD -> prefix + "_helmet";
            case CHEST -> prefix + "_chest";
            case LEGS, FEET -> prefix + "_leg";
            default -> prefix + "_chest";
        };
    }

    private static String stripKnownArmorSuffix(String itemPath) {
        if (itemPath == null || itemPath.isBlank()) return "power_armor";

        if (itemPath.endsWith("_helmet")) return itemPath.substring(0, itemPath.length() - "_helmet".length());
        if (itemPath.endsWith("_chestplate")) return itemPath.substring(0, itemPath.length() - "_chestplate".length());
        if (itemPath.endsWith("_leggings")) return itemPath.substring(0, itemPath.length() - "_leggings".length());
        if (itemPath.endsWith("_boots")) return itemPath.substring(0, itemPath.length() - "_boots".length());

        // Fallback: treat the whole path as a "prefix" if it doesn't match a known armor naming scheme.
        return itemPath;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private PowerArmorEmptyModel model;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, 
                    EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.model == null) {
                    ModelPart layer = Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.POWER_ARMOR);
                    this.model = new PowerArmorEmptyModel(layer);
                }

                this.model.setAllVisible(false);
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
        if (world.isClientSide()) return; // Клиентские эффекты обрабатываются отдельно
        
        // Применяем эффекты ТОЛЬКО для нагрудника (избегаем дублирования от 4 предметов)
        if (this.getType() == Type.CHESTPLATE && hasFSBArmor(player)) {
            long energy = getCharge(stack);
            
            if (energy > 0) {
                applyPassiveEffects(player, specs.passiveEffects);
            } else {
                removePassiveEffects(player, specs.passiveEffects);
            }
            
            handlePowerArmorGeiger(stack, world, player);
        }
        
        // Дрен энергии обрабатывается в родительском классе
        super.onArmorTick(stack, world, player);
    }

    private void applyPassiveEffects(Player player, List<MobEffectInstance> effects) {
        for (var effect : effects) {
            MobEffectInstance current = player.getEffect(effect.getEffect());
            // Применяем эффект ТОЛЬКО если:
            // 1) Его нет у игрока ИЛИ
            // 2) Его длительность < 20 тиков (чтобы избежать мерцания при обновлении)
            if (current == null || current.getDuration() < 20) {
                int duration = Math.max(40, effect.getDuration()); // Минимум 2 секунды
                player.addEffect(new MobEffectInstance(
                    effect.getEffect(),
                    duration,
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    true // showParticles
                ));
            }
        }
    }
    
    private void removePassiveEffects(Player player, List<MobEffectInstance> effects) {
        for (var effect : effects) {
            player.removeEffect(effect.getEffect());
        }
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false; // Силовая броня не изнашивается ванильным способом
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return 0; // Нулевая прочность = отключение ванильной системы
    }

    private void handlePowerArmorGeiger(ItemStack stack, Level world, Player player) {
        if (!specs.hasGeigerSound) return;
        if (!hasFSBArmor(player)) return;
        
        // Оптимизированная проверка гейгера (кэширование + редкие обновления)
        if (hasExternalGeigerDeviceCached(player)) return;
        
        // Таймер на игроке (НЕ на классе предмета!)
        CompoundTag data = player.getPersistentData();
        int counter = data.getInt("hbm_power_armor_geiger_tick");
        counter++;
        
        final int SOUND_INTERVAL_TICKS = 5;
        if (counter >= SOUND_INTERVAL_TICKS) {
            counter = 0;
            if (player instanceof ServerPlayer serverPlayer) {
                playArmorGeigerSound(serverPlayer);
            }
        }
        
        data.putInt("hbm_power_armor_geiger_tick", counter);
    }

    private boolean hasExternalGeigerDeviceCached(Player player) {
        CompoundTag data = player.getPersistentData();
        long currentTick = player.level().getGameTime();
        long lastCheckTick = data.getLong(TAG_GEIGER_CHECK_TICK);
        
        // Проверяем устройство не чаще 1 раза в 20 тиков (~1 секунда)
        if (currentTick - lastCheckTick > 20) {
            boolean hasDevice = playerHasGeigerDevice(player);
            data.putBoolean(TAG_HAS_GEIGER_DEVICE, hasDevice);
            data.putInt(TAG_GEIGER_CHECK_TICK, (int) currentTick);
            return hasDevice;
        }
        
        // Возвращаем кэшированное значение
        return data.getBoolean(TAG_HAS_GEIGER_DEVICE);
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
                ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), 
                        new GeigerSoundPacket(soundLocation, 0.4F, 1.0F));
            }
        });
    }

    public static boolean hasFullSet(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        
        if (head.isEmpty() || chest.isEmpty() || legs.isEmpty() || feet.isEmpty()) return false;
        if (!(head.getItem() instanceof ModPowerArmorItem) || 
            !(chest.getItem() instanceof ModPowerArmorItem) || 
            !(legs.getItem() instanceof ModPowerArmorItem) || 
            !(feet.getItem() instanceof ModPowerArmorItem)) return false;
        
        ModPowerArmorItem armorItem = (ModPowerArmorItem) chest.getItem();
        return ((ModPowerArmorItem) head.getItem()).getMaterial() == armorItem.getMaterial() && 
               ((ModPowerArmorItem) legs.getItem()).getMaterial() == armorItem.getMaterial() && 
               ((ModPowerArmorItem) feet.getItem()).getMaterial() == armorItem.getMaterial();
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
            if (event.getTo().getItem() instanceof ModPowerArmorItem || 
                event.getFrom().getItem() instanceof ModPowerArmorItem) {
                event.getEntity().playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 1.0F);
            }
        }
    }
}