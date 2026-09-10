# PruebaMEB

Aplicación educativa para estudiar cómo una aplicación Android basada en
Android for Cars App Library puede comunicarse con Android Auto y mostrar
información expuesta por un Volkswagen ID.4.

El proyecto no es una aplicación oficial de Volkswagen, Android Auto ni Google.
Su finalidad es exclusivamente experimental y formativa.

## Características

- Integración con Android Auto mediante `CarAppService` y plantillas de Android for Cars.
- Pantalla compacta de telemetría para el Volkswagen ID.4.
- Lectura de modelo, fabricante, batería, autonomía y estado de carga.
- Lectura de velocidad y odómetro cuando el vehículo y Android Auto los exponen.
- Solicitud de permisos desde la interfaz del coche.
- Registro local de instantáneas en un archivo CSV privado de la aplicación.
- Pruebas mediante Desktop Head Unit o un vehículo real compatible.
- Compatibilidad probada únicamente con Volkswagen ID.4.

## Datos y privacidad

La aplicación no utiliza servidores propios, cuentas de usuario, publicidad ni
analítica. Los datos recibidos se procesan localmente y el registro CSV se
guarda en el almacenamiento privado de la aplicación.

La aplicación solicita permisos de ubicación y de acceso a datos del vehículo
porque Android Auto y la API de Android for Cars pueden requerirlos para
proporcionar la información disponible del coche. La aplicación no envía esos
datos a un servidor propio.

Consulta la [política de privacidad](docs/privacy-policy.html).

## Requisitos

- Android Studio.
- JDK 17.
- Android SDK con API 36.
- Un teléfono Android compatible con Android Auto.
- Android Auto actualizado.
- Un Volkswagen ID.4 para la prueba en vehículo real, o Desktop Head Unit para pruebas locales.

## Compilación

1. Clona el repositorio.
2. Abre el proyecto en Android Studio.
3. Configura `local.properties` con la ruta local del Android SDK.
4. Compila la variante de depuración:

   ```text
   .\gradlew.bat assembleDebug
   ```

La APK de depuración se genera en `app/build/outputs/apk/debug/`. Los
artefactos generados no forman parte del repositorio.

Para una compilación release se necesita un keystore local propio. No se debe
subir nunca el keystore ni sus contraseñas a GitHub.

## Instalación local

Con la depuración USB autorizada:

```text
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

El paquete de la aplicación es:

```text
com.oscargines.pruebameb
```

Abre la aplicación una vez en el teléfono y concede los permisos cuando
Android Auto los solicite con el vehículo estacionado.

## Desktop Head Unit

El Desktop Head Unit permite observar la interfaz sin conectar el teléfono a
un coche.

1. Instala `Android Auto Desktop Head Unit Emulator` desde las herramientas del SDK.
2. Activa el modo desarrollador de Android Auto.
3. Inicia `Start head unit server` en Android Auto.
4. Conecta el teléfono por USB.
5. Ejecuta:

   ```text
   adb forward tcp:5277 tcp:5277
   ```

6. Ejecuta `desktop-head-unit.exe` desde `extras/google/auto/` del SDK.

El DHU no proporciona necesariamente datos reales del vehículo. Los campos no
expuestos por el host aparecen como `N/D`.

## Prueba en vehículo

La aplicación está pensada para Android Auto proyectado desde el teléfono.
Debe probarse con el vehículo estacionado al conceder permisos. La experiencia
puede variar según la versión de Android Auto, el teléfono y los datos que el
Volkswagen ID.4 exponga al host.

## Registro CSV

El registro se guarda en el almacenamiento privado de la aplicación como:

```text
files/meb_probe_log.csv
```

Durante una prueba local puede extraerse con:

```text
adb exec-out run-as com.oscargines.pruebameb cat files/meb_probe_log.csv
```

## Licencia

Este proyecto se distribuye bajo la licencia [MIT](LICENSE).
