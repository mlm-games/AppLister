-keepattributes Signature
-keepattributes *Annotation*
-dontobfuscate

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keep class app.applister.data.** { *; }
-keepnames class app.applister.data.**
