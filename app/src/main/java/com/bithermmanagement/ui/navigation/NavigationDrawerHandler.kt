package com.bithermmanagement.ui.navigation

import android.view.View
import android.widget.ImageButton
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import android.animation.ValueAnimator
import android.view.ViewGroup
import androidx.core.view.GravityCompat

class NavigationDrawerHandler(
    private val drawerLayout: DrawerLayout,
    private val navigationView: NavigationView
) {
    private var isExpanded = true
    private val expandedWidth: Int = navigationView.resources.getDimensionPixelSize(com.bithermmanagement.R.dimen.nav_drawer_width_expanded)
    private val collapsedWidth: Int = navigationView.resources.getDimensionPixelSize(com.bithermmanagement.R.dimen.nav_drawer_width_collapsed)

    init {
        setupDrawerToggle()
    }

    private fun setupDrawerToggle() {
        // No need to set up drawer toggle as menuToggle has been removed
    }

    private fun toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            if (isExpanded) {
                collapseDrawer()
            } else {
                expandDrawer()
            }
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun collapseDrawer() {
        animateDrawerWidth(expandedWidth, collapsedWidth)
        isExpanded = false
    }

    private fun expandDrawer() {
        animateDrawerWidth(collapsedWidth, expandedWidth)
        isExpanded = true
    }

    private fun animateDrawerWidth(startWidth: Int, endWidth: Int) {
        val animator = ValueAnimator.ofInt(startWidth, endWidth)
        animator.addUpdateListener { animation ->
            val params = navigationView.layoutParams as ViewGroup.LayoutParams
            params.width = animation.animatedValue as Int
            navigationView.layoutParams = params
        }
        animator.duration = 300
        animator.start()
    }
} 