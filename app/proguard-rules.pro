-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''
-dontpreverify
-optimizations !code/simplification/arithmetic,!code/allocation/variable
-dontwarn androidx.compose.**
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn org.bouncycastle.**
-dontwarn org.ietf.jgss.**
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn com.jcraft.jsch.**
-keep class com.jcraft.jsch.JSch { *; }
-keep class com.jcraft.jsch.Session { *; }
-keep class com.jcraft.jsch.Channel { *; }
-keep class com.jcraft.jsch.ChannelShell { *; }
-keep class com.jcraft.jsch.ChannelSession { *; }
-keep class com.jcraft.jsch.UserInfo
-keep class com.jcraft.jsch.UIKeyboardInteractive
-keep class com.jcraft.jsch.jce.** { *; }
-keep class com.jcraft.jsch.** { *; }
-keep class com.jcraft.jsch.Request { *; }
-keep class * extends com.jcraft.jsch.Request { *; }
-keep class com.openwrt.mgr.MainActivity
-keep class com.openwrt.mgr.MainAlias*
-keepclassmembers enum com.openwrt.mgr.** { public static **[] values(); public static ** valueOf(java.lang.String); }
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}
