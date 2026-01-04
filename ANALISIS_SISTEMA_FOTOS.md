# ANÁLISIS COMPLETO DEL SISTEMA DE FOTOS

## 🔍 ARQUITECTURA ACTUAL

### Flujo de trabajo actual:
1. Usuario toma foto desde WorkCameraFragment
2. Foto temporal se guarda en cache
3. EditorWhatsAppStyleFragment se abre con:
   - `rutaFoto`: Ruta temporal de la foto
   - `equipoId`: TAG del equipo
   - `tipoFoto`: Enum (EQUIPO, GPS, PROYECTO, etc.)
   - `coordenadas`, `precision`, `altitud`: Datos GPS
   - `nombreProyecto`: Nombre del proyecto (si aplica)

4. Editor carga foto en PhotoView
5. Usuario puede:
   - Rotar imagen
   - Dibujar elementos (círculos, flechas, texto, etc.)
   - Cambiar tipo de foto en spinner
   - Agregar comentarios

6. Al presionar ACEPTAR:
   - Se aplican elementos editables al bitmap
   - Se determina directorio según tipo de foto
   - Se construye nombre con: `TIPO_TIMESTAMP_GPS_COMENTARIOS.jpg`
   - Se guarda en directorio específico

### Componentes clave:
- **PhotoView**: Muestra imagen con zoom/pan/rotate
- **ElementosEditablesOverlay**: Capa transparente sobre PhotoView para dibujar
- **elementosEditables**: Lista de elementos (STAMP, LOGO, CIRCULO, FLECHA, etc.)
- **bitmapActual**: Bitmap de la foto cargada

---

## ❌ PROBLEMAS IDENTIFICADOS

### 1. **FOTOS DUPLICADAS**

#### Problema:
Fotos se guardan duplicadas en carpeta raíz de proyectos o en carpeta incorrecta.

#### Causa raíz:
```kotlin
// Línea 1266-1284
val directorio = when (tipoFotoEnum) {
    TipoFoto.FOTO_PROYECTO -> {
        val nombreProyectoFinal = nombreProyecto?.takeIf { it.isNotBlank() } ?: tipoSeleccionado
        
        // Si nombreProyecto viene vacío o "PROYECTO", se usa tipoSeleccionado
        // PERO tipoSeleccionado puede ser genérico "PROYECTO" si spinner no está bien configurado
        if (nombreProyectoFinal == "PROYECTO" || nombreProyectoFinal.isBlank()) {
            throw IllegalStateException("No se puede guardar foto de proyecto sin nombre específico")
        }
        
        File(requireContext().filesDir, "proyectos/$nombreProyectoFinal")
    }
    else -> File(requireContext().filesDir, "fotos")
}
```

**Escenarios problemáticos:**
- `nombreProyecto` viene `null` o vacío desde WorkCameraFragment
- Spinner tiene "PROYECTO" genérico en lugar de nombre específico
- Usuario cambia spinner pero `nombreProyecto` no se actualiza

#### Evidencia en logs:
```
📁 Directorio de proyecto: /data/user/0/.../files/proyectos/Pruebas  ✓ CORRECTO
🔍 VERIFICANDO DUPLICADOS EN RAÍZ:
⚠️ ARCHIVO EN RAÍZ (NO DEBERÍA ESTAR AQUÍ): proyecto_xxx.jpg  ✗ INCORRECTO
```

### 2. **PROBLEMAS DE ESCALA CON ZOOM**

#### Problema:
Círculos/elementos desaparecen o cambian de posición según zoom aplicado antes de guardar.

#### Causa raíz (YA CORREGIDA):
```kotlin
// ANTES (INCORRECTO):
val displayRect = photoView.displayRect  // ← Cambia con zoom!
val escalaX = bitmapFinal.width / displayRect.width()

// AHORA (CORRECTO):
val overlayAncho = overlayEditables.width.toFloat()  // ← Siempre fijo
val escalaX = bitmapFinal.width / overlayAncho
```

**El problema era:**
- Elementos dibujados en coordenadas de overlay (990x634)
- Al guardar, se escalaban según displayRect que varía con zoom
- Si zoom IN → displayRect más pequeño → escala incorrecta
- Si zoom OUT → displayRect más grande → escala incorrecta

**Solución aplicada:**
- Usar dimensiones fijas del overlay, no displayRect
- Escala siempre es: `bitmapFinal / overlay`

### 3. **ARQUITECTURA INADECUADA PARA LAYER-BASE**

#### Problema actual:
- PhotoView es manipulable (zoom/pan/rotate) durante TODO el proceso
- Elementos editables se dibujan en coordenadas de overlay dinámico
- No hay concepto de "layer-base fijo" con dimensiones estándar
- STAMP y LOGO usan posiciones relativas al overlay, no absolutas

