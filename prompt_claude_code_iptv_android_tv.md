# Prompt para Claude Code — App IPTV para Android TV (fácil para personas mayores)

Actúa como desarrollador senior de Android TV. Crea desde cero una app IPTV nativa para Android TV, pensada sobre todo para **personas mayores**: pocas opciones, elementos grandes, todo siempre en el mismo sitio y manejo 100% con el mando a distancia. La app solo reproducirá fuentes que el usuario tenga derecho a ver (listas M3U/Xtream Codes legítimas del proveedor).

Antes de escribir código, preséntame un plan breve (arquitectura, pantallas y fases) y espera mi confirmación. Después trabaja por fases, compilando y probando al final de cada una.

---

## 1. Stack técnico

- **Lenguaje:** Kotlin.
- **UI:** Jetpack Compose for TV (`androidx.tv:tv-material`), con tema oscuro.
- **Reproductor:** Media3 ExoPlayer (HLS, DASH, MPEG-TS), con `media3-ui` o controles propios en Compose.
- **Arquitectura:** MVVM + Clean Architecture ligera (ui / domain / data), un solo módulo `app` al principio.
- **Inyección de dependencias:** Hilt.
- **Red:** Retrofit + OkHttp; imágenes con Coil (con caché y tamaño reducido).
- **Persistencia:** Room (canales, favoritos, progreso de visionado, EPG) y DataStore (ajustes).
- **Fuentes de contenido:** lista M3U por URL, API Xtream Codes (live, VOD, series) y guía EPG en XMLTV. Crea una interfaz `ContentProvider` para poder añadir otras fuentes.
- **minSdk 23, targetSdk actual.** Manifest con `LEANBACK_LAUNCHER`, `android.software.leanback` requerido, `touchscreen` no requerido y banner de 320x180.
- Tests unitarios para la lógica (parser M3U, parser XMLTV, marcación numérica de canales, orden de favoritos).

## 2. Principios de diseño (10-foot UI + personas mayores)

- Texto mínimo **32sp** para contenido y ajuste "Tamaño de letra": Normal / Grande / Muy grande.
- Alto contraste y **foco muy visible**: borde grueso amarillo o blanco más ligero aumento de escala.
- Márgenes de seguridad de ~5% en todos los bordes (overscan).
- Iconos **siempre con texto**. Nada de menús ocultos, pulsaciones largas ni gestos.
- Sin textos en movimiento ni animaciones rápidas. Los menús no se cierran solos antes de 8–10 segundos.
- Navegación con D-pad predecible, sin elementos inalcanzables.
- Botón **Atrás** coherente en todas las pantallas. En Inicio, pedir confirmación: "¿Quiere salir de la app?".
- Mensajes en lenguaje sencillo, con botón **Reintentar** cuando proceda (ej.: "No hay internet. Revise el router").
- Pocas carátulas por fila (4–5) y título escrito debajo.
- Todos los textos en `strings.xml` (español por defecto, preparado para otros idiomas).

## 3. Pantallas

### 3.1 Primer arranque / activación
- Pantalla para introducir la fuente sin escribir con el mando: mostrar **código QR y código corto** para configurarla desde el móvil (deja una interfaz `RemoteSetupService` con implementación simulada; el backend se hará después).
- Alternativa manual: formulario con URL M3U o servidor/usuario/contraseña Xtream.

### 3.2 Inicio (muy simple)
- Solo 4 botones grandes: **TV en directo**, **Películas**, **Series**, **Favoritos**, más un botón pequeño de **Ajustes**.
- Fila "Seguir viendo" (películas/episodios a medias).
- Ajuste opcional: "Al abrir, ir directamente al último canal visto".

### 3.3 TV en directo
- Lista de canales con **número, logo grande y nombre**, y categorías sencillas (Noticias, Deportes, Cine, Infantil…).
- Al reproducir, **banner** de ~5 s: número · nombre · programa actual (con hora de fin) · siguiente programa.
- Guía EPG simplificada: **"Ahora" y "Después"** por canal. Cuadrícula completa solo como opción en Ajustes.
- Tecla **Info** muestra el banner de nuevo; tecla **Atrás** durante la reproducción abre la lista de canales (no sale).
- Botón o tecla para "canal anterior".
- Timeshift/catch-up solo si la fuente lo soporta (detectarlo y ocultar la opción si no).

### 3.4 Favoritos con numeración propia (función clave)
- La lista de Favoritos tiene **numeración propia 1, 2, 3…** según el orden elegido.
- Por defecto (modo sencillo), **los números del mando y CH+/CH− usan la lista de favoritos**. En Ajustes, opción para usar la numeración de la lista completa (nunca ambas a la vez).
- **Marcación numérica** (teclas `KEYCODE_0`–`KEYCODE_9` y teclado numérico):
  - Mostrar el número en grande en una esquina ("1_", "12_").
  - Esperar **2 segundos** por otra cifra (máximo 3 cifras); OK sintoniza al instante.
  - Si no existe: mensaje "No hay canal en el 25" y se mantiene el canal actual.
  - Debe funcionar tanto durante la reproducción como desde la lista de canales.
