import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.*
import java.io.File

// Защита от «выпавших из сборки» классов. Инкрементальная компиляция Gradle иногда
// рассинхронизируется и оставляет в build/classes неполный вывод, помечая его UP-TO-DATE;
// такой jar уезжает на площадки, а у игроков падает рантайм с NoClassDefFoundError
// (реальный случай: com.hbm_m.client.gui.ConfigScreen$ConfigSlider). Задачи ниже сверяют
// ожидаемые имена class-файлов (по объявлениям типов в исходниках main source set) с
// фактическим выводом компиляции. Исходники, исключённые из source set (datagen в NeoForge),
// не проверяются — у source set берётся уже отфильтрованный java.files.

private val CLASS_TOKEN = Regex("""(?<![.\w])(?:@interface|class|interface|enum|record)\s+(\w+)|(\{)|(\})""")
private val PACKAGE_DECLARATION = Regex("""(?m)^\s*package\s+([\w.]+)""")

/** Убирает комментарии и строковые литералы, чтобы «class Foo» в строке или джавадоке не дал ложного объявления. */
private fun stripNonCode(src: String): String {
	val sb = StringBuilder(src.length)
	var i = 0
	val n = src.length
	while (i < n) {
		val c = src[i]
		when {
			c == '/' && i + 1 < n && src[i + 1] == '/' -> {
				while (i < n && src[i] != '\n') i++
				sb.append(' ')
			}
			c == '/' && i + 1 < n && src[i + 1] == '*' -> {
				i += 2
				while (i + 1 < n && !(src[i] == '*' && src[i + 1] == '/')) i++
				i = minOf(i + 2, n)
				sb.append(' ')
			}
			c == '"' && src.startsWith("\"\"\"", i) -> {
				i += 3
				while (i + 2 < n && !src.startsWith("\"\"\"", i)) i += if (src[i] == '\\') 2 else 1
				i = minOf(i + 3, n)
				sb.append(' ')
			}
			c == '"' || c == '\'' -> {
				i++
				while (i < n && src[i] != c) i += if (src[i] == '\\') 2 else 1
				i = minOf(i + 1, n)
				sb.append(' ')
			}
			else -> {
				sb.append(c)
				i++
			}
		}
	}
	return sb.toString()
}

/**
 * Ожидаемые имена class-файлов относительно корня classes-директории:
 * пары (путь без .class, является ли класс top-level). Вложенность считается
 * по скобкам; анонимные классы идут как скобки без имени и баланс не ломают.
 */
private fun expectedClassFiles(stripped: String): List<Pair<String, Boolean>> {
	val pkg = PACKAGE_DECLARATION.find(stripped)?.groupValues?.get(1)?.replace('.', '/')
	val result = mutableListOf<Pair<String, Boolean>>()
	val braceStack = ArrayDeque<String?>()
	var pendingDecl: String? = null
	for (m in CLASS_TOKEN.findAll(stripped)) {
		when {
			m.groups[1] != null -> {
				val chain = braceStack.filterNotNull()
				val simple = (if (chain.isEmpty()) "" else chain.joinToString("$") + "$") + m.groupValues[1]
				result += (if (pkg.isNullOrEmpty()) "" else "$pkg/") + simple to chain.isEmpty()
				pendingDecl = m.groupValues[1]
			}
			m.groups[2] != null -> {
				braceStack.addLast(pendingDecl)
				pendingDecl = null
			}
			else -> braceStack.removeLastOrNull()
		}
	}
	return result
}

/** Локальные классы компилируются с числовым префиксом (Outer$1Name) — допускаем такой вариант имени. */
private fun hasLocalClassVariant(expected: String, classFiles: Set<String>): Boolean {
	val slash = expected.lastIndexOf('/')
	val dir = if (slash < 0) "" else expected.substring(0, slash + 1)
	val simple = expected.substring(slash + 1)
	val outer = simple.substringBefore('$')
	val name = simple.substringAfterLast('$')
	val re = Regex("""^${Regex.escape(outer)}\$[\d]*${Regex.escape(name)}$""")
	return classFiles.any {
		it.startsWith(dir) && it.substring(dir.length).matches(re)
	}
}

