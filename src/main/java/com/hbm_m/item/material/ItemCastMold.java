package com.hbm_m.item.material;

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
        HULL_BIG      ("Cast Big Hull Mold", 0),
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
        MOGUS         ("Cast Mogus Mold", 0);

        public final String label;
        /** 0 = малая форма (foundry_mold), 1 = большая (foundry_basin) — ItemMold.Mold.size оригинала. */
        public final int size;
        MoldType(String label, int size) {
            this.label = label;
            this.size = size;
        }

        /** 0 = малая форма (foundry_mold), 1 = большая (foundry_basin). */
        public int getSize() { return this.size; }

        /**
         * Стоимость заливки в mB (ёмкость формы) — 1:1 с {@code ItemMold.Mold.getCost()} оригинала.
         * Оригинальные кванты (MaterialShapes): NUGGET=8, WIRE=9, BILLET=48, INGOT/GEM/DENSEWIRE/
         * PLATE=72, CASTPLATE=216, SHELL=288, PIPE=216, BLOCK=648, LIGHTBARREL=216, HEAVYBARREL=432,
         * LIGHTRECEIVER=288, HEAVYRECEIVER=648, MECHANISM/STOCK=288, GRIP=144; квант = 1000/72 mB.
         * 0 = формы нет в оригинале (заливка невозможна).
         */
        public int getCostMb() {
            return switch (this) {
                case NUGGET                        -> 111;   // NUGGET.q(1) = 8q
                case BILLET                        -> 667;   // BILLET.q(1) = 48q
                case INGOT, PLATE, WIRE, WIRE_DENSE, GEM -> 1000; // 72q (WIRE берётся ×8 = 72q)
                case BLADE                         -> 3000;  // INGOT.q(3) = 216q
                case BLADES, STAMP, SHELL          -> 4000;  // 288q (INGOT.q(4) / SHELL.q(1))
                case PLATE_CAST                    -> 3000;  // CASTPLATE.q(1) = 216q
                case WIRES_DENSE                   -> 9000;  // DENSEWIRE.q(9) = 648q
                case PIPE                          -> 3000;  // PIPE.q(1) = 216q
                case INGOTS, PLATES, PLATES_CAST, BLOCK, PIPES -> 9000; // 648q
                case BARREL_LIGHT                  -> 3000;  // LIGHTBARREL = 216q
                case BARREL_HEAVY                  -> 6000;  // HEAVYBARREL = 432q
                case RECEIVER_LIGHT                -> 4000;  // LIGHTRECEIVER = 288q
                case RECEIVER_HEAVY                -> 9000;  // HEAVYRECEIVER = 648q
                case MECHANISM, STOCK              -> 4000;  // 288q
                case GRIP                          -> 2000;  // GRIP = 144q
                default                            -> 0;     // нет в ItemMold оригинала
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
        // 1:1 с ItemMold.addInformation оригинала: жёлтое название формы, затем
        // золотая строка "Foundry Mold" (малая) или красная "Foundry Basin" (большая).
        list.add(Component.literal(ChatFormatting.YELLOW + moldType.label));
        list.add(Component.translatable(moldType.getSize() == 1 ? "foundry.hbm_m.mold_large" : "foundry.hbm_m.mold_small")
                .withStyle(moldType.getSize() == 1 ? ChatFormatting.RED : ChatFormatting.GOLD));
    }
}
