package com.bithermmanagement.core.location

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bithermmanagement.core.R
import com.bithermmanagement.core.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationSettingsFragment : Fragment() {
    
    @Inject
    lateinit var locationManager: LocationManager
    
    private lateinit var switchLocationTracking: Switch
    private lateinit var seekBarInterval: SeekBar
    private lateinit var textInterval: TextView
    private lateinit var buttonApply: Button
    
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            enableLocationTracking()
        } else {
            Toast.makeText(context, "Se requieren permisos de ubicación", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupListeners()
        loadCurrentSettings()
    }
    
    private fun initViews(view: View) {
        switchLocationTracking = view.findViewById(R.id.switch_location_tracking)
        seekBarInterval = view.findViewById(R.id.seekbar_interval)
        textInterval = view.findViewById(R.id.text_interval)
        buttonApply = view.findViewById(R.id.button_apply)
    }
    
    private fun setupListeners() {
        switchLocationTracking.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkLocationPermissions()
            } else {
                locationManager.setLocationTrackingEnabled(false)
            }
        }
        
        seekBarInterval.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minutes = progress + 1 // Mínimo 1 minuto
                textInterval.text = "$minutes minutos"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        buttonApply.setOnClickListener {
            applySettings()
        }
    }
    
    private fun loadCurrentSettings() {
        // Cargar configuración actual
        switchLocationTracking.isChecked = locationManager.isLocationTrackingEnabled()
        
        val currentInterval = locationManager.getTrackingInterval()
        seekBarInterval.progress = (currentInterval - 1).toInt() // Ajustar al rango del SeekBar
        textInterval.text = "${currentInterval} minutos"
    }
    
    private fun checkLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (allGranted) {
            enableLocationTracking()
        } else {
            locationPermissionLauncher.launch(permissions)
        }
    }
    
    private fun enableLocationTracking() {
        locationManager.setLocationTrackingEnabled(true)
        Toast.makeText(context, "Servicio de ubicación activado", Toast.LENGTH_SHORT).show()
    }
    
    private fun applySettings() {
        val interval = (seekBarInterval.progress + 1).toLong()
        locationManager.setTrackingInterval(interval)
        
        if (switchLocationTracking.isChecked) {
            locationManager.restartLocationTracking()
        }
        
        Toast.makeText(context, "Configuración aplicada", Toast.LENGTH_SHORT).show()
    }
} 