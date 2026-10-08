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
        MOGUS         ("Cast Mogus Mold"),
        C9            ("Small Caliber Casing Mold"),
        C50           ("Large Caliber Casing Mold");

        public final String label;
        MoldType(String label) { this.label = label; }

    }

    private final MoldType moldType;

    public ItemCastMold(MoldType moldType, Properties props) {
        super(props.stacksTo(1));
        this.moldType = moldType;
    }

    public MoldType getMoldType() { return moldType; }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        // Original ItemMold.addInformation: Formtitel, darunter das passende Becken
        ItemMold.Mold mold = ItemMold.getMold(stack);
        if (mold == null) {
            list.add(Component.literal(moldType.label).withStyle(ChatFormatting.GRAY));
            return;
        }
        list.add(mold.getTitle().copy().withStyle(ChatFormatting.YELLOW));
        list.add(ItemMold.sizeName(mold).copy().withStyle(mold.size == 0 ? ChatFormatting.GOLD : ChatFormatting.RED));
    }
}
