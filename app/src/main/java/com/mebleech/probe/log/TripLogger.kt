package com.mebleech.probe.log

import android.content.Context
import com.mebleech.probe.vehicle.VehicleSnapshot
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registra en CSV (una fila por segundo) la instantánea más reciente del
 * vehículo. El fichero se crea/amplía en filesDir; se extrae con:
 *   adb exec-out run-as com.oscargines.pruebameb cat files/meb_probe_log.csv
 */
class TripLogger {
    companion object {
        private const val FILE_NAME = "meb_probe_log.csv"
        private const val MIN_INTERVAL_MS = 1000L
        private const val HEADER =
            "ts;soc_pct;range_km;speed_kmh;odo_km;energy_low;plug_connected;plug_open;connector;model;manufacturer;unavailable"
    }

    private var writer: PrintWriter? = null
    private var lastWriteAt = 0L
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun open(context: Context) {
        close()
        val file = File(context.filesDir, FILE_NAME)
        val isNew = !file.exists()
        val w = PrintWriter(FileWriter(file, true), true)
        if (isNew) w.println(HEADER)
        writer = w
    }

    fun record(snapshot: VehicleSnapshot) {
        val w = writer ?: return
        if (snapshot.timestampMillis - lastWriteAt < MIN_INTERVAL_MS) return
        lastWriteAt = snapshot.timestampMillis
        w.println(line(snapshot))
    }

    fun close() {
        writer?.close()
        writer = null
    }

    private fun line(s: VehicleSnapshot): String {
        val ts = dateFormat.format(Date(s.timestampMillis))
        return listOf(
            ts,
            s.batteryPercent?.toString() ?: "",
            s.rangeRemainingMeters?.let { String.format(Locale.US, "%.1f", it / 1000.0) } ?: "",
            s.speedMetersPerSecond?.let { String.format(Locale.US, "%.1f", it * 3.6) } ?: "",
            s.odometerMeters?.let { String.format(Locale.US, "%.1f", it / 1000.0) } ?: "",
            s.energyIsLow?.let { if (it) "1" else "0" } ?: "",
            s.evChargePortConnected?.let { if (it) "1" else "0" } ?: "",
            s.evChargePortOpen?.let { if (it) "1" else "0" } ?: "",
            s.connectorText(),
            s.modelName ?: "",
            s.modelManufacturer ?: "",
            s.unavailable.sorted().joinToString(";")
        ).joinToString(";")
    }
}
