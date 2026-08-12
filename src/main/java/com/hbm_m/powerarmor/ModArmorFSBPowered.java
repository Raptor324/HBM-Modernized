package com.hbm_m.powerarmor;

import com.hbm_m.item.ITooltipProvider;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.util.EnergyFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.hbm_m.platform.PlatformHooks;
//? if forge {
import com.hbm_m.api.energy.EnergyCapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
//?}

// Full Set Bonus Powered armor - combines FSB functionality with battery system
public class ModArmorFSBPowered extends ModArmorFSB implements ITooltipProvider {

    public long maxPower = 1;
    public long chargeRate;
    public long consumption; // Energy cost when armor takes damage (via setDamage)
    public long drain;       // Passive energy drain per tick

    public static final int ARMOR_SLOT_HEAD = 0;
    public static final int ARMOR_SLOT_CHEST = 1;
    public static final int ARMOR_SLOT_LEGS = 2;
    public static final int ARMOR_SLOT_FEET = 3;

    public ModArmorFSBPowered(ModArmorMaterials material, Type type, Properties properties,
                              String texture, long maxPower, long chargeRate,
                              long consumption, long drain) {
        super(material, type, properties, texture);
        this.maxPower = maxPower;
        this.chargeRate = chargeRate;
        this.consumption = consumption;
        this.drain = drain;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, 
                               List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Charge: " + 
            EnergyFormatter.format(getCharge(stack)) + " / " + 
            EnergyFormatter.format(getMaxCharge(stack)))
            .withStyle(ChatFormatting.AQUA));

        ArmorTooltipHandler.getFSBTooltip(stack).ifPresent(tooltip::addAll);
    }

    @Override
    public boolean isArmorEnabled(ItemStack stack) {
        return getCharge(stack) > 0;
    }

    // @Override
    public void chargeBattery(ItemStack stack, long amount) {
        if (!(stack.getItem() instanceof ModArmorFSBPowered)) return;
        
        long prev = getCharge(stack);
        long newCharge = Math.min(prev + amount, getMaxCharge(stack));
        PlatformHooks.putLong(stack, "charge", newCharge);
        
        // Синхронизация (вызов в onArmorTick или при получении урона)
    }

    public void setCharge(ItemStack stack, long amount) {
        if (stack.getItem() instanceof ModArmorFSBPowered) {
            if (PlatformHooks.hasItemTag(stack)) {
                PlatformHooks.putLong(stack, "charge", amount);
            } else {
                PlatformHooks.setItemTag(stack, new CompoundTag());
                PlatformHooks.putLong(stack, "charge", amount);
            }
        }
    }


    public void dischargeBattery(ItemStack stack, long amount) {
        if (stack.getItem() instanceof ModArmorFSBPowered) {
            long prev = PlatformHooks.getLong(stack, "charge");
            long newCharge = Math.max(0, prev - amount);
            PlatformHooks.putLong(stack, "charge", newCharge);

            // Синхронизация будет вызвана в onArmorTick() или при получении урона
        }
    }

    public long getCharge(ItemStack stack) {
        if (stack.getItem() instanceof ModArmorFSBPowered) {
            if (PlatformHooks.hasItemTag(stack)) {
                return Math.min(PlatformHooks.getLong(stack, "charge"), getMaxCharge(stack));
            } else {
                PlatformHooks.setItemTag(stack, new CompoundTag());
                PlatformHooks.putLong(stack, "charge", getMaxCharge(stack));
                return PlatformHooks.getLong(stack, "charge");
            }
        }
        return 0;
    }

    public boolean showDurabilityBar(ItemStack stack) {
        return getCharge(stack) < getMaxCharge(stack);
    }

    public double getDurabilityForDisplay(ItemStack stack) {
        return 1 - ((double) getCharge(stack) / (double) getMaxCharge(stack));
    }

    public long getMaxCharge(ItemStack stack) {
        return (long) (maxPower * ArmorModificationHelper.getBatteryCapacityMultiplier(stack));
    }

    public long getChargeRate(ItemStack stack) {
        return chargeRate;
    }

    public long getDischargeRate(ItemStack stack) {
        return 0;
    }

    /**
     * MATCHES 1.7.10 BEHAVIOR
     * This is called by Minecraft when armor would take damage.
     * Instead of damaging the item, we drain energy based on consumption.
     * (Forge/NeoForge: IItemExtension#setDamage; на Fabric такого хука нет.)
     */
    //? if !fabric {
    @Override
    public void setDamage(ItemStack stack, int damage) {
        if (this.consumption > 0) {
            this.dischargeBattery(stack, (long) damage * this.consumption);
        }
    }
    //?}

    private int getArmorContainerId(Player player, EquipmentSlot slot) {
        int slotIndex = switch (slot) {
            case HEAD -> ARMOR_SLOT_HEAD;
            case CHEST -> ARMOR_SLOT_CHEST;
            case LEGS -> ARMOR_SLOT_LEGS;
            case FEET -> ARMOR_SLOT_FEET;
            default -> -1;
        };
        // Отрицательное значение = броня (не инвентарь)
        return -(player.getId() * 4 + slotIndex);
    }

    /**
     * MATCHES 1.7.10 BEHAVIOR
     * Passive energy drain per tick when wearing full FSB armor set.
     */
    //? if forge {
    @Override
    public void onArmorTick(ItemStack stack, Level world, Player player) {
        super.onArmorTick(stack, world, player);
        tickPoweredDrain(stack, world, player);
    }
    //?} else {
    /*@Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, world, entity, slotId, selected);
        if (!(entity instanceof Player player)) return;
        tickPoweredDrain(stack, world, player);
    }
    *///?}

    private void tickPoweredDrain(ItemStack stack, Level world, Player player) {
        if (this.drain > 0 && ModArmorFSB.hasFSBArmor(player)
                && !player.getAbilities().instabuild && !player.isSpectator()) {

            long prevCharge = getCharge(stack);
            this.dischargeBattery(stack, drain);
            long newCharge = getCharge(stack);
            long maxCharge = getMaxCharge(stack);

            if (maxCharge > 0 && (Math.abs(newCharge - prevCharge) > maxCharge * 0.05 || newCharge == 0)) {
                //? if < 1.21.1 {
                syncEnergyToClient(player, stack, world, net.minecraft.world.entity.Mob.getEquipmentSlotForItem(stack));
                //?} else {
                /*syncEnergyToClient(player, stack, world, player.getEquipmentSlotForItem(stack));
                *///?}
            }
        }
    }

    public void syncEnergyToClient(Player player, ItemStack stack, Level world, EquipmentSlot slot) {
        if (world.isClientSide() || !(player instanceof ServerPlayer)) return;
        
        long current = getCharge(stack);
        long max = getMaxCharge(stack);
        int containerId = getArmorContainerId(player, slot);
        
        // Отправляем пакет через существующий канал
        com.hbm_m.network.packet.PacketSyncEnergy.sendTo(
                (ServerPlayer) player,
                containerId,
                current,
                max,
                0L
        );
    }

    //? if forge {
    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        long modifiedCapacity = getMaxCharge(stack);
        return new EnergyCapabilityProvider(stack, modifiedCapacity, chargeRate, modifiedCapacity);
    }
    //?}
}