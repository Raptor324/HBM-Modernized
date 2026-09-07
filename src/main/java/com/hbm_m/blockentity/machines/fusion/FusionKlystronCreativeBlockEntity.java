package com.hbm_m.blockentity.machines.fusion;

import com.hbm_m.api.fusion.KlystronNetwork;
import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.recipe.FusionRecipe;
import com.hbm_m.recipe.ModRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1-Port von {@code TileEntityFusionKlystronCreative} (1.7.10).
 *
 * <p>Liefert jeden Tick {@code FusionRecipes.INSTANCE.maxInput} - also genau so viel
 * Klystronenergie, wie das teuerste registrierte Fusionsrezept zum Zuenden braucht.
 * Der Wert wird hier aus dem geladenen Rezeptsatz bestimmt (Original: {@code registerPost()}).</p>
 */
public class FusionKlystronCreativeBlockEntity extends FusionSyncedBlockEntity {

    private GenNode<KlystronNetwork> klystronNode;

    public boolean isConnected = false;

    // Rendering (Client)
    public float fan;
    public float prevFan;
    public float fanSpeed;
    public static final float FAN_ACCELERATION = 0.125F;

    public FusionKlystronCreativeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_KLYSTRON_CREATIVE_BE.get(), pos, state);
    }

    /** Original: {@code FusionRecipes.maxInput} - hoechste Zuendschwelle aller Rezepte. */
    public static long getMaxInput(Level level) {
        long max = 0;
        for (FusionRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.FUSION_TYPE.get())) {
            if (recipe.getIgnitionTemp() > max) max = recipe.getIgnitionTemp();
        }
        return max;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionKlystronCreativeBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.serverTick(serverLevel, pos, state);
        } else {
            be.clientTick();
        }
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        this.klystronNode = FusionKlystronBlockEntity.handleKlystronNode(this.klystronNode, this, level, pos, state);
        this.isConnected = FusionKlystronBlockEntity.provideKyU(this.klystronNode, getMaxInput(level));

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick() {
        if (this.isConnected) this.fanSpeed += FAN_ACCELERATION;
        else this.fanSpeed -= FAN_ACCELERATION;

        this.fanSpeed = Mth.clamp(this.fanSpeed, 0F, 5F);

        this.prevFan = this.fan;
        this.fan += this.fanSpeed;

        if (this.fan >= 360F) {
            this.fan -= 360F;
            this.prevFan -= 360F;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel && klystronNode != null) {
            UniNodespace.destroyNode(serverLevel, klystronNode);
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("connected", isConnected);
    }

    @Override
    protected void readNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.isConnected = tag.getBoolean("connected");
    }

    private AABB renderBounds = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (renderBounds == null) {
            renderBounds = new AABB(
                    worldPosition.getX() - 4, worldPosition.getY(), worldPosition.getZ() - 4,
                    worldPosition.getX() + 5, worldPosition.getY() + 5, worldPosition.getZ() + 5);
        }
        return renderBounds;
    }
}
