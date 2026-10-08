package com.hbm_m.item.tool;

import com.hbm_m.platform.PlatformHooks;

import com.hbm_m.platform.AttributeOps;

import com.hbm_m.platform.ItemHooks;

import com.hbm_m.platform.StackNbt;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.hbm_m.api.item.IDepthRockTool;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.handler.EnumKeybind;
import com.hbm_m.handler.ability.AvailableAbilities;
import com.hbm_m.handler.ability.IBaseAbility;
import com.hbm_m.handler.ability.IToolAreaAbility;
import com.hbm_m.handler.ability.IToolHarvestAbility;
import com.hbm_m.handler.ability.ToolPreset;
import com.hbm_m.interfaces.IItemControlReceiver;
import com.hbm_m.interfaces.IKeybindReceiver;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.network.InfoToastPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 1:1 {@code com.hbm.items.tool.ItemToolAbility}: Werkzeug mit Flaechen- und Erntefaehigkeiten, Presets
 * (Rechtsklick-Taste schaltet weiter, Schleichen springt zum ersten, Alt oeffnet das Einstellungsfenster)
 * und Waffenfaehigkeiten. Abbaubare Bloecke kommen aus den 1.20-Blocktags statt aus den alten Materialien.
 */
public class ItemToolAbility extends TieredItem implements IDepthRockTool, IItemControlReceiver, IKeybindReceiver, ITooltipProvider {

    /** {@code Item.field_111210_e} */
    protected static final UUID ITEM_MODIFIER_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");

    protected boolean isShears = false;
    protected EnumToolType toolType;
    protected Rarity rarity = Rarity.COMMON;
    protected float damage;
    protected double movement;
    protected AvailableAbilities availableAbilities = new AvailableAbilities().addToolAbilities();
    protected boolean rockBreaker = false;

    public enum EnumToolType {
        PICKAXE,
        AXE,
        SHOVEL,
        MINER;

        /** Original: {@code materials} / {@code blocks} - in 1.20 die mineable-Tags der Werkzeugklassen. */
        public boolean isEffective(BlockState state) {
            return switch (this) {
                case PICKAXE -> state.is(BlockTags.MINEABLE_WITH_PICKAXE);
                case AXE -> state.is(BlockTags.MINEABLE_WITH_AXE);
                case SHOVEL -> state.is(BlockTags.MINEABLE_WITH_SHOVEL);
                case MINER -> state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
            };
        }
    }

    public ItemToolAbility setShears() {
        this.isShears = true;
        return this;
    }

    public ItemToolAbility(float damage, double movement, Tier material, EnumToolType type) {
        this(damage, movement, material, type, new Properties());
    }

    public ItemToolAbility(float damage, double movement, Tier material, EnumToolType type, Properties properties) {
        // unzerstoerbare Materialien (Haltbarkeit 0) wuerden sonst stapelbar
        this(damage, movement, material, type, material.getUses() == 0 ? properties.stacksTo(1) : properties, true);
    }

    /** Fuer Werkzeuge, die ihre Haltbarkeit selbst setzen (Akku, Treibstoff, {@code setMaxDamage}). */
    public ItemToolAbility(float damage, double movement, Tier material, EnumToolType type, Properties prepared, boolean preparedMarker) {
        super(material, prepared);
        this.damage = damage;
        this.movement = movement;
        this.toolType = type;
        //? if >= 1.21.1 {
        /*com.hbm_m.platform.ItemComponentHooks.deferRarity(this, () -> this.rarity != Rarity.COMMON ? this.rarity : null);
        *///?}
    }

    public ItemToolAbility addAbility(IBaseAbility ability, int level) {
        this.availableAbilities.addAbility(ability, level);
        return this;
    }

    public ItemToolAbility setDepthRockBreaker() {
        this.rockBreaker = true;
        return this;
    }

    // <insert obvious Rarity joke here>
    public ItemToolAbility setRarity(Rarity rarity) {
        this.rarity = rarity;
        return this;
    }

    public AvailableAbilities getAvailableAbilities() {
        return availableAbilities;
    }

    //? if < 1.21.1 {
    @Override
    public Rarity getRarity(ItemStack stack) {
        return this.rarity != Rarity.COMMON ? this.rarity : super.getRarity(stack);
    }
    //?}

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {

        if (!attacker.level().isClientSide && attacker instanceof Player player && canOperate(stack)) {
            this.availableAbilities.getWeaponAbilities().forEach((ability, level) ->
                    ability.onHit(level, attacker.level(), player, victim, this));
        }

        ItemHooks.hurtAndBreak(stack, 1, attacker, EquipmentSlot.MAINHAND);

        return true;
    }

