package com.thepublichistorian.heritagehunter.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.thepublichistorian.heritagehunter.R
import com.thepublichistorian.heritagehunter.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var placesListener: ListenerRegistration? = null // Echtzeit-Listener

    // Konstanten zur Berechtigungsanfrage
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        // Setze den LayoutManager für den RecyclerView
        binding.recyclerViewPlaces.layoutManager = LinearLayoutManager(context)

        // Standortdienst initialisieren
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialisiere das LocationRequest-Objekt, um den Standort regelmäßig abzufragen
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(10f)
            .build()

        // LocationCallback zum Verarbeiten der Standort-Updates
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    Log.d("HomeFragment", "Aktualisierter Standort: ${location.latitude}, ${location.longitude}")
                    refreshPlaces() // Liste der Orte aktualisieren
                }
            }
        }

        // Überprüfe die Berechtigung
        checkLocationPermission()

        // Starte den Echtzeit-Listener
        setupRealTimeListener()

        return binding.root
    }

    private fun setupRealTimeListener() {
        // Echtzeit-Aktualisierungen für die Orte einrichten
        placesListener = db.collection("places")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("HomeFragment", "Listen failed.", error)
                    return@addSnapshotListener
                }

                // Stelle sicher, dass wir eine gültige Liste haben und die Liste neu laden
                refreshPlaces()
            }
    }

    // Methode zur Überprüfung, ob die Berechtigung erteilt wurde
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            Toast.makeText(context, "Standortberechtigung nicht erteilt", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun refreshPlaces() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    homeViewModel.fetchPlaces(location.latitude, location.longitude)
                    homeViewModel.places.observe(viewLifecycleOwner) { places ->
                        if (places != null && places.isNotEmpty()) {
                            binding.recyclerViewPlaces.adapter =
                                PlacesAdapter(places, location.latitude, location.longitude)
                        } else {
                            Toast.makeText(context, "Keine Orte gefunden", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } else {
            Toast.makeText(context, "Standortberechtigung nicht erteilt", Toast.LENGTH_SHORT).show()
        }
    }

    // Methode zum Stoppen des Echtzeit-Listeners
    private fun stopRealTimeListener() {
        placesListener?.remove()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        stopRealTimeListener() // Echtzeit-Listener stoppen, wenn das Fragment nicht mehr sichtbar ist
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
