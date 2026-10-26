# Consumer ProGuard rules for SF Symbols Compose library
# Keeps Compose ImageVector properties if accessed
-keepclassmembers class com.composables.sfsymbols.** {
    public static ** get*();
}
