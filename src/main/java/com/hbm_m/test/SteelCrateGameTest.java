package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.crates.BaseCrateBlock;
import com.hbm_m.blockentity.crates.SteelCrateBlockEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * GameTests for the steel crate (CRATE_STEEL).
 *
 * <p>Covers the key "portability" contract: the crate contents are saved
 * to the item on destruction ({@code BaseCrateBlockEntity#saveToItem}) and
 * restored on re-placement ({@code BaseCrateBlock#setPlacedBy}, reading
 * {@code BlockEntityTag} via {@code PlatformHooks}).
 *
 * <p>Locks/pins/spiders from the original 1.7.10 are absent in the modern
 * port — the corresponding checks were not carried over.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class SteelCrateGameTest {

    private SteelCrateGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty5x5x5", batch = "crate_steel", timeoutTicks = 100)
    public static void steelCrateContentsSurviveBreakAndPlacement(GameTestHelper helper) {
        Level level = helper.getLevel();
        BaseCrateBlock block = (BaseCrateBlock) ModBlocks.CRATE_STEEL.get();

        BlockPos sourcePos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(sourcePos, block.defaultBlockState(), Block.UPDATE_ALL);
        SteelCrateBlockEntity source =
                (SteelCrateBlockEntity) level.getBlockEntity(sourcePos);
        source.getItemHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 17));
        source.getItemHandler().setStackInSlot(53, new ItemStack(Items.GOLD_INGOT, 9));

        // Survival player break: the block itself packs its contents into the item.
        ItemStack portable = new ItemStack(block);
        source.saveToItem(portable);
        check(portable.getItem() == ModItems.CRATE_STEEL.get(),
                "On break the crate turns into a steel crate item");

        // Re-placement: setPlacedBy restores the contents from BlockEntityTag.
        BlockPos restoredPos = helper.absolutePos(new BlockPos(3, 1, 1));
        level.setBlock(restoredPos, block.defaultBlockState(), Block.UPDATE_ALL);
        block.setPlacedBy(level, restoredPos, level.getBlockState(restoredPos), null, portable);

        SteelCrateBlockEntity restored =
                (SteelCrateBlockEntity) level.getBlockEntity(restoredPos);
        check(restored.getItemHandler().getStackInSlot(0).is(Items.DIAMOND)
                        && restored.getItemHandler().getStackInSlot(0).getCount() == 17,
                "First slot must survive the break-and-place cycle");
        check(restored.getItemHandler().getStackInSlot(53).is(Items.GOLD_INGOT)
                        && restored.getItemHandler().getStackInSlot(53).getCount() == 9,
                "Last slot must survive the break-and-place cycle");

        // State cleanup so nothing leaks into neighboring tests.
        level.removeBlockEntity(sourcePos);
        level.setBlock(sourcePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.removeBlockEntity(restoredPos);
        level.setBlock(restoredPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.succeed();
    }
}
