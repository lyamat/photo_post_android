package com.example.photo_post

import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.photo_post.models.Cart
import com.example.photo_post.models.Instrument

class CartAdapter(private val cart: Cart, private val viewModel: SharedViewModel) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val instrumentNameTextView = itemView.findViewById<TextView>(R.id.instrumentNameTextView)
        val instrumentPropertiesTextView = itemView.findViewById<TextView>(R.id.instrumentPropertiesTextView)
        val deleteInstrumentFromCartButton = itemView.findViewById<ImageView>(R.id.deleteInstrumentFromCartButton)
        val instrumentQuantityTextView = itemView.findViewById<TextView>(R.id.instrumentQuantityTextView)

        val increaseInstrumentButton = itemView.findViewById<ImageView>(R.id.increaseInstrumentButton)
        val decreaseInstrumentButton = itemView.findViewById<ImageView>(R.id.decreaseInstrumentButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart_instrument, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cartItem = cart.cartItems[position]
        val instrument = cartItem.instrument
        holder.instrumentNameTextView.text = instrument.instrumentName
        holder.instrumentPropertiesTextView.text = instrument.instrumentProperties.joinToString(", ")
        holder.instrumentQuantityTextView.text = cartItem.quantity.toString()

        holder.deleteInstrumentFromCartButton.setOnClickListener {
            cart.cartItems.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, cart.cartItems.size)
        }

        holder.increaseInstrumentButton.setOnClickListener {
            if (cartItem.quantity < 999) {
                cartItem.quantity++
                holder.instrumentQuantityTextView.text = cartItem.quantity.toString()
            }
        }

        holder.decreaseInstrumentButton.setOnClickListener {
            if (cartItem.quantity > 1) {
                cartItem.quantity--
                holder.instrumentQuantityTextView.text = cartItem.quantity.toString()
            }
        }

        holder.instrumentQuantityTextView.setOnClickListener {
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

            builder.show()
        }

    }

    override fun getItemCount() = cart.cartItems.size

}
