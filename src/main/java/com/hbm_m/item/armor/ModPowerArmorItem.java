package com.hbm_m.item.armor;

import com.hbm_m.api.energy.EnergyCapabilityProvider;
import com.hbm_m.armormod.item.ItemModBattery;
import com.hbm_m.armormod.item.ItemModBatteryMk2;
import com.hbm_m.armormod.item.ItemModBatteryMk3;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.client.model.ModModelLayers; // Импорт слоев
import com.hbm_m.client.model.T51ArmorModel; // Импорт модели
import com.hbm_m.item.AbstractRadiationMeterItem;
import com.hbm_m.main.MainRegistry;       // Импорт главного класса
import com.hbm_m.util.EnergyFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class ModPowerArmorItem extends ArmorItem {

    private final PowerArmorSpecs specs;

    public ModPowerArmorItem(ArmorMaterial material, Type type, Properties properties, PowerArmorSpecs specs) {
        super(material, type, properties.stacksTo(1));
        this.specs = specs;
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


    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        long modifiedCapacity = getModifiedCapacity(stack);
        return new EnergyCapabilityProvider(stack, modifiedCapacity, specs.maxReceive, modifiedCapacity);
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
        long currentEnergy = getEnergy(stack);
        long maxEnergy = getModifiedCapacity(stack);
        if (maxEnergy <= 0) return 0;
        return (int) Math.round(13.0 - (1.0 - (double) currentEnergy / maxEnergy) * 13.0);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        long currentEnergy = getEnergy(stack);
        long maxEnergy = getModifiedCapacity(stack);
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

    public long getEnergy(ItemStack stack) {
        if (stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).isPresent()) {
            return stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).map(e -> e.getEnergyStored()).orElse(0L);
        }
        return stack.getOrCreateTag().getLong("energy");
    }

    public void extractEnergy(ItemStack stack, long amount, boolean simulate) {
        stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).ifPresent(e -> e.extractEnergy(amount, simulate));
        if (!simulate) {
            long current = stack.getOrCreateTag().getLong("energy");
            stack.getOrCreateTag().putLong("energy", Math.max(0, current - amount));
        }
    }


    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        long current = getEnergy(stack);
        long maxCapacity = getModifiedCapacity(stack); // Используем модифицированную емкость
        tooltip.add(Component.literal("Power Armor System").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(String.format("Charge: %s / %s HE", EnergyFormatter.format(current), EnergyFormatter.format(maxCapacity))).withStyle(ChatFormatting.AQUA));
        if (flag.isAdvanced()) {
            tooltip.add(Component.literal("Type: " + (specs.mode == PowerArmorSpecs.EnergyMode.CONSTANT_DRAIN ? "Active Field" : "Reactive Shield")).withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void onArmorTick(ItemStack stack, Level world, Player player) {
        if (!world.isClientSide && this.getType() == Type.CHESTPLATE) {
            if (hasFSBArmor(player)) handleActiveDrain(player);

            // Geiger sound in armor - порт из ArmorFSB.onArmorTick()
            if (hasFSBArmor(player) && specs.hasGeigerSound) {
                // Проверяем что у игрока нет геигер-счетчика или дозиметра в инвентаре
                if (!hasGeigerCounter(player) && !hasDosimeter(player)) {
                    if (world.getGameTime() % 5 == 0) { // Каждые 5 тиков
                        playArmorGeigerSound(world, player);
                    }
                }
            }
        }
    }

    private void handleActiveDrain(Player player) {
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.getItem() instanceof ModPowerArmorItem armorItem) {
                PowerArmorSpecs armorSpecs = armorItem.getSpecs();
                if (armorSpecs.mode == PowerArmorSpecs.EnergyMode.CONSTANT_DRAIN) {
                    long energy = armorItem.getEnergy(stack);
                    if (energy >= armorSpecs.usagePerTick) {
                        armorItem.extractEnergy(stack, armorSpecs.usagePerTick, false);
                        for (var effect : armorSpecs.passiveEffects) {
                            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect.getEffect(), 40, effect.getAmplifier(), effect.isAmbient(), effect.isVisible()));
                        }
                    }
                }
            }
        }
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
     * Аналог hasFSBArmor() из оригинального ArmorFSB.java
     */
    public static boolean hasFSBArmor(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty() || !(chest.getItem() instanceof ModPowerArmorItem)) {
            return false;
        }

        ModPowerArmorItem chestplate = (ModPowerArmorItem) chest.getItem();
        boolean noHelmet = chestplate.getSpecs().noHelmetRequired;

        // Проверяем все слоты (если noHelmet=true, то шлем не требуется)
        int requiredSlots = noHelmet ? 3 : 4;
        for (int i = 0; i < requiredSlots; i++) {
            ItemStack armor = player.getItemBySlot(EquipmentSlot.values()[i + 2]); // HEAD=2, CHEST=3, LEGS=4, FEET=5
            if (armor.isEmpty() || !(armor.getItem() instanceof ModPowerArmorItem)) {
                return false;
            }

            // Проверяем что это один и тот же материал
            if (((ModPowerArmorItem) armor.getItem()).getMaterial() != chestplate.getMaterial()) {
                return false;
            }

            // Проверяем что броня включена (если такая система есть)
            // TODO: добавить проверку isArmorEnabled если будет реализована
        }

        return true;
    }

    /**
     * Проверяет, носит ли игрок полный сет силовой брони, игнорируя заряд.
     * Аналог hasFSBArmorIgnoreCharge() из оригинального ArmorFSB.java
     */
    public static boolean hasFSBArmorIgnoreCharge(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty() || !(chest.getItem() instanceof ModPowerArmorItem)) {
            return false;
        }

        ModPowerArmorItem chestplate = (ModPowerArmorItem) chest.getItem();
        boolean noHelmet = chestplate.getSpecs().noHelmetRequired;

        // Проверяем все слоты (если noHelmet=true, то шлем не требуется)
        int requiredSlots = noHelmet ? 3 : 4;
        for (int i = 0; i < requiredSlots; i++) {
            ItemStack armor = player.getItemBySlot(EquipmentSlot.values()[i + 2]); // HEAD=2, CHEST=3, LEGS=4, FEET=5
            if (armor.isEmpty() || !(armor.getItem() instanceof ModPowerArmorItem)) {
                return false;
            }

            // Проверяем что это один и тот же материал
            if (((ModPowerArmorItem) armor.getItem()).getMaterial() != chestplate.getMaterial()) {
                return false;
            }

            // Не проверяем заряд - игнорируем его
        }

        return true;
    }

    public float getRadiationResistance(ItemStack stack) {
        long energy = getEnergy(stack);
        if (energy <= 0) return 0.0f;
        return specs.resRadiation;
    }

    /**
     * Получает модифицированную емкость с учетом батарейных модификаторов
     */
    public long getModifiedCapacity(ItemStack stack) {
        long baseCapacity = specs.capacity;

        // Проверяем наличие модификаторов батареи
        if (ArmorModificationHelper.hasMods(stack)) {
            ItemStack batteryMod = ArmorModificationHelper.pryMod(stack, ArmorModificationHelper.battery);
            if (!batteryMod.isEmpty()) {
                if (batteryMod.getItem() instanceof ItemModBatteryMk3) {
                    return (long) (baseCapacity * 2.0D); // MK3: удваивает емкость
                } else if (batteryMod.getItem() instanceof ItemModBatteryMk2) {
                    return (long) (baseCapacity * 1.5D); // MK2: увеличивает на 50%
                } else if (batteryMod.getItem() instanceof ItemModBattery battery) {
                    return (long) (baseCapacity * battery.getCapacityMultiplier()); // Обычная батарея: +25%
                }
            }
        }

        return baseCapacity;
    }

    /**
     * Проверяет, есть ли у игрока геигер-счетчик в инвентаре
     */
    private boolean hasGeigerCounter(Player player) {
        // Проверяем руки
        if (isGeigerCounter(player.getMainHandItem()) || isGeigerCounter(player.getOffhandItem())) {
            return true;
        }
        // Проверяем инвентарь
        for (ItemStack stack : player.getInventory().items) {
            if (isGeigerCounter(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверяет, есть ли у игрока дозиметр в инвентаре
     */
    private boolean hasDosimeter(Player player) {
        // Проверяем руки
        if (isDosimeter(player.getMainHandItem()) || isDosimeter(player.getOffhandItem())) {
            return true;
        }
        // Проверяем инвентарь
        for (ItemStack stack : player.getInventory().items) {
            if (isDosimeter(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверяет, является ли предмет геигер-счетчиком
     */
    private boolean isGeigerCounter(ItemStack stack) {
        return stack.getItem().getDescriptionId().contains("geiger_counter");
    }

    /**
     * Проверяет, является ли предмет дозиметром
     */
    private boolean isDosimeter(ItemStack stack) {
        return stack.getItem().getDescriptionId().contains("dosimeter");
    }

    /**
     * Воспроизводит звук геигера в броне на основе уровня радиации
     * Использует логику из AbstractRadiationMeterItem для измерения радиации
     */
    private void playArmorGeigerSound(Level world, Player player) {
        // Измеряем радиацию с помощью статического метода AbstractRadiationMeterItem
        var radiationData = AbstractRadiationMeterItem.measureRadiationStatic(world, player);

        // Используем playerRad (радиацию игрока) с учетом защиты брони
        // В AbstractRadiationMeterItem.measureRadiation уже учтена защита брони
        float effectiveRadiation = radiationData.playerRad();

        if (effectiveRadiation > 1E-5) {
            // Определяем какой звук воспроизвести на основе уровня радиации
            // Используем ту же логику, что и в оригинальном ArmorFSB.onArmorTick()
            var soundOptions = new java.util.ArrayList<Integer>();

            if (effectiveRadiation < 1) soundOptions.add(0);
            if (effectiveRadiation < 5) soundOptions.add(0);
            if (effectiveRadiation < 10) soundOptions.add(1);
            if (effectiveRadiation > 5 && effectiveRadiation < 15) soundOptions.add(2);
            if (effectiveRadiation > 10 && effectiveRadiation < 20) soundOptions.add(3);
            if (effectiveRadiation > 15 && effectiveRadiation < 25) soundOptions.add(4);
            if (effectiveRadiation > 20 && effectiveRadiation < 30) soundOptions.add(5);
            if (effectiveRadiation > 25) soundOptions.add(6);

            if (!soundOptions.isEmpty()) {
                int r = soundOptions.get(world.random.nextInt(soundOptions.size()));

                if (r > 0) {
                    // Воспроизводим звук геигера по индексу
                    playArmorGeigerSoundByIndex(world, player, r);
                }
            }
        }
    }

    /**
     * Воспроизводит звук геигера по индексу, используя ModSounds
     */
    private void playArmorGeigerSoundByIndex(Level world, Player player, int soundIndex) {
        var soundEvent = switch (soundIndex) {
            case 1 -> com.hbm_m.sound.ModSounds.GEIGER_1.get();
            case 2 -> com.hbm_m.sound.ModSounds.GEIGER_2.get();
            case 3 -> com.hbm_m.sound.ModSounds.GEIGER_3.get();
            case 4 -> com.hbm_m.sound.ModSounds.GEIGER_4.get();
            case 5 -> com.hbm_m.sound.ModSounds.GEIGER_5.get();
            case 6 -> com.hbm_m.sound.ModSounds.GEIGER_6.get();
            default -> com.hbm_m.sound.ModSounds.GEIGER_1.get();
        };

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            soundEvent, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}