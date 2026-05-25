# ProGuard configuration for wear module

# Keep Samsung Health SDK sensor tracking classes and API signatures intact
-keep class com.samsung.android.service.health.tracking.** { *; }
