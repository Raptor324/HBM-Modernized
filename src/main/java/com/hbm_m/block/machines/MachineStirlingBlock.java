package com.hbm_m.block.machines;

import com.hbm_m.platform.StackNbt;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineStirlingBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineStirling} (BlockDummyable): {@code getDimensions {1,0,1,1,1,1}}, {@code getOffset 1}, die vier
 * Seitenmitten des Sockels sind {@code makeExtra}-Zellen ({@code TileEntityProxyCombo().power()}). Eine Klasse fuer alle
 * 3 Bauarten (normal/Stahl/kreativ), unterschieden per Block-Identitaet wie im Original. Kein GUI - Rechtsklick mit dem
 * passenden Grosszahnrad setzt ein fehlendes Zahnrad ein. Ohne Zahnrad abgebaut droppt die Variante "ohne Zahnrad"
 * (Original Item-Schaden 1, hier Item-Tag {@link #TAG_NO_COG}).
 *
 * <p>Alte Welten (Port-Einzelblock): der Kern bleibt an seiner Stelle, die Dummy-Zellen ergaenzt die
 * Mehrblock-Selbstreparatur beim Laden ({@code MultiblockStructureHelper.attemptAutoRepair}), sobald Platz ist.</p>
 */
public class MachineStirlingBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay {

    public MachineStirlingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original fillSpace: makeExtra an x+1, x-1, z+1, z-1 um den Kern
        return DummyableStructureBuilder.create()
                .box(1, 0, 1, 1, 1, 1)
                .extra(1, 0, 0)
                .extra(-1, 0, 0)
                .extra(0, 0, 1)
                .extra(0, 0, -1)
                .placementOffset(1)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Zahnraeder und Kolben zeichnet der StirlingRenderer (RenderStirling); das Blockmodell bleibt fuer Partikel.
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineStirlingBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.STIRLING_BE.get(), MachineStirlingBlockEntity::tick);
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

    /** Original {@code onBlockActivated}: nicht schleichend, passendes Zahnrad ({@code getGeatMeta}) und keins eingebaut. */
    private InteractionResult activate(Level level, BlockPos pos, Player player, ItemStack held) {

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;

        } else if (!player.isShiftKeyDown()) {

            if (!(level.getBlockEntity(pos) instanceof MachineStirlingBlockEntity stirling))
                return InteractionResult.PASS;

            if (stirling.tryRepair(player, held)) {
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineStirlingBlock> CODEC = simpleCodec(MachineStirlingBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    // ---- Original Meta 1: Motor ohne Zahnrad (Item-Schaden 1) - im Port als Item-Tag ----
    // Item-Modell: ItemProperty hbm_m:no_cog (ClientSetup) bzw. AnimatedMachineItemRenderer (Teil "Cog" ausgeblendet).

    /** Item-Tag fuer Original-Meta 1 ({@code hasCog = false}). */
    public static final String TAG_NO_COG = "no_cog";

    /** Original {@code new ItemStack(machine_stirling, 1, 1)}. */
    public static ItemStack noCogStack(net.minecraft.world.level.ItemLike item) {
        ItemStack s = new ItemStack(item);
        StackNbt.orCreate(s).putBoolean(TAG_NO_COG, true);
        return s;
    }

    public static boolean isNoCog(ItemStack stack) {
        net.minecraft.nbt.CompoundTag t = StackNbt.read(stack);
        return t != null && t.getBoolean(TAG_NO_COG);
    }

    /** Original {@code onBlockPlacedBy}: Item-Schaden 1 setzt am Kern {@code hasCog = false}. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (isNoCog(stack) && level.getBlockEntity(pos) instanceof MachineStirlingBlockEntity stirling) {
            stirling.setHasCog(false);
        }
    }

    /** Original {@code getDrops}: ohne Zahnrad faellt Meta 1. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof MachineStirlingBlockEntity stirling && !stirling.hasCog()) {
            for (ItemStack s : drops) {
                if (s.is(asItem())) StackNbt.orCreate(s).putBoolean(TAG_NO_COG, true);
            }
        }
        return drops;
    }

    /** Original {@code printHook}: Waerme, Leistung, beim nicht-kreativen Motor Auslastung, Ueberdrehzahl, fehlendes Zahnrad. */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineStirlingBlockEntity stirling)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(stirling.getHeat() + "TU/t"));
        text.add(Component.literal((stirling.hasCog() ? stirling.getPowerBuffer() : 0) + "HE/t"));

        if (this != ModBlocks.STIRLING_CREATIVE.get()) {
            int maxHeat = stirling.renderMaxHeat();
            double percent = (double) stirling.getHeat() / (double) maxHeat;
            int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);

            if (percent > 1D)
                color = 0xff0000;

            final int c = color;
            text.add(Component.literal(((stirling.getHeat() * 1000 / maxHeat) / 10D) + "%").withStyle(s -> s.withColor(c)));

            if (stirling.getHeat() > maxHeat) {
                final int blink = com.hbm_m.util.BobMathUtil.getBlink() ? 0xff0000 : 0xffff00;
                text.add(Component.literal("! ! ! OVERSPEED ! ! !").withStyle(s -> s.withColor(blink)));
            }

            if (!stirling.hasCog()) {
                text.add(Component.literal("Gear missing!").withStyle(s -> s.withColor(0xff0000)));
            }
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    /** Original {@code addInformation}: {@code addStandardInfo} (Umschalttaste zeigt {@code .desc}). */
    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }
}
