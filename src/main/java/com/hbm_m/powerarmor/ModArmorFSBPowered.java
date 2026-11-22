package com.hbm_m.powerarmor;

import com.hbm_m.platform.StackNbt;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.hbm_m.platform.PlatformHooks;
//? if forge {
import com.hbm_m.api.energy.EnergyCapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
//?}

/**
 * 1:1 {@code com.hbm.items.armor.ArmorFSBPowered}: FSB-Ruestung mit Akku. Treffer ziehen statt Haltbarkeit
 * {@code consumption} HE je Schadenspunkt ab, das volle Set verbraucht {@code drain} HE pro Tick, ohne
 * Ladung ist das Teil abgeschaltet ({@link #isArmorEnabled}).
 */
public class ModArmorFSBPowered extends ModArmorFSB implements com.hbm_m.api.item.IBatteryItem {

    public long maxPower = 1;
    public long chargeRate;
    public long consumption;
    public long drain;

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
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Charge: " + BobMathUtil.getShortNumber(getCharge(stack)) + " / " + BobMathUtil.getShortNumber(getMaxCharge(stack))));
        super.appendHbmTooltip(stack, level, list, flag);
    }

    @Override
    public boolean isArmorEnabled(ItemStack stack) {
        return getCharge(stack) > 0;
    }

    @Override
    public void chargeBattery(ItemStack stack, long i) {
        if (stack.getItem() instanceof ModArmorFSBPowered) {
            if (StackNbt.has(stack)) {
                StackNbt.tag(stack).putLong("charge", StackNbt.read(stack).getLong("charge") + i);
            } else {
                StackNbt.orCreate(stack).putLong("charge", i);
            }
        }
    }

    @Override
    public void setCharge(ItemStack stack, long i) {
        if (stack.getItem() instanceof ModArmorFSBPowered) {
            StackNbt.orCreate(stack).putLong("charge", i);
        }
    }

    @Override
    public void dischargeBattery(ItemStack stack, long i) {
        if (stack.getItem() instanceof ModArmorFSBPowered) {
            if (StackNbt.has(stack)) {
                StackNbt.tag(stack).putLong("charge", StackNbt.read(stack).getLong("charge") - i);
            } else {
                StackNbt.orCreate(stack).putLong("charge", getMaxCharge(stack) - i);
            }

            if (StackNbt.read(stack).getLong("charge") < 0)
                StackNbt.tag(stack).putLong("charge", 0);
        }
    }

    public long getCharge(ItemStack stack) {
        if (stack.getItem() instanceof ModArmorFSBPowered) {
            if (StackNbt.has(stack)) {
                return Math.min(StackNbt.read(stack).getLong("charge"), getMaxCharge(stack));
            } else {
                CompoundTag t = StackNbt.orCreate(stack);
                t.putLong("charge", getMaxCharge(stack));
                return t.getLong("charge");
            }
        }
        return 0;
    }

    /** Original showDurabilityBar. */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) < getMaxCharge(stack);
    }

    /** Original getDurabilityForDisplay. */
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1 - (double) getCharge(stack) / (double) getMaxCharge(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13.0D - getDurabilityForDisplay(stack) * 13.0D);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = Math.max(0.0F, (float) (1.0D - getDurabilityForDisplay(stack)));
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
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
    @Override
    public void setDamage(ItemStack stack, int damage) {
        // 1.20: ItemStack.setTag ruft setDamageValue(0) auf - das darf nichts tun (sonst Endlosschleife).
        if (damage <= 0) return;
        this.dischargeBattery(stack, damage * consumption);
    }

    //? if forge {
    @Override
    @SuppressWarnings("removal")
    //?} else {
    /*@Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, world, entity, slotId, selected);
        if (world.isClientSide()) return;
        if (!(entity instanceof Player player)) return;
        // Both checks exist in the neighbouring overrides: without them spare armour pieces drained
        // while sitting in the inventory, and the client drained its own copy of the stack too.
        if (!stackIsEquippedArmor(player, stack)) return;
        armorTick(stack, world, player);
    }
    *///?}
    public void onArmorTick(ItemStack stack, Level world, Player player) {
        super.onArmorTick(stack, world, player);
        armorTick(stack, world, player);
    }

    /** Original onArmorTick-Rumpf der Unterklassen (nach dem Akku-Abzug). */
    protected void armorTick(ItemStack stack, Level world, Player player) {
        if (this.drain > 0 && ModArmorFSB.hasFSBArmor(player) && !player.getAbilities().instabuild) {
            this.dischargeBattery(stack, drain);
        }
    }

    // ── Energie-Synchronisation (aus machines): nach Dash/grossem Abzug den Ladestand an den Client schicken ──

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

    public void syncEnergyToClient(Player player, ItemStack stack, Level world, EquipmentSlot slot) {
        if (world.isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer)) return;

        long current = getCharge(stack);
        long max = getMaxCharge(stack);
        int containerId = getArmorContainerId(player, slot);

        // Отправляем пакет через существующий канал
        com.hbm_m.network.packet.PacketSyncEnergy.sendTo(
                (net.minecraft.server.level.ServerPlayer) player,
                containerId,
                current,
                max,
                0L
        );
    }

    // neo-pendant: ModCapabilities.registerBatteryItemCaps (RegisterCapabilitiesEvent, Item-Capabilities)
    //? if forge {
    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        long modifiedCapacity = getMaxCharge(stack);
        return new EnergyCapabilityProvider(stack, modifiedCapacity, chargeRate, modifiedCapacity);
    }
    //?}

    // Взаимная блокировка с маской в слоте лица Curios: шлем силовой брони нельзя
    // надеть, пока там стоит противогаз (и наоборот — см. GasMaskCurio/ArmorGasMaskItem).
    //? if < 1.21.1 {
    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, Entity entity) {
        return super.canEquip(stack, slot, entity)
                && (slot != EquipmentSlot.HEAD
                    || !(entity instanceof LivingEntity living)
                    || com.hbm_m.compat.curios.CuriosCompat.getFaceMask(living).isEmpty());
    }
    //?} else {
    /*@Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, LivingEntity entity) {
        return super.canEquip(stack, slot, entity)
                && (slot != EquipmentSlot.HEAD
                    || com.hbm_m.compat.curios.CuriosCompat.getFaceMask(entity).isEmpty());
    }
     *///?}
}
