# Musica — Reproductor de Audio Nativo

Un reproductor de música moderno y nativo para Android construido con Jetpack Compose y Clean Architecture. Escanea automáticamente las canciones almacenadas en el dispositivo, las organiza en bibliotecas (canciones, favoritos, reproducciones recientes, listas de reproducción y artistas) y adapta la interfaz visual en tiempo real generando paletas de colores dinámicas a partir de la carátula del tema en reproducción.

---

## Stack Tecnológico y Librerías

A continuación se detallan las herramientas utilizadas en el proyecto, sus versiones actuales y el motivo de su elección:

| Tecnología / Librería | Versión | Para qué se usa en esta app | Por qué se eligió |
| :--- | :--- | :--- | :--- |
| **Jetpack Compose** | `BOM 2026.02.01` (`Kotlin 2.2.10`) | Construcción de toda la interfaz de usuario (UI). | Permite crear interfaces declarativas en código Kotlin puro, eliminando los layouts XML tradicionales y facilitando animaciones complejas. |
| **Room Database** | `2.8.5` | Persistencia local de canciones, listas de reproducción, historial y caché de colores. | Es la solución oficial de Android para SQLite. Garantiza consultas seguras en tiempo de compilación y se integra nativamente con Flow. |
| **Media3 ExoPlayer & Session** | `1.11.1` | Motor de reproducción de audio y servicio multimedia en segundo plano (`PlaybackService`). | Reemplaza al antiguo MediaPlayer. Maneja el estado de reproducción, notificaciones multimedia del sistema y controles en segundo plano. |
| **Coil Compose** | `2.7.0` | Carga eficiente de carátulas e imágenes de álbumes. | Diseñada para Kotlin y Compose. Se integra con EmbeddedArtworkFetcher para extraer carátulas incrustadas en archivos MP3/FLAC. |
| **AndroidX Palette** | `1.0.0` | Extracción de paletas de colores cromáticas desde las carátulas. | Permite extraer colores dominantes y vibrantes de cualquier imagen para personalizar el tema visual de la aplicación. |
| **Haze** | `1.3.1` | Efectos visuales de difuminado y cristal (blur/glassmorphism). | Proporciona un desenfoque elegante tipo cristal en la pantalla del reproductor (`PlayerScreen`) sin penalizar el rendimiento. |
| **Navigation Compose** | `2.10.1` | Enrutamiento e itinerario entre la pantalla principal y el reproductor. | Permite transiciones fluidas entre pantallas en Compose gestionando la pila de navegación de forma sencilla. |
| **KSP (Kotlin Symbol Processing)** | `2.1.10-1.0.29` | Procesamiento de anotaciones para la generación de código de Room. | Sustituye a kapt ofreciendo tiempos de compilación significativamente más rápidos. |

---

## Estructura de Carpetas

El código está organizado siguiendo los principios de Clean Architecture:

* `data/`: Capa de Datos — Maneja el almacenamiento local (Room, DAOs, Entidades) y la lectura del dispositivo (`MediaStoreRepository`).
* `domain/`: Capa de Dominio — Contiene la lógica de negocio pura (modelos, sincronización de biblioteca, analizador de carátulas y gestor de reproducción).
* `presentation/`: Capa de Presentación — Contiene la interfaz gráfica en Compose organizada en componentes, pantalla principal (`HomeScreen`) y reproductor (`PlayerScreen`).
* `service/`: Capa de Servicios — Aloja `PlaybackService` para reproducción en segundo plano y el procesador de ondas de audio (`AudioVisualizerProcessor`).
* `ui/`: Capa de Tema — Almacena la tipografía, colores del sistema y el motor de temas dinámicos (`DynamicColors.kt`).

---

## Arquitectura Detallada por Pantalla Principal

