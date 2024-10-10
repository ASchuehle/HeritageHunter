package com.thepublichistorian.heritagehunter.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.thepublichistorian.heritagehunter.databinding.FragmentHomeBinding
import android.widget.Toast

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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

        // Beobachte die Orte und aktualisiere den RecyclerView
        homeViewModel.places.observe(viewLifecycleOwner) { places ->
            if (places != null && places.isNotEmpty()) {
                binding.recyclerViewPlaces.adapter = PlacesAdapter(places)
            } else {
                Toast.makeText(context, "Keine Orte gefunden", Toast.LENGTH_SHORT).show()
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Überprüfe die Berechtigung
        checkLocationPermission()

        return binding.root
    }

    // Methode zur Überprüfung, ob die Berechtigung erteilt wurde
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Berechtigung nicht erteilt, anfordern
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Berechtigung wurde bereits erteilt, Standort abrufen
            getUserLocation()
        }
    }

    // Methode zum Abrufen des Standorts des Nutzers
    private fun getUserLocation() {
        // Überprüfe erneut, ob die Berechtigung erteilt wurde, bevor der Standort abgerufen wird
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    homeViewModel.fetchPlaces(location.latitude, location.longitude)
                } else {
                    Toast.makeText(context, "Standort nicht verfügbar", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Berechtigung wurde nicht erteilt
            Toast.makeText(context, "Standortberechtigung nicht erteilt", Toast.LENGTH_SHORT).show()
        }
    }

    // Callback, um das Ergebnis der Berechtigungsanfrage zu behandeln
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Berechtigung erteilt, Standort abrufen
                getUserLocation()
            } else {
                // Berechtigung nicht erteilt
                Toast.makeText(context, "Standortberechtigung erforderlich", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
