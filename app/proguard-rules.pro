#基本配置
# 设置混淆的压缩比率 0 ~ 7
-optimizationpasses 7
# 混淆时不使用大小写混合，混淆后的类名为小写
-dontusemixedcaseclassnames

# 指定不去忽略非公共库的类
#-dontskipnonpubliclibraryclasses
# 指定不去忽略非公共库的成员
#-dontskipnonpubliclibraryclassmembers
# 混淆时不做预校验
#-dontpreverify
# 混淆时不记录日志
-verbose
# 代码优化
-dontshrink
#小写
-dontusemixedcaseclassnames
# 不优化输入的类文件
-dontoptimize
# dump.txt文件列出apk包内所有class的内部结构
#-dump class_files.txt
# seeds.txt文件列出未混淆的类和成员
-printseeds seeds.txt
# usage.txt文件列出从apk中删除的代码
-printusage unused.txt
# mapping.txt文件列出混淆前后的映射
-printmapping mapping.txt

-keepattributes Signature,InnerClasses
-keepclasseswithmembers class io.netty.** {
    *;
}
-dontwarn io.netty.**
-dontwarn sun.**