import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

// Shared defaults; dependencies remain explicit in each module.
subprojects {
    pluginManager.withPlugin("com.android.library") {
        extensions.configure<LibraryExtension> {
            namespace = "com.universalremote." + project.path.trim(':').replace(':', '.')
            compileSdk = 37
            defaultConfig { minSdk = 26 }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
        dependencies.add("testImplementation", rootProject.libs.junit)
        dependencies.add("testImplementation", rootProject.libs.coroutines.test)
    }
}
