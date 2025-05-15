package com.trello.app_trello.Firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Profile("prod")
public class Firebase {

    @PostConstruct
    public void initialize() throws IOException {
        String firebaseCredentialsBase64 = System.getenv("FIREBASE_CREDENTIALS_BASE64");
        if (firebaseCredentialsBase64 == null || firebaseCredentialsBase64.isEmpty()) {
            throw new IllegalStateException("La variable firebase.credentials.base64 no está configurada");
        }

        // Decodificar Base64
        byte[] decodedBytes = Base64.getDecoder().decode(firebaseCredentialsBase64);
        String credentialsJson = new String(decodedBytes, StandardCharsets.UTF_8);

        try (InputStream serviceAccount = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://TU-PROYECTO.firebaseio.com") // Reemplaza con tu ID
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        }
    }

    public Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }
}