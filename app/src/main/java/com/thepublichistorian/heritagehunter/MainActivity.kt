package com.thepublichistorian.heritagehunter

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.thepublichistorian.heritagehunter.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import android.app.AlertDialog
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import com.thepublichistorian.heritagehunter.ui.home.HomeFragment
import com.thepublichistorian.heritagehunter.models.Place
import android.widget.TextView
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient // FusedLocationClient hinzufügen
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

                // Initialisiere den FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // FAB referenzieren und Klick-Listener setzen
        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            // Funktionalität für das Hinzufügen eines neuen Ortes hier
            showAddNewPlaceDialog()
        }

        setSupportActionBar(binding.appBarMain.toolbar)

        // Initialisiere Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Aktualisiere die Anzeige des Levels in der Toolbar
        updateUserLevelDisplay()

        // Überprüfe, ob der Nutzer eingeloggt ist
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Nutzer ist nicht eingeloggt, leite zur Anmeldeseite weiter
            val signInIntent = Intent(this, SignInActivity::class.java)
            startActivity(signInIntent)
            finish()  // Beende die MainActivity, damit der Nutzer nicht zurück navigieren kann
            return
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Passing each menu ID as a set of IDs because each
        // menu should be considered as top-level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow, R.id.nav_settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    // Dialog zum Hinzufügen eines neuen Ortes anzeigen
    private fun showAddNewPlaceDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_place, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Neuen Ort hinzufügen")
            .setPositiveButton("Hinzufügen") { dialog, _ ->
                val placeName = dialogView.findViewById<EditText>(R.id.place_name_input).text.toString()
                val category = dialogView.findViewById<Spinner>(R.id.category_spinner).selectedItem.toString()

                if (placeName.isNotEmpty()) {
                    addNewPlace(placeName, category)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Bitte einen Ortsnamen eingeben", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Abbrechen") { dialog, _ -> dialog.dismiss() }

        dialogBuilder.create().show()
    }

    // Neuen Ort hinzufügen und den Standort verwenden
    private fun addNewPlace(placeName: String, category: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            // Überprüfe die Standortberechtigung
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Berechtigung wurde erteilt, Standort ermitteln
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val place = hashMapOf(
                            "name" to placeName,
                            "category" to category,
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "counter" to 1,
                            "visitors" to listOf(uid)
                        )

                        // Ort zur Firebase hinzufügen
                        FirebaseFirestore.getInstance().collection("places")
                            .add(place)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Ort hinzugefügt", Toast.LENGTH_SHORT).show()

                                // Füge 10 Punkte zum Nutzer hinzu
                                val userRef = db.collection("users").document(uid)
                                db.runTransaction { transaction ->
                                    val snapshot = transaction.get(userRef)
                                    val currentPoints = snapshot.getLong("points") ?: 0
                                    val newPoints = currentPoints + 10  // 10 Punkte für das Hinzufügen eines neuen Ortes

                                    transaction.update(userRef, "points", newPoints)
                                }.addOnSuccessListener {
                                    Toast.makeText(this, "10 Punkte hinzugefügt!", Toast.LENGTH_SHORT).show()
                                }.addOnFailureListener {
                                    Toast.makeText(this, "Fehler beim Hinzufügen der Punkte", Toast.LENGTH_SHORT).show()
                                }

                                // Orte neu laden, nachdem der neue Ort hinzugefügt wurde
                                val homeFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as? HomeFragment
                                homeFragment?.refreshPlaces()  // Methode zum Neuladen der Orte aufrufen
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Fehler beim Hinzufügen des Ortes", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Standort konnte nicht ermittelt werden", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Berechtigung nicht erteilt, zeige eine Warnung oder fordere sie an
                Toast.makeText(this, "Standortberechtigung nicht erteilt", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun logVisit(placeId: String, place: Place, userId: String) {
        val placeRef = db.collection("places").document(placeId)
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            // Zuerst alle benötigten Daten lesen
            val placeSnapshot = transaction.get(placeRef)
            val userSnapshot = transaction.get(userRef)

            // Leseoperationen abgeschlossen, jetzt Schreiboperationen durchführen
            val currentVisitors = placeSnapshot.get("visitors") as? MutableList<String> ?: mutableListOf()
            val currentCounter = placeSnapshot.getLong("counter") ?: 0

            if (!currentVisitors.contains(userId)) {
                currentVisitors.add(userId)
                transaction.update(placeRef, "visitors", currentVisitors)
                transaction.update(placeRef, "counter", currentCounter + 1)

                // Berechne die Punkte basierend auf der Anzahl der Besucher
                val pointsToAdd = when {
                    currentCounter < 5 -> 8
                    currentCounter < 10 -> 6
                    currentCounter < 50 -> 4
                    currentCounter < 100 -> 2
                    else -> 1
                }

                val currentPoints = userSnapshot.getLong("points") ?: 0
                val newPoints = currentPoints + pointsToAdd

                // Punkte des Nutzers aktualisieren
                transaction.update(userRef, "points", newPoints)

                // Logge den Besuch bei diesem Ort für den Nutzer
                val userPlacesLogged = userSnapshot.get("placesLogged") as? MutableMap<String, Any> ?: mutableMapOf()
                userPlacesLogged[placeId] = mapOf("visits" to (currentCounter + 1), "pointsEarned" to pointsToAdd)
                transaction.update(userRef, "placesLogged", userPlacesLogged)
            }
        }.addOnSuccessListener {
            Toast.makeText(this, "Besuch erfolgreich eingeloggt! Punkte wurden gutgeschrieben.", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { exception ->
            Log.e("MainActivity", "Fehler beim Einloggen des Besuchs: ", exception)
            Toast.makeText(this, "Fehler beim Einloggen des Besuchs", Toast.LENGTH_SHORT).show()
        }
    }

    // Funktion zur Berechnung des Levels basierend auf den Punkten
    private fun calculateLevel(totalPoints: Long): Int {
        return when {
            totalPoints >= 16000 -> 10
            totalPoints >= 8000 -> 9
            totalPoints >= 4000 -> 8
            totalPoints >= 2000 -> 7
            totalPoints >= 1000 -> 6
            totalPoints >= 500 -> 5
            totalPoints >= 250 -> 4
            totalPoints >= 100 -> 3
            totalPoints >= 50 -> 2
            else -> 1
        }
    }

    private fun updateUserLevelAndPoints(uid: String, pointsToAdd: Int) {
        val userRef = db.collection("users").document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentPoints = snapshot.getLong("points") ?: 0
            val currentLevel = snapshot.getLong("level")?.toInt() ?: 1
            val newTotalPoints = currentPoints + pointsToAdd

            val newLevel = calculateLevel(newTotalPoints)

            transaction.update(userRef, mapOf(
                "points" to newTotalPoints,
                "level" to newLevel
            ))

            if (newLevel > currentLevel) {
                showLevelUpNotification(newLevel)  // this wird als Kontext verwendet
            }
        }.addOnSuccessListener {
            Toast.makeText(this, "Punkte und Level aktualisiert!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLevelUpNotification(newLevel: Int) {
        Toast.makeText(this, "Glückwunsch! Du hast Level $newLevel erreicht!", Toast.LENGTH_LONG).show()
    }

    private fun updateUserLevelDisplay() {
        val userRef = db.collection("users").document(auth.currentUser?.uid ?: "")
        userRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val userLevel = document.getLong("level") ?: 1
                val levelTextView: TextView = findViewById(R.id.user_level)
                levelTextView.text = "Level $userLevel"
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Fehler beim Abrufen des Levels", Toast.LENGTH_SHORT).show()
        }
    }
}
