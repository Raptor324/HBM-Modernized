package com.hbm_m.blockentity.generic;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.world.gen.util.LogicBlockActions;
import com.hbm_m.world.gen.util.LogicBlockConditions;
import com.hbm_m.world.gen.util.LogicBlockInteractions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code LogicBlock.TileEntityLogicBlock} (tileentity_ntm_logic_block): fuehrt pro Tick die Aktion aus und prueft
 * danach die Bedingung - erfuellt zaehlt {@link #phase} hoch und setzt {@link #timer} zurueck, sonst laeuft der Timer.
 * Interaktionen laufen beim Rechtsklick. Die Tarnung ist ein Blockzustand (statt Block + Meta).
 */
public class LogicBlockEntity extends BaseHbmBlockEntity {

    // phase zaehlt je erfuellter Bedingung hoch, timer zaehlt seit der letzten Erfuellung
    public int phase = 0;
    public int timer = 0;

    @Nullable public BlockState disguise;

    /** Aktionen laufen immer vor den Bedingungen; phase und timer steuern das Verhalten ueber die Bedingungen. */
    public String conditionID = "PLAYER_CUBE_5";
    public String actionID = "FODDER_WAVE";
    /** Interaktionen laufen beim Rechtsklick und bekommen dessen Parameter. */
    @Nullable public String interactionID;

    public Function<LogicBlockEntity, Boolean> condition;
    public Consumer<LogicBlockEntity> action;
    /** Welt, Blockentity, x, y, z, Spieler, Seite, hitX, hitY, hitZ - in dieser Reihenfolge. */
    public Consumer<Object[]> interaction;

    public Player player;

    public ForgeDirection direction = ForgeDirection.UNKNOWN;

    boolean disguised = false;

    public LogicBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOGIC_BLOCK.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LogicBlockEntity be) {
        be.updateEntity();
    }

    public void updateEntity() {

        if (!level.isClientSide) {
            if (action == null) {
                action = LogicBlockActions.actions.get(actionID);
            }
            if (condition == null) {
                condition = LogicBlockConditions.conditions.get(conditionID);
            }
            if (interaction == null && interactionID != null) {
                interaction = LogicBlockInteractions.interactions.get(interactionID);
            }

            if (action == null || condition == null) {
                level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 3);
                return;
            }
            action.accept(this);
            // die Aktion kann den Block bereits ersetzt haben
            if (isRemoved()) return;
            if (condition.apply(this)) {
                phase++;
                timer = 0;
            } else {
                timer++;
            }
        }
        if (!disguised) {
            setChanged();
            if (!level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            disguised = true;
        }
    }

    public Level getWorldObj() {
        return level;
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putInt("phase", phase);

        nbt.putString("actionID", actionID);
        nbt.putString("conditionID", conditionID);
        if (interactionID != null)
            nbt.putString("interactionID", interactionID);

        nbt.putInt("direction", direction.ordinal());
        if (disguise != null) {
            nbt.put("disguise", NbtUtils.writeBlockState(disguise));
        }
    }

    @Override
    protected void readNbtData(CompoundTag nbt, HolderLookup.Provider registries) {
        this.phase = nbt.getInt("phase");

        this.actionID = nbt.getString("actionID");
        this.conditionID = nbt.getString("conditionID");
        if (nbt.contains("interactionID")) this.interactionID = nbt.getString("interactionID");

        this.direction = ForgeDirection.getOrientation(nbt.getInt("direction"));

        if (nbt.contains("disguise")) {
            disguise = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), nbt.getCompound("disguise"));
        }
    }
}
