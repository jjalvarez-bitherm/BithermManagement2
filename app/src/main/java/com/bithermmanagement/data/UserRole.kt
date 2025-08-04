package com.bithermmanagement.data

enum class UserRole(val weight: Int) {
    SUPERADMIN(5),
    ADMIN(4),
    TEAMLIDER(3),
    INSPECTOR(2),
    VIEWER(1),
    AUDITOR(1),
    TRABAJADOR(1)
} 