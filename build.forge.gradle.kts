import org.gradle.kotlin.dsl.runtimeOnly

plugins {
	id("mod-platform")
	id("net.neoforged.moddev.legacyforge")
}

platform {
	loader = "forge"
	dependencies {
		required("minecraft") {
			forgeVersionRange = "[${prop("deps.minecraft")}]"
		}
		required("forge") {
			// Минимальная версия Forge: 47.3.31. Более старые версии Forge не смогут загрузить мод.
			forgeVersionRange = "[47.3.31,)"
		}
		optional("architectury") {
			slug("architectury-api")
			forgeVersionRange = "[${prop("deps.architectury")},)"
		}
	}
}

legacyForge {
	version = "${property("deps.minecraft")}-${property("deps.forge")}"
	if (hasProperty("deps.parchment")) parchment {
		val parchmentProp = property("deps.parchment") as String
		val parts = parchmentProp.split(":")
		mappingsVersion = parts[1]
		minecraftVersion = parts[0]
	}
	validateAccessTransformers = true
	accessTransformers.from(rootProject.file("src/main/resources/aw/${stonecutter.current.version}.cfg"))

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
			ideName = "Forge Client (${stonecutter.active?.version})"
			programArgument("--username=Dev")
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8")
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "Forge Server (${stonecutter.active?.version})"
			programArgument("--nogui")
			jvmArguments.addAll("-Xmx4G", "-Xms2G")
		}
		// GameTest-сервер: headless-прогон всех @GameTest без GUI.
		register("gameTestServer") {
			server()
			gameDirectory = file("run/")
			ideName = "Forge GameTest (${stonecutter.active?.version})"
			systemProperty("forge.gameTestServer", "true")
			systemProperty("forge.enableGameTest", "true")
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8")
		}

		register("data") {
			data()
			gameDirectory = file("run/")
			ideName = "Forge Data (${stonecutter.active?.version})"
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8")
			val datagenOutput = rootProject.file("src/generated/resources")
			val existingResources = rootProject.file("src/main/resources")
			programArgument("--mod")
			programArgument(prop("mod.id"))
			programArgument("--all")
			programArgument("--output")
			programArgument(datagenOutput.absolutePath)
			programArgument("--existing")
			programArgument(existingResources.absolutePath)
		}
	}

	mods {
		register(prop("mod.id")) {
			sourceSet(sourceSets["main"])
		}
	}
}

mixin {
	add(sourceSets.main.get(), prop("mod.mixin_refmap"))
	config(prop("mod.mixin_config"))
}

repositories {
	mavenLocal()
	mavenCentral()
	maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
	maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
	maven("https://maven.shedaniel.me/") { name = "Shedaniel" }
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
	strictMaven("https://cursemaven.com", "curse.maven") { name = "CurseForge" }
	strictMaven("https://maven.architectury.dev/", "dev.architectury") { name = "Architectury" }
	// Create compat (опционально): Create, Flywheel. См. com.hbm_m.compat.create.
	maven("https://maven.createmod.net") { name = "Create" }
	maven("https://maven.ithundxr.dev/mirror") { name = "Ithundxr Mirror" }
	flatDir { dirs(rootProject.file("libs")) }
}

dependencies {
	annotationProcessor("org.spongepowered:mixin:${libs.versions.mixin.get()}:processor")

	implementation(libs.moulberry.mixinconstraints)
	jarJar(libs.moulberry.mixinconstraints)

	"modImplementation"("dev.architectury:architectury-forge:${prop("deps.architectury")}")
	jarJar("dev.architectury:architectury-forge:${prop("deps.architectury")}")

	"modCompileOnly"("curse.maven:jei-238222:${prop("deps.jei")}")
	"modRuntimeOnly"("curse.maven:jei-238222:${prop("deps.jei")}")

	"modCompileOnly"("com.simibubi.create:create-1.20.1:${prop("deps.create")}:slim") {
		isTransitive = false
	}
	// Sable (экосистема Create Aeronautics, существует только на 1.21.1+):
	// compileOnly только для валидации строковых таргетов миксинов на этапе
	// компиляции. На 1.20.1 классы Sable в рантайме отсутствуют -> миксины
	// не применяются (no-op, мод не падает).
	compileOnly("maven.modrinth:sable:2.0.5+mc1.21.1")
	"modCompileOnly"("dev.engine-room.flywheel:flywheel-forge-api-1.20.1:${prop("deps.flywheel")}")
	"modRuntimeOnly"("curse.maven:embeddium-908741:5681725")
	"modRuntimeOnly"("curse.maven:oculus-581495:6020952")
	"modRuntimeOnly"("curse.maven:modernfix-790626:7515215")
	"modRuntimeOnly"("curse.maven:smooth-boot-reloaded-633412:5016280")
	
	"modRuntimeOnly"("curse.maven:screenshot-to-clipboard-326950:3643026")
	// "modRuntimeOnly"("maven.modrinth:cwoL6CqY:3PEwIAxS") // Item Transforms Helper
	// "modRuntimeOnly"("maven.modrinth:f3zK7pP5:8gUY8UiV") // Tick Freeze
	"modRuntimeOnly"("maven.modrinth:spark:1.10.53-forge")
	"modRuntimeOnly"("maven.modrinth:VYRu7qmG:QtSVNyjm") // Observable -  profiles (tile) entities and shows you what's taking up tick time and where.
	"modRuntimeOnly"("maven.modrinth:ordsPcFz:Zsh14XeQ") // Kotlin For Forge
	// "modRuntimeOnly"("curse.maven:konkrete-410295:5028413")
	// Distant Horizons: compileOnly для официального API (DhApiBeforeApplyShaderRenderEvent
	// и пр.) в com.hbm_m.client.compat.dh.DhRenderBridge. Класс моста грузится только при
	// установленном DH, поэтому отсутствие зависимости в рантайме безопасно.
	"modCompileOnly"("maven.modrinth:distanthorizons:3.2.0-b-1.20.1") // 3.2.0-b-1.20.1
	// "modRuntimeOnly"("maven.modrinth:distanthorizons:3.2.0-b-1.20.1")

	// "modRuntimeOnly"("curse.maven:xaeros-world-map-317780:7598469")
    // "modRuntimeOnly"("curse.maven:xaeros-minimap-263420:7598586")
	"modRuntimeOnly"("curse.maven:jade-324717:6855440")
}

sourceSets {
	main {
		resources.srcDir(rootProject.file("src/generated/resources"))
	}
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}

tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }

stonecutter {
}



