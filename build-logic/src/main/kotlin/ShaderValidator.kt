import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File

private val VALID_UNIFORM_TYPES = setOf("int", "float", "matrix2x2", "matrix3x3", "matrix4x4")

private val GLSL_QUALIFIERS = setOf(
	"const", "in", "out", "inout", "uniform", "buffer", "shared",
	"coherent", "volatile", "restrict", "readonly", "writeonly",
	"flat", "smooth", "noperspective", "highp", "mediump", "lowp",
	"precise", "invariant", "layout", "struct", "attribute", "varying"
)

private val GLSL_BUILTIN_TYPES = setOf(
	"void", "bool", "int", "uint", "float", "double", "atomic_uint",
	"vec2", "vec3", "vec4", "dvec2", "dvec3", "dvec4",
	"bvec2", "bvec3", "bvec4", "ivec2", "ivec3", "ivec4", "uvec2", "uvec3", "uvec4",
	"mat2", "mat3", "mat4", "mat2x2", "mat2x3", "mat2x4",
	"mat3x2", "mat3x3", "mat3x4", "mat4x2", "mat4x3", "mat4x4",
	"dmat2", "dmat3", "dmat4", "dmat2x2", "dmat2x3", "dmat2x4",
	"dmat3x2", "dmat3x3", "dmat3x4", "dmat4x2", "dmat4x3", "dmat4x4",
	"sampler1D", "sampler2D", "sampler3D", "samplerCube", "sampler2DRect",
	"sampler1DShadow", "sampler2DShadow", "samplerCubeShadow", "sampler2DRectShadow",
	"sampler1DArray", "sampler2DArray", "sampler1DArrayShadow", "sampler2DArrayShadow",
	"samplerBuffer", "sampler2DMS", "sampler2DMSArray", "samplerCubeArray", "samplerCubeArrayShadow",
	"isampler1D", "isampler2D", "isampler3D", "isamplerCube", "isampler2DRect",
	"isampler1DArray", "isampler2DArray", "isamplerBuffer", "isampler2DMS", "isampler2DMSArray", "isamplerCubeArray",
	"usampler1D", "usampler2D", "usampler3D", "usamplerCube", "usampler2DRect",
	"usampler1DArray", "usampler2DArray", "usamplerBuffer", "usampler2DMS", "usampler2DMSArray", "usamplerCubeArray",
	"image1D", "image2D", "image3D", "imageCube", "image2DRect", "image1DArray", "image2DArray",
	"imageBuffer", "image2DMS", "image2DMSArray", "imageCubeArray",
	"iimage1D", "iimage2D", "iimage3D", "iimageCube", "iimage2DRect", "iimage1DArray", "iimage2DArray",
	"iimageBuffer", "iimage2DMS", "iimage2DMSArray", "iimageCubeArray",
	"uimage1D", "uimage2D", "uimage3D", "uimageCube", "uimage2DRect", "uimage1DArray", "uimage2DArray",
	"uimageBuffer", "uimage2DMS", "uimage2DMSArray", "uimageCubeArray"
)

private val GLSL_CONTROL_KEYWORDS = setOf(
	"if", "else", "for", "while", "do", "switch", "case", "default",
	"return", "break", "continue", "discard"
)

private val ASSIGN_AND_POSTFIX_OPS = setOf(
	"=", "+=", "-=", "*=", "/=", "%=", "<<=", ">>=", "&=", "^=", "|=", "++", "--"
)

