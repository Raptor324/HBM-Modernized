plugins {
	id("mod-platform")
	id("net.neoforged.moddev")
}

platform {
	loader = "neoforge"
	dependencies {
		required("minecraft") {
			forgeVersionRange = "[${prop("deps.minecraft")}]"
		}
		required("neoforge") {
			forgeVersionRange = "[1,)"
		}
		required("architectury") {
			slug("architectury-api")
			forgeVersionRange = "[${prop("deps.architectury")},)"
		}
		// Create — опциональная совместимость (рендер блоков на поездах + двери на контрапшенах).
		// Зеркало optional-зависимости из src/main/resources/META-INF/mods.toml.
		optional("create") {
			slug("create")
			forgeVersionRange = "[6.0.0,6.1.0)"
		}
	}
}

neoForge {
	version = property("deps.neoforge") as String
	accessTransformers.from(rootProject.file("src/main/resources/aw/${stonecutter.current.version}.cfg"))
	validateAccessTransformers = true

	if (hasProperty("deps.parchment")) parchment {
		val (mc, ver) = (property("deps.parchment") as String).split(':')
		mappingsVersion = ver
		minecraftVersion = mc
	}

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
			ideName = "NeoForge Client (${stonecutter.active?.version})"
			programArgument("--username=Dev")
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "NeoForge Server (${stonecutter.active?.version})"
		}
		// GameTest-сервер: headless-прогон всех @GameTest без GUI.
		// NeoForge 1.21.1 НЕ поддерживает аргумент CLI --gametest (это Forge-only).
		// Вместо этого пропатченный Main.main() читает СИСТЕМНОЕ СВОЙСТВО JVM
		// "neoforge.gameTestServer" и при true запускает GameTestServer.create(...)
		// "neoforge.enableGameTest" дополнительно активирует регистрацию тестов в dev-среде.
		// Шаблоны (empty3x3x3/empty5x5x5) — ванильные; RegisterGameTestsEvent регистрирует
		// классы test-методов (тесты берутся из GameTestRegistry.getAllTestFunctions()).
		// Запуск: ./gradlew :1.21.1-neoforge:runGameTestServer
		register("gameTestServer") {
			server()
			gameDirectory = file("run/")
			ideName = "NeoForge GameTest (${stonecutter.active?.version})"
			systemProperty("neoforge.gameTestServer", "true")
			systemProperty("neoforge.enableGameTest", "true")
		}
	}

	mods {
		register(property("mod.id") as String) {
			sourceSet(sourceSets["main"])
		}
	}
	sourceSets["main"].resources.srcDir(rootProject.file("src/generated/resources"))
}

repositories {
	mavenCentral()
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
	strictMaven("https://maven.architectury.dev/", "dev.architectury") { name = "Architectury" }
	maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
	maven("https://maven.createmod.net") { name = "CreateMod" }
	strictMaven("https://cursemaven.com", "curse.maven") { name = "CurseForge" }
	// Curios API (опционально): слот лица для противогазов. См. com.hbm_m.compat.curios.
	maven("https://maven.theillusivec4.top/") { name = "Illusive Soul Works" }
	maven("https://maven.caffeinemc.net/releases") { name = "CaffeineMC" }

}

dependencies {
	implementation(libs.moulberry.mixinconstraints)
	jarJar(libs.moulberry.mixinconstraints)

	// Architectury API — implementation + jarJar (встраивается в итоговый jar, как на Forge).
	implementation("dev.architectury:architectury-neoforge:${prop("deps.architectury")}")
	jarJar("dev.architectury:architectury-neoforge:${prop("deps.architectury")}")

	val mcVer = stonecutter.current.version
	"compileOnly"("com.simibubi.create:create-$mcVer:${prop("deps.create")}:slim") {
		isTransitive = false
	}
	// Sable (экосистема Create Aeronautics): compileOnly только для валидации
	// строковых таргетов миксинов (@Mixin(targets = "...")) на этапе компиляции.
	// В рантайм не пакуется; в отсутствие Sable миксины просто не применяются.
	"compileOnly"("maven.modrinth:sable:2.0.5+mc1.21.1")

	// Рантайм-зависимости для ручного тестирования интеграции с Create
	// Aeronautics / Sable в runClient (только 1.21.1 - на других версиях
	// этих модов нет). Create 6.x тащит Flywheel/Ponder внутри себя (jarJar).
	if (stonecutter.current.version == "1.21.1") {
		// "runtimeOnly"("maven.modrinth:create:6.0.10+mc1.21.1")
		// "runtimeOnly"("maven.modrinth:sable:2.0.5+mc1.21.1")
		// "runtimeOnly"("maven.modrinth:create-aeronautics:1.3.1+mc1.21.1") // bundled: simulated + offroad внутри
	}
	// В NeoForge артефакт называется flywheel-neoforge-api
	"compileOnly"("dev.engine-room.flywheel:flywheel-neoforge-api-$mcVer:${prop("deps.flywheel")}")
	// Distant Horizons: compileOnly для официального API (см. DhRenderBridge).
	// Класс моста грузится только при установленном DH.
	"compileOnly"("maven.modrinth:distanthorizons:3.2.0-b-1.21.1") // 3.2.0-b-1.21.1
	// "runtimeOnly"("maven.modrinth:distanthorizons:3.2.0-b-1.21.1")

	"compileOnly"("maven.modrinth:u6dRKJwZ:${prop("deps.jei")}")
	"runtimeOnly"("maven.modrinth:u6dRKJwZ:${prop("deps.jei")}")

	// Curios (опционально): API для компиляции, сам мод — в рантайм для тестов.
	"compileOnly"("top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1:api")
	"runtimeOnly"("top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1")
	"runtimeOnly"("maven.modrinth:l6YH9Als:v5qtqRQi") // spark
	"runtimeOnly"("maven.modrinth:1bokaNcj:JXvcT1hp") // xaeros minimap
	"runtimeOnly"("maven.modrinth:NcUtCpym:fOv9QzLO") // xaeros world map
	
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}