### 1. Pantalla Principal (`HomeScreen.kt`)
* **Propósito**: Es el centro neurálgico de la aplicación donde el usuario explora su biblioteca musical, realiza búsquedas y gestiona sus listas de reproducción.
* **Patrón de Arquitectura**: Sigue el patrón MVI/MVVM. Se conecta a `HomeViewModel`, el cual expone el estado global de la pantalla en una única estructura inmutable llamada `HomeUiState` mediante `StateFlow`.
* **Clases y Componentes que utiliza**:
  - `HomeScreen`: Composable principal que estructura la pantalla con `Scaffold`, conteniendo la barra superior, la búsqueda, la barra de pestañas, el reproductor flotante inferior y el menú desplegable de ordenación.
  - `HorizontalPager` & `rememberPagerState`: Componentes de Compose que permiten la navegación deslizable horizontalmente entre las 5 pestañas de la biblioteca.
  - `HomeSearchBar`: Campo de entrada de texto (`TextField`) para filtrar canciones o artistas en tiempo real sin bloquear el hilo principal.
  - `HomeTabRow`: Barra de pestañas (`ScrollableTabRow` y `Tab`) sincronizada bidireccionalmente con el `HorizontalPager`. Tocar una pestaña desplaza el Pager y deslizar el Pager actualiza el indicador de pestaña.
  - `HomeSortDropdownMenu`: Menú flotante que permite cambiar el criterio de ordenación (A-Z, Z-A, Más escuchadas, Menos escuchadas) ejecutando `HomeViewModel.setSortOption()`.
  - `SongListTab`: Módulo que renderiza la lista de canciones en las pestañas "Canciones", "Favoritos" y "Recientes" utilizando `LazyColumn` y `SongCard`. Aplica transformaciones tridimensionales en vivo con `graphicsLayer` para lograr el efecto de rotación en esfera.
  - `PlaylistTab`: Módulo que muestra las listas de reproducción creadas por el usuario guardadas en la base de datos Room (`PlaylistEntity`).
  - `ArtistListTab` y `ArtistDetailTab`: Módulos para listar los artistas agrupados (`Artist`) y ver las canciones específicas de un artista seleccionado.
  - `MiniPlayer`: Reproductor flotante persistente en la parte inferior de la pantalla que muestra la canción en curso (`playbackState.currentSong`) y permite pausar, reanudar o abrir el reproductor a pantalla completa.
  - `SongContextMenu`: Menú contextual desplegable al pulsar el icono de opciones de una canción (reproducir, añadir/quitar de favoritos, agregar a playlist o eliminar del dispositivo).
  - `ProvideDynamicColors` & `LocalDynamicColors`: Proveedor de tema dinámico que calcula los colores dinámicos en tiempo real y evalúa el contraste con `ColorUtils.calculateContrast` para asegurar que el texto sea siempre legible.

### 2. Pantalla de Reproducción Extendida (`PlayerScreen.kt`)
* **Propósito**: Proporciona una experiencia inmersiva a pantalla completa dedicada a la reproducción en curso, mostrando la portada ampliada, el visualizador radial de audio y controles de sonido avanzados.
* **Patrón de Arquitectura**: Se conecta directamente con `HomeViewModel` y consume flujos en tiempo real expuestos por `PlaybackManager` (`playbackState`, `currentPalette` y `visualizerAmplitudes`).
* **Clases y Componentes que utiliza**:
  - `PlayerScreen`: Composable principal que dibuja el fondo con desenfoque dinámico usando la librería `Haze` (`hazeSource` y `hazeEffect`) sobre el arte del álbum.
  - `RotatingArtworkDisc`: Componente gráfico que renderiza la portada del álbum en formato de disco de vinilo y la hace rotar continuamente mientras la música se encuentra en estado de reproducción.
  - `RadialVisualizer`: Dibujante personalizado sobre un `Canvas` que transforma las amplitudes transmitidas por `AudioVisualizerProcessor` en barras o pulsos circulares alrededor del disco de vinilo.
  - `Slider`: Barra de progreso deslizable que permite al usuario avanzar o retroceder a un segundo específico de la canción llamando a `PlaybackManager.seekTo()`.
  - Controles de Transporte: Botones para reproducción/pausa (`playOrPause`), canción siguiente/anterior (`next`, `previous`), modo de repetición (`toggleRepeat` manejando `RepeatMode.OFF`, `ALL`, `ONE`) y modo aleatorio (`toggleShuffle`).
  - Control de Volumen y Ecualización: Controles para ajustar el nivel de audio de salida ejecutando `PlaybackManager.setVolume()`.

