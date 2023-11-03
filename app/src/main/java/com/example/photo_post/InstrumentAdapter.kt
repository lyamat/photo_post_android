package com.example.photo_post

import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.photo_post.models.Instrument


class InstrumentAdapter(private val instruments: MutableList<Instrument>,
                        private val viewModel: SharedViewModel) : RecyclerView.Adapter<InstrumentAdapter.InstrumentViewHolder>() {

    class InstrumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val instrumentNameTextView = itemView.findViewById<TextView>(R.id.instrumentNameTextView)
        val instrumentPropertiesTextView = itemView.findViewById<TextView>(R.id.instrumentPropertiesTextView)
        val deleteInstrumentButton = itemView.findViewById<ImageView>(R.id.deleteInstrumentButton)
        val addInstrumentToCartButton = itemView.findViewById<ImageView>(R.id.addInstrumentToCartButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstrumentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_instrument, parent, false)
        return InstrumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: InstrumentViewHolder, position: Int) {
        val instrument = instruments[position]
        holder.instrumentNameTextView.text = instrument.instrumentName
        holder.instrumentPropertiesTextView.text = instrument.instrumentProperties.joinToString(", ")

        holder.deleteInstrumentButton.setOnClickListener {
            viewModel.instruments.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, viewModel.instruments.size)
        }

        holder.addInstrumentToCartButton.setOnClickListener {
            val builder = AlertDialog.Builder(it.context)
            builder.setTitle("Input quantity:")

            val input = EditText(it.context)
            input.inputType = InputType.TYPE_CLASS_NUMBER
            input.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(3))
            builder.setView(input)

            builder.setPositiveButton("OK") { dialog, _ ->
                if (input.text.isNotEmpty()) {
                    val quantity = input.text.toString().toInt()
                    if (quantity in 1..999) {
                        val existingItem =
                            viewModel.cart.cartItems.find { it.instrument == instrument }
                        if (existingItem != null) {
                            existingItem.quantity += quantity
                        } else {
                            viewModel.addToCart(instrument, quantity)
                        }
                        notifyDataSetChanged()
                    }
                    dialog.dismiss()
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

            builder.show()
        }

    }

    override fun getItemCount() = instruments.size

    fun addInstrument(instrument: Instrument) {
        viewModel.instruments.add(instrument)
        notifyItemInserted(viewModel.instruments.size - 1)
        notifyDataSetChanged()
    }

}
