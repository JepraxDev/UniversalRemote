plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.compose); alias(libs.plugins.hilt); alias(libs.plugins.ksp) }
android { buildFeatures { compose = true } }
dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
