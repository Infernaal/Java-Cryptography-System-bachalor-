-dontnote
-dontwarn
-dontshrink
-dontoptimize
-flattenpackagehierarchy ''
-keepattributes Signature,SourceFile,*Annotation*
-adaptresourcefilecontents **.fxml,**.properties,META-INF/MANIFEST.MF

-obfuscationdictionary filename.txt
-classobfuscationdictionary classnames.txt
-packageobfuscationdictionary packagenames.txt

-keepclassmembers class * {
    @javafx.fxml.FXML *;
}

# Keep - Applications. Keep all application classes, along with their 'main'
# methods.
-keep class javafx.** { *; }
-keep class com.sun.** { *; }
-keepclasseswithmembers public class sample.dataencryption.MainStart {
    public static void main(java.lang.String[]);
}
