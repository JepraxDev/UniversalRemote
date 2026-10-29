plugins { alias(libs.plugins.android.library); alias(libs.plugins.ksp) }
dependencies {
    api(project(":core:model"))
    api(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
