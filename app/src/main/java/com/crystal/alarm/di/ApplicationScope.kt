package com.crystal.alarm.di

import javax.inject.Qualifier

/** Scope that outlives screens: used by receivers and background sync. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
