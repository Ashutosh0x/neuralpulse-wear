# ProGuard configuration for app module

# Keep Samsung Health SDK stub classes and API signatures intact
-keep class com.samsung.android.health.data.** { *; }

# Keep MediaPipe framework classes to prevent issues with native code invocation (JNI)
-keep class com.google.mediapipe.** { *; }
-keep class com.google.mediapipe.proto.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
