package com.bithermmanagement.data

import android.content.Context
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.database.entities.MenuEntity
import com.bithermmanagement.database.entities.SubMenuEntity
import com.bithermmanagement.database.entities.FavoritoEntity
import com.bithermmanagement.database.entities.PermisoEntity

object MenuRepository {
    suspend fun syncMenusForUser(context: Context, usuarioLogin: String, rolPoundUsuario: Int, googleSheetsManager: GoogleSheetsManager) {
        // 1. Descargar estructura desde Google Sheets
        val menuStructure = googleSheetsManager.getMenuStructureForUser(rolPoundUsuario, usuarioLogin)
        if (menuStructure != null) {
            val db = AppDatabase.getDatabase(context)
            // Preparar los datos a insertar
            val menus = menuStructure.mainMenus.map {
                MenuEntity(
                    fragment = it.fragment,
                    nombreVisible = it.nombreVisible,
                    peso = it.index,
                    colorHex = it.color // ahora es HEX
                )
            }
            val subMenus = menuStructure.subMenus.map {
                SubMenuEntity(
                    menuPrincipal = it.menuPrincipal,
                    item = it.item,
                    rol = it.rol,
                    colorHex = it.colorCard,
                    nombreVisible = it.nombreVisible
                )
            }
            val favoritos = menuStructure.favoritos.map {
                FavoritoEntity(
                    usuario = usuarioLogin,
                    item = it.item
                )
            }
            val permisos = (menuStructure.mainMenus.map {
                PermisoEntity(usuario = usuarioLogin, fragment = it.fragment, permiso = true)
            } + menuStructure.subMenus.map {
                PermisoEntity(usuario = usuarioLogin, fragment = it.item, permiso = true)
            })
            // 2. Solo si la descarga fue exitosa, borrar e insertar
            db.menuDao().deleteAllMenus()
            db.subMenuDao().deleteAllSubMenus()
            db.favoritoDao().deleteFavoritosByUsuario(usuarioLogin)
            db.permisoDao().deletePermisosByUsuario(usuarioLogin)
            db.menuDao().insertMenus(menus)
            db.subMenuDao().insertSubMenus(subMenus)
            db.favoritoDao().insertFavoritos(favoritos)
            db.permisoDao().insertPermisos(permisos)
        }
        // Si falla, no se borra nada y se mantiene la última configuración local
    }
} 