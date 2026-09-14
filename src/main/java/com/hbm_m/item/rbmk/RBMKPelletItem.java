package com.hbm_m.item.rbmk;

import com.hbm_m.blockentity.machines.rbmk.IRBMKFluxReceiver.NType;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Raw RBMK fuel pellet, 1:1 with the original's {@code com.hbm.items.machine.ItemRBMKPellet}.
 *
 * <p>The original encoded the pellet's condition in the item damage value: {@code meta % 5} is the
 * enrichment tier (0 = brand new … 4 = fully depleted) and {@code meta >= 5} additionally flags high
 * xenon poison, giving ten states per pellet type. Since 1.13 has no item metadata, the same value
 * lives in the stack's NBT/data component here and is exposed to the model via the
 * {@code hbm_m:pellet_state} item property, which reproduces the original's enrichment/xenon
 * overlay render passes as model layers.</p>
 *
 * <p>Pellets are never a crafting ingredient for rods - the original only ever produces them by
 * disassembling a rod ({@code RBMKFuelDisassemblyRecipe}); they exist for recycling.</p>
 */
public class RBMKPelletItem extends Item {

    /** Every registered pellet, used to bind the model property on the client. */
    public static final List<RBMKPelletItem> pellets = new ArrayList<>();

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

    /** Original: pellets without a xenon variant only have the five enrichment states. */
    protected boolean hasXenon = true;

    public RBMKPelletItem(Properties props) {
        super(props);
        pellets.add(this);
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

    /** ItemRBMKPellet.disableXenon() - no xenon-poisoned variants for this pellet type. */
    public RBMKPelletItem disableXenon()                    { this.hasXenon = false; return this; }

    public boolean isXenonEnabled() { return hasXenon; }

    /** Number of distinct states this pellet has (ItemRBMKPellet.getSubItems). */
    public int stateCount() { return hasXenon ? 10 : 5; }

    // ─── State (the original's item damage) ───────────────────────────────────

    /** ItemRBMKPellet.rectify - clamps any value into the valid 0-9 range. */
    public static int rectify(int meta) { return Math.abs(meta) % 10; }

    /** ItemRBMKPellet.hasXenon(meta) - states 5-9 are the xenon-poisoned ones. */
    public static boolean isPoisoned(int meta) { return rectify(meta) >= 5; }

    //? if < 1.21.1 {
    public static int getState(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : rectify(tag.getInt("pellet_state"));
    }

    public static void setState(ItemStack stack, int state) {
        stack.getOrCreateTag().putInt("pellet_state", rectify(state));
    }
    //?} else {
    /*public static int getState(ItemStack stack) {
        net.minecraft.world.item.component.CustomData data =
            stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data == null ? 0 : rectify(data.copyTag().getInt("pellet_state"));
    }

    public static void setState(ItemStack stack, int state) {
        net.minecraft.world.item.component.CustomData data =
            stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        CompoundTag tag = data != null ? data.copyTag() : new CompoundTag();
        tag.putInt("pellet_state", rectify(state));
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.of(tag));
    }
    *///?}

    /** Convenience for the disassembly recipe: builds a stack in the given state. */
    public ItemStack withState(int count, int state) {
        ItemStack stack = new ItemStack(this, count);
        if (rectify(state) != 0) setState(stack, state);
        return stack;
    }

    // ─── Tooltip (ItemRBMKPellet.addInformation) ──────────────────────────────

    //? if < 1.21.1 {
    // @Override omitted intentionally — Stonecutter removes this block for >= 1.21.1
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal(ChatFormatting.ITALIC + fullName));
        list.add(Component.literal(ChatFormatting.DARK_GRAY.toString() + ChatFormatting.ITALIC + "Pellet for recycling"));

        int meta = getState(stack);

        switch (meta % 5) {
            case 0 -> list.add(Component.literal("Brand New").withStyle(ChatFormatting.GOLD));
            case 1 -> list.add(Component.literal("Barely Depleted").withStyle(ChatFormatting.YELLOW));
            case 2 -> list.add(Component.literal("Moderately Depleted").withStyle(ChatFormatting.GREEN));
            case 3 -> list.add(Component.literal("Highly Depleted").withStyle(ChatFormatting.DARK_GREEN));
            case 4 -> list.add(Component.literal("Fully Depleted").withStyle(ChatFormatting.DARK_GRAY));
        }

        if (isPoisoned(meta))
            list.add(Component.literal("High Xenon Poison").withStyle(ChatFormatting.DARK_PURPLE));
    }

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
