package com.thepublichistorian.heritagehunter.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thepublichistorian.heritagehunter.R
import com.thepublichistorian.heritagehunter.models.Place
import android.widget.ImageView

class PlacesAdapter(private val places: List<Place>) : RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]

        holder.nameTextView.text = place.name
        holder.counterTextView.text = "Besuche: ${place.counter}"

        // Kategorie prüfen und das entsprechende Icon setzen
        val context = holder.itemView.context
        when (place.category) {
            "Museum" -> holder.iconImageView.setImageResource(R.drawable.ic_museum)
            "Denkmal" -> holder.iconImageView.setImageResource(R.drawable.ic_memorial)
            "Gedenkstätte" -> holder.iconImageView.setImageResource(R.drawable.ic_monument)
            "Historische Stätte" -> holder.iconImageView.setImageResource(R.drawable.ic_historicalsite)
            "Schloss/ Burg" -> holder.iconImageView.setImageResource(R.drawable.ic_castle)
            "Industriedenkmal" -> holder.iconImageView.setImageResource(R.drawable.ic_industrialmonument)
            "Religiöse Stätte" -> holder.iconImageView.setImageResource(R.drawable.ic_religioussite)
            else -> holder.iconImageView.setImageResource(R.drawable.ic_default) // Standard-Icon
        }
    }

    override fun getItemCount(): Int = places.size

    class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.place_name)
        val counterTextView: TextView = itemView.findViewById(R.id.place_counter)
        val iconImageView: ImageView = itemView.findViewById(R.id.place_icon)
    }
}


