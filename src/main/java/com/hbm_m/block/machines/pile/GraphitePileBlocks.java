package com.hbm_m.block.machines.pile;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.block.IBlowable;
import com.hbm_m.api.block.IInsertable;
import com.hbm_m.api.block.IToolable;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.bomb.BlockFlammable;
import com.hbm_m.blockentity.machines.pile.GraphitePileBlockEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Chicago Pile MK1 des Originals ({@code com.hbm.blocks.machine.pile}, LEGACY): gebohrte Graphitbloecke mit Staeben.
 * Die 1.7.10-Metadaten bleiben als {@code META} (0-15) erhalten: Bit 0-1 Achse der Bohrung (0 = hoch, 1 = Nord/Sued,
 * 2 = West/Ost), Bit 2 Aluminiumhuelle, Bit 3 je nach Block Pu-239-reich / Stab herausgezogen / Melder ausgeloest.
 */
public final class GraphitePileBlocks {

    private GraphitePileBlocks() {}

    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);

    public static int meta(BlockState s) { return s.hasProperty(META) ? s.getValue(META) : 0; }

    static boolean isFront(int meta, Direction side) {
        int cfg = meta & 3;
        int s = side.get3DDataValue();
        return s == cfg * 2 || s == cfg * 2 + 1;
    }

    static Item graphiteIngot() { return ModMaterialItems.item(ModMaterials.GRAPHITE, MaterialShape.INGOT); }

    // ================================================================================================

    /** 1:1 {@code BlockGraphiteDrilledBase}: Graphit-Huelle, Schraubenzieher zieht den Stab, Kolben-Einschieber schiebt bis zu drei Staebe weiter. */
    public abstract static class DrilledBase extends BlockFlammable implements IToolable, IInsertable {

        protected DrilledBase(Properties p) {
            super(p, 30, 5);
            registerDefaultState(stateDefinition.any().setValue(META, 0));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(META); }

        protected static void ejectItem(Level world, BlockPos pos, Direction dir, ItemStack stack) {
            ItemEntity dust = new ItemEntity(world, pos.getX() + 0.5D + dir.getStepX() * 0.75D, pos.getY() + 0.5D + dir.getStepY() * 0.75D, pos.getZ() + 0.5D + dir.getStepZ() * 0.75D, stack);
            dust.setDeltaMovement(dir.getStepX() * 0.25, dir.getStepY() * 0.25, dir.getStepZ() * 0.25);
            world.addFreshEntity(dust);
        }

        @Override
        public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
            if (tool != ToolType.SCREWDRIVER) return false;
            if (!world.isClientSide) {
                int meta = meta(world.getBlockState(pos));
                if (isFront(meta, side)) {
                    world.setBlock(pos, ModBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState().setValue(META, meta & 7), 3);
                    ejectItem(world, pos, side, new ItemStack(getInsertedItem(meta)));
                }
            }
            return true;
        }

        protected Item getInsertedItem(int meta) { return getInsertedItem(); }

        @Nullable protected Item getInsertedItem() { return null; }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            int meta = meta(state);
            List<ItemStack> drops = new ArrayList<>();
            drops.add(new ItemStack(graphiteIngot(), 8));
            if ((meta & 4) == 4) drops.add(new ItemStack(ModItems.SHELL_ALUMINUM.get()));
            if (getInsertedItem() != null) drops.add(new ItemStack(getInsertedItem(meta), 1));
            return drops;
        }

        /** {@code checkInteractions}: welcher Block ein eingeschobener Gegenstand wird. */
        @Nullable
        private static Block blockFor(ItemStack stack) {
            if (stack.is(ModItems.PILE_ROD_URANIUM.get()) || stack.is(ModItems.PILE_ROD_PU239.get())) return ModBlocks.BLOCK_GRAPHITE_FUEL.get();
            if (stack.is(ModItems.PILE_ROD_PLUTONIUM.get())) return ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get();
            if (stack.is(ModItems.PILE_ROD_SOURCE.get())) return ModBlocks.BLOCK_GRAPHITE_SOURCE.get();
            if (stack.is(ModItems.PILE_ROD_BORON.get())) return ModBlocks.BLOCK_GRAPHITE_ROD.get();
            if (stack.is(ModItems.PILE_ROD_LITHIUM.get())) return ModBlocks.BLOCK_GRAPHITE_LITHIUM.get();
            if (stack.is(ModItems.CELL_TRITIUM.get())) return ModBlocks.BLOCK_GRAPHITE_TRITIUM.get();
            if (stack.is(ModItems.PILE_ROD_DETECTOR.get())) return ModBlocks.BLOCK_GRAPHITE_DETECTOR.get();
            return null;
        }

        @Override
        public boolean insertItem(Level world, BlockPos pos, Direction dir, ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            Block baseBlock = blockFor(stack);
            if (baseBlock == null) return false;
            int baseMeta = stack.is(ModItems.PILE_ROD_PU239.get()) ? 0b1000 : 0;

            final int pureMeta = meta(world.getBlockState(pos)) & 3;
            if (!isFront(pureMeta, dir)) return false;

            // erst pruefen, ob sich ueberhaupt etwas herausschieben laesst (hoechstens drei)
            for (int i = 0; i <= 3; i++) {
                BlockPos p = pos.relative(dir, i);
                BlockState s = world.getBlockState(p);
                if (s.getBlock() instanceof DrilledBase b) {
                    int m = meta(s);
                    if ((m & 3) != pureMeta) return false;
                    if (b.getInsertedItem(m) == null) break;
                    else if (i >= 3) return false;
                } else {
                    if (s.isRedstoneConductor(world, p)) return false;
                    else break;
                }
            }

            int oldMeta = pureMeta | baseMeta;
            Block oldBlock = baseBlock;
            CompoundTag oldTag = new CompoundTag();

            for (int i = 0; i <= 3; i++) {
                BlockPos p = pos.relative(dir, i);
                BlockState s = world.getBlockState(p);
                Block newBlock = s.getBlock();

                if (newBlock instanceof DrilledBase) {
                    int newMeta = meta(s);
                    CompoundTag newTag = new CompoundTag();
                    BlockEntity te = world.getBlockEntity(p);
                    if (newBlock instanceof DrilledTE && te != null) newTag = te.saveWithoutMetadata();

                    world.setBlock(p, oldBlock.defaultBlockState().setValue(META, (oldMeta & ~0b100) | (newMeta & 0b100)), 3);

                    if (oldBlock instanceof DrilledTE) {
                        BlockEntity nte = world.getBlockEntity(p);
                        if (nte != null) {
                            if (!oldTag.isEmpty()) nte.load(oldTag);
                            else if (nte instanceof GraphitePileBlockEntities.Fuel f && (oldMeta & 8) != 0) f.progress = GraphitePileBlockEntities.Fuel.maxProgress - 1000;
                        }
                    }

                    oldMeta = newMeta;
                    oldBlock = newBlock;
                    oldTag = newTag;

                    if (oldBlock instanceof Drilled) break;
                } else {
                    Item eject = ((DrilledBase) oldBlock).getInsertedItem(oldMeta);
                    ejectItem(world, p.relative(dir.getOpposite()), dir, new ItemStack(eject));
                    world.playSound(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, HbmSoundsNT.get("item.upgradePlug"), SoundSource.BLOCKS, 1.25F, 1.0F);
                    break;
                }
            }
            return true;
        }
    }

    // ================================================================================================

    /** 1:1 {@code BlockGraphiteDrilled}: leere Bohrung - Stab, Tritiumzelle, Aluminiumhuelle oder Graphitbarren einsetzen. */
    public static class Drilled extends DrilledBase {
        public Drilled(Properties p) { super(p); }

        //? if < 1.21.1 {
        @Override
        public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return activate(state, world, pos, player, player.getItemInHand(hand), hit.getDirection()) ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
        }
        //?} else {
        /*@Override
        protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return activate(state, world, pos, player, stack, hit.getDirection()) ? net.minecraft.world.ItemInteractionResult.SUCCESS : net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        *///?}

        private boolean activate(BlockState state, Level world, BlockPos pos, Player player, ItemStack held, Direction side) {
            if (held.isEmpty()) return false;
            int meta = meta(state);
            if (!isFront(meta, side)) return false;

            if (check(world, pos, meta, held, ModItems.PILE_ROD_URANIUM.get(), ModBlocks.BLOCK_GRAPHITE_FUEL.get())) return true;
            if (check(world, pos, meta | 8, held, ModItems.PILE_ROD_PU239.get(), ModBlocks.BLOCK_GRAPHITE_FUEL.get())) return true;
            if (check(world, pos, meta, held, ModItems.PILE_ROD_PLUTONIUM.get(), ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get())) return true;
            if (check(world, pos, meta, held, ModItems.PILE_ROD_SOURCE.get(), ModBlocks.BLOCK_GRAPHITE_SOURCE.get())) return true;
            if (check(world, pos, meta, held, ModItems.PILE_ROD_BORON.get(), ModBlocks.BLOCK_GRAPHITE_ROD.get())) return true;
            if (check(world, pos, meta, held, ModItems.PILE_ROD_LITHIUM.get(), ModBlocks.BLOCK_GRAPHITE_LITHIUM.get())) return true;
            if (check(world, pos, meta, held, ModItems.CELL_TRITIUM.get(), ModBlocks.BLOCK_GRAPHITE_TRITIUM.get())) return true;
            if (check(world, pos, meta, held, ModItems.PILE_ROD_DETECTOR.get(), ModBlocks.BLOCK_GRAPHITE_DETECTOR.get())) return true;
            if (meta >> 2 != 1) {
                if (check(world, pos, meta | 4, held, ModItems.SHELL_ALUMINUM.get(), ModBlocks.BLOCK_GRAPHITE_DRILLED.get())) return true;
                if (check(world, pos, -1, held, graphiteIngot(), ModBlocks.BLOCK_GRAPHITE.get())) return true;
            }
            return false;
        }

        private boolean check(Level world, BlockPos pos, int meta, ItemStack held, Item item, Block block) {
            if (!held.is(item)) return false;
            held.shrink(1);
            BlockState s = meta < 0 ? block.defaultBlockState() : block.defaultBlockState().setValue(META, meta);
            world.setBlock(pos, s, 3);
            if (block == ModBlocks.BLOCK_GRAPHITE_FUEL.get() && (meta & 8) != 0 && world.getBlockEntity(pos) instanceof GraphitePileBlockEntities.Fuel f)
                f.progress = GraphitePileBlockEntities.Fuel.maxProgress - 1000;
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, HbmSoundsNT.get("item.upgradePlug"), SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }

        @Override
        public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
            int meta = meta(world.getBlockState(pos));
            if (tool != ToolType.SCREWDRIVER) return false;
            if (!world.isClientSide && isFront(meta, side) && meta >> 2 == 1) {
                world.setBlock(pos, defaultBlockState().setValue(META, meta & 3), 3);
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, HbmSoundsNT.get("item.upgradePlug"), SoundSource.BLOCKS, 1.0F, 0.85F);
                ejectItem(world, pos, side, new ItemStack(ModItems.SHELL_ALUMINUM.get()));
            }
            return true;
        }
    }

    // ================================================================================================

    /** 1:1 {@code BlockGraphiteDrilledTE}: Varianten mit Blockentity. */
    public abstract static class DrilledTE extends DrilledBase implements EntityBlock {
        protected DrilledTE(Properties p) { super(p); }

        @Nullable @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide) return null;
            return (l, p, s, be) -> { if (be instanceof GraphitePileBlockEntities.Ticking t) t.serverTick(); };
        }
    }

    /** 1:1 {@code BlockGraphiteFuel}: Uranstab (Bit 3 = Pu-239-reich), Komparator = Abbrand, Handbohrer zeigt Werte, Ventilator kuehlt. */
    public static class Fuel extends DrilledTE implements IBlowable {
        public Fuel(Properties p) { super(p); }

        @Nullable @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            GraphitePileBlockEntities.Fuel pile = new GraphitePileBlockEntities.Fuel(pos, state);
            if ((meta(state) & 8) != 0) pile.progress = GraphitePileBlockEntities.Fuel.maxProgress - 1000;
            return pile;
        }

        @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }

        @Override
        public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
            if (!(world.getBlockEntity(pos) instanceof GraphitePileBlockEntities.Fuel pile)) return 0;
            return Mth.clamp((pile.progress * 15) / (GraphitePileBlockEntities.Fuel.maxProgress - 1000), 0, 15);
        }

        @Override
        public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
            if (!world.isClientSide) {
                int meta = meta(world.getBlockState(pos));
                if (tool == ToolType.SCREWDRIVER && isFront(meta, side)) {
                    world.setBlock(pos, ModBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState().setValue(META, meta & 7), 3);
                    ejectItem(world, pos, side, new ItemStack(getInsertedItem(meta)));
                }
                if (tool == ToolType.HAND_DRILL && world.getBlockEntity(pos) instanceof GraphitePileBlockEntities.Fuel pile) {
                    player.sendSystemMessage(Component.literal("CP1 FUEL ASSEMBLY " + pos.getX() + " " + pos.getY() + " " + pos.getZ()).withStyle(ChatFormatting.GOLD));
                    player.sendSystemMessage(Component.literal("HEAT: " + pile.heat + "/" + GraphitePileBlockEntities.Fuel.maxHeat).withStyle(ChatFormatting.YELLOW));
                    player.sendSystemMessage(Component.literal("DEPLETION: " + pile.progress + "/" + GraphitePileBlockEntities.Fuel.maxProgress).withStyle(ChatFormatting.YELLOW));
                    player.sendSystemMessage(Component.literal("FLUX: " + pile.lastNeutrons).withStyle(ChatFormatting.YELLOW));
                    if ((meta & 8) == 8) player.sendSystemMessage(Component.literal("PU-239 RICH").withStyle(ChatFormatting.DARK_GREEN));
                }
            }
            return true;
        }

        @Override
        protected Item getInsertedItem(int meta) {
            return (meta & 8) == 8 ? ModItems.PILE_ROD_PU239.get() : ModItems.PILE_ROD_URANIUM.get();
        }

        @Override
        public void applyFan(Level world, BlockPos pos, Direction dir, int dist) {
            if (world.getBlockEntity(pos) instanceof GraphitePileBlockEntities.Fuel pile) pile.heat = (int) (pile.heat - pile.heat * 0.025);
        }
    }

    /** 1:1 {@code BlockGraphiteRod}: Borsteuerstab, per Klick auf die Stirnseite samt Nachbarn der gleichen Reihe ein-/ausgefahren. */
    public static class Rod extends DrilledBase {
        public Rod(Properties p) { super(p); }

        //? if < 1.21.1 {
        @Override
        public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return toggle(state, world, pos, player, hit.getDirection());
        }
        //?} else {
        /*@Override
        protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
            return toggle(state, world, pos, player, hit.getDirection());
        }
        *///?}

        private InteractionResult toggle(BlockState state, Level world, BlockPos pos, Player player, Direction side) {
            if (player.isShiftKeyDown()) return InteractionResult.PASS;
            int oldMeta = meta(state);
            int newMeta = oldMeta ^ 8;
            int pureMeta = oldMeta & 3;
            if (!isFront(pureMeta, side)) return InteractionResult.PASS;
            if (world.isClientSide) return InteractionResult.SUCCESS;

            world.setBlock(pos, state.setValue(META, newMeta), 3);
            world.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, pureMeta == (oldMeta & 11) ? 0.75F : 0.65F);

            for (int i = -1; i <= 1; i += 1) {
                BlockPos p = pos.relative(side, i);
                while (world.getBlockState(p).is(this) && meta(world.getBlockState(p)) == oldMeta) {
                    world.setBlock(p, world.getBlockState(p).setValue(META, newMeta), 3);
                    p = p.relative(side, i);
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override protected Item getInsertedItem() { return ModItems.PILE_ROD_BORON.get(); }
    }

    /** 1:1 {@code BlockGraphiteSource}: Plutonium- (2 Neutronen) bzw. Ra-Be-Quelle (1 Neutron) je Strahl. */
    public static class Source extends DrilledTE {
        private final boolean plutonium;

        public Source(Properties p, boolean plutonium) {
            super(p);
            this.plutonium = plutonium;
        }

        public boolean isPlutonium() { return plutonium; }

        @Nullable @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GraphitePileBlockEntities.Source(pos, state); }

        @Override protected Item getInsertedItem() { return plutonium ? ModItems.PILE_ROD_PLUTONIUM.get() : ModItems.PILE_ROD_SOURCE.get(); }
    }

    /** 1:1 {@code BlockGraphiteBreedingFuel}: Lithiumstab, erbrueter zu Tritium. */
    public static class BreedingFuel extends DrilledTE {
        public BreedingFuel(Properties p) { super(p); }

        @Nullable @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GraphitePileBlockEntities.BreedingFuel(pos, state); }

        @Override
        public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
            if (!world.isClientSide) {
                int meta = meta(world.getBlockState(pos));
                if (tool == ToolType.SCREWDRIVER && isFront(meta, side)) {
                    world.setBlock(pos, ModBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState().setValue(META, meta), 3);
                    ejectItem(world, pos, side, new ItemStack(ModItems.PILE_ROD_LITHIUM.get()));
                }
                if (tool == ToolType.HAND_DRILL && world.getBlockEntity(pos) instanceof GraphitePileBlockEntities.BreedingFuel pile) {
                    player.sendSystemMessage(Component.literal("CP1 FUEL ASSEMBLY " + pos.getX() + " " + pos.getY() + " " + pos.getZ()).withStyle(ChatFormatting.GOLD));
                    player.sendSystemMessage(Component.literal("DEPLETION: " + pile.progress + "/" + GraphitePileBlockEntities.BreedingFuel.maxProgress).withStyle(ChatFormatting.YELLOW));
                    player.sendSystemMessage(Component.literal("FLUX: " + pile.lastNeutrons).withStyle(ChatFormatting.YELLOW));
                }
            }
            return true;
        }

        @Override protected Item getInsertedItem() { return ModItems.PILE_ROD_LITHIUM.get(); }
    }

    /** 1:1 {@code BlockGraphiteBreedingProduct}: fertige Tritiumzelle. */
    public static class BreedingProduct extends DrilledBase {
        public BreedingProduct(Properties p) { super(p); }

        @Override protected Item getInsertedItem() { return ModItems.CELL_TRITIUM.get(); }
    }

    /** 1:1 {@code BlockGraphiteNeutronDetector}: faehrt bei Ueber-/Unterschreiten der Schwelle die Steuerstaebe seiner Achse. */
    public static class Detector extends DrilledTE {
        public Detector(Properties p) { super(p); }

        @Nullable @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GraphitePileBlockEntities.Detector(pos, state); }

        public void triggerRods(Level world, BlockPos pos) {
            BlockState state = world.getBlockState(pos);
            int oldMeta = meta(state);
            int newMeta = oldMeta ^ 8;
            int pureMeta = oldMeta & 3;

            world.setBlock(pos, state.setValue(META, newMeta), 3);
            world.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, HbmSoundsNT.get("item.techBleep"), SoundSource.BLOCKS, 0.02F, 1.0F);

            if (pureMeta > 2) return; // ForgeDirection.UNKNOWN
            Direction dir = Direction.from3DDataValue(pureMeta * 2);
            for (int i = -1; i <= 1; i += 1) {
                BlockPos p = pos.relative(dir, i);
                while (world.getBlockState(p).is(ModBlocks.BLOCK_GRAPHITE_ROD.get()) && meta(world.getBlockState(p)) == oldMeta) {
                    world.setBlock(p, world.getBlockState(p).setValue(META, newMeta), 3);
                    p = p.relative(dir, i);
                }
            }
        }

        @Override
        public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
            if (!world.isClientSide) {
                if (tool == ToolType.SCREWDRIVER) {
                    int meta = meta(world.getBlockState(pos));
                    if (!player.isShiftKeyDown()) {
                        if (isFront(meta, side)) {
                            world.setBlock(pos, ModBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState().setValue(META, meta & 7), 3);
                            ejectItem(world, pos, side, new ItemStack(getInsertedItem()));
                        }
                    } else if (world.getBlockEntity(pos) instanceof GraphitePileBlockEntities.Detector pile) {
                        player.sendSystemMessage(Component.literal("CP1 FUEL ASSEMBLY " + pos.getX() + " " + pos.getY() + " " + pos.getZ()).withStyle(ChatFormatting.GOLD));
                        player.sendSystemMessage(Component.literal("FLUX: " + pile.lastNeutrons + "/" + pile.maxNeutrons).withStyle(ChatFormatting.YELLOW));
                    }
                }
                if (tool == ToolType.DEFUSER && world.getBlockEntity(pos) instanceof GraphitePileBlockEntities.Detector pile) {
                    if (player.isShiftKeyDown()) {
                        if (pile.maxNeutrons > 1) pile.maxNeutrons--;
                    } else {
                        pile.maxNeutrons++;
                    }
                    pile.setChanged();
                }
            }
            return true;
        }

        @Override protected Item getInsertedItem() { return ModItems.PILE_ROD_DETECTOR.get(); }
    }
}
