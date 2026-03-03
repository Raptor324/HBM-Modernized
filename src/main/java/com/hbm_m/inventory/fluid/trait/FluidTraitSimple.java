package com.hbm_m.inventory.fluid.trait;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class FluidTraitSimple {

    public static class FT_Gaseous extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[Gaseous]").withStyle(ChatFormatting.BLUE));
        }
    }

    public static class FT_Gaseous_ART extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[Gaseous at Room Temperature]").withStyle(ChatFormatting.BLUE));
        }
    }

    public static class FT_Liquid extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[Liquid]").withStyle(ChatFormatting.BLUE));
        }
    }

    public static class FT_Viscous extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[Viscous]").withStyle(ChatFormatting.BLUE));
        }
    }

    public static class FT_Plasma extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[Plasma]").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static class FT_Amat extends FluidTrait {
        @Override public void addInfo(List<Component> info) {
            info.add(Component.literal("[Antimatter]").withStyle(ChatFormatting.DARK_RED));
        }
    }

    public static class FT_LeadContainer extends FluidTrait {
        @Override public void addInfo(List<Component> info) {
            info.add(Component.literal("[Requires hazardous material tank to hold]").withStyle(ChatFormatting.DARK_RED));
        }
    }

    public static class FT_Delicious extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[Delicious]").withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    public static class FT_Unsiphonable extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[Ignored by siphon]").withStyle(ChatFormatting.BLUE));
        }
    }

    public static class FT_NoID extends FluidTrait { }
    public static class FT_NoContainer extends FluidTrait { }
}