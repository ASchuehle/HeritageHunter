package com.thepublichistorian.heritagehunter.ui.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.thepublichistorian.heritagehunter.databinding.FragmentAchievementsBinding

class AchievementsFragment : Fragment() {

    private lateinit var binding: FragmentAchievementsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAchievementsBinding.inflate(inflater, container, false)

        // Lade die Level- und Punkteinformationen des Nutzers
        loadUserLevelAndPoints()

        return binding.root
    }

    private fun loadUserLevelAndPoints() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val uid = currentUser.uid
            val userRef = db.collection("users").document(uid)

            userRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentLevel = document.getLong("level")?.toInt() ?: 1
                    val currentPoints = document.getLong("points") ?: 0

                    val pointsForNextLevel = calculatePointsForNextLevel(currentLevel)

                    // Zeige das Level und die Punkte an
                    binding.textViewLevel.text = "Level $currentLevel"
                    binding.textViewPoints.text = "$currentPoints/$pointsForNextLevel Punkte"

                    // Setze den Fortschrittsbalken
                    val progress = ((currentPoints.toDouble() / pointsForNextLevel.toDouble()) * 100).toInt()
                    binding.progressBarPoints.progress = progress
                }
            }
        }
    }

    // Berechnung der Punkte für das nächste Level
    private fun calculatePointsForNextLevel(currentLevel: Int): Int {
        return when (currentLevel) {
            1 -> 50
            2 -> 100
            3 -> 250
            4 -> 500
            5 -> 1000
            6 -> 2000
            7 -> 4000
            8 -> 8000
            9 -> 16000
            else -> 20000  // Für Level 10 oder höher
        }
    }
}
