/* ============================================================================
 * File        : StationCache.kt
 * Purpose     : Stores the grid nodes last returned by the Web API in SQLite so
 *               the map, the booking filters and every node name still render
 *               when the service cannot be reached. Read-through cache only.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.smartgrid_mobile.data.remote.StationDto

class StationCache(context: Context) {

    private val helper = SmartGridDbHelper(context)

    /** Replaces the cached node list with what the service just returned. */
    fun replaceAll(stations: List<StationDto>) {
        val db = helper.writableDatabase
        val now = System.currentTimeMillis()

        // One transaction, so a failure part-way cannot leave a half-written list.
        db.beginTransaction()
        try {
            db.delete(SmartGridDbHelper.TABLE_STATION, null, null)
            stations.forEach { station ->
                db.insertWithOnConflict(
                    SmartGridDbHelper.TABLE_STATION,
                    null,
                    station.toValues(now),
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Reads the cached nodes back, ordered by name so the list is stable. */
    fun readAll(): List<StationDto> {
        val stations = mutableListOf<StationDto>()

        helper.readableDatabase.query(
            SmartGridDbHelper.TABLE_STATION,
            null,
            null,
            null,
            null,
            null,
            "${SmartGridDbHelper.COL_STATION_NAME} ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val row = CursorRow(cursor)
                stations += StationDto(
                    id = row.text(SmartGridDbHelper.COL_STATION_ID).orEmpty(),
                    stationName = row.text(SmartGridDbHelper.COL_STATION_NAME),
                    latitude = row.real(SmartGridDbHelper.COL_LATITUDE),
                    longitude = row.real(SmartGridDbHelper.COL_LONGITUDE),
                    capacityKWh = row.real(SmartGridDbHelper.COL_CAPACITY_KWH),
                    totalBatterySlots = row.int(SmartGridDbHelper.COL_TOTAL_SLOTS),
                    operatingSchedule = row.text(SmartGridDbHelper.COL_SCHEDULE),
                    isActive = row.boolean(SmartGridDbHelper.COL_IS_ACTIVE)
                )
            }
        }

        return stations
    }

    /** Drops the cached nodes, used when the session is cleared on sign-out. */
    fun clear() {
        helper.writableDatabase.delete(SmartGridDbHelper.TABLE_STATION, null, null)
    }

    /** Flattens one node into the column values the table expects. */
    private fun StationDto.toValues(cachedAt: Long): ContentValues = ContentValues().apply {
        put(SmartGridDbHelper.COL_STATION_ID, id)
        put(SmartGridDbHelper.COL_STATION_NAME, stationName)
        put(SmartGridDbHelper.COL_LATITUDE, latitude)
        put(SmartGridDbHelper.COL_LONGITUDE, longitude)
        put(SmartGridDbHelper.COL_CAPACITY_KWH, capacityKWh)
        put(SmartGridDbHelper.COL_TOTAL_SLOTS, totalBatterySlots)
        put(SmartGridDbHelper.COL_SCHEDULE, operatingSchedule)
        put(SmartGridDbHelper.COL_IS_ACTIVE, if (isActive == true) 1 else 0)
        put(SmartGridDbHelper.COL_CACHED_AT, cachedAt)
    }
}
