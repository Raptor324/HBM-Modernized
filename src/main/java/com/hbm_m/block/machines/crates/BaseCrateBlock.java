package com.hbm_m.block.machines.crates;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.crates.BaseCrateBlockEntity;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.platform.PlatformHooks;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Базовый блок для всех ящиков HBM.
 * Сохраняет содержимое при разрушении (как Shulker Box).
 */
public abstract class BaseCrateBlock extends BaseEntityBlock {

    protected BaseCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return openCrateMenu(state, level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openCrateMenu(state, level, pos, player);
    }
    *///?}

    private InteractionResult openCrateMenu(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BaseCrateBlockEntity crateEntity) {
            // Генерируем лут из таблицы структуры (если назначена) при первом открытии.
            crateEntity.unpackLootTable(player);
            playOpenSound(level, pos);
            MenuRegistry.openExtendedMenu((ServerPlayer) player, crateEntity, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        //? if < 1.21.1 {
        if (PlatformHooks.hasItemTag(stack)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BaseCrateBlockEntity crateEntity) {
                CompoundTag tag = PlatformHooks.getItemTag(stack);
                if (tag != null && tag.contains("BlockEntityTag")) {
                    PlatformHooks.loadBlockEntityTag(crateEntity, tag.getCompound("BlockEntityTag"), level.registryAccess());
                }
            }
        }
        //?} else {
        /*// 1.21.1: {@link #saveToItem} кладёт содержимое в DataComponents.BLOCK_ENTITY_DATA
        // (без обёртки BlockEntityTag), поэтому читаем компонент напрямую.
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BaseCrateBlockEntity crateEntity) {
            net.minecraft.world.item.component.CustomData data =
                    stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
            if (data != null) {
                CompoundTag tag = data.copyTag();
                if (!tag.isEmpty()) {
                    crateEntity.loadWithComponents(tag, level.registryAccess());
                }
            }
        }
        *///?}
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if < 1.21.1 {
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        handlePlayerWillDestroy(level, pos, state, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        handlePlayerWillDestroy(level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

    private void handlePlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.getAbilities().instabuild) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BaseCrateBlockEntity crateEntity) {
            if (!level.isClientSide) {
                ItemStack stack = new ItemStack(this);
                if (!crateEntity.isEmpty()) {
                    crateEntity.saveToItem(stack);
                }
                popResource(level, pos, stack);
            }
        }
    }

    private void playOpenSound(Level level, BlockPos pos) {
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                ModSounds.CRATE_OPEN.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.0F);
    }
}