/* ============================================================================
 * File        : SmartGridDbHelper.kt
 * Purpose     : Raw SQLite database helper for the SmartGrid mobile client.
 *               Holds the local session/user cache and the reference data cached
 *               from the Web API (grid nodes and reservations), as required by
 *               the assignment ("pure native Android with local SQLite, no
 *               frameworks"). The cache never decides anything - every business
 *               rule stays in the service.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SmartGridDbHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    /** Creates the local schema the first time the app runs on the device. */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_SESSION_TABLE)
        db.execSQL(CREATE_STATION_TABLE)
        db.execSQL(CREATE_RESERVATION_TABLE)
    }

    /** Local cache only, so an upgrade simply rebuilds the tables. */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESSION")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STATION")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RESERVATION")
        onCreate(db)
    }

    companion object {
        private const val DB_NAME = "smartgrid.db"

        /** 2 added the station and reservation reference-data tables. */
        private const val DB_VERSION = 2

        const val TABLE_SESSION = "session"
        const val TABLE_STATION = "station"
        const val TABLE_RESERVATION = "reservation"

        const val COL_ID = "id"
        const val COL_TOKEN = "token"
        const val COL_EXPIRES_AT = "expires_at"
        const val COL_NIC = "nic"
        const val COL_FULL_NAME = "full_name"
        const val COL_EMAIL = "email"
        const val COL_PHONE = "phone"
        const val COL_ADDRESS = "address"
        const val COL_CAPACITY = "solar_capacity_kw"
        const val COL_ROLE = "role"
        const val COL_STATUS = "status"

        // ---- Cached grid nodes (GET /stations) -----------------------------
        const val COL_STATION_ID = "station_id"
        const val COL_STATION_NAME = "station_name"
        const val COL_LATITUDE = "latitude"
        const val COL_LONGITUDE = "longitude"
        const val COL_CAPACITY_KWH = "capacity_kwh"
        const val COL_TOTAL_SLOTS = "total_battery_slots"
        const val COL_SCHEDULE = "operating_schedule"
        const val COL_IS_ACTIVE = "is_active"

        // ---- Cached reservations (GET /reservations/...) --------------------
        const val COL_RESERVATION_ID = "reservation_id"
        const val COL_SLOT_ID = "slot_id"
        const val COL_TYPE = "type"
        const val COL_ENERGY_KWH = "energy_kwh"
        const val COL_START = "reservation_start"
        const val COL_END = "reservation_end"
        const val COL_QR_TOKEN = "qr_token"
        const val COL_CANCEL_REASON = "cancellation_reason"

        /** Which list a cached reservation came from, so the two can be read back apart. */
        const val COL_BUCKET = "bucket"
        const val BUCKET_UPCOMING = "upcoming"
        const val BUCKET_HISTORY = "history"

        /** When the row was last written, for showing how stale the cache is. */
        const val COL_CACHED_AT = "cached_at"

        /**
         * Single-row table: the CHECK constraint pins the primary key to 1 so the
         * device can only ever hold one signed-in session at a time.
         */
        private const val CREATE_SESSION_TABLE = """
            CREATE TABLE $TABLE_SESSION (
                $COL_ID INTEGER PRIMARY KEY CHECK ($COL_ID = 1),
                $COL_TOKEN TEXT NOT NULL,
                $COL_EXPIRES_AT TEXT,
                $COL_NIC TEXT NOT NULL,
                $COL_FULL_NAME TEXT,
                $COL_EMAIL TEXT,
                $COL_PHONE TEXT,
                $COL_ADDRESS TEXT,
                $COL_CAPACITY REAL,
                $COL_ROLE TEXT,
                $COL_STATUS TEXT
            )
        """

        /** Grid nodes last returned by the service, keyed by their server id. */
        private const val CREATE_STATION_TABLE = """
            CREATE TABLE $TABLE_STATION (
                $COL_STATION_ID TEXT PRIMARY KEY,
                $COL_STATION_NAME TEXT,
                $COL_LATITUDE REAL,
                $COL_LONGITUDE REAL,
                $COL_CAPACITY_KWH REAL,
                $COL_TOTAL_SLOTS INTEGER,
                $COL_SCHEDULE TEXT,
                $COL_IS_ACTIVE INTEGER,
                $COL_CACHED_AT INTEGER NOT NULL
            )
        """

        /**
         * Reservations last returned for the signed-in prosumer. Status and the QR
         * token are stored as the service reported them; nothing here is authoritative.
         */
        private const val CREATE_RESERVATION_TABLE = """
            CREATE TABLE $TABLE_RESERVATION (
                $COL_RESERVATION_ID TEXT PRIMARY KEY,
                $COL_NIC TEXT,
                $COL_STATION_ID TEXT,
                $COL_SLOT_ID TEXT,
                $COL_TYPE TEXT,
                $COL_ENERGY_KWH REAL,
                $COL_START TEXT,
                $COL_END TEXT,
                $COL_STATUS TEXT,
                $COL_QR_TOKEN TEXT,
                $COL_CANCEL_REASON TEXT,
                $COL_BUCKET TEXT NOT NULL,
                $COL_CACHED_AT INTEGER NOT NULL
            )
        """
    }
}
