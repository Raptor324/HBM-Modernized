package com.hbm_m.item.industrial;

import com.hbm_m.item.ITooltipProvider;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Апгрейд машины — порт ItemMachineUpgrade из 1.7.10.
 *
 * Каждый предмет имеет {@link UpgradeType} и числовой tier
 * (Mk.I = 1, Mk.II = 2, Mk.III = 3). Уровень суммируется
 * в {@link com.hbm_m.inventory.UpgradeManager}.
 */
public class ItemMachineUpgrade extends Item implements ITooltipProvider {

    public enum UpgradeType {
        SPEED,
        EFFECT,
        POWER,
        FORTUNE,
        AFTERBURN,
        OVERDRIVE,
        STACK,
        EJECTOR,
        /** Kraftfeld: Radius +16, Verbrauch +500 je Stueck im Stapel. */
        RADIUS,
        /** Kraftfeld: Schild +50, Verbrauch +250 je Stueck im Stapel. */
        HEALTH,
        // Original-Typen: SPECIAL (Einzelstuecke, per Item-Vergleich), Bergbaulaser, Gaszentrifuge
        SPECIAL,
        LM_DESROYER,
        LM_SCREM,
        LM_SMELTER(true),
        LM_SHREDDER(true),
        LM_CENTRIFUGE(true),
        LM_CRYSTALLIZER(true),
        GS_SPEED;

        public boolean mutex = false;

        UpgradeType() { }

        UpgradeType(boolean mutex) {
            this.mutex = mutex;
        }

        public String getTranslationKeySuffix() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private final UpgradeType type;
    private final int tier;

    public ItemMachineUpgrade(Properties properties, UpgradeType type, int tier) {
        super(properties.stacksTo(1));
        this.type = type;
        this.tier = tier;
    }

    public ItemMachineUpgrade(Properties properties, UpgradeType type) {
        this(properties, type, 0);
    }

    /**
     * Fuer Aufwertungen, die sich stapeln duerfen. Im Original sind das nur die beiden des
     * Kraftfelds, deren Wirkung sich nach der Stapelgroesse richtet statt nach einer Stufe.
     */
    public ItemMachineUpgrade(Properties properties, UpgradeType type, int tier, int maxStackSize) {
        super(properties.stacksTo(maxStackSize));
        this.type = type;
        this.tier = tier;
    }

    public UpgradeType getUpgradeType() {
        return type;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        boolean ext = flag.isAdvanced();

        // Original: die offene Maschinen-GUI liefert die Wirkung dieses Upgrades (IUpgradeInfoProvider)
        if (level != null && level.isClientSide && ClientHook.provideInfo(this.type, this.tier, list, ext)) return;

        if (this == com.hbm_m.item.ModItems.UPGRADE_RADIUS.get()) {
            list.add(Component.literal("Forcefield Range Upgrade").withStyle(ChatFormatting.RED));
            list.add(Component.literal("Radius +16 / Consumption +500"));
            list.add(Component.literal("Stacks to 16"));
        }

        if (this == com.hbm_m.item.ModItems.UPGRADE_HEALTH.get()) {
            list.add(Component.literal("Forcefield Health Upgrade").withStyle(ChatFormatting.RED));
            list.add(Component.literal("Max. Health +50 / Consumption +250"));
            list.add(Component.literal("Stacks to 16"));
        }

        if (this == com.hbm_m.item.ModItems.UPGRADE_SMELTER.get()) {
            list.add(Component.literal("Mining Laser Upgrade").withStyle(ChatFormatting.RED));
            list.add(Component.literal("Smelts blocks. Easy enough."));
        }

        if (this == com.hbm_m.item.ModItems.UPGRADE_SHREDDER.get()) {
            list.add(Component.literal("Mining Laser Upgrade").withStyle(ChatFormatting.RED));
            list.add(Component.literal("Crunches ores"));
        }

        if (this == com.hbm_m.item.ModItems.UPGRADE_CENTRIFUGE.get()) {
            list.add(Component.literal("Mining Laser Upgrade").withStyle(ChatFormatting.RED));
            list.add(Component.literal("Hopefully self-explanatory"));
        }

        if (this == com.hbm_m.item.ModItems.UPGRADE_CRYSTALLIZER.get()) {
            list.add(Component.literal("Mining Laser Upgrade").withStyle(ChatFormatting.RED));
            list.add(Component.literal("Your new best friend"));
        }

        if (this == com.hbm_m.item.ModItems.UPGRADE_SCREM.get()) {
            list.add(Component.literal("Mining Laser Upgrade").withStyle(ChatFormatting.RED));
            list.add(Component.literal("It's like in Super Mario where all blocks are"));
            list.add(Component.literal("actually Toads, but here it's Half-Life scientists"));
            list.add(Component.literal("and they scream. A lot."));
        }

        if (this == com.hbm_m.item.ModItems.UPGRADE_NULLIFIER.get()) {
            list.add(Component.literal("Mining Laser Upgrade").withStyle(ChatFormatting.RED));
            list.add(Component.literal("50% chance to override worthless items with /dev/zero"));
            list.add(Component.literal("50% chance to move worthless items to /dev/null"));
        }

        if (this == com.hbm_m.item.ModItems.UPGRADE_GC_SPEED.get()) {
            list.add(Component.literal("Gas Centrifuge Upgrade").withStyle(ChatFormatting.RED));
            list.add(Component.literal("Allows for total isotopic separation of HEUF6"));
            list.add(Component.literal("also your centrifuge goes sicko mode").withStyle(ChatFormatting.YELLOW));
        }
    }

    /** Clientteil: die offene Container-GUI fragen (Original: erster Slot -> IInventory -> IUpgradeInfoProvider). */
    private static final class ClientHook {
        static boolean provideInfo(UpgradeType type, int tier, List<Component> list, boolean ext) {
            net.minecraft.client.gui.screens.Screen open = net.minecraft.client.Minecraft.getInstance().screen;
            if (!(open instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> gui)) return false;
            Object be;
            try {
                java.lang.reflect.Method m = gui.getMenu().getClass().getMethod("getBlockEntity");
                be = m.invoke(gui.getMenu());
            } catch (Throwable t) {
                return false;
            }
            if (be instanceof com.hbm_m.interfaces.IUpgradeInfoProvider provider && provider.canProvideInfo(type, tier, ext)) {
                provider.provideInfo(type, tier, list, ext);
                return true;
            }
            return false;
        }
    }
}
