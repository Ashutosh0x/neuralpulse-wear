package com.samsung.android.health.data.error

class HealthConnectionErrorResult(val errorCode: Int) {
    companion object {
        const val SDK_VERSION_MISMATCH = 1
        const val PLATFORM_NOT_INSTALLED = 2
        const val OLD_PLATFORM_VERSION = 3
        const val USER_AGREEMENT_NEEDED = 4
    }
}
