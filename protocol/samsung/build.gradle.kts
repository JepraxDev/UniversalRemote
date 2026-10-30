plugins { alias(libs.plugins.android.library) }
dependencies {
    api(project(":protocol:api"))
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