// 1.21+ переименовала папки дата-паков из множественного числа в единственное
// (recipes → recipe, tags/blocks → tags/block и т.д.), ItemStack-кодек сменил
// ключ "item" на "id" (у варочных рецептов result стал объектом вместо строки),
// а конвенциональные теги Forge переехали из неймспейса forge: в c:.
// Датаген — 1.20.1-only и пишет во всём старом формате, поэтому нормализуем
// ресурсы на выходе processResources, не трогая датаген.
tasks.named<ProcessResources>("processResources") {
	doLast {
		val dataDir = File(destinationDir, "data")
		if (!dataDir.isDirectory) return@doLast

		// [Phase D] Eigener Forge-1.20.1-Biom-Modifikator hbm_m:add_carvers -> NeoForge-Standardtyp (gleiches Format).
		dataDir.walkTopDown().filter { it.isFile && it.extension == "json" && it.path.contains("biome_modifier") }.forEach { file ->
			val text = file.readText()
			if (text.contains("\"hbm_m:add_carvers\"")) file.writeText(text.replace("\"hbm_m:add_carvers\"", "\"neoforge:add_carvers\""))
		}

		// [Phase C/P3] Eigene forge:-Tags merken, bevor sie nach c: wandern: Verweise darauf behalten ihren Namen,
		// alle anderen forge:-Verweise bekommen den NeoForge-1.21-Namen (stone -> stones usw.).
		val ownForgeTags = mutableSetOf<String>()
		File(dataDir, "forge/tags").takeIf { it.isDirectory }?.listFiles()?.filter { it.isDirectory }?.forEach { kindDir ->
			kindDir.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach {
				ownForgeTags += it.relativeTo(kindDir).invariantSeparatorsPath.removeSuffix(".json")
			}
		}

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

		// Переименование переименованных в 1.21 директорий (merge при коллизии).
		val dirRenames = mapOf(
			"recipes" to "recipe", "advancements" to "advancement",
			"loot_tables" to "loot_table", "structures" to "structure",
		)
		val tagRenames = mapOf(
			"blocks" to "block", "items" to "item", "entity_types" to "entity_type",
			"fluids" to "fluid", "game_events" to "game_event",
		)

		// Biome-модификаторы на NeoForge 1.21+ читаются из neoforge/biome_modifier
		// с типом neoforge:add_features; датаген (1.20.1) пишет
		// forge/biome_modifier + forge:add_features — без ремапа руды не спавнятся.
		dataDir.listFiles()!!.filter { it.isDirectory }.forEach { nsDir ->
			val bm = File(nsDir, "forge/biome_modifier")
			if (bm.isDirectory) {
				val target = File(nsDir, "neoforge/biome_modifier")
				moveInto(bm, target)
				target.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { f ->
					f.writeText(f.readText().replace("\"forge:add_features\"", "\"neoforge:add_features\""))
				}
			}
		}

		dataDir.listFiles()!!.filter { it.isDirectory }.forEach { nsDir ->
			// Теги forge: → c: (конвенциональные теги на NeoForge 1.21+ живут в c:).
			if (nsDir.name == "forge") {
				moveInto(nsDir, File(dataDir, "c"))
				// После переезда в c: теги тоже надо привести к единственному числу
				// (1.21 переименовала tags/items → tags/item и т.д.).
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

		// Remap silk-touch условия в loot-таблицах: датаген (1.20.1) пишет
		// match_tool-предикат с полем "enchantments" внутри ItemPredicate,
		// а на 1.21.1 это поле удалено — проверки зачарований переехали в
		// ItemSubPredicates ("predicates"."minecraft:enchantments", а внутри
		// каждой записи зачарование теперь "enchantments", не "enchantment").
		// Без ремапа silk-ветка рудных таблиц никогда не срабатывает.
		val lootRoots = dataDir.listFiles()!!.mapNotNull { File(it, "loot_table").takeIf(File::isDirectory) }
		lootRoots.forEach { root ->
			root.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
				val text = file.readText()
				if (!text.contains("minecraft:match_tool")) return@forEach
				val tree = groovy.json.JsonSlurper().parseText(text)
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
				file.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(tree)))
			}
		}

		// Нормализация ванильных рецептов (minecraft:*) в формат 1.21.x:
		// "result": {"item": X} → {"id": X};  "result": "X" → {"id": X}.
		// Кастомные рецепты (hbm_m:*) не трогаем — их нормализует RecipeHooks.
		val recipeRoots = dataDir.listFiles()!!.mapNotNull { File(it, "recipe").takeIf(File::isDirectory) }
		val slurper = groovy.json.JsonSlurper()
		recipeRoots.forEach { root ->
			root.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
				val tree = slurper.parse(file) as? Map<*, *> ?: return@forEach
				// [Phase D] hbm_m:container_upgrade liest mit dem Vanilla-ShapedRecipe-Codec -> gleiches Ergebnisformat.
				val rtype = tree["type"] as? String
				if (rtype?.startsWith("minecraft:") != true && rtype != "hbm_m:container_upgrade") return@forEach
				val result = tree["result"]
				val normalized: Any? = when (result) {
					is String -> mapOf("id" to result)
					is Map<*, *> ->
						if (result.containsKey("item") && !result.containsKey("id"))
							result.entries.associate { (k, v) -> if (k == "item") "id" to v else k to v }
						else null
					else -> null
				}
				// [Phase C/P3] 1.20.1-"nbt" am Ergebnis -> Komponenten (wie RecipeHooks fuer hbm_m-Rezepte):
				// SNBT bleibt komplett custom_data (eigene Leser), BlockStateTag zusaetzlich als minecraft:block_state.
				val withNbt = (normalized ?: result) as? Map<*, *>
				val finalResult: Any? = if (withNbt != null && withNbt.containsKey("nbt") && !withNbt.containsKey("components")) {
					val nbt = withNbt["nbt"]
					val comps = linkedMapOf<String, Any?>("minecraft:custom_data" to nbt)
					val snbt = nbt as? String
					if (snbt != null) {
						Regex("""BlockStateTag\s*:\s*\{([^}]*)\}""").find(snbt)?.let { m ->
							val props = linkedMapOf<String, String>()
							Regex("""([A-Za-z0-9_]+)\s*:\s*"?([^",}]*)"?""").findAll(m.groupValues[1]).forEach { p ->
								props[p.groupValues[1]] = p.groupValues[2]
							}
							if (props.isNotEmpty()) comps["minecraft:block_state"] = props
						}
					}
					withNbt.entries.filter { it.key != "nbt" }.associate { (k, v) -> k to v } + mapOf("components" to comps)
				} else normalized
				if (finalResult != null) {
					val copy = tree.toMutableMap()
					copy["result"] = finalResult
					file.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(copy)))
				}
			}
		}

		// [Phase C/P3] Rezept-/Advancement-/Loot-Bedingungen: Forge "conditions" (oberste Ebene, Liste) ->
		// "neoforge:conditions"; Bedingungstypen forge:* -> neoforge:* (siehe Umschreibung unten).
		listOf("recipe", "advancement", "loot_table").forEach { dirName ->
			dataDir.listFiles()!!.mapNotNull { File(it, dirName).takeIf(File::isDirectory) }.forEach { root ->
				root.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
					val text = file.readText()
					if (!text.contains("\"conditions\"")) return@forEach
					val tree = slurper.parseText(text) as? Map<*, *> ?: return@forEach
					val conds = tree["conditions"] as? List<*> ?: return@forEach
					val copy = linkedMapOf<Any?, Any?>()
					tree.forEach { (k, v) -> if (k == "conditions") copy["neoforge:conditions"] = conds else copy[k] = v }
					file.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(copy)))
				}
			}
		}

		// [Phase C/P3] Advancement-Symbole: {"item": X, "nbt": ..} -> {"id": X, "components": {"minecraft:custom_data": ..}}.
		dataDir.listFiles()!!.mapNotNull { File(it, "advancement").takeIf(File::isDirectory) }.forEach { root ->
			root.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
				val text = file.readText()
				if (!text.contains("\"icon\"")) return@forEach
				@Suppress("UNCHECKED_CAST")
				val tree = slurper.parseText(text) as? MutableMap<String, Any?> ?: return@forEach
				@Suppress("UNCHECKED_CAST")
				val icon = (tree["display"] as? MutableMap<String, Any?>)?.get("icon") as? MutableMap<String, Any?> ?: return@forEach
				var changed = false
				icon.remove("item")?.let { if (!icon.containsKey("id")) icon["id"] = it; changed = true }
				icon.remove("nbt")?.let { icon["components"] = mapOf("minecraft:custom_data" to it); changed = true }
				if (changed) file.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(tree)))
			}
		}

		// [Phase C/P3] Verweise forge:* in allen Daten:
		//  - Tag-Verweise ("#forge:X", "tag": "forge:X") -> c:X, bzw. NeoForge-1.21-Name bei Vanilla-Konventionstags;
		//  - Forge-Typen mit NeoForge-Gegenstueck (Zutaten difference/compound/intersection, Bedingungen, Biome-Modifier) -> neoforge:*;
		//  - forge:nbt / forge:partial_nbt bleiben (eigene NeoForge-Zutatentypen, com.hbm_m.recipe.LegacyNbtIngredients).
		// Loot: minecraft:set_nbt -> minecraft:set_custom_data (gleiches Feld "tag").
		val cTagRenames = mapOf(
			"stone" to "stones", "cobblestone" to "cobblestones", "sand" to "sands", "gravel" to "gravels",
			"glass" to "glass_blocks", "glass/colorless" to "glass_blocks/colorless", "glass/tinted" to "glass_blocks/tinted",
			"string" to "strings", "obsidian" to "obsidians", "gunpowder" to "gunpowders", "netherrack" to "netherracks",
			"leather" to "leathers", "sandstone" to "sandstone/blocks", "end_stones" to "end_stones",
		)
		fun cName(name: String) = if (name in ownForgeTags) name else (cTagRenames[name] ?: name)
		val neoTypes = setOf(
			"difference", "compound", "intersection",
			"not", "and", "or", "mod_loaded", "item_exists", "tag_empty", "true", "false",
			"add_features", "remove_features", "add_spawns", "remove_spawns", "add_carvers", "remove_carvers", "none",
		)
		val keyedRef = Regex(""""([A-Za-z0-9_:]+)"(\s*:\s*)"(#?)forge:([a-z0-9_./-]+)"""")
		val bareTagRef = Regex(""""#forge:([a-z0-9_./-]+)"""")
		dataDir.walkTopDown()
			.filter { it.isFile && it.extension == "json" }.forEach { file ->
				val text = file.readText()
				if (!text.contains("forge:") && !text.contains("\"minecraft:set_nbt\"")) return@forEach
				var out = keyedRef.replace(text) { m ->
					val (key, sep, hash, name) = m.destructured
					when {
						hash == "#" -> "\"$key\"$sep\"#c:${cName(name)}\""
						key == "tag" -> "\"$key\"$sep\"c:${cName(name)}\""
						key == "type" && name in neoTypes -> "\"$key\"$sep\"neoforge:$name\""
						else -> m.value
					}
				}
				out = bareTagRef.replace(out) { m -> "\"#c:${cName(m.groupValues[1])}\"" }
				out = out.replace("\"minecraft:set_nbt\"", "\"minecraft:set_custom_data\"")
				if (out != text) file.writeText(out)
			}

		// Лут-таблицы батарей используют minecraft:copy_nbt, удалённый в 1.20.5+.
		// На 1.21.1 состояние батареи переносится кодом
		// (MachineBatteryBlock#playerWillDestroy → MachineBatteryBlockEntity#saveToItemStack),
		// поэтому таблицы просто исключаем из сборки.
		listOf("loot_table", "loot_tables").forEach { dirName ->
			File(File(dataDir, "hbm_m"), dirName).walkTopDown()
				.filter { it.isFile && it.name.startsWith("machine_battery") }
				.forEach { it.delete() }
		}

		// minecraft:grass переименован в short_grass в 1.20.3+ (датаген 1.20.1 пишет старое имя).
		dataDir.walkTopDown()
			.filter { it.isFile && it.extension == "json" }.forEach { file ->
				val text = file.readText()
				if (text.contains("\"minecraft:grass\"")) {
					file.writeText(text.replace("\"minecraft:grass\"", "\"minecraft:short_grass\""))
				}
			}
	}
}

sourceSets {
	main {
		java {
			exclude("com/hbm_m/datagen/**")
		}
	}
}

// GameTest'ы нужны только в dev-ранах (gameTestServer работает из classes-директории,
// не из jar) — в продакшен-jar они не попадают.
tasks.named<Jar>("jar") {
	exclude("com/hbm_m/test/**")
}

tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xmaxerrs", "100000", "-XDshould-stop.ifError=FLOW"))
	options.encoding = "UTF-8"
}

stonecutter {
}
