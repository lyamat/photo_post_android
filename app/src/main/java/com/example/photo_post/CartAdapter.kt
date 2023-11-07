package com.example.photo_post

import android.app.Activity
import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.photo_post.models.Cart
import com.example.photo_post.server.NetworkHelper

class CartAdapter(private val viewModel: SharedViewModel): RecyclerView.Adapter<CartAdapter.CartAdapterViewHolder>() {
    class CartAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cartNameTextView = itemView.findViewById<TextView>(R.id.cartNameTextView)
        val editCartButton = itemView.findViewById<TextView>(R.id.editCartButton)
        val sendCartButton = itemView.findViewById<TextView>(R.id.sendCartButton)
        val childRecyclerView = itemView.findViewById<RecyclerView>(R.id.childRecyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartAdapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartAdapterViewHolder(view)
    }

    override fun getItemCount(): Int {
        return if (viewModel.cartListFromServer.isEmpty()) 1
        else viewModel.cartListFromServer.size
    }

    override fun onBindViewHolder(holder: CartAdapterViewHolder, position: Int) {
        val cart: Cart
        if (viewModel.cartListFromServer.isEmpty())
        {
            holder.sendCartButton.visibility = View.VISIBLE
            holder.editCartButton.visibility = View.INVISIBLE
            cart = viewModel.currentCart
        }
        else cart = viewModel.cartListFromServer[position]
        holder.cartNameTextView.text = cart.cartName

        holder.childRecyclerView.setHasFixedSize(false)
        holder.childRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)

        if (viewModel.cartListFromServer.isNotEmpty()) {
            val adapter = InstrumentInfoAdapter(cart.cartItems)
            holder.childRecyclerView.adapter = adapter
        }
        else {
            val adapter = InstrInCartAdapter(cart, viewModel, holder)
            holder.childRecyclerView.adapter = adapter
        }

        if (viewModel.currentCart.cartItems.isEmpty())
        {
            holder.sendCartButton.visibility = View.INVISIBLE
        }

        holder.editCartButton.setOnClickListener {
            viewModel.currentCart = viewModel.cartListFromServer[position]
            viewModel.cartListFromServer.clear()
            val adapter = InstrInCartAdapter(cart, viewModel, holder)
            holder.childRecyclerView.adapter = adapter
            notifyDataSetChanged()
        }

        holder.cartNameTextView.setOnClickListener {
            if (holder.editCartButton.visibility == View.INVISIBLE) {
                val builder = AlertDialog.Builder(it.context)
                builder.setTitle("Change cart name:")

                val input = EditText(it.context)
                input.inputType = InputType.TYPE_CLASS_TEXT
                input.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(20))

                input.setText(holder.cartNameTextView.text.toString())
                input.requestFocus()

                builder.setView(input)

                builder.setPositiveButton("OK") { dialog, _ ->
                    if (input.text.isNotEmpty()) {
                        viewModel.currentCart.cartName = input.text.toString()
                        val adapter = InstrInCartAdapter(viewModel.currentCart, viewModel, holder)
                        holder.childRecyclerView.adapter = adapter
                        notifyDataSetChanged()
                    }
                    dialog.dismiss()
                }

                builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

                val dialog = builder.create()
                dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                dialog.show()

                input.postDelayed({
                    val imm =
                        it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
                }, 100)
            }
        }

        holder.sendCartButton.setOnClickListener {
            NetworkHelper(it.context).uploadCart(viewModel) { isUploaded, message ->
                (it.context as Activity).runOnUiThread {
                    Toast.makeText(holder.itemView.context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

    }



}