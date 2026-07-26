# R8 is on for release (minifyEnabled + shrinkResources). Most of this app's dependencies ship
# their own consumer rules -- Room, Hilt, Navigation, Compose, Play Services -- so only the things
# R8 cannot see through belong here.

# SessionStatus is persisted by name: the enum constant is written into the `status` column and read
# back with valueOf(). The default android-optimize rules keep values()/valueOf() themselves, but
# not the constant names, so obfuscation would rename PAUSED to something like a and every row
# written by a previous install would fail to parse on first launch after an update.
-keepclassmembers enum com.khiemnph.domain.model.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keeps the original names in stack traces from a shrunk build; without this a crash report from
# release is unreadable. mapping.txt is still produced for deobfuscation.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
