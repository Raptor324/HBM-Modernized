package com.hbm_m.handler.ability;

import com.hbm_m.platform.ItemHooks;

import java.util.List;
import java.util.Optional;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tool.ItemToolAbility;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;
import com.hbm_m.recipe.CentrifugeRecipe;
import com.hbm_m.recipe.CrystallizerRecipe;
import com.hbm_m.recipe.ShredderRecipe;
import com.hbm_m.util.EnchantmentUtil;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code com.hbm.handler.ability.IToolHarvestAbility}. */
public interface IToolHarvestAbility extends IBaseAbility {

    default void preHarvestAll(int level, Level world, Player player) { }
    default void postHarvestAll(int level, Level world, Player player) { }

    // You must call harvestBlock to actually break the block.
    // If you don't, visual glitches ensue
    default void onHarvestBlock(int level, Level world, BlockPos pos, Player player, BlockState state) {
        harvestBlock(false, world, pos, player);
    }

    static void harvestBlock(boolean skipDefaultDrops, Level world, BlockPos pos, Player player) {
        if (skipDefaultDrops) {
            // Emulate the block breaking without drops
            world.removeBlock(pos, false);
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty()) ItemHooks.hurtAndBreak(stack, 1, player, net.minecraft.world.InteractionHand.MAIN_HAND);
        } else if (player instanceof ServerPlayer sp) {
            // Break the block conventionally
            ItemToolAbility.standardDigPost(world, pos, sp);
        }
    }

    /** Legt einen Gegenstand am Abbauort des Referenzblocks ab (Original: dropX/Y/Z + 0.5). */
    static void spawnAtDrop(Level world, ItemStack stack) {
        BlockPos d = ItemToolAbility.dropPos;
        world.addFreshEntity(new ItemEntity(world, d.getX() + 0.5, d.getY() + 0.5, d.getZ() + 0.5, stack));
    }

    /** Original {@code new ItemStack(block, 1, meta)}: der Block als Gegenstand, leuchtendes Redstone-Erz als normales. */
    static ItemStack asStack(BlockState state) {
        return new ItemStack(state.getBlock());
    }

    int SORT_ORDER_BASE = 100;

    // region handlers
    IToolHarvestAbility NONE = new IToolHarvestAbility() {
        @Override public String getName() { return ""; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 0; }
    };

    IToolHarvestAbility SILK = new IToolHarvestAbility() {
        @Override public String getName() { return "tool.ability.silktouch"; }
        @Override public boolean isAllowed() { return ModClothConfig.get().toolAbilitySilk; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 1; }

        @Override
        public void preHarvestAll(int level, Level world, Player player) {
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty()) EnchantmentUtil.addEnchantment(stack, Enchantments.SILK_TOUCH, 1);
        }

        @Override
        public void postHarvestAll(int level, Level world, Player player) {
            // ToC-ToU mismatch should be impossible
            // because both calls happen on the same tick.
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty()) EnchantmentUtil.removeEnchantment(stack, Enchantments.SILK_TOUCH);
        }
    };

    IToolHarvestAbility LUCK = new IToolHarvestAbility() {
        @Override public String getName() { return "tool.ability.luck"; }
        @Override public boolean isAllowed() { return ModClothConfig.get().toolAbilityLuck; }

        public final int[] powerAtLevel = { 1, 2, 3, 4, 5, 9 };

        @Override public int levels() { return powerAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + powerAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 2; }

        @Override
        public void preHarvestAll(int level, Level world, Player player) {
            ItemStack stack = player.getMainHandItem();
            //? if < 1.21.1 {
            if (!stack.isEmpty()) EnchantmentUtil.addEnchantment(stack, Enchantments.BLOCK_FORTUNE, powerAtLevel[level]);
            //?} else {
            /*if (!stack.isEmpty()) EnchantmentUtil.addEnchantment(stack, EnchantmentUtil.FORTUNE, powerAtLevel[level]);
            *///?}
        }

        @Override
        public void postHarvestAll(int level, Level world, Player player) {
            ItemStack stack = player.getMainHandItem();
            //? if < 1.21.1 {
            if (!stack.isEmpty()) EnchantmentUtil.removeEnchantment(stack, Enchantments.BLOCK_FORTUNE);
            //?} else {
            /*if (!stack.isEmpty()) EnchantmentUtil.removeEnchantment(stack, EnchantmentUtil.FORTUNE);
            *///?}
        }
    };

    IToolHarvestAbility SMELTER = new IToolHarvestAbility() {
        @Override public String getName() { return "tool.ability.smelter"; }
        @Override public boolean isAllowed() { return ModClothConfig.get().toolAbilityFurnace; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 3; }

        @Override
        public void onHarvestBlock(int level, Level world, BlockPos pos, Player player, BlockState state) {
            if (!(world instanceof ServerLevel server)) return;
            // Original: block.getDrops(world, x, y, z, meta, 0) - ohne Glueck, ohne Werkzeug
            List<ItemStack> drops = Block.getDrops(state, server, pos, world.getBlockEntity(pos));

            boolean doesSmelt = false;

            for (int i = 0; i < drops.size(); i++) {
                ItemStack stack = drops.get(i).copy();
                Optional<SmeltingRecipe> recipe = com.hbm_m.platform.recipe.RecipeHooks.getRecipeFor(world, RecipeType.SMELTING, stack);

                if (recipe.isPresent()) {
                    ItemStack result = recipe.get().getResultItem(world.registryAccess()).copy();
                    result.setCount(result.getCount() * stack.getCount());
                    drops.set(i, result);
                    doesSmelt = true;
                }
            }

            harvestBlock(doesSmelt, world, pos, player);

            if (doesSmelt) {
                for (ItemStack stack : drops) {
                    spawnAtDrop(world, stack.copy());
                }
            }
        }
    };

    IToolHarvestAbility SHREDDER = new IToolHarvestAbility() {
        @Override public String getName() { return "tool.ability.shredder"; }
        @Override public boolean isAllowed() { return ModClothConfig.get().toolAbilityShredder; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 4; }

        @Override
        public void onHarvestBlock(int level, Level world, BlockPos pos, Player player, BlockState state) {
            ItemStack stack = asStack(state);
            RecipeInputWrapper wrapper = new RecipeInputWrapper(new SimpleContainer(stack));
            // Original: getShredderResult liefert Schrott, wenn kein Rezept existiert - das zaehlt nicht
            ItemStack result = RecipeHooks.getAllRecipes(world, ShredderRecipe.Type.INSTANCE).stream()
                    .filter(r -> r.matchesRecipe(wrapper, world)).findFirst()
                    .map(r -> r.getOutput().copy()).orElse(ItemStack.EMPTY);

            boolean doesShred = !stack.isEmpty() && !result.isEmpty();

            harvestBlock(doesShred, world, pos, player);

            if (doesShred) {
                spawnAtDrop(world, result.copy());
            }
        }
    };

    IToolHarvestAbility CENTRIFUGE = new IToolHarvestAbility() {
        @Override public String getName() { return "tool.ability.centrifuge"; }
        @Override public boolean isAllowed() { return ModClothConfig.get().toolAbilityCentrifuge; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 5; }

        @Override
        public void onHarvestBlock(int level, Level world, BlockPos pos, Player player, BlockState state) {
            ItemStack stack = asStack(state);
            RecipeInputWrapper wrapper = new RecipeInputWrapper(new SimpleContainer(stack));
            ItemStack[] result = stack.isEmpty() ? null : RecipeHooks.getAllRecipes(world, CentrifugeRecipe.Type.INSTANCE).stream()
                    .filter(r -> r.matchesRecipe(wrapper, world)).findFirst()
                    .map(CentrifugeRecipe::getOutputs).orElse(null);

            boolean doesCentrifuge = result != null;

            harvestBlock(doesCentrifuge, world, pos, player);

            if (doesCentrifuge) {
                for (ItemStack st : result) {
                    if (st != null && !st.isEmpty()) {
                        spawnAtDrop(world, st.copy());
                    }
                }
            }
        }
    };

    IToolHarvestAbility CRYSTALLIZER = new IToolHarvestAbility() {
        @Override public String getName() { return "tool.ability.crystallizer"; }
        @Override public boolean isAllowed() { return ModClothConfig.get().toolAbilityCrystallizer; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 6; }

        @Override
        public void onHarvestBlock(int level, Level world, BlockPos pos, Player player, BlockState state) {
            ItemStack stack = asStack(state);
            FluidStack peroxide = FluidStack.create(ModFluids.PEROXIDE.getSource(), 1000L);
            CrystallizerRecipe result = stack.isEmpty() ? null : RecipeHooks.getAllRecipes(world, CrystallizerRecipe.Type.INSTANCE).stream()
                    .filter(r -> r.matchesInput(stack) && r.matchesAcid(peroxide)).findFirst()
                    .orElse(null);

            boolean doesCrystallize = result != null;

            harvestBlock(doesCrystallize, world, pos, player);

            if (doesCrystallize) {
                spawnAtDrop(world, result.getOutput());
            }
        }
    };

    IToolHarvestAbility MERCURY = new IToolHarvestAbility() {
        @Override public String getName() { return "tool.ability.mercury"; }
        @Override public boolean isAllowed() { return ModClothConfig.get().toolAbilityMercury; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 7; }

        @Override
        public void onHarvestBlock(int level, Level world, BlockPos pos, Player player, BlockState state) {
            Block block = state.getBlock();
            int mercury = 0;

            if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE)
                mercury = player.getRandom().nextInt(5) + 4;
            if (block == Blocks.REDSTONE_BLOCK)
                mercury = player.getRandom().nextInt(7) + 8;

            boolean doesConvert = mercury > 0;

            harvestBlock(doesConvert, world, pos, player);

            if (doesConvert) {
                // Original ModItems.ingot_mercury (registriert als "nugget_mercury")
                spawnAtDrop(world, new ItemStack(ModItems.NUGGET_MERCURY.get(), mercury));
            }
        }
    };
    // endregion handlers

    IToolHarvestAbility[] abilities = { NONE, SILK, LUCK, SMELTER, SHREDDER, CENTRIFUGE, CRYSTALLIZER, MERCURY };

    static IToolHarvestAbility getByName(String name) {
        for (IToolHarvestAbility ability : abilities) {
            if (ability.getName().equals(name))
                return ability;
        }

        return NONE;
    }
}
