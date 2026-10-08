package com.hbm_m.item.machine;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code ItemPACoil} (1.7.10): die Magnetspulen des Teilchenbeschleunigers.
 *
 * <p>Das Original ist ein einzelnes Item mit vier Metadaten-Varianten. Da dieser Port keine
 * Metadaten-Items kennt, gibt es vier eigene Items, die sich denselben {@link CoilType} teilen.</p>
 *
 * <p>Die vier Werte je Sorte begrenzen, womit ein Quadrupol beziehungsweise ein Dipol umgehen kann:
 * unterhalb von {@code quadMin}/{@code diMin} arbeitet das Bauteil zwar, braucht aber die zehnfache
 * Energie; oberhalb von {@code quadMax}/{@code diMax} verliert der Strahl die Spur.</p>
 */
public class ItemPACoil extends Item {

    /** 1:1 aus {@code ItemPACoil.EnumCoilType}. */
    public enum CoilType {
        GOLD(0, 2_200, 0, 2_200, 15),
        NIOBIUM(1_500, 8_400, 1_500, 8_400, 21),
        BSCCO(7_500, 15_000, 7_500, 15_000, 27),
        CHLOROPHYTE(14_500, 75_000, 14_500, 75_000, 51);

        /** Impuls, ab dem der Quadrupol sparsam arbeitet. */
        public final int quadMin;
        /** Impuls, ab dem der Quadrupol den Strahl verliert. */
        public final int quadMax;
        public final int diMin;
        public final int diMax;
        /** Mindeststrecke seit der letzten Ablenkung, damit der Dipol sparsam arbeitet. */
        public final int diDistMin;

        CoilType(int quadMin, int quadMax, int diMin, int diMax, int diDistMin) {
            this.quadMin = quadMin;
            this.quadMax = quadMax;
            this.diMin = diMin;
            this.diMax = diMax;
            this.diDistMin = diDistMin;
        }
    }

    private final CoilType type;

    public ItemPACoil(Properties properties, CoilType type) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    public CoilType getCoilType() {
        return type;
    }

    /** Liefert die Sorte, wenn der Stapel eine Spule ist - sonst {@code null}. */
    @Nullable
    public static CoilType typeOf(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemPACoil coil ? coil.getCoilType() : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // 1:1 Original addInformation: blaue Beschriftung, Wert nach RESET weiss, drei rote Warnzeilen
        tooltip.add(Component.literal("Quadrupole operational range: ").withStyle(ChatFormatting.BLUE)
                .append(Component.literal(String.format(java.util.Locale.US, "%,d", type.quadMin) + " - " + String.format(java.util.Locale.US, "%,d", type.quadMax)).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("Dipole operational range: ").withStyle(ChatFormatting.BLUE)
                .append(Component.literal(String.format(java.util.Locale.US, "%,d", type.diMin) + " - " + String.format(java.util.Locale.US, "%,d", type.diMax)).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("Dipole minimum side length: ").withStyle(ChatFormatting.BLUE)
                .append(Component.literal(String.valueOf(type.diDistMin)).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("Minimums not met result in a power draw penalty!").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("Maximums exceeded result in the particle crashing!").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("Particles will crash in dipoles if both penalties take effect!").withStyle(ChatFormatting.RED));
    }
}
