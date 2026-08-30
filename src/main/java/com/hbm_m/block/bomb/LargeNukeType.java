package com.hbm_m.block.bomb;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Типы больших ядерных бомб (Gadget / Little Boy / Ivy Mike / Tsar Bomba).
 * Раскладка слотов, состав заряда и радиус (из конфига) на каждый тип.
 */
public enum LargeNukeType {
    GADGET("gadget", "container.hbm_m.nuke_gadget", 6, 176, 166, 8, 84,
            "textures/gui/weapon/gadget_schematic.png"),
    BOY("boy", "container.hbm_m.nuke_boy", 5, 176, 222, 8, 140,
            "textures/gui/weapon/lil_boy_schematic.png"),
    MIKE("mike", "container.hbm_m.nuke_mike", 8, 176, 217, 8, 135,
            "textures/gui/weapon/ivy_mike_schematic.png"),
    TSAR("tsar", "container.hbm_m.nuke_tsar", 6, 256, 233, 48, 151,
            "textures/gui/weapon/tsar_bomba_schematic.png");

    private final String id;
    private final String containerKey;
    private final int slots;
    private final int guiWidth;
    private final int guiHeight;
    private final int inventoryX;
    private final int inventoryY;
    private final String schematicPath;

    LargeNukeType(String id, String containerKey, int slots, int guiWidth, int guiHeight,
                  int inventoryX, int inventoryY, String schematicPath) {
        this.id = id;
        this.containerKey = containerKey;
        this.slots = slots;
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
        this.inventoryX = inventoryX;
        this.inventoryY = inventoryY;
        this.schematicPath = schematicPath;
    }

    public String id() { return id; }
    public String containerKey() { return containerKey; }
    public int slots() { return slots; }
    public int guiWidth() { return guiWidth; }
    public int guiHeight() { return guiHeight; }
    public int inventoryX() { return inventoryX; }
    public int inventoryY() { return inventoryY; }

    public ResourceLocation schematic() {
        //? if < 1.21.1 {
        return new ResourceLocation(RefStrings.MODID, schematicPath);
        //?} else {
        /*return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, schematicPath);
         *///?}
    }

    public static LargeNukeType byId(String id) {
        for (LargeNukeType t : values()) {
            if (t.id.equals(id)) return t;
        }
        return GADGET;
    }

    public int slotX(int slot) {
        return switch (this) {
            case GADGET -> new int[]{26, 8, 44, 8, 44, 98}[slot];
            case BOY -> 26 + slot * 18;
            case MIKE -> new int[]{26, 26, 44, 44, 39, 98, 116, 134}[slot];
            case TSAR -> new int[]{48, 66, 84, 102, 55, 138}[slot];
        };
    }

    public int slotY(int slot) {
        return switch (this) {
            case GADGET -> new int[]{35, 17, 17, 53, 53, 35}[slot];
            case BOY -> 36;
            case MIKE -> new int[]{83, 101, 83, 101, 35, 91, 91, 91}[slot];
            case TSAR -> new int[]{101, 101, 101, 101, 51, 101}[slot];
        };
    }

    /** Заряд собран до минимально боеспособного состояния. */
    public boolean isReady(ItemStack[] items) {
        return switch (this) {
            case GADGET -> items[0].is(ModItems.GADGET_WIREING.get())
                    && lenses(items, ModItems.EARLY_EXPLOSIVE_LENSES.get())
                    && items[5].is(ModItems.GADGET_CORE.get());
            case BOY -> items[0].is(ModItems.BOY_SHIELDING.get())
                    && items[1].is(ModItems.BOY_TARGET.get())
                    && items[2].is(ModItems.BOY_BULLET.get())
                    && items[3].is(ModItems.BOY_PROPELLANT.get())
                    && items[4].is(ModItems.BOY_IGNITER.get());
            case MIKE, TSAR -> items[0].is(ModItems.EXPLOSIVE_LENSES.get())
                    && items[1].is(ModItems.EXPLOSIVE_LENSES.get())
                    && items[2].is(ModItems.EXPLOSIVE_LENSES.get())
                    && items[3].is(ModItems.EXPLOSIVE_LENSES.get())
                    && items[4].is(ModItems.MAN_CORE.get());
        };
    }

    /** Полная комплектация (для Mike/Tsar даёт усиленный заряд). */
    public boolean isFilled(ItemStack[] items) {
        if (!isReady(items)) return false;
        return switch (this) {
            case MIKE -> items[5].is(ModItems.MIKE_CORE.get())
                    && items[6].is(ModItems.MIKE_DEUT.get())
                    && items[7].is(ModItems.MIKE_COOLING_UNIT.get());
            case TSAR -> items[5].is(ModItems.TSAR_CORE.get());
            default -> true;
        };
    }

    public int detonationRadius(ItemStack[] items) {
        ModClothConfig cfg = ModClothConfig.get();
        return switch (this) {
            case GADGET -> cfg.gadgetRadius;
            case BOY -> cfg.boyRadius;
            case MIKE -> cfg.mikeRadius;
            // Особенность оригинала: без tsar_core взрыв по радиусу Fat Man
            case TSAR -> isFilled(items) ? cfg.tsarRadius : cfg.manRadius;
        };
    }

    /** Разрешённые предметы для слота (используется canPlaceItem). */
    public boolean canPlace(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        return switch (this) {
            case GADGET -> switch (slot) {
                case 0 -> stack.is(ModItems.GADGET_WIREING.get());
                case 1, 2, 3, 4 -> stack.is(ModItems.EARLY_EXPLOSIVE_LENSES.get());
                case 5 -> stack.is(ModItems.GADGET_CORE.get());
                default -> false;
            };
            case BOY -> switch (slot) {
                case 0 -> stack.is(ModItems.BOY_SHIELDING.get());
                case 1 -> stack.is(ModItems.BOY_TARGET.get());
                case 2 -> stack.is(ModItems.BOY_BULLET.get());
                case 3 -> stack.is(ModItems.BOY_PROPELLANT.get());
                case 4 -> stack.is(ModItems.BOY_IGNITER.get());
                default -> false;
            };
            case MIKE -> switch (slot) {
                case 0, 1, 2, 3 -> stack.is(ModItems.EXPLOSIVE_LENSES.get());
                case 4 -> stack.is(ModItems.MAN_CORE.get());
                case 5 -> stack.is(ModItems.MIKE_CORE.get());
                case 6 -> stack.is(ModItems.MIKE_DEUT.get());
                case 7 -> stack.is(ModItems.MIKE_COOLING_UNIT.get());
                default -> false;
            };
            case TSAR -> switch (slot) {
                case 0, 1, 2, 3 -> stack.is(ModItems.EXPLOSIVE_LENSES.get());
                case 4 -> stack.is(ModItems.MAN_CORE.get());
                case 5 -> stack.is(ModItems.TSAR_CORE.get());
                default -> false;
            };
        };
    }

    private static boolean lenses(ItemStack[] items, net.minecraft.world.item.Item lens) {
        return items[1].is(lens) && items[2].is(lens) && items[3].is(lens) && items[4].is(lens);
    }
}