private fun analyzeCompiledClasses(sources: Collection<File>, classDirs: FileCollection): List<String> {
	// имена без .class, с '/', относительно корня classes-директории
	val classFiles = sortedSetOf<String>()
	classDirs.forEach { dir ->
		if (dir.isDirectory) dir.walkTopDown()
			.filter { it.isFile && it.name.endsWith(".class") }
			.forEach { classFiles += dir.toPath().relativize(it.toPath()).toString().replace('\\', '/').removeSuffix(".class") }
	}

	val sourceTopLevels = mutableSetOf<String>()
	val problems = mutableListOf<String>()
	for (src in sources) {
		for ((expected, isTop) in expectedClassFiles(stripNonCode(src.readText()))) {
			if (isTop) sourceTopLevels += expected
			if (expected in classFiles) continue
			if (!isTop && hasLocalClassVariant(expected, classFiles)) continue
			problems += "${src.invariantSeparatorsPath}: отсутствует $expected.class"
		}
	}

	// Обратная проверка: .class без исходника — мусор от старых сборок, попадёт в jar.
	for (cf in classFiles) {
		val top = cf.substringBefore('$')
		if (top == "package-info" || top == "module-info") continue
		if (top !in sourceTopLevels) problems += "$cf.class: класс без исходника (остался от старой сборки?)"
	}
	return problems
}

private fun describeProblems(problems: List<String>, repairCommand: String): String {
	val shown = problems.take(30).joinToString("\n  ")
	return buildString {
		appendLine("Сборка неполна: ${problems.size} расхождений между исходниками и выводом компиляции:")
		append("  ").appendLine(shown)
		if (problems.size > 30) appendLine("  …и ещё ${problems.size - 30}")
		append("Починка и пересборка одной командой: ").append(repairCommand)
	}
}

fun Project.registerCompiledClassesGuard(jarTaskName: String) {
	val mainSourceSet = the<SourceSetContainer>().named("main")
	val sources = mainSourceSet.map { it.java }
	val classDirs = mainSourceSet.map { it.output.classesDirs }
	val repairCommand = "./gradlew $path:repairCompiledClasses"

	val verify = tasks.register("verifyCompiledClasses") {
		group = "verification"
		description =
			"Сверяет вывод компиляции с исходниками: ловит классы, выпавшие из сборки из-за рассинхрона инкрементальной компиляции"
		dependsOn(mainSourceSet.map { it.classesTaskName })
		// classesDirs — именно деревом: files() на директории трекает только сам путь,
		// и удаление класса внутри не сбросило бы UP-TO-DATE — битая сборка ушла бы в jar
		inputs.files(sources)
		inputs.files(classDirs.map { it.asFileTree })
		val marker = temporaryDir.resolve("verified.marker")
		outputs.file(marker)
		doLast {
			val problems = analyzeCompiledClasses(sources.get().files, classDirs.get())
			if (problems.isNotEmpty()) {
				throw GradleException(describeProblems(problems, repairCommand))
			}
			marker.writeText(java.time.Instant.now().toString())
		}
	}

	tasks.register("repairCompiledClasses") {
		group = "build"
		description = "При расхождении исходников с выводом компиляции чистит classes и инкрементальное состояние, затем пересобирает"
		doLast {
			val problems = analyzeCompiledClasses(sources.get().files, classDirs.get())
			if (problems.isEmpty()) {
				logger.lifecycle("repairCompiledClasses: вывод компиляции полный, чистить нечего")
				return@doLast
			}
			logger.lifecycle("repairCompiledClasses: ${problems.size} расхождений, сбрасываю classes и инкрементальное состояние")
			classDirs.get().forEach { it.deleteRecursively() }
			file("build/tmp/compileJava").deleteRecursively()
			file("build/tmp/compileKotlin").deleteRecursively()
			// ksp-кэш тоже склонен к порче (PersistentEnumerator storage corrupted) —
			// пересоздаётся компилятором, дешевле удалить
			file("build/kspCaches").deleteRecursively()
		}
		finalizedBy("compileJava", "compileKotlin")
	}

	// Гейт перед упаковкой и перед любым запуском игры: jar пакует ровно то,
	// что лежит в classes-директориях, так что полнота classes = полнота jar.
	tasks.matching { it.name == jarTaskName }.configureEach { dependsOn(verify) }
	tasks.matching { it.name.startsWith("run") }.configureEach { dependsOn(verify) }
}
