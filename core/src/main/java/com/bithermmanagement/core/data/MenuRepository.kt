package com.bithermmanagement.core.data

import com.bithermmanagement.core.data.db.MenuDao
import com.bithermmanagement.core.data.db.MenuEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepository @Inject constructor(
    private val googleSheetsManager: GoogleSheetsManager,
    private val menuDao: MenuDao
) {

    val mainMenuFlow: Flow<List<MenuEntity>> = menuDao.getAllMainMenuItems()

    suspend fun refreshMainMenu(userRol: String, userRolPound: Int) {
        try {
            val menuItems = googleSheetsManager.getMainMenuItems(userRol, userRolPound)
            menuDao.deleteAllMenuItems()
            menuDao.insertAll(menuItems)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSubMenuFlow(parentId: String): Flow<List<MenuEntity>> {
        return menuDao.getSubMenuItems(parentId)
    }

    suspend fun refreshSubMenu(mainMenuKey: String, userRol: String, userRolPound: Int) {
        try {
            val subMenuItems = googleSheetsManager.getSubMenuItems(mainMenuKey, userRol, userRolPound)
            // No borramos, solo insertamos/actualizamos para no eliminar el menú principal
            menuDao.insertAll(subMenuItems)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 