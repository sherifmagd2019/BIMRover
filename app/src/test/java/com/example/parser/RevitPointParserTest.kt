package com.example.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RevitPointParserTest {

    @Test
    fun `parseCsv parses standard revit export table properly`() {
        val sampleCsv = """
            Point_ID,Local_X,Local_Y,Local_Z,Category,Description
            COL-A1,10.50,25.20,-1.50,Structural Column,Main Tower Corner
            FTG-01,0.00,0.00,-3.20,Foundation,Anchor Grid Base
        """.trimIndent()

        val parsed = RevitPointParser.parseCsv(sampleCsv)

        assertEquals(2, parsed.size)
        assertEquals("COL-A1", parsed[0].name)
        assertEquals(10.50, parsed[0].localX, 1e-6)
        assertEquals(25.20, parsed[0].localY, 1e-6)
        assertEquals(-1.50, parsed[0].localZ, 1e-6)
        assertEquals("Structural Column", parsed[0].category)

        assertEquals("FTG-01", parsed[1].name)
        assertEquals(0.00, parsed[1].localX, 1e-6)
    }

    @Test
    fun `parseJson parses json point schedule properly`() {
        val sampleJson = """
            [
              {"name": "PIER-1", "localX": 15.0, "localY": 45.0, "localZ": 2.5, "category": "Bridge Pier"},
              {"name": "PIER-2", "localX": 30.0, "localY": 45.0, "localZ": 2.5, "category": "Bridge Pier"}
            ]
        """.trimIndent()

        val parsed = RevitPointParser.parseJson(sampleJson)

        assertEquals(2, parsed.size)
        assertEquals("PIER-1", parsed[0].name)
        assertEquals(15.0, parsed[0].localX, 1e-6)
        assertEquals(45.0, parsed[0].localY, 1e-6)
        assertEquals(2.5, parsed[0].localZ, 1e-6)
        assertEquals("Bridge Pier", parsed[0].category)
    }

    @Test
    fun `exportToCsv exports correct headers and rows`() {
        val points = RevitPointParser.getSkylineTowerSamplePoints()
        val csv = RevitPointParser.exportToCsv(points)

        assertTrue(csv.startsWith("Point_ID,Local_X,Local_Y,Local_Z,Category,Description"))
        assertTrue(csv.contains("C1-NW"))
        assertTrue(csv.contains("CORE-ELEV"))
    }
}
