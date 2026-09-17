/* ============================================================================
 * File        : SmartGridDbHelper.kt
 * Purpose     : Raw SQLite database helper for the SmartGrid mobile client.
 *               Holds the local session/user cache required by the assignment
 *               ("pure native Android with local SQLite, no frameworks").
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
    }

    /** Local cache only, so an upgrade simply rebuilds the tables. */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESSION")
        onCreate(db)
    }

    companion object {
        private const val DB_NAME = "smartgrid.db"
        private const val DB_VERSION = 1

        const val TABLE_SESSION = "session"

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
    }
}
