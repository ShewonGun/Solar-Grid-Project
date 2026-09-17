/* ============================================================================
 * File        : CursorRow.kt
 * Purpose     : Small reader over a SQLite cursor that returns null for NULL
 *               columns, so the caches can build DTOs without repeating the
 *               same index-and-null-check dance for every field.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.data.local

import android.database.Cursor

class CursorRow(private val cursor: Cursor) {

    /** Reads a text column, or null when the column holds NULL. */
    fun text(name: String): String? = read(name) { cursor.getString(it) }

    /** Reads a floating-point column, or null when the column holds NULL. */
    fun real(name: String): Double? = read(name) { cursor.getDouble(it) }

    /** Reads an integer column, or null when the column holds NULL. */
    fun int(name: String): Int? = read(name) { cursor.getInt(it) }

    /** Reads a flag stored as 0 or 1, or null when the column holds NULL. */
    fun boolean(name: String): Boolean? = int(name)?.let { it != 0 }

    /** Resolves the column index once and guards it against a NULL value. */
    private fun <T> read(name: String, value: (Int) -> T): T? {
        val index = cursor.getColumnIndexOrThrow(name)
        return if (cursor.isNull(index)) null else value(index)
    }
}
