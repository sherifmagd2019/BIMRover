package com.example.parser

import com.example.model.ProjectAnchor
import com.example.model.RevitPoint
import com.example.model.SurveyUnit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parser for Autodesk Revit exported point schedules.
 * Supports CSV, JSON, and pre-configured structural engineering benchmarks.
 */
object RevitPointParser {

    /**
     * Parses a CSV string containing Revit structural points.
     * Expected columns (header optional, detected automatically):
     * PointName/ID, LocalX, LocalY, LocalZ, [Category], [Description]
     *
     * Example:
     * Point_ID, Local_X, Local_Y, Local_Z, Category, Description
     * C-1, 12.500, 24.300, -2.150, Column Grid, Pier Column Center
     */
    fun parseCsv(csvContent: String): List<RevitPoint> {
        val points = mutableListOf<RevitPoint>()
        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotEmpty() }

        if (lines.isEmpty()) return emptyList()

        var hasHeader = false
        val firstLine = lines.first().lowercase()
        if (firstLine.contains("name") || firstLine.contains("point") || firstLine.contains("local") || firstLine.contains("x")) {
            hasHeader = true
        }

        val dataLines = if (hasHeader) lines.drop(1) else lines

        for (line in dataLines) {
            // Split by comma, tab, or semicolon
            val tokens = line.split(Regex("[,;\\t]")).map { it.trim().trim('"', '\'') }
            if (tokens.size >= 4) {
                try {
                    val id = tokens[0]
                    val x = tokens[1].toDoubleOrNull() ?: continue
                    val y = tokens[2].toDoubleOrNull() ?: continue
                    val z = tokens[3].toDoubleOrNull() ?: 0.0
                    val category = if (tokens.size >= 5 && tokens[4].isNotEmpty()) tokens[4] else "BIM Layout"
                    val desc = if (tokens.size >= 6) tokens[5] else ""

                    points.add(
                        RevitPoint(
                            id = id,
                            name = id,
                            localX = x,
                            localY = y,
                            localZ = z,
                            category = category,
                            description = desc
                        )
                    )
                } catch (_: Exception) {
                    // Skip malformed rows gracefully
                }
            }
        }

