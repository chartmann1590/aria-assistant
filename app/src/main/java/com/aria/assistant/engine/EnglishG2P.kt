package com.aria.assistant.engine

import java.io.File

class EnglishG2P {

    private val wordMap = HashMap<String, List<String>>()
    private var loaded = false

    private val arpabetToIpa = mapOf(
        "AA" to "ɑ", "AE" to "æ", "AH" to "ʌ", "AO" to "ɔ",
        "AW" to "aʊ", "AY" to "aɪ", "B" to "b", "CH" to "tʃ",
        "D" to "d", "DH" to "ð", "EH" to "ɛ", "ER" to "ɝ",
        "EY" to "eɪ", "F" to "f", "G" to "ɡ", "HH" to "h",
        "IH" to "ɪ", "IY" to "i", "JH" to "dʒ", "K" to "k",
        "L" to "l", "M" to "m", "N" to "n", "NG" to "ŋ",
        "OW" to "oʊ", "OY" to "ɔɪ", "P" to "p", "R" to "r",
        "S" to "s", "SH" to "ʃ", "T" to "t", "TH" to "θ",
        "UH" to "ʊ", "UW" to "u", "V" to "v", "W" to "w",
        "Y" to "j", "Z" to "z", "ZH" to "ʒ"
    )

    private val arpabetVowelStress = mapOf(
        "AA" to true, "AE" to true, "AH" to true, "AO" to true,
        "AW" to true, "AY" to true, "EH" to true, "ER" to true,
        "EY" to true, "IH" to true, "IY" to true, "OW" to true,
        "OY" to true, "UH" to true, "UW" to true
    )

    fun load(dictFile: File): Boolean {
        if (loaded) return true
        if (!dictFile.exists()) return false

        try {
            dictFile.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isBlank() || trimmed.startsWith(";;;")) return@forEachLine
                val spaceIdx = trimmed.indexOf(' ')
                if (spaceIdx <= 0) return@forEachLine
                val word = trimmed.substring(0, spaceIdx).lowercase()
                val phonemes = trimmed.substring(spaceIdx + 1).trim()
                if (phonemes.isNotBlank()) {
                    wordMap[word] = phonemes.split(" ")
                }
            }
            loaded = true
            AriaLogger.d("EnglishG2P", "Loaded ${wordMap.size} words from cmudict")
            return true
        } catch (e: Exception) {
            AriaLogger.e("EnglishG2P", "Failed to load cmudict: ${e.message}")
            return false
        }
    }

    fun isLoaded(): Boolean = loaded

    fun sentenceToPhonemeIds(
        text: String,
        phonemeIdMap: Map<String, List<Int>>,
        bosToken: Int = 1,
        eosToken: Int = 2
    ): IntArray {
        val words = text.lowercase().trim().split(Regex("\\s+"))
        val phonemeSymbols = mutableListOf<String>()

        for ((i, word) in words.withIndex()) {
            if (i > 0) {
                phonemeSymbols.add(" ") // word separator
            }
            val clean = word.trim(',', '.', '!', '?', '"', '\'', ':', ';')

            val arpabetPhonemes = wordMap[clean]
            if (arpabetPhonemes != null) {
                var currentStress = 0
                for (arpa in arpabetPhonemes) {
                    val base = arpa.trimEnd('0', '1', '2')
                    val stress = when {
                        arpa.endsWith('1') -> 1
                        arpa.endsWith('2') -> 2
                        else -> 0
                    }
                    val ipa = arpabetToIpa[base]
                    if (ipa != null) {
                        if (stress == 1 && arpabetVowelStress[base] == true) {
                            phonemeSymbols.add("ˈ")
                        } else if (stress == 2 && arpabetVowelStress[base] == true) {
                            phonemeSymbols.add("ˌ")
                        }
                        phonemeSymbols.add(ipa)
                        currentStress = stress
                    }
                }
            } else {
                for (ch in clean) {
                    phonemeSymbols.addAll(letterToIpa(ch))
                }
            }

            if (clean.endsWith('.') || clean.endsWith('!') || clean.endsWith('?')) {
                val punct = clean.last().toString()
                val punctId = phonemeIdMap[punct]
                if (punctId != null) {
                    phonemeSymbols.add(punct)
                }
            }
        }

        val ids = mutableListOf(bosToken)
        for (symbol in phonemeSymbols) {
            val idList = phonemeIdMap[symbol]
            if (idList != null && idList.isNotEmpty()) {
                ids.add(idList[0])
            }
        }
        ids.add(eosToken)

        return ids.toIntArray()
    }

    private fun letterToIpa(ch: Char): List<String> {
        return when (ch.lowercaseChar()) {
            'a' -> listOf("ə")
            'b' -> listOf("b")
            'c' -> listOf("k")
            'd' -> listOf("d")
            'e' -> listOf("ɛ")
            'f' -> listOf("f")
            'g' -> listOf("ɡ")
            'h' -> listOf("h")
            'i' -> listOf("ɪ")
            'j' -> listOf("dʒ")
            'k' -> listOf("k")
            'l' -> listOf("l")
            'm' -> listOf("m")
            'n' -> listOf("n")
            'o' -> listOf("oʊ")
            'p' -> listOf("p")
            'q' -> listOf("k")
            'r' -> listOf("ɹ")
            's' -> listOf("s")
            't' -> listOf("t")
            'u' -> listOf("ʌ")
            'v' -> listOf("v")
            'w' -> listOf("w")
            'x' -> listOf("k", "s")
            'y' -> listOf("j")
            'z' -> listOf("z")
            ',' -> listOf(",")
            '.' -> listOf(".")
            '!' -> listOf("!")
            '?' -> listOf("?")
            '\'' -> listOf("'")
            ':' -> listOf(":")
            ';' -> listOf(";")
            else -> emptyList()
        }
    }
}
