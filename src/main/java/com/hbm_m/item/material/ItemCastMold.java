package com.hbm_m.item.material;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemCastMold extends Item {

    public enum MoldType {
        PLATE         ("Cast Plate Mold"),
        PLATE_CAST    ("Cast Plate Mold (Cast)"),
        PLATES        ("Cast Plates Mold"),
        PLATES_CAST   ("Cast Plates Mold (Cast)"),
        INGOT         ("Cast Ingot Mold"),
        INGOTS        ("Cast Ingots Mold"),
        NUGGET        ("Cast Nugget Mold"),
        WIRE          ("Cast Wire Mold"),
        WIRE_DENSE    ("Cast Dense Wire Mold"),
        WIRES_DENSE   ("Cast Dense Wires Mold"),
        PIPE          ("Cast Pipe Mold"),
        PIPES         ("Cast Pipes Mold"),
        BLOCK         ("Cast Block Mold"),
        BILLET        ("Cast Billet Mold"),
        BLADE         ("Cast Blade Mold"),
        BLADES        ("Cast Blades Mold"),
        GEM           ("Cast Gem Mold"),
        HULL_SMALL    ("Cast Small Hull Mold"),
        HULL_BIG      ("Cast Big Hull Mold"),
        SHELL         ("Cast Shell Mold"),
        MECHANISM     ("Cast Mechanism Mold"),
        GRIP          ("Cast Grip Mold"),
        STOCK         ("Cast Stock Mold"),
        BARREL_LIGHT  ("Cast Light Barrel Mold"),
        BARREL_HEAVY  ("Cast Heavy Barrel Mold"),
        RECEIVER_LIGHT("Cast Light Receiver Mold"),
        RECEIVER_HEAVY("Cast Heavy Receiver Mold"),
        BASE          ("Cast Base Mold"),
        STEEL_BASE    ("Cast Steel Base Mold"),
        STAMP         ("Cast Stamp Mold"),
        C357          ("Cast .357 Casing Mold"),
        CBUCKSHOT     ("Cast Buckshot Mold"),
        MOGUS         ("Cast Mogus Mold");

        public final String label;
        MoldType(String label) { this.label = label; }

        /**
         * Стоимость заливки в mB (ёмкость формы foundry basin).
         * Порт прежнего {@code MoldCastingRecipes.getCost(mold)} — статическая таблица,
         * не имеет data-driven источника правды (зависит только от типа формы, не от материала).
         */
        public int getCostMb() {
            return switch (this) {
                case NUGGET                        -> com.hbm_m.inventory.material.MaterialStack.MB_PER_NUGGET;
                case PLATE, INGOT, WIRE, WIRE_DENSE,
                     SHELL, PIPE, BILLET            -> com.hbm_m.inventory.material.MaterialStack.MB_PER_INGOT;
                case PLATE_CAST                    -> com.hbm_m.inventory.material.MaterialStack.MB_PER_PLATE;
                case PLATES_CAST                   -> com.hbm_m.inventory.material.MaterialStack.MB_PER_PLATE * 3;
                case INGOTS, PLATES, WIRES_DENSE,
                     BLOCK                          -> com.hbm_m.inventory.material.MaterialStack.MB_PER_INGOT * 9;
                default                            -> 0;
            };
        }
    }

    private final MoldType moldType;

    public ItemCastMold(MoldType moldType, Properties props) {
        super(props.stacksTo(1));
        this.moldType = moldType;
    }

    public MoldType getMoldType() { return moldType; }

    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal(ChatFormatting.GRAY + moldType.label));
        list.add(Component.literal(ChatFormatting.DARK_GRAY + "Place in Foundry Basin"));
    }
}
