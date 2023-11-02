package com.example.photo_post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.photo_post.models.Instrument


class InstrumentAdapter(private val instruments: MutableList<Instrument>) : RecyclerView.Adapter<InstrumentAdapter.InstrumentViewHolder>() {

    class InstrumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val instrumentNameTextView = itemView.findViewById<TextView>(R.id.instrumentNameTextView)
        val instrumentPropertiesTextView = itemView.findViewById<TextView>(R.id.instrumentPropertiesTextView)
        val deleteButton = itemView.findViewById<ImageView>(R.id.deleteButton)
        val addButton = itemView.findViewById<ImageView>(R.id.addButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstrumentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_instrument, parent, false)
        return InstrumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: InstrumentViewHolder, position: Int) {
        val instrument = instruments[position]
        holder.instrumentNameTextView.text = instrument.instrumentName
        holder.instrumentPropertiesTextView.text = instrument.instrumentProperties.joinToString(", ")

        holder.deleteButton.setOnClickListener {
            instruments.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, instruments.size)
        }

        holder.addButton.setOnClickListener {
            // Здесь вы можете добавить код для добавления инструмента в корзину
            // Например, вы можете использовать instrumentNameTextView и instrumentPropertiesTextView
            val name = holder.instrumentNameTextView.text.toString()
            val properties = holder.instrumentPropertiesTextView.text.toString()
            addToCart(name, properties)
        }
    }

    override fun getItemCount() = instruments.size

    fun addInstrument(instrument: Instrument) {
        instruments.add(instrument)
        notifyDataSetChanged()
        notifyItemInserted(instruments.size - 1)
    }

    private fun addToCart(name: String, properties: String) {
        // Реализуйте этот метод для добавления инструмента в корзину
    }
}
