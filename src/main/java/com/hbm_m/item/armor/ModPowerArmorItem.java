package com.hbm_m.item.armor;

import com.hbm_m.api.energy.EnergyCapabilityProvider;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.client.model.ModModelLayers; // Импорт слоев
import com.hbm_m.client.model.T51ArmorModel; // Импорт модели
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
        // Это укажет игре на файл: src/main/resources/assets/hbm_m/textures/models/armor/t51_atlas.png
        return MainRegistry.MOD_ID + ":textures/armor/t51_atlas.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            private T51ArmorModel model;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.model == null) {
                    ModelPart layer = Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.T51_ARMOR);
                    this.model = new T51ArmorModel(layer);
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
                        this.model.body.visible = true;
                        this.model.rightLeg.visible = true;
                        this.model.leftLeg.visible = true;
                    }
                    case FEET -> {
                        this.model.rightLeg.visible = true;
                        this.model.leftLeg.visible = true;
                        // Если у тебя нет rightBoot/leftBoot в модели, то ничего страшного,
                        // но в методе renderToBuffer в T51ArmorModel мы их добавили, так что они будут рисоваться.
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

    // ... (Остальные методы про энергию и тултипы остаются без изменений) ...

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new EnergyCapabilityProvider(stack, specs.capacity, specs.maxReceive, specs.capacity);
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
    public boolean isBarVisible(@Nonnull ItemStack stack) { return true; }

    @Override
    public int getBarWidth(@Nonnull ItemStack stack) {
        long current = getEnergy(stack);
        long max = specs.capacity;
        if (max <= 0) return 0;
        return (int) Math.round(13.0 * current / (double) max);
    }

    @Override
    public int getBarColor(@Nonnull ItemStack stack) {
        long current = getEnergy(stack);
        long max = specs.capacity;
        float f = Math.max(0.0F, (float) current / (float) max);
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        long current = getEnergy(stack);
        tooltip.add(Component.literal("Power Armor System").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(String.format("Charge: %s / %s HE", EnergyFormatter.format(current), EnergyFormatter.format(specs.capacity))).withStyle(ChatFormatting.AQUA));
        if (flag.isAdvanced()) {
            tooltip.add(Component.literal("Type: " + (specs.mode == PowerArmorSpecs.EnergyMode.CONSTANT_DRAIN ? "Active Field" : "Reactive Shield")).withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void onArmorTick(ItemStack stack, Level world, Player player) {
        if (!world.isClientSide && this.getType() == Type.CHESTPLATE) {
            if (hasFullSet(player)) handleActiveDrain(player);
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

    public float getRadiationResistance(ItemStack stack) {
        long energy = getEnergy(stack);
        if (energy <= 0) return 0.0f;
        return specs.resRadiation;
    }
}