### 3. Capa de Negocio y Gestores Centrales (`Domain / Managers`)
* **`PlaybackManager.kt`**: Es el controlador central de la reproducción multimedia. Administra la conexión con `ExoPlayer` vía `MediaController`, gestiona la cola activa de canciones (`setQueue`), el bucle de actualización de posición (`startProgressTracker`) y registra las reproducciones en el historial con `HistoryManager`.
* **`LibrarySyncManager.kt`**: Coordina la sincronización entre los archivos físicos del almacenamiento (`MediaStoreRepository`) y la base de datos SQLite (`RoomRepository`).
* **`ArtworkAnalyzer.kt`**: Analiza las portadas de los archivos de audio con AndroidX `Palette` para extraer los colores dominantes y construir la paleta cromática (`ArtworkPalette`) e identidad visual (`CardIdentity`).
* **`VisualPreloadManager.kt`**: Precalcula y precarga en memoria las carátulas y paletas de las canciones adyacentes en la lista de reproducción para que las transiciones entre canciones sean instantáneas y sin tirones.
* **`AudioVisualizerProcessor.kt`**: Captura las frecuencias del audio en reproducción para generar el vector de amplitudes consumido por el visualizador radial.

### 4. Capa de Servicios y Persistencia (`Service / Data`)
* **`PlaybackService.kt`**: Servicio de Android derivado de `MediaSessionService` (Media3) que mantiene el proceso de reproducción activo en segundo plano aunque el usuario salga de la aplicación o bloquee la pantalla.
* **`AppDatabase.kt`**: Base de datos SQLite Room que define las tablas `songs`, `playlists`, `history` y `card_identities`, garantizando operaciones reactivas mediante sus DAOs (`SongDao`, `PlaylistDao`, `HistoryDao`, `CardIdentityDao`).

---

## Flujo Principal de Datos

1. **Escaneo de Medios (`MediaStore` -> `Room`)**:
   Al iniciar la app o pulsar el botón de Actualizar, `LibrarySyncManager` lee los archivos de audio del dispositivo mediante `MediaStoreRepository`.
2. **Persistencia e Identidad Cromática**:
   Las canciones se guardan en la base de datos SQLite con Room. Si una canción es nueva, `ArtworkAnalyzer` analiza su carátula, extrae sus colores y guarda su identidad cromática en la tabla `card_identities`.
3. **Flujo Reactivo (`Room` -> `HomeViewModel` -> `UI`)**:
   `HomeViewModel` observa la base de datos a través de `Flow`. Cualquier cambio en la base de datos actualiza el estado `HomeUiState` automáticamente.
4. **Colores Dinámicos y Contraste Legible**:
   `HomeScreen` escucha la canción en reproducción y envuelve la interfaz con `ProvideDynamicColors`. Este calcula el contraste entre el texto y el fondo con `ColorUtils.calculateContrast`, garantiza legibilidad (contraste mínimo de 4.5) y anima suavemente los colores globales con `tween(1000)`.

---

## Conceptos Clave para Estudiar

Si estás aprendiendo a programar en Android, te recomendamos estudiar estos conceptos presentes en el código:

1. **`StateFlow` y `SharedFlow`**: Flujos reactivos de Kotlin para emitir estados del ViewModel a la interfaz de forma segura.
2. **`CompositionLocal`**: Mecanismo de Compose para transmitir datos implícitos (como el tema dinámico `LocalDynamicColors`) a todo el árbol de pantallas.
3. **`Recomposición`**: El proceso en el cual Compose vuelve a renderizar únicamente las partes de la interfaz cuyos datos han cambiado.
4. **`LaunchedEffect` & `snapshotFlow`**: Herramientas de Compose para ejecutar tareas asíncronas y reaccionar a cambios de estado en segundo plano.
5. **`HorizontalPager` & `PagerState`**: Componente para crear navegación deslizable horizontalmente entre pestañas sincronizadas.
6. **`Clean Architecture`**: Separación del proyecto en capas independientes (data, domain, presentation) para mantener el código ordenado y testeable.

---

## Cómo Compilar y Ejecutar el Proyecto

### Requisitos Previos:
* **Android Studio** Ladybug (2024.2) o superior.
* **JDK 17** instalado y configurado.
* Dispositivo físico o emulador con **Android 7.0 (API 24)** o superior.

### Pasos:
1. Clonar el repositorio en tu equipo:
   ```bash
   git clone https://github.com/SrMrPandora/musica_reproductor.git
   ```
2. Abrir el proyecto en **Android Studio** y esperar a que finalice la sincronización de Gradle (Gradle Sync).
3. Conectar un teléfono Android con depuración USB activada o iniciar un emulador.
4. Seleccionar el módulo `app` en la barra superior y hacer clic en **Run** (`Shift + F10` / `Ctrl + R`).
5. Opcionalmente, para generar el APK desde la consola:
   ```bash
   ./gradlew assembleDebug
   ```
