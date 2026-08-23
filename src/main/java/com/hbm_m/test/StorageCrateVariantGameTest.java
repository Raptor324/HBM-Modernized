package com.hbm_m.test;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.crates.BaseCrateBlock;
import com.hbm_m.blockentity.crates.BaseCrateBlockEntity;
import com.hbm_m.blockentity.crates.DeshCrateBlockEntity;
import com.hbm_m.blockentity.crates.IronCrateBlockEntity;
import com.hbm_m.inventory.menu.DeshCrateMenu;
import com.hbm_m.inventory.menu.IronCrateMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.platform.recipe.RecipeHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * GameTests for the storage crate variants (Iron/Desh/Steel).
 *
 * <p>Covers: source sizes (36/104 slots via {@code CrateType}), last-slot
 * survival across a break-and-place cycle, anti-duplication of the portable
 * crate on non-player destruction, menu layouts (original coordinates
 * 176x186 and 248x256), and the iron crate and steel-to-desh upgrade recipes.
 *
 * <p>Not ported due to missing behavior in this port: trap spiders,
 * locks/pins, capability pipelines, held-crate containers.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class StorageCrateVariantGameTest {

    private StorageCrateVariantGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    /** Version-compatible mock player. */
    private static Player makePlayer(GameTestHelper helper) {
        //? if < 1.21.1 {
        return helper.makeMockPlayer();
        //?} else {
        /*return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        *///?}
    }

    private static ItemStack craft(GameTestHelper helper, NonNullList<ItemStack> grid) {
        var crafting = new com.hbm_m.util.SimpleCraftingContainer(grid, 3, 3);
        var recipe = RecipeHooks.getCraftingRecipeFor(helper.getLevel(), crafting).orElseThrow();
        return RecipeHooks.assembleCrafting(recipe, crafting, helper.getLevel());
    }

    private static void cleanupCrate(Level level, BlockPos pos) {
        level.removeBlockEntity(pos);
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    @GameTest(template = "empty5x5x5", batch = "crate_variants", timeoutTicks = 100)
    public static void ironAndDeshCratesKeepTheirSourceSlotCountsAndLastSlots(GameTestHelper helper) {
        Level level = helper.getLevel();

        BlockPos ironPos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(ironPos, ModBlocks.CRATE_IRON.get().defaultBlockState(), Block.UPDATE_ALL);
        IronCrateBlockEntity iron = (IronCrateBlockEntity) level.getBlockEntity(ironPos);
        check(iron.getSlotCount() == 36,
                "Iron crate must keep the original 36 slots (9x4)");
        iron.getItemHandler().setStackInSlot(35, new ItemStack(Items.IRON_INGOT, 12));

        BlockPos deshPos = helper.absolutePos(new BlockPos(3, 1, 1));
        level.setBlock(deshPos, ModBlocks.CRATE_DESH.get().defaultBlockState(), Block.UPDATE_ALL);
        DeshCrateBlockEntity desh = (DeshCrateBlockEntity) level.getBlockEntity(deshPos);
        check(desh.getSlotCount() == 104,
                "Desh crate must keep the original 104 slots (13x8)");
        desh.getItemHandler().setStackInSlot(103, new ItemStack(Items.DIAMOND, 7));

        // Edge slots must survive packing into an item and re-placement.
        check(roundTripLastSlot(level, ModBlocks.CRATE_IRON.get(), ironPos, 35),
                "Iron crate edge slot must survive break and place");
        check(roundTripLastSlot(level, ModBlocks.CRATE_DESH.get(), deshPos, 103),
                "Desh crate edge slot must survive break and place");

        cleanupCrate(level, ironPos);
        cleanupCrate(level, deshPos);
        helper.succeed();
    }

    /**
     * Packs the crate contents into an item ({@code saveToItem}) and restores
     * them via {@code BaseCrateBlock#setPlacedBy} at a new location; returns
     * true if the stack in the edge slot survived.
     */
    private static boolean roundTripLastSlot(Level level, Block block, BlockPos sourcePos,
                                             int lastSlot) {
        BaseCrateBlockEntity source = (BaseCrateBlockEntity) level.getBlockEntity(sourcePos);
        ItemStack expected = source.getItemHandler().getStackInSlot(lastSlot).copy();

        ItemStack portable = new ItemStack(block.asItem());
        source.saveToItem(portable);

        BlockPos restoredPos = sourcePos.relative(net.minecraft.core.Direction.EAST);
        level.setBlock(restoredPos, block.defaultBlockState(), Block.UPDATE_ALL);
        ((BaseCrateBlock) block).setPlacedBy(level, restoredPos,
                level.getBlockState(restoredPos), null, portable);
        BaseCrateBlockEntity restored = (BaseCrateBlockEntity) level.getBlockEntity(restoredPos);
        ItemStack actual = restored.getItemHandler().getStackInSlot(lastSlot);

        boolean ok = expected.getItem() == actual.getItem()
                && expected.getCount() == actual.getCount();
        cleanupCrate(level, restoredPos);
        return ok;
    }

    @GameTest(template = "empty3x3x3", batch = "crate_non_player_break_isolated", timeoutTicks = 100)
    public static void nonPlayerDestructionDoesNotDropPortableCrate(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, ModBlocks.CRATE_IRON.get().defaultBlockState(), Block.UPDATE_ALL);
        BaseCrateBlockEntity crate = (BaseCrateBlockEntity) level.getBlockEntity(pos);
        crate.getItemHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));

        // Non-player destruction (explosion/machine): contents are lost,
        // but the portable crate must not be duplicated.
        level.destroyBlock(pos, true);

        List<net.minecraft.world.entity.item.ItemEntity> drops =
                level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new AABB(pos).inflate(2.0D));
        check(drops.stream().noneMatch(drop -> drop.getItem().is(ModItems.CRATE_IRON.get())),
                "Non-player destruction must not drop the portable crate (anti-duplication)");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "crate_menus", timeoutTicks = 100)
    public static void crateMenusMatchOriginalLayouts(GameTestHelper helper) {
        Player player = makePlayer(helper);

        var ironMenu = new IronCrateMenu(0, player.getInventory(),
                new IronCrateBlockEntity(BlockPos.ZERO, ModBlocks.CRATE_IRON.get().defaultBlockState()));
        check(ironMenu.slots.size() == 36 + 36
                        && ironMenu.slots.get(35).x == 152 && ironMenu.slots.get(35).y == 72
                        && ironMenu.slots.get(36).x == 8 && ironMenu.slots.get(36).y == 104
                        && ironMenu.slots.get(71).y == 162,
                "Iron crate menu slots must keep the exact original coordinates 176x186");

        var deshMenu = new DeshCrateMenu(1, player.getInventory(),
                new DeshCrateBlockEntity(BlockPos.ZERO, ModBlocks.CRATE_DESH.get().defaultBlockState()));
        check(deshMenu.slots.size() == 104 + 36
                        && deshMenu.slots.get(103).x == 224 && deshMenu.slots.get(103).y == 144
                        && deshMenu.slots.get(104).x == 44 && deshMenu.slots.get(104).y == 174
                        && deshMenu.slots.get(139).y == 232,
                "Desh crate menu slots must keep the exact original coordinates 248x256");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "crate_recipes", timeoutTicks = 100)
    public static void recipesCreateIronCrateAndUpgradeSteelToDesh(GameTestHelper helper) {
        // Iron crate: AAA / B B / BBB — iron plates on top, ingots below.
        NonNullList<ItemStack> ironGrid = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int col = 0; col < 3; col++) {
            ironGrid.set(col, new ItemStack(ModItems.PLATE_IRON.get()));
            ironGrid.set(6 + col, new ItemStack(Items.IRON_INGOT));
        }
        ironGrid.set(3, new ItemStack(Items.IRON_INGOT));
        ironGrid.set(5, new ItemStack(Items.IRON_INGOT));
        ItemStack ironResult = craft(helper, ironGrid);
        check(ironResult.is(ModItems.CRATE_IRON.get()),
                "Recipe of iron plates and ingots must produce an iron crate");

        // Upgrade: AAA / ABA / AAA — desh plates around a steel crate.
        NonNullList<ItemStack> upgradeGrid = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) {
            upgradeGrid.set(i, (i == 4)
                    ? new ItemStack(ModBlocks.CRATE_STEEL.get())
                    : new ItemStack(ModItems.PLATE_DESH.get()));
        }
        ItemStack upgraded = craft(helper, upgradeGrid);
        check(upgraded.is(ModItems.CRATE_DESH.get()) && upgraded.getCount() == 1,
                "Eight desh plates around a steel crate must produce a desh crate");
        helper.succeed();
    }
}