#### Lo que necesitas:
```
LAYER-BASE (1:1 aspect ratio, ej: 2000x2000px)
├─ STAMP (40% ancho, esquina sup-izq, margen 30px)
├─ LOGO (40% ancho, esquina sup-der, margen 30px)
├─ FOTO DINÁMICA (movible, rotable, escalable)
│  └─ Al presionar FIN → se fusiona con layer-base
└─ ELEMENTOS EDITABLES (círculos, flechas, etc.)
   └─ Coordenadas absolutas referenciadas a layer-base
```

**Flujo propuesto:**
1. Tomar foto → Cargar en modo edición
2. Usuario puede mover/rotar/zoom la foto sobre layer-base
3. Presionar FIN → Foto se fusiona con layer-base (ya no editable)
4. Ahora se pueden agregar elementos (círculos, flechas)
5. Guardar → Todo referenciado a layer-base fijo

---

## ✅ SOLUCIONES PROPUESTAS

### SOLUCIÓN 1: Corregir duplicación de fotos

#### Problema específico:
`nombreProyecto` no se actualiza cuando usuario cambia spinner.

#### Código actual problemático:
```kotlin
// La variable nombreProyecto se inicializa al crear fragment
private var nombreProyecto: String? = null

// Pero NO se actualiza cuando cambia spinner
spinnerTipoFoto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(...) {
        // ❌ No actualiza nombreProyecto aquí
    }
}
```

#### Solución:
```kotlin
spinnerTipoFoto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val seleccionado = parent?.getItemAtPosition(position).toString()
        
        // Actualizar nombreProyecto si es un proyecto
        if (obtenerProyectosDisponibles().contains(seleccionado)) {
            nombreProyecto = seleccionado
            Log.d("EditorWhatsApp", "📝 Proyecto actualizado: $nombreProyecto")
        }
    }
}
```

### SOLUCIÓN 2: Implementar Layer-Base fijo

#### Arquitectura nueva:

```kotlin
// 1. Definir dimensiones layer-base (cuadrado)
private val LAYER_BASE_SIZE = 2000  // 2000x2000px

// 2. Crear layer-base al inicio
private lateinit var layerBase: Bitmap
private lateinit var canvasBase: Canvas

private fun inicializarLayerBase() {
    layerBase = Bitmap.createBitmap(LAYER_BASE_SIZE, LAYER_BASE_SIZE, Bitmap.Config.ARGB_8888)
    canvasBase = Canvas(layerBase)
    canvasBase.drawColor(Color.WHITE)  // Fondo blanco
    
    // Dibujar STAMP (40% ancho, esquina sup-izq, margen 30px)
    dibujarStampEnLayerBase()
    
    // Dibujar LOGO (40% ancho, esquina sup-der, margen 30px)
    dibujarLogoEnLayerBase()
}

private fun dibujarStampEnLayerBase() {
    val stampDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.fondo_logo)
    if (stampDrawable != null) {
        val anchoStamp = (LAYER_BASE_SIZE * 0.4f).toInt()
        val altoStamp = (anchoStamp * stampDrawable.intrinsicHeight / stampDrawable.intrinsicWidth)
        
        val bitmap = Bitmap.createScaledBitmap(
            (stampDrawable as BitmapDrawable).bitmap,
            anchoStamp,
            altoStamp,
            true
        )
        
        // Posición: esquina superior izquierda + margen 30px
        canvasBase.drawBitmap(bitmap, 30f, 30f, null)
        bitmap.recycle()
    }
}

private fun dibujarLogoEnLayerBase() {
    val logoDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.logo)
    if (logoDrawable != null) {
        val anchoLogo = (LAYER_BASE_SIZE * 0.4f).toInt()
        val altoLogo = (anchoLogo * logoDrawable.intrinsicHeight / logoDrawable.intrinsicWidth)
        
        val bitmap = Bitmap.createScaledBitmap(
            (logoDrawable as BitmapDrawable).bitmap,
            anchoLogo,
            altoLogo,
            true
        )
        
        // Posición: esquina superior derecha + margen 30px
        val x = LAYER_BASE_SIZE - anchoLogo - 30f
        canvasBase.drawBitmap(bitmap, x, 30f, null)
        bitmap.recycle()
    }
}

// 3. Modo edición de foto (antes de FIN)
private var fotoEditableActiva = true
private var matrizTransformacionFoto = Matrix()

private fun aplicarTransformacionFoto(dx: Float, dy: Float, escala: Float, rotacion: Float) {
    matrizTransformacionFoto.reset()
    matrizTransformacionFoto.postTranslate(dx, dy)
    matrizTransformacionFoto.postScale(escala, escala)
    matrizTransformacionFoto.postRotate(rotacion)
    
    // Redibujar en overlay
    overlayEditables.invalidate()
}

// 4. Fusionar foto con layer-base al presionar FIN
private fun fusionarFotoConLayerBase() {
    if (!fotoEditableActiva) return
    
    val fotoTransformada = Bitmap.createBitmap(
        bitmapActual!!,
        0, 0,
        bitmapActual!!.width, bitmapActual!!.height,
        matrizTransformacionFoto,
        true
    )
    
    // Dibujar foto transformada en layer-base
    canvasBase.drawBitmap(fotoTransformada, 0f, 0f, null)
    fotoTransformada.recycle()
    
    fotoEditableActiva = false
    Log.d("EditorWhatsApp", "✅ Foto fusionada con layer-base")
    
    // Ahora sí se pueden agregar elementos editables
    habilitarHerramientasEdicion()
}

// 5. Elementos editables referenciados a layer-base
private fun añadirCirculo(x: Float, y: Float) {
    // x, y ya están en coordenadas de overlay (990x634)
    // Convertir a coordenadas de layer-base (2000x2000)
    val escalaOverlayToBase = LAYER_BASE_SIZE.toFloat() / overlayEditables.width.toFloat()
    
    val xBase = x * escalaOverlayToBase
    val yBase = y * escalaOverlayToBase
    
    elementosEditables.add(
        ElementoEditable(
            tipo = TipoElemento.CIRCULO,
            x = xBase,  // Coordenadas absolutas en layer-base
            y = yBase,
            width = 100f * escalaOverlayToBase,
            height = 100f * escalaOverlayToBase,
            color = colorActual,
            seleccionado = true
        )
    )
}

// 6. Guardar: layer-base ES el bitmap final
private fun guardarFoto() {
    // layer-base ya tiene: STAMP + LOGO + FOTO + ELEMENTOS
    val bitmapFinal = layerBase.copy(Bitmap.Config.ARGB_8888, false)
    
    // Guardar como siempre
    val outputStream = FileOutputStream(archivoFinal)
    bitmapFinal.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    outputStream.close()
}
```

