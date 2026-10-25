package com.hbm_m.item.material;

import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.item.ITooltipProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemCastMold extends Item implements ITooltipProvider {

    public enum MoldType {
        // Размер 1:1 с ItemMold оригинала (S = 0 — блок foundry_mold, L = 1 — блок foundry_basin):
        // большими (L) в оригинале были только ingots, plates, plates_cast, wires_dense и block.
        PLATE         ("Cast Plate Mold", 0),
        PLATE_CAST    ("Cast Plate Mold (Cast)", 0),
        PLATES        ("Cast Plates Mold", 1),
        PLATES_CAST   ("Cast Plates Mold (Cast)", 1),
        INGOT         ("Cast Ingot Mold", 0),
        INGOTS        ("Cast Ingots Mold", 1),
        NUGGET        ("Cast Nugget Mold", 0),
        WIRE          ("Cast Wire Mold", 0),
        WIRE_DENSE    ("Cast Dense Wire Mold", 0),
        WIRES_DENSE   ("Cast Dense Wires Mold", 1),
        PIPE          ("Cast Pipe Mold", 0),
        PIPES         ("Cast Pipes Mold", 0),
        BLOCK         ("Cast Block Mold", 1),
        BILLET        ("Cast Billet Mold", 0),
        BLADE         ("Cast Blade Mold", 0),
        BLADES        ("Cast Blades Mold", 0),
        GEM           ("Cast Gem Mold", 0),
        HULL_SMALL    ("Cast Small Hull Mold", 0),
        HULL_BIG      ("Cast Big Hull Mold", 1),
        SHELL         ("Cast Shell Mold", 0),
        MECHANISM     ("Cast Mechanism Mold", 0),
        GRIP          ("Cast Grip Mold", 0),
        STOCK         ("Cast Stock Mold", 0),
        BARREL_LIGHT  ("Cast Light Barrel Mold", 0),
        BARREL_HEAVY  ("Cast Heavy Barrel Mold", 0),
        RECEIVER_LIGHT("Cast Light Receiver Mold", 0),
        RECEIVER_HEAVY("Cast Heavy Receiver Mold", 0),
        BASE          ("Cast Base Mold", 0),
        STEEL_BASE    ("Cast Steel Base Mold", 0),
        STAMP         ("Cast Stamp Mold", 0),
        C357          ("Cast .357 Casing Mold", 0),
        CBUCKSHOT     ("Cast Buckshot Mold", 0),
        MOGUS         ("Cast Mogus Mold", 0),
        C9            ("Small Caliber Casing Mold", 0),
        C50           ("Large Caliber Casing Mold", 0);

        public final String label;
        /** 0 = малая форма (foundry_mold), 1 = большая (foundry_basin) — ItemMold.Mold.size оригинала. */
        public final int size;

        MoldType(String label, int size) {
            this.label = label;
            this.size = size;
        }

        MoldType(String label) {
            this(label, 0);
        }

        /** 0 = малая форма (foundry_mold), 1 = большая (foundry_basin). */
        public int getSize() { return this.size; }

        /**
         * Стоимость заливки в квантах (ёмкость формы) — 1:1 с {@code ItemMold.Mold.getCost()}
         * оригинала.
         * 0 = формы нет в оригинале (заливка невозможна).
         */
        public int getCost() {
            return switch (this) {
                case NUGGET                        -> MaterialShapes.NUGGET.q(1);   // 8
                case BILLET                        -> MaterialShapes.BILLET.q(1);   // 48
                case INGOT, PLATE, WIRE, WIRE_DENSE, GEM -> MaterialShapes.INGOT.q(1); // 72
                case BLADE                         -> MaterialShapes.INGOT.q(1) * 3;   // 216
                case BLADES, STAMP                 -> MaterialShapes.INGOT.q(1) * 4;   // 288
                case SHELL                         -> MaterialShapes.SHELL.q(1);    // 288
                case PLATE_CAST                    -> MaterialShapes.CASTPLATE.q(1); // 216
                case WIRES_DENSE                   -> MaterialShapes.BLOCK.q(1);    // 648
                case PIPE                          -> MaterialShapes.PIPE.q(1);     // 216
                case INGOTS, PLATES, PLATES_CAST, BLOCK, PIPES -> MaterialShapes.BLOCK.q(1); // 648
                case BARREL_LIGHT                  -> MaterialShapes.LIGHTBARREL.q(1);   // 216
                case BARREL_HEAVY                  -> MaterialShapes.HEAVYBARREL.q(1);   // 432
                case RECEIVER_LIGHT                -> MaterialShapes.LIGHTRECEIVER.q(1); // 288
                case RECEIVER_HEAVY                -> MaterialShapes.HEAVYRECEIVER.q(1); // 648
                case MECHANISM, STOCK              -> MaterialShapes.MECHANISM.q(1);     // 288
                case GRIP                          -> MaterialShapes.GRIP.q(1);     // 144
                case C357                          -> MaterialShapes.INGOT.q(1) / 4;    // 18
                case CBUCKSHOT                     -> MaterialShapes.INGOT.q(1) / 2;    // 36
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

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        ItemMold.Mold mold = ItemMold.getMold(stack);
        if (mold == null) {
            list.add(Component.literal(moldType.label).withStyle(ChatFormatting.YELLOW));
            list.add(Component.translatable(moldType.getSize() == 1 ? "foundry.hbm_m.mold_large" : "foundry.hbm_m.mold_small")
                    .withStyle(moldType.getSize() == 1 ? ChatFormatting.RED : ChatFormatting.GOLD));
            return;
        }
        list.add(mold.getTitle().copy().withStyle(ChatFormatting.YELLOW));
        list.add(ItemMold.sizeName(mold).copy().withStyle(mold.size == 0 ? ChatFormatting.GOLD : ChatFormatting.RED));
    }
}
