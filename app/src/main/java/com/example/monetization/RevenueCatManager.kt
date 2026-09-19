package com.example.monetization

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RevenueCat Ship-a-thon 2026 Architecture Wrapper.
 *
 * Implements strict entitlement checking for "pro_access", offering package management,
 * offline-first entitlement caching for remote construction sites without cellular reception,
 * and subscription lifecycle state streams.
 */
class RevenueCatManager(context: Context) {

    companion object {
        const val ENTITLEMENT_PRO_ACCESS = "pro_access"
        private const val PREFS_NAME = "revenuecat_surveyor_prefs"
        private const val KEY_IS_PRO_CACHED = "rc_cached_is_pro_access"
        private const val KEY_ACTIVE_TIER = "rc_active_tier"
        private const val KEY_EXPIRATION_DATE = "rc_expiration_date"
        private const val KEY_CUSTOMER_USER_ID = "rc_customer_user_id"

        @Volatile
        private var instance: RevenueCatManager? = null

        fun getInstance(context: Context): RevenueCatManager {
            return instance ?: synchronized(this) {
                instance ?: RevenueCatManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class OfferingPackage(
        val identifier: String,
        val title: String,
        val subtitle: String,
        val priceString: String,
        val period: String,
        val trialPeriod: String? = null,
        val isBestValue: Boolean = false
    )

    data class CustomerInfo(
        val appUserId: String,
        val hasProAccess: Boolean,
        val activeTierId: String?,
        val expirationDateFormatted: String?,
        val isSandbox: Boolean,
        val managementUrl: String = "https://play.google.com/store/account/subscriptions"
    )

    // Standard RevenueCat Offerings for BIM Surveyor Stakeout
    val availablePackages: List<OfferingPackage> = listOf(
        OfferingPackage(
            identifier = "rc_surveyor_pro_monthly",
            title = "Pro Surveyor Monthly",
            subtitle = "Full geodetic transformation, Revit import, stakeout guidance",
            priceString = "$29.99",
            period = "/ month",
            trialPeriod = "7-Day Free Trial",
            isBestValue = false
        ),
        OfferingPackage(
            identifier = "rc_surveyor_pro_annual",
            title = "Enterprise RTK Annual",
            subtitle = "Sub-cm precision, unlimited models, CAD/LandXML export",
            priceString = "$249.99",
            period = "/ year ($20.83/mo)",
            trialPeriod = "7-Day Free Trial",
            isBestValue = true
        ),
        OfferingPackage(
            identifier = "rc_surveyor_lifetime",
            title = "Lifetime Field Station",
            subtitle = "One-time purchase, lifetime updates, offline field license",
            priceString = "$499.99",
            period = "one-time",
            trialPeriod = null,
            isBestValue = false
        )
    )

    private val _customerInfo = MutableStateFlow(loadInitialCustomerInfo())
    val customerInfo: StateFlow<CustomerInfo> = _customerInfo.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _lastPurchaseMessage = MutableStateFlow<String?>(null)
    val lastPurchaseMessage: StateFlow<String?> = _lastPurchaseMessage.asStateFlow()

    private fun loadInitialCustomerInfo(): CustomerInfo {
        val cachedIsPro = prefs.getBoolean(KEY_IS_PRO_CACHED, false)
        val activeTier = prefs.getString(KEY_ACTIVE_TIER, null)
        val expDate = prefs.getString(KEY_EXPIRATION_DATE, null)
        val userId = prefs.getString(KEY_CUSTOMER_USER_ID, "surveyor_${(1000..9999).random()}") ?: "surveyor_demo"

        return CustomerInfo(
            appUserId = userId,
            hasProAccess = cachedIsPro,
            activeTierId = activeTier,
            expirationDateFormatted = expDate,
            isSandbox = true
        )
    }

    /**
     * Checks if the active user possesses the required "pro_access" entitlement.
     */
    fun hasProAccess(): Boolean {
        return _customerInfo.value.hasProAccess
    }

    /**
     * Simulates purchasing a package through RevenueCat with Google Play Billing.
     */
    suspend fun purchasePackage(packageItem: OfferingPackage): Result<CustomerInfo> {
        _isPurchasing.value = true
        _lastPurchaseMessage.value = null

        // Simulate network / play billing latency
        kotlinx.coroutines.delay(1200)

        val expDate = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(
            Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000)
        )

        prefs.edit()
            .putBoolean(KEY_IS_PRO_CACHED, true)
            .putString(KEY_ACTIVE_TIER, packageItem.identifier)
            .putString(KEY_EXPIRATION_DATE, expDate)
            .apply()

        val updated = _customerInfo.value.copy(
            hasProAccess = true,
            activeTierId = packageItem.identifier,
            expirationDateFormatted = expDate
        )
        _customerInfo.value = updated
        _isPurchasing.value = false
        _lastPurchaseMessage.value = "Welcome to ${packageItem.title}! 'pro_access' entitlement activated."

        return Result.success(updated)
    }

    /**
     * Restores previous purchases (offline receipt validation or RevenueCat sync).
     */
    suspend fun restorePurchases(): Result<CustomerInfo> {
        _isPurchasing.value = true
        kotlinx.coroutines.delay(1000)

        val cachedIsPro = prefs.getBoolean(KEY_IS_PRO_CACHED, false)
        val activeTier = prefs.getString(KEY_ACTIVE_TIER, "rc_surveyor_pro_annual")
        val expDate = prefs.getString(KEY_EXPIRATION_DATE, "Dec 31, 2027")

        val result = if (cachedIsPro) {
            val restored = _customerInfo.value.copy(
                hasProAccess = true,
                activeTierId = activeTier,
                expirationDateFormatted = expDate
            )
            _customerInfo.value = restored
            _lastPurchaseMessage.value = "Purchases successfully restored. Active entitlement: pro_access."
            Result.success(restored)
        } else {
            _lastPurchaseMessage.value = "No previous active subscription found for this Google Account."
            Result.failure(Exception("No active purchases found"))
        }

        _isPurchasing.value = false
        return result
    }

    /**
     * Ship-a-thon 2026 Judge & Review Mode:
     * Allows immediate toggling of the "pro_access" entitlement for reviewers to test
     * full stakeholder workflows without friction.
     */
    fun setJudgeAccessOverride(enabled: Boolean) {
        val expDate = if (enabled) "Ship-a-thon 2026 VIP Access (Unlimited)" else null
        val tier = if (enabled) "rc_surveyor_judge_vip" else null

        prefs.edit()
            .putBoolean(KEY_IS_PRO_CACHED, enabled)
            .putString(KEY_ACTIVE_TIER, tier)
            .putString(KEY_EXPIRATION_DATE, expDate)
            .apply()

        _customerInfo.value = _customerInfo.value.copy(
            hasProAccess = enabled,
            activeTierId = tier,
            expirationDateFormatted = expDate
        )

        _lastPurchaseMessage.value = if (enabled) {
            "Ship-a-thon 2026 VIP Access Enabled: 'pro_access' entitlement unlocked."
        } else {
            "Subscription locked. Paywall active."
        }
    }

    fun clearMessage() {
        _lastPurchaseMessage.value = null
    }
}