- **CH+/CH−** (`KEYCODE_CHANNEL_UP/DOWN`, y también arriba/abajo durante la reproducción a pantalla completa) recorren los favoritos en ese orden, de forma circular.
- **Pantalla "Ordenar favoritos"**:
  - Seleccionar canal con OK → moverlo con arriba/abajo → soltar con OK.
  - Opción "Poner en el número…": elegir canal y escribir su posición con el mando.
  - Añadir/quitar favoritos con un botón visible ("★ Añadir a favoritos").
  - Protegida con **PIN** (modo sencillo) para evitar desórdenes accidentales.
- Persistir el orden en Room y dejar una interfaz `FavoritesSyncRepository` para sincronizarlo con un servidor ligado al perfil (implementación simulada por ahora).
- Implementa la lógica de marcación en una clase aislada y testeable (`ChannelDialer`), sin depender de la UI.

### 3.5 Películas
- Carátulas grandes con título debajo, 4–5 por fila, categorías sencillas (Comedia, Drama, Cine clásico, Documentales…).
- Ficha: carátula, año, duración, sinopsis corta y botones grandes **Ver** / **Continuar** / **Empezar de nuevo**.
- Guardar progreso cada 10 s y al salir; mostrar en "Seguir viendo".

### 3.6 Series
- Ficha con selector de **temporada** y lista de **capítulos** clara (número, título, duración, marca de visto).
- **Reproducción automática del siguiente episodio** con cuenta atrás de 10 s y opción de cancelar.
- "Continuar viendo" recuerda el episodio y el minuto.

### 3.7 Búsqueda
- Búsqueda por **voz** (intent de reconocimiento de voz de Android TV) como opción principal.
- Teclado en pantalla con letras grandes como alternativa. Resultados agrupados: Canales, Películas, Series.

### 3.8 Reproductor
- Controles grandes: Play/Pausa, retroceder/avanzar 10 s, audio, subtítulos, calidad.
- Soporte de teclas `MEDIA_PLAY_PAUSE`, `MEDIA_PLAY`, `MEDIA_PAUSE`, `MEDIA_FAST_FORWARD`, `MEDIA_REWIND`.
- **Subtítulos grandes** con tamaño configurable y fondo semitransparente.
- **Reconexión automática** si el stream se corta (reintentos con espera progresiva) y mensaje "Reconectando…".
- Zapping rápido: buffer de arranque ajustado para cambiar de canal en menos de ~2 s; mostrar logo del canal mientras carga.
- Normalización de volumen entre canales si es viable (p. ej. `LoudnessEnhancer`); si no, déjalo documentado como mejora.
- Mantener la pantalla encendida durante la reproducción.

### 3.9 Ajustes
- **Modo sencillo** (activado por defecto) que oculta opciones avanzadas; salir de él requiere PIN.
- Tamaño de letra, tamaño de subtítulos, idioma de audio/subtítulos preferido.
- **Control parental** con PIN y ocultación de categorías.
- Perfiles de usuario (opcional, fase final).
- Cambiar PIN, cambiar fuente, "Configurar desde el móvil" (QR).
- Información de la app y botón "Borrar caché".

## 4. Rendimiento y robustez

- Carga diferida de listas (`LazyRow`/`LazyColumn` de TV) e imágenes redimensionadas.
- Parseo de M3U y XMLTV en segundo plano (corrutinas, `Dispatchers.IO`) y en streaming, sin cargar todo en memoria; las guías XMLTV pueden pesar mucho.
- Actualizar EPG en segundo plano con WorkManager (una vez al día).
- Estados claros en todas las pantallas: cargando, vacío, error (con Reintentar).
- Debe funcionar fluido en dispositivos modestos (Fire TV Stick, Android TV con 1–2 GB de RAM).
- No registrar contraseñas en logs; guardar credenciales con cifrado (EncryptedSharedPreferences o equivalente).

## 5. Fases de trabajo

1. Proyecto base, tema, navegación, pantalla de Inicio y Ajustes básicos.
2. Fuentes (M3U + Xtream), parser y base de datos. Tests del parser.
3. TV en directo + reproductor + banner + reconexión.
4. **Favoritos con numeración, marcación numérica, CH+/CH− y pantalla de ordenar.** Tests de `ChannelDialer`.
5. EPG "Ahora/Después".
6. Películas, Series, progreso y "Seguir viendo".
7. Búsqueda (voz + teclado).
8. Modo sencillo, PIN, control parental y accesibilidad (tamaños de letra).
9. Pulido: rendimiento, mensajes de error, README con instrucciones de compilación y prueba en emulador Android TV.

## 6. Criterios de aceptación

- Todo se puede hacer solo con el mando (D-pad, OK, Atrás, números, CH+/CH−).
- Pulsar "5" en el mando sintoniza el favorito nº 5 en ≤ 2 s tras la última cifra.
- El orden de favoritos se mantiene tras cerrar y reabrir la app.
- El foco siempre es visible y nunca se pierde al volver atrás.
- La app no se cierra con Atrás sin confirmación.
- Ningún texto de contenido es menor de 32sp con el tamaño Normal.
- El proyecto compila sin errores y los tests pasan.

Al terminar cada fase, resume qué has hecho, qué falta y cómo probarlo en el emulador de Android TV.
