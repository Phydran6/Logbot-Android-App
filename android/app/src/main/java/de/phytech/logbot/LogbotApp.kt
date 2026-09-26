/**
 * Application-Klasse mit Hilt-Setup. Einstiegspunkt für Dependency Injection.
 */
package de.phytech.logbot

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LogbotApp : Application()
