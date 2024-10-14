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

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient // FusedLocationClient hinzufügen

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
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
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

                        FirebaseFirestore.getInstance().collection("places")
                            .add(place)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Ort hinzugefügt", Toast.LENGTH_SHORT).show()

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
}
