package com.hbm_m.block.machines.pile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;
import com.hbm_m.platform.BlockHooks;
import com.hbm_m.platform.LevelHooks;

/**
 * Port of NEXT {@code com.hbm.blocks.machine.pile.BlockGraphiteDrilledBase}: abstract base of
 * every CP-1 pile channel block (drilled graphite, control rod, neutron detector). A channel is
 * a drilled hole through the pile along {@link #AXIS}; its two end faces are the "channel faces".
 *
 * <p>State space: {@link #AXIS} (hole axis) + {@link #SHROUDED} (aluminium shell) for all pile
 * blocks, {@link #PU239} (fuel machines' scope) and {@link #WITHDRAWN} (rod/detector) added by
 * subclasses. NEXT implements {@code IToolable}/{@code IInsertable} on this class; Modernized has
 * neither interface, so the screwdriver path became {@link #onScrew} invoked from the subclasses'
 * version-gated use hook (direct {@link #isScrewdriver} item check, SteelWallBlock convention),
 * and {@link #insertItem} is kept as a plain public method (its only NEXT caller,
 * BlockEntityPistonInserter, is not ported yet).</p>
 */
public abstract class BlockGraphiteDrilledBase extends Block {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty SHROUDED = BooleanProperty.create("shrouded");

    /** NEXT keeps this on the base so the fuel machine ports can map their rods onto BLOCK_GRAPHITE_FUEL. */
    public static final BooleanProperty PU239 = BooleanProperty.create("pu239");

    public static final BooleanProperty WITHDRAWN = BooleanProperty.create("withdrawn");

    protected BlockGraphiteDrilledBase(Properties props) {
        super(props);
        BlockState def =
                stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(SHROUDED, false);
        if (def.hasProperty(PU239)) def = def.setValue(PU239, false);
        if (def.hasProperty(WITHDRAWN)) def = def.setValue(WITHDRAWN, false);
        registerDefaultState(def);
    }

    /** NEXT {@code isChannelFace}: the clicked face lies on the drilled axis (a channel opening). */
    public static boolean isChannelFace(BlockState state, @Nullable Direction side) {
        return side != null && side.getAxis() == state.getValue(AXIS);
    }

    /** NEXT {@code copyShape}: carries only AXIS+SHROUDED to the new block (1.7.10's {@code meta & 7}). */
    public static BlockState copyShape(BlockState from, BlockState to) {
        return to.setValue(AXIS, from.getValue(AXIS)).setValue(SHROUDED, from.getValue(SHROUDED));
    }

    protected static void ejectItem(Level level, BlockPos pos, Direction dir, ItemStack stack) {
        ItemEntity dust =
                new ItemEntity(
                        level,
                        pos.getX() + 0.5D + dir.getStepX() * 0.75D,
                        pos.getY() + 0.5D + dir.getStepY() * 0.75D,
                        pos.getZ() + 0.5D + dir.getStepZ() * 0.75D,
                        stack);
        dust.setDeltaMovement(dir.getStepX() * 0.25, dir.getStepY() * 0.25, dir.getStepZ() * 0.25);
        level.addFreshEntity(dust);
    }

    /** NEXT {@code aluminiumShell()}; the shell item's Modernized name is {@code shell_aluminum}. */
    public static Item aluminiumShell() {
        return ModItems.SHELL_ALUMINUM.get();
    }

