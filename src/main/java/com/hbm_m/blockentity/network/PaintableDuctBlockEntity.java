package com.hbm_m.blockentity.network;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.FluidNetProvider;
import com.hbm_m.api.fluids.FluidNode;
import com.hbm_m.api.fluids.IFluidPipeMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1 {@code FluidDuctPaintable.TileEntityPipePaintable}: Rohr ({@code TileEntityPipeBaseNT}) als Vollblock, das
 * sich mit einem Block ueberstreichen laesst.
 */
public class PaintableDuctBlockEntity extends BaseHbmBlockEntity implements IFluidPipeMK2 {

    private Fluid fluidType = Fluids.EMPTY;
    @Nullable private FluidNode node;
    @Nullable private BlockState camo;
    /** {@code TileEntityPipeExhaustPaintable}: drei Knoten fuer Rauch, verbleiten und giftigen Rauch. */
    private final FluidNode[] smokeNodes = new FluidNode[3];

    public boolean isExhaust() {
        Block b = getBlockState().getBlock();
        if (b instanceof com.hbm_m.block.network.BoxDuctBlock box) return box.kind == com.hbm_m.block.network.BoxDuctGeometry.Kind.EXHAUST;
        return b instanceof com.hbm_m.block.network.PaintableDuctBlock p && p.isExhaust();
    }

    private static Fluid[] smokes() {
        return new Fluid[] { com.hbm_m.inventory.fluid.ModFluids.SMOKE.getSource(), com.hbm_m.inventory.fluid.ModFluids.SMOKE_LEADED.getSource(),
                com.hbm_m.inventory.fluid.ModFluids.SMOKE_POISON.getSource() };
    }

    public PaintableDuctBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_DUCT_PAINTABLE.get(), pos, state);
    }

    /** Fuer andere Rohrbloecke ({@code TileEntityPipeBaseNT}), z.B. die Kastenrohre. */
    protected PaintableDuctBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override public Fluid getFluidType() { return fluidType; }

    /** Knoten des Rohrnetzes (fuer das Messrohr {@code TileEntityPipeGauge}). */
    @Nullable public FluidNode getNode() { return node; }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        if (fromDir == null) return false;
        if (isExhaust()) {
            for (Fluid f : smokes()) if (fluid == f) return true;
            return false;
        }
        return VanillaFluidEquivalence.sameSubstance(fluid, this.fluidType);
    }

    @Nullable public BlockState getCamo() { return camo; }

    public void setCamo(@Nullable BlockState camo) {
        this.camo = camo;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void setFluidType(Fluid fluid) {
        Fluid prev = this.fluidType;
        this.fluidType = fluid != null ? fluid : Fluids.EMPTY;
        setChanged();
        if (level instanceof ServerLevel sl) {
            if (node != null && !node.isExpired()) UniNodespace.destroyNode(sl, node);
            node = null;
            if (prev != Fluids.EMPTY && prev != fluidType) UniNodespace.destroyNode(sl, worldPosition, FluidNetProvider.forFluid(prev));
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            if (getBlockState().getBlock() instanceof com.hbm_m.block.network.BoxDuctBlock) com.hbm_m.block.network.BoxDuctBlock.refreshAround(level, worldPosition);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PaintableDuctBlockEntity te) {
        if (!(level instanceof ServerLevel sl)) return;
        if (te.isExhaust()) {
            Fluid[] sm = smokes();
            for (int i = 0; i < 3; i++) {
                if (te.smokeNodes[i] == null || te.smokeNodes[i].isExpired()) {
                    var existing = UniNodespace.getNode(sl, pos, FluidNetProvider.forFluid(sm[i]));
                    if (existing instanceof FluidNode fn && !fn.isExpired()) te.smokeNodes[i] = fn;
                    else {
                        te.smokeNodes[i] = te.createNode(sm[i], pos);
                        UniNodespace.createNode(sl, te.smokeNodes[i]);
                    }
                }
            }
            return;
        }
        if (te.fluidType == Fluids.EMPTY) return;
        if (te.node == null || te.node.isExpired()) {
            var existing = UniNodespace.getNode(sl, pos, FluidNetProvider.forFluid(te.fluidType));
            if (existing instanceof FluidNode fn && !fn.isExpired()) te.node = fn;
            else {
                te.node = te.createNode(te.fluidType, pos);
                UniNodespace.createNode(sl, te.node);
            }
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel sl) {
            if (node != null && !node.isExpired()) UniNodespace.destroyNode(sl, node);
            for (int i = 0; i < 3; i++) if (smokeNodes[i] != null && !smokeNodes[i].isExpired()) UniNodespace.destroyNode(sl, smokeNodes[i]);
        }
        node = null;
        java.util.Arrays.fill(smokeNodes, null);
        super.setRemoved();
    }

    //? if forge {
    @Override
    public void onChunkUnloaded() {
        if (node != null) node.expired = true;
        for (FluidNode n : smokeNodes) if (n != null) n.expired = true;
        super.onChunkUnloaded();
    }
    //?}

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        ResourceLocation loc = BuiltInRegistries.FLUID.getKey(fluidType);
        if (loc != null) tag.putString("FluidType", loc.toString());
        if (camo != null) tag.put("camo", NbtUtils.writeBlockState(camo));
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        if (tag.contains("FluidType")) {
            Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString("FluidType")));
            this.fluidType = f != null ? f : Fluids.EMPTY;
        }
        camo = null;
        if (tag.contains("camo")) {
            HolderLookup.Provider access = registries;
            if (access == null && level != null) access = level.registryAccess();
            if (access != null) camo = NbtUtils.readBlockState(access.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), tag.getCompound("camo"));
            else camo = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("camo"));
        }
    }
}
