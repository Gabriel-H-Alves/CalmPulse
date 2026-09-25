# ==============================================================================
# Regras de ProGuard / R8 para o CalmPulse
# ==============================================================================

# 1. Preservar modelos de dados usados em serialização e Compose
-keepclassmembers class com.calmpulse.data.model.** { *; }
-keep class com.calmpulse.data.model.** { *; }

# 2. Preservar o SDK do Google Generative AI
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# 3. Preservar classes de Coroutines e Flow
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-dontwarn kotlinx.coroutines.**

# 4. Remover COMPLETAMENTE chamadas de Log em build de produção (Prevenção de vazamento SEC-005)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int println(...);
}

# 5. Preservar OkHttp e Okio para o Auto-Updater
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# 6. Preservar ViewModels instanciados por reflexão no Jetpack Compose
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class com.calmpulse.ui.chat.ChatViewModel {
    <init>(...);
    *;
}
-keep class com.calmpulse.util.AgentNameDetector { *; }
-keep class com.calmpulse.audio.** { *; }


