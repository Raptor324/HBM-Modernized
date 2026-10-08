package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineMassStorageBlockEntity;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Port of {@code BlockMassStorage} (1.7.10 Original). */
public class MachineMassStorageBlock extends BaseEntityBlock {

    /**
     * 1:1-Port der vier Metadatenstufen aus {@code BlockMassStorage}: von der Holzkiste mit
     * hundert Plaetzen bis zum Stahlspeicher mit einer Million. Die Zahl ist der ganze
     * Unterschied - alles andere verhaelt sich gleich.
     */
    public enum Tier {
        WOOD(100L, "wood"),
        IRON(10_000L, "iron"),
        DESH(100_000L, "desh"),
        STEEL(1_000_000L, null);

        public final long capacity;
        /** Der Texturzusatz des Originals; {@code null} ist die Grundtextur. */
        public final String suffix;

        Tier(long capacity, String suffix) {
            this.capacity = capacity;
            this.suffix = suffix;
        }
    }

    private final Tier tier;

    public Tier getTier() { return tier; }

    public MachineMassStorageBlock(Properties properties) {
        this(properties, Tier.STEEL);
    }

    public MachineMassStorageBlock(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    /**
     * Original: Richtungsanteil der Metadate ({@code meta / 4}, Front = Seite {@code dir + 2}); beim Setzen zeigt die
     * Front zum Spieler ({@code i == 0} -> Norden). Traegt die Frontanzeige ({@code RenderMassStorage}).
     */
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineMassStorageBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_MASS_STORAGE_BE.get(),
                (lvl, pos, st, be) -> MachineMassStorageBlockEntity.tick(lvl, pos, st, (MachineMassStorageBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Original: mit Schloss oder Nachschluessel-Set in der Hand handelt das Item; schleichend oeffnet sich nichts
        net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && (held.getItem() instanceof com.hbm_m.item.tool.ItemLock || held.is(com.hbm_m.item.ModItems.KEY_KIT.get()))) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS; // Original: Client true, Server geschlichen false

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MachineMassStorageBlockEntity storage && !storage.canAccess(player)) return InteractionResult.SUCCESS;
            if (entity instanceof MenuProvider menuProvider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider menuProvider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
        }
    *///?}


    // ---- 1:1 BlockMassStorage.removedByPlayer / onBlockPlacedBy / breakBlock / addInformation ----

    //? if < 1.21.1 {
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        removedByPlayer(level, pos, state, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        removedByPlayer(level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

    /**
     * Original {@code removedByPlayer}: im Ueberlebensmodus mit Ernte faellt der Speicher als Item mit allen drei
     * Plaetzen ({@code slot0..2}), dem Schloss ({@code lock}/{@code lockMod}) und dem Vorrat ({@code stack}); in jedem
     * Fall verschwindet der Inhalt danach mit dem Block ({@code dropInv = false}).
     */
    private void removedByPlayer(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) return;
        if (!(level.getBlockEntity(pos) instanceof MachineMassStorageBlockEntity storage)) return;
        net.minecraft.core.HolderLookup.Provider registries = level.registryAccess();
        var inv = storage.getInventory();

        if (!player.getAbilities().instabuild && player.hasCorrectToolForDrops(state)) {
            net.minecraft.world.item.ItemStack drop = new net.minecraft.world.item.ItemStack(this);
            net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();

            for (int i = 0; i < inv.getSlots(); i++) {
                net.minecraft.world.item.ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                nbt.put("slot" + i, com.hbm_m.platform.PlatformHooks.safeItemSave(stack, registries));
            }

            if (storage.lockState.isLocked) {
                nbt.putInt("lock", storage.lockState.lock);
                nbt.putDouble("lockMod", storage.lockState.lockMod);
            }

            if (!nbt.isEmpty()) {
                nbt.putInt("stack", (int) storage.getStockpile());
            }

            if (!nbt.isEmpty()) {
                com.hbm_m.platform.PlatformHooks.setItemTag(drop, nbt);
            }

            level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop));
        }

        // dropInv = false: nichts mehr ausschuetten
        for (int i = 0; i < inv.getSlots(); i++) inv.setStackInSlot(i, net.minecraft.world.item.ItemStack.EMPTY);
        storage.setStockpile(0L);
    }

