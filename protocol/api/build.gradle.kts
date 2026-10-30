plugins { alias(libs.plugins.android.library) }
dependencies {
    api(project(":core:model"))
    api(libs.coroutines.android)
}
