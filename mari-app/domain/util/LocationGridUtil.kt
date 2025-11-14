package com.Mari.mobileapp.domain.util

object LocationGridUtil {
    fun encodeLocation(latitude: Double, longitude: Double, precision: Int = 6): String {
        val lat = ((latitude + 90) * Math.pow(10.0, precision.toDouble())).toLong()
        val lng = ((longitude + 180) * Math.pow(10.0, precision.toDouble())).toLong()
        return "${lat}X${lng}"
    }

    fun decodeLocation(grid: String): Pair<Double, Double>? {
        return try {
            val parts = grid.split("X")
            if (parts.size != 2) return null
            val precision = parts[0].length - 1
            val lat = parts[0].toDouble() / Math.pow(10.0, precision.toDouble()) - 90
            val lng = parts[1].toDouble() / Math.pow(10.0, precision.toDouble()) - 180
            Pair(lat, lng)
        } catch (e: Exception) {
            null
        }
    }

    fun areAdjacent(grid1: String, grid2: String, threshold: Double = 0.001): Boolean {
        val loc1 = decodeLocation(grid1) ?: return false
        val loc2 = decodeLocation(grid2) ?: return false
        val latDiff = Math.abs(loc1.first - loc2.first)
        val lngDiff = Math.abs(loc1.second - loc2.second)
        return latDiff <= threshold && lngDiff <= threshold
    }
}
