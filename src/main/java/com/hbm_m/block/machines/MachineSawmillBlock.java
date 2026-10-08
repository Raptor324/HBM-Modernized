package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineSawmillBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineSawmill}: {@code getDimensions {1,0,1,1,1,1}}, {@code getOffset 1}, die vier Seiten des Sockels sind
 * Inventarzugaenge. Rechtsklick setzt ein fehlendes Blatt ein, nimmt Ausgaben heraus oder legt ein Stueck ein. Ohne
 * Blatt abgebaut droppt es als Variante "ohne Blatt" (NBT {@code noBlade}, Original Metadatum 1).
 */
public class MachineSawmillBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay {

    public static final String NO_BLADE = "noBlade";

    public MachineSawmillBlock(Properties properties) {
        super(properties);
    }

    /**
     * audit13/w16d: Original {@code bounding} - Kollision und Trefferbox jeder Zelle (keine Vollbloecke). Drehung 1:1
     * {@code getAABBRotationOffset} mit {@code rot = dir.getRotation(UP)} (Forge: NORTH->EAST, SOUTH->WEST, WEST->NORTH,
     * EAST->SOUTH) ueber {@link MultiblockStructureHelper#boundingMasters}; Kern- und Zellform macht DummyableMachineBlock.
     */
    private static final java.util.Map<net.minecraft.core.Direction, net.minecraft.world.phys.shapes.VoxelShape> BOUNDING =
            MultiblockStructureHelper.boundingMasters(new double[][] {
                    { -1.5D, 0D, -1.5D, 1.5D, 1D, 1.5D },
                    { -1.25D, 1D, -0.5D, -0.625D, 1.875D, 0.5D },
                    { -0.625D, 1D, -1D, 1.375D, 2D, 1D } });

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getCustomMasterVoxelShape(BlockState state) {
        return BOUNDING.get(state.getValue(FACING));
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(1, 0, 1, 1, 1, 1)
                .extra(1, 0, 0)
                .extra(-1, 0, 0)
                .extra(0, 0, 1)
                .extra(0, 0, -1)
                .placementOffset(1)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineSawmillBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SAWMILL_BE.get(),
                (lvl, pos, st, be) -> MachineSawmillBlockEntity.tick(lvl, pos, st, (MachineSawmillBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos, player, player.getMainHandItem());
    }
    *///?}

    private InteractionResult activate(Level world, BlockPos pos, Player player, ItemStack held) {

        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;

        } else if (!player.isShiftKeyDown()) {

            if (!(world.getBlockEntity(pos) instanceof MachineSawmillBlockEntity sawmill))
                return InteractionResult.PASS;

            if (!sawmill.hasBlade && !held.isEmpty() && held.getItem() == ModItems.SAWBLADE.get()) {
                held.shrink(1);
                sawmill.hasBlade = true;
                sawmill.setChanged();
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        com.hbm_m.sound.HbmSoundsNT.get("hbm:item.upgradePlug"), SoundSource.BLOCKS, 1.5F, 0.75F);
                return InteractionResult.CONSUME;
            }

            var inv = sawmill.getInventory();

            if (!inv.getStackInSlot(1).isEmpty() || !inv.getStackInSlot(2).isEmpty()) {
                for (int i = 1; i < 3; i++) {
                    ItemStack s = inv.getStackInSlot(i);
                    if (!s.isEmpty()) {
                        if (!player.getInventory().add(s.copy())) {
                            player.drop(s.copy(), false);
                        }
                        inv.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
                player.inventoryMenu.broadcastChanges();
                sawmill.setChanged();
                return InteractionResult.CONSUME;

            } else {
                if (inv.getStackInSlot(0).isEmpty() && !held.isEmpty() && sawmill.getOutput(held) != null) {
                    inv.setStackInSlot(0, held.copyWithCount(1));
                    held.shrink(1);
                    sawmill.setChanged();
                    player.inventoryMenu.broadcastChanges();
                    return InteractionResult.CONSUME;
                }
            }
        }

        return InteractionResult.PASS;
    }

    /** Original {@code onBlockPlacedBy}: die Variante ohne Blatt setzt {@code hasBlade = false}. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        CompoundTagView.read(stack, tag -> {
            if (tag.getBoolean(NO_BLADE) && level.getBlockEntity(pos) instanceof MachineSawmillBlockEntity sawmill) {
                sawmill.hasBlade = false;
                sawmill.setChanged();
            }
        });
    }

    /** Original {@code getDrops}: fehlt das Blatt, droppt die Variante ohne Blatt. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof MachineSawmillBlockEntity sawmill && !sawmill.hasBlade) {
            for (ItemStack drop : drops) {
                if (drop.getItem() == this.asItem()) com.hbm_m.platform.PlatformHooks.editItemTag(drop, t -> t.putBoolean(NO_BLADE, true));
            }
        }
        return drops;
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineSawmillBlockEntity sawmill)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(sawmill.heat + "TU/t"));

        double percent = (double) sawmill.heat / (double) 300;
        int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);

        if (percent > 1D)
            color = 0xff0000;

        final int c = color;
        text.add(Component.literal(((sawmill.heat * 1000 / 300) / 10D) + "%").withStyle(s -> s.withColor(c)));

        int limiter = sawmill.progress * 26 / MachineSawmillBlockEntity.processingTime;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 25; i++) bar.append("▏");
        Component barC = Component.literal("[ ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(bar.substring(0, Math.min(limiter, 25))).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(bar.substring(Math.min(limiter, 25))).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" ]").withStyle(ChatFormatting.GREEN));
        text.add(barC);

        for (int i = 0; i < 3; i++) {
            ItemStack s = sawmill.getInventory().getStackInSlot(i);
            if (!s.isEmpty()) {
                text.add((i == 0 ? Component.literal("-> ").withStyle(ChatFormatting.GREEN) : Component.literal("<- ").withStyle(ChatFormatting.RED))
                        .append(Component.empty().append(s.getHoverName()).withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */)
                        .append(Component.literal(s.getCount() > 1 ? " x" + s.getCount() : "")));
            }
        }

        if (sawmill.heat > 300) {
            int blink = System.currentTimeMillis() % 1000 < 500 ? 0xff0000 : 0xffff00;
            text.add(Component.literal("! ! ! OVERSPEED ! ! !").withStyle(s -> s.withColor(blink)));
        }

        if (!sawmill.hasBlade) {
            text.add(Component.literal("Blade missing!").withStyle(s -> s.withColor(0xff0000)));
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    /** Kleiner Helfer: liest das Item-NBT, falls vorhanden. */
    private static final class CompoundTagView {
        static void read(ItemStack stack, java.util.function.Consumer<net.minecraft.nbt.CompoundTag> consumer) {
            net.minecraft.nbt.CompoundTag tag = com.hbm_m.platform.PlatformHooks.getItemTag(stack);
            if (tag != null) consumer.accept(tag);
        }
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineSawmillBlock> CODEC = simpleCodec(MachineSawmillBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
