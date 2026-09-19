package com.example.gis

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.StakedRecordEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * GPX Export Mode specifying which point geometry to export.
 */
enum class GpxExportMode(val label: String, val description: String) {
    AS_BUILT_ONLY(
        label = "As-Built Measured (Field GNSS)",
        description = "Export actual surveyed ground positions with field elevation and error metadata"
    ),
    AS_BUILT_AND_DESIGN(
        label = "As-Built & Design Pairs",
        description = "Export both surveyed actual points and reference CAD design points"
    ),
    DESIGN_ONLY(
        label = "Design Target Points (Revit CAD)",
        description = "Export reference CAD design coordinates georeferenced to WGS-84"
    )
}

/**
 * Configuration options for GPX export.
 */
data class GpxExportOptions(
    val mode: GpxExportMode = GpxExportMode.AS_BUILT_ONLY,
    val includeExtensions: Boolean = true,
    val includeDeviationTracks: Boolean = false,
    val projectName: String = "BIM Stakeout As-Built Survey",
    val creator: String = "BIM Surveyor Stakeout v1.0 (Android)"
)

/**
 * Comprehensive GPX 1.1 Exporter for surveyed as-built points, CAD design geometry,
 * and geodetic GIS field data compatible with QGIS, ArcGIS, Trimble, Leica, Topcon, and Garmin.
 */
object GpxExporter {

    private val isoDateFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private val fileTimestampFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Escape XML text to produce valid, well-formed XML content.
     */
    fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Generate default GPX file name based on project and timestamp.
     */
    fun generateDefaultFileName(mode: GpxExportMode = GpxExportMode.AS_BUILT_ONLY): String {
        val prefix = when (mode) {
            GpxExportMode.AS_BUILT_ONLY -> "BIM_Stakeout_AsBuilt"
            GpxExportMode.AS_BUILT_AND_DESIGN -> "BIM_Stakeout_Pairs"
            GpxExportMode.DESIGN_ONLY -> "BIM_Stakeout_Design"
        }
        val timestamp = fileTimestampFormat.format(Date())
        return "${prefix}_$timestamp.gpx"
    }

