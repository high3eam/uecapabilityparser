package it.smartphonecombo.uecapabilityparser.cli

import com.github.ajalt.clikt.testing.test
import it.smartphonecombo.uecapabilityparser.UtilityForTests.RECREATE_ORACLES
import it.smartphonecombo.uecapabilityparser.UtilityForTests.recreateCapabilitiesListOracles
import it.smartphonecombo.uecapabilityparser.UtilityForTests.scatAvailable
import it.smartphonecombo.uecapabilityparser.model.Capabilities
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

internal class CliMultiJsonOutputTest {
    private val path = "src/test/resources/cli"

    @Test
    fun testPcap() {
        test(
            "-i",
            "$path/input/pcap.pcap",
            "-t",
            "P",
            "-j",
            "-",
            "--json-pretty-print",
            "--suppress-annoying-messages",
            oracleFilename = "pcap.json",
        )
    }

    @Test
    fun testPcapSegmented() {
        test(
            "-i",
            "$path/input/segmented.pcap",
            "-t",
            "P",
            "-j",
            "-",
            "--json-pretty-print",
            "--suppress-annoying-messages",
            oracleFilename = "segmented.json",
        )
    }

    @Test
    fun testMultiInput() {
        test(
            "-i",
            "$path/input/0xB826.hex",
            "-t",
            "QNR",
            "-i",
            "$path/input/0xB0CD.txt",
            "-t",
            "Q",
            "-j",
            "-",
            "--suppress-annoying-messages",
            oracleFilename = "0xB826-0xB0CD.json",
        )
    }

    @Test
    fun testMultiScat() {
        Assumptions.assumeTrue(scatAvailable)
        test(
            "-i",
            "$path/input/scat.dlf",
            "-t",
            "DLF",
            "-i",
            "$path/input/scat.sdm",
            "-t",
            "SDM",
            "-i",
            "$path/input/scat.hdf",
            "-t",
            "HDF",
            "-i",
            "$path/input/scat.qmdl",
            "-t",
            "QMDL",
            "-j",
            "-",
            "--suppress-annoying-messages",
            oracleFilename = "scat.json",
        )
    }

    @Test
    fun testMultiNsgJson() {
        test(
            "-i",
            "$path/input/nsgExy.json",
            "-t",
            "NSG",
            "-i",
            "$path/input/airscreenQcom.json",
            "-t",
            "NSG",
            "-j",
            "-",
            "--suppress-annoying-messages",
            oracleFilename = "nsgJson.json",
        )
    }

    private fun test(vararg args: String, oracleFilename: String) {
        val oraclePath = "$path/oracleMultiJson/$oracleFilename"

        val result = Cli.test(*args)
        val actual = Json.decodeFromString<List<Capabilities>>(result.stdout)

        if (RECREATE_ORACLES) recreateCapabilitiesListOracles(oraclePath, actual)

        val expected = Json.decodeFromString<List<Capabilities>>(File(oraclePath).readText())

        // Check size
        Assertions.assertEquals(expected.size, actual.size)

        // Override dynamic properties

        for (i in expected.indices) {
            val capA = actual[i]
            val capE = expected[i]

            capA.getStringMetadata("processingTime")?.let { capE.setMetadata("processingTime", it) }
        }

        // check files
        Assertions.assertEquals(expected, actual)
    }
}
