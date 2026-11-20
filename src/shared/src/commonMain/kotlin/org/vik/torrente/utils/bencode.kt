package org.vik.torrente.utils

import java.io.ByteArrayOutputStream
import java.security.MessageDigest

enum class NodeType {
    Integer,
    ByteString,
    List,
    Dictionary,
}

data class BencodeNode(
    val nodeType: NodeType,
    val data: Any,
    val startIndex: Int, // offsets so we can calculate infoHash later
    val endIndex: Int,
)

@Suppress("UNCHECKED_CAST")
fun BencodeNode.asDictionary(): Map<String, BencodeNode> {
    require(nodeType == NodeType.Dictionary)
    return data as Map<String, BencodeNode>
}

@Suppress("UNCHECKED_CAST")
fun BencodeNode.asList(): List<BencodeNode> {
    require(nodeType == NodeType.List)
    return data as List<BencodeNode>
}

// Byte strings are kept as raw bytes, since fields like "pieces" are binary
fun BencodeNode.asBytes(): ByteArray {
    require(nodeType == NodeType.ByteString)
    return data as ByteArray
}

fun BencodeNode.asString(): String = String(asBytes(), Charsets.UTF_8)

fun BencodeNode.asLong(): Long {
    require(nodeType == NodeType.Integer)
    return data as Long
}

/** SHA-1 of the raw bencoded `info` dictionary (20 bytes). */
fun bencodeInfoHash(bytes: ByteArray): ByteArray {
    val infoNode = bencodeDecode(bytes)[0].asDictionary()["info"]
        ?: throw IllegalArgumentException("Torrent has no info dictionary")
    return MessageDigest.getInstance("SHA-1").digest(bytes.copyOfRange(infoNode.startIndex, infoNode.endIndex))
}

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

fun bencodeDecode(input: ByteArray): List<BencodeNode> {
    val ret = mutableListOf<BencodeNode>()
    var index = 0
    while (index < input.size) {
        val node = decodeNode(input, index)
        ret.add(node)
        index = node.endIndex
    }
    return ret
}

private fun decodeNode(input: ByteArray, startIndex: Int): BencodeNode {
    require(startIndex < input.size) { "Unexpected end of input" }
    var index = startIndex

    return when (val c = input[index].toInt().toChar()) {
        'i' -> {
            index++ // Skip 'i'
            val end = indexOf(input, 'e', index)
            val value = String(input, index, end - index, Charsets.US_ASCII).toLongOrNull()
                ?: throw IllegalArgumentException("Invalid Int format")
            BencodeNode(NodeType.Integer, value, startIndex, end + 1)
        }
        'l' -> {
            index++ // Skip 'l'
            val list = mutableListOf<BencodeNode>()
            while (index < input.size && input[index].toInt().toChar() != 'e') {
                val child = decodeNode(input, index)
                list.add(child)
                index = child.endIndex
            }
            require(index < input.size) { "Unterminated List format" }
            BencodeNode(NodeType.List, list, startIndex, index + 1)
        }
        'd' -> {
            index++ // Skip 'd'
            val map = mutableMapOf<String, BencodeNode>()
            while (index < input.size && input[index].toInt().toChar() != 'e') {
                val keyNode = decodeNode(input, index)
                require(keyNode.nodeType == NodeType.ByteString) { "Dictionary keys must be ByteStrings" }
                val valNode = decodeNode(input, keyNode.endIndex)
                map[keyNode.asString()] = valNode
                index = valNode.endIndex
            }
            require(index < input.size) { "Unterminated Dictionary format" }
            BencodeNode(NodeType.Dictionary, map, startIndex, index + 1)
        }
        in '0'..'9' -> {
            val colon = indexOf(input, ':', index)
            val length = String(input, index, colon - index, Charsets.US_ASCII).toIntOrNull()
                ?: throw IllegalArgumentException("Invalid ByteString length format")
            val dataStart = colon + 1
            require(dataStart + length <= input.size) { "ByteString length exceeds input length" }
            BencodeNode(NodeType.ByteString, input.copyOfRange(dataStart, dataStart + length), startIndex, dataStart + length)
        }
        else -> throw IllegalArgumentException("Invalid character '$c' for Node identification")
    }
}

private fun indexOf(input: ByteArray, char: Char, from: Int): Int {
    for (i in from until input.size) {
        if (input[i].toInt().toChar() == char) return i
    }
    throw IllegalArgumentException("Expected '$char' but reached end of input")
}

/**
 * Encodes Kotlin values into bencode. Supports Int/Long, String, ByteArray, List and Map<String, *>.
 * Dictionary keys are sorted as required by the spec.
 */
fun bencodeEncode(value: Any): ByteArray = ByteArrayOutputStream().also { encodeTo(it, value) }.toByteArray()

private fun encodeTo(out: ByteArrayOutputStream, value: Any) {
    when (value) {
        is Int, is Long -> out.writeBytes("i${value}e".toByteArray(Charsets.US_ASCII))
        is String -> encodeTo(out, value.toByteArray(Charsets.UTF_8))
        is ByteArray -> {
            out.writeBytes("${value.size}:".toByteArray(Charsets.US_ASCII))
            out.writeBytes(value)
        }
        is List<*> -> {
            out.write('l'.code)
            value.forEach { encodeTo(out, it!!) }
            out.write('e'.code)
        }
        is Map<*, *> -> {
            out.write('d'.code)
            value.entries.sortedBy { it.key as String }.forEach { (k, v) ->
                encodeTo(out, k!!)
                encodeTo(out, v!!)
            }
            out.write('e'.code)
        }
        else -> throw IllegalArgumentException("Cannot bencode ${value::class}")
    }
}
