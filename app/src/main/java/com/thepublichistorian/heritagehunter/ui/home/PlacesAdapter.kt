package com.thepublichistorian.heritagehunter.ui.home

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.thepublichistorian.heritagehunter.R
import com.thepublichistorian.heritagehunter.models.Place

class PlacesAdapter(
    private val placesWithIds: List<Pair<String, Place>>,
    private val userLatitude: Double,
    private val userLongitude: Double
) : RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.place_name)
        val counterTextView: TextView = itemView.findViewById(R.id.place_counter)
        val distanceTextView: TextView = itemView.findViewById(R.id.place_distance)
        val iconImageView: ImageView = itemView.findViewById(R.id.place_icon)
        val targetIconImageView: ImageView = itemView.findViewById(R.id.place_target_icon) // Das neue Icon
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val (placeId, place) = placesWithIds[position]
        val context = holder.itemView.context

        // Setze den Namen und die Anzahl der Besuche
        holder.nameTextView.text = place.name
        holder.counterTextView.text = "Besuche: ${place.counter}"

        // Berechne die Entfernung zwischen dem Nutzer und dem Ort
        val results = FloatArray(1)
        Location.distanceBetween(userLatitude, userLongitude, place.latitude, place.longitude, results)
        val distanceInMeters = results[0]
        holder.distanceTextView.text = String.format("Entfernung: %.2f km", distanceInMeters / 1000)

        // Kategorie prüfen und das entsprechende Icon setzen
        when (place.category) {
            "Museum" -> holder.iconImageView.setImageResource(R.drawable.ic_museum)
            "Denkmal" -> holder.iconImageView.setImageResource(R.drawable.ic_memorial)
            "Gedenkstätte" -> holder.iconImageView.setImageResource(R.drawable.ic_monument)
            "Historische Stätte" -> holder.iconImageView.setImageResource(R.drawable.ic_historicalsite)
            "Schloss/ Burg" -> holder.iconImageView.setImageResource(R.drawable.ic_castle)
            "Industriedenkmal" -> holder.iconImageView.setImageResource(R.drawable.ic_industrialmonument)
            "Religiöse Stätte" -> holder.iconImageView.setImageResource(R.drawable.ic_religioussite)
            else -> holder.iconImageView.setImageResource(R.drawable.ic_default)
        }

        // Überprüfe, ob der Nutzer eingeloggt ist und der Ort im Umkreis von 50 Metern liegt
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val placeRef = db.collection("places").document(placeId)

            placeRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val visitors = document.get("visitors") as? List<String>
                    if (visitors != null && visitors.contains(uid)) {
                        // Nutzer hat den Ort bereits besucht
                        holder.itemView.setBackgroundColor(
                            ContextCompat.getColor(context, R.color.accent)
                        )
                        holder.targetIconImageView.visibility = View.GONE // Icon ausblenden, da bereits besucht
                    } else {
                        // Nutzer hat den Ort noch nicht besucht
                        holder.itemView.setBackgroundColor(
                            ContextCompat.getColor(context, R.color.primary)
                        )
                        if (distanceInMeters <= 50) {
                            holder.targetIconImageView.visibility = View.VISIBLE
                            holder.targetIconImageView.setOnClickListener {
                                logVisit(placeId, place, holder)
                            }
                        } else {
                            holder.targetIconImageView.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    // Methode zum Einloggen eines Besuchs
    private fun logVisit(placeId: String, place: Place, holder: PlaceViewHolder) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val placeRef = db.collection("places").document(placeId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(placeRef)
                val currentCounter = snapshot.getLong("counter") ?: 0
                val visitors = snapshot.get("visitors") as? MutableList<String> ?: mutableListOf()

                // Erhöhe den Counter und füge den Nutzer hinzu
                if (!visitors.contains(uid)) {
                    visitors.add(uid)
                    transaction.update(placeRef, "visitors", visitors)
                    transaction.update(placeRef, "counter", currentCounter + 1)
                }
            }.addOnSuccessListener {
                // Aktualisiere das UI erst, nachdem der Besuch erfolgreich in der DB eingetragen wurde
                holder.counterTextView.text = "Besuche: ${place.counter + 1}"
                holder.targetIconImageView.visibility = View.GONE

                // Hintergrundfarbe ändern, um anzuzeigen, dass der Nutzer eingeloggt ist
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.accent)
                )

                Toast.makeText(holder.itemView.context, "Besuch erfolgreich eingeloggt!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(holder.itemView.context, "Fehler beim Einloggen des Besuchs", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = placesWithIds.size
}
