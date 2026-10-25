package com.hbm_m.blockentity.generic;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockWandLogic;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.interfaces.ICopiable;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.world.gen.nbt.INBTTileEntityTransformable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockWandLogic.TileEntityWandLogic} (tileentity_wand_spawner): haelt Aktion, Bedingung, Interaktion,
 * Ausrichtung und Tarnung; ausgeloest (Zuender oder Strukturaufbau) ersetzt es sich durch den eigentlichen Logikblock.
 */
public class WandLogicBlockEntity extends BaseHbmBlockEntity implements INBTTileEntityTransformable, ICopiable {

    private boolean triggerReplace;

    public int placedRotation;

    @Nullable public BlockState disguise;

    public String actionID = "FODDER_WAVE";
    public String conditionID = "PLAYER_CUBE_5";
    @Nullable public String interactionID;

    public WandLogicBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WAND_LOGIC.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WandLogicBlockEntity be) {
        if (!level.isClientSide) {
            if (be.triggerReplace) {
                // im ersten Tick nach dem Ausloesen durch den eigentlichen Block ersetzen
                be.replace();
            }
        }
    }

    public void setTriggerReplace() {
        this.triggerReplace = true;
    }

    /** Original {@code networkPackNT}: Zustand an die Clients. */
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void replace() {
        if (!level.isClientSide) {
            if (!(level.getBlockState(worldPosition).getBlock() instanceof BlockWandLogic)) {
                MainRegistry.LOGGER.warn("Somehow the block at: " + worldPosition.getX() + ", " + worldPosition.getY() + ", " + worldPosition.getZ() + " isn't a logic block but we're doing a TE update as if it is, cancelling!");
                return;
            }

            String action = actionID, condition = conditionID, interaction = interactionID;
            int rotation = placedRotation;
            BlockState dis = disguise;

            level.setBlock(worldPosition, (dis == null ? ModBlocks.LOGIC_BLOCK_INVIS : ModBlocks.LOGIC_BLOCK).get().defaultBlockState(), 3);

            BlockEntity te = level.getBlockEntity(worldPosition);

            if (te == null) {
                MainRegistry.LOGGER.warn("TE for logic block set incorrectly at: " + worldPosition.getX() + ", " + worldPosition.getY() + ", " + worldPosition.getZ() + ". If you're using some sort of world generation mod, report it to the author!");
                te = new LogicBlockEntity(worldPosition, level.getBlockState(worldPosition));
                level.setBlockEntity(te);
            }

            if (te instanceof LogicBlockEntity logic) {
                logic.actionID = action;
                logic.conditionID = condition;
                logic.interactionID = interaction;
                logic.direction = ForgeDirection.getOrientation(rotation);
                logic.disguise = dis;
                logic.setChanged();
            }
        }
    }

    @Override
    public void transformTE(net.minecraft.world.level.LevelAccessor world, int coordBaseMode) {
        triggerReplace = !ModClothConfig.get().structureDebug;
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putString("actionID", actionID);
        nbt.putString("conditionID", conditionID);
        if (interactionID != null)
            nbt.putString("interactionID", interactionID);
        nbt.putInt("rotation", placedRotation);
        if (disguise != null) {
            nbt.put("disguise", NbtUtils.writeBlockState(disguise));
        }
    }

    @Override
    protected void readNbtData(CompoundTag nbt, HolderLookup.Provider registries) {
        actionID = nbt.getString("actionID");
        conditionID = nbt.getString("conditionID");
        if (nbt.contains("interactionID"))
            interactionID = nbt.getString("interactionID");
        placedRotation = nbt.getInt("rotation");
        if (nbt.contains("disguise")) {
            disguise = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), nbt.getCompound("disguise"));
        }
    }

    @Override
    public CompoundTag getSettings(Level world, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("actionID", actionID);
        nbt.putString("conditionID", conditionID);
        if (interactionID != null)
            nbt.putString("interactionID", interactionID);
        nbt.putInt("rotation", placedRotation);
        if (disguise != null) {
            nbt.put("disguise", NbtUtils.writeBlockState(disguise));
        }
        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level world, Player player, BlockPos pos) {
        actionID = nbt.getString("actionID");
        conditionID = nbt.getString("conditionID");
        interactionID = nbt.getString("interactionID");
        // Original liest hier "disguiseMeta" statt "rotation" - so beibehalten
        placedRotation = nbt.getInt("disguiseMeta");
        if (nbt.contains("disguise")) {
            disguise = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), nbt.getCompound("disguise"));
        }
        sync();
    }
}
