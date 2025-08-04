package com.bithermmanagement.fichaje.models

data class OTSelectionItem(
    val id: String,
    val name: String,
    var isSelected: Boolean = false,
    val isSpecial: Boolean = false, // VACACIONES, BAJAMED
    val isAdministrative: Boolean = false, // PERM.RET, DELEGACION
    val isCommon: Boolean = false // OTs comunes de trabajo
) 