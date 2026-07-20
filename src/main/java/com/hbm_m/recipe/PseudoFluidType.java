package com.hbm_m.recipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * Direct port of the 1.7.10 Gas Centrifuge's {@code GasCentrifugeRecipes.PseudoFluidType}.
 * Represents an internal enrichment stage that is not a real registered {@link Fluid} — it only
 * exists inside a centrifuge's {@code inputTank}/{@code outputTank} and is handed off between
 * cascaded centrifuges.
 */
public final class PseudoFluidType {

    private static final Map<String, PseudoFluidType> TYPES = new HashMap<>();

    public static final PseudoFluidType NONE = new PseudoFluidType("none", 0, 0, null, false);

    // Uranium enrichment chain: NUF6 -> LEUF6 -> MEUF6 -> HEUF6 -> (none)
    public static final PseudoFluidType HEUF6 = new PseudoFluidType("heuf6", 300, 0, NONE, true,
            new ItemStack(ModItems.NUGGET_U238.get(), 2),
            new ItemStack(ModItems.NUGGET_U235.get(), 1),
            new ItemStack(ModItems.FLUORITE.get(), 1));
    public static final PseudoFluidType MEUF6 = new PseudoFluidType("meuf6", 200, 100, HEUF6, false,
            new ItemStack(ModItems.NUGGET_U238.get(), 1));
    public static final PseudoFluidType LEUF6 = new PseudoFluidType("leuf6", 300, 200, MEUF6, false,
            new ItemStack(ModItems.NUGGET_U238.get(), 1),
            new ItemStack(ModItems.FLUORITE.get(), 1));
    public static final PseudoFluidType NUF6 = new PseudoFluidType("nuf6", 400, 300, LEUF6, false,
            new ItemStack(ModItems.NUGGET_U238.get(), 1));

    // Plutonium enrichment: single step.
    public static final PseudoFluidType PF6 = new PseudoFluidType("pf6", 300, 0, NONE, false,
            new ItemStack(ModItems.NUGGET_PU238.get(), 1),
            new ItemStack(ModItems.NUGGET_PU_MIX.get(), 2),
            new ItemStack(ModItems.FLUORITE.get(), 1));

    // WATZ byproduct chain: MUD -> MUD_HEAVY -> (none)
    public static final PseudoFluidType MUD_HEAVY = new PseudoFluidType("mud_heavy", 500, 0, NONE, false,
            new ItemStack(ModItems.getPowders(ModPowders.IRON).get(), 1),
            new ItemStack(ModItems.DUST.get(), 1),
            new ItemStack(ModItems.NUCLEAR_WASTE_TINY.get(), 1));
    public static final PseudoFluidType MUD = new PseudoFluidType("mud", 1000, 500, MUD_HEAVY, false,
            new ItemStack(ModItems.getPowder(ModIngots.LEAD).get(), 1),
            new ItemStack(ModItems.DUST.get(), 1));

    /** Maps a real piped {@link Fluid} to the pseudo-fluid stage a centrifuge starts converting it into. */
    public static final Map<Fluid, PseudoFluidType> FLUID_CONVERSIONS = new HashMap<>();
    static {
        FLUID_CONVERSIONS.put(ModFluids.UF6.getSource(), NUF6);
        FLUID_CONVERSIONS.put(ModFluids.PUF6.getSource(), PF6);
        FLUID_CONVERSIONS.put(ModFluids.WATZ.getSource(), MUD);
    }

    private final String id;
    private final int fluidConsumed;
    private final int fluidProduced;
    private final PseudoFluidType outputType;
    private final boolean highSpeed;
    private final List<ItemStack> output;

    private PseudoFluidType(String id, int fluidConsumed, int fluidProduced,
                             PseudoFluidType outputType, boolean highSpeed, ItemStack... output) {
        this.id = id;
        this.fluidConsumed = fluidConsumed;
        this.fluidProduced = fluidProduced;
        this.outputType = outputType;
        this.highSpeed = highSpeed;
        this.output = output == null ? List.of() : List.of(output);
        TYPES.put(id, this);
    }

    public String getId() { return id; }
    public int getFluidConsumed() { return fluidConsumed; }
    public int getFluidProduced() { return fluidProduced; }
    /** Next stage this pseudo-fluid should be handed off as, or {@code null} if this is the final stage. */
    public PseudoFluidType getOutputType() { return outputType; }
    public boolean isHighSpeed() { return highSpeed; }
    public List<ItemStack> getOutput() { return output; }
    public Component getDisplayName() { return Component.translatable("pseudofluid.hbm_m." + id); }

    public static PseudoFluidType byId(String id) {
        return TYPES.get(id);
    }
}
