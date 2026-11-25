import org.gradle.api.Project
import org.gradle.kotlin.dsl.named
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

private fun triangulatePolygon(verts: Array<FloatArray>): IntArray {
	val n = verts.size
	if (n < 3) return IntArray(0)
	if (n == 3) return intArrayOf(0, 1, 2)

	var nx = 0.0; var ny = 0.0; var nz = 0.0
	for (i in 0 until n) {
		val j = (i + 1) % n
		val vi = verts[i]; val vj = verts[j]
		nx += (vi[1] - vj[1]).toDouble() * (vi[2] + vj[2]).toDouble()
		ny += (vi[2] - vj[2]).toDouble() * (vi[0] + vj[0]).toDouble()
		nz += (vi[0] - vj[0]).toDouble() * (vi[1] + vj[1]).toDouble()
	}
	val lenSq = nx * nx + ny * ny + nz * nz
	if (lenSq < 1e-12) {
		val fan = IntArray((n - 2) * 3)
		var idx = 0
		for (i in 1 until n - 1) {
			fan[idx++] = 0; fan[idx++] = i; fan[idx++] = i + 1
		}
		return fan
	}

	val absX = Math.abs(nx); val absY = Math.abs(ny); val absZ = Math.abs(nz)
	val u = DoubleArray(n); val v = DoubleArray(n)
	if (absZ >= absX && absZ >= absY) {
		for (i in 0 until n) { u[i] = verts[i][0].toDouble(); v[i] = verts[i][1].toDouble() }
	} else if (absY >= absX && absY >= absZ) {
		for (i in 0 until n) { u[i] = verts[i][0].toDouble(); v[i] = verts[i][2].toDouble() }
	} else {
		for (i in 0 until n) { u[i] = verts[i][1].toDouble(); v[i] = verts[i][2].toDouble() }
	}

	var area2D = 0.0
	for (i in 0 until n) {
		val j = (i + 1) % n
		area2D += (u[i] * v[j] - u[j] * v[i])
	}
	if (Math.abs(area2D) < 1e-12) {
		val fan = IntArray((n - 2) * 3)
		var idx = 0
		for (i in 1 until n - 1) {
			fan[idx++] = 0; fan[idx++] = i; fan[idx++] = i + 1
		}
		return fan
	}
	val areaSign = if (area2D > 0) 1.0 else -1.0

	fun cross2D(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Double =
		(bx - ax) * (cy - ay) - (by - ay) * (cx - ax)

	fun pointInTriangle(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Boolean {
		val s1 = cross2D(ax, ay, bx, by, px, py) * areaSign
		val s2 = cross2D(bx, by, cx, cy, px, py) * areaSign
		val s3 = cross2D(cx, cy, ax, ay, px, py) * areaSign
		return s1 >= -1e-7 && s2 >= -1e-7 && s3 >= -1e-7
	}

	val active = IntArray(n) { it }
	var m = n
	val out = IntArray((n - 2) * 3)
	var triCursor = 0

	while (m > 3) {
		var earIdx = -1
		for (k in 0 until m) {
			val prev = active[(k - 1 + m) % m]
			val curr = active[k]
			val next = active[(k + 1) % m]
			val c = cross2D(u[prev], v[prev], u[curr], v[curr], u[next], v[next])
			if (c * areaSign <= 1e-7) continue

			var ear = true
			for (j in 0 until m) {
				val p = active[j]
				if (p == prev || p == curr || p == next) continue
				if (pointInTriangle(u[p], v[p], u[prev], v[prev], u[curr], v[curr], u[next], v[next])) {
					ear = false
					break
				}
			}
			if (ear) {
				earIdx = k
				break
			}
		}

		if (earIdx < 0) {
			var maxCross = -Double.MAX_VALUE
			var bestK = 0
			for (k in 0 until m) {
				val prev = active[(k - 1 + m) % m]
				val curr = active[k]
				val next = active[(k + 1) % m]
				val sc = cross2D(u[prev], v[prev], u[curr], v[curr], u[next], v[next]) * areaSign
				if (sc > maxCross) {
					maxCross = sc
					bestK = k
				}
			}
			earIdx = bestK
		}

		val prev = active[(earIdx - 1 + m) % m]
		val curr = active[earIdx]
		val next = active[(earIdx + 1) % m]
		out[triCursor++] = prev
		out[triCursor++] = curr
		out[triCursor++] = next

		for (i in earIdx until m - 1) {
			active[i] = active[i + 1]
		}
		m--
	}

	out[triCursor++] = active[0]
	out[triCursor++] = active[1]
	out[triCursor++] = active[2]
	return out
}

private fun floatToHalf(f: Float): Short {
	val bits = java.lang.Float.floatToRawIntBits(f)
	val sign = (bits ushr 16) and 0x8000
	val exp = (bits ushr 23) and 0xFF
	val mant = bits and 0x007FFFFF
	if (exp == 255) {
		return (sign or 0x7C00 or (if (mant != 0) 0x0200 or (mant ushr 13) else 0)).toShort()
	}
	val e = exp - 127 + 15
	if (e >= 31) return (sign or 0x7C00).toShort()
	if (e <= 0) {
		if (e < -10) return sign.toShort()
		val m = mant or 0x00800000
		val shift = 126 - exp
		var half = m shr shift
		val rem = m and ((1 shl shift) - 1)
		val halfPoint = 1 shl (shift - 1)
		if (rem > halfPoint || (rem == halfPoint && (half and 1) != 0)) half++
		return (sign or half).toShort()
	}
	var half = mant shr 13
	val rem = mant and 0x1FFF
	if (rem > 0x1000 || (rem == 0x1000 && (half and 1) != 0)) half++
	var res = (e shl 10) + half
	if (res >= 0x7C00) res = 0x7C00
	return (sign or res).toShort()
}

private fun quantizeNormalByte(n: Float): Byte =
	Math.round(n.coerceIn(-1f, 1f) * 127f).coerceIn(-127, 127).toByte()

private class VertexWelder {
	private var verts = IntArray(64 * 8) // raw int bits of 8 floats per vertex
	private var table = IntArray(128) { -1 }
	var size = 0
		private set

	private fun growTable(newCap: Int) {
		val newTable = IntArray(newCap) { -1 }
		val mask = newCap - 1
		for (i in 0 until size) {
			val b = i * 8
			var slot = hash(verts[b], verts[b + 1], verts[b + 2], verts[b + 3],
				verts[b + 4], verts[b + 5], verts[b + 6], verts[b + 7]) and mask
			while (newTable[slot] >= 0) slot = (slot + 1) and mask
			newTable[slot] = i
		}
		table = newTable
	}

	private fun hash(w0: Int, w1: Int, w2: Int, w3: Int, w4: Int, w5: Int, w6: Int, w7: Int): Int {
		var h = w0 * 31 + w1
		h = h * 31 + w2
		h = h * 31 + w3
		h = h * 31 + w4
		h = h * 31 + w5
		h = h * 31 + w6
		h = h * 31 + w7
		return h xor (h ushr 16)
	}

	fun intern(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float, v: Float): Int {
		if (size * 2 >= table.size) growTable(table.size * 2)
		if ((size + 1) * 8 > verts.size) verts = verts.copyOf(verts.size * 2)
		val w0 = java.lang.Float.floatToRawIntBits(if (x == 0f) 0f else x)
		val w1 = java.lang.Float.floatToRawIntBits(if (y == 0f) 0f else y)
		val w2 = java.lang.Float.floatToRawIntBits(if (z == 0f) 0f else z)
		val w3 = java.lang.Float.floatToRawIntBits(if (nx == 0f) 0f else nx)
		val w4 = java.lang.Float.floatToRawIntBits(if (ny == 0f) 0f else ny)
		val w5 = java.lang.Float.floatToRawIntBits(if (nz == 0f) 0f else nz)
		val w6 = java.lang.Float.floatToRawIntBits(if (u == 0f) 0f else u)
		val w7 = java.lang.Float.floatToRawIntBits(if (v == 0f) 0f else v)

		val mask = table.size - 1
		var slot = hash(w0, w1, w2, w3, w4, w5, w6, w7) and mask
		while (true) {
			val cand = table[slot]
			if (cand < 0) {
				val idx = size++
				val b = idx * 8
				verts[b] = w0; verts[b + 1] = w1; verts[b + 2] = w2
				verts[b + 3] = w3; verts[b + 4] = w4; verts[b + 5] = w5
				verts[b + 6] = w6; verts[b + 7] = w7
				table[slot] = idx
				return idx
			}
			val b = cand * 8
			if (verts[b] == w0 && verts[b + 1] == w1 && verts[b + 2] == w2 &&
				verts[b + 3] == w3 && verts[b + 4] == w4 && verts[b + 5] == w5 &&
				verts[b + 6] == w6 && verts[b + 7] == w7) {
				return cand
			}
			slot = (slot + 1) and mask
		}
	}

	fun floatAt(vIdx: Int, comp: Int): Float =
		java.lang.Float.intBitsToFloat(verts[vIdx * 8 + comp])
}

private fun resolveMtlFile(objFile: File, mtlRef: String, nucleusAssetsRoots: List<File>): File? {
	if (mtlRef.contains(":")) {
		val parts = mtlRef.split(":", limit = 2)
		for (r in nucleusAssetsRoots) {
			val f = File(r, "${parts[0]}/${parts[1]}")
			if (f.isFile) return f
		}
	} else {
		val f = File(objFile.parentFile, mtlRef)
		if (f.isFile) return f
		for (r in nucleusAssetsRoots) {
			val relF = File(r, mtlRef)
			if (relF.isFile) return relF
		}
	}
	return null
}

private fun compileObjToNmesh(objFile: File, outFile: File, nucleusAssetsRoots: List<File>) {
	fun normalize(x: Float, y: Float, z: Float): FloatArray {
		val len = Math.sqrt(x.toDouble() * x + y.toDouble() * y + z.toDouble() * z)
		if (len == 0.0) return floatArrayOf(0f, 0f, 0f)
		return floatArrayOf((x / len).toFloat(), (y / len).toFloat(), (z / len).toFloat())
	}

	data class MtlInfo(val mapKd: String, val kd: List<Float>, val ka: List<Float>)
	val mtlMap = mutableMapOf<String, MtlInfo>()
	fun parseMtlColor(args: String): List<Float> {
		val p = args.split(Regex("\\s+"))
		fun f(i: Int): Float = if (i < p.size) (p[i].toFloatOrNull() ?: 0f) else 0f
		return listOf(f(0), f(1), f(2))
	}
	fun parseMtl(file: File) {
		var curMtl = ""
		var mapKd = ""
		var kd = listOf(1f, 1f, 1f)
		var ka = listOf(0f, 0f, 0f)
		file.forEachLine(Charsets.UTF_8) { raw ->
			val l = raw.trim()
			if (l.isEmpty() || l.startsWith("#")) return@forEachLine
			when {
				l.startsWith("newmtl ") -> {
					if (curMtl.isNotEmpty()) mtlMap[curMtl] = MtlInfo(mapKd, kd, ka)
					curMtl = l.substring(7).trim()
					mapKd = ""
					kd = listOf(1f, 1f, 1f)
					ka = listOf(0f, 0f, 0f)
				}
				l.startsWith("map_Kd ") && curMtl.isNotEmpty() -> mapKd = l.substring(7).trim()
				l.startsWith("Kd ") && curMtl.isNotEmpty() -> kd = parseMtlColor(l.substring(3).trim())
				l.startsWith("Ka ") && curMtl.isNotEmpty() -> ka = parseMtlColor(l.substring(3).trim())
			}
		}
		if (curMtl.isNotEmpty()) mtlMap[curMtl] = MtlInfo(mapKd, kd, ka)
	}

	objFile.useLines(Charsets.UTF_8) { lines ->
		for (raw in lines) {
			val l = raw.trim()
			if (l.startsWith("mtllib ")) {
				val f = resolveMtlFile(objFile, l.substring(7).trim(), nucleusAssetsRoots)
				if (f != null && f.isFile) parseMtl(f)
			}
		}
	}
	if (mtlMap.isEmpty()) {
		val fallback = File(objFile.parentFile, objFile.nameWithoutExtension + ".mtl")
		if (fallback.isFile) parseMtl(fallback)
	}

	data class BucketKey(val obj: String, val grp: String, val mat: String, val mapKd: String)

	val pos = mutableListOf<FloatArray>()
	val uv = mutableListOf<FloatArray>()
	val nrm = mutableListOf<FloatArray>()
	var objName = ""
	var grpName = ""
	var matName = ""
	var mapKdName = ""
	var facesSinceLastO = false

	val order = mutableListOf<BucketKey>()
	val bucketWelders = linkedMapOf<BucketKey, VertexWelder>()
	val bucketTris = linkedMapOf<BucketKey, MutableList<IntArray>>()

	fun bucket(key: BucketKey): Pair<VertexWelder, MutableList<IntArray>> {
		var w = bucketWelders[key]
		if (w == null) {
			order.add(key)
			w = VertexWelder()
			bucketWelders[key] = w
			bucketTris[key] = mutableListOf()
		}
		return Pair(w, bucketTris[key]!!)
	}

	fun addFace(corners: List<Triple<Int, Int, Int>>) {
		if (corners.size < 3) return
		val p0 = pos[corners[0].first]
		val p1 = pos[corners[1].first]
		val p2 = pos[corners[2].first]
		val e1x = p1[0] - p0[0]; val e1y = p1[1] - p0[1]; val e1z = p1[2] - p0[2]
		val e2x = p2[0] - p0[0]; val e2y = p2[1] - p0[1]; val e2z = p2[2] - p0[2]
		val flat = normalize(e1y * e2z - e1z * e2y, e1z * e2x - e1x * e2z, e1x * e2y - e1y * e2x)
		val key = BucketKey(objName, grpName, matName, mapKdName)
		val (bw, bt) = bucket(key)
		val idxs = IntArray(corners.size)
		for ((i, c) in corners.withIndex()) {
			val pp = pos[c.first]
			val tt = if (c.second >= 0 && c.second < uv.size) uv[c.second] else floatArrayOf(0f, 0f)
			val nn = if (c.third >= 0 && c.third < nrm.size) {
				val n = nrm[c.third]; normalize(n[0], n[1], n[2])
			} else flat
			idxs[i] = bw.intern(pp[0], pp[1], pp[2], nn[0], nn[1], nn[2], tt[0], tt[1])
		}
		val faceVerts = Array(corners.size) { i -> pos[corners[i].first] }
		val triIndices = triangulatePolygon(faceVerts)
		for (i in 0 until triIndices.size step 3) {
			bt.add(intArrayOf(idxs[triIndices[i]], idxs[triIndices[i + 1]], idxs[triIndices[i + 2]]))
		}
	}

	objFile.useLines(Charsets.UTF_8) { lines ->
		for (raw in lines) {
			val line = raw.trim()
			if (line.isEmpty() || line.startsWith("#")) continue
			val t = line.split(Regex("\\s+"))
			when (t[0]) {
				"v" -> pos.add(floatArrayOf(t[1].toFloat(), t[2].toFloat(), t[3].toFloat()))
				"vt" -> uv.add(floatArrayOf(t[1].toFloat(), t[2].toFloat()))
				"vn" -> nrm.add(normalize(t[1].toFloat(), t[2].toFloat(), t[3].toFloat()))
				"o" -> {
					objName = if (t.size > 1) line.substring(2).trim() else ""
					grpName = ""
					facesSinceLastO = false
				}
				"g" -> {
					if (facesSinceLastO) {
						objName = ""
						facesSinceLastO = false
					}
					grpName = if (t.size > 1) line.substring(2).trim() else ""
				}
				"usemtl" -> {
					matName = if (t.size > 1) line.substring(7).trim() else ""
					mapKdName = mtlMap[matName]?.mapKd ?: ""
				}
				"f" -> {
					facesSinceLastO = true
					if (t.size < 4) continue
					fun idx(s: String, n: Int): Int {
						if (s.isEmpty()) return -1
						val v = s.toInt()
						return if (v > 0) v - 1 else n + v
					}
					val corners = ArrayList<Triple<Int, Int, Int>>(t.size - 1)
					for (i in 1 until t.size) {
						val p = t[i].split("/")
						corners.add(Triple(
							idx(p[0], pos.size),
							if (p.size > 1) idx(p[1], uv.size) else -1,
							if (p.size > 2) idx(p[2], nrm.size) else -1,
						))
					}
					addFace(corners)
				}
			}
		}
	}

	outFile.parentFile.mkdirs()
	fun strBytes(s: String): ByteArray = s.toByteArray(Charsets.UTF_8)
	var totalBytes = 12
	for (key in order) {
		val bw = bucketWelders[key]!!
		val bt = bucketTris[key]!!
		val vCount = bw.size
		val idxCount = bt.size * 3
		val idxStride = if (vCount <= 256) 1 else if (vCount <= 65535) 2 else 4
		totalBytes += 2 + strBytes(key.obj).size
		totalBytes += 2 + strBytes(key.grp).size
		totalBytes += 2 + strBytes(key.mat).size
		totalBytes += 2 + strBytes(key.mapKd).size
		totalBytes += 24 + 24 + 8
		totalBytes += vCount * 12 // positions: 3 * float32
		totalBytes += vCount * 8  // uvs: 2 * float32
		totalBytes += vCount * 3  // normals: 3 * snorm8
		totalBytes += idxCount * idxStride
	}

	val buf = ByteBuffer.allocate(totalBytes).order(ByteOrder.LITTLE_ENDIAN)
	buf.putInt(0x4E4D5348)
	buf.putInt(4)
	buf.putInt(order.size)

	fun writeStr(s: String) {
		val b = strBytes(s)
		buf.putShort(b.size.toShort())
		buf.put(b)
	}

	for (key in order) {
		val bw = bucketWelders[key]!!
		val bt = bucketTris[key]!!
		val vCount = bw.size
		val idxCount = bt.size * 3
		val mtl = mtlMap[key.mat]
		val kd = mtl?.kd ?: listOf(1f, 1f, 1f)
		val ka = mtl?.ka ?: listOf(0f, 0f, 0f)
		writeStr(key.obj); writeStr(key.grp); writeStr(key.mat); writeStr(key.mapKd)
		buf.putFloat(kd[0]); buf.putFloat(kd[1]); buf.putFloat(kd[2])
		buf.putFloat(ka[0]); buf.putFloat(ka[1]); buf.putFloat(ka[2])

		var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
		var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
		for (vi in 0 until vCount) {
			val vx = bw.floatAt(vi, 0); val vy = bw.floatAt(vi, 1); val vz = bw.floatAt(vi, 2)
			if (vx < minX) minX = vx; if (vy < minY) minY = vy; if (vz < minZ) minZ = vz
			if (vx > maxX) maxX = vx; if (vy > maxY) maxY = vy; if (vz > maxZ) maxZ = vz
		}
		if (vCount == 0) { minX = 0f; minY = 0f; minZ = 0f; maxX = 0f; maxY = 0f; maxZ = 0f }
		buf.putFloat(minX); buf.putFloat(minY); buf.putFloat(minZ)
		buf.putFloat(maxX); buf.putFloat(maxY); buf.putFloat(maxZ)
		buf.putInt(vCount)
		buf.putInt(idxCount)

		// Stream 1: positions (3 * float32)
		val fb = buf.asFloatBuffer()
		for (vi in 0 until vCount) {
			fb.put(bw.floatAt(vi, 0))
			fb.put(bw.floatAt(vi, 1))
			fb.put(bw.floatAt(vi, 2))
		}
		buf.position(buf.position() + vCount * 12)

		// Stream 2: UVs (2 * float32)
		val uvb = buf.asFloatBuffer()
		for (vi in 0 until vCount) {
			uvb.put(bw.floatAt(vi, 6))
			uvb.put(bw.floatAt(vi, 7))
		}
		buf.position(buf.position() + vCount * 8)

		// Stream 3: normals (3 * snorm8)
		for (vi in 0 until vCount) {
			buf.put(quantizeNormalByte(bw.floatAt(vi, 3)))
			buf.put(quantizeNormalByte(bw.floatAt(vi, 4)))
			buf.put(quantizeNormalByte(bw.floatAt(vi, 5)))
		}

		// Stream 4: delta-encoded indices
		var prev = 0
		if (vCount <= 256) {
			for (tri in bt) {
				for (i in 0..2) {
					val idx = tri[i]
					buf.put(((idx - prev) and 0xFF).toByte())
					prev = idx
				}
			}
		} else if (vCount <= 65535) {
			val sb = buf.asShortBuffer()
			for (tri in bt) {
				for (i in 0..2) {
					val idx = tri[i]
					sb.put((idx - prev).toShort())
					prev = idx
				}
			}
			buf.position(buf.position() + idxCount * 2)
		} else {
			val idxBuf = buf.asIntBuffer()
			for (tri in bt) {
				for (i in 0..2) {
					val idx = tri[i]
					idxBuf.put(idx - prev)
					prev = idx
				}
			}
			buf.position(buf.position() + idxCount * 4)
		}
	}

	FileOutputStream(outFile).use { it.write(buf.array()) }
}

private fun nmeshVersionMatches(f: File): Boolean {
	if (f.length() < 8) return false
	return RandomAccessFile(f, "r").use { raf ->
		val header = ByteArray(8)
		raf.readFully(header)
		ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(4) == 4
	}
}

private fun maxMtlTimestamp(objFile: File, nucleusAssetsRoots: List<File>): Long {
	var maxT = 0L
	val fallback = File(objFile.parentFile, objFile.nameWithoutExtension + ".mtl")
	if (fallback.isFile) maxT = fallback.lastModified()
	objFile.useLines(Charsets.UTF_8) { lines ->
		for (raw in lines) {
			val l = raw.trim()
			if (l.startsWith("mtllib ")) {
				val f = resolveMtlFile(objFile, l.substring(7).trim(), nucleusAssetsRoots)
				if (f != null && f.isFile && f.lastModified() > maxT) {
					maxT = f.lastModified()
				}
			}
		}
	}
	return maxT
}

fun Project.registerNucleusMeshCompiler(mcVersion: String) {
	val nucleusAssetsRoots = listOf(
		rootProject.file("src/main/resources/assets"),
		rootProject.file("src/generated/resources/assets"),
	)
	val nucleusNmeshOut = layout.buildDirectory.dir("generated/nmesh")

	val compileMeshesTask = tasks.register("compileNucleusMeshes") {
		group = "build"
		description = "Compiles assets/**/*.obj into binary .nmesh (NMeshData format) under build/generated/nmesh"
		nucleusAssetsRoots.filter { it.isDirectory }.forEach { inputs.dir(it) }
		outputs.dir(nucleusNmeshOut)
		doLast {
			val outDir = nucleusNmeshOut.get().asFile
			var compiled = 0
			var upToDate = 0
			for (root in nucleusAssetsRoots) {
				if (!root.isDirectory) continue
				val rootNorm = root.parentFile.absolutePath.replace('\\', '/')
				root.walkTopDown().filter { it.isFile && it.extension == "obj" }.forEach { obj ->
					val rel = obj.absolutePath.replace('\\', '/').removePrefix("$rootNorm/")
					val out = File(outDir, rel.removeSuffix(".obj") + ".nmesh")
					val mtlTime = maxMtlTimestamp(obj, nucleusAssetsRoots)
					val srcTime = maxOf(obj.lastModified(), mtlTime)
					if (out.exists() && out.lastModified() >= srcTime && nmeshVersionMatches(out)) {
						upToDate++
						return@forEach
					}
					compileObjToNmesh(obj, out, nucleusAssetsRoots)
					compiled++
				}
			}
			if (outDir.isDirectory) {
				val objRoots = nucleusAssetsRoots.filter { it.isDirectory }.map { it.parentFile.absolutePath.replace('\\', '/') }
				outDir.walkTopDown().filter { it.isFile && it.extension == "nmesh" }.forEach { f ->
					val relNmesh = f.absolutePath.replace('\\', '/').removePrefix(outDir.absolutePath.replace('\\', '/') + "/")
					val relObj = relNmesh.removeSuffix(".nmesh") + ".obj"
					if (objRoots.none { File("$it/$relObj").isFile }) f.delete()
				}
			}
			println("compileNucleusMeshes: compiled $compiled, up-to-date $upToDate -> $outDir")
		}
	}

	tasks.named<ProcessResources>("processResources") {
		from(rootProject.file("src/main/resources/aw/$mcVersion.cfg")) {
			rename { "accesstransformer.cfg" }
			into("META-INF")
		}
		exclude("aw/")
		dependsOn(compileMeshesTask)
		from(nucleusNmeshOut)
		exclude("**/*.obj", "**/*.mtl")
	}
}
