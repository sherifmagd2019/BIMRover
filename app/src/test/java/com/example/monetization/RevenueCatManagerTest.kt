package com.example.monetization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RevenueCatManagerTest {

    @Test
    fun `default customer info without pro access`() {
        val info = RevenueCatManager.CustomerInfo(
            appUserId = "surveyor_anon_1",
            hasProAccess = false,
            activeTierId = null,
            expirationDateFormatted = null,
            isSandbox = true
        )
        assertFalse(info.hasProAccess)
        assertEquals(null, info.activeTierId)
    }

    @Test
    fun `active entitlement grants pro access`() {
        val info = RevenueCatManager.CustomerInfo(
            appUserId = "surveyor_pro_user",
            hasProAccess = true,
            activeTierId = "rc_surveyor_pro_annual",
            expirationDateFormatted = "2027-01-01",
            isSandbox = true
        )
        assertTrue(info.hasProAccess)
        assertEquals("rc_surveyor_pro_annual", info.activeTierId)
    }

    @Test
    fun `offering package structure holds pricing and benefits`() {
        val pkg = RevenueCatManager.OfferingPackage(
            identifier = "rc_surveyor_pro_annual",
            title = "Enterprise RTK Annual",
            subtitle = "Full geodetic engine, unlimited points & offline DB",
            priceString = "$249.99",
            period = "/ year",
            trialPeriod = "7-Day Free Trial",
            isBestValue = true
        )

        assertEquals("rc_surveyor_pro_annual", pkg.identifier)
        assertEquals("Enterprise RTK Annual", pkg.title)
        assertTrue(pkg.isBestValue)
        assertEquals("7-Day Free Trial", pkg.trialPeriod)
    }
}
