// file: build-logic/src/main/kotlin/NeoForgeResourceNormalizer.kt
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.kotlin.dsl.named
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File

fun Project.registerNeoForgeResourceNormalizer() {
	tasks.named<ProcessResources>("processResources") {
		doLast {
			val dataDir = File(destinationDir, "data")
			if (!dataDir.isDirectory) return@doLast

			fun moveInto(source: File, target: File) {
				target.mkdirs()
				source.listFiles()!!.forEach { child ->
					val dest = File(target, child.name)
					if (dest.exists()) {
						if (child.isDirectory) moveInto(child, dest) else child.delete()
					} else {
						child.renameTo(dest)
					}
				}
				source.deleteRecursively()
			}

			val dirRenames = mapOf(
				"recipes" to "recipe", "advancements" to "advancement",
				"loot_tables" to "loot_table", "structures" to "structure",
			)
			val tagRenames = mapOf(
				"blocks" to "block", "items" to "item", "entity_types" to "entity_type",
				"fluids" to "fluid", "game_events" to "game_event",
			)

			// 1. Biome-модификаторы: forge/biome_modifier -> neoforge/biome_modifier
			dataDir.listFiles()!!.filter { it.isDirectory }.forEach { nsDir ->
				val bm = File(nsDir, "forge/biome_modifier")
				if (bm.isDirectory) {
					val target = File(nsDir, "neoforge/biome_modifier")
					moveInto(bm, target)
					target.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { f ->
						f.writeText(
							f.readText()
								.replace("\"forge:add_features\"", "\"neoforge:add_features\"")
								.replace("\"forge:add_spawns\"", "\"neoforge:add_spawns\"")
								.replace("\"hbm_m:add_carvers\"", "\"neoforge:add_carvers\"")
						)
					}
				}
			}

			// 2. Переименование папок во множественном числе и переезд forge: -> c:
			dataDir.listFiles()!!.filter { it.isDirectory }.forEach { nsDir ->
				if (nsDir.name == "forge") {
					moveInto(nsDir, File(dataDir, "c"))
					val cTags = File(File(dataDir, "c"), "tags")
					if (cTags.isDirectory) {
						for ((old, new) in tagRenames) {
							val plural = File(cTags, old)
							if (plural.isDirectory) moveInto(plural, File(cTags, new))
						}
					}
				}
				for ((old, new) in dirRenames) {
					val plural = File(nsDir, old)
					if (plural.isDirectory) moveInto(plural, File(nsDir, new))
				}
				val tags = File(nsDir, "tags")
				if (tags.isDirectory) {
					for ((old, new) in tagRenames) {
						val plural = File(tags, old)
						if (plural.isDirectory) moveInto(plural, File(tags, new))
					}
				}
			}

			// 3. Ссылки на теги внутри JSON: forge: -> c:
			dataDir.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
				val text = file.readText()
				if (!text.contains("forge:")) return@forEach
				val out = text.replace("\"#forge:", "\"#c:").replace("\"forge:", "\"c:")
					.replace("\"c:glass\"", "\"c:glass_blocks\"").replace("\"#c:glass\"", "\"#c:glass_blocks\"")
				if (out != text) file.writeText(out)
			}

			// 4. Ремап silk-touch предикатов в loot_table
			val lootRoots = dataDir.listFiles()!!.mapNotNull { File(it, "loot_table").takeIf(File::isDirectory) }
			val slurper = JsonSlurper()
			lootRoots.forEach { root ->
				root.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
					val text = file.readText()
					if (!text.contains("minecraft:match_tool")) return@forEach
					val tree = slurper.parseText(text)
					@Suppress("UNCHECKED_CAST")
					fun fixPredicate(predicate: MutableMap<String, Any>) {
						val ench = predicate.remove("enchantments") as? List<Any> ?: return
						val converted = ench.map { e ->
							val m = e as MutableMap<String, Any>
							val out = linkedMapOf<String, Any>()
							m["enchantment"]?.let { out["enchantments"] = it }
							m["levels"]?.let { out["levels"] = it }
							out
						}
						predicate["predicates"] = linkedMapOf("minecraft:enchantments" to converted)
					}
					@Suppress("UNCHECKED_CAST")
					fun walk(node: Any?) {
						when (node) {
							is MutableMap<*, *> -> {
								if (node["condition"] == "minecraft:match_tool") {
									(node["predicate"] as? MutableMap<String, Any>)?.let { fixPredicate(it) }
								}
								node.values.forEach { walk(it) }
							}
							is MutableList<*> -> node.forEach { walk(it) }
						}
					}
					walk(tree)
					file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(tree)))
				}
			}

			// 5. Нормализация ванильных рецептов ("item" -> "id")
			val recipeRoots = dataDir.listFiles()!!.mapNotNull { File(it, "recipe").takeIf(File::isDirectory) }
			recipeRoots.forEach { root ->
				root.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
					val tree = slurper.parse(file) as? Map<*, *> ?: return@forEach
					val type = tree["type"] as? String ?: return@forEach
					if (!type.startsWith("minecraft:") && type != "hbm_m:container_upgrade") return@forEach
					val result = tree["result"]
					val normalized: Any? = when (result) {
						is String -> mapOf("id" to result)
						is Map<*, *> ->
							if (result.containsKey("item") && !result.containsKey("id"))
								result.entries.associate { (k, v) -> if (k == "item") "id" to v else k to v }
							else null
						else -> null
					}
					if (normalized != null) {
						val copy = tree.toMutableMap()
						copy["result"] = normalized
						file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(copy)))
					}
				}
			}

			// 6. Удаление устаревших лут-таблиц батарей/конденсаторов/графита с copy_nbt
			listOf("loot_table", "loot_tables").forEach { dirName ->
				File(File(dataDir, "hbm_m"), dirName).walkTopDown()
					.filter {
						it.isFile && ((it.name.startsWith("machine_battery") || it.name == "machine_fensu.json")
							&& it.name != "machine_battery_socket.json" && it.name != "machine_battery_redd.json" && it.name != "machine_fensu.json"
							|| (it.name.startsWith("capacitor_") && it.name != "capacitor_bus.json")
							|| it.name.startsWith("block_graphite_"))
					}
					.forEach { it.delete() }
			}

			// 7. set_nbt -> set_custom_data и grass -> short_grass
			dataDir.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
				var text = file.readText()
				var changed = false
				if (text.contains("\"minecraft:set_nbt\"")) {
					text = text.replace("\"minecraft:set_nbt\"", "\"minecraft:set_custom_data\"")
					changed = true
				}
				if (text.contains("\"minecraft:grass\"")) {
					text = text.replace("\"minecraft:grass\"", "\"minecraft:short_grass\"")
					changed = true
				}
				if (changed) file.writeText(text)
			}

			// 8. Нормализация ассетов под NeoForge 1.21.1 (separate_transforms / neoforge_data)
			val assetsDir = File(destinationDir, "assets")
			if (assetsDir.isDirectory) {
				assetsDir.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
					val text = file.readText()
					var out = text
					if (out.contains("\"forge:separate_transforms\"")) {
						out = out.replace("\"forge:separate_transforms\"", "\"neoforge:separate_transforms\"")
					}
					if (out.contains("\"forge_data\":")) {
						out = out.replace("\"forge_data\":", "\"neoforge_data\":")
					}
					if (out != text) file.writeText(out)
				}
			}
		}
	}
}
