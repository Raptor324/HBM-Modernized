package com.hbm_m.item.rbmk;

import com.hbm_m.blockentity.machines.rbmk.IRBMKFluxReceiver.NType;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Represents a raw RBMK fuel pellet. Used to craft fuel rods.
 * Carries all stats that are copied to the rod item.
 */
public class RBMKPelletItem extends Item {

    public String fullName    = "";
    public double reactivity  = 1.0;
    public double selfRate    = 0;
    public double xGen        = 0.5;
    public double xBurn       = 50;
    public double heat        = 1.0;
    public double yield       = 10_000;
    public double meltingPoint = 1000;
    public double diffusion   = 0.02;
    public NType nType        = NType.SLOW;
    public NType rType        = NType.FAST;
    public int colorTint      = 0x304825;

    public RBMKPelletItem(Properties props) {
        super(props);
    }

    public RBMKPelletItem setFullName(String name)          { this.fullName = name; return this; }
    public RBMKPelletItem setReactivity(double r)           { this.reactivity = r; return this; }
    public RBMKPelletItem setYield(double y)                { this.yield = y; return this; }
    public RBMKPelletItem setHeat(double h)                 { this.heat = h; return this; }
    public RBMKPelletItem setMeltingPoint(double mp)        { this.meltingPoint = mp; return this; }
    public RBMKPelletItem setDiffusion(double d)            { this.diffusion = d; return this; }
    public RBMKPelletItem setXenon(double gen, double burn) { this.xGen = gen; this.xBurn = burn; return this; }
    public RBMKPelletItem setNeutronTypes(NType n, NType r) { this.nType = n; this.rType = r; return this; }
    public RBMKPelletItem setTint(int tint)                 { this.colorTint = tint; return this; }

    /** Copies pellet stats onto a rod item. */
    public static void applyToRod(RBMKPelletItem pellet, RBMKRodItem rod) {
        rod.reactivity  = pellet.reactivity;
        rod.selfRate    = pellet.selfRate;
        rod.xGen        = pellet.xGen;
        rod.xBurn       = pellet.xBurn;
        rod.heat        = pellet.heat;
        rod.yield       = pellet.yield;
        rod.meltingPoint = pellet.meltingPoint;
        rod.diffusion   = pellet.diffusion;
        rod.nType       = pellet.nType;
        rod.rType       = pellet.rType;
        rod.colorTint   = pellet.colorTint;
    }

    /** Creates a fresh rod ItemStack with default NBT. */
    public ItemStack createRod(RBMKRodItem rodItem) {
        ItemStack stack = new ItemStack(rodItem);
        RBMKRodItem.setYield(stack, yield);
        RBMKRodItem.setCoreHeat(stack, 20.0);
        RBMKRodItem.setHullHeat(stack, 20.0);
        return stack;
    }
}
