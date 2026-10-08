package com.hbm_m.block.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineTurbofan}: {@code getDimensions {2,0,1,1,3,3}}, {@code getOffset 1}; die vier Anschluesse liegen an
 * den Zellen beiderseits des Kerns (Extras vor/hinter dem Kern und deren linke Nachbarn). Gezeichnet vom
 * {@code TurbofanRenderer}.
 */
public class MachineTurbofanBlock extends DummyableMachineBlock {

    public MachineTurbofanBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original fillSpace nutzt die Setzposition (Kern + dir), nicht den Kern
        return DummyableStructureBuilder.create()
                .box(2, 0, 1, 1, 3, 3)
                .extra(1, 0, 0)
                .extra(1, 0, -1)
                .extra(-1, 0, 0)
                .extra(-1, 0, -1)
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
        return new MachineTurbofanBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.TURBOFAN_BE.get(),
                (lvl, pos, st, be) -> MachineTurbofanBlockEntity.tick(lvl, pos, st, (MachineTurbofanBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }
    *///?}

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof MachineTurbofanBlockEntity machine) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, machine, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Fuel efficiency:").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("-").withStyle(ChatFormatting.YELLOW)
                .append(Component.translatable("hbmfluid.trait.fuel.aviation"))
                .append(Component.literal(": ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("100%").withStyle(ChatFormatting.RED)));
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineTurbofanBlock> CODEC = simpleCodec(MachineTurbofanBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
