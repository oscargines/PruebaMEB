package com.mebleech.probe

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mebleech.probe.log.TripLogger
import com.mebleech.probe.vehicle.VehicleDataProvider

/**
 * Sesión principal: arranca el proveedor de datos del vehículo y presenta la
 * pantalla en vivo. El log CSV se escribe en filesDir (extraíble con adb).
 */
class ProbeSession : Session() {

    private val tripLogger = TripLogger()
    private var dataProvider: VehicleDataProvider? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                dataProvider?.stop()
                tripLogger.close()
                dataProvider = null
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen {
        val provider = VehicleDataProvider(carContext) { tripLogger.record(it) }
        dataProvider = provider
        tripLogger.open(carContext)
        // El host debe conceder los permisos antes de suscribirnos al hardware.
        provider.requestPermissions {
            if (provider.hasRequiredPermissions()) {
                provider.start()
            }
        }
        return LiveDataScreen(carContext, provider)
    }
}
