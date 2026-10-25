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
		replace("net.minecraftforge.client.model.geometry.IUnbakedGeometry", "net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry")
		replace("net.minecraftforge.client.model.geometry.IGeometryLoader", "net.neoforged.neoforge.client.model.geometry.IGeometryLoader")
		// fluids
		replace("net.minecraftforge.fluids.FluidStack", "net.neoforged.neoforge.fluids.FluidStack")
		replace("net.minecraftforge.fluids.capability.IFluidHandler", "net.neoforged.neoforge.fluids.capability.IFluidHandler")
		replace("net.minecraftforge.fluids.capability.FluidAction", "net.neoforged.neoforge.fluids.capability.FluidAction")
		replace("net.minecraftforge.fluids.capability.templates.FluidTank", "net.neoforged.neoforge.fluids.capability.templates.FluidTank")
		// items / energy
		replace("net.minecraftforge.items.IItemHandler", "net.neoforged.neoforge.items.IItemHandler")
		replace("net.minecraftforge.items.SlotItemHandler", "net.neoforged.neoforge.items.SlotItemHandler")
		replace("net.minecraftforge.energy.IEnergyStorage", "net.neoforged.neoforge.energy.IEnergyStorage")
		// gametest
		replace("net.minecraftforge.gametest.GameTestHolder", "net.neoforged.neoforge.gametest.GameTestHolder")
		replace("net.minecraftforge.gametest.PrefixGameTestTemplate", "net.neoforged.neoforge.gametest.PrefixGameTestTemplate")
		replace("net.minecraftforge.gametest.GameTest", "net.neoforged.neoforge.gametest.GameTest")
	}
}
