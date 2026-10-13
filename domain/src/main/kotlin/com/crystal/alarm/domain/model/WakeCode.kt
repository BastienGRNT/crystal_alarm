package com.crystal.alarm.domain.model

/**
 * A physical QR code / barcode saved by the user. The active one must be scanned to stop the alarm.
 *
 * @property name label chosen by the user ("Dentifrice", "Café"…).
 * @property value raw decoded content of the code.
 * @property format human readable format label (e.g. "QR", "EAN-13").
 */
data class WakeCode(
    val id: String,
    val name: String,
    val value: String,
    val format: String,
) {
    init {
        require(value.isNotBlank()) { "A wake code cannot be blank" }
    }

    fun matches(scanned: String): Boolean = value == scanned.trim()

    companion object {
        const val MAX_NAME_LENGTH = 30
    }
}
