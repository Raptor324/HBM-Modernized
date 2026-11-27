plugins {
	alias(libs.plugins.stonecutter)
	alias(libs.plugins.dotenv)
	alias(libs.plugins.neoforged.moddev).apply(false)
	alias(libs.plugins.jsonlang.postprocess).apply(false)
	alias(libs.plugins.mod.publish.plugin).apply(false)
	alias(libs.plugins.kotlin.jvm).apply(false)
	alias(libs.plugins.devtools.ksp).apply(false)
	alias(libs.plugins.fletching.table).apply(false)
	alias(libs.plugins.legacyforge.moddev).apply(false)
}

stonecutter active file(".sc_active_version")

for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
	group = "publishing"
	dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
}

stonecutter tasks {
	val ordering = versionComparator

	listOf("publishModrinth", "publishCurseforge").forEach { taskName ->
		gradle.allprojects {
			if (project.tasks.findByName(taskName) != null) {
				order(taskName, ordering)
			}
		}
	}
}

stonecutter parameters {
	constants.match(node.metadata.project.substringAfterLast('-'), "neoforge", "forge")
	filters.include("**/*.fsh", "**/*.vsh")
	swaps["mod_version"] = "\"" + property("mod.version") + "\";"
	swaps["mod_id"] = "\"" + property("mod.id") + "\";"
	swaps["mod_name"] = "\"" + property("mod.name") + "\";"
	swaps["mod_group"] = "\"" + property("mod.group") + "\";"
	swaps["minecraft"] = "\"" + node.metadata.version + "\";"
	constants["release"] = property("mod.id") != "modtemplate"

	val isNeoForge121 = current.project.endsWith("neoforge") && eval(current.version, ">=1.21.1")
	val is26 = current.project.endsWith("neoforge") && eval(current.version, ">=26.1")

	replacements.string(isNeoForge121, "!loader_packages") {
		replace("net.minecraftforge.api.distmarker.Dist", "net.neoforged.api.distmarker.Dist")
		replace("net.minecraftforge.api.distmarker.OnlyIn", "net.neoforged.api.distmarker.OnlyIn")
		replace("net.minecraftforge.client.extensions.common.IClientMobEffectExtensions", "net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions")
		replace("net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions", "net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions")
		replace("net.minecraftforge.fml.loading.FMLEnvironment", "net.neoforged.fml.loading.FMLEnvironment")
		replace("net.minecraftforge.client.model.geometry.IGeometryBakingContext", "net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext")
		replace("net.minecraftforge.client.model.obj.ObjLoader", "net.neoforged.neoforge.client.model.obj.ObjLoader")
		replace("net.minecraftforge.client.model.obj.ObjModel", "net.neoforged.neoforge.client.model.obj.ObjModel")
		// model data / wrappers
		replace("net.minecraftforge.client.model.data.ModelData", "net.neoforged.neoforge.client.model.data.ModelData")
		replace("net.minecraftforge.client.model.data.ModelProperty", "net.neoforged.neoforge.client.model.data.ModelProperty")
		replace("net.minecraftforge.client.model.BakedModelWrapper", "net.neoforged.neoforge.client.model.BakedModelWrapper")
		replace("net.minecraftforge.client.ChunkRenderTypeSet", "net.neoforged.neoforge.client.ChunkRenderTypeSet")
		replace("net.minecraftforge.client.RenderTypeHelper", "net.neoforged.neoforge.client.RenderTypeHelper")
		replace("net.minecraftforge.client.model.geometry.IUnbakedGeometry", "net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry")
		replace("net.minecraftforge.client.model.geometry.IGeometryLoader", "net.neoforged.neoforge.client.model.geometry.IGeometryLoader")
		// fluids
		replace("net.minecraftforge.fluids.FluidStack", "net.neoforged.neoforge.fluids.FluidStack")
		if (!is26) replace("net.minecraftforge.fluids.capability.IFluidHandler", "net.neoforged.neoforge.fluids.capability.IFluidHandler")
		if (!is26) replace("net.minecraftforge.fluids.capability.FluidAction", "net.neoforged.neoforge.fluids.capability.FluidAction")
		if (!is26) replace("net.minecraftforge.fluids.capability.templates.FluidTank", "net.neoforged.neoforge.fluids.capability.templates.FluidTank")
		// items / energy
		if (!is26) replace("net.minecraftforge.items.IItemHandler", "net.neoforged.neoforge.items.IItemHandler")
		if (!is26) replace("net.minecraftforge.items.SlotItemHandler", "net.neoforged.neoforge.items.SlotItemHandler")
		if (!is26) replace("net.minecraftforge.energy.IEnergyStorage", "net.neoforged.neoforge.energy.IEnergyStorage")
		// gametest
		replace("net.minecraftforge.gametest.GameTestHolder", "net.neoforged.neoforge.gametest.GameTestHolder")
		replace("net.minecraftforge.gametest.PrefixGameTestTemplate", "net.neoforged.neoforge.gametest.PrefixGameTestTemplate")
		replace("net.minecraftforge.gametest.GameTest", "net.neoforged.neoforge.gametest.GameTest")
		// client events (same simple class names on both loaders)
		replace("net.minecraftforge.client.event.InputEvent", "net.neoforged.neoforge.client.event.InputEvent")
		replace("net.minecraftforge.client.event.RenderHandEvent", "net.neoforged.neoforge.client.event.RenderHandEvent")
		replace("net.minecraftforge.client.event.RenderLevelStageEvent", "net.neoforged.neoforge.client.event.RenderLevelStageEvent")
		replace("net.minecraftforge.event.BuildCreativeModeTabContentsEvent;", "net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;")
		// event bus (NeoForge moved the package, class names unchanged)
		replace("net.minecraftforge.eventbus.api.EventPriority", "net.neoforged.bus.api.EventPriority")
		replace("net.minecraftforge.eventbus.api.SubscribeEvent", "net.neoforged.bus.api.SubscribeEvent")
	}

	// ===== Minecraft 26.x (NeoForge 26.3) =====
	// Reine Umbenennungen ohne API-Änderung; alles Weitere läuft über //? if >=26.1 Blöcke.
	// ResourceLocation -> Identifier (gleiche Methoden: fromNamespaceAndPath, parse, withDefaultNamespace, ...)
	// Nur verlustfreie Regeln: Rückrichtung muss den alten Code exakt wiederherstellen ODER
	// das Vorwärts-Ergebnis muss auch auf 1.20.1/1.21.1 kompilieren (dann Rückrichtung "(?!)" = nie).
	replacements.string(is26, "!mc26_packages") {
		replace("net.minecraft.client.gui.GuiGraphics;", "net.minecraft.client.gui.GuiGraphicsExtractor;")
		replace("net.minecraft.client.renderer.block.model.BakedQuad;", "net.minecraft.client.resources.model.geometry.BakedQuad;")
		replace("net.minecraft.client.renderer.RenderType;", "net.minecraft.client.renderer.rendertype.RenderType;")
		replace("com.mojang.blaze3d.platform.GlStateManager;", "com.mojang.renderpearl.backend.opengl.GlStateManager;")
		replace("net.minecraft.world.level.block.state.properties.DirectionProperty;", "net.minecraft.world.level.block.state.properties.EnumProperty ;")
		replace("RenderShape.ENTITYBLOCK_ANIMATED", "RenderShape .INVISIBLE")
		replace(".noCollission()", ".noCollision()")
	}
	// NeoForge 26 hat IItemHandler/IFluidHandler/IEnergyStorage entfernt (Transfer API).
	// Der Mod nutzt intern weiter die alten Schnittstellen aus com.hbm_m.platform.legacy;
	// die Brücke zu ResourceHandler/EnergyHandler sitzt an den Capability-Registrierungen.
	replacements.string(is26, "!mc26_legacy_handlers") {
		replace("net.neoforged.neoforge.items.", "com.hbm_m.platform.legacy.items.")
		replace("net.neoforged.neoforge.fluids.capability.", "com.hbm_m.platform.legacy.fluids.capability.")
		replace("net.neoforged.neoforge.fluids.IFluidTank", "com.hbm_m.platform.legacy.fluids.IFluidTank")
		replace("net.neoforged.neoforge.fluids.FluidActionResult", "com.hbm_m.platform.legacy.fluids.FluidActionResult")
		replace("net.neoforged.neoforge.energy.", "com.hbm_m.platform.legacy.energy.")
	}
	// Modellsystem „nur NMesh": BakedModel-Typen auf die Shims in platform/legacy/client/model,
	// Modell-Lookups auf NMeshPartRegistry (siehe docs/porting/26.3-neoforge-port.md).
	replacements.string(is26, "!mc26_bakedmodel") {
		replace("net.minecraft.client.resources.model.BakedModel", "com.hbm_m.platform.legacy.client.model .BakedModel")
		replace("com.hbm_m.client.model.ConfiguredMultipartBakedModel", "com.hbm_m.platform.legacy.client.model .ConfiguredMultipartBakedModel")
		replace("com.hbm_m.client.model.AbstractMultipartBakedModel", "com.hbm_m.platform.legacy.client.model .AbstractMultipartBakedModel")
		replace("com.hbm_m.client.model.MissileBakedModel", "com.hbm_m.platform.legacy.client.model .MissileBakedModel")
		replace("com.hbm_m.client.model.SoyuzRocketBakedModel", "com.hbm_m.platform.legacy.client.model .SoyuzRocketBakedModel")
		replace("com.hbm_m.client.model.DoorBakedModel", "com.hbm_m.platform.legacy.client.model .DoorBakedModel")
		replace("com.hbm_m.client.render.weapon.WeaponMultipartBakedModel", "com.hbm_m.platform.legacy.client.model .WeaponMultipartBakedModel")
		replace("net.minecraft.client.renderer.block.model.ItemTransforms;", "net.minecraft.client.resources.model.cuboid.ItemTransforms;")
		replace("net.minecraft.client.renderer.block.model.ItemTransform;", "net.minecraft.client.resources.model.cuboid.ItemTransform;")
		replace("Transformation.identity()", "Transformation.IDENTITY")
		replace("new BakedQuad(", "com.hbm_m.platform.legacy.client.model.LegacyQuads.of(")
		// VertexFormat/-Element liegen in 26.x in renderpearl.
		replace("com.mojang.blaze3d.vertex.VertexFormat;", "com.mojang.renderpearl.api.vertex.VertexFormat;")
		replace("com.mojang.blaze3d.vertex.VertexFormatElement;", "com.mojang.renderpearl.api.vertex.VertexFormatElement;")
		replace("com.mojang.blaze3d.vertex.VertexFormat.", "com.mojang.renderpearl.api.vertex.VertexFormat.")
		replace("Direction.getNearest(", "Direction.getApproximateNearest(")
		replace("import net.minecraft.client.resources.model.Material;", "import com.hbm_m.platform.legacy.client.model .Material;")
		replace("net.minecraft.client.resources.model.ModelResourceLocation", "com.hbm_m.platform.legacy.client.model .ModelResourceLocation")
		replace("com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat", "com.hbm_m.platform.legacy.client.model .ItemTransformHelperCompat")
	}
	replacements.regex(is26, "!mc26_getblockmodel") {
		replace("""(?:Minecraft\.getInstance\(\)|this\.minecraft|minecraft|mc)\.getBlockRenderer\(\)\.getBlockModel\(""", """com.hbm_m.platform.legacy.client.model.NMeshPartRegistry.blockModel(""", """com\.hbm_m\.platform\.legacy\.client\.model\.NMeshPartRegistry\.blockModel\(""", """Minecraft.getInstance().getBlockRenderer().getBlockModel(""")
	}
	replacements.string(is26, "!mc26_legacy_caps") {
		replace("Capabilities.ItemHandler.BLOCK", "com.hbm_m.platform.legacy.LegacyCaps.ITEM_HANDLER_BLOCK")
		replace("Capabilities.ItemHandler.ITEM", "com.hbm_m.platform.legacy.LegacyCaps.ITEM_HANDLER_ITEM")
		replace("Capabilities.ItemHandler.ENTITY", "com.hbm_m.platform.legacy.LegacyCaps.ITEM_HANDLER_ENTITY")
		replace("Capabilities.FluidHandler.BLOCK", "com.hbm_m.platform.legacy.LegacyCaps.FLUID_HANDLER_BLOCK")
		replace("Capabilities.FluidHandler.ITEM", "com.hbm_m.platform.legacy.LegacyCaps.FLUID_HANDLER_ITEM")
		replace("Capabilities.EnergyStorage.BLOCK", "com.hbm_m.platform.legacy.LegacyCaps.ENERGY_BLOCK")
		replace("Capabilities.EnergyStorage.ITEM", "com.hbm_m.platform.legacy.LegacyCaps.ENERGY_ITEM")
	}
	// InteractionResultHolder / ItemInteractionResult (entfernt in 1.21.2) -> InteractionResult.
	replacements.regex(is26, "!mc26_irh_type") {
		replace("""(?<![\w.])InteractionResultHolder<ItemStack>""", """net.minecraft.world .InteractionResult""", """\bnet\.minecraft\.world \.InteractionResult\b""", """InteractionResultHolder<ItemStack>""")
	}
	replacements.regex(is26, "!mc26_irh_type_fq") {
		replace("""\bnet\.minecraft\.world\.InteractionResultHolder<(?:net\.minecraft\.world\.item\.)?ItemStack>""", """net.minecraft.world.  InteractionResult""", """\bnet\.minecraft\.world\.  InteractionResult\b""", """net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack>""")
	}
	replacements.regex(is26, "!mc26_irh_call") {
		replace("""(?<![\w.])InteractionResultHolder\.(pass|success|sidedSuccess|fail|consume)\(""", "com.hbm_m.platform.legacy.InteractionCompat.holder_$1(", """\bcom\.hbm_m\.platform\.legacy\.InteractionCompat\.holder_(\w+)\(""", "InteractionResultHolder.$1(")
	}
	replacements.regex(is26, "!mc26_irh_call_fq") {
		replace("""\bnet\.minecraft\.world\.InteractionResultHolder\.(pass|success|sidedSuccess|fail|consume)\(""", """com.hbm_m.platform.legacy.InteractionCompat.fq_holder_$1(""", """\bcom\.hbm_m\.platform\.legacy\.InteractionCompat\.fq_holder_(\w+)\(""", """net.minecraft.world.InteractionResultHolder.$1(""")
	}
	replacements.regex(is26, "!mc26_irh_bare") {
		replace("""(?<![\w.])InteractionResultHolder\b(?![<.])""", """net.minecraft .world.InteractionResult""", """\bnet\.minecraft \.world\.InteractionResult\b""", """InteractionResultHolder""")
	}
	replacements.regex(is26, "!mc26_irh_bare_fq") {
		replace("""\bnet\.minecraft\.world\.InteractionResultHolder\b(?![<.])""", """net.minecraft. world.InteractionResult""", """\bnet\.minecraft\. world\.InteractionResult\b""", """net.minecraft.world.InteractionResultHolder""")
	}
	replacements.regex(is26, "!mc26_ir_sided") {
		replace("""\bInteractionResult\.sidedSuccess\(""", "com.hbm_m.platform.legacy.InteractionCompat.sidedSuccess(", """\bcom\.hbm_m\.platform\.legacy\.InteractionCompat\.sidedSuccess\(""", "InteractionResult.sidedSuccess(")
	}
	replacements.regex(is26, "!mc26_iir_sided") {
		replace("""\bItemInteractionResult\.sidedSuccess\(""", "com.hbm_m.platform.legacy.InteractionCompat.itemSidedSuccess(", """\bcom\.hbm_m\.platform\.legacy\.InteractionCompat\.itemSidedSuccess\(""", "ItemInteractionResult.sidedSuccess(")
	}
	replacements.regex(is26, "!mc26_iir_const") {
		replace("""(?<![\w.])ItemInteractionResult\.(SUCCESS|CONSUME|FAIL)\b""", """net .minecraft.world.InteractionResult.$1""", """\bnet \.minecraft\.world\.InteractionResult\.(SUCCESS|CONSUME|FAIL)\b""", """ItemInteractionResult.$1""")
	}
	replacements.regex(is26, "!mc26_iir_const_fq") {
		replace("""\bnet\.minecraft\.world\.ItemInteractionResult\.(SUCCESS|CONSUME|FAIL)\b""", """net. minecraft.world.InteractionResult.$1""", """\bnet\. minecraft\.world\.InteractionResult\.(SUCCESS|CONSUME|FAIL)\b""", """net.minecraft.world.ItemInteractionResult.$1""")
	}
	replacements.regex(is26, "!mc26_iir_default") {
		replace("""(?<![\w.])ItemInteractionResult\.PASS_TO_DEFAULT_BLOCK_INTERACTION\b""", """net .minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND""", """\bnet \.minecraft\.world\.InteractionResult\.TRY_WITH_EMPTY_HAND\b""", """ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION""")
	}
	replacements.regex(is26, "!mc26_iir_default_fq") {
		replace("""\bnet\.minecraft\.world\.ItemInteractionResult\.PASS_TO_DEFAULT_BLOCK_INTERACTION\b""", """net. minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND""", """\bnet\. minecraft\.world\.InteractionResult\.TRY_WITH_EMPTY_HAND\b""", """net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION""")
	}
	replacements.regex(is26, "!mc26_iir_partial") {
		replace("""(?<![\w.])ItemInteractionResult\.CONSUME_PARTIAL\b""", """net.minecraft.world. InteractionResult.CONSUME""", """\bnet\.minecraft\.world\. InteractionResult\.CONSUME\b""", """ItemInteractionResult.CONSUME_PARTIAL""")
	}
	replacements.regex(is26, "!mc26_iir_skip") {
		replace("""(?<![\w.])ItemInteractionResult\.SKIP_DEFAULT_BLOCK_INTERACTION\b""", """net.minecraft.world. InteractionResult.PASS""", """\bnet\.minecraft\.world\. InteractionResult\.PASS\b""", """ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION""")
	}
	replacements.regex(is26, "!mc26_iir_type") {
		replace("""(?<![\w.])ItemInteractionResult\b(?!\.)""", """net .minecraft.world.InteractionResult""", """\bnet \.minecraft\.world\.InteractionResult\b(?!\.)""", """ItemInteractionResult""")
	}
	replacements.regex(is26, "!mc26_iir_type_fq") {
		replace("""\bnet\.minecraft\.world\.ItemInteractionResult\b(?!\.)""", """net. minecraft.world.InteractionResult""", """\bnet\. minecraft\.world\.InteractionResult\b(?!\.)""", """net.minecraft.world.ItemInteractionResult""")
	}
	// CompoundTag/ListTag-Getter liefern ab 1.21.5 Optional -> ...Or(key, default)-Varianten.
	replacements.regex(is26, "!mc26_nbt_int") {
		replace("""\.getInt\(([^(),;"]*"[^"]*"[^(),;]*|[A-Z][A-Z0-9_]+(?:\s*\+[^(),;]+)?|\w+\.[A-Z][A-Z0-9_]+|key|name|tagName|nbtKey)\)""", """.getIntOr($1, 0)""", """\.getIntOr\(([^(),;]+), 0\)""", """.getInt($1)""")
	}
	replacements.regex(is26, "!mc26_nbt_long") {
		replace("""\.getLong\(([^(),;"]*"[^"]*"[^(),;]*|[A-Z][A-Z0-9_]+(?:\s*\+[^(),;]+)?|\w+\.[A-Z][A-Z0-9_]+|key|name|tagName|nbtKey)\)""", """.getLongOr($1, 0L)""", """\.getLongOr\(([^(),;]+), 0L\)""", """.getLong($1)""")
	}
	replacements.regex(is26, "!mc26_nbt_double") {
		replace("""\.getDouble\(([^(),;"]*"[^"]*"[^(),;]*|[A-Z][A-Z0-9_]+(?:\s*\+[^(),;]+)?|\w+\.[A-Z][A-Z0-9_]+|key|name|tagName|nbtKey)\)""", """.getDoubleOr($1, 0.0D)""", """\.getDoubleOr\(([^(),;]+), 0\.0D\)""", """.getDouble($1)""")
	}
	replacements.regex(is26, "!mc26_nbt_float") {
		replace("""\.getFloat\(([^(),;"]*"[^"]*"[^(),;]*|[A-Z][A-Z0-9_]+(?:\s*\+[^(),;]+)?|\w+\.[A-Z][A-Z0-9_]+|key|name|tagName|nbtKey)\)""", """.getFloatOr($1, 0.0F)""", """\.getFloatOr\(([^(),;]+), 0\.0F\)""", """.getFloat($1)""")
	}
	replacements.regex(is26, "!mc26_nbt_short") {
		replace("""\.getShort\(([^(),;"]*"[^"]*"[^(),;]*|[A-Z][A-Z0-9_]+(?:\s*\+[^(),;]+)?|\w+\.[A-Z][A-Z0-9_]+|key|name|tagName|nbtKey)\)""", """.getShortOr($1, (short) 0)""", """\.getShortOr\(([^(),;]+), \(short\) 0\)""", """.getShort($1)""")
	}
	replacements.regex(is26, "!mc26_nbt_byte") {
		replace("""\.getByte\(([^(),;"]*"[^"]*"[^(),;]*|[A-Z][A-Z0-9_]+(?:\s*\+[^(),;]+)?|\w+\.[A-Z][A-Z0-9_]+|key|name|tagName|nbtKey)\)""", """.getByteOr($1, (byte) 0)""", """\.getByteOr\(([^(),;]+), \(byte\) 0\)""", """.getByte($1)""")
	}
	replacements.regex(is26, "!mc26_nbt_boolean") {
		replace("""\.getBoolean\(([^(),;"]*"[^"]*"[^(),;]*|[A-Z][A-Z0-9_]+(?:\s*\+[^(),;]+)?|\w+\.[A-Z][A-Z0-9_]+|key|name|tagName|nbtKey)\)""", """.getBooleanOr($1, false)""", """\.getBooleanOr\(([^(),;]+), false\)""", """.getBoolean($1)""")
	}
	replacements.regex(is26, "!mc26_nbt_string") {
		replace("""\.getString\(([^(),;]+)\)""", """.getStringOr($1, "")""", """\.getStringOr\(([^(),;]+), ""\)""", """.getString($1)""")
	}
	replacements.regex(is26, "!mc26_nbt_compound") {
		replace("""\.getCompound\(([^(),;]+)\)""", """.getCompoundOrEmpty($1)""", """\.getCompoundOrEmpty\(([^(),;]+)\)""", """.getCompound($1)""")
	}
	replacements.regex(is26, "!mc26_nbt_list") {
		replace("""\.getList\(([^(),;]+),\s*([\w.]+)\)""", """.getListOrEmpty(com.hbm_m.platform.legacy.Nbt.k($1, $2))""", """\.getListOrEmpty\(com\.hbm_m\.platform\.legacy\.Nbt\.k\(([^(),;]+), ([\w.]+)\)\)""", """.getList($1, $2)""")
	}
	replacements.regex(is26, "!mc26_nbt_intarray") {
		replace("""\.getIntArray\(([^(),;"]*"[^"]*"[^(),;]*|[A-Z][A-Z0-9_]+(?:\s*\+[^(),;]+)?|\w+\.[A-Z][A-Z0-9_]+|key|name|tagName|nbtKey)\)""", """.getIntArray($1).orElseGet(() -> new int[0])""", """\.getIntArray\(([^(),;]+)\)\.orElseGet\(\(\) -> new int\[0\]\)""", """.getIntArray($1)""")
	}
	replacements.regex(is26, "!mc26_nbt_longarray") {
		replace("""\.getLongArray\(([^(),;"]*"[^"]*"[^(),;]*|[A-Z][A-Z0-9_]+(?:\s*\+[^(),;]+)?|\w+\.[A-Z][A-Z0-9_]+|key|name|tagName|nbtKey)\)""", """.getLongArray($1).orElseGet(() -> new long[0])""", """\.getLongArray\(([^(),;]+)\)\.orElseGet\(\(\) -> new long\[0\]\)""", """.getLongArray($1)""")
	}
	replacements.regex(is26, "!mc26_nbt_bytearray") {
		replace("""\.getByteArray\(([^(),;"]*"[^"]*"[^(),;]*|[A-Z][A-Z0-9_]+(?:\s*\+[^(),;]+)?|\w+\.[A-Z][A-Z0-9_]+|key|name|tagName|nbtKey)\)""", """.getByteArray($1).orElseGet(() -> new byte[0])""", """\.getByteArray\(([^(),;]+)\)\.orElseGet\(\(\) -> new byte\[0\]\)""", """.getByteArray($1)""")
	}
	replacements.regex(is26, "!mc26_nbt_contains") {
		replace("""\.contains\(([^(),;"]*"[^"]*"[^(),;]*|[A-Z][A-Z0-9_]+(?:\s*\+[^(),;]+)?|\w+\.[A-Z][A-Z0-9_]+|key|name|tagName|nbtKey),\s*(Tag\.TAG_\w+|\d+)\)""", """.contains(com.hbm_m.platform.legacy.Nbt.k($1, $2))""", """\.contains\(com\.hbm_m\.platform\.legacy\.Nbt\.k\(([^(),;]+), (Tag\.TAG_\w+|\d+)\)\)""", """.contains($1, $2)""")
	}
	replacements.regex(is26, "!mc26_nbt_allkeys") {
		replace("""\.getAllKeys\(\)""", """.keySet( )""", """\.keySet\( \)""", """.getAllKeys()""")
	}
	// Block.codec()/simpleCodec und BlockEntityType.Builder gibt es in 26.x nicht mehr.
	replacements.regex(is26, "!mc26_simplecodec") {
		replace("""(?<![\w.])simpleCodec\(""", """com.hbm_m.platform.legacy.BlockCodecs.legacySimpleCodec(""", """\bcom\.hbm_m\.platform\.legacy\.BlockCodecs\.legacySimpleCodec\(""", """simpleCodec(""")
	}
	replacements.regex(is26, "!mc26_codec_override") {
		replace("""@Override(\s+(?:protected|public)\s+(?:com\.mojang\.serialization\.)?MapCodec<[^>]*>\s+codec\(\))""", """@SuppressWarnings("hbm_m:mc26codec")$1""", """@SuppressWarnings\("hbm_m:mc26codec"\)""", """@Override""")
	}
	replacements.regex(is26, "!mc26_be_builder") {
		replace("""\bBlockEntityType\.Builder\.of\(""", """com.hbm_m.platform.legacy.BeTypes.of(""", """\bcom\.hbm_m\.platform\.legacy\.BeTypes\.of\(""", """BlockEntityType.Builder.of(""")
	}
	replacements.regex(is26, "!mc26_be_builder_generic") {
		replace("""\bBlockEntityType\.Builder\.<([\w.]+)>of\(""", """com.hbm_m.platform.legacy.BeTypes.<$1>of(""", """\bcom\.hbm_m\.platform\.legacy\.BeTypes\.<([\w.]+)>of\(""", """BlockEntityType.Builder.<$1>of(""")
	}
	// RenderType-Fabriken liegen in 26.x in RenderTypes (Cull-Semantik von entityCutout ist vertauscht).
	replacements.regex(is26, "!mc26_rendertypes") {
		replace("""(?<![\w.])RenderType\.(entityTranslucent|entityTranslucentCull|entityTranslucentEmissive|eyes|lines|armorCutoutNoCull|lightning|translucentMovingBlock|entitySolid|text|debugQuads|energySwirl|entityShadow|leash|outline)\(""", """net.minecraft.client.renderer.rendertype.RenderTypes.$1(""", """\bnet\.minecraft\.client\.renderer\.rendertype\.RenderTypes\.(\w+)\(""", """RenderType.$1(""")
	}
	replacements.regex(is26, "!mc26_rt_cutout_cull") {
		replace("""(?<![\w.])RenderType\.entityCutout\(""", """net.minecraft.client.renderer.rendertype.RenderTypes .entityCutoutCull(""", """\bnet\.minecraft\.client\.renderer\.rendertype\.RenderTypes \.entityCutoutCull\(""", """RenderType.entityCutout(""")
	}
	replacements.regex(is26, "!mc26_rt_cutout_nocull") {
		replace("""(?<![\w.])RenderType\.entityCutoutNoCull\(""", """net.minecraft.client.renderer.rendertype.RenderTypes  .entityCutout(""", """\bnet\.minecraft\.client\.renderer\.rendertype\.RenderTypes  \.entityCutout\(""", """RenderType.entityCutoutNoCull(""")
	}
	// Client-Brücken: MultiBufferSource (entfernt) und Block-Entity-Renderer im alten Stil.
	replacements.string(is26, "!mc26_multibuffersource") {
		replace("net.minecraft.client.renderer.MultiBufferSource", "com.hbm_m.platform.legacy.client.MultiBufferSource")
	}
	replacements.regex(is26, "!mc26_ber") {
		replace("""(?<![\w.])BlockEntityRenderer<(\w+)>""", """com.hbm_m.platform.legacy.client.LegacyBer<$1>""", """\bcom\.hbm_m\.platform\.legacy\.client\.LegacyBer<(\w+)>""", """BlockEntityRenderer<$1>""")
	}
	replacements.regex(is26, "!mc26_entity_renderer") {
		replace("""\bextends EntityRenderer<(\w+)>""", """extends com.hbm_m.platform.legacy.client.LegacyEntityRenderer<$1>""", """\bextends com\.hbm_m\.platform\.legacy\.client\.LegacyEntityRenderer<(\w+)>""", """extends EntityRenderer<$1>""")
	}
	// GuiGraphics-Aufrufe (1.21.1) -> GuiCompat (26.x), siehe com.hbm_m.platform.legacy.client.GuiCompat.
	replacements.regex(is26, "!mc26_gui_calls") {
		replace("""\b(guiGraphics|g|gui|graphics|gfx|pGuiGraphics)\.(blit|blitSprite|drawString|drawCenteredString|drawWordWrap|renderItem|renderFakeItem|renderItemDecorations|renderTooltip|renderComponentTooltip|hLine|vLine|renderOutline|setColor)\(""", """com.hbm_m.platform.legacy.client.GuiCompat.$2($1, """, """\bcom\.hbm_m\.platform\.legacy\.client\.GuiCompat\.(\w+)\((\w+), """, """$2.$1(""")
	}
	replacements.regex(is26, "!mc26_gui_noarg") {
		replace("""\b(guiGraphics|g|gui|graphics|gfx|pGuiGraphics)\.(pose|flush)\(\)""", """com.hbm_m.platform.legacy.client.GuiCompat.$2($1)""", """\bcom\.hbm_m\.platform\.legacy\.client\.GuiCompat\.(pose|flush)\((\w+)\)""", """$2.$1()""")
	}
	// Umbenannte Level/Player/Buffer-Methoden.
	replacements.string(is26, "!mc26_misc_renames") {
		replace(".readResourceLocation()", ".readIdentifier()")
		replace(".writeResourceLocation(", ".writeIdentifier(")
		replace(".getMinBuildHeight()", ".getMinY( )")
	}
	replacements.regex(is26, "!mc26_maxbuildheight") {
		replace("""\b((?:\w+\.)*\w+(?:\(\))?)\.getMaxBuildHeight\(\)""", """com.hbm_m.platform.legacy.LevelCompat.getMaxBuildHeight($1)""", """\bcom\.hbm_m\.platform\.legacy\.LevelCompat\.getMaxBuildHeight\(((?:\w+\.)*\w+(?:\(\))?)\)""", """$1.getMaxBuildHeight()""")
	}
	replacements.regex(is26, "!mc26_displayclientmessage") {
		replace("""\b((?:\w+\.)*\w+(?:\(\))?)\.displayClientMessage\(""", """com.hbm_m.platform.legacy.LevelCompat.displayClientMessage($1, """, """\bcom\.hbm_m\.platform\.legacy\.LevelCompat\.displayClientMessage\(((?:\w+\.)*\w+(?:\(\))?), """, """$1.displayClientMessage(""")
	}
	// RenderSystem-Immediate-State (1.21.1) -> LegacyRenderSystem (26.x). Nur Methoden, die es in 26.x nicht
	// (oder mit anderer Signatur) gibt; vorhandene wie isOnRenderThread bleiben bei RenderSystem.
	replacements.regex(is26, "!mc26_rendersystem_shader") {
		replace("""(?<![\w.])RenderSystem\.setShader\(GameRenderer::(\w+)\)""", """com.hbm_m.platform.legacy.client.LegacyRenderSystem.setVanillaShader("$1")""", """\bcom\.hbm_m\.platform\.legacy\.client\.LegacyRenderSystem\.setVanillaShader\("(\w+)"\)""", """RenderSystem.setShader(GameRenderer::$1)""")
	}
	replacements.regex(is26, "!mc26_rendersystem_shader_other") {
		replace("""(?<![\w.])RenderSystem\.setShader\((?!GameRenderer::)""", """com.hbm_m.platform.legacy.client.LegacyRenderSystem.setShader(""", """\bcom\.hbm_m\.platform\.legacy\.client\.LegacyRenderSystem\.setShader\(""", """RenderSystem.setShader(""")
	}
	replacements.regex(is26, "!mc26_rendersystem") {
		replace("""(?<![\w.])RenderSystem\.(setShaderColor|getShaderColor|setShaderTexture|getShaderTexture|getShader|enableBlend|disableBlend|defaultBlendFunc|blendFunc|blendFuncSeparate|blendEquation|enableDepthTest|disableDepthTest|depthMask|depthFunc|enableCull|disableCull|colorMask|enablePolygonOffset|disablePolygonOffset|polygonOffset|enableScissor|disableScissor|activeTexture|bindTexture|viewport|clear|getProjectionMatrix|setProjectionMatrix|getVertexSorting|getModelViewMatrix|applyModelViewMatrix|getInverseViewRotationMatrix|setTextureMatrix|resetTextureMatrix|getShaderFogStart|getShaderFogEnd|setShaderFogStart|setShaderFogEnd|getShaderFogColor|setShaderGameTime|recordRenderCall)\(""", """com.hbm_m.platform.legacy.client.LegacyRenderSystem.$1(""", """\bcom\.hbm_m\.platform\.legacy\.client\.LegacyRenderSystem\.(setShaderColor|getShaderColor|setShaderTexture|getShaderTexture|getShader|enableBlend|disableBlend|defaultBlendFunc|blendFunc|blendFuncSeparate|blendEquation|enableDepthTest|disableDepthTest|depthMask|depthFunc|enableCull|disableCull|colorMask|enablePolygonOffset|disablePolygonOffset|polygonOffset|enableScissor|disableScissor|activeTexture|bindTexture|viewport|clear|getProjectionMatrix|setProjectionMatrix|getVertexSorting|getModelViewMatrix|applyModelViewMatrix|getInverseViewRotationMatrix|setTextureMatrix|resetTextureMatrix|getShaderFogStart|getShaderFogEnd|setShaderFogStart|setShaderFogEnd|getShaderFogColor|setShaderGameTime|recordRenderCall)\(""", """RenderSystem.$1(""")
	}
	replacements.regex(is26, "!mc26_rendersystem_fq") {
		replace("""\bcom\.mojang\.blaze3d\.systems\.RenderSystem\.(setShaderColor|getShaderColor|setShaderTexture|getShaderTexture|getShader|enableBlend|disableBlend|defaultBlendFunc|blendFunc|blendFuncSeparate|blendEquation|enableDepthTest|disableDepthTest|depthMask|depthFunc|enableCull|disableCull|colorMask|enablePolygonOffset|disablePolygonOffset|polygonOffset|enableScissor|disableScissor|activeTexture|bindTexture|viewport|clear|getProjectionMatrix|setProjectionMatrix|getVertexSorting|getModelViewMatrix|applyModelViewMatrix|getInverseViewRotationMatrix|setTextureMatrix|resetTextureMatrix|getShaderFogStart|getShaderFogEnd|setShaderFogStart|setShaderFogEnd|getShaderFogColor|setShaderGameTime|recordRenderCall)\(""", """com.hbm_m.platform.legacy.client.LegacyRenderSystem .$1(""", """\bcom\.hbm_m\.platform\.legacy\.client\.LegacyRenderSystem \.(setShaderColor|getShaderColor|setShaderTexture|getShaderTexture|getShader|enableBlend|disableBlend|defaultBlendFunc|blendFunc|blendFuncSeparate|blendEquation|enableDepthTest|disableDepthTest|depthMask|depthFunc|enableCull|disableCull|colorMask|enablePolygonOffset|disablePolygonOffset|polygonOffset|enableScissor|disableScissor|activeTexture|bindTexture|viewport|clear|getProjectionMatrix|setProjectionMatrix|getVertexSorting|getModelViewMatrix|applyModelViewMatrix|getInverseViewRotationMatrix|setTextureMatrix|resetTextureMatrix|getShaderFogStart|getShaderFogEnd|setShaderFogStart|setShaderFogEnd|getShaderFogColor|setShaderGameTime|recordRenderCall)\(""", """com.mojang.blaze3d.systems.RenderSystem.$1(""")
	}
	replacements.regex(is26, "!mc26_blendfactors") {
		replace("""\bGlStateManager\.(SourceFactor|DestFactor)\b""", """com.hbm_m.platform.legacy.client.LegacyRenderSystem.$1""", """\bcom\.hbm_m\.platform\.legacy\.client\.LegacyRenderSystem\.(SourceFactor|DestFactor)\b""", """GlStateManager.$1""")
	}
	// Forge-Paketnamen direkt auf die Legacy-Handler (die NeoForge-Umbenennung läuft erst danach).
	replacements.string(is26, "!mc26_legacy_handlers_forge") {
		replace("net.minecraftforge.items.", "com.hbm_m.platform.legacy.items .")
		replace("net.minecraftforge.fluids.capability.", "com.hbm_m.platform.legacy.fluids.capability .")
		replace("net.minecraftforge.fluids.FluidActionResult", "com.hbm_m.platform.legacy.fluids .FluidActionResult")
		replace("net.minecraftforge.energy.", "com.hbm_m.platform.legacy.energy .")
	}
	// LightTexture -> LightCoordsUtil, Kamera, PoseStack.mulPose(Quaternionf) -> rotate.
	replacements.string(is26, "!mc26_lightcoords_import") {
		replace("import net.minecraft.client.renderer.LightTexture;", "import net.minecraft.util.LightCoordsUtil ;")
	}
	replacements.regex(is26, "!mc26_lightcoords") {
		replace("""(?<![\w.])LightTexture\.(FULL_BRIGHT|FULL_SKY|pack|sky|block)\b""", """net.minecraft.util.LightCoordsUtil.$1""", """\bnet\.minecraft\.util\.LightCoordsUtil\.(FULL_BRIGHT|FULL_SKY|pack|sky|block)\b""", """LightTexture.$1""")
	}
	replacements.regex(is26, "!mc26_camera_pos") {
		replace("""\.getMainCamera\(\)\.getPosition\(\)""", """.mainCamera().position()""", """\.mainCamera\(\)\.position\(\)""", """.getMainCamera().getPosition()""")
	}
	replacements.regex(is26, "!mc26_camera") {
		replace("""\.getMainCamera\(\)(?!\.getPosition\(\))""", """.mainCamera( )""", """\.mainCamera\( \)""", """.getMainCamera()""")
	}
	replacements.regex(is26, "!mc26_mulpose") {
		replace("""\.mulPose\(""", """.rotate (""", """\.rotate \(""", """.mulPose(""")
	}
	// Screens: render/renderBg/mouseClicked(double,double,int)/... auf die 26.x-API abbilden.
	replacements.regex(is26, "!mc26_container_screen") {
		replace("""\bextends AbstractContainerScreen<""", """extends com.hbm_m.platform.legacy.client.LegacyContainerScreen<""", """\bextends com\.hbm_m\.platform\.legacy\.client\.LegacyContainerScreen<""", """extends AbstractContainerScreen<""")
	}
	replacements.regex(is26, "!mc26_screen") {
		replace("""\bextends Screen\b(?![\w.])""", """extends com.hbm_m.platform.legacy.client.LegacyScreen""", """\bextends com\.hbm_m\.platform\.legacy\.client\.LegacyScreen\b""", """extends Screen""")
	}
	replacements.regex(is26, "!mc26_entity_build") {
		replace("""\.build\(("[^"]*")\)""", """.build(com.hbm_m.platform.legacy.RegIds.entity($1))""", """\.build\(com\.hbm_m\.platform\.legacy\.RegIds\.entity\(("[^"]*")\)\)""", """.build($1)""")
	}
	// Registry.get(id) liefert ab 1.21.2 Optional<Reference>; getValue(id) ist das alte Verhalten.
	replacements.regex(is26, "!mc26_registry_getvalue") {
		replace("""\b(BuiltInRegistries\.\w+)\.get\(""", """$1.getValue (""", """\b(BuiltInRegistries\.\w+)\.getValue \(""", """$1.get(""")
	}
	replacements.regex(is26, "!mc26_level_random2") {
		replace("""(\.level\(\)|\.level|\bthis\.level|\b\w*[lL]evel\w*|\bworld\w*)\.random\b(?!\s*=)""", """$1.getRandom()""", """(?!)""", """$0""")
	}
	replacements.regex(is26, "!mc26_guigraphics") {
		replace("""\bGuiGraphics\b(?!Extractor)""", "GuiGraphicsExtractor", """\bGuiGraphicsExtractor\b""", "GuiGraphics")
	}
	replacements.regex(is26, "!mc26_dirprop_create") {
		replace("""\bDirectionProperty\.create\(("[^"]*")""", "EnumProperty.create($1, Direction.class", """\bEnumProperty\.create\(("[^"]*"), Direction\.class""", "DirectionProperty.create($1")
	}
	replacements.regex(is26, "!mc26_dirprop_type") {
		replace("""(?<!properties\.)\bDirectionProperty\b(?!\.create)""", "EnumProperty<Direction>", """\bEnumProperty<Direction>""", "DirectionProperty")
	}
	// Level.isClientSide ist ab 26 privat; isClientSide() gibt es auch in 1.20.1/1.21.1.
	replacements.regex(is26, "!mc26_isclientside") {
		replace("""\.isClientSide\b(?!\s*\()""", ".isClientSide()", """(?!)""", "$0")
	}
	// Level.random ist ab 26 protected; getRandom() gibt es überall.
	replacements.regex(is26, "!mc26_levelrandom") {
		replace("""\b(level|world|worldObj|pLevel|serverLevel|lvl)\.random\b(?!\s*=)""", "$1.getRandom()", """(?!)""", "$0")
	}
	replacements.regex(is26, "!mc26_identifier") {
		replace("""\bResourceLocation\b""", "Identifier", """(?<![-\w])Identifier\b""", "ResourceLocation")
	}
}
