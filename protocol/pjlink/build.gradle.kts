plugins { alias(libs.plugins.android.library) }
dependencies {
    api(project(":protocol:api"))
    implementation(project(":core:network"))
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
