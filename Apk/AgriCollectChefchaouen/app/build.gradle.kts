plugins {
    // Plugins requis pour un projet Android avec code Java et Views (essentiel)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    // Configuration de base de l'application
    namespace = "com.example.agricollectchefchaouen" // VÉRIFIEZ LE NOM DE VOTRE PACKAGE
    compileSdk = 34 // API de compilation moderne

    defaultConfig {
        applicationId = "com.example.agricollectchefchaouen" // VÉRIFIEZ LE NOM DE VOTRE PACKAGE
        minSdk = 21 // API Minimale (notre cible)
        targetSdk = 34 // API Cible (recommandée)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Configuration de compilation (Kotlin et Java)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Dépendances de l'application : SEULEMENT CE DONT VOUS AVEZ BESOIN
dependencies {

    // Dépendances ESSENTIELLES pour la WebView et AppCompatActivity
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.10.0")

    // Dépendances de support (core-ktx est nécessaire pour la plupart des projets Kotlin/Java)
    implementation(libs.androidx.core.ktx)

    // Dépendances de test (laissées pour que l'IDE ne se plaigne pas)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}