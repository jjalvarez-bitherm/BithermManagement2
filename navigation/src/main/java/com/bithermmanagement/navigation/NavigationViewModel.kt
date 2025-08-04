package com.bithermmanagement.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bithermmanagement.core.data.GoogleSheetsManager
import com.bithermmanagement.core.data.LoginRepository
import com.bithermmanagement.core.data.MenuRepository
import com.bithermmanagement.core.data.db.MenuEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
    private val menuRepository: MenuRepository,
    private val googleSheetsManager: GoogleSheetsManager
) : ViewModel() {

    private val _subMenuItems = MutableStateFlow<List<MenuEntity>>(emptyList())
    val subMenuItems: StateFlow<List<MenuEntity>> = _subMenuItems

    val userName: StateFlow<String?> = loginRepository.getLoggedInUser()
        .map { userData -> userData?.nombre }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val menuItems: Flow<List<MenuEntity>> = menuRepository.mainMenuFlow

    fun loadMenuData(rolpound: Int, userName: String) {
        viewModelScope.launch {
            // Suponiendo que 'rol' se puede obtener o no es estrictamente necesario para el menú principal
            // Si es necesario, habría que obtenerlo del userData
            val userData = loginRepository.getLoggedInUser().first()
            if (userData != null) {
                menuRepository.refreshMainMenu(userData.rol, rolpound)
            }
        }
    }

    fun loadSubMenuItems(mainMenuKey: String): Flow<List<MenuEntity>> {
        return flow {
            val userData = loginRepository.getLoggedInUser().first()
            if (userData != null) {
                val subItems = if (mainMenuKey == "Favoritos") {
                    googleSheetsManager.getFavoriteMenuItems(userData.app)
                } else {
                    googleSheetsManager.getSubMenuItems(mainMenuKey, userData.rol, userData.rolPound)
                }
                emit(subItems)
            } else {
                emit(emptyList())
            }
        }.flowOn(Dispatchers.IO)
    }
} 