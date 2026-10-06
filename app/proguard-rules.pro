# Type-safe navigation routes are serialized by kotlinx.serialization
-keepclassmembers @kotlinx.serialization.Serializable class dev.haseeb.handi.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
