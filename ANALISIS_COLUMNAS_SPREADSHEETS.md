# ANÁLISIS DE COLUMNAS - SPREADSHEETS BITHERMMANAGEMENT

## COLUMNAS ACTUALMENTE MAPEADAS

### ColumnMapping.kt (Actual)
```kotlin
const val COD = "COD"
const val NOMBRE = "NOMBRE"
const val APELLIDOS = "APELLIDOS"
const val DNI = "DNI"
const val FECHA_NAC = "FECHA_NAC"
const val APP = "APP"
const val PASS = "PASS"
const val ROL = "ROL"
const val SW_WEB = "SW WEB"
const val EQUIPO_ASIGN = "EQUIPO_ASIGN"
const val FECHA_CAL = "FECHA_CAL"
const val TELEF_EMPR = "TELEF_EMPR"
const val EMAIL_EMPR = "EMAIL_EMPR"
const val ALTA_EMPR = "ALTA_EMPR"
const val TELEF_PERSONAL = "TELEF_PERSONAL"
const val EMAIL_PERSONAL = "EMAIL_PERSONAL"
const val CATEGORIA = "CATEGORIA"
const val R_MEDICO = "R.MEDICO"
const val ACCESO_RLR = "ACCESO RLR"
const val SUP_EJEC = "SUP.EJEC"
```

### UserEntity.kt (Campos actuales)
```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val cod: String,
    val nombre: String,
    val apellidos: String,
    val dni: String,
    val fechaNacimiento: Date?,
    val app: String,
    val password: String,
    val rol: String,
    val swWeb: Boolean,
    val equipoAsignado: String?,
    val fechaCalibracion: Date?,
    val telefonoEmpresa: String?,
    val emailEmpresa: String?,
    val altaEmpresa: Date?,
    val telefonoPersonal: String?,
    val emailPersonal: String?,
    val categoria: String?,
    val rMedico: Boolean,
    val accesoRLR: Boolean,
    val supEjec: Boolean
)
```

## NUEVAS COLUMNAS IDENTIFICADAS (DE SESIONES ANTERIORES)

### Columnas de Información Personal
- [ ] **APODO** - Apodo del usuario
- [ ] **DIRECCION** - Dirección personal
- [ ] **CIUDAD** - Ciudad de residencia
- [ ] **PROVINCIA** - Provincia
- [ ] **CODIGO_POSTAL** - Código postal
- [ ] **PAIS** - País de residencia

### Columnas de Información Laboral
- [ ] **DEPARTAMENTO** - Departamento de trabajo
- [ ] **PUESTO** - Puesto de trabajo
- [ ] **JEFE_DIRECTO** - Jefe directo
- [ ] **FECHA_CONTRATO** - Fecha de inicio de contrato
- [ ] **TIPO_CONTRATO** - Tipo de contrato laboral
- [ ] **SALARIO** - Salario (si aplica)
- [ ] **HORARIO** - Horario de trabajo

### Columnas de Seguridad y Acceso
- [ ] **PERMISOS_ESPECIALES** - Permisos especiales
- [ ] **ACCESO_BIOMETRICO** - Acceso biométrico
- [ ] **ULTIMO_ACCESO** - Último acceso al sistema
- [ ] **INTENTOS_LOGIN** - Intentos de login fallidos
- [ ] **CUENTA_BLOQUEADA** - Estado de bloqueo de cuenta

### Columnas de Equipamiento
- [ ] **EQUIPO_PRINCIPAL** - Equipo principal asignado
- [ ] **EQUIPOS_SECUNDARIOS** - Equipos secundarios
- [ ] **FECHA_ASIGNACION_EQUIPO** - Fecha de asignación
- [ ] **ESTADO_EQUIPO** - Estado del equipo asignado

### Columnas de Formación y Certificaciones
- [ ] **CERTIFICACIONES** - Certificaciones obtenidas
- [ ] **FECHA_CERTIFICACION** - Fecha de certificación
- [ ] **CURSO_ACTUAL** - Curso en el que está inscrito
- [ ] **NIVEL_FORMACION** - Nivel de formación

### Columnas de Salud y Seguridad
- [ ] **GRUPO_SANGUINEO** - Grupo sanguíneo
- [ ] **ALERGIAS** - Alergias conocidas
- [ ] **CONTACTO_EMERGENCIA** - Contacto de emergencia
- [ ] **RELACION_EMERGENCIA** - Relación con contacto de emergencia
- [ ] **TELEFONO_EMERGENCIA** - Teléfono de emergencia

### Columnas de Configuración de App
- [ ] **TEMA_APP** - Tema preferido de la aplicación
- [ ] **IDIOMA_APP** - Idioma preferido
- [ ] **NOTIFICACIONES** - Configuración de notificaciones
- [ ] **FAVORITOS** - Elementos favoritos del usuario

## COLUMNAS SIN MAPEAR (PENDIENTES DE CONFIRMACIÓN)

### Columnas que aparecieron en sesiones anteriores pero necesitan verificación:
- [ ] **FOTO_PERFIL** - URL o ruta de foto de perfil
- [ ] **FIRMA_DIGITAL** - Firma digital del usuario
- [ ] **DOCUMENTOS_ADJUNTOS** - Documentos adjuntos
- [ ] **HISTORIAL_CAMBIOS** - Historial de cambios en el perfil
- [ ] **VERSION_APP** - Versión de la app que usa
- [ ] **DISPOSITIVO_REGISTRADO** - Dispositivo registrado
- [ ] **IP_ULTIMO_ACCESO** - IP del último acceso
- [ ] **UBICACION_ULTIMO_ACCESO** - Ubicación del último acceso

## COLUMNAS DE SISTEMA (INTERNAS)
- [ ] **FECHA_CREACION** - Fecha de creación del registro
- [ ] **FECHA_MODIFICACION** - Fecha de última modificación
- [ ] **USUARIO_CREACION** - Usuario que creó el registro
- [ ] **USUARIO_MODIFICACION** - Usuario que modificó por última vez
- [ ] **VERSION_REGISTRO** - Versión del registro para control de cambios

## PRÓXIMOS PASOS

1. **Verificar columnas reales** en el spreadsheet actual
2. **Confirmar nombres exactos** de las cabeceras
3. **Actualizar ColumnMapping.kt** con las nuevas columnas
4. **Expandir UserEntity.kt** con los nuevos campos
5. **Actualizar UserDao.kt** con nuevos métodos si es necesario
6. **Modificar GoogleSheetsManager.kt** para mapear las nuevas columnas
7. **Probar sincronización** con la nueva estructura

## NOTAS IMPORTANTES

- Todas las columnas se localizarán por cabecera, no por posición
- El orden de las columnas no importa
- Se debe manejar columnas opcionales (que pueden no existir)
- Se debe validar tipos de datos (fechas, booleanos, etc.)
- Se debe manejar valores nulos o vacíos correctamente 