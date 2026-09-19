# Appwrite
-keep class io.appwrite.** { *; }
-dontwarn io.appwrite.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp / Retrofit style
-dontwarn okhttp3.**
-dontwarn okio.**
