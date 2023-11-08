package com.example.photo_post

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.photo_post.server.NetworkHelper

class CartFragment : Fragment() {

    private lateinit var instrInCartRecyclerView: RecyclerView
    private lateinit var viewModel: SharedViewModel
    private lateinit var instrInCartAdapter: CartAdapter
    private lateinit var getAllCartsButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_cart, container, false)

        instrInCartRecyclerView = view.findViewById(R.id.instrInCartRecyclerView)
        instrInCartRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        instrInCartAdapter = CartAdapter(viewModel)
        instrInCartRecyclerView.adapter = instrInCartAdapter

        getAllCartsButton = view.findViewById(R.id.getAllCartsButton)

        getAllCartsButton.setOnClickListener {
            getAllCartsButton.isEnabled = false
            NetworkHelper(it.context).getUserCarts() { carts, message ->
                activity?.runOnUiThread {
                    if (carts.isNotEmpty()) {
                        viewModel.cartListFromServer = carts
                        val adapter = CartAdapter(viewModel)
                        instrInCartRecyclerView.adapter = adapter

                        viewModel.cartListFromServerLiveData.value = viewModel.cartListFromServer
                    } else {
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireActivity(),
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    getAllCartsButton.isEnabled = true
                }

            }
        }

        viewModel.cartListFromServerLiveData.observe(viewLifecycleOwner, Observer { cartList ->
            if (cartList.isNotEmpty()) {
                getAllCartsButton.visibility = View.GONE
            } else {
                getAllCartsButton.visibility = View.VISIBLE
            }
        })



        return view
    }

}
