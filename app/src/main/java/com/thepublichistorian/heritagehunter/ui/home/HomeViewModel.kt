package com.thepublichistorian.heritagehunter.ui.home

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.thepublichistorian.heritagehunter.models.Place
import android.util.Log

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // Die Liste enthält jetzt Paare aus Dokument-ID und dem Place-Objekt
    private val _places = MutableLiveData<List<Pair<String, Place>>>()
    val places: LiveData<List<Pair<String, Place>>> = _places

    // Abrufen der Orte aus Firebase und Sortieren nach Entfernung
    fun fetchPlaces(userLatitude: Double, userLongitude: Double) {
        firestore.collection("places")
            .get()
            .addOnSuccessListener { snapshot ->
                val placesList = mutableListOf<Pair<String, Place>>()

                for (document in snapshot) {
                    // Dokument-ID abrufen
                    val placeId = document.id

                    // Hole die Daten für jedes Dokument als Place-Objekt
                    val place = document.toObject(Place::class.java)

                    // Füge die Dokument-ID zusammen mit dem Place in die Liste ein
                    placesList.add(Pair(placeId, place))

                    // Debugging: Logge die Details jedes Ortes
                    Log.d("HomeViewModel", "Place found: ${place.name}, Latitude: ${place.latitude}, Longitude: ${place.longitude}, Document ID: $placeId")
                }

                // Falls keine Orte gefunden wurden, logge dies
                if (placesList.isEmpty()) {
                    Log.d("HomeViewModel", "No places found in Firebase.")
                } else {
                    Log.d("HomeViewModel", "Total places found: ${placesList.size}")
                }

                // Sortiere die Orte nach Entfernung vom Nutzerstandort
                val sortedPlaces = placesList.sortedBy { (placeId, place) ->
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        userLatitude, userLongitude,
                        place.latitude, place.longitude,
                        results
                    )
                    results[0]  // Rückgabe der Entfernung in Metern
                }

                // Zeige die Anzahl der nächsten 10 Orte im Log
                Log.d("HomeViewModel", "Nearest 10 places: ${sortedPlaces.take(10).size}")

                // Setze die 10 nächsten Orte (Pair aus placeId und Place)
                _places.value = sortedPlaces.take(10)
            }
            .addOnFailureListener { exception ->
                // Fehlerbehandlung
                Log.e("HomeViewModel", "Fehler beim Abrufen der Orte", exception)
            }
    }
}