    /** Tool-dispatch glue: Modernized has no IToolable, blocks compare held items directly (SteelWallBlock). */
    protected static boolean isScrewdriver(ItemStack stack) {
        return stack.getItem() == ModItems.SCREWDRIVER.get()
                || stack.getItem() == ModItems.SCREWDRIVER_DESH.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, SHROUDED);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return RotatedPillarBlock.rotatePillar(state, rotation);
    }

    /**
     * Port of NEXT {@code onScrew} (ToolType.SCREWDRIVER branch): a screwdriver on a channel face
     * ejects the inserted item back to the plain drilled channel. Modernized has no IToolable
     * interface, so this runs from the subclasses' version-gated use hook (non-sneak clicks) and,
     * for sneak clicks (vanilla skips the block use hooks whenever the player sneaks with a
     * non-empty hand), through the public {@link #onScrewForTool} bridge from
     * ScrewdriverItem/ItemDefuser. Tool durability loss (NEXT ItemTooling.hurtAndBreak) is not
     * applied, matching project convention.
     */
    protected InteractionResult onScrew(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            @Nullable Direction side,
            InteractionHand hand) {

        if (!isScrewdriver(player.getItemInHand(hand))) return InteractionResult.PASS;

        if (!level.isClientSide() && isChannelFace(state, side)) {
            Item inserted = getInsertedItem(state);
            level.setBlock(
                    pos,
                    copyShape(
                            state, ModBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState()),
                    3);
            // NEXT does `new ItemStack(inserted)` unconditionally, so a null-inserted eject
            // (plain channel) would crash there - treated as unreachable; we eject
            // ItemStack.EMPTY defensively instead.
            ejectItem(level, pos, side, inserted == null ? ItemStack.EMPTY : new ItemStack(inserted));
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Public bridge for the item-side dispatch (ScrewdriverItem/ItemDefuser): NEXT reaches the
     * screwdriver/defuser paths via {@code IToolable.onScrew} from the held item, which vanilla
     * never sneak-gates. Virtual dispatch reaches the subclass overrides.
     */
    public final InteractionResult onScrewForTool(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            @Nullable Direction side,
            InteractionHand hand) {
        return onScrew(state, level, pos, player, side, hand);
    }

    protected @Nullable Item getInsertedItem(BlockState state) {
        return getInsertedItem();
    }

    public @Nullable Item getInsertedItem() {
        return null;
    }

    /**
     * Port of NEXT {@code checkInteractions}. INTERIM: only the tritium mapping
     * (CELL_TRITIUM -&gt; BLOCK_GRAPHITE_TRITIUM, NEXT :151-152) is still unmapped, pending the
     * breeding-product port. PLUTONIUM/SOURCE landed with the block_graphite_source port - the
     * stub-era reason for their deferral (targets without AXIS crashing copyShape) is gone,
     * both targets are real BlockGraphiteSource blocks now.
     */
    protected @Nullable BlockState checkInteractions(ItemStack stack) {
        Item item = stack.getItem();
        if (item == ModItems.PILE_ROD_URANIUM.get())
            return ModBlocks.BLOCK_GRAPHITE_FUEL.get().defaultBlockState();
        if (item == ModItems.PILE_ROD_PU239.get())
            return ModBlocks.BLOCK_GRAPHITE_FUEL.get().defaultBlockState().setValue(PU239, true);
        if (item == ModItems.PILE_ROD_PLUTONIUM.get())
            return ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get().defaultBlockState();
        if (item == ModItems.PILE_ROD_SOURCE.get())
            return ModBlocks.BLOCK_GRAPHITE_SOURCE.get().defaultBlockState();
        if (item == ModItems.PILE_ROD_BORON.get())
            return ModBlocks.BLOCK_GRAPHITE_ROD.get().defaultBlockState();
        if (item == ModItems.PILE_ROD_LITHIUM.get())
            return ModBlocks.BLOCK_GRAPHITE_LITHIUM.get().defaultBlockState();
        if (item == ModItems.PILE_ROD_DETECTOR.get())
            return ModBlocks.BLOCK_GRAPHITE_DETECTOR.get().defaultBlockState();
        return null;
    }

    /**
     * Port of NEXT {@code IInsertable.insertItem} (piston shift-insertion chain). No caller in
     * Modernized yet (BlockEntityPistonInserter is out of scope); accepts only the rods mapped
     * by {@link #checkInteractions}. Carries BlockEntity NBT along the chain via PlatformHooks
     * (NEXT uses saveCustomOnly/loadCustomOnly).
     */
    public boolean insertItem(Level level, BlockPos pos, Direction dir, ItemStack stack) {

        if (stack.isEmpty()) return false;

        BlockState insert = checkInteractions(stack);
        if (insert == null) return false;

        BlockState here = level.getBlockState(pos);
        Direction.Axis axis = here.getValue(AXIS);
        if (dir.getAxis() != axis) return false;

        for (int i = 0; i <= 3; i++) {
            BlockPos probe = pos.relative(dir, i);
            BlockState state = level.getBlockState(probe);

            if (state.getBlock() instanceof BlockGraphiteDrilledBase graphite) {
                if (state.getValue(AXIS) != axis) return false;

                if (graphite.getInsertedItem(state) == null) break;
                else if (i >= 3) return false;
            } else {

                if (state.isCollisionShapeFullBlock(level, probe)
                        && state.canOcclude()
                        && !state.isSignalSource()) return false;
                else break;
            }
        }

        HolderLookup.Provider registries = level.registryAccess();
        BlockState carried = copyShape(here, insert);
        CompoundTag carriedData = new CompoundTag();

        for (int i = 0; i <= 3; i++) {
            BlockPos probe = pos.relative(dir, i);
            BlockState state = level.getBlockState(probe);

            if (state.getBlock() instanceof BlockGraphiteDrilledBase) {
                BlockEntity existing = level.getBlockEntity(probe);
                CompoundTag data =
                        existing != null
                                ? BlockHooks.saveWithoutMetadata(existing, registries)
                                : null;

                level.setBlock(probe, carried.setValue(SHROUDED, state.getValue(SHROUDED)), 3);

                if (carriedData != null) {
                    BlockEntity moved = level.getBlockEntity(probe);
                    if (moved != null) {
                        BlockHooks.loadCustomOnly(moved, carriedData, registries);
                        moved.setChanged();
                    }
                }

                carried = state;
                carriedData = data;

                if (state.getBlock() instanceof BlockGraphiteDrilled) break;
            } else {
                // Same as onScrew above: NEXT does `new ItemStack(eject)` unconditionally, so a
                // null-inserted eject would crash there - treated as unreachable; we eject
                // ItemStack.EMPTY defensively instead.
                Item eject =
                        ((BlockGraphiteDrilledBase) carried.getBlock()).getInsertedItem(carried);
                ejectItem(
                        level,
                        probe.relative(dir.getOpposite()),
                        dir,
                        eject == null ? ItemStack.EMPTY : new ItemStack(eject));
                LevelHooks.playSound(
                        level,
                        probe,
                        ModSounds.UPGRADE_PLUG.get(),
                        SoundSource.BLOCKS,
                        1.25F,
                        1.0F);
                break;
            }
        }

        return true;
    }
}
