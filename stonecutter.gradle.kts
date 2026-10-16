plugins {
	alias(libs.plugins.stonecutter)
	alias(libs.plugins.dotenv)
	alias(libs.plugins.fabric.loom).apply(false)
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
	val ordering = versionComparator.thenComparingInt { task ->
		if (task.metadata.project.endsWith("fabric")) 1 else 0
	}

	listOf("publishModrinth", "publishCurseforge").forEach { taskName ->
		gradle.allprojects {
			if (project.tasks.findByName(taskName) != null) {
				order(taskName, ordering)
			}
		}
	}
}

stonecutter parameters {
	constants.match(node.metadata.project.substringAfterLast('-'), "fabric", "neoforge", "forge")
	filters.include("**/*.fsh", "**/*.vsh")
	swaps["mod_version"] = "\"" + property("mod.version") + "\";"
	swaps["mod_id"] = "\"" + property("mod.id") + "\";"
	swaps["mod_name"] = "\"" + property("mod.name") + "\";"
	swaps["mod_group"] = "\"" + property("mod.group") + "\";"
	swaps["minecraft"] = "\"" + node.metadata.version + "\";"
	constants["release"] = property("mod.id") != "modtemplate"
}
