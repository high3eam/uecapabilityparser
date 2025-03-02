package it.smartphonecombo.uecapabilityparser.importer

import it.smartphonecombo.uecapabilityparser.extension.firstOrNull
import it.smartphonecombo.uecapabilityparser.extension.mutableListWithCapacity
import it.smartphonecombo.uecapabilityparser.io.InputSource
import it.smartphonecombo.uecapabilityparser.model.BwClass
import it.smartphonecombo.uecapabilityparser.model.Capabilities
import it.smartphonecombo.uecapabilityparser.model.EmptyMimo
import it.smartphonecombo.uecapabilityparser.model.combo.ComboLte
import it.smartphonecombo.uecapabilityparser.model.combo.ComboNr
import it.smartphonecombo.uecapabilityparser.model.component.ComponentLte
import it.smartphonecombo.uecapabilityparser.model.component.ComponentNr
import it.smartphonecombo.uecapabilityparser.model.modulation.ModulationOrder
import it.smartphonecombo.uecapabilityparser.model.modulation.toModulation
import it.smartphonecombo.uecapabilityparser.model.toMimo

/** A parser for LTE and NR Combinations as reported by Qct Modem Capabilities */
object ImportQctModemCap : ImportCapabilities {

    /**
     * This parser take as [input] a [InputSource] containing LTE or NR Combinations as reported by Qct
     * Modem Capabilities.
     *
     * The output is a [Capabilities] with the list of parsed LTE combos stored in
     * [lteCombos][Capabilities.lteCombos] and NR combos stored in [nrCombos][Capabilities.nrCombos].
     *
     * It can parse multiple messages in the same input.
     */
    override fun parse(input: InputSource): Capabilities {
        val capabilities = Capabilities()
        val listLteCombo = mutableListOf<ComboLte>()
        val listNrCombo = mutableListOf<ComboNr>()

        input.useLines { seq ->
            try {
                val lines = seq.iterator()
                while (lines.hasNext()) {
                    val source = getValue(lines, "Source")
                    val type = getValue(lines, "Type")
                    val numCombos = getValue(lines, "Combos")?.toIntOrNull() ?: 0
                    val combosHeader = lines.firstOrNull { it.contains("""^\s+#\s+""".toRegex()) }

                    if (combosHeader == null) {
                        continue
                    }

                    val isNrRrc = source.equals("RRC", true) && type?.contains("NR", true) == true

                    val sourceStr = "${source}-${type}".uppercase()
                    capabilities.addMetadata("source", sourceStr)
                    capabilities.addMetadata("numCombos", numCombos)

                    if (isNrRrc) {
                        // Parse NR RRC combos
                        parseNrRrcCombos(lines, numCombos, listNrCombo)
                    } else {
                        // Parse LTE combos
                        val indexDl = combosHeader.indexOf("DL Bands", ignoreCase = true)
                        val indexUl = combosHeader.indexOf("UL Bands", ignoreCase = true)

                        // This is used for DLCA > 5
                        val indexBands = combosHeader.indexOf("Bands", ignoreCase = true)
                        val twoRowFormat = indexBands > -1 && indexDl < 0

                        if (!twoRowFormat && (indexDl < 0 || indexUl < 0)) {
                            continue
                        }

                        repeat(numCombos) {
                            val combo =
                                if (twoRowFormat) {
                                    parseComboTwoRow(lines.next(), lines.next(), indexBands)
                                } else {
                                    parseCombo(lines.next(), indexDl, indexUl)
                                }
                            combo?.let { listLteCombo.add(it) }
                        }
                    }
                }
            } catch (ignored: NoSuchElementException) {
                // Do nothing
            }
        }

        capabilities.lteCombos = listLteCombo
        capabilities.nrCombos = listNrCombo

        return capabilities
    }
    
    /**
     * Parse NR RRC combos from the QCT modem output
     */
    private fun parseNrRrcCombos(
        lines: Iterator<String>,
        numCombos: Int,
        listNrCombo: MutableList<ComboNr>
    ) {
        repeat(numCombos) {
            try {
                // Each combo consists of two lines: DL and UL
                val dlLine = lines.next()
                val ulLine = lines.next()
                
                // Extract the DL components from the first line
                val dlComponents = parseNrComponents(extractNrBandsString(dlLine), true)
                
                // Extract the UL components from the second line
                val ulComponents = parseNrComponents(extractNrBandsString(ulLine), false)
                
                // Create a ComboNr with the DL components
                val combo = ComboNr(dlComponents)
                
                listNrCombo.add(combo)
            } catch (e: Exception) {
                // Skip this combo if parsing fails
            }
        }
    }
    
    /**
     * Extract the band string from an NR RRC line
     */
    private fun extractNrBandsString(line: String): String {
        // Skip the combo number at the beginning of the line
        val startIndex = line.indexOfFirst { it.isLetter() || it == '̵' }
        if (startIndex == -1) return ""
        
        // Extract until the capacity information (if present)
        val capacityIndex = line.indexOf("G ", startIndex)
        return if (capacityIndex > 0) {
            line.substring(startIndex, capacityIndex).trim()
        } else {
            line.substring(startIndex).trim()
        }
    }

    /**
     * Converts the given comboString to a [ComboLte].
     *
     * Returns null if parsing fails
     */
    private fun parseCombo(comboString: String, indexDl: Int, indexUl: Int): ComboLte? {
        try {
            val dlComponents = parseComponents(comboString.substring(indexDl, indexUl), true)

            val ulComponents = parseComponents(comboString.substring(indexUl), false)

            return ComboLte(dlComponents, ulComponents)
        } catch (ignored: Exception) {
            return null
        }
    }

