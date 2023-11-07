package com.example.photo_post

import android.annotation.SuppressLint
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
import com.example.photo_post.models.Cart

class InstrInCartAdapter(private val cart: Cart, private val viewModel: SharedViewModel) : RecyclerView.Adapter<InstrInCartAdapter.InstrInCartViewHolder>() {

    class InstrInCartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val instrNameTextView = itemView.findViewById<TextView>(R.id.instrNameTextView)
        val instrPropsTextView = itemView.findViewById<TextView>(R.id.instrPropsTextView)
        val deleteInstrumentFromCartButton = itemView.findViewById<ImageView>(R.id.deleteInstrumentFromCartButton)
        val instrumentQuantityTextView = itemView.findViewById<TextView>(R.id.instrumentQuantityTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstrInCartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_instr_in_cart, parent, false)
        return InstrInCartViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: InstrInCartViewHolder, position: Int) {
        val cartItem = cart.cartItems[position]
        val instrument = cartItem.instrument
        holder.instrNameTextView.text = instrument.instrName
        holder.instrPropsTextView.text = instrument.instrProps
        holder.instrumentQuantityTextView.text = cartItem.quantity.toString()

        holder.deleteInstrumentFromCartButton.setOnClickListener {
            val builder = AlertDialog.Builder(it.context)
            builder.setTitle("Remove instrument from cart?")
            builder.setMessage("Name: ${instrument.instrName}\nProperties: ${instrument.instrProps}\nQuantity: ${cartItem.quantity}")

            builder.setPositiveButton("OK") { dialog, _ ->
                cart.cartItems.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, cart.cartItems.size)
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

            builder.show()
        }

        holder.instrumentQuantityTextView.setOnClickListener {
            val builder = AlertDialog.Builder(it.context)
            builder.setTitle("Input quantity:")

            val input = EditText(it.context)
            input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(8))

            input.setText(holder.instrumentQuantityTextView.text.toString())
            input.requestFocus()

            builder.setView(input)

            builder.setPositiveButton("OK") { dialog, _ ->
                if (input.text.isNotEmpty()) {
                    val quantity = input.text.toString().toDouble()
                    if (quantity in 0.000001..9.9999999E7) {
                        val existingItem = cart.cartItems.find { it.instrument == instrument }
                        if (existingItem != null) {
                            existingItem.quantity = quantity
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

    override fun getItemCount() = cart.cartItems.size

}
