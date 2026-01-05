package com.example.agricollectchefchaouen;
// ⬅️ VÉRIFIEZ ET REMPLACEZ 'com.example.agricollectchefchaouen' par le nom de votre package réel

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.GeolocationPermissions;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider; // NOUVEAU: Import pour la gestion des URI de fichiers
import android.util.Log;
import java.io.File; // NOUVEAU: Import pour la gestion des fichiers
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ValueCallback<Uri[]> mFilePathCallback;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private final static int PERMISSION_REQUEST_CODE = 100;

    // Déclare les permissions requises au runtime
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE // Nécessaire pour la compatibilité
    };

    private WebView webView;
    private Uri mCameraPhotoPath; // URI du fichier temporaire créé pour la photo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation du WebView. VÉRIFIEZ que l'ID dans activity_main.xml est 'webview' (minuscule)
        webView = findViewById(R.id.webview);

        // 1. Démarre le processus de demande de permissions
        requestPermissionsIfNecessary();

        // 2. Le reste de l'initialisation sera géré dans initWebView() APRÈS les permissions.
    }

    /** Initialise la WebView une fois que les permissions sont (ou non) accordées. */
    private void initWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setBuiltInZoomControls(false);

        // 🚨 VÉRIFICATIONS CRITIQUES POUR L'ACCÈS AUX FICHIERS ET PHOTOS
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        webView.setWebChromeClient(new CustomWebChromeClient());
        webView.loadUrl("file:///android_asset/index.html");
        webView.resumeTimers();
    }

    // --- GESTION DES PERMISSIONS ---
    private void requestPermissionsIfNecessary() {
        boolean allGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            // Demande les permissions à l'utilisateur
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        } else {
            // Les permissions sont déjà accordées, on peut initialiser la WebView
            initWebView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Quoi qu'il arrive, on essaie de lancer la WebView après la réponse
            initWebView();
        }
    }

    /**
     * Crée un fichier image temporaire avec un nom unique (horodatage).
     * @return L'URI du fichier temporaire.
     */
    private Uri getOutputUri() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Utilise FileProvider pour obtenir un URI compatible avec toutes les versions d'Android (API 24+)
        return FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".fileprovider", // Doit correspondre à l'autorité dans AndroidManifest.xml
                imageFile);
    }

    // --- CLASSE POUR GÉRER L'OUVERTURE DE LA CAMÉRA ET DU GPS ---
    public class CustomWebChromeClient extends WebChromeClient {

        // GESTION DE LA LOCALISATION (GPS)
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                                                       GeolocationPermissions.Callback callback) {
            // Autorise toujours le GPS (car la permission native est gérée par requestPermissionsIfNecessary)
            callback.invoke(origin, true, false);
        }

        // GESTION DE L'APAREIL PHOTO / SÉLECTION DE FICHIERS (Crucial pour le bouton photo)
        @Override
        public boolean onShowFileChooser(WebView webView,
                                         ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {

            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;

            // NOUVEAU: Préparation du fichier URI pour la caméra
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    // Créer l'URI du fichier temporaire
                    mCameraPhotoPath = getOutputUri();
                    // Stocke le chemin URI complet de l'image
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraPhotoPath); // Assigne l'URI à l'intent de la caméra
                } catch (IOException ex) {
                    Log.e("MainActivity", "Erreur lors de la création du fichier image", ex);
                    mCameraPhotoPath = null;
                }
            }

            // Intention pour la sélection de fichiers/galerie
            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType(fileChooserParams.getAcceptTypes()[0]);

            // Combinaison des intentions (Caméra et Galerie)
            Intent[] intentArray;
            if (mCameraPhotoPath != null) { // Si l'URI temporaire a été créée avec succès
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Sélectionner une source");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            try {
                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
            } catch (Exception e) {
                mFilePathCallback = null;
                Log.e("MainActivity", "Erreur lors du lancement du sélecteur de fichiers", e);
                return false;
            }
            return true;
        }
    }

    // --- RÉCUPÉRATION DU RÉSULTAT DE LA CAMÉRA (ESSENTIEL) ---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mFilePathCallback) {
                return;
            }

            Uri[] results = null;

            if (resultCode == Activity.RESULT_OK) {
                if (intent == null || intent.getData() == null) {
                    // C'est le cas où la caméra a été utilisée et qu'elle n'a pas renvoyé l'URI dans l'intent (cas fréquent)
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{mCameraPhotoPath};
                    }
                } else {
                    // C'est le cas où la galerie ou un gestionnaire de fichiers a été utilisé
                    String dataString = intent.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    } else if (intent.getClipData() != null) {
                        int count = intent.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = intent.getClipData().getItemAt(i).getUri();
                        }
                    } else {
                        results = new Uri[]{intent.getData()};
                    }
                }
            }

            // Envoie l'URI au JavaScript
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
            mCameraPhotoPath = null; // Réinitialisation
        }
    }
}