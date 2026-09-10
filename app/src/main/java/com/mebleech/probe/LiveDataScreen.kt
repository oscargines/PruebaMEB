package com.mebleech.probe

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mebleech.probe.vehicle.VehicleDataProvider
import java.util.Locale

/**
 * Pantalla en vivo: muestra la última instantánea del vehículo y se refresca
 * con cada dato nuevo. Incluye un botón (solo cuando el coche está parado)
 * para conceder los permisos de hardware del vehículo.
 */
class LiveDataScreen(
    carContext: CarContext,
    private val dataProvider: VehicleDataProvider
) : Screen(carContext) {

    private val onUpdate = { invalidate() }

    init {
        dataProvider.onUpdate = onUpdate
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                if (dataProvider.onUpdate === onUpdate) {
                    dataProvider.onUpdate = null
                }
            }
        })
    }

    override fun onGetTemplate(): Template {
        val s = dataProvider.currentSnapshot
        val permissionAction = Action.Builder()
            .setTitle("Conceder permisos")
            .setOnClickListener(
                ParkedOnlyOnClickListener.create {
                    dataProvider.requestPermissions {
                        dataProvider.start()
                        invalidate()
                    }
                }
            )
            .build()

        val builder = Pane.Builder()
            .addRow(row("Vehículo", listOfNotNull(s.modelManufacturer, s.modelName).joinToString(" ").ifEmpty { "N/D" }))
            .addRow(row("Batería", listOfNotNull(
                s.batteryPercent?.let { "$it %" },
                s.rangeRemainingMeters?.let { fmt1(it / 1000.0) + " km" }
            ).joinToString(" / ").ifEmpty { "N/D" }))
            .addRow(row("Carga", listOfNotNull(
                s.connectorText().ifEmpty { null },
                s.evChargePortConnected?.let { if (it) "enchufado" else "desenchufado" }
            ).joinToString(" / ").ifEmpty { "N/D" }))
            .addRow(row("Conducción", listOfNotNull(
                s.speedMetersPerSecond?.let { fmt0(it * 3.6) + " km/h" },
                s.odometerMeters?.let { fmt1(it / 1000.0) + " km" }
            ).joinToString(" / ").ifEmpty { "N/D" }))

        if (!dataProvider.hasRequiredPermissions()) {
            builder.addAction(permissionAction)
        }

        return PaneTemplate.Builder(builder.build())
            .setHeader(
                Header.Builder()
                    .setTitle("MEB Probe")
                    .setStartHeaderAction(Action.APP_ICON)
                    .build()
            )
            .build()
    }

    private fun row(title: String, text: String): Row =
        Row.Builder().setTitle(title).addText(text).build()

    private fun fmt0(v: Double): String = String.format(Locale.US, "%.0f", v)
    private fun fmt1(v: Double): String = String.format(Locale.US, "%.1f", v)
}
