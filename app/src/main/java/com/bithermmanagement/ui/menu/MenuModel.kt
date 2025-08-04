package com.bithermmanagement.ui.menu

import androidx.annotation.DrawableRes

// Modelo para un hijo del menú
data class MenuChild(
    val title: String,
    @DrawableRes val iconRes: Int,
    val onClick: () -> Unit
)

// Modelo para un grupo del menú
data class MenuGroup(
    val title: String,
    @DrawableRes val iconRes: Int,
    val children: List<MenuChild>
) 