# MQTT
-keep class org.eclipse.paho.** { *; }
-dontwarn org.eclipse.paho.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.hisense.remote.model.** { *; }

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
