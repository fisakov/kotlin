
description = "Kotlin Ant Tools"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(commonDependency("org.apache.ant", "ant"))
    api(project(":kotlin-preloader"))
    api(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar {
    manifest.attributes["Class-Path"] = "$compilerManifestClassPath kotlin-preloader.jar"
}
