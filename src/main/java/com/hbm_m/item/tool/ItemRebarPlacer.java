package com.hbm_m.item.tool;

import com.hbm_m.platform.StackNbt;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.RebarBlockEntity;
import com.hbm_m.inventory.HeldItemInventory;
import com.hbm_m.inventory.menu.RebarMenu;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code com.hbm.items.tool.ItemRebarPlacer} ({@code rebar_placer}): erster Klick auf einen Block merkt die
 * Ecke ("pos", der Block vor der angeklickten Seite), der zweite fuellt den Quader mit Bewehrung aus dem Inventar.
 * Die Betonsorte, zu der die Bewehrung spaeter aushaertet, waehlt man im Schleich-Rechtsklick-Menue (Musterplatz).
 */
public class ItemRebarPlacer extends Item {

    private static List<Block> acceptableConk;

    public ItemRebarPlacer(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Original: concrete, concrete_rebar, concrete_smooth, concrete_pillar, concrete_colored 0-15, concrete_colored_ext. */
    public static List<Block> getAcceptableConk() {
        if (acceptableConk == null) {
            List<Block> list = new ArrayList<>();
            list.add(ModBlocks.CONCRETE_TILE.get());
            list.add(ModBlocks.CONCRETE_REBAR.get());
            list.add(ModBlocks.CONCRETE.get());
            list.add(ModBlocks.CONCRETE_PILLAR.get());

            // BlockColored: Meta 0 = weiss ... 15 = schwarz
            list.add(ModBlocks.CONCRETE_WHITE.get());
            list.add(ModBlocks.CONCRETE_ORANGE.get());
            list.add(ModBlocks.CONCRETE_MAGENTA.get());
            list.add(ModBlocks.CONCRETE_LIGHT_BLUE.get());
            list.add(ModBlocks.CONCRETE_YELLOW.get());
            list.add(ModBlocks.CONCRETE_LIME.get());
            list.add(ModBlocks.CONCRETE_PINK.get());
            list.add(ModBlocks.CONCRETE_GRAY.get());
            list.add(ModBlocks.CONCRETE_SILVER.get());
            list.add(ModBlocks.CONCRETE_CYAN.get());
            list.add(ModBlocks.CONCRETE_PURPLE.get());
            list.add(ModBlocks.CONCRETE_BLUE.get());
            list.add(ModBlocks.CONCRETE_BROWN.get());
            list.add(ModBlocks.CONCRETE_GREEN.get());
            list.add(ModBlocks.CONCRETE_RED.get());
            list.add(ModBlocks.CONCRETE_BLACK.get());

            // EnumConcreteType: MACHINE, MACHINE_STRIPE, INDIGO, PURPLE, PINK, HAZARD, SAND, BRONZE
            list.add(ModBlocks.CONCRETE_COLORED_EXT_MACHINE.get());
            list.add(ModBlocks.CONCRETE_COLORED_EXT_MACHINE_STRIPE.get());
            list.add(ModBlocks.CONCRETE_COLORED_EXT_INDIGO.get());
            list.add(ModBlocks.CONCRETE_COLORED_EXT_PURPLE.get());
            list.add(ModBlocks.CONCRETE_COLORED_EXT_PINK.get());
            list.add(ModBlocks.CONCRETE_COLORED_EXT_HAZARD.get());
            list.add(ModBlocks.CONCRETE_COLORED_EXT_SAND.get());
            list.add(ModBlocks.CONCRETE_COLORED_EXT_BRONZE.get());
            acceptableConk = list;
        }
        return acceptableConk;
    }

    /** Die Metadaten des Originals sind im Port eigene Bloecke, {@code meta} ist immer 0. */
    public static boolean isValidConk(Item item, int meta) {

        for (Block conk : getAcceptableConk()) {
            if (item == conk.asItem() && meta == 0) return true;
        }
        return false;
    }

    public static boolean isValidConk(ItemStack stack) {
        return stack != null && !stack.isEmpty() && isValidConk(stack.getItem(), 0);
    }

    @Override
    //? if < 1.21.1 {
    public int getUseDuration(ItemStack stack) {
    //?} else {
    /*public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity hbmUser) {
    *///?}
        return 1;
    }

    // if the placer isn't equipped or no concrete is loaded, forget the cached position
    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean held) {
        if (StackNbt.has(stack) && StackNbt.read(stack).contains("pos")) {
            ItemStack[] stacks = HeldItemInventory.readStacksFromNBT(stack, 1);
            ItemStack theConk = stacks == null ? null : stacks[0];

            if (!held || theConk == null) {
                StackNbt.tag(stack).remove("pos");
                return;
            }

            if (!isValidConk(theConk)) {
                StackNbt.tag(stack).remove("pos");
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide && player.isShiftKeyDown() && player instanceof ServerPlayer sp) {
            MenuRegistry.openExtendedMenu(sp, new MenuProvider() {
                @Override public Component getDisplayName() { return stack.getHoverName(); }
                @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new RebarMenu(id, inv, hand); }
            }, buf -> buf.writeEnum(hand));
        }
        return InteractionResultHolder.pass(stack);
    }

