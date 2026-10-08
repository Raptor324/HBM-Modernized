package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFoundryBaseBlockEntity;
import com.hbm_m.blockentity.machines.MachineStrandCasterBlockEntity;
import com.hbm_m.item.material.ItemMold;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineStrandCaster}: {@code getDimensions {0,0,6,0,1,0}} plus Giessturm {@code {2,0,1,0,1,0}},
 * {@code getOffset 0}; vier Fluidanschluesse unten, vier Einguesse oben. Form einsetzen per Rechtsklick, Schaufel
 * leert die Schmelze, sonst GUI. Gezeichnet vom {@code StrandCasterRenderer}.
 */
public class MachineStrandCasterBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay, com.hbm_m.api.block.IToolable {

    public MachineStrandCasterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(0, 0, 6, 0, 1, 0)
                .box(2, 0, 1, 0, 1, 0)
                // Fluid ports
                .extra(-1, 0, 1)
                .extra(-1, 0, 0)
                .extra(-5, 0, 0)
                .extra(-5, 0, 1)
                // Molten slop ports
                .extra(-1, 2, 1)
                .extra(-1, 2, 0)
                .extra(0, 2, 1)
                .extra(0, 2, 0)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineStrandCasterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.STRAND_CASTER_BE.get(),
                (lvl, pos, st, be) -> MachineFoundryBaseBlockEntity.tick(lvl, pos, st, be));
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

        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (world.getBlockEntity(pos) instanceof MachineStrandCasterBlockEntity cast) {

            // insert mold
            if (!held.isEmpty() && ItemMold.getMold(held) != null && cast.slots[0].isEmpty()) {
                cast.slots[0] = held.copyWithCount(1);
                held.shrink(1);
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, com.hbm_m.sound.HbmSoundsNT.get("hbm:item.upgradePlug"), SoundSource.BLOCKS, 1.0F, 1.0F);
                cast.setChanged();
                return InteractionResult.CONSUME;
            }

            if (FoundryBlockUtil.isShovel(held)) {
                FoundryBlockUtil.shovelOut(world, pos, player, cast, 1.0);
                return InteractionResult.CONSUME;
            }

            if (!player.isShiftKeyDown()) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, cast, buf -> buf.writeBlockPos(pos));
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    /** audit10: 1:1 {@code MachineStrandCaster.onScrew} - der Schraubenzieher (schleichend) nimmt die Gussform heraus. */
    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, net.minecraft.core.Direction side, float fX, float fY, float fZ,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (!(world.getBlockEntity(pos) instanceof MachineStrandCasterBlockEntity cast)) return false;
        if (cast.slots[0].isEmpty()) return false;
        if (world.isClientSide) return true;

        ItemStack mold = cast.slots[0].copy();
        if (!player.getInventory().add(mold)) {
            world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, mold));
        } else {
            player.inventoryMenu.broadcastChanges();
        }

        cast.slots[0] = ItemStack.EMPTY;
        cast.setChanged();
        return true;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !world.isClientSide && world.getBlockEntity(pos) instanceof MachineStrandCasterBlockEntity cast) {
            FoundryBlockUtil.dropContents(world, pos, cast, 1.0);
            for (ItemStack stack : cast.slots) {
                if (!stack.isEmpty()) world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack.copy()));
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineStrandCasterBlockEntity cast)) return;
        List<Component> text = new ArrayList<>();
        if (cast.slots[0].isEmpty()) {
            text.add(Component.translatable("foundry.noCast").withStyle(ChatFormatting.RED));
        } else if (cast.getInstalledMold() != null) {
            text.add(cast.getInstalledMold().getTitle().copy().withStyle(ChatFormatting.BLUE));
        }
        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xFF4000, 0x401000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineStrandCasterBlock> CODEC = simpleCodec(MachineStrandCasterBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