---

## 📋 PLAN DE IMPLEMENTACIÓN

### FASE 1: Corregir duplicación (INMEDIATO)
- [ ] Actualizar `nombreProyecto` al cambiar spinner
- [ ] Validar que `nombreProyecto` nunca sea genérico
- [ ] Agregar logs detallados en construcción de ruta

### FASE 2: Implementar layer-base (MEDIO PLAZO)
- [ ] Crear sistema de layer-base fijo (2000x2000px)
- [ ] Posicionar STAMP y LOGO en coordenadas absolutas
- [ ] Implementar modo "edición de foto" vs "foto fija"
- [ ] Botón FIN fusiona foto con layer-base
- [ ] Convertir coordenadas overlay → layer-base

### FASE 3: Refactorizar overlay (LARGO PLAZO)
- [ ] Overlay muestra layer-base escalado
- [ ] Eliminar transformaciones dinámicas de PhotoView
- [ ] Todos los elementos en coordenadas absolutas
- [ ] Testing exhaustivo de escala/zoom/rotación

---

## 🎯 PRIORIDAD INMEDIATA

**FIX 1: Duplicación de fotos**
```kotlin
// En configurarUI(), agregar listener para spinner:
spinnerTipoFoto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val seleccionado = parent?.getItemAtPosition(position).toString()
        val proyectosDisponibles = obtenerProyectosDisponibles()
        
        if (proyectosDisponibles.contains(seleccionado)) {
            nombreProyecto = seleccionado
            tipoFoto = TipoFoto.FOTO_PROYECTO
            Log.d("EditorWhatsApp", "📝 Proyecto seleccionado: $nombreProyecto")
        } else {
            // Mapear a otros tipos
            tipoFoto = when (seleccionado) {
                "EQUIPO" -> TipoFoto.FOTO_EQUIPO
                "GPS" -> TipoFoto.CAPTURA_GPS
                // etc...
                else -> TipoFoto.EXTRAS
            }
        }
    }
    
    override fun onNothingSelected(parent: AdapterView<*>?) {}
}
```

**FIX 2: Validación estricta antes de guardar**
```kotlin
// En guardarFoto(), antes de construir directorio:
if (tipoFotoEnum == TipoFoto.FOTO_PROYECTO) {
    val nombreFinal = nombreProyecto ?: spinnerTipoFoto.selectedItem.toString()
    
    if (nombreFinal.isBlank() || nombreFinal == "PROYECTO") {
        Toast.makeText(requireContext(), 
            "ERROR: Selecciona un proyecto específico", 
            Toast.LENGTH_LONG).show()
        return
    }
    
    Log.d("EditorWhatsApp", "✓ Proyecto validado: $nombreFinal")
}
```
