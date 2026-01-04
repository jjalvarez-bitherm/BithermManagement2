package com.bithermmanagement.ui.profile

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.models.UserProfile
import com.bithermmanagement.models.VisibilitySettings
import com.bithermmanagement.ui.profile.adapters.InterestsAdapter
import com.bithermmanagement.ui.profile.adapters.HobbiesAdapter
import com.bithermmanagement.ui.profile.adapters.LanguagesAdapter

class ProfileActivity : AppCompatActivity() {
    
    private lateinit var etNombre: EditText
    private lateinit var etEdad: EditText
    private lateinit var spSexo: Spinner
    private lateinit var etBio: EditText
    private lateinit var etOcupacion: EditText
    private lateinit var etEstudios: EditText
    private lateinit var etAltura: EditText
    private lateinit var spSignoZodiacal: Spinner
    private lateinit var spQueBusca: Spinner
    
    // Switches de visibilidad
    private lateinit var swNombreVisible: Switch
    private lateinit var swEdadVisible: Switch
    private lateinit var swSexoVisible: Switch
    private lateinit var swFotosVisible: Switch
    private lateinit var swBioVisible: Switch
    private lateinit var swOcupacionVisible: Switch
    private lateinit var swEstudiosVisible: Switch
    private lateinit var swHobbiesVisible: Switch
    private lateinit var swIdiomasVisible: Switch
    private lateinit var swAlturaVisible: Switch
    private lateinit var swSignoZodiacalVisible: Switch
    private lateinit var swInteresesVisible: Switch
    private lateinit var swQueBuscaVisible: Switch
    
    // RecyclerViews para listas
    private lateinit var rvIntereses: RecyclerView
    private lateinit var rvHobbies: RecyclerView
    private lateinit var rvIdiomas: RecyclerView
    
    // Adapters
    private lateinit var interesesAdapter: InterestsAdapter
    private lateinit var hobbiesAdapter: HobbiesAdapter
    private lateinit var idiomasAdapter: LanguagesAdapter
    
