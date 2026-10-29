# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class hu.nutrinaplo.app.**$$serializer { *; }
-keepclassmembers class hu.nutrinaplo.app.** { *** Companion; }
-keepclasseswithmembers class hu.nutrinaplo.app.** { kotlinx.serialization.KSerializer serializer(...); }
-keep class hu.nutrinaplo.app.core.** { *; }
