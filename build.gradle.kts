// Top-level build file where you can add configuration options common to all sub-projects/modules.
// 使用兼容Gradle 8.7的配置
plugins {
    // 仅声明插件，让模块自行应用
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.7.20" apply false
}