    /**
     * Generate compliant GPX 1.1 XML string from a list of staked records.
     */
    fun generateGpx(
        records: List<StakedRecordEntity>,
        options: GpxExportOptions = GpxExportOptions()
    ): String {
        val sb = StringBuilder()
        val utcNow = isoDateFormat.format(Date())

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"").append(escapeXml(options.creator)).append("\"\n")
        sb.append("     xmlns=\"http://www.topografix.com/GPX/1/1\"\n")
        sb.append("     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
        if (options.includeExtensions) {
            sb.append("     xmlns:bim=\"http://schemas.bimsurveyor.com/gpx/1.0\"\n")
        }
        sb.append("     xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n")

        // Metadata block
        sb.append("  <metadata>\n")
        sb.append("    <name>").append(escapeXml(options.projectName)).append("</name>\n")
        sb.append("    <desc>Field-staked as-built GNSS coordinates and BIM Revit CAD tolerance verifications</desc>\n")
        sb.append("    <time>").append(utcNow).append("</time>\n")
        if (records.isNotEmpty()) {
            val minLat = records.minOf { if (options.mode == GpxExportMode.DESIGN_ONLY) it.targetLat else it.measuredLat }
            val maxLat = records.maxOf { if (options.mode == GpxExportMode.DESIGN_ONLY) it.targetLat else it.measuredLat }
            val minLng = records.minOf { if (options.mode == GpxExportMode.DESIGN_ONLY) it.targetLng else it.measuredLng }
            val maxLng = records.maxOf { if (options.mode == GpxExportMode.DESIGN_ONLY) it.targetLng else it.measuredLng }
            sb.append(
                String.format(
                    Locale.US,
                    "    <bounds minlat=\"%.7f\" minlon=\"%.7f\" maxlat=\"%.7f\" maxlon=\"%.7f\"/>\n",
                    minLat, minLng, maxLat, maxLng
                )
            )
        }
        sb.append("  </metadata>\n")

        // Write Waypoints
        for (record in records) {
            val recordTime = isoDateFormat.format(Date(record.timestamp))

            // 1. As-Built Measured Point
            if (options.mode == GpxExportMode.AS_BUILT_ONLY || options.mode == GpxExportMode.AS_BUILT_AND_DESIGN) {
                val pointName = if (options.mode == GpxExportMode.AS_BUILT_AND_DESIGN) {
                    "${record.pointName}_MEASURED"
                } else {
                    record.pointName
                }

                val sym = if (record.isWithinTolerance) "Flag, Green" else "Flag, Red"
                val statusText = if (record.isWithinTolerance) "PASS (Within Tol)" else "FAIL (Exceeds Tol)"

                val comment = String.format(
                    Locale.US,
                    "As-Built | %s | Horiz Err: %.3f m | dN: %+.3f m, dE: %+.3f m, dZ: %+.3f m | %s",
                    record.pointCategory,
                    record.totalHorizontalErrorMeters,
                    record.deltaNorthMeters,
                    record.deltaEastMeters,
                    record.deltaElevMeters,
                    statusText
                )

                val desc = buildString {
                    append(String.format(Locale.US, "Surveyed As-Built Point: %s (%s). ", record.pointName, record.pointCategory))
                    append(String.format(Locale.US, "Target Lat: %.7f, Lng: %.7f, Elev: %.3f m. ", record.targetLat, record.targetLng, record.targetElev))
                    append(String.format(Locale.US, "Measured Lat: %.7f, Lng: %.7f, Elev: %.3f m. ", record.measuredLat, record.measuredLng, record.measuredElev))
                    append(String.format(Locale.US, "Delta North: %+.3f m, Delta East: %+.3f m, Delta Elev: %+.3f m. ", record.deltaNorthMeters, record.deltaEastMeters, record.deltaElevMeters))
                    append(String.format(Locale.US, "Total 2D Error: %.3f m (%s). ", record.totalHorizontalErrorMeters, statusText))
                    if (record.notes.isNotBlank()) {
                        append("Notes: ").append(record.notes).append(". ")
                    }
                    append("Surveyor: ").append(record.surveyorName)
                }

                sb.append(
                    String.format(
                        Locale.US,
                        "  <wpt lat=\"%.8f\" lon=\"%.8f\">\n",
                        record.measuredLat,
                        record.measuredLng
                    )
                )
                sb.append(String.format(Locale.US, "    <ele>%.3f</ele>\n", record.measuredElev))
                sb.append("    <time>").append(recordTime).append("</time>\n")
                sb.append("    <name>").append(escapeXml(pointName)).append("</name>\n")
                sb.append("    <cmt>").append(escapeXml(comment)).append("</cmt>\n")
                sb.append("    <desc>").append(escapeXml(desc)).append("</desc>\n")
                sb.append("    <sym>").append(escapeXml(sym)).append("</sym>\n")
                sb.append("    <type>").append(escapeXml(record.pointCategory)).append("</type>\n")

                if (options.includeExtensions) {
                    sb.append("    <extensions>\n")
                    sb.append("      <bim:stakedRecord>\n")
                    sb.append("        <bim:pointName>").append(escapeXml(record.pointName)).append("</bim:pointName>\n")
                    sb.append("        <bim:category>").append(escapeXml(record.pointCategory)).append("</bim:category>\n")
                    sb.append(String.format(Locale.US, "        <bim:targetLat>%.8f</bim:targetLat>\n", record.targetLat))
                    sb.append(String.format(Locale.US, "        <bim:targetLng>%.8f</bim:targetLng>\n", record.targetLng))
                    sb.append(String.format(Locale.US, "        <bim:targetElev>%.3f</bim:targetElev>\n", record.targetElev))
                    sb.append(String.format(Locale.US, "        <bim:measuredLat>%.8f</bim:measuredLat>\n", record.measuredLat))
                    sb.append(String.format(Locale.US, "        <bim:measuredLng>%.8f</bim:measuredLng>\n", record.measuredLng))
                    sb.append(String.format(Locale.US, "        <bim:measuredElev>%.3f</bim:measuredElev>\n", record.measuredElev))
                    sb.append(String.format(Locale.US, "        <bim:deltaNorthMeters>%.4f</bim:deltaNorthMeters>\n", record.deltaNorthMeters))
                    sb.append(String.format(Locale.US, "        <bim:deltaEastMeters>%.4f</bim:deltaEastMeters>\n", record.deltaEastMeters))
                    sb.append(String.format(Locale.US, "        <bim:deltaElevMeters>%.4f</bim:deltaElevMeters>\n", record.deltaElevMeters))
                    sb.append(String.format(Locale.US, "        <bim:horizontalErrorMeters>%.4f</bim:horizontalErrorMeters>\n", record.totalHorizontalErrorMeters))
                    sb.append("        <bim:isWithinTolerance>").append(record.isWithinTolerance).append("</bim:isWithinTolerance>\n")
                    sb.append("        <bim:surveyor>").append(escapeXml(record.surveyorName)).append("</bim:surveyor>\n")
                    if (record.notes.isNotBlank()) {
                        sb.append("        <bim:notes>").append(escapeXml(record.notes)).append("</bim:notes>\n")
                    }
                    sb.append("      </bim:stakedRecord>\n")
                    sb.append("    </extensions>\n")
                }
                sb.append("  </wpt>\n")
            }

            // 2. CAD Design Reference Point
            if (options.mode == GpxExportMode.DESIGN_ONLY || options.mode == GpxExportMode.AS_BUILT_AND_DESIGN) {
                val pointName = if (options.mode == GpxExportMode.AS_BUILT_AND_DESIGN) {
                    "${record.pointName}_DESIGN"
                } else {
                    record.pointName
                }

                val comment = String.format(
                    Locale.US,
                    "CAD Design Point | %s | Target Elev: %.3f m",
                    record.pointCategory,
                    record.targetElev
                )

                val desc = String.format(
                    Locale.US,
                    "Original CAD Design Target for %s (%s). Elev: %.3f m",
                    record.pointName,
                    record.pointCategory,
                    record.targetElev
                )

                sb.append(
                    String.format(
                        Locale.US,
                        "  <wpt lat=\"%.8f\" lon=\"%.8f\">\n",
                        record.targetLat,
                        record.targetLng
                    )
                )
                sb.append(String.format(Locale.US, "    <ele>%.3f</ele>\n", record.targetElev))
                sb.append("    <time>").append(recordTime).append("</time>\n")
                sb.append("    <name>").append(escapeXml(pointName)).append("</name>\n")
                sb.append("    <cmt>").append(escapeXml(comment)).append("</cmt>\n")
                sb.append("    <desc>").append(escapeXml(desc)).append("</desc>\n")
                sb.append("    <sym>Waypoint</sym>\n")
                sb.append("    <type>Revit CAD Design Point</type>\n")
                sb.append("  </wpt>\n")
            }
        }

        // Optional: Deviation Tracks connecting Design -> Measured points
        if (options.includeDeviationTracks && (options.mode == GpxExportMode.AS_BUILT_AND_DESIGN || options.mode == GpxExportMode.AS_BUILT_ONLY)) {
            for (record in records) {
                sb.append("  <trk>\n")
                sb.append("    <name>").append(escapeXml("Deviation_${record.pointName}")).append("</name>\n")
                sb.append(
                    String.format(
                        Locale.US,
                        "    <desc>Offset vector: dN=%+.3fm, dE=%+.3fm, 2D Error=%.3fm</desc>\n",
                        record.deltaNorthMeters,
                        record.deltaEastMeters,
                        record.totalHorizontalErrorMeters
                    )
                )
                sb.append("    <trkseg>\n")
                // Start at design point
                sb.append(
                    String.format(
                        Locale.US,
                        "      <trkpt lat=\"%.8f\" lon=\"%.8f\">\n        <ele>%.3f</ele>\n      </trkpt>\n",
                        record.targetLat,
                        record.targetLng,
                        record.targetElev
                    )
                )
                // End at measured point
                sb.append(
                    String.format(
                        Locale.US,
                        "      <trkpt lat=\"%.8f\" lon=\"%.8f\">\n        <ele>%.3f</ele>\n      </trkpt>\n",
                        record.measuredLat,
                        record.measuredLng,
                        record.measuredElev
                    )
                )
                sb.append("    </trkseg>\n")
                sb.append("  </trk>\n")
            }
        }

        sb.append("</gpx>\n")
        return sb.toString()
    }

