package com.aliJafari.bbarq.di

import javax.inject.Qualifier

/** Dispatcher for blocking IO: disk, database, network. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Dispatcher for CPU work: parsing, mapping, status computation. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * A scope that lives as long as the process.
 *
 * Exists so nothing has to reach for `CoroutineScope(Dispatchers.IO)` inline
 * again: those scopes were never cancelled and outlived their owners.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
