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
		"runtimeOnly"("maven.modrinth:create:6.0.10+mc1.21.1")
		"runtimeOnly"("maven.modrinth:sable:2.0.5+mc1.21.1")
		"runtimeOnly"("maven.modrinth:create-aeronautics:1.3.1+mc1.21.1") // bundled: simulated + offroad внутри
	}
	// В NeoForge артефакт называется flywheel-neoforge-api
	"compileOnly"("dev.engine-room.flywheel:flywheel-neoforge-api-$mcVer:${prop("deps.flywheel")}")
	// Distant Horizons: compileOnly для официального API (см. DhRenderBridge).
	// Класс моста грузится только при установленном DH.
	"compileOnly"("maven.modrinth:distanthorizons:3.2.0-b-1.21.1") // 3.2.0-b-1.21.1
	// "runtimeOnly"("maven.modrinth:distanthorizons:3.2.0-b-1.21.1")

	"compileOnly"("maven.modrinth:u6dRKJwZ:${prop("deps.jei")}")
	"runtimeOnly"("maven.modrinth:u6dRKJwZ:${prop("deps.jei")}")
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

		// Нормализация ванильных рецептов (minecraft:*) в формат 1.21.x:
		// "result": {"item": X} → {"id": X};  "result": "X" → {"id": X}.
		// Кастомные рецепты (hbm_m:*) не трогаем — их нормализует RecipeHooks.
		val recipeRoots = dataDir.listFiles()!!.mapNotNull { File(it, "recipe").takeIf(File::isDirectory) }
		val slurper = groovy.json.JsonSlurper()
		recipeRoots.forEach { root ->
			root.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
				val tree = slurper.parse(file) as? Map<*, *> ?: return@forEach
				if ((tree["type"] as? String)?.startsWith("minecraft:") != true) return@forEach
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
					file.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(copy)))
				}
			}
		}

		// Ссылки на теги forge:* внутри рецептов → c:* (неймспейс переименован выше).
		dataDir.resolve("hbm_m").resolve("recipe").walkTopDown()
			.filter { it.isFile && it.extension == "json" }.forEach { file ->
				val text = file.readText()
				if (text.contains("\"forge:")) {
					file.writeText(text.replace(Regex("\"(tag)\"\\s*:\\s*\"forge:"), "\"$1\": \"c:"))
				}
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
	options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
	options.encoding = "UTF-8"
}

stonecutter {
}