        return points
    }

    /**
     * Parses a JSON string containing an array of Revit points or a wrapper object.
     */
    fun parseJson(jsonContent: String): List<RevitPoint> {
        val points = mutableListOf<RevitPoint>()
        val trimmed = jsonContent.trim()

        try {
            val jsonArray = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("points") ?: obj.optJSONArray("revit_points") ?: JSONArray()
            }

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val id = item.optString("id", item.optString("name", "PT-${i + 1}"))
                val name = item.optString("name", id)
                val x = item.optDouble("x", item.optDouble("localX", 0.0))
                val y = item.optDouble("y", item.optDouble("localY", 0.0))
                val z = item.optDouble("z", item.optDouble("localZ", 0.0))
                val category = item.optString("category", "Structural Grid")
                val desc = item.optString("description", "")

                points.add(
                    RevitPoint(
                        id = id,
                        name = name,
                        localX = x,
                        localY = y,
                        localZ = z,
                        category = category,
                        description = desc
                    )
                )
            }
        } catch (_: Throwable) {
            // Pure Kotlin regex fallback for host JVM unit test environments without Android framework
            val objectRegex = Regex("""\{([^}]+)\}""")
            val matches = objectRegex.findAll(trimmed)

            for (match in matches) {
                val body = match.groupValues[1]

                fun extractString(key: String): String? {
                    val r = Regex("""["']?$key["']?\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    return r.find(body)?.groupValues?.get(1)
                }

                fun extractDouble(key: String): Double? {
                    val r = Regex("""["']?$key["']?\s*:\s*(-?[\d.]+)""", RegexOption.IGNORE_CASE)
                    return r.find(body)?.groupValues?.get(1)?.toDoubleOrNull()
                }

                val name = extractString("name") ?: extractString("id") ?: "PT"
                val id = extractString("id") ?: name
                val x = extractDouble("localX") ?: extractDouble("x") ?: 0.0
                val y = extractDouble("localY") ?: extractDouble("y") ?: 0.0
                val z = extractDouble("localZ") ?: extractDouble("z") ?: 0.0
                val cat = extractString("category") ?: "Structural Grid"
                val desc = extractString("description") ?: ""

                points.add(
                    RevitPoint(
                        id = id,
                        name = name,
                        localX = x,
                        localY = y,
                        localZ = z,
                        category = cat,
                        description = desc
                    )
                )
            }
        }
        return points
    }

    /**
     * Exports a list of RevitPoints to standardized CSV format.
     */
    fun exportToCsv(points: List<RevitPoint>): String {
        val sb = StringBuilder()
        sb.append("Point_ID,Local_X,Local_Y,Local_Z,Category,Description\n")
        for (p in points) {
            sb.append("${p.id},${"%.4f".format(p.localX)},${"%.4f".format(p.localY)},${"%.4f".format(p.localZ)},\"${p.category}\",\"${p.description}\"\n")
        }
        return sb.toString()
    }

    /**
     * Professional benchmark dataset representing a commercial high-rise tower
     * foundation pile and structural column grid layout.
     */
    fun getSkylineTowerSamplePoints(): List<RevitPoint> = listOf(
        RevitPoint("C1-NW", "Col C1 (Grid 1-A)", 0.000, 0.000, -2.450, "Column Grid", "NW Corner Main Core Pier"),
        RevitPoint("C2-NE", "Col C2 (Grid 1-D)", 18.250, 0.000, -2.450, "Column Grid", "NE Corner Main Column"),
        RevitPoint("C3-SE", "Col C3 (Grid 4-D)", 18.250, 24.500, -2.450, "Column Grid", "SE Perimeter Column Center"),
        RevitPoint("C4-SW", "Col C4 (Grid 4-A)", 0.000, 24.500, -2.450, "Column Grid", "SW Perimeter Column Center"),
        RevitPoint("PILE-101", "Drilled Shaft P101", -3.200, -2.800, -8.600, "Foundation Pile", "1200mm Dia Cast-in-place Pile"),
        RevitPoint("PILE-102", "Drilled Shaft P102", 21.450, -2.800, -8.600, "Foundation Pile", "1200mm Dia Cast-in-place Pile"),
        RevitPoint("CORE-ELEV", "Elevator Core Center", 9.125, 12.250, -3.100, "Elevator Core", "Primary Shear Wall Anchor"),
        RevitPoint("BOLT-A1", "Anchor Bolt Cluster A1", 4.500, 6.200, -0.150, "Anchor Bolts", "Baseplate Level Anchor M36"),
        RevitPoint("SLAB-BENCH", "Grade Slab Benchmark", 12.000, 8.500, 0.000, "Slab Level", "Finished Grade Datum 0.000"),
        RevitPoint("RET-WALL-01", "Retaining Wall Corner", -5.500, 30.000, -1.800, "Retaining Wall", "Basement Retaining Shoring")
    )

    /**
     * Sample benchmark anchor for Skyline Tower.
     */
    fun getSkylineTowerAnchor(): ProjectAnchor = ProjectAnchor(
        id = "skyline_base_point",
        siteName = "Skyline Tower - Foundation Package",
        latitude = 37.774929,
        longitude = -122.419416,
        elevationMeters = 35.420,
        trueNorthOffsetDegrees = 34.50,
        unit = SurveyUnit.METERS
    )

    /**
     * Bridge Infrastructure benchmark dataset.
     */
    fun getBridgeAbutmentSamplePoints(): List<RevitPoint> = listOf(
        RevitPoint("ABUT-P1", "Abutment Pier Center", 0.000, 0.000, 14.200, "Bridge Pier", "South Pier Bearing Centerline"),
        RevitPoint("BEAR-01", "Elastomeric Bearing 1", -2.800, 1.400, 15.650, "Bearings", "Girder G1 Seat"),
        RevitPoint("BEAR-02", "Elastomeric Bearing 2", 2.800, 1.400, 15.650, "Bearings", "Girder G2 Seat"),
        RevitPoint("WING-L", "Left Wingwall Apex", -6.500, -4.200, 17.000, "Wingwall", "Reinforced Wingwall Top"),
        RevitPoint("WING-R", "Right Wingwall Apex", 6.500, -4.200, 17.000, "Wingwall", "Reinforced Wingwall Top")
    )

    fun getBridgeAbutmentAnchor(): ProjectAnchor = ProjectAnchor(
        id = "bridge_pier_anchor",
        siteName = "Interstate Overpass Abutment 4",
        latitude = 37.789172,
        longitude = -122.401449,
        elevationMeters = 52.800,
        trueNorthOffsetDegrees = 112.00,
        unit = SurveyUnit.METERS
    )
}
