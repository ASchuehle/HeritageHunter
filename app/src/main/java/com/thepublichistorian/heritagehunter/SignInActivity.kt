package com.thepublichistorian.heritagehunter

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance() // Firestore-Instanz

    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // Google SignIn Optionen konfigurieren
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Die ID aus google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseAuth.getInstance()

        // Klick-Listener für den Sign-In-Button
        findViewById<Button>(R.id.sign_in_button).setOnClickListener {
            signIn()
        }
    }

    // Die Google-SignIn-Logik
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Das Ergebnis des Anmeldeversuchs
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.result
            firebaseAuthWithGoogle(account!!)
        }
    }

    // Firebase mit Google-Anmeldung verknüpfen
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Anmeldung erfolgreich, Nutzer in Firestore speichern
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        checkAndCreateUserInFirestore(currentUser.uid)
                    }

                    // Weiter zur MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Anmeldung fehlgeschlagen
                    Toast.makeText(this, "Anmeldung fehlgeschlagen", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Überprüfen, ob der Nutzer bereits in Firestore existiert, und falls nicht, erstellen
    private fun checkAndCreateUserInFirestore(uid: String) {
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Nutzer existiert bereits in der Datenbank
                Toast.makeText(this, "Willkommen zurück!", Toast.LENGTH_SHORT).show()
            } else {
                // Erstelle neuen Nutzereintrag
                val userData = hashMapOf(
                    "level" to 1,
                    "points" to 0,
                    "email" to auth.currentUser?.email
                )

                userRef.set(userData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Nutzerprofil erstellt.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Fehler beim Erstellen des Nutzerprofils.", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Fehler beim Überprüfen des Nutzerprofils.", Toast.LENGTH_SHORT).show()
        }
    }
}
