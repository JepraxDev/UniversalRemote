plugins { alias(libs.plugins.android.library); alias(libs.plugins.hilt); alias(libs.plugins.ksp) }
dependencies {
    api(project(":domain"))
    implementation(project(":core:database"))
    implementation(project(":core:infrared"))
    implementation(project(":protocol:ir"))
    implementation(libs.datastore.preferences)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
