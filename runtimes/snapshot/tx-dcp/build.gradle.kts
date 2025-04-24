plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.spi.identity.trust)
    implementation(libs.edc.spi.contract)
    implementation(libs.edc.spi.transfer)
    implementation(libs.edc.spi.catalog)
    implementation(libs.tx.core.spi)
    implementation(libs.tx.core.utils)
}