    // Should be safe, considering the AoE ability does a similar trick already.
    public static BlockPos dropPos = BlockPos.ZERO;

    //? if forge {
    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
    //?} else {
    /*public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
    *///?}
        Level world = player.level();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        /*
         * Since keyholes aren't processable and exempt from silk touch anyway, we just default to the vanilla implementation in every case.
         */
        if (block == ModBlocks.STONE_KEYHOLE.get() || block == ModBlocks.STONE_KEYHOLE_META.get()) return false;

        if (!world.isClientSide && (canHarvestBlock(state, stack) || canShearBlock(state, stack, world, pos)) && canOperate(stack)) {
            Configuration config = getConfiguration(stack);
            ToolPreset preset = config.getActivePreset();

            dropPos = pos.immutable();

            preset.harvestAbility.preHarvestAll(preset.harvestAbilityLevel, world, player);

            boolean skipRef = preset.areaAbility.onDig(preset.areaAbilityLevel, world, pos, player, this);

            if (!skipRef) {
                breakExtraBlock(world, pos, player, pos);
            }

            preset.harvestAbility.postHarvestAll(preset.harvestAbilityLevel, world, player);

            return true;
        }

        return false;
    }

    /** Original {@code ItemTool.onBlockDestroyed}: 1 Haltbarkeit pro Block mit Haerte. */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) {
            ItemHooks.hurtAndBreak(stack, 1, entity, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {

        if (!canOperate(stack))
            return 1;

        if (toolType == null)
            return 1.0F;

        if (toolType.isEffective(state))
            return this.getTier().getSpeed();

        return 1.0F;
    }

    public boolean canOperate(ItemStack stack) {
        return true;
    }

    /** Original {@code canHarvestBlock(Block, ItemStack)}. */
    public boolean canHarvestBlock(BlockState state, ItemStack stack) {

        if (!canOperate(stack))
            return false;

        if (this.getConfiguration(stack).getActivePreset().harvestAbility == IToolHarvestAbility.SILK)
            return true;

        return getDestroySpeed(stack, state) > 1;
    }

    /** Die Abbaustufen-Pruefung von {@code ForgeHooks.canHarvestBlock}. */
    //? if forge {
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (!canOperate(stack)) return false;
        if (toolType == null || !toolType.isEffective(state)) return false;
        return net.minecraftforge.common.TierSortingRegistry.isCorrectTierForDrops(getTier(), state);
    }
    //?} else {
    /*@Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (!canOperate(stack)) return false;
        if (toolType == null || !toolType.isEffective(state)) return false;
        // 1.21.1: Abbaustufe ueber das Tag der nicht abbaubaren Bloecke des Materials
        return !state.is(getTier().getIncorrectBlocksForDrops());
    }
    *///?}

    @Override
    public boolean canBreakRock(BlockGetter world, Player player, ItemStack tool, BlockState block, BlockPos pos) {
        return canOperate(tool) && this.rockBreaker;
    }

    public boolean canShearBlock(BlockState state, ItemStack stack, Level world, BlockPos pos) {
        //? if forge {
        return this.isShears(stack) && state.getBlock() instanceof net.minecraftforge.common.IForgeShearable s && s.isShearable(stack, world, pos);
        //?} else {
        /*return this.isShears(stack) && state.getBlock() instanceof net.neoforged.neoforge.common.IShearable s && s.isShearable(null, stack, world, pos);
        *///?}
    }

    public boolean isShears(ItemStack stack) {
        return this.isShears;
    }

    @Override
    @SuppressWarnings("deprecation")
    //? if < 1.21.1 {
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) return super.getDefaultAttributeModifiers(slot);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
    //?} else {
    /*public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return com.hbm_m.platform.AttributeHooks.fromSlots(this::hbmSlotModifiers);
    }

    private Multimap<net.minecraft.core.Holder<Attribute>, AttributeModifier> hbmSlotModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) return ImmutableMultimap.of();
        ImmutableMultimap.Builder<net.minecraft.core.Holder<Attribute>, AttributeModifier> builder = ImmutableMultimap.builder();
    *///?}
        builder.put(Attributes.ATTACK_DAMAGE, com.hbm_m.platform.AttributeHooks.modifier(ITEM_MODIFIER_UUID, "Tool modifier", this.damage, AttributeOps.ADDITION));
        builder.put(Attributes.MOVEMENT_SPEED, com.hbm_m.platform.AttributeHooks.modifier(ITEM_MODIFIER_UUID, "Tool modifier", movement, AttributeOps.MULTIPLY_BASE));
        return builder.build();
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.isEnchanted() || !getConfiguration(stack).getActivePreset().isNone();
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {

        availableAbilities.addInformation(list);

        if (this.rockBreaker) {
            list.add(Component.literal(""));
            list.add(Component.literal("Can break depth rock!").withStyle(ChatFormatting.RED));
        }
    }

    public void breakExtraBlock(Level world, BlockPos pos, Player playerEntity, BlockPos ref) {

        if (world.isEmptyBlock(pos))
            return;

        if (!(playerEntity instanceof ServerPlayer player))
            return;

        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            return;
        }

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (!(canHarvestBlock(state, stack) || canShearBlock(state, stack, world, pos)) ||
                (state.getDestroySpeed(world, pos) == -1.0F && state.getDestroyProgress(player, world, pos) == 0.0F) ||
                block == ModBlocks.STONE_KEYHOLE.get()) return;

        BlockState refState = world.getBlockState(ref);
        float refStrength = refState.getDestroyProgress(player, world, ref);
        float strength = state.getDestroyProgress(player, world, pos);

        if (
            !canHarvestWithHand(state, world, pos, player) ||
            refStrength / strength > 10f ||
            refStrength < 0
        )
            return;

        //? if forge {
        if (net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(world, player.gameMode.getGameModeForPlayer(), player, pos) == -1)
            return;
        //?} else {
        /*if (net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(world, player.gameMode.getGameModeForPlayer(), player, pos, state).isCanceled())
            return;
        *///?}

        Configuration config = getConfiguration(stack);
        ToolPreset preset = config.getActivePreset();

        preset.harvestAbility.onHarvestBlock(preset.harvestAbilityLevel, world, pos, player, state);
    }

    /** {@code ForgeHooks.canHarvestBlock(block, player, meta)} */
    private static boolean canHarvestWithHand(BlockState state, Level world, BlockPos pos, Player player) {
        //? if forge {
        return state.canHarvestBlock(world, pos, player);
        //?} else {
        /*return state.canHarvestBlock(world, pos, player);
        *///?}
    }

    /** Assumes a canShearBlock check has passed, will most likely crash otherwise! */
    public static void shearBlock(Level world, BlockPos pos, BlockState state, Player player) {

        ItemStack held = player.getMainHandItem();

        //? if forge {
        net.minecraftforge.common.IForgeShearable target = (net.minecraftforge.common.IForgeShearable) state.getBlock();
        //?} else {
        /*net.neoforged.neoforge.common.IShearable target = (net.neoforged.neoforge.common.IShearable) state.getBlock();
        *///?}
        //? if < 1.21.1 {
        if (target.isShearable(held, world, pos)) {
            List<ItemStack> drops = target.onSheared(player, held, world, pos, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, held));
        //?} else {
        /*if (target.isShearable(player, held, world, pos)) {
            List<ItemStack> drops = target.onSheared(player, held, world, pos);
        *///?}
            Random rand = new Random();

            for (ItemStack stack : drops) {
                float f = 0.7F;
                double d = (double) (rand.nextFloat() * f) + (double) (1.0F - f) * 0.5D;
                double d1 = (double) (rand.nextFloat() * f) + (double) (1.0F - f) * 0.5D;
                double d2 = (double) (rand.nextFloat() * f) + (double) (1.0F - f) * 0.5D;
                ItemEntity entityitem = new ItemEntity(world, dropPos.getX() + d, dropPos.getY() + d1, dropPos.getZ() + d2, stack);
                entityitem.setPickUpDelay(10);
                world.addFreshEntity(entityitem);
            }

            ItemHooks.hurtAndBreak(held, 1, player, EquipmentSlot.MAINHAND);
            player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
        }
    }

    /**
     * Original {@code standardDigPost}: bricht den Block wie ein Spieler, faengt die Drops ab und legt sie am
     * Referenzblock ({@link #dropPos}) ab. Erfahrung faellt wie im Original keine.
     */
    public static void standardDigPost(Level world, BlockPos pos, ServerPlayer player) {

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        world.levelEvent(player, 2001, pos, Block.getId(state));
        boolean removedByPlayer;

        if (player.isCreative()) {
            removeBlock(world, pos, false, player);
            player.connection.send(new ClientboundBlockUpdatePacket(world, pos));
        } else {
            ItemStack itemstack = player.getMainHandItem();
            ItemStack toolCopy = itemstack.copy();
            boolean canHarvest = canHarvestWithHand(state, world, pos, player);
            BlockEntity be = world.getBlockEntity(pos);

            removedByPlayer = removeBlock(world, pos, canHarvest, player);

            if (!itemstack.isEmpty()) {
                itemstack.mineBlock(world, state, pos, player);

                if (itemstack.isEmpty()) {
                    //? if forge {
                    net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, toolCopy, InteractionHand.MAIN_HAND);
                    //?} elif neoforge {
                    /*net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem(player, toolCopy, InteractionHand.MAIN_HAND);
                    *///?}
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }

            if (removedByPlayer && canHarvest) {
                // captureDrops des Originals: neue Gegenstaende am Block einsammeln und zum Referenzblock verlegen
                AABB box = new AABB(pos).inflate(1.0D);
                List<ItemEntity> before = world.getEntitiesOfClass(ItemEntity.class, box);
                block.playerDestroy(world, player, pos, state, be, toolCopy);
                if (!pos.equals(dropPos)) {
                    for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, box)) {
                        if (before.contains(item)) continue;
                        item.setPos(dropPos.getX() + (item.getX() - pos.getX()), dropPos.getY() + (item.getY() - pos.getY()), dropPos.getZ() + (item.getZ() - pos.getZ()));
                    }
                }
            }
        }
    }

    public static boolean removeBlock(Level world, BlockPos pos, boolean canHarvest, Player player) {
        BlockState state = world.getBlockState(pos);
        state.getBlock().playerWillDestroy(world, pos, state, player);
        boolean flag = state.onDestroyedByPlayer(world, pos, player, canHarvest, world.getFluidState(pos));

        if (flag) {
            state.getBlock().destroy(world, pos, state);
        }

        return flag;
    }

    public static class Configuration {
        public List<ToolPreset> presets;
        public int currentPreset;

        public Configuration() {
            this.presets = null;
            this.currentPreset = 0;
        }

        public Configuration(List<ToolPreset> presets, int currentPreset) {
            this.presets = presets;
            this.currentPreset = currentPreset;
        }

        public void writeToNBT(CompoundTag nbt) {
            nbt.putInt("ability", currentPreset);

            ListTag nbtPresets = new ListTag();

            for (ToolPreset preset : presets) {
                CompoundTag nbtPreset = new CompoundTag();
                preset.writeToNBT(nbtPreset);
                nbtPresets.add(nbtPreset);
            }

            nbt.put("abilityPresets", nbtPresets);
        }

        public void readFromNBT(CompoundTag nbt) {
            currentPreset = nbt.getInt("ability");

            ListTag nbtPresets = nbt.getList("abilityPresets", 10);
            int numPresets = Math.min(nbtPresets.size(), 99);

            presets = new ArrayList<>(numPresets);

            for (int i = 0; i < numPresets; i++) {
                CompoundTag nbtPreset = nbtPresets.getCompound(i);
                ToolPreset preset = new ToolPreset();
                preset.readFromNBT(nbtPreset);
                presets.add(preset);
            }

            currentPreset = Math.max(0, Math.min(currentPreset, presets.size() - 1));
        }

        public void reset(AvailableAbilities availableAbilities) {
            currentPreset = 0;

            presets = new ArrayList<>(availableAbilities.size());
            presets.add(new ToolPreset());

            availableAbilities.getToolAreaAbilities().forEach((ability, level) -> {
                if (ability == IToolAreaAbility.NONE)
                    return;
                presets.add(new ToolPreset(ability, level, IToolHarvestAbility.NONE, 0));
            });

            availableAbilities.getToolHarvestAbilities().forEach((ability, level) -> {
                if (ability == IToolHarvestAbility.NONE)
                    return;
                presets.add(new ToolPreset(IToolAreaAbility.NONE, 0, ability, level));
            });

            presets.sort(
                Comparator
                    .comparing((ToolPreset p) -> p.harvestAbility)
                    .thenComparingInt(p -> p.harvestAbilityLevel)
                    .thenComparing(p -> p.areaAbility)
                    .thenComparingInt(p -> p.areaAbilityLevel)
            );
        }

        public void restrictTo(AvailableAbilities availableAbilities) {
            for (ToolPreset preset : presets) {
                preset.restrictTo(availableAbilities);
            }
        }

        public ToolPreset getActivePreset() {
            return presets.get(currentPreset);
        }
    }

    public Configuration getConfiguration(ItemStack stack) {
        Configuration config = new Configuration();

        if (stack == null || !StackNbt.has(stack) || !StackNbt.read(stack).contains("ability") || !StackNbt.read(stack).contains("abilityPresets")) {
            config.reset(availableAbilities);
            return config;
        }

        config.readFromNBT(StackNbt.tag(stack));
        config.restrictTo(availableAbilities);
        return config;
    }

    public void setConfiguration(ItemStack stack, Configuration config) {
        if (stack == null) {
            return;
        }

        config.writeToNBT(StackNbt.orCreate(stack));
    }

    @Override
    public void receiveControl(ItemStack stack, CompoundTag data) {
        Configuration config = new Configuration();
        config.readFromNBT(data);
        config.restrictTo(availableAbilities);
        setConfiguration(stack, config);
    }

    @Override
    public boolean canHandleKeybind(Player player, ItemStack stack, EnumKeybind keybind) {
        if (player.level().isClientSide) return keybind == EnumKeybind.ABILITY_ALT;
        return keybind == EnumKeybind.ABILITY_CYCLE;
    }

    @Override
    public void handleKeybind(Player player, ItemStack stack, EnumKeybind keybind, boolean state) {

        if (keybind == EnumKeybind.ABILITY_CYCLE && state) {

            Level world = player.level();
            if (!canOperate(stack)) return;
            if (!(player instanceof ServerPlayer playerMP)) return;

            double reach = 4.5D;
            //? if forge {
            reach = playerMP.getBlockReach();
            //?} elif >= 1.21.1 {
            /*reach = playerMP.blockInteractionRange();
            *///?}
            HitResult mop = playerMP.pick(reach, 1.0F, false);

            if (mop instanceof BlockHitResult bhr && mop.getType() == HitResult.Type.BLOCK) {
                // simply put, we check if we are aiming at a block, and if we do, we compare that block's use
                // method to the one from BlockBehaviour. If the declaring class doesn't match, it's overridden, and we
                // assume that something is going to happen, which it might not, but either way we cancel the ability switch.
                Block b = world.getBlockState(bhr.getBlockPos()).getBlock();
                if (overridesUse(b, "use") || overridesUse(b, "m_6227_")) return;
            }

            Configuration config = getConfiguration(stack);
            if (config.presets.size() < 2 || world.isClientSide) return;

            if (player.isShiftKeyDown()) {
                config.currentPreset = 0;
            } else {
                config.currentPreset = (config.currentPreset + 1) % config.presets.size();
            }

            setConfiguration(stack, config);
            InfoToastPacket.sendTo(playerMP, config.getActivePreset().getMessage(), 20, InfoToastPacket.ID_TOOLABILITY, 0xFFFFFF);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                    0.25F, config.getActivePreset().isNone() ? 0.75F : 1.25F);
        }
    }

    private static boolean overridesUse(Block b, String name) {
        try {
            Class<?>[] params = { BlockState.class, Level.class, BlockPos.class, Player.class, InteractionHand.class, BlockHitResult.class };
            Method m0 = b.getClass().getMethod(name, params);
            Method m1 = BlockBehaviour.class.getMethod(name, params);
            return m0.getDeclaringClass() != m1.getDeclaringClass();
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public void handleKeybindClient(Player player, ItemStack stack, EnumKeybind keybind, boolean state) {
        if (state) com.hbm_m.client.ToolAbilityClient.openGui(this.availableAbilities);
    }

    /** Original {@code abilityGui}: Symbol der Flaechenfaehigkeit neben dem Fadenkreuz (Textur gui_tool_ability). */
    public static final Map<IBaseAbility, int[]> abilityGui = new HashMap<>();

    static {
        abilityGui.put(IToolAreaAbility.RECURSION, new int[] { 0, 138 });
        abilityGui.put(IToolAreaAbility.HAMMER, new int[] { 16, 138 });
        abilityGui.put(IToolAreaAbility.HAMMER_FLAT, new int[] { 32, 138 });
        abilityGui.put(IToolAreaAbility.EXPLOSION, new int[] { 48, 138 });
    }
}