fun Project.registerShaderValidator() {
	val shaderRoot = rootProject.file("src/main/resources/assets/hbm_m/shaders")

	val validateTask = tasks.register("validateShaders") {
		group = "verification"
		description = "Validates GLSL shaders (.comp, .vsh, .fsh, .glsl) and shader JSONs at build time"
		inputs.dir(shaderRoot)
		// Всегда запускаем валидацию (занимает ~10 мс, не зависит от кэша Gradle)
		outputs.upToDateWhen { false }

		doLast {
			if (!shaderRoot.isDirectory) return@doLast
			val errors = mutableListOf<String>()

			val glslFiles = shaderRoot.walkTopDown()
				.filter { it.isFile && it.extension in setOf("comp", "vsh", "fsh", "glsl") }
				.toList()

			val glslang = findGlslangValidator()

			for (file in glslFiles) {
				val rel = shaderRoot.toPath().relativize(file.toPath()).toString().replace('\\', '/')
				val raw = file.readText(Charsets.UTF_8)

				for (takeElse in listOf(false, true)) {
					val branchLabel = "$rel (${if (takeElse) "else-branch" else "if-branch"})"
					val branchSource = preprocessBranches(raw, file.parentFile, errors, rel, takeElse)
					validateGlslSyntax(branchSource, branchLabel, file.extension != "glsl", errors)
				}

				if (glslang != null && file.extension in setOf("comp", "vsh", "fsh")) {
					runGlslangValidator(glslang, file, raw, rel, errors)
				}
			}

			val jsonFiles = shaderRoot.walkTopDown()
				.filter { it.isFile && it.extension == "json" && !it.invariantSeparatorsPath.contains("/post/") }
				.toList()

			for (jf in jsonFiles) {
				val rel = shaderRoot.toPath().relativize(jf.toPath()).toString().replace('\\', '/')
				validateShaderJson(jf, rel, shaderRoot, errors)
			}

			if (errors.isNotEmpty()) {
				val report = errors.distinct().joinToString("\n  • ", prefix = "\n  • ")
				throw GradleException("❌ [ShaderValidator] Found ${errors.distinct().size} shader error(s):$report")
			}
		}
	}

	tasks.matching { it.name in setOf("processResources", "compileJava", "verifyCompiledClasses") }.configureEach {
		dependsOn(validateTask)
	}
}

private data class CondFrame(
	val parentActive: Boolean,
	var branchTaken: Boolean,
	var currentActive: Boolean
)

