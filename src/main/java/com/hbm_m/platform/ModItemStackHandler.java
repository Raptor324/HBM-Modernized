package com.hbm_m.platform;

//? if forge {
import net.minecraftforge.items.ItemStackHandler;
//?} elif neoforge {
/*import net.neoforged.neoforge.items.ItemStackHandler;
*///?}

public abstract class ModItemStackHandler extends ItemStackHandler {

    public ModItemStackHandler(int size) {
        super(size);
    }
}