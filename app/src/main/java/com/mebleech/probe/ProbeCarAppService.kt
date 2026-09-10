package com.mebleech.probe

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Servicio de Android Auto de la app.
 *
 * Para uso personal (sideload) se acepta cualquier host; para publicar en
 * Play Store habría que restringir el validador al host oficial de Google.
 */
class ProbeCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = ProbeSession()
}
