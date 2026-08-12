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
}

dependencies {
	implementation(libs.moulberry.mixinconstraints)
	jarJar(libs.moulberry.mixinconstraints)
	implementation("dev.architectury:architectury-neoforge:${prop("deps.architectury")}")
	val mcVer = stonecutter.current.version
	"compileOnly"("com.simibubi.create:create-$mcVer:${prop("deps.create")}:slim") {
		isTransitive = false
	}
	// В NeoForge артефакт называется flywheel-neoforge-api 
	"compileOnly"("dev.engine-room.flywheel:flywheel-neoforge-api-$mcVer:${prop("deps.flywheel")}")
	"compileOnly"("curse.maven:jei-238222:${prop("deps.jei")}")
	"runtimeOnly"("curse.maven:jei-238222:${prop("deps.jei")}")
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}

sourceSets {
	main {
		java {
			exclude("com/hbm_m/datagen/**")
		}
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
	options.encoding = "UTF-8"
}

stonecutter {
	val isModern = current.parsed >= "1.21.11"

	replacements.regex(isModern) {
		replace("\\bResourceLocation\\b", "Identifier")
		reversePattern.set("\\bIdentifier\\b")
		reverseValue.set("ResourceLocation")
	}

	replacements.regex(isModern) {
		replace("\\blocation\\(\\)", "identifier()")
		reversePattern.set("\\bidentifier\\(\\)")
		reverseValue.set("location()")
	}

	replacements.regex(isModern) {
		replace("net\\.minecraft\\.resources\\.ResourceLocation", "net.minecraft.util.Identifier")
		reversePattern.set("net\\.minecraft\\.util\\.Identifier")
		reverseValue.set("net.minecraft.resources.ResourceLocation")
	}
}
