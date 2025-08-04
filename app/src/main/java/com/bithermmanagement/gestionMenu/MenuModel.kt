package com.bithermmanagement.gestionMenu

import androidx.annotation.DrawableRes

/**
 * Modelo para un hijo del menú expandible.
 * @param title Título del hijo.
 * @param iconRes Recurso del icono.
 * @param onClick Acción a ejecutar al pulsar el hijo.
 */
data class MenuChild(
    val title: String,
    @DrawableRes val iconRes: Int,
    val onClick: () -> Unit
)

/**
 * Modelo para un grupo del menú expandible.
 * @param title Título del grupo.
 * @param iconRes Recurso del icono.
 * @param children Lista de hijos de este grupo.
 */
data class MenuGroup(
    val title: String,
    @DrawableRes val iconRes: Int,
    val children: List<MenuChild>
) 