    // Datos
    private var userProfile = UserProfile()
    private var visibilitySettings = VisibilitySettings()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        setupUI()
        setupAdapters()
        loadUserData()
    }
    
    private fun setupUI() {
        // Campos de texto
        etNombre = findViewById(R.id.et_nombre)
        etEdad = findViewById(R.id.et_edad)
        spSexo = findViewById(R.id.sp_sexo)
        etBio = findViewById(R.id.et_bio)
        etOcupacion = findViewById(R.id.et_ocupacion)
        etEstudios = findViewById(R.id.et_estudios)
        etAltura = findViewById(R.id.et_altura)
        spSignoZodiacal = findViewById(R.id.sp_signo_zodiacal)
        spQueBusca = findViewById(R.id.sp_que_busca)
        
        // Switches de visibilidad
        swNombreVisible = findViewById(R.id.sw_nombre_visible)
        swEdadVisible = findViewById(R.id.sw_edad_visible)
        swSexoVisible = findViewById(R.id.sw_sexo_visible)
        swFotosVisible = findViewById(R.id.sw_fotos_visible)
        swBioVisible = findViewById(R.id.sw_bio_visible)
        swOcupacionVisible = findViewById(R.id.sw_ocupacion_visible)
        swEstudiosVisible = findViewById(R.id.sw_estudios_visible)
        swHobbiesVisible = findViewById(R.id.sw_hobbies_visible)
        swIdiomasVisible = findViewById(R.id.sw_idiomas_visible)
        swAlturaVisible = findViewById(R.id.sw_altura_visible)
        swSignoZodiacalVisible = findViewById(R.id.sw_signo_zodiacal_visible)
        swInteresesVisible = findViewById(R.id.sw_intereses_visible)
        swQueBuscaVisible = findViewById(R.id.sw_que_busca_visible)
        
        // RecyclerViews
        rvIntereses = findViewById(R.id.rv_intereses)
        rvHobbies = findViewById(R.id.rv_hobbies)
        rvIdiomas = findViewById(R.id.rv_idiomas)
        
        // Configurar spinners
        setupSpinners()
        
        // Configurar switches obligatorios
        setupObligatorySwitches()
        
        // Botones
        findViewById<Button>(R.id.btn_guardar).setOnClickListener { saveProfile() }
        findViewById<Button>(R.id.btn_cancelar).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_cancelar_bottom).setOnClickListener { finish() }
    }
    
    private fun setupSpinners() {
        // Spinner de sexo
        val sexoOptions = arrayOf("Seleccionar", "Masculino", "Femenino", "Otro")
        val sexoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sexoOptions)
        sexoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSexo.adapter = sexoAdapter
        
        // Spinner de signo zodiacal
        val signoOptions = arrayOf("Seleccionar", "Aries", "Tauro", "Géminis", "Cáncer", "Leo", "Virgo", 
                                  "Libra", "Escorpio", "Sagitario", "Capricornio", "Acuario", "Piscis")
        val signoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, signoOptions)
        signoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSignoZodiacal.adapter = signoAdapter
        
        // Spinner de qué busca
        val queBuscaOptions = arrayOf("Seleccionar", "Amistad", "Relación seria", "Cita casual", "Networking")
        val queBuscaAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, queBuscaOptions)
        queBuscaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spQueBusca.adapter = queBuscaAdapter
    }
    
    private fun setupObligatorySwitches() {
        // Campos obligatorios siempre visibles en estancias
        swEdadVisible.isChecked = true
        swEdadVisible.isEnabled = false
        swSexoVisible.isChecked = true
        swSexoVisible.isEnabled = false
        swInteresesVisible.isChecked = true
        swInteresesVisible.isEnabled = false
        swQueBuscaVisible.isChecked = true
        swQueBuscaVisible.isEnabled = false
    }
    
    private fun setupAdapters() {
        // Adapter de intereses
        interesesAdapter = InterestsAdapter { interest ->
            // Lógica para agregar/quitar interés
        }
        rvIntereses.layoutManager = LinearLayoutManager(this)
        rvIntereses.adapter = interesesAdapter
        
        // Adapter de hobbies
        hobbiesAdapter = HobbiesAdapter { hobby ->
            // Lógica para agregar/quitar hobby
        }
        rvHobbies.layoutManager = LinearLayoutManager(this)
        rvHobbies.adapter = hobbiesAdapter
        
        // Adapter de idiomas
        idiomasAdapter = LanguagesAdapter { language ->
            // Lógica para agregar/quitar idioma
        }
        rvIdiomas.layoutManager = LinearLayoutManager(this)
        rvIdiomas.adapter = idiomasAdapter
    }
    
    private fun loadUserData() {
        // Cargar datos del usuario desde SharedPreferences o base de datos
        // Por ahora, cargar datos por defecto
        etNombre.setText(userProfile.nombre)
        etEdad.setText(userProfile.edad.toString())
        etBio.setText(userProfile.bio)
        etOcupacion.setText(userProfile.ocupacion)
        etEstudios.setText(userProfile.estudios)
        etAltura.setText(userProfile.altura.toString())
        
        // Cargar switches de visibilidad
        swNombreVisible.isChecked = visibilitySettings.nombreVisible
        swEdadVisible.isChecked = visibilitySettings.edadVisible
        swSexoVisible.isChecked = visibilitySettings.sexoVisible
        swFotosVisible.isChecked = visibilitySettings.fotosVisible
        swBioVisible.isChecked = visibilitySettings.bioVisible
        swOcupacionVisible.isChecked = visibilitySettings.ocupacionVisible
        swEstudiosVisible.isChecked = visibilitySettings.estudiosVisible
        swHobbiesVisible.isChecked = visibilitySettings.hobbiesVisible
        swIdiomasVisible.isChecked = visibilitySettings.idiomasVisible
        swAlturaVisible.isChecked = visibilitySettings.alturaVisible
        swSignoZodiacalVisible.isChecked = visibilitySettings.signoZodiacalVisible
        swInteresesVisible.isChecked = visibilitySettings.interesesVisible
        swQueBuscaVisible.isChecked = visibilitySettings.queBuscaVisible
    }
    
    private fun saveProfile() {
        if (validateFields()) {
            // Guardar datos del usuario
            userProfile = userProfile.copy(
                nombre = etNombre.text.toString(),
                edad = etEdad.text.toString().toIntOrNull() ?: 0,
                bio = etBio.text.toString(),
                ocupacion = etOcupacion.text.toString(),
                estudios = etEstudios.text.toString(),
                altura = etAltura.text.toString().toIntOrNull() ?: 0
            )
            
            // Guardar configuración de visibilidad
            visibilitySettings = VisibilitySettings(
                nombreVisible = swNombreVisible.isChecked,
                edadVisible = swEdadVisible.isChecked,
                sexoVisible = swSexoVisible.isChecked,
                fotosVisible = swFotosVisible.isChecked,
                bioVisible = swBioVisible.isChecked,
                ocupacionVisible = swOcupacionVisible.isChecked,
                estudiosVisible = swEstudiosVisible.isChecked,
                hobbiesVisible = swHobbiesVisible.isChecked,
                idiomasVisible = swIdiomasVisible.isChecked,
                alturaVisible = swAlturaVisible.isChecked,
                signoZodiacalVisible = swSignoZodiacalVisible.isChecked,
                interesesVisible = swInteresesVisible.isChecked,
                queBuscaVisible = swQueBuscaVisible.isChecked
            )
            
            // TODO: Guardar en SharedPreferences o base de datos
            Toast.makeText(this, "Perfil guardado correctamente", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun validateFields(): Boolean {
        var isValid = true
        
        // Validar campos obligatorios
        if (etNombre.text.toString().isEmpty()) {
            etNombre.error = "El nombre es obligatorio"
            isValid = false
        }
        
        if (etEdad.text.toString().isEmpty() || etEdad.text.toString().toIntOrNull() == null) {
            etEdad.error = "La edad es obligatoria"
            isValid = false
        }
        
        if (spSexo.selectedItemPosition == 0) {
            Toast.makeText(this, "Debes seleccionar un sexo", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        if (spQueBusca.selectedItemPosition == 0) {
            Toast.makeText(this, "Debes seleccionar qué buscas", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        return isValid
    }
}
