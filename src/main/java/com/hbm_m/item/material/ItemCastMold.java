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
        SHELL         ("Cast Shell Mold", 0),
        MECHANISM     ("Cast Mechanism Mold", 0),
        GRIP          ("Cast Grip Mold", 0),
        STOCK         ("Cast Stock Mold", 0),
        BARREL_LIGHT  ("Cast Light Barrel Mold", 0),
        BARREL_HEAVY  ("Cast Heavy Barrel Mold", 0),
        RECEIVER_LIGHT("Cast Light Receiver Mold", 0),
        RECEIVER_HEAVY("Cast Heavy Receiver Mold", 0),
        STAMP         ("Cast Stamp Mold", 0),
        C357          ("Cast .357 Casing Mold", 0),
        CBUCKSHOT     ("Cast Buckshot Mold", 0);

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
         * Стоимость заливки в квантах (ёмкость формы) — 1:1 с {@code ItemMold.Mold.getCost()}
         * оригинала: NUGGET=8, BILLET=48, INGOT/GEM/DENSEWIRE/PLATE=72 (WIRE берётся ×8 = 72),
         * CASTPLATE=216, INGOT.q(3)=216 (blade), INGOT.q(4)=SHELL=288 (blades/stamp/shell),
         * PIPE=216, ×9 слитков/плит=BLOCK=648, LIGHTBARREL=216, HEAVYBARREL=432,
         * LIGHTRECEIVER=288, HEAVYRECEIVER=648, MECHANISM/STOCK=288, GRIP=144.
         * 0 = формы нет в оригинале (заливка невозможна).
         */
        public int getCost() {
            return switch (this) {
                case NUGGET                        -> MaterialShapes.NUGGET;   // 8
                case BILLET                        -> MaterialShapes.BILLET;   // 48
                case INGOT, PLATE, WIRE, WIRE_DENSE, GEM -> MaterialShapes.INGOT; // 72
                case BLADE                         -> MaterialShapes.INGOT * 3;   // 216
                case BLADES, STAMP                 -> MaterialShapes.INGOT * 4;   // 288
                case SHELL                         -> MaterialShapes.SHELL;    // 288
                case PLATE_CAST                    -> MaterialShapes.CASTPLATE; // 216
                case WIRES_DENSE                   -> MaterialShapes.BLOCK;    // 648
                case PIPE                          -> MaterialShapes.PIPE;     // 216
                case INGOTS, PLATES, PLATES_CAST, BLOCK, PIPES -> MaterialShapes.BLOCK; // 648
                case BARREL_LIGHT                  -> MaterialShapes.LIGHTBARREL;   // 216
                case BARREL_HEAVY                  -> MaterialShapes.HEAVYBARREL;   // 432
                case RECEIVER_LIGHT                -> MaterialShapes.LIGHTRECEIVER; // 288
                case RECEIVER_HEAVY                -> MaterialShapes.HEAVYRECEIVER; // 648
                case MECHANISM, STOCK              -> MaterialShapes.MECHANISM;     // 288
                case GRIP                          -> MaterialShapes.GRIP;     // 144
                // Казённые формы (ориг. id 16 c9 / id 17 c50, PLATE.q(1,4) / PLATE.q(1,2)):
                // порт переименовал их в C357/CBUCKSHOT — те же стоимости и выходы.
                case C357                          -> MaterialShapes.INGOT / 4;    // 18
                case CBUCKSHOT                     -> MaterialShapes.INGOT / 2;    // 36
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
