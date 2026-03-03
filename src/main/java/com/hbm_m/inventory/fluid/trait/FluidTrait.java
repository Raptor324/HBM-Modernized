package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.HashBiMap;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public abstract class FluidTrait {
    
    public static List<Class<? extends FluidTrait>> traitList = new ArrayList<>();
    public static HashBiMap<String, Class<? extends FluidTrait>> traitNameMap = HashBiMap.create();
    
    static {
        // complex traits with values
        registerTrait("corrosive", FT_Corrosive.class);
        registerTrait("flammable", FT_Flammable.class);
        registerTrait("combustible", FT_Combustible.class);
        registerTrait("coolable", FT_Coolable.class);
        registerTrait("ventradiation", FT_VentRadiation.class);
        // TODO: Добавить FT_Polluting, FT_Heatable, FT_Poison/Toxin по мере портирования
        
        // simple traits, "tags"
        registerTrait("gaseous", FluidTraitSimple.FT_Gaseous.class);
        registerTrait("gaseous_art", FluidTraitSimple.FT_Gaseous_ART.class);
        registerTrait("liquid", FluidTraitSimple.FT_Liquid.class);
        registerTrait("viscous", FluidTraitSimple.FT_Viscous.class);
        registerTrait("plasma", FluidTraitSimple.FT_Plasma.class);
        registerTrait("amat", FluidTraitSimple.FT_Amat.class);
        registerTrait("leadcontainer", FluidTraitSimple.FT_LeadContainer.class);
        registerTrait("delicious", FluidTraitSimple.FT_Delicious.class);
        registerTrait("noid", FluidTraitSimple.FT_NoID.class);
        registerTrait("nocontainer", FluidTraitSimple.FT_NoContainer.class);
        registerTrait("unsiphonable", FluidTraitSimple.FT_Unsiphonable.class);
    }
    
    protected static void registerTrait(String name, Class<? extends FluidTrait> clazz) {
        traitNameMap.put(name, clazz);
        traitList.add(clazz);
    }

    /** Important information that should always be displayed */
    public void addInfo(List<Component> info) { }
    
    /** General names of simple traits which are displayed when holding shift */
    public void addInfoHidden(List<Component> info) { }
    
    public void onFluidRelease(Level level, BlockPos pos, FluidTank tank, int overflowAmount, FluidReleaseType type) { }

    public void serializeJSON(JsonWriter writer) throws IOException { }
    public void deserializeJSON(JsonObject obj) { }
    
    public enum FluidReleaseType {
        VOID,   // if fluid is deleted entirely, shouldn't be used
        BURN,   // if fluid is burned or combusted
        SPILL   // if fluid is spilled via leakage or the container breaking
    }
}