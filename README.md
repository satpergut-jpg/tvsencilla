# TV Sencilla — app IPTV para Android TV y Fire TV

App IPTV nativa pensada para **personas mayores**: pocas opciones, elementos grandes, todo siempre
en el mismo sitio y manejo 100 % con el mando a distancia.

La app solo reproduce fuentes que el usuario tenga derecho a ver: cuentas Xtream Codes o listas M3U
de su propio proveedor. No incluye ni descubre contenido por su cuenta.

Funciona en **Android TV, Google TV y Fire TV** (Android 6 o superior). No funciona en Samsung
(Tizen), LG (webOS), Roku ni Apple TV, que no ejecutan apps Android.

---

## Compilar y probar

Requisitos: JDK 17+ (sirve el que trae Android Studio) y el SDK de Android con **android-36**.

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:testDebugUnitTest
```

La versión de publicación (3,5 MB, optimizada) se genera con:

```bash
./gradlew :app:assembleRelease
```

Para instalarla hay que firmarla con una clave propia (ver «Pendiente»).

### Instalar en un Fire TV o Android TV por la red

En el televisor: activar **Depuración ADB** en las opciones de desarrollador y anotar su IP.

```bash
adb connect 192.168.1.109:5555
```

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Tests

90 tests cubren la lógica que no depende de Android: marcación numérica, numeración de favoritos,
orden de favoritos, parsers M3U y XMLTV, reconexión, formato del directo, búsqueda (números en
letra, palabras juntas, frases habladas), selección de canal por voz y limpieza de nombres.

## Verificado en un Fire TV Stick 4K real (Android 11) con un proveedor Xtream

- Inicio de sesión, TV en directo, cambio de canal en menos de 2 s, lista encima del vídeo.
- Favoritos desde la lista y desde la barra del reproductor; «Me gusta» en películas y series.
- Seguir viendo: la película se reanuda en el minuto guardado.
- Búsqueda con la lista real («antena tres» → Antena 3).
- La versión de publicación optimizada descarga y lee bien los datos del proveedor.

---

## Arquitectura

MVVM con una capa de dominio ligera, en un único módulo `app`.

```
ui/        Compose for TV: pantallas, ViewModels, tema, componentes, teclas del mando
domain/    Modelos, interfaces de repositorio y la lógica pura (marcación, numeración, búsqueda)
data/      Room, DataStore, credenciales cifradas, parsers y proveedores de contenido
player/    ExoPlayer: fábrica, reconexión, memoria de formato del directo, errores
di/        Módulos de Hilt
```

Decisiones que conviene conocer:

- **Las teclas del mando van por `RemoteKeyBus`**, no por el foco: números, CH+/CH− y teclas
  multimedia funcionan estén donde estén. Si ninguna pantalla escucha, la tecla pasa al sistema.
- **Sin favoritos, el mando usa la lista completa.** Si no, alguien que estrena la app no podría
  cambiar de canal.
- **El formato del directo se aprende.** Hay paneles que anuncian HLS y sirven MPEG-TS; tras el
  primer canal, los siguientes se piden ya en el formato que funciona.
- **Un canal que no arranca en 15 s** muestra «Este canal no responde» en vez de dejar esperando.
- **Nada se carga entero en memoria**: M3U como secuencia y XMLTV con SAX, en lotes de 500.
- **Las credenciales van cifradas** y nunca llegan a los registros.
- **Borrar caché** solo borra lo que se vuelve a descargar; favoritos, «Me gusta» y progreso se
  conservan. La base de datos se actualiza con migraciones, no borrándola.

---

## Pendiente

- **Firma propia para repartir la app.** Hoy solo existe la clave de pruebas de este PC. Hace
  falta crear una clave (con contraseña que guarde el dueño) para publicar actualizaciones.
- **Búsqueda por voz en Fire TV.** El Fire TV no ofrece reconocimiento de voz a otras apps: se
  dicta con el botón de Alexa en el teclado, y ese texto no pone el canal automáticamente.
- **Guía de programación incompleta.** Muchos canales del proveedor no traen guía («Sin
  información de programación»). Depende del proveedor.
- **Normalización de volumen entre canales** y **perfiles de usuario**: no implementados.
