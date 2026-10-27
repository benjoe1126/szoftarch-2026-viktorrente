package org.vik.torrente.engine

enum class NodeType {
    Integer,
    ByteString,
    List,
    Dictionary,
    Invalid
}

data class BencodeNode(val nodeType: NodeType, val data : Any)

fun bencodeDecode(input: String): List<BencodeNode> {
    val ret = mutableListOf<BencodeNode>()
    var index = 0

    while (index < input.length) {
        val (node, newIndex) = decodeNode(input, index)
        ret.add(node)
        index = newIndex
    }

    return ret
}

private fun getNodeType(input: String, index: Int): NodeType{
    when(input[index]){
        'i' -> return NodeType.Integer
        'l' -> return NodeType.List
        'd' -> return NodeType.Dictionary
    }

    if(input[index].isDigit()){
        return NodeType.ByteString
    }

    throw IllegalArgumentException("Invalid character for Node identificítion")
}

private fun decodeNode(input: String, startIndex: Int): Pair<BencodeNode, Int> {
    var index = startIndex
    val nodeType = getNodeType(input, index)
    var retNode = BencodeNode(NodeType.Invalid, 2)

    when (nodeType) {
        NodeType.Integer -> {
            index++ // Skip 'i'
            var buffer = ""

            while (index < input.length && input[index] != 'e') {
                val char = input[index]
                if (char.isDigit() || (char == '-' && buffer.isEmpty())) {
                    buffer += char
                } else {
                    throw IllegalArgumentException("Invalid Int format")
                }
                index++
            }

            if (index >= input.length || input[index] != 'e') {
                throw IllegalArgumentException("Unterminated Int format")
            }
            index++ // Skip 'e'

            retNode = BencodeNode(NodeType.Integer, Integer.parseInt(buffer))
        }
        NodeType.ByteString -> {
            var lenBuffer = ""

            while (index < input.length && input[index] != ':') {
                val char = input[index]
                if (char.isDigit()) {
                    lenBuffer += char
                } else {
                    throw IllegalArgumentException("Invalid ByteString length format")
                }
                index++
            }

            if (index >= input.length || input[index] != ':') {
                throw IllegalArgumentException("Unterminated ByteString length")
            }
            index++ // Skip ':'

            val length = lenBuffer.toInt()
            if (index + length > input.length) {
                throw IllegalArgumentException("ByteString length exceeds input length")
            }

            val strData = input.substring(index, index + length)
            index += length
            retNode = BencodeNode(NodeType.ByteString, strData)
        }
        NodeType.List -> {
            index++ // Skip 'l'
            val list = mutableListOf<BencodeNode>()

            while (index < input.length && input[index] != 'e') {
                val (childNode, newIndex) = decodeNode(input, index)
                list.add(childNode)
                index = newIndex
            }

            if (index >= input.length || input[index] != 'e') {
                throw IllegalArgumentException("Unterminated List format")
            }
            index++ // Skip 'e'

            retNode = BencodeNode(NodeType.List, list)
        }
        NodeType.Dictionary -> {
            index++ // Skip 'd'
            val map = mutableMapOf<String, BencodeNode>()

            while (index < input.length && input[index] != 'e') {
                val (keyNode, keyIndex) = decodeNode(input, index)
                index = keyIndex

                if (keyNode.nodeType != NodeType.ByteString) {
                    throw IllegalArgumentException("Dictionary keys must be ByteStrings")
                }
                val key = keyNode.data as String

                val (valNode, valIndex) = decodeNode(input, index)
                index = valIndex
                map[key] = valNode
            }

            if (index >= input.length || input[index] != 'e') {
                throw IllegalArgumentException("Unterminated Dictionary format")
            }
            index++ // Skip 'e'

            retNode = BencodeNode(NodeType.Dictionary, map)
        }
        NodeType.Invalid -> {
            throw IllegalArgumentException("Invalid node type")
        }
    }

    return Pair(retNode, index)
}