    /**
     * Converts the given comboString to a [ComboLte].
     *
     * Returns null if parsing fails
     */
    private fun parseComboTwoRow(
        comboStringDl: String,
        comboStringUl: String,
        index: Int,
    ): ComboLte? {
        try {
            val dlComponents = parseComponents(comboStringDl.substring(index), true)

            val ulComponents = parseComponents(comboStringUl.substring(index), false)

            return ComboLte(dlComponents, ulComponents)
        } catch (ignored: Exception) {
            return null
        }
    }

    /** Converts the given componentsString to a List of [ComponentLte]. */
    private fun parseComponents(componentsString: String, isDl: Boolean): List<ComponentLte> {
        val components = mutableListWithCapacity<ComponentLte>(6)
        for (componentStr in componentsString.split('-', ' ')) {
            val component = parseComponent(componentStr, isDl)
            if (component != null) {
                components.add(component)
            }
        }
        return components
    }
    
    /** Converts the given NR componentsString to a List of [ComponentNr]. */
    private fun parseNrComponents(componentsString: String, isDl: Boolean): List<ComponentNr> {
        val components = mutableListWithCapacity<ComponentNr>(6)
        for (componentStr in componentsString.split(' ')) {
            if (componentStr.isNotBlank()) {
                val component = parseNrComponent(componentStr, isDl)
                if (component != null) {
                    components.add(component)
                }
            }
        }
        return components
    }

    /**
     * Regex used to extract the various parts of a component.
     *
     * Mixed mimo is represented with the highest value as normal digit and the others as subscript
     * separated by space (MMSP).
     *
     * Modulation is represented as superscript digits.
     *
     * Example: 40D4 ₄ ₂²⁵⁶
     *
     * Note: in some versions bwClass is lowercase.
     */
    private val componentRegex =
        """(\d{1,3})([A-Fa-f])([124]?(?:\p{Zs}[₁₂₄]){0,4})([⁰¹²⁴⁵⁶]{0,4})""".toRegex()
        
    /**
     * Regex used to extract the various parts of an NR component.
     * 
     * Example: 3a4 ̵ͣ40
     * 
     * The format is: <band><bwClass><mimo> ̵ͣ<powerClass><modulation>
     */
    private val nrComponentRegex =
        """(\d{1,3})([a-z])([124])(?: ̵ͣ(\d{1,2}))?([⁰¹²⁴⁵⁶]{0,4})?""".toRegex()

    /**
     * Converts the given componentString to a [ComponentLte].
     *
     * Returns null if parsing fails.
     */
    private fun parseComponent(componentString: String, isDl: Boolean): ComponentLte? {
        val result = componentRegex.find(componentString) ?: return null

        val (_, bandRegex, bwClassRegex, mimoRegex, modRegex) = result.groupValues

        val baseBand = bandRegex.toInt()
        val bwClass = BwClass.valueOf(bwClassRegex)
        val mimoStr = mimoRegex.subscriptToDigit().filterNot(Char::isWhitespace)
        val mimo = mimoStr.toIntOrNull()?.toMimo() ?: EmptyMimo

        return if (isDl) {
            ComponentLte(baseBand, classDL = bwClass, mimoDL = mimo)
        } else {
            val modUL = ModulationOrder.of(modRegex.superscriptToDigit()).toModulation()
            ComponentLte(baseBand, classUL = bwClass, mimoUL = mimo, modUL = modUL)
        }
    }
    
    /**
     * Converts the given componentString to a [ComponentNr].
     *
     * Returns null if parsing fails.
     */
    private fun parseNrComponent(componentString: String, isDl: Boolean): ComponentNr? {
        val result = nrComponentRegex.find(componentString) ?: return null

        val groupValues = result.groupValues
        val bandRegex = groupValues[1]
        val bwClassRegex = groupValues[2]
        val mimoRegex = groupValues[3]
        val powerClassRegex = groupValues.getOrNull(4)
        val modRegex = groupValues.getOrNull(5)

        val baseBand = bandRegex.toInt()
        val bwClass = BwClass.valueOf(bwClassRegex)
        val mimo = mimoRegex.toIntOrNull()?.toMimo() ?: EmptyMimo

        return if (isDl) {
            ComponentNr(baseBand, classDL = bwClass, mimoDL = mimo)
        } else {
            val modUL = ModulationOrder.of(modRegex?.superscriptToDigit() ?: "").toModulation()
            ComponentNr(baseBand, classUL = bwClass, mimoUL = mimo, modUL = modUL)
        }
    }

    /**
     * Search for the first string beginning with given key. Then extract the value. This works for
     * strings like "key : value".
     */
    private fun getValue(iterator: Iterator<String>, key: String): String? {
        val string = iterator.firstOrNull { it.startsWith(key, true) } ?: return null
        return string.split(":").last().trim()
    }

    /** Converts all the subscript in the given string to digit */
    private fun String.subscriptToDigit(): String {
        val listChar = map { char ->
            if (char in '₀'..'₉') {
                char - '₀'.code + '0'.code
            } else {
                char
            }
        }
        return String(listChar.toCharArray())
    }

    /** Converts all the superscript in the given string to digit */
    private fun String.superscriptToDigit(): String {
        val listChar = map { char ->
            when (char) {
                '¹' -> '1'
                '²' -> '2'
                '³' -> '3'
                '⁰',
                in '⁴'..'⁹' -> char - '⁰'.code + '0'.code
                else -> char
            }
        }
        return String(listChar.toCharArray())
    }
}
