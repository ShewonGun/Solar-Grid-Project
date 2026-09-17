/* ============================================================================
 * File        : ReservationCache.kt
 * Purpose     : Stores the prosumer's reservations last returned by the Web API
 *               in SQLite, so the bookings list, the summary and the QR screen
 *               still render without a connection. Read-through cache only: the
 *               service remains the authority on every status and rule.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.smartgrid_mobile.data.remote.ReservationDto

class ReservationCache(context: Context) {

    private val helper = SmartGridDbHelper(context)

    /** Replaces one list (upcoming or history) with what the service just returned. */
    fun replaceBucket(bucket: String, reservations: List<ReservationDto>) {
        val db = helper.writableDatabase
        val now = System.currentTimeMillis()

        // One transaction, so a failure part-way cannot leave a half-written list.
        db.beginTransaction()
        try {
            db.delete(
                SmartGridDbHelper.TABLE_RESERVATION,
                "${SmartGridDbHelper.COL_BUCKET} = ?",
                arrayOf(bucket)
            )
            reservations.forEach { reservation ->
                db.insertWithOnConflict(
                    SmartGridDbHelper.TABLE_RESERVATION,
                    null,
                    reservation.toValues(bucket, now),
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Writes a single booking, used after it is created, changed or cancelled. */
    fun upsert(bucket: String, reservation: ReservationDto) {
        helper.writableDatabase.insertWithOnConflict(
            SmartGridDbHelper.TABLE_RESERVATION,
            null,
            reservation.toValues(bucket, System.currentTimeMillis()),
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /** Reads back one cached list, soonest slot first. */
    fun readBucket(bucket: String): List<ReservationDto> = query(
        selection = "${SmartGridDbHelper.COL_BUCKET} = ?",
        args = arrayOf(bucket)
    )

    /** Reads back one cached booking by id, or null when it was never cached. */
    fun readById(id: String): ReservationDto? = query(
        selection = "${SmartGridDbHelper.COL_RESERVATION_ID} = ?",
        args = arrayOf(id)
    ).firstOrNull()

    /** Drops the cached bookings, used when the session is cleared on sign-out. */
    fun clear() {
        helper.writableDatabase.delete(SmartGridDbHelper.TABLE_RESERVATION, null, null)
    }

    /** Runs one query and maps every row back into a reservation. */
    private fun query(selection: String, args: Array<String>): List<ReservationDto> {
        val reservations = mutableListOf<ReservationDto>()

        helper.readableDatabase.query(
            SmartGridDbHelper.TABLE_RESERVATION,
            null,
            selection,
            args,
            null,
            null,
            "${SmartGridDbHelper.COL_START} ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val row = CursorRow(cursor)
                reservations += ReservationDto(
                    id = row.text(SmartGridDbHelper.COL_RESERVATION_ID).orEmpty(),
                    prosumerNic = row.text(SmartGridDbHelper.COL_NIC),
                    stationId = row.text(SmartGridDbHelper.COL_STATION_ID),
                    slotId = row.text(SmartGridDbHelper.COL_SLOT_ID),
                    type = row.text(SmartGridDbHelper.COL_TYPE),
                    energyKWh = row.real(SmartGridDbHelper.COL_ENERGY_KWH),
                    reservationStart = row.text(SmartGridDbHelper.COL_START),
                    reservationEnd = row.text(SmartGridDbHelper.COL_END),
                    status = row.text(SmartGridDbHelper.COL_STATUS),
                    qrToken = row.text(SmartGridDbHelper.COL_QR_TOKEN),
                    cancellationReason = row.text(SmartGridDbHelper.COL_CANCEL_REASON)
                )
            }
        }

        return reservations
    }

    /** Flattens one booking into the column values the table expects. */
    private fun ReservationDto.toValues(bucket: String, cachedAt: Long): ContentValues =
        ContentValues().apply {
            put(SmartGridDbHelper.COL_RESERVATION_ID, id)
            put(SmartGridDbHelper.COL_NIC, prosumerNic)
            put(SmartGridDbHelper.COL_STATION_ID, stationId)
            put(SmartGridDbHelper.COL_SLOT_ID, slotId)
            put(SmartGridDbHelper.COL_TYPE, type)
            put(SmartGridDbHelper.COL_ENERGY_KWH, energyKWh)
            put(SmartGridDbHelper.COL_START, reservationStart)
            put(SmartGridDbHelper.COL_END, reservationEnd)
            put(SmartGridDbHelper.COL_STATUS, status)
            put(SmartGridDbHelper.COL_QR_TOKEN, qrToken)
            put(SmartGridDbHelper.COL_CANCEL_REASON, cancellationReason)
            put(SmartGridDbHelper.COL_BUCKET, bucket)
            put(SmartGridDbHelper.COL_CACHED_AT, cachedAt)
        }
}