    /** Original {@code onBlockPlacedBy}: Plaetze, Schloss und Vorrat aus dem Item zurueckholen. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !com.hbm_m.platform.PlatformHooks.hasItemTag(stack)) return;
        if (!(level.getBlockEntity(pos) instanceof MachineMassStorageBlockEntity storage)) return;
        net.minecraft.nbt.CompoundTag nbt = com.hbm_m.platform.PlatformHooks.getItemTag(stack);
        if (nbt == null) return;
        net.minecraft.core.HolderLookup.Provider registries = level.registryAccess();
        var inv = storage.getInventory();

        for (int i = 0; i < inv.getSlots(); i++) {
            net.minecraft.nbt.CompoundTag slot = nbt.getCompound("slot" + i);
            inv.setStackInSlot(i, slot.isEmpty() ? net.minecraft.world.item.ItemStack.EMPTY : com.hbm_m.platform.PlatformHooks.itemStackOf(slot, registries));
        }

        if (nbt.contains("lock")) {
            storage.setPins(nbt.getInt("lock"));
            storage.setMod(nbt.getDouble("lockMod"));
            storage.lock();
        }

        storage.setStockpile(nbt.getInt("stack"));
    }

    /** Original {@code breakBlock} mit {@code dropInv}: alles ausser dem Filter (Platz 1) faellt heraus. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !com.hbm_m.multiblock.ContraptionAssemblyGuard.isMoving()
                && level.getBlockEntity(pos) instanceof MachineMassStorageBlockEntity storage) {
            var inv = storage.getInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                if (i == MachineMassStorageBlockEntity.SLOT_FILTER) continue;
                net.minecraft.world.item.ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                }
            }
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    /** Original {@code addInformation}: Sorte in Gold und "Vorrat / Fassungsvermoegen". */
    public void addInformation(net.minecraft.world.item.ItemStack stack, java.util.List<net.minecraft.network.chat.Component> list) {
        if (!com.hbm_m.platform.PlatformHooks.hasItemTag(stack)) return;
        net.minecraft.nbt.CompoundTag nbt = com.hbm_m.platform.PlatformHooks.getItemTag(stack);
        if (nbt == null || !nbt.contains("slot1")) return;

        //? if < 1.21.1 {
        net.minecraft.core.HolderLookup.Provider registries = null;
        //?} else {
        /*net.minecraft.core.HolderLookup.Provider registries = com.hbm_m.platform.PlatformHooks.clientProvider();
        *///?}
        net.minecraft.world.item.ItemStack type = com.hbm_m.platform.PlatformHooks.itemStackOf(nbt.getCompound("slot1"), registries);

        if (!type.isEmpty()) {
            list.add(net.minecraft.network.chat.Component.literal(type.getHoverName().getString()).withStyle(net.minecraft.ChatFormatting.GOLD));
            list.add(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.US, "%,d", nbt.getInt("stack")) + " / " + String.format(java.util.Locale.US, "%,d", tier.capacity)));
        }
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineMassStorageBlock> CODEC = simpleCodec(MachineMassStorageBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    /** Original {@code getComparatorInputOverride}: {@code redstone = Vorrat * 15 / Kapazitaet}. */
    @Override
    public boolean hasAnalogOutputSignal(net.minecraft.world.level.block.state.BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.block.entity.BlockEntity te = level.getBlockEntity(pos);
        if (!(te instanceof com.hbm_m.blockentity.machines.MachineMassStorageBlockEntity storage)) return 0;
        long cap = storage.getCapacity();
        return cap <= 0 ? 0 : (int) (storage.getStockpile() * 15L / cap);
    }
}
