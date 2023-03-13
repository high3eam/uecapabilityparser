package it.smartphonecombo.uecapabilityparser.importer.lte

import it.smartphonecombo.uecapabilityparser.Utility
import java.io.File
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class Import0xB0CDTest {

    companion object {
        val import0xB0CD = Import0xB0CD()
    }

    private fun parse(inputFilename: String, oracleFilename: String) {
        val path = "src/test/resources/0xB0CD/"

        val inputPath = "$path/input/$inputFilename"
        val oraclePath = "$path/oracle/$oracleFilename"

        val capabilities = import0xB0CD.parse(File(inputPath).readText())
        val actualCsv = Utility.toCsv(capabilities).lines().dropLastWhile { it.isBlank() }
        val expectedCsv =
            File(oraclePath).bufferedReader().readLines().dropLastWhile { it.isBlank() }

        Assertions.assertLinesMatch(expectedCsv, actualCsv)
    }

    @Test
    fun parse0xB0CDv32() {
        parse("v32.txt", "v32.csv")
    }

    @Test
    fun parse0xB0CDv40() {
        parse("v40.txt", "v40.csv")
    }

    @Test
    fun parse0xB0CDv41() {
        parse("v41.txt", "v41.csv")
    }
}