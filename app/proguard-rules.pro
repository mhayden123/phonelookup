# libphonenumber loads its metadata by reflection over resource bundles.
-keep class com.google.i18n.phonenumbers.** { *; }
-keepclassmembers class com.google.i18n.phonenumbers.** { *; }
-dontwarn com.google.i18n.phonenumbers.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
