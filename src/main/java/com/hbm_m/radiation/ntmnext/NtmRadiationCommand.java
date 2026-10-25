// Port-eigene Klasse (HBM-Modernized), Stufe 2: Ausgabe je Strahlungsart fuer /ntmrad.

package com.hbm_m.radiation.ntmnext;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** Hilfen fuer die /ntmrad-Unterbefehle des NTM-Next-Systems. */
public final class NtmRadiationCommand {

    private NtmRadiationCommand() {}

    /** /ntmrad info: Feld, Nahfeld und Gewicht je Art sowie die Summe an der eigenen Position. */
    public static int info(CommandSourceStack src) {
        if (!NtmRadiationApi.isActive()) {
            src.sendSystemMessage(
                    Component.literal("Radiation system is RAPTOR; per-type info needs ADVANCED.")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }
        ServerLevel level = src.getLevel();
        BlockPos pos = BlockPos.containing(src.getPosition());
        float[] near = NtmRadiationSystem.nearFieldAt(level, pos);
        src.sendSystemMessage(
                Component.literal("Radiation at " + pos.toShortString() + " (" + level.dimension().location() + ")")
                        .withStyle(ChatFormatting.YELLOW));
        for (RadiationType type : RadiationType.FIELD_TYPES) {
            double field = NtmRadiationSystem.getRadForCoord(level, pos, type);
            double nf = near[type.ordinal()];
            double weight = NtmRadiationConfig.typeWeight[type.ordinal()];
            src.sendSystemMessage(
                    Component.literal(
                                    String.format(
                                            Locale.ROOT,
                                            "  %-7s field %.3f  near %.3f  x%.2f = %.3f RAD/s",
                                            type.id,
                                            field,
                                            nf,
                                            weight,
                                            (field + nf) * weight))
                            .withStyle(ChatFormatting.GRAY));
        }
        for (ContaminationGroup g : ContaminationGroup.values()) {
            double amount = NtmRadiationSystem.contaminationAt(level, pos, g);
            if (amount <= 0.0D) continue;
            double airborne = NtmRadiationSystem.airborneAt(level, pos, g);
            src.sendSystemMessage(
                    Component.literal(
                                    String.format(
                                            Locale.ROOT,
                                            "  contamination %-6s ground %.3f  airborne %.3f  (half-life %s)",
                                            g.id,
                                            amount - airborne,
                                            airborne,
                                            formatSeconds(NtmRadiationConfig.contamHalfLife[g.ordinal()])))
                            .withStyle(ChatFormatting.GOLD));
        }
        double halving = NtmRadiationSystem.contaminationHalvingSeconds(level, pos);
        if (Double.isFinite(halving)) {
            src.sendSystemMessage(
                    Component.literal(
                                    "  contamination halves in " + formatSeconds(halving)
                                            + ", alpha hazard "
                                            + String.format(Locale.ROOT, "%.3f", NtmRadiationApi.alphaHazardAt(level, pos)))
                            .withStyle(ChatFormatting.GOLD));
        }
        src.sendSystemMessage(
                Component.literal(
                                String.format(
                                        Locale.ROOT,
                                        "  total %.3f RAD/s, ambient %.3f, dose %.3f",
                                        NtmRadiationSystem.totalAt(level, pos, true),
                                        NtmRadiationSystem.ambientRad(level),
                                        NtmRadiationSystem.doseAt(level, pos)))
                        .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    /** Sekunden als Spielzeit (1 Spieltag = 20 min). */
    static String formatSeconds(double s) {
        if (!Double.isFinite(s) || s > 1.0e9D) return "permanent";
        double days = s / 1200.0D;
        if (days >= 1.0D) return String.format(Locale.ROOT, "%.1f game days", days);
        if (s >= 60.0D) return String.format(Locale.ROOT, "%.1f min", s / 60.0D);
        return String.format(Locale.ROOT, "%.0f s", s);
    }

    /** /ntmrad contaminate <group> <amount>: Kontamination an der eigenen Position ablegen. */
    public static int contaminate(CommandSourceStack src, String groupId, double amount) {
        ContaminationGroup group = ContaminationGroup.byId(groupId);
        if (group == null || !NtmRadiationApi.isActive()) {
            src.sendSystemMessage(
                    Component.literal("Usage: /ntmrad contaminate <short|medium|long|exotic> <amount> (ADVANCED only)")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }
        NtmRadiationApi.contaminate(src.getLevel(), BlockPos.containing(src.getPosition()), group, amount);
        src.sendSystemMessage(Component.literal("Contaminated with " + amount + " " + group.id + "."));
        return 1;
    }

    /** /ntmrad wind: aktuelle Windrichtung und -staerke. */
    public static int wind(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        if (!WindModel.hasWind(level)) {
            src.sendSystemMessage(Component.literal("No wind in this dimension (or wind disabled).")
                    .withStyle(ChatFormatting.GRAY));
            return 1;
        }
        double[] v = WindModel.velocity(level);
        double speed = Math.sqrt(v[0] * v[0] + v[1] * v[1]);
        // Kompassrichtung, in die der Wind weht (+Z = Sueden, +X = Osten).
        double deg = (Math.toDegrees(Math.atan2(v[0], -v[1])) + 360.0D) % 360.0D;
        String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        String dir = names[(int) Math.round(deg / 45.0D) % 8];
        src.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                        "Wind blowing towards %s (%.0f deg), %.2f blocks/s%s%s",
                        dir, deg, speed,
                        level.isRaining() ? ", raining" : "",
                        level.isThundering() ? ", thunder" : ""))
                .withStyle(ChatFormatting.AQUA));
        return 1;
    }

    /** /ntmrad add <type> <amount>: Eintrag genau einer Art an der eigenen Position. */
    public static int add(CommandSourceStack src, String typeId, double amount) {
        RadiationType type = RadiationType.byId(typeId);
        if (type == null || !type.hasField || !NtmRadiationApi.isActive()) {
            src.sendSystemMessage(
                    Component.literal("Usage: /ntmrad add <gamma|neutron|beta> <amount> (ADVANCED only)")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }
        NtmRadiationApi.add(src.getLevel(), BlockPos.containing(src.getPosition()), type, amount);
        src.sendSystemMessage(Component.literal("Added " + amount + " " + type.id + "."));
        return 1;
    }
}
