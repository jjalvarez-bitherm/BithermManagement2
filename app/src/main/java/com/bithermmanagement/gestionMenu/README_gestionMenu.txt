Estructura y edición del menú expandible (gestionMenu)
========================================================

1. Estructura de carpetas y archivos:
-------------------------------------
- java/com/bithermmanagement/gestionMenu/
    - MenuModel.kt              // Modelos de datos para grupos e hijos
    - MenuExpandableAdapter.kt  // Adapter personalizado para ExpandableListView
    - README_gestionMenu.txt    // Este archivo de ayuda
- res/layout/gestionMenu/
    - item_menu_group.xml       // Layout para los grupos del menú
    - item_menu_child.xml       // Layout para los hijos del menú
- res/drawable/gestionMenu/
    - ic_settings.xml, ic_workers.xml, ... // Iconos SVG/XML para cada opción

2. Cómo añadir o modificar grupos e hijos:
------------------------------------------
- Edita la lista 'menuGroups' en StartPageMenuActivity.kt
- Cada grupo es un MenuGroup con título, icono y lista de hijos (MenuChild)
- Cada hijo tiene título, icono y una función onClick (acción al pulsar)

Ejemplo de grupo e hijo:
MenuGroup(
    title = "CONFIGURACIÓN",
    iconRes = R.drawable.gestionMenu_ic_settings,
    children = listOf(
        MenuChild("CONF. USUARIO", R.drawable.gestionMenu_ic_person) { /* acción */ },
        ...
    )
)

3. Cómo cambiar iconos:
-----------------------
- Los iconos están en res/drawable/gestionMenu/
- Puedes añadir nuevos SVG/XML y referenciarlos en los grupos/hijos

4. Cómo cambiar el diseño visual:
---------------------------------
- Modifica los layouts en res/layout/gestionMenu/item_menu_group.xml y item_menu_child.xml
- Cambia colores, tamaños, márgenes, etc.

5. Cómo cambiar acciones:
-------------------------
- Edita la función onClick de cada MenuChild en StartPageMenuActivity.kt
- Puedes navegar, mostrar mensajes, abrir fragments, etc.

6. Reutilización para otras ramas:
----------------------------------
- Copia la carpeta gestionMenu y adapta los datos/modelos según la rama/menú que necesites

¡Todo el menú está centralizado y documentado para que puedas editarlo fácilmente! 