plugins { alias(libs.plugins.android.library) }
dependencies {
    api(project(":core:model"))
    api(project(":protocol:api"))
}
