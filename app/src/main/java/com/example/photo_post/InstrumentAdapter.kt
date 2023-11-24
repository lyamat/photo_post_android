package com.example.photo_post

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
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
        val instrUnitsTextView = itemView.findViewById<TextView>(R.id.instrUnitsTextView)
//        val instrAmountTextView = itemView.findViewById<TextView>(R.id.instrAmountTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstrumentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_instrument, parent, false)
        return InstrumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: InstrumentViewHolder, position: Int) {
        val instrument = instruments[position]
        holder.instrNameTextView.text = instrument.instrName
        holder.instrPropsTextView.text = instrument.instrProps
        holder.instrUnitsTextView.text = "${instrument.instrAmount}(${instrument.instrUnits})"
//        holder.instrAmountTextView.text = "(${instrument.instrAmount})"

        if (!instrument.isAddToCartEnabled) {
            holder.addInstrumentToCartButton.visibility = View.GONE
        }

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

        holder.addInstrumentToCartButton.setOnClickListener { it ->
            val builder = AlertDialog.Builder(it.context)
            builder.setTitle("Input amount: (${instrument.instrUnits})")

            val input = EditText(it.context)
            input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(5))

            val existingItem =
                viewModel.currentCart.cartItems.find { it.instrument == instrument }
            if (existingItem != null) {
                input.setText((instrument.instrAmount - existingItem.amount_in_cart).toString())
            }
            else {
                input.setText(instrument.instrAmount.toString())
            }
            input.requestFocus()

            input.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    val amount = s.toString().toDoubleOrNull()
                    val existingItem =
                        viewModel.currentCart.cartItems.find { it.instrument == instrument }
                    if (amount != null) {
                        if(amount > instrument.instrAmount){
                        input.error = "The entered value cannot be greater than ${instrument.instrAmount}"
                        }
                        if (existingItem != null) {
                            if (existingItem.amount_in_cart + amount > instrument.instrAmount) {
                                input.error = "Cart already have ${existingItem.amount_in_cart}\n"+
                                        "${existingItem.amount_in_cart} + ${amount} > ${instrument.instrAmount}"
                            }
                        }
                    }

                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                    // Не используется, но должен быть переопределен
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    // Не используется, но должен быть переопределен
                }
            })


            builder.setView(input)

            builder.setPositiveButton("OK") { dialog, _ ->
                if (input.text.isNotEmpty()) {
                    val amount = input.text.toString().toDouble()
                    if (amount in 0.001..instrument.instrAmount) {
                        val existingItem =
                            viewModel.currentCart.cartItems.find { it.instrument == instrument }
                        if (existingItem != null) {
//                            val newAmount = instrument.instrAmount - amount
//                            holder.instrAmountTextView.text = "(newAmount)"
//                            instrument.instrAmount -= newAmount

                            existingItem.amount_in_cart += amount

                        } else {
                            viewModel.addToCart(instrument, amount)
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