    /**
     * Generate GPX XML for a single staked point record.
     */
    fun generateSinglePointGpx(record: StakedRecordEntity): String {
        return generateGpx(
            records = listOf(record),
            options = GpxExportOptions(
                projectName = "Point ${record.pointName} As-Built Stakeout",
                mode = GpxExportMode.AS_BUILT_ONLY,
                includeExtensions = true
            )
        )
    }

    /**
     * Write GPX content to the app's cache directory so it can be shared via FileProvider.
     */
    fun writeGpxToCache(context: Context, gpxContent: String, fileName: String): File {
        val cacheDir = File(context.cacheDir, "gpx")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val file = File(cacheDir, fileName)
        FileOutputStream(file).use { out ->
            out.write(gpxContent.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    /**
     * Write GPX content to an output Uri provided by the Storage Access Framework (SAF).
     */
    fun writeGpxToUri(context: Context, gpxContent: String, destinationUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                out.write(gpxContent.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Create an Android ACTION_SEND Intent with FileProvider URI to share GPX file.
     */
    fun createShareIntent(context: Context, gpxFile: File, pointCount: Int): Intent {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, gpxFile)

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "BIM Stakeout As-Built Points ($pointCount Points)")
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached is the GPX 1.1 file containing $pointCount field-staked as-built survey points, with geodetic coordinates, field elevations, and BIM tolerance verifications."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
