package com.hbm_m.blockentity.generic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm_m.api.tile.ILockableTile;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockWandLoot;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.decorations.DecoLootBlockEntity;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.interfaces.ICopiable;
import com.hbm_m.itempool.ItemPool;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.util.LootGenerator;
import com.hbm_m.world.gen.nbt.INBTTileEntityTransformable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * 1:1 {@code BlockWandLoot.TileEntityWandLoot} (tileentity_wand_loot): merkt sich Ersatzblock, Lootpool, Mindest-
 * und Hoechstzahl, Schloss und die Blickrichtung beim Setzen. Ausgeloest (Strukturaufbau oder Zuender) ersetzt es
 * sich im naechsten Tick durch den Behaelter und befuellt ihn.
 */
public class WandLootBlockEntity extends BaseHbmBlockEntity implements INBTTileEntityTransformable, ICopiable {

    private boolean triggerReplace;

    public Block replaceBlock = ModBlocks.DECO_LOOT.get();
    /** Original-Meta des Ersatzblocks; im Port nur fuer die Ausrichtung behaelterartiger Bloecke genutzt. */
    public int replaceMeta;

    public String poolName = LootGenerator.LOOT_BOOKLET;
    public int minItems;
    public int maxItems = 1;

    public float placedRotation;

    public float lockMod = 0;
    public int lockCode = 0;
    public boolean cheesable = true;

    public WandLootBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WAND_LOOT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WandLootBlockEntity be) {
        if (!level.isClientSide) {
            if (be.triggerReplace) {
                // im ersten Tick durch den eigentlichen Block ersetzen und befuellen
                be.replace();
            }
        }
    }

    public void setTriggerReplace() {
        this.triggerReplace = true;
        setChanged();
    }

    /** Original {@code networkPackNT}: Zustand an die Clients. */
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void replace() {
        if (!(level.getBlockState(worldPosition).getBlock() instanceof BlockWandLoot)) {
            MainRegistry.LOGGER.warn("Somehow the block at: " + worldPosition.getX() + ", " + worldPosition.getY() + ", " + worldPosition.getZ() + " isn't a loot block but we're doing a TE update as if it is, cancelling!");
            return;
        }

        ItemPool.WeightedContent[] pool = ItemPool.getPool(poolName);

        // Original: onBlockPlacedBy eines Fake-Spielers mit der gespeicherten Blickrichtung -> hier direkt die Ausrichtung
        BlockState state = orient(replaceBlock.defaultBlockState(), placedRotation);
        level.setBlock(worldPosition, state, 2);

        BlockEntity te = level.getBlockEntity(worldPosition);

        if (te instanceof WandLootBlockEntity) {
            MainRegistry.LOGGER.warn("TE set incorrectly at: " + worldPosition.getX() + ", " + worldPosition.getY() + ", " + worldPosition.getZ() + ". If you're using some sort of world generation mod, report it to the author!");
            return;
        }

        if (te instanceof ILockableTile lockable && lockCode != 0) {
            lockable.setPins(lockCode);
            lockable.setMod(lockMod);
            lockable.setCheesable(lockMod != 0);
            lockable.lock();
        }
        if (ItemPool.isInventory(te)) {
            int count = minItems;
            if (maxItems - minItems > 0) count += level.random.nextInt(maxItems - minItems);
            ItemPool.generateChestContents(level.random, pool, te, count);
        } else if (te instanceof DecoLootBlockEntity loot) {
            loot.clearItems();
            LootGenerator.applyLoot(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), poolName);
        }
        if (te != null) te.setChanged();
    }

    /** Fake-Spieler-Ausrichtung: Blickrichtung (Gier) auf FACING des Ersatzblocks; Behaelter schauen zum Spieler. */
    private static BlockState orient(BlockState state, float yaw) {
        Direction look = Direction.fromYRot(yaw);
        DirectionProperty prop = null;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) prop = BlockStateProperties.HORIZONTAL_FACING;
        else if (state.hasProperty(BlockStateProperties.FACING)) prop = BlockStateProperties.FACING;
        if (prop == null) return state;
        return state.setValue(prop, look.getOpposite());
    }

    public List<String> getPoolNames(boolean loot) {
        if (loot) return Arrays.asList(LootGenerator.getLootNames());

        ItemPool.initialize();
        List<String> names = new ArrayList<>(ItemPool.pools.keySet());
        return names;
    }

    @Override
    public void transformTE(LevelAccessor world, int coordBaseMode) {
        triggerReplace = !ModClothConfig.get().structureDebug;
        placedRotation = Mth.wrapDegrees(placedRotation + coordBaseMode * 90);
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, HolderLookup.Provider registries) {
        Block writeBlock = replaceBlock == null ? ModBlocks.DECO_LOOT.get() : replaceBlock;
        nbt.putString("block", BuiltInRegistries.BLOCK.getKey(writeBlock).toString());
        nbt.putInt("meta", replaceMeta);
        nbt.putInt("min", minItems);
        nbt.putInt("max", maxItems);
        nbt.putString("pool", poolName);
        nbt.putFloat("rot", placedRotation);

        nbt.putInt("lockCode", lockCode);
        nbt.putFloat("lockMod", lockMod);
        nbt.putBoolean("cheesable", cheesable);

        nbt.putBoolean("trigger", triggerReplace);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, HolderLookup.Provider registries) {
        replaceBlock = readBlock(nbt.getString("block"));
        replaceMeta = nbt.getInt("meta");
        minItems = nbt.getInt("min");
        maxItems = nbt.getInt("max");
        poolName = nbt.getString("pool");
        placedRotation = nbt.getFloat("rot");

        lockCode = nbt.getInt("lockCode");
        lockMod = nbt.getFloat("lockMod");
        cheesable = nbt.getBoolean("cheesable");

        triggerReplace = nbt.getBoolean("trigger");
    }

    static Block readBlock(String name) {
        ResourceLocation rl = ResourceLocation.tryParse(name);
        Block b = rl == null ? null : BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
        if (b == null || b == Blocks.AIR) return ModBlocks.DECO_LOOT.get();
        return b;
    }

    @Override
    public CompoundTag getSettings(Level world, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        Block block = replaceBlock != null ? replaceBlock : ModBlocks.DECO_LOOT.get();

        nbt.putString("replaceBlock", BuiltInRegistries.BLOCK.getKey(block).toString());
        nbt.putInt("replaceMeta", replaceMeta);
        nbt.putInt("minItems", minItems);
        nbt.putInt("maxItems", maxItems);
        nbt.putString("poolName", poolName);

        nbt.putInt("lockCode", lockCode);
        nbt.putFloat("lockMod", lockMod);
        nbt.putBoolean("cheesable", cheesable);

        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level world, Player player, BlockPos pos) {
        replaceBlock = readBlock(nbt.getString("replaceBlock"));
        replaceMeta = nbt.getInt("replaceMeta");
        minItems = nbt.getInt("minItems");
        maxItems = nbt.getInt("maxItems");
        poolName = nbt.getString("poolName");

        lockCode = nbt.getInt("lockCode");
        lockMod = nbt.getFloat("lockMod");
        cheesable = nbt.getBoolean("cheesable");
        sync();
    }
}
