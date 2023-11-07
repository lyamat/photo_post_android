package com.example.photo_post


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.photo_post.models.CartItem

class InstrumentInfoAdapter(private val cartItems: List<CartItem>) :
    RecyclerView.Adapter<InstrumentInfoAdapter.InstrumentViewHolder>() {

    inner class InstrumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val instrumentNameTextView: TextView = itemView.findViewById(R.id.instrNameTextView)
        val instrumentDescriptionTextView: TextView = itemView.findViewById(R.id.instrPropsTextView)
        val instrumentQuantityTextView: TextView = itemView.findViewById(R.id.instrumentQuantityTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstrumentViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_instr_info, parent, false)
        return InstrumentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: InstrumentViewHolder, position: Int) {

        val instrument = cartItems[position].instrument
        holder.instrumentNameTextView.text = instrument.instrName
        holder.instrumentDescriptionTextView.text = instrument.instrProps
        holder.instrumentQuantityTextView.text = cartItems[position].quantity.toString()
    }

    override fun getItemCount(): Int {
        return cartItems.size
    }
}

