package com.khiemnph.simpletracking.settings

/** Which theme the user asked for, independent of what the system is currently doing. */
enum class ThemeChoice {
    System,
    Light,
    Dark,
}

/**
 * Everything the user can choose about how the app looks.
 *
 * The defaults are the brief's: follow the system, and keep the app's own palette. Dynamic colour
 * is offered rather than assumed, because an app whose identity is its palette should not surrender
 * it to a wallpaper on first launch.
 */
data class UserPreferences(
    val theme: ThemeChoice = ThemeChoice.System,
    val dynamicColour: Boolean = false,
)
