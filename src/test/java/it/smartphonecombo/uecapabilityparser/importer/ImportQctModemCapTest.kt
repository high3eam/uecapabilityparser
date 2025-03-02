package it.smartphonecombo.uecapabilityparser.importer

import it.smartphonecombo.uecapabilityparser.extension.toInputSource
import java.io.File
import org.junit.jupiter.api.Test

internal class ImportQctModemCapTest :
    AbstractImportCapabilities(ImportQctModemCap, "src/test/resources/qctModemCap/") {
    @Test
    fun parseEfsQuery() {
        parse("efs-query.txt", "efs-query.json")
    }

    @Test
    fun parseFtmQuery() {
        parse("ftm-query.txt", "ftm-query.json")
    }

    @Test
    fun parseLowercase() {
        parse("lowercase.txt", "lowercase.json")
    }

    @Test
    fun parseTwoRow() {
        parse("two-row.txt", "two-row.json")
    }

    @Test
    fun parseNrRrc() {
        // We now support NR RRC parsing
        val capabilities = ImportQctModemCap.parse(File("src/test/resources/qctModemCap/input/nr-rrc.txt").toInputSource())
        
        // Verify that we have NR combos
        assert(capabilities.nrCombos.isNotEmpty()) { "NR combos should not be empty" }
        
        // Verify metadata
        assert(capabilities.metadata["source"] == "RRC-NR VER. 0.13") { "Source metadata is incorrect" }
        assert(capabilities.metadata["numCombos"] == "1203") { "Number of combos metadata is incorrect" }
    }

    @Test
    fun parseLteRrc() {
        parse("lte-rrc.txt", "lte-rrc.json")
    }

    @Test
    fun parseLteRrcInvalid() {
        parse("lte-rrc-invalid.txt", "lte-rrc-invalid.json")
    }

    @Test
    fun parseMulti() {
        parse("multi.txt", "multi.json")
    }
}
