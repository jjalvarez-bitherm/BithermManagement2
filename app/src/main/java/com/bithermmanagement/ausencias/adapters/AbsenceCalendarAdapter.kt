package com.bithermmanagement.ausencias.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.ausencias.databinding.ItemCalendarDayBinding
import com.bithermmanagement.ausencias.models.AbsenceRecord
import com.bithermmanagement.ausencias.models.AbsenceType
import java.text.SimpleDateFormat
import java.util.*

class AbsenceCalendarAdapter(
    private val onDateSelected: (Date) -> Unit
) : RecyclerView.Adapter<AbsenceCalendarAdapter.CalendarDayViewHolder>() {
    
    private var absences: List<AbsenceRecord> = emptyList()
    private var currentMonth: Calendar = Calendar.getInstance()
    
    private val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    
    fun updateAbsences(newAbsences: List<AbsenceRecord>) {
        absences = newAbsences
        notifyDataSetChanged()
    }
    
    fun setCurrentMonth(month: Calendar) {
        currentMonth = month
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CalendarDayViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        holder.bind(getDayForPosition(position))
    }
    
    override fun getItemCount(): Int {
        // Mostrar 42 días (6 semanas x 7 días) para cubrir todo el mes
        return 42
    }
    
    private fun getDayForPosition(position: Int): CalendarDay {
        val calendar = currentMonth.clone() as Calendar
        
        // Ajustar al primer día del mes
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        
        // Ajustar al primer día de la semana (Lunes = 2, Domingo = 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
        
        // Calcular el día para esta posición
        calendar.add(Calendar.DAY_OF_MONTH, position - offset)
        
        val date = calendar.time
        val isCurrentMonth = calendar.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)
        val isToday = isToday(date)
        val dayAbsences = getAbsencesForDate(date)
        
        return CalendarDay(
            date = date,
            dayNumber = calendar.get(Calendar.DAY_OF_MONTH),
            dayName = dayFormat.format(date),
            isCurrentMonth = isCurrentMonth,
            isToday = isToday,
            absences = dayAbsences
        )
    }
    
    private fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val checkDate = Calendar.getInstance()
        checkDate.time = date
        
        return today.get(Calendar.YEAR) == checkDate.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == checkDate.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == checkDate.get(Calendar.DAY_OF_MONTH)
    }
    
    private fun getAbsencesForDate(date: Date): List<AbsenceRecord> {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val checkYear = calendar.get(Calendar.YEAR)
        val checkMonth = calendar.get(Calendar.MONTH)
        val checkDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        return absences.filter { absence ->
            val startCalendar = Calendar.getInstance()
            startCalendar.time = absence.startDate
            val endCalendar = Calendar.getInstance()
            endCalendar.time = absence.endDate
            
            val startYear = startCalendar.get(Calendar.YEAR)
            val startMonth = startCalendar.get(Calendar.MONTH)
            val startDay = startCalendar.get(Calendar.DAY_OF_MONTH)
            
            val endYear = endCalendar.get(Calendar.YEAR)
            val endMonth = endCalendar.get(Calendar.MONTH)
            val endDay = endCalendar.get(Calendar.DAY_OF_MONTH)
            
            // Verificar si la fecha está dentro del rango de la ausencia
            val checkDate = Calendar.getInstance()
            checkDate.set(checkYear, checkMonth, checkDay)
            
            val startDate = Calendar.getInstance()
            startDate.set(startYear, startMonth, startDay)
            
            val endDate = Calendar.getInstance()
            endDate.set(endYear, endMonth, endDay)
            
            !checkDate.before(startDate) && !checkDate.after(endDate)
        }
    }
    
    inner class CalendarDayViewHolder(
        private val binding: ItemCalendarDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(day: CalendarDay) {
            binding.apply {
                // Configurar el número del día
                textViewDayNumber.text = day.dayNumber.toString()
                
                // Configurar el nombre del día (solo para la primera fila)
                if (bindingAdapterPosition < 7) {
                    textViewDayName.text = day.dayName
                    textViewDayName.visibility = android.view.View.VISIBLE
                } else {
                    textViewDayName.visibility = android.view.View.GONE
                }
                
                // Configurar el color del texto según el mes
                val textColor = if (day.isCurrentMonth) {
                    if (day.isToday) android.R.color.holo_blue_dark
                    else android.R.color.black
                } else {
                    android.R.color.darker_gray
                }
                
                textViewDayNumber.setTextColor(
                    itemView.context.getColor(textColor)
                )
                
                // Configurar el fondo si es hoy
                if (day.isToday) {
                    root.setBackgroundResource(android.R.color.holo_blue_light)
                } else {
                    root.background = null
                }
                
                // Configurar las ausencias del día
                setupAbsences(day.absences)
                
                // Configurar el click listener
                root.setOnClickListener {
                    onDateSelected(day.date)
                }
            }
        }
        
        private fun setupAbsences(absences: List<AbsenceRecord>) {
            binding.apply {
                // Limpiar indicadores anteriores
                indicatorContainer.removeAllViews()
                
                // Mostrar solo las primeras 3 ausencias para no saturar la UI
                val absencesToShow = absences.take(3)
                
                absencesToShow.forEach { absence ->
                    val indicator = createAbsenceIndicator(absence.absenceType)
                    indicatorContainer.addView(indicator)
                }
                
                // Si hay más de 3 ausencias, mostrar contador
                if (absences.size > 3) {
                    val moreIndicator = createMoreIndicator(absences.size - 3)
                    indicatorContainer.addView(moreIndicator)
                }
                
                // Mostrar/ocultar el contenedor de indicadores
                indicatorContainer.visibility = if (absences.isNotEmpty()) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }
        
        private fun createAbsenceIndicator(type: AbsenceType): android.view.View {
            val indicator = android.widget.ImageView(itemView.context)
            val size = (8 * itemView.context.resources.displayMetrics.density).toInt()
            
            val layoutParams = ViewGroup.LayoutParams(size, size)
            indicator.layoutParams = layoutParams
            
            // Configurar el color según el tipo de ausencia
            val color = android.graphics.Color.parseColor(type.color)
            indicator.setColorFilter(color)
            
            // Configurar el icono según el tipo
            when (type) {
                AbsenceType.VACACIONES -> indicator.setImageResource(android.R.drawable.ic_menu_myplaces)
                AbsenceType.PERMISO -> indicator.setImageResource(android.R.drawable.ic_menu_agenda)
                AbsenceType.ENFERMEDAD -> indicator.setImageResource(android.R.drawable.ic_menu_help)
                AbsenceType.ASUNTOS_PERSONALES -> indicator.setImageResource(android.R.drawable.ic_menu_myplaces)
                AbsenceType.FORMACION -> indicator.setImageResource(android.R.drawable.ic_menu_edit)
                AbsenceType.OTROS -> indicator.setImageResource(android.R.drawable.ic_menu_more)
            }
            
            return indicator
        }
        
        private fun createMoreIndicator(count: Int): android.view.View {
            val indicator = android.widget.TextView(itemView.context)
            val size = (16 * itemView.context.resources.displayMetrics.density).toInt()
            
            val layoutParams = ViewGroup.LayoutParams(size, size)
            indicator.layoutParams = layoutParams
            
            indicator.text = "+$count"
            indicator.textSize = 8f
            indicator.setTextColor(android.graphics.Color.GRAY)
            indicator.gravity = android.view.Gravity.CENTER
            
            return indicator
        }
    }
    
    data class CalendarDay(
        val date: Date,
        val dayNumber: Int,
        val dayName: String,
        val isCurrentMonth: Boolean,
        val isToday: Boolean,
        val absences: List<AbsenceRecord>
    )
}
