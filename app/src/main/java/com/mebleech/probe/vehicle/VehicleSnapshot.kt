package com.mebleech.probe.vehicle

import androidx.car.app.hardware.info.EnergyProfile

/**
 * Instantánea mutable con el último valor conocido de cada campo del vehículo.
 * Un campo vale `null` cuando aún no hay datos o el fabricante no lo expone.
 * `unavailable` recoge los nombres de los campos que VW no rellena (N/D).
 */
data class VehicleSnapshot(
    var timestampMillis: Long = System.currentTimeMillis(),
    var modelName: String? = null,
    var modelManufacturer: String? = null,
    var batteryPercent: Float? = null,
    var rangeRemainingMeters: Float? = null,
    var energyIsLow: Boolean? = null,
    var fuelPercent: Float? = null,
    var connectorTypes: List<Int>? = null,
    var speedMetersPerSecond: Float? = null,
    var odometerMeters: Float? = null,
    var evChargePortConnected: Boolean? = null,
    var evChargePortOpen: Boolean? = null,
    val unavailable: MutableSet<String> = mutableSetOf()
) {
    fun connectorText(): String {
        val types = connectorTypes ?: return ""
        if (types.isEmpty()) return "desconocido"
        return types.joinToString(",") { connectorName(it) }
    }

    companion object {
        fun connectorName(type: Int): String = when (type) {
            EnergyProfile.EVCONNECTOR_TYPE_UNKNOWN -> "?"
            EnergyProfile.EVCONNECTOR_TYPE_J1772 -> "J1772"
            EnergyProfile.EVCONNECTOR_TYPE_MENNEKES -> "Mennekes (Tipo 2)"
            EnergyProfile.EVCONNECTOR_TYPE_CHADEMO -> "CHAdeMO"
            EnergyProfile.EVCONNECTOR_TYPE_COMBO_1 -> "CCS1"
            EnergyProfile.EVCONNECTOR_TYPE_COMBO_2 -> "CCS2"
            EnergyProfile.EVCONNECTOR_TYPE_TESLA_ROADSTER -> "Tesla Roadster"
            EnergyProfile.EVCONNECTOR_TYPE_TESLA_HPWC -> "Tesla HPWC"
            EnergyProfile.EVCONNECTOR_TYPE_TESLA_SUPERCHARGER -> "Tesla SC"
            EnergyProfile.EVCONNECTOR_TYPE_GBT -> "GBT AC"
            EnergyProfile.EVCONNECTOR_TYPE_GBT_DC -> "GBT DC"
            EnergyProfile.EVCONNECTOR_TYPE_SCAME -> "SCAME"
            EnergyProfile.EVCONNECTOR_TYPE_OTHER -> "otro"
            else -> "tipo $type"
        }
    }
}
