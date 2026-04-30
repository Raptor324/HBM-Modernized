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
			forgeVersionRange = "[1,)"
		}
		optional("architectury") {
			slug("architectury-api")
			forgeVersionRange = "[${prop("deps.architectury")},)"
		}
		required("cloth_config") {
			slug(modrinthSlug = "cloth-config", curseforgeSlug = "cloth-config")
			forgeVersionRange = "[${prop("deps.cloth-config")},)"
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
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8", "-Dhbm_m.modelDebug=true", "-Dhbm_m.modelDebugFilter=fluid_tank,centrifuge,geiger_counter_block,ore_acidizer")
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "Forge Server (${stonecutter.active?.version})"
			programArgument("--nogui")
			jvmArguments.addAll("-Xmx4G", "-Xms2G")
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
	flatDir { dirs(rootProject.file("libs")) }
}

dependencies {
	annotationProcessor("org.spongepowered:mixin:${libs.versions.mixin.get()}:processor")

	implementation(libs.moulberry.mixinconstraints)
	jarJar(libs.moulberry.mixinconstraints)

	// Все Forge-моды (особенно сторонние) нужно подключать через mod...,
	// чтобы ModDevGradle принудительно пропустил их через ремаппер.

	// В Kotlin DSL динамические конфигурации оборачиваются в кавычки, либо вызываются через add()
	"modImplementation"("dev.architectury:architectury-forge:${prop("deps.architectury")}")
	jarJar("dev.architectury:architectury-forge:[${prop("deps.architectury")},)")
	"modImplementation"("me.shedaniel.cloth:cloth-config-forge:${prop("deps.cloth-config")}")

	"modCompileOnly"("curse.maven:jei-238222:${prop("deps.jei")}")
	"modRuntimeOnly"("curse.maven:jei-238222:${prop("deps.jei")}")
	"modRuntimeOnly"("curse.maven:embeddium-908741:5681725")
	"modRuntimeOnly"("curse.maven:oculus-581495:6020952")
	"modRuntimeOnly"("curse.maven:modernfix-790626:7515215")
	"modRuntimeOnly"("curse.maven:smooth-boot-reloaded-633412:5016280")
	"modRuntimeOnly"("maven.modrinth:spark:1.10.53-forge")
	"modRuntimeOnly"("curse.maven:screenshot-to-clipboard-326950:3643026")
}

sourceSets {
	main {
		resources.srcDir(rootProject.file("src/generated/resources"))
	}
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}

stonecutter {
}
