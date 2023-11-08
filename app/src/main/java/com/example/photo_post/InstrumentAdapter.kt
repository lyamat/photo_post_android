package com.example.photo_post

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.photo_post.models.Instrument


class InstrumentAdapter(private val instruments: MutableList<Instrument>,
                        private val viewModel: SharedViewModel) : RecyclerView.Adapter<InstrumentAdapter.InstrumentViewHolder>() {

    class InstrumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val instrNameTextView = itemView.findViewById<TextView>(R.id.instrNameTextView)
        val instrPropsTextView = itemView.findViewById<TextView>(R.id.instrPropsTextView)
        val deleteInstrumentButton = itemView.findViewById<ImageView>(R.id.deleteInstrumentButton)
        val addInstrumentToCartButton = itemView.findViewById<ImageView>(R.id.addInstrumentToCartButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstrumentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_instrument, parent, false)
        return InstrumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: InstrumentViewHolder, position: Int) {
        val instrument = instruments[position]
        holder.instrNameTextView.text = instrument.instrName
        holder.instrPropsTextView.text = instrument.instrProps

        holder.deleteInstrumentButton.setOnClickListener {
            val builder = AlertDialog.Builder(it.context)
            builder.setTitle("Remove instrument?")
            builder.setMessage("Name: ${instrument.instrName}\nProperties: ${instrument.instrProps}")

            builder.setPositiveButton("OK") { dialog, _ ->
                viewModel.instruments.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, viewModel.instruments.size)
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

            builder.show()
        }

        holder.addInstrumentToCartButton.setOnClickListener {
            val builder = AlertDialog.Builder(it.context)
            builder.setTitle("Input quantity:")

            val input = EditText(it.context)
            input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(5))

            input.setText("1.0")
            input.requestFocus()

            builder.setView(input)


            builder.setPositiveButton("OK") { dialog, _ ->
                if (input.text.isNotEmpty()) {
                    val quantity = input.text.toString().toDouble()
                    if (quantity in 0.001..999.0) {
                        val existingItem =
                            viewModel.currentCart.cartItems.find { it.instrument == instrument }
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

            val dialog = builder.create()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            dialog.show()

            input.postDelayed({
                val imm = it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }, 100)
        }

    }

    override fun getItemCount() = instruments.size

    fun addInstrument(instrument: Instrument) {
        viewModel.instruments.add(instrument)
        notifyItemInserted(viewModel.instruments.size - 1)
        notifyDataSetChanged()
    }

}
