plugins { alias(libs.plugins.android.library); alias(libs.plugins.hilt); alias(libs.plugins.ksp) }
dependencies {
    api(project(":domain"))
    implementation(project(":core:database"))
    implementation(project(":core:infrared"))
    implementation(project(":core:network"))
    implementation(project(":protocol:api"))
    implementation(project(":protocol:ir"))
    implementation(project(":protocol:lgwebos"))
    implementation(project(":protocol:pjlink"))
    implementation(project(":protocol:samsung"))
    implementation(project(":protocol:androidtv"))
    implementation(libs.datastore.preferences)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
