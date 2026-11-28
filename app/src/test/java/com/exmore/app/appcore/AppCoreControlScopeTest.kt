package com.exmore.app.appcore

import org.junit.Assert.*
import org.junit.Test

class AppCoreControlScopeTest {
    @Test fun pilotControlPathsAndCachesAreSeparate() {
        assertTrue(AppCoreConfig.endpoint(true).endsWith("/api/v1/pilot/apps/exmore/android/config"))
        assertTrue(AppCoreConfig.endpoint(false).endsWith("/api/v1/apps/exmore/android/config"))
        assertNotEquals(AppCoreConfig.storageKey(true), AppCoreConfig.storageKey(false))
        assertTrue(AppCoreBulletin.endpoint(true).endsWith("/pilotBulletins/exmore"))
        assertTrue(AppCoreBulletin.endpoint(false).endsWith("/publicBulletins/exmore"))
        assertNotEquals(AppCoreBulletin.storageKey(true), AppCoreBulletin.storageKey(false))
    }
}
