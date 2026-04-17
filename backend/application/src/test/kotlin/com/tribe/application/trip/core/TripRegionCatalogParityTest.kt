package com.tribe.application.trip.core

import com.tribe.domain.trip.core.TripRegion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class TripRegionCatalogParityTest {
    @Test
    fun `frontend trip region catalog stays aligned with backend enum`() {
        val frontendRegions = parseFrontendCatalog(loadFrontendCatalogSource())
            .associateBy { it.code }
        val backendRegions = TripRegion.entries.associate {
            it.code to FrontendTripRegion(
                code = it.code,
                countryCode = it.country.code,
                label = it.label,
                type = it.type.name,
                centerLat = it.centerLat,
                centerLng = it.centerLng,
            )
        }

        assertEquals(backendRegions.keys, frontendRegions.keys, "TripRegion code set drifted between backend and frontend catalog")

        backendRegions.forEach { (code, backend) ->
            val frontend = frontendRegions[code]
            assertTrue(frontend != null, "frontend catalog entry missing for $code")
            assertEquals(backend.countryCode, frontend?.countryCode, "countryCode mismatch for $code")
            assertEquals(backend.label, frontend?.label, "label mismatch for $code")
            assertEquals(backend.type, frontend?.type, "type mismatch for $code")
            assertEquals(backend.centerLat, frontend!!.centerLat, 0.0001, "centerLat mismatch for $code")
            assertEquals(backend.centerLng, frontend.centerLng, 0.0001, "centerLng mismatch for $code")
        }
    }

    private fun loadFrontendCatalogSource(): String {
        val workingDirectory = Path.of(System.getProperty("user.dir"))
        val candidateRoots = listOf(
            workingDirectory,
            workingDirectory.parent,
            workingDirectory.parent?.parent,
        ).filterNotNull()

        val catalogPath = candidateRoots
            .map { it.resolve("frontend/src/lib/tripRegions.ts") }
            .firstOrNull { Files.exists(it) }

        assertTrue(catalogPath != null, "frontend/src/lib/tripRegions.ts not found from ${System.getProperty("user.dir")}")
        return Files.readString(catalogPath)
    }

    private fun parseFrontendCatalog(source: String): List<FrontendTripRegion> {
        val regex = Regex(
            """\{\s*code:\s*"([^"]+)",\s*countryCode:\s*"([^"]+)",\s*label:\s*"([^"]+)",\s*type:\s*"([^"]+)",\s*centerLat:\s*([-\d.]+),\s*centerLng:\s*([-\d.]+),""",
        )

        return regex.findAll(source)
            .map { match ->
                FrontendTripRegion(
                    code = match.groupValues[1],
                    countryCode = match.groupValues[2],
                    label = match.groupValues[3],
                    type = match.groupValues[4],
                    centerLat = match.groupValues[5].toDouble(),
                    centerLng = match.groupValues[6].toDouble(),
                )
            }
            .toList()
    }

    private data class FrontendTripRegion(
        val code: String,
        val countryCode: String,
        val label: String,
        val type: String,
        val centerLat: Double,
        val centerLng: Double,
    )
}