    private MutableComponent prefix() {
        return Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (player == null) return InteractionResult.PASS;

        if (!StackNbt.has(stack)) {
            StackNbt.set(stack, new CompoundTag());
            HeldItemInventory.addStacksToNBT(stack, new ItemStack(ModBlocks.CONCRETE_REBAR.get()));
        }
        ItemStack[] stacks = HeldItemInventory.readStacksFromNBT(stack, 1);
        ItemStack theConk = stacks == null ? null : stacks[0];

        boolean hasConk = theConk != null && isValidConk(theConk);

        if (!hasConk) {
            player.sendSystemMessage(prefix().append(Component.literal("No valid concrete type set!").withStyle(ChatFormatting.RED)));
            return InteractionResult.SUCCESS;
        }

        Direction dir = ctx.getClickedFace();
        BlockPos clicked = ctx.getClickedPos();

        if (!StackNbt.read(stack).contains("pos")) {
            StackNbt.tag(stack).putIntArray("pos", new int[] {clicked.getX() + dir.getStepX(), clicked.getY() + dir.getStepY(), clicked.getZ() + dir.getStepZ()});
        } else {
            Item rebarItem = ModBlocks.REBAR.get().asItem();
            int rebarLeft = 0;
            for (ItemStack s : player.getInventory().items) if (s.is(rebarItem)) rebarLeft += s.getCount();

            if (rebarLeft <= 0) {
                player.sendSystemMessage(prefix().append(Component.literal("Out of rebar!").withStyle(ChatFormatting.RED)));
                StackNbt.tag(stack).remove("pos");
                return InteractionResult.SUCCESS;
            }

            int[] pos = StackNbt.read(stack).getIntArray("pos");
            int iX = clicked.getX() + dir.getStepX();
            int iY = clicked.getY() + dir.getStepY();
            int iZ = clicked.getZ() + dir.getStepZ();

            int minX = Math.min(pos[0], iX);
            int maxX = Math.max(pos[0], iX);
            int minY = Math.min(pos[1], iY);
            int maxY = Math.max(pos[1], iY);
            int minZ = Math.min(pos[2], iZ);
            int maxZ = Math.max(pos[2], iZ);

            int rebarUsed = 0;
            Block conkBlock = Block.byItem(theConk.getItem());

            outer: for (int k = minY; k <= maxY; k++) {
                for (int j = minZ; j <= maxZ; j++) {
                    for (int i = minX; i <= maxX; i++) {
                        if (rebarLeft <= 0) break outer;
                        BlockPos p = new BlockPos(i, k, j);

                        if (world.getBlockState(p).canBeReplaced() && player.mayUseItemAt(p, dir, stack)) {
                            world.setBlock(p, ModBlocks.REBAR.get().defaultBlockState(), 3);
                            BlockEntity tile = world.getBlockEntity(p);
                            if (tile instanceof RebarBlockEntity rebar) {
                                rebar.setup(conkBlock, 0);
                            }
                            rebarUsed++;
                            rebarLeft--;
                        }
                    }
                }
            }

            int toConsume = rebarUsed;
            for (ItemStack s : player.getInventory().items) {
                if (toConsume <= 0) break;
                if (!s.is(rebarItem)) continue;
                int take = Math.min(toConsume, s.getCount());
                s.shrink(take);
                toConsume -= take;
            }

            player.sendSystemMessage(prefix().append(Component.literal("Placed " + rebarUsed + " rebar!").withStyle(ChatFormatting.GREEN)));

            StackNbt.tag(stack).remove("pos");
            player.inventoryMenu.broadcastChanges();
        }

        return InteractionResult.SUCCESS;
    }
}
