# Reglas de R8/ProGuard para el build de release.

# ── Atributos necesarios para reflexión (Retrofit/Gson) ─────────────────────
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ── Retrofit ─────────────────────────────────────────────────────────────────
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-dontwarn retrofit2.KotlinExtensions*
-dontwarn javax.annotation.**

# ── OkHttp / Okio ────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── Gson ─────────────────────────────────────────────────────────────────────
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# DTOs de red (Gson los deserializa por reflexión: nombres de campos intactos)
-keep class com.vaia.data.api.** { *; }
-keep class com.vaia.data.network.ErrorInterceptor$ErrorResponse { *; }

# ── Google API Client / Drive ────────────────────────────────────────────────
-dontwarn com.google.api.**
-dontwarn com.google.http-client.**
-dontwarn com.google.errorprone.annotations.**
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
    @com.google.api.client.util.Value <fields>;
}
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }

# ── Kotlin coroutines ────────────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**