/**
 * Вырезает `//` и `/* ... */` комментарии, сохраняя переводы строк `\n` и колонки (заменяет пробелами).
 */
private fun stripGlslComments(src: String, label: String, errors: MutableList<String>): String {
	val sb = StringBuilder(src.length)
	var i = 0
	val n = src.length
	var line = 1
	while (i < n) {
		val c = src[i]
		if (c == '\n') {
			line++
			sb.append('\n')
			i++
		} else if (c == '/' && i + 1 < n && src[i + 1] == '/') {
			while (i < n && src[i] != '\n') {
				sb.append(' ')
				i++
			}
		} else if (c == '/' && i + 1 < n && src[i + 1] == '*') {
			val startLine = line
			sb.append("  ")
			i += 2
			var closed = false
			while (i + 1 < n) {
				if (src[i] == '*' && src[i + 1] == '/') {
					sb.append("  ")
					i += 2
					closed = true
					break
				}
				if (src[i] == '\n') {
					line++
					sb.append('\n')
				} else {
					sb.append(' ')
				}
				i++
			}
			if (!closed) {
				errors += "$label:$startLine: unclosed /* block comment"
				break
			}
		} else {
			sb.append(c)
			i++
		}
	}
	return sb.toString()
}

private fun preprocessBranches(
	rawSrc: String,
	dir: File,
	errors: MutableList<String>,
	relPath: String,
	takeElseBranch: Boolean
): String {
	val src = stripGlslComments(rawSrc, relPath, errors)
	val lines = src.lines()
	val out = StringBuilder(src.length)
	val condStack = ArrayDeque<CondFrame>()

	for ((idx, line) in lines.withIndex()) {
		val lineNo = idx + 1
		val trimmed = line.trim()

		if (trimmed.startsWith("#include ")) {
			val incMatch = Regex("""#include\s+"([^"]+)"""").find(trimmed)
			if (incMatch == null) {
				if (!takeElseBranch) errors += "$relPath:$lineNo: malformed #include directive: $trimmed"
				out.appendLine()
				continue
			}
			val incFile = File(dir, incMatch.groupValues[1])
			if (!incFile.isFile) {
				if (!takeElseBranch) errors += "$relPath:$lineNo: missing #include file '${incMatch.groupValues[1]}'"
				out.appendLine()
				continue
			}
			val incNoComments = stripGlslComments(incFile.readText(Charsets.UTF_8), incFile.name, errors)
			val incSingleLine = incNoComments.lines()
				.filter { !it.trim().startsWith("#") }
				.joinToString(" ")
			out.appendLine(incSingleLine)
			continue
		}

		when {
			trimmed.startsWith("#if") -> {
				val parentActive = condStack.all { it.currentActive }
				val takeFirst = parentActive && !takeElseBranch
				condStack.addLast(CondFrame(parentActive, branchTaken = takeFirst, currentActive = takeFirst))
				out.appendLine()
			}
			trimmed.startsWith("#elif") -> {
				val top = condStack.lastOrNull()
				if (top == null) {
					if (!takeElseBranch) errors += "$relPath:$lineNo: unmatched $trimmed without #if"
				} else if (top.branchTaken || !top.parentActive) {
					top.currentActive = false
				} else {
					top.currentActive = true
					top.branchTaken = true
				}
				out.appendLine()
			}
			trimmed.startsWith("#else") -> {
				val top = condStack.lastOrNull()
				if (top == null) {
					if (!takeElseBranch) errors += "$relPath:$lineNo: unmatched #else without #if"
				} else {
					top.currentActive = top.parentActive && !top.branchTaken
					top.branchTaken = true
				}
				out.appendLine()
			}
			trimmed.startsWith("#endif") -> {
				if (condStack.isEmpty()) {
					if (!takeElseBranch) errors += "$relPath:$lineNo: unmatched #endif"
				} else {
					condStack.removeLast()
				}
				out.appendLine()
			}
			trimmed.startsWith("#") -> {
				out.appendLine()
			}
			else -> {
				if (condStack.all { it.currentActive }) {
					out.appendLine(line)
				} else {
					out.appendLine()
				}
			}
		}
	}

	if (condStack.isNotEmpty() && !takeElseBranch) {
		errors += "$relPath: unclosed #if/#ifdef directive at end of file"
	}
	return out.toString()
}

private enum class TokKind { IDENT, NUMBER, PUNCT, OP }
private data class GlslToken(val kind: TokKind, val text: String, val line: Int, val col: Int)
private data class BracketToken(val ch: Char, val line: Int, val col: Int)

private val MULTI_CHAR_OPS = listOf(
	"<<=", ">>=", "++", "--", "<=", ">=", "==", "!=", "&&", "||", "<<", ">>",
	"+=", "-=", "*=", "/=", "%=", "&=", "^=", "|="
)

private val SINGLE_CHAR_OPS = setOf('+', '-', '*', '/', '%', '<', '>', '&', '|', '^', '~', '!', '?', ':', '=')
private val PUNCT_CHARS = setOf('{', '}', '(', ')', '[', ']', ';', ',', '.')
private val NUM_REGEX = Regex("""^(?:0[xX][0-9a-fA-F]+[uU]?|(?:[0-9]+\.[0-9]*|\.[0-9]+|[0-9]+)(?:[eE][+-]?[0-9]+)?[fFuU]?)$""")

private fun tokenizeGlsl(src: String, label: String, errors: MutableList<String>): List<GlslToken> {
	val tokens = ArrayList<GlslToken>(512)
	var i = 0
	val n = src.length
	var line = 1
	var col = 1

	while (i < n) {
		val c = src[i]
		if (c == '\n') {
			line++
			col = 1
			i++
			continue
		}
		if (c == ' ' || c == '\t' || c == '\r') {
			col++
			i++
			continue
		}
		// Любой не-ASCII символ вне комментариев (например кириллица "фыфывфы") — мгновенная ошибка GLSL
		if (c.code > 127) {
			val startCol = col
			val bad = StringBuilder()
			while (i < n && src[i].code > 127) {
				bad.append(src[i])
				i++
				col++
			}
			errors += "$label:$line:$startCol: illegal non-ASCII text '$bad' in GLSL code"
			continue
		}

		// Идентификатор
		if (c in 'a'..'z' || c in 'A'..'Z' || c == '_') {
			val startCol = col
			val sb = StringBuilder()
			while (i < n && (src[i] in 'a'..'z' || src[i] in 'A'..'Z' || src[i] in '0'..'9' || src[i] == '_')) {
				sb.append(src[i])
				i++
				col++
			}
			tokens += GlslToken(TokKind.IDENT, sb.toString(), line, startCol)
			continue
		}

		// Числовой литерал
		if (c in '0'..'9' || (c == '.' && i + 1 < n && src[i + 1] in '0'..'9')) {
			val startCol = col
			val sb = StringBuilder()
			while (i < n && (src[i] in '0'..'9' || src[i] in 'a'..'z' || src[i] in 'A'..'Z' || src[i] == '.' || src[i] == '_')) {
				if ((src[i] == 'e' || src[i] == 'E') && i + 1 < n && (src[i + 1] == '+' || src[i + 1] == '-')) {
					sb.append(src[i]).append(src[i + 1])
					i += 2
					col += 2
					continue
				}
				sb.append(src[i])
				i++
				col++
			}
			val numStr = sb.toString()
			if (!NUM_REGEX.matches(numStr)) {
				errors += "$label:$line:$startCol: malformed numeric literal '$numStr'"
			}
			tokens += GlslToken(TokKind.NUMBER, numStr, line, startCol)
			continue
		}

		// Многосимвольные операторы
		var matchedOp: String? = null
		for (op in MULTI_CHAR_OPS) {
			if (src.startsWith(op, i)) {
				matchedOp = op
				break
			}
		}
		if (matchedOp != null) {
			tokens += GlslToken(TokKind.OP, matchedOp, line, col)
			i += matchedOp.length
			col += matchedOp.length
			continue
		}

		if (c in PUNCT_CHARS) {
			tokens += GlslToken(TokKind.PUNCT, c.toString(), line, col)
			i++
			col++
			continue
		}

		if (c in SINGLE_CHAR_OPS) {
			tokens += GlslToken(TokKind.OP, c.toString(), line, col)
			i++
			col++
			continue
		}

		errors += "$label:$line:$col: unexpected character '$c' in GLSL code"
		i++
		col++
	}
	return tokens
}

private fun validateGlslSyntax(
	src: String,
	label: String,
	requireMain: Boolean,
	errors: MutableList<String>
) {
	val initialErrors = errors.size
	val tokens = tokenizeGlsl(src, label, errors)
	if (tokens.isEmpty()) {
		if (requireMain) errors += "$label: shader file is empty"
		return
	}

	// 1. Проверка баланса скобок {}, (), []
	val stack = ArrayDeque<BracketToken>()
	for (t in tokens) {
		if (t.kind != TokKind.PUNCT) continue
		when (t.text) {
			"{", "(", "[" -> stack.addLast(BracketToken(t.text[0], t.line, t.col))
			"}", ")", "]" -> {
				val expectedOpen = when (t.text) {
					"}" -> '{'
					")" -> '('
					else -> '['
				}
				val top = stack.removeLastOrNull()
				if (top == null) {
					errors += "$label:${t.line}:${t.col}: unexpected closing '${t.text}' (no matching '$expectedOpen')"
					return
				} else if (top.ch != expectedOpen) {
					errors += "$label:${t.line}:${t.col}: mismatched '${t.text}' (expected closing for '${top.ch}' opened at line ${top.line}:${top.col})"
					return
				}
			}
		}
	}
	if (stack.isNotEmpty()) {
		val unclosed = stack.last()
		errors += "$label:${unclosed.line}:${unclosed.col}: unclosed '${unclosed.ch}' at end of shader"
		return
	}

	if (requireMain && !Regex("""\bvoid\s+main\s*\(""").containsMatchIn(src)) {
		errors += "$label: missing 'void main()' entry point"
	}

	// 2. Собираем пользовательские struct-типы в шейдере
	val knownTypes = HashSet<String>(GLSL_BUILTIN_TYPES)
	for (idx in 0 until tokens.size - 1) {
		if (tokens[idx].kind == TokKind.IDENT && tokens[idx].text == "struct" && tokens[idx + 1].kind == TokKind.IDENT) {
			knownTypes += tokens[idx + 1].text
		}
	}

	fun isTypeOrQualifier(text: String): Boolean =
		text in GLSL_QUALIFIERS || text in knownTypes

	// 3. Потоковая проверка синтаксиса инструкций, объявлений и выражений
	var braceDepth = 0
	var parenDepth = 0
	var atStmtStart = true

	var i = 0
	val m = tokens.size
	while (i < m) {
		val tok = tokens[i]
		val prev = if (i > 0) tokens[i - 1] else null
		val next = if (i + 1 < m) tokens[i + 1] else null

		// Перед закрывающей '}' в блоке ОБЯЗАНА стоять ';', '{' или '}'
		if (tok.text == "}") {
			if (prev != null && prev.text != ";" && prev.text != "{" && prev.text != "}") {
				errors += "$label:${tok.line}:${tok.col}: missing ';' before '}' (after '${prev.text}' at line ${prev.line}:${prev.col})"
				return
			}
			braceDepth--
			atStmtStart = true
			i++
			continue
		}

		if (tok.text == "{") {
			braceDepth++
			atStmtStart = true
			i++
			continue
		}

		if (tok.text == ";") {
			atStmtStart = true
			i++
			continue
		}

		if (tok.text == "(") parenDepth++
		if (tok.text == ")") {
			parenDepth--
			// Если закрылась скобка условия if (...) / for (...) / while (...) / switch (...),
			// следующий токен начинает тело инструкции
			if (parenDepth == 0 && braceDepth > 0) {
				// Проверим, не было ли перед '(' ключевого слова управления
				var openIdx = i - 1
				var d = 1
				while (openIdx >= 0 && d > 0) {
					if (tokens[openIdx].text == ")") d++
					else if (tokens[openIdx].text == "(") d--
					openIdx--
				}
				if (openIdx >= 0 && tokens[openIdx].text in setOf("if", "for", "while", "switch", "layout")) {
					atStmtStart = true
					i++
					continue
				}
			}
		}

		// Проверка начала инструкции внутри функции/блока (при parenDepth == 0)
		if (atStmtStart && parenDepth == 0 && braceDepth > 0) {
			atStmtStart = false
			if (tok.kind == TokKind.IDENT) {
				when {
					tok.text in setOf("if", "for", "while", "switch") -> {
						if (next == null || next.text != "(") {
							errors += "$label:${tok.line}:${tok.col}: expected '(' after '${tok.text}'"
							return
						}
					}
					tok.text == "else" -> {
						atStmtStart = true
					}
					tok.text == "do" -> {
						atStmtStart = true
					}
					tok.text in setOf("break", "continue", "discard") -> {
						if (next == null || next.text != ";") {
							errors += "$label:${tok.line}:${tok.col}: expected ';' after '${tok.text}'"
							return
						}
					}
					tok.text == "return" -> {
						// return; или return expr;
					}
					tok.text == "case" || tok.text == "default" -> {
						// case expr: или default:
					}
					isTypeOrQualifier(tok.text) -> {
						// Объявление переменной: пропускаем квалификаторы и тип (включая массив типа float[4])
						var j = i
						while (j < m && tokens[j].kind == TokKind.IDENT && isTypeOrQualifier(tokens[j].text)) {
							j++
							while (j < m && tokens[j].text == "[") {
								while (j < m && tokens[j].text != "]") j++
								if (j < m) j++ // skip ']'
							}
						}
						// После типа обязан идти идентификатор имени переменной
						if (j >= m || tokens[j].kind != TokKind.IDENT) {
							val errTok = if (j < m) tokens[j] else tok
							errors += "$label:${errTok.line}:${errTok.col}: expected identifier after type '${tok.text}', found '${errTok.text}'"
							return
						}
						val varTok = tokens[j]
						val afterVar = if (j + 1 < m) tokens[j + 1] else null
						if (afterVar == null || afterVar.text !in setOf(";", "=", ",", "[")) {
							errors += "$label:${varTok.line}:${varTok.col}: expected ';', '=', ',' or '[' after variable '${varTok.text}', found '${afterVar?.text ?: "EOF"}'"
							return
						}
						i = j
						continue
					}
					else -> {
						// Инструкция-выражение, начинающаяся с идентификатора (вызов функции или присваивание переменной):
						// Следующий токен ОБЯЗАН быть '(', '[', '.' или оператором присваивания/инкремента!
						if (next == null || (next.text !in setOf("(", "[", ".") && next.text !in ASSIGN_AND_POSTFIX_OPS)) {
							errors += "$label:${tok.line}:${tok.col}: invalid statement or unknown type '${tok.text}' (followed by '${next?.text ?: "EOF"}')"
							return
						}
					}
				}
			} else if (tok.kind == TokKind.NUMBER) {
				errors += "$label:${tok.line}:${tok.col}: statement cannot start with numeric literal '${tok.text}'"
				return
			}
		} else if (prev != null) {
			// Внутри выражения: два идентификатора подряд или число+идентификатор без оператора — всегда синтаксическая ошибка!
			if (tok.kind == TokKind.IDENT && prev.kind == TokKind.IDENT) {
				val legalPair = (prev.text == "else" && tok.text == "if")
						|| isTypeOrQualifier(prev.text)
						|| (prev.text == "return" || prev.text == "case")
				if (!legalPair) {
					errors += "$label:${tok.line}:${tok.col}: unexpected identifier '${tok.text}' after '${prev.text}' (missing ';' or operator?)"
					return
				}
			}
			if (prev.kind == TokKind.NUMBER && (tok.kind == TokKind.IDENT || tok.kind == TokKind.NUMBER)) {
				errors += "$label:${tok.line}:${tok.col}: unexpected '${tok.text}' immediately after number '${prev.text}'"
				return
			}
			// Ключевые слова инструкций (if, for, while, return, discard) не могут идти внутри выражения без ';'
			if (tok.kind == TokKind.IDENT && tok.text in GLSL_CONTROL_KEYWORDS && tok.text != "else") {
				if (prev.text !in setOf(";", "{", "}", ")", ":", "else")) {
					errors += "$label:${tok.line}:${tok.col}: unexpected '${tok.text}' after '${prev.text}' (missing ';'?)"
					return
				}
			}
		}

		i++
	}
}

private fun validateShaderJson(
	jsonFile: File,
	rel: String,
	shaderRoot: File,
	errors: MutableList<String>
) {
	val map = try {
		@Suppress("UNCHECKED_CAST")
		JsonSlurper().parse(jsonFile) as? Map<String, Any?>
	} catch (e: Exception) {
		errors += "$rel: invalid JSON syntax (${e.message})"
		return
	} ?: return

	val isProgramJson = jsonFile.invariantSeparatorsPath.contains("/program/")
	val folder = if (isProgramJson) "program" else "core"

	for ((key, ext) in listOf("vertex" to "vsh", "fragment" to "fsh")) {
		val ref = map[key] as? String ?: continue
		if (!ref.startsWith("hbm_m:")) continue
		val path = ref.removePrefix("hbm_m:")
		val target = File(shaderRoot, "$folder/$path.$ext")
		if (!target.isFile && path != "block_lit_instanced") {
			errors += "$rel: references missing $key shader '$ref' (expected ${target.relativeTo(shaderRoot).invariantSeparatorsPath})"
		}
	}

	@Suppress("UNCHECKED_CAST")
	val uniforms = map["uniforms"] as? List<Map<String, Any?>> ?: return
	for (u in uniforms) {
		val uName = u["name"] as? String ?: continue
		val uType = u["type"] as? String ?: continue
		val uCount = (u["count"] as? Number)?.toInt() ?: continue
		val uValues = u["values"] as? List<*> ?: continue
		if (uType !in VALID_UNIFORM_TYPES) {
			errors += "$rel: uniform '$uName' has invalid vanilla type '$uType' (allowed: $VALID_UNIFORM_TYPES)"
		}
		if (uValues.size != uCount) {
			errors += "$rel: uniform '$uName' declares count=$uCount but has ${uValues.size} values"
		}
	}
}

private fun findGlslangValidator(): String? {
	val candidates = listOf("glslangValidator", "glslangValidator.exe")
	val pathDirs = (System.getenv("PATH") ?: "").split(File.pathSeparator)
	for (dir in pathDirs) {
		for (c in candidates) {
			val f = File(dir, c)
			if (f.isFile && f.canExecute()) return f.absolutePath
		}
	}
	return null
}

private fun runGlslangValidator(
	glslangPath: String,
	file: File,
	rawSource: String,
	rel: String,
	errors: MutableList<String>
) {
	val stage = when (file.extension) {
		"comp" -> "comp"
		"vsh" -> "vert"
		"fsh" -> "frag"
		else -> return
	}
	var patched = rawSource.replace(
		Regex("""#moj_import\s*<fog\.glsl>"""),
		"vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor) { return inColor; }"
	)
	patched = Regex("""#include\s+"([^"]+)"""").replace(patched) { m ->
		val inc = File(file.parentFile, m.groupValues[1])
		if (inc.isFile) inc.readText(Charsets.UTF_8) else ""
	}
	val tmp = File.createTempFile("hbm_shader_", ".$stage")
	try {
		tmp.writeText(patched, Charsets.UTF_8)
		val proc = ProcessBuilder(glslangPath, "-S", stage, tmp.absolutePath)
			.redirectErrorStream(true)
			.start()
		val output = proc.inputStream.bufferedReader().readText()
		val code = proc.waitFor()
		if (code != 0) {
			val msg = output.lines()
				.filter { it.contains("ERROR:") }
				.joinToString("; ")
				.ifEmpty { output.trim() }
			errors += "$rel [glslangValidator]: $msg"
		}
	} finally {
		tmp.delete()
	}
}