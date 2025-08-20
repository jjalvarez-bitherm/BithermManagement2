package com.bithermmanagement.ausencias.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.ausencias.models.AbsenceRecord
import com.bithermmanagement.ausencias.models.AbsenceType
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class AbsenceCalendarAdapter(
    private val onDayClick: ((CalendarDay) -> Unit)? = null
) : RecyclerView.Adapter<AbsenceCalendarAdapter.CalendarDayViewHolder>() {

    private var calendarDays: List<CalendarDay> = emptyList()
    private var absences: List<AbsenceRecord> = emptyList()
    private var holidays: Map<String, String> = emptyMap() // Fecha -> Tipo (F, FS)

    data class CalendarDay(
        val date: Date,
        val dayNumber: Int,
        val dayName: String,
        val isCurrentMonth: Boolean,
        val isToday: Boolean,
        val absences: List<AbsenceRecord>
    ) {
        // Obtener el tipo de ausencia predominante para este día
        fun getPrimaryAbsenceType(): AbsenceType? {
            return absences.firstOrNull()?.absenceType
        }
        
        // Verificar si tiene ausencias
        fun hasAbsences(): Boolean = absences.isNotEmpty()
    }



    class CalendarDayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewDayNumber: TextView = itemView.findViewById(R.id.textViewDayNumber)
        val indicatorContainer: LinearLayout = itemView.findViewById(R.id.indicatorContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarDayViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        val day = calendarDays[position]
        
        // Configurar el número del día
        holder.textViewDayNumber.text = day.dayNumber.toString()
        
        // Mostrar todos los días pero con diferentes estilos
        holder.itemView.visibility = View.VISIBLE
        
        // Configurar el estilo según el tipo de día
        when {
            !day.isCurrentMonth -> {
                // Días de otros meses: transparentes
                holder.textViewDayNumber.setTextColor(
                    holder.itemView.context.getColor(R.color.gray_400)
                )
                holder.itemView.setBackgroundResource(android.R.color.transparent)
            }
            day.isToday -> {
                holder.textViewDayNumber.setTextColor(
                    holder.itemView.context.getColor(R.color.white)
                )
                holder.itemView.setBackgroundResource(R.drawable.bg_calendar_today)
            }
            isWeekend(day.date) -> {
                holder.textViewDayNumber.setTextColor(
                    holder.itemView.context.getColor(R.color.black)
                )
                holder.itemView.setBackgroundResource(R.drawable.bg_calendar_weekend)
            }
            isHoliday(day.date) -> {
                holder.textViewDayNumber.setTextColor(
                    holder.itemView.context.getColor(R.color.black)
                )
                holder.itemView.setBackgroundResource(R.drawable.bg_calendar_holiday)
            }
            else -> {
                holder.textViewDayNumber.setTextColor(
                    holder.itemView.context.getColor(R.color.black)
                )
                holder.itemView.setBackgroundResource(R.drawable.bg_calendar_day)
            }
        }
        
        // Configurar indicadores de ausencias
        if (day.hasAbsences()) {
            Log.d("AbsenceCalendarAdapter", "=== DÍA ${day.dayNumber} - PROCESANDO AUSENCIAS ===")
            Log.d("AbsenceCalendarAdapter", "Día ${day.dayNumber} tiene ${day.absences.size} ausencias")
            
            // Log detallado de todas las ausencias del día con hash para rastrear origen
            day.absences.forEachIndexed { index, absence ->
                val hash = System.identityHashCode(absence)
                Log.d("AbsenceCalendarAdapter", "DÍA ${day.dayNumber} - Ausencia $index [Hash: $hash]: employeeId='${absence.employeeId}', employeeName='${absence.employeeName}', tipo='${absence.absenceType}', fechas='${absence.startDate}' a '${absence.endDate}'")
            }
            
            // Limpiar indicadores anteriores
            holder.indicatorContainer.removeAllViews()
            
            // Mostrar un punto por cada ausencia única
            val uniqueDayAbsences = day.absences.distinctBy { 
                "${it.employeeId}-${it.absenceType}-${it.startDate.time}-${it.endDate.time}"
            }
            
            Log.d("AbsenceCalendarAdapter", "DÍA ${day.dayNumber}: ${day.absences.size} ausencias originales, ${uniqueDayAbsences.size} únicas después de distinctBy")
            
            uniqueDayAbsences.forEachIndexed { index, absence ->
                val hash = System.identityHashCode(absence)
                Log.d(
                    "AbsenceCalendarAdapter",
                    "DÍA ${day.dayNumber} - Creando indicador $index [Hash: $hash]: ${absence.absenceType.displayName} - ${absence.employeeName} (GS ID=${absence.googleSheetsId})"
                )
                
                val indicator = View(holder.itemView.context).apply {
                    val layoutParams = LinearLayout.LayoutParams(
                        holder.itemView.context.resources.getDimensionPixelSize(R.dimen.absence_indicator_size),
                        holder.itemView.context.resources.getDimensionPixelSize(R.dimen.absence_indicator_size)
                    )
                    layoutParams.marginEnd = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.absence_indicator_margin)
                    this.layoutParams = layoutParams
                    
                    val indicatorColor = when (absence.absenceType) {
                        AbsenceType.VACACIONES -> holder.itemView.context.getColor(R.color.absence_vacaciones)
                        AbsenceType.PERMISO -> holder.itemView.context.getColor(R.color.absence_permiso)
                        AbsenceType.ENFERMEDAD -> holder.itemView.context.getColor(R.color.absence_enfermedad)
                        else -> holder.itemView.context.getColor(R.color.absence_otros)
                    }
                    
                    // Crear un drawable circular con el color específico
                    val drawable = holder.itemView.context.getDrawable(R.drawable.bg_absence_indicator)?.mutate()
                    drawable?.setTint(indicatorColor)
                    background = drawable
                }
                
                holder.indicatorContainer.addView(indicator)
                Log.d("AbsenceCalendarAdapter", "DÍA ${day.dayNumber} - Indicador $index agregado al contenedor [Hash: $hash]")
            }
            
            holder.indicatorContainer.visibility = View.VISIBLE
            Log.d("AbsenceCalendarAdapter", "DÍA ${day.dayNumber} - Indicadores mostrados: ${uniqueDayAbsences.size} puntos")
            Log.d("AbsenceCalendarAdapter", "=== FIN DÍA ${day.dayNumber} ===")
        } else {
            holder.indicatorContainer.visibility = View.GONE
        }
        
        // Configurar click listener
        holder.itemView.setOnClickListener {
            onDayClick?.invoke(day)
        }
    }

    override fun getItemCount(): Int = calendarDays.size

    fun updateData(newCalendarDays: List<CalendarDay>, newAbsences: List<AbsenceRecord>) {
        Log.d("AbsenceCalendarAdapter", "=== INICIANDO UPDATE DATA ===")
        Log.d("AbsenceCalendarAdapter", "updateData: ${newCalendarDays.size} días, ${newAbsences.size} ausencias")
        
        // Log detallado de todas las ausencias recibidas
        newAbsences.forEachIndexed { index, absence ->
            val hash = System.identityHashCode(absence)
            Log.d("AbsenceCalendarAdapter", "AUSENCIA $index [Hash: $hash]: employeeId='${absence.employeeId}', employeeName='${absence.employeeName}', tipo='${absence.absenceType}', fechas='${absence.startDate}' a '${absence.endDate}'")
        }
        
        absences = newAbsences
        
        // Actualizar las ausencias en cada día del calendario
        calendarDays = newCalendarDays.map { day ->
            val calendar = Calendar.getInstance()
            calendar.time = day.date
            
            // Filtrar ausencias para este día
            val dayAbsences = absences.filter { absence ->
                val absenceCalendar = Calendar.getInstance()
                absenceCalendar.time = absence.startDate
                val endCalendar = Calendar.getInstance()
                endCalendar.time = absence.endDate
                
                val isInRange = calendar.get(Calendar.YEAR) == absenceCalendar.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == absenceCalendar.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) >= absenceCalendar.get(Calendar.DAY_OF_MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) <= endCalendar.get(Calendar.DAY_OF_MONTH)
                
                // NUEVA LÓGICA: Excluir fines de semana y festivos EXCEPTO para "Baja por Enfermedad"
                val isWeekendOrHoliday = isWeekend(day.date) || isHoliday(day.date)
                val isSickLeave = absence.absenceType == AbsenceType.ENFERMEDAD
                
                // Solo mostrar ausencias si:
                // 1. Está en el rango de fechas Y
                // 2. (No es fin de semana/festivo O es baja por enfermedad)
                val shouldShow = isInRange && (!isWeekendOrHoliday || isSickLeave)
                
                if (shouldShow) {
                    val hash = System.identityHashCode(absence)
                    Log.d("AbsenceCalendarAdapter", "DÍA ${day.dayNumber}: Ausencia asignada [Hash: $hash]: ${absence.employeeId} - ${absence.absenceType.displayName}")
                } else if (isInRange && isWeekendOrHoliday && !isSickLeave) {
                    Log.d("AbsenceCalendarAdapter", "DÍA ${day.dayNumber}: Ausencia EXCLUIDA por ser fin de semana/festivo: ${absence.employeeId} - ${absence.absenceType.displayName}")
                }
                
                shouldShow
            }
            
            if (dayAbsences.isNotEmpty()) {
                Log.d("AbsenceCalendarAdapter", "DÍA ${day.dayNumber}: ${dayAbsences.size} ausencias asignadas")
            }
            
            day.copy(absences = dayAbsences)
        }
        
        Log.d("AbsenceCalendarAdapter", "=== FIN UPDATE DATA ===")
        notifyDataSetChanged()
    }
    
    fun updateHolidays(newHolidays: Map<String, String>) {
        Log.d("AbsenceCalendarAdapter", "updateHolidays: ${newHolidays.size} festivos")
        holidays = newHolidays
        notifyDataSetChanged()
    }

    fun generateCalendarDays(year: Int, month: Int): List<CalendarDay> {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        
        // Configurar para que la semana comience en lunes
        calendar.firstDayOfWeek = Calendar.MONDAY
        
        calendar.set(year, month, 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val days = mutableListOf<CalendarDay>()
        
        // Calcular cuántos días del mes anterior necesitamos para completar la primera semana
        val daysFromPreviousMonth = when (firstDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        
        // Agregar días del mes anterior si es necesario
        if (daysFromPreviousMonth > 0) {
            calendar.add(Calendar.DAY_OF_MONTH, -daysFromPreviousMonth)
            for (i in 0 until daysFromPreviousMonth) {
                days.add(createCalendarDay(calendar.time, false, today))
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        // Agregar días del mes actual
        for (i in 1..daysInMonth) {
            days.add(createCalendarDay(calendar.time, true, today))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // Calcular cuántos días del mes siguiente necesitamos para completar la última semana
        val totalDays = days.size
        val weeks = (totalDays + 6) / 7 // Redondear hacia arriba
        val targetDays = weeks * 7
        val remainingDays = targetDays - totalDays
        
        // Agregar días del mes siguiente si es necesario
        for (i in 0 until remainingDays) {
            days.add(createCalendarDay(calendar.time, false, today))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return days
    }
    
    private fun createCalendarDay(date: Date, isCurrentMonth: Boolean, today: Calendar): CalendarDay {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        val dayNumber = calendar.get(Calendar.DAY_OF_MONTH)
        val dayName = getDayName(calendar.get(Calendar.DAY_OF_WEEK))
        val isToday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
        
        // Filtrar ausencias para este día
        val dayAbsences = absences.filter { absence ->
            val absenceCalendar = Calendar.getInstance()
            absenceCalendar.time = absence.startDate
            val endCalendar = Calendar.getInstance()
            endCalendar.time = absence.endDate
            
            val isInRange = calendar.get(Calendar.YEAR) == absenceCalendar.get(Calendar.YEAR) &&
            calendar.get(Calendar.MONTH) == absenceCalendar.get(Calendar.MONTH) &&
            calendar.get(Calendar.DAY_OF_MONTH) >= absenceCalendar.get(Calendar.DAY_OF_MONTH) &&
            calendar.get(Calendar.DAY_OF_MONTH) <= endCalendar.get(Calendar.DAY_OF_MONTH)
            
            if (isInRange) {
                Log.d("AbsenceCalendarAdapter", "Día $dayNumber coincide con ausencia: ${absence.employeeName} - ${absence.absenceType.displayName}")
            }
            
            isInRange
        }
        
        if (dayAbsences.isNotEmpty()) {
            Log.d("AbsenceCalendarAdapter", "Día $dayNumber tiene ${dayAbsences.size} ausencias")
        }
        
        return CalendarDay(date, dayNumber, dayName, isCurrentMonth, isToday, dayAbsences)
    }
    
    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "D"
            Calendar.MONDAY -> "L"
            Calendar.TUESDAY -> "M"
            Calendar.WEDNESDAY -> "X"
            Calendar.THURSDAY -> "J"
            Calendar.FRIDAY -> "V"
            Calendar.SATURDAY -> "S"
            else -> ""
        }
    }
    
    private fun isWeekend(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }
    
    private fun isHoliday(date: Date): Boolean {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateString = dateFormat.format(date)
        return holidays.containsKey(dateString)
    }
    
    private fun getHolidayType(date: Date): String? {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateString = dateFormat.format(date)
        return holidays[dateString]
    }
}
