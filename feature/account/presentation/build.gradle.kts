plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.android.library.compose)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.navigation)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.account.presentation"
}

dependencies {
    implementation(projects.core.logging)
    implementation(projects.core.settings)
    implementation(projects.core.theme)
    implementation(projects.feature.account.domain)
    // The confirmation panel reports the balance the account restored, which is
    // the studio's port — a read across features at the domain layer, as with
    // the generated-card saver.
    implementation(projects.feature.aistudio.domain)
    // The community block counts the lists this phone has published, which the
    // tier side owns — the same domain-layer read across features.
    implementation(projects.feature.tier.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    // Google's own sign-in mark; their guidelines forbid a redrawing.
    implementation(libs.play.services.base)
    implementation(libs.googleid)
    implementation(libs.coil.compose)
}
