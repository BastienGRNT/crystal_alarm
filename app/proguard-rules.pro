# Navigation type-safe routes are @Serializable objects: keep their serializers.
-keepclassmembers @kotlinx.serialization.Serializable class com.crystal.alarm.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
