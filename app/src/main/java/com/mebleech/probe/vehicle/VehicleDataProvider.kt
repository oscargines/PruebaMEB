package com.mebleech.probe.vehicle

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.OnRequestPermissionsListener
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.CarInfo
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.info.EnergyProfile
import androidx.car.app.hardware.info.EvStatus
import androidx.car.app.hardware.info.Mileage
import androidx.car.app.hardware.info.Model
import androidx.car.app.hardware.info.Speed
import androidx.core.content.ContextCompat

/**
 * Lee los datos del vehículo que la pantalla del coche expone a Android Auto
 * vía la API CarInfo de la Car App Library. Sin OBD, sin servidores VW.
 *
 * Cada callback actualiza la instantánea y notifica; los campos que el
 * fabricante no rellena quedan marcados en [VehicleSnapshot.unavailable].
 */
class VehicleDataProvider(
    private val carContext: CarContext,
    private val onSnapshot: (VehicleSnapshot) -> Unit
) {
    private val carInfo: CarInfo =
        carContext.getCarService(CarHardwareManager::class.java).carInfo

    private val snapshot = VehicleSnapshot()

    val currentSnapshot: VehicleSnapshot get() = snapshot

    /** Callback de la UI para refrescar la plantilla. */
    var onUpdate: (() -> Unit)? = null

    private var energyListener: OnCarDataAvailableListener<EnergyLevel>? = null
    private var evListener: OnCarDataAvailableListener<EvStatus>? = null
    private var speedListener: OnCarDataAvailableListener<Speed>? = null
    private var mileageListener: OnCarDataAvailableListener<Mileage>? = null
    private var started = false

    fun start() {
        if (started) return
        if (!hasRequiredPermissions()) {
            Log.w(TAG, "No se inicia la lectura: faltan permisos de hardware")
            return
        }
        started = true
        val ex = ContextCompat.getMainExecutor(carContext)

        carInfo.fetchEnergyProfile(ex) {
            handleProfile(it)
            push()
        }
        carInfo.fetchModel(ex) {
            handleModel(it)
            push()
        }

        energyListener = OnCarDataAvailableListener { data -> handleEnergy(data); push() }
        evListener = OnCarDataAvailableListener { data -> handleEvStatus(data); push() }
        speedListener = OnCarDataAvailableListener { data -> handleSpeed(data); push() }
        mileageListener = OnCarDataAvailableListener { data -> handleMileage(data); push() }

        carInfo.addEnergyLevelListener(ex, energyListener!!)
        carInfo.addEvStatusListener(ex, evListener!!)
        carInfo.addSpeedListener(ex, speedListener!!)
        carInfo.addMileageListener(ex, mileageListener!!)
    }

    fun stop() {
        energyListener?.let { runCatching { carInfo.removeEnergyLevelListener(it) } }
        evListener?.let { runCatching { carInfo.removeEvStatusListener(it) } }
        speedListener?.let { runCatching { carInfo.removeSpeedListener(it) } }
        mileageListener?.let { runCatching { carInfo.removeMileageListener(it) } }
        energyListener = null
        evListener = null
        speedListener = null
        mileageListener = null
        started = false
    }

    /** Solicita los permisos de hardware de coche (debe llamarse parado). */
    fun requestPermissions(onComplete: () -> Unit = {}) {
        val missing = REQUIRED_PERMISSIONS.filter {
            carContext.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            onComplete()
            return
        }
        try {
            carContext.requestPermissions(
                missing,
                ContextCompat.getMainExecutor(carContext),
                object : OnRequestPermissionsListener {
                    override fun onRequestPermissionsResult(
                        approved: List<String>,
                        rejected: List<String>
                    ) {
                        Log.d(TAG, "Permisos aprobados=$approved rechazados=$rejected")
                        onComplete()
                    }
                }
            )
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo pedir permisos", t)
            onComplete()
        }
    }

    fun hasRequiredPermissions(): Boolean = REQUIRED_PERMISSIONS.all {
        carContext.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    private fun handleEnergy(data: EnergyLevel) {
        snapshot.batteryPercent = ok(data.batteryPercent)?.value
        snapshot.rangeRemainingMeters = ok(data.rangeRemainingMeters)?.value
        snapshot.energyIsLow = ok(data.energyIsLow)?.value
        snapshot.fuelPercent = ok(data.fuelPercent)?.value
        mark("battery", data.batteryPercent)
        mark("range", data.rangeRemainingMeters)
        mark("energyIsLow", data.energyIsLow)
        Log.d(TAG, "Energy status=${data.batteryPercent.status}, battery=${snapshot.batteryPercent}, range=${snapshot.rangeRemainingMeters}")
    }

    private fun handleProfile(data: EnergyProfile) {
        snapshot.connectorTypes = ok(data.evConnectorTypes)?.value
        mark("connector", data.evConnectorTypes)
        Log.d(TAG, "Profile status=${data.evConnectorTypes.status}, connector=${snapshot.connectorTypes}")
    }

    private fun handleModel(data: Model) {
        snapshot.modelName = ok(data.name)?.value
        snapshot.modelManufacturer = ok(data.manufacturer)?.value
        mark("model", data.name)
        mark("manufacturer", data.manufacturer)
        Log.d(TAG, "Model status name=${data.name.status}, manufacturer=${data.manufacturer.status}, value=${snapshot.modelName}/${snapshot.modelManufacturer}")
    }

    private fun handleEvStatus(data: EvStatus) {
        snapshot.evChargePortConnected = ok(data.evChargePortConnected)?.value
        snapshot.evChargePortOpen = ok(data.evChargePortOpen)?.value
        mark("plugConnected", data.evChargePortConnected)
        mark("plugOpen", data.evChargePortOpen)
    }

    private fun handleSpeed(data: Speed) {
        snapshot.speedMetersPerSecond = ok(data.rawSpeedMetersPerSecond)?.value
        mark("speed", data.rawSpeedMetersPerSecond)
    }

    private fun handleMileage(data: Mileage) {
        snapshot.odometerMeters = ok(data.odometerMeters)?.value
        mark("mileage", data.odometerMeters)
    }

    private fun <T> ok(v: CarValue<T>): CarValue<T>? =
        if (v.status == CarValue.STATUS_SUCCESS) v else null

    private fun mark(key: String, v: CarValue<*>) {
        if (v.status == CarValue.STATUS_SUCCESS) snapshot.unavailable.remove(key)
        else snapshot.unavailable.add(key)
    }

    private fun push() {
        snapshot.timestampMillis = System.currentTimeMillis()
        onSnapshot(snapshot.copy())
        onUpdate?.invoke()
    }

    companion object {
        private const val TAG = "MEBProbe"
        private const val PERMISSION_FUEL = "com.google.android.gms.permission.CAR_FUEL"
        private const val PERMISSION_SPEED = "com.google.android.gms.permission.CAR_SPEED"
        private const val PERMISSION_MILEAGE = "com.google.android.gms.permission.CAR_MILEAGE"
        val REQUIRED_PERMISSIONS = listOf(
            PERMISSION_FUEL,
            PERMISSION_SPEED,
            PERMISSION_MILEAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}
