package com.hbm_m.test;

import com.hbm_m.armormod.menu.ArmorTableMenu;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * Armor modification table driven through real container clicks
 * ({@link net.minecraft.world.inventory.AbstractContainerMenu#clicked}), the way a player does it.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class ArmorTableGameTest {

    private ArmorTableGameTest() {}

    private static final BlockPos TABLE = new BlockPos(1, 1, 1);

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static Player mockPlayer(GameTestHelper helper) {
        //? if < 1.21.1 {
        return helper.makeMockPlayer();
        //?} else {
        /*return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        *///?}
    }

    private static boolean hasMaxHealthModifier(ItemStack armor) {
        //? if < 1.21.1 {
        return armor.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.CHEST).containsKey(Attributes.MAX_HEALTH);
        //?} else {
        /*boolean[] found = {false};
        armor.forEachModifier(net.minecraft.world.entity.EquipmentSlot.CHEST, (attr, mod) -> {
            if (attr.equals(Attributes.MAX_HEALTH)) found[0] = true;
        });
        return found[0];
        *///?}
    }

    private static ArmorTableMenu openTable(GameTestHelper helper, Player player) {
        helper.setBlock(TABLE, ModBlocks.ARMOR_TABLE.get());
        ArmorTableMenu menu = new ArmorTableMenu(1, player.getInventory(), helper.absolutePos(TABLE));
        player.containerMenu = menu;
        return menu;
    }

    private static void pickup(ArmorTableMenu menu, Player player, int slot) {
        menu.clicked(slot, 0, ClickType.PICKUP, player);
    }

    /** Heart piece goes in, armour is taken back out with the cursor: the health attribute must be on it. */
    @GameTest(template = "empty3x3x3", batch = "armortable", timeoutTicks = 100)
    public static void heartPieceAppliesMaxHealthOnPickup(GameTestHelper helper) {
        Player player = mockPlayer(helper);
        ArmorTableMenu menu = openTable(helper, player);

        menu.setCarried(new ItemStack(Items.IRON_CHESTPLATE));
        pickup(menu, player, ArmorTableMenu.SLOT_ARMOR_IN);
        check(menu.getCarried().isEmpty(), "armour must land in the centre slot");

        menu.setCarried(new ItemStack(ModItems.HEART_PIECE.get()));
        pickup(menu, player, ArmorModificationHelper.extra);
        check(menu.getCarried().isEmpty(), "heart piece must be accepted by the special slot");
        ItemStack centre = menu.getSlot(ArmorTableMenu.SLOT_ARMOR_IN).getItem();
        check(!ArmorModificationHelper.pryMod(centre, ArmorModificationHelper.extra).isEmpty(),
                "heart piece must be written into the armour NBT, got " + com.hbm_m.platform.PlatformHooks.getItemTag(centre));

        pickup(menu, player, ArmorTableMenu.SLOT_ARMOR_IN);
        ItemStack armor = menu.getCarried();
        check(armor.is(Items.IRON_CHESTPLATE), "armour must be on the cursor after pickup");
        check(menu.getSlot(ArmorModificationHelper.extra).getItem().isEmpty(), "mod item is consumed into the armour");
        check(!ArmorModificationHelper.pryMod(armor, ArmorModificationHelper.extra).isEmpty(),
                "armour on the cursor must still carry the heart piece");

        check(hasMaxHealthModifier(armor), "armour taken with the cursor must carry the MAX_HEALTH modifier");
        helper.succeed();
    }

    /** Cladding stays on the armour after the table is closed, and its protection is counted. */
    @GameTest(template = "empty3x3x3", batch = "armortable", timeoutTicks = 100)
    public static void claddingSurvivesTableClose(GameTestHelper helper) {
        Player player = mockPlayer(helper);
        ArmorTableMenu menu = openTable(helper, player);

        menu.setCarried(new ItemStack(Items.IRON_HELMET));
        pickup(menu, player, ArmorTableMenu.SLOT_ARMOR_IN);
        menu.setCarried(new ItemStack(ModItems.LEAD_CLADDING.get()));
        pickup(menu, player, ArmorModificationHelper.cladding);
        check(menu.getCarried().isEmpty(), "cladding must be accepted by the cladding slot");

        menu.removed(player);
        ItemStack armor = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.IRON_HELMET)) armor = player.getInventory().getItem(i);
        }
        check(!armor.isEmpty(), "closing the table must hand the armour back to the player");
        check(!ArmorModificationHelper.pryMod(armor, ArmorModificationHelper.cladding).isEmpty(),
                "cladding must be stored on the returned armour");
        check(ArmorModificationHelper.getTotalAbsoluteRadProtection(armor) > 0.0F,
                "cladding must add rad protection");
        helper.succeed();
    }
}
