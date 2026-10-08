package com.j41k.fridamanager.repack

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Patcher del pool de cadenas de un AndroidManifest.xml compilado (Android binary XML).
 *
 * En lugar de reconstruir el APK con aapt, se edita in situ el pool de cadenas del
 * manifiesto binario para sustituir el
 * nombre de paquete y los nombres de componentes marcadores. Todo lo demás del chunk
 * (árbol XML, mapa de recursos) permanece intacto.
 *
 * ⚠ Esta clase manipula bytes a bajo nivel: valídala en dispositivo (patch → firma →
 * `pm install`) antes de confiar en ella. Soporta pools UTF-8 y UTF-16; aborta
 * (devuelve null) si el manifiesto trae estilos, caso que los manifiestos no tienen.
 */
object AXML {

    private const val TYPE_XML = 0x0003
    private const val TYPE_STRING_POOL = 0x0001
    private const val FLAG_UTF8 = 0x00000100

    /**
     * Devuelve una copia del manifiesto con cada cadena transformada por [map], o null
     * si el formato no es el esperado.
     */
    fun patchStrings(input: ByteArray, map: (String) -> String): ByteArray? {
        val buf = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN)
        if (buf.getShort(0).toInt() and 0xFFFF != TYPE_XML) return null

        val poolStart = 8
        if (buf.getShort(poolStart).toInt() and 0xFFFF != TYPE_STRING_POOL) return null

        val poolSize = buf.getInt(poolStart + 4)
        val stringCount = buf.getInt(poolStart + 8)
        val styleCount = buf.getInt(poolStart + 12)
        if (styleCount != 0) return null // los manifiestos no usan estilos
        val flags = buf.getInt(poolStart + 16)
        val stringsStart = buf.getInt(poolStart + 20)
        val isUtf8 = flags and FLAG_UTF8 != 0

        val offsetsBase = poolStart + 28
        val dataBase = poolStart + stringsStart

        val strings = ArrayList<String>(stringCount)
        for (i in 0 until stringCount) {
            val off = buf.getInt(offsetsBase + i * 4)
            strings.add(decode(buf, dataBase + off, isUtf8))
        }

        // Reconstruir el pool con las cadenas transformadas.
        val newOffsets = IntArray(stringCount)
        val data = ByteArrayOutputStream()
        for (i in 0 until stringCount) {
            newOffsets[i] = data.size()
            encode(data, map(strings[i]), isUtf8)
        }
        var stringData = data.toByteArray()
        // Alineación a 4 bytes del bloque de datos.
        val pad = (4 - stringData.size % 4) % 4
        if (pad != 0) stringData += ByteArray(pad)

        val newStringsStart = 28 + stringCount * 4
        val newPoolSize = newStringsStart + stringData.size
        val tailStart = poolStart + poolSize
        val tail = input.copyOfRange(tailStart, input.size)

        val out = ByteBuffer
            .allocate(poolStart + newPoolSize + tail.size)
            .order(ByteOrder.LITTLE_ENDIAN)

        // Cabecera de fichero XML (8 bytes), con el tamaño total actualizado.
        out.putShort(TYPE_XML.toShort())
        out.putShort(8)
        out.putInt(poolStart + newPoolSize + tail.size)

        // Cabecera del pool (28 bytes).
        out.putShort(TYPE_STRING_POOL.toShort())
        out.putShort(28)
        out.putInt(newPoolSize)
        out.putInt(stringCount)
        out.putInt(0)                 // styleCount
        out.putInt(flags)
        out.putInt(newStringsStart)
        out.putInt(0)                 // stylesStart
        for (o in newOffsets) out.putInt(o)
        out.put(stringData)
        out.put(tail)

        return out.array()
    }

    private fun decode(buf: ByteBuffer, pos: Int, utf8: Boolean): String {
        var p = pos
        return if (utf8) {
            // [nº caracteres][nº bytes] (cada uno u8, extensible con el bit alto) + bytes + 0x00
            p = skipLen8(buf, p)
            val (byteLen, after) = readLen8(buf, p)
            val bytes = ByteArray(byteLen)
            for (i in 0 until byteLen) bytes[i] = buf.get(after + i)
            String(bytes, Charsets.UTF_8)
        } else {
            val (charLen, after) = readLen16(buf, p)
            val sb = StringBuilder(charLen)
            for (i in 0 until charLen) sb.append(buf.getShort(after + i * 2).toInt().and(0xFFFF).toChar())
            sb.toString()
        }
    }

    private fun skipLen8(buf: ByteBuffer, pos: Int): Int {
        val (_, after) = readLen8(buf, pos)
        return after
    }

    private fun readLen8(buf: ByteBuffer, pos: Int): Pair<Int, Int> {
        val b0 = buf.get(pos).toInt() and 0xFF
        return if (b0 and 0x80 != 0) {
            val b1 = buf.get(pos + 1).toInt() and 0xFF
            Pair(((b0 and 0x7F) shl 8) or b1, pos + 2)
        } else Pair(b0, pos + 1)
    }

    private fun readLen16(buf: ByteBuffer, pos: Int): Pair<Int, Int> {
        val v0 = buf.getShort(pos).toInt() and 0xFFFF
        return if (v0 and 0x8000 != 0) {
            val v1 = buf.getShort(pos + 2).toInt() and 0xFFFF
            Pair(((v0 and 0x7FFF) shl 16) or v1, pos + 4)
        } else Pair(v0, pos + 2)
    }

    private fun encode(out: ByteArrayOutputStream, s: String, utf8: Boolean) {
        if (utf8) {
            val bytes = s.toByteArray(Charsets.UTF_8)
            writeLen8(out, s.length)       // nº de code units UTF-16
            writeLen8(out, bytes.size)     // nº de bytes UTF-8
            out.write(bytes)
            out.write(0)
        } else {
            writeLen16(out, s.length)
            for (c in s) {
                out.write(c.code and 0xFF)
                out.write((c.code shr 8) and 0xFF)
            }
            out.write(0); out.write(0)
        }
    }

    private fun writeLen8(out: ByteArrayOutputStream, len: Int) {
        if (len > 0x7F) {
            out.write(((len shr 8) and 0x7F) or 0x80)
            out.write(len and 0xFF)
        } else out.write(len)
    }

    private fun writeLen16(out: ByteArrayOutputStream, len: Int) {
        if (len > 0x7FFF) {
            val hi = ((len shr 16) and 0x7FFF) or 0x8000
            out.write(hi and 0xFF); out.write((hi shr 8) and 0xFF)
            out.write(len and 0xFF); out.write((len shr 8) and 0xFF)
        } else {
            out.write(len and 0xFF); out.write((len shr 8) and 0xFF)
        }
    }
}
