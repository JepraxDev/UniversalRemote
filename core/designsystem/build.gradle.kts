plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.compose) }
android { buildFeatures { compose = true } }
dependencies {
    api(project(":core:model"))
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.material.icons)
}
