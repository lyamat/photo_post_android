package com.example.photo_post

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.photo_post.models.Project
import com.example.photo_post.server.NetworkHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private const val TAG = "CartFragment"

class CartFragment : Fragment() {

    private lateinit var instrInCartRecyclerView: RecyclerView
    private lateinit var viewModel: SharedViewModel
    private lateinit var instrInCartAdapter: CartAdapter
    private lateinit var getAllCartsButton: Button

    private lateinit var projectAdapter: ArrayAdapter<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    @SuppressLint("ClickableViewAccessibility")
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
                        viewModel.isCurrentCartIsTemplate = false
                        viewModel.cartListFromServer = carts
                        val adapter = CartAdapter(viewModel)
                        instrInCartRecyclerView.adapter = adapter

                        viewModel.cartListFromServerLiveData.value = viewModel.cartListFromServer
                    } else {
                        toastAndLog(message)
                    }
                    getAllCartsButton.isEnabled = true
                }
            }
        }

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val gson = Gson()
        val jsonProjects = sharedPrefs.getString("projects", "")
        val projects: List<Project> = if (jsonProjects?.isNotEmpty() == true) {
            val listType = TypeToken.getParameterized(List::class.java, Project::class.java).type
            gson.fromJson(jsonProjects, listType)
        } else {
            emptyList()
        }

        val projectNames = projects.map { it.projectName }.toMutableList()

        val processNames = mutableListOf<String>()

        val cartFilterButton = view.findViewById<Button>(R.id.cartFilterButton)

        cartFilterButton.setOnClickListener {
            val projectBuilder = AlertDialog.Builder(requireContext())
            projectBuilder.setTitle("Choose project to get processes:")

            val projectAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, projectNames)
            projectBuilder.setAdapter(projectAdapter) { dialog, which ->
                val selectedProject = projectNames[which]
                NetworkHelper(it.context).getProcessesByProject(selectedProject) { processes, message ->
                    activity?.runOnUiThread {
                        if (processes.isNotEmpty()) {

                            processNames.clear()
                            processNames.addAll(processes.map { it.processName }.toMutableList())

                            val processBuilder = AlertDialog.Builder(requireContext())
                            processBuilder.setTitle("Choose process to get box template:")

                            val processAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, processNames)
                            processBuilder.setAdapter(processAdapter) { dialog, which ->
                                val selectedProcess = processNames[which]
                                NetworkHelper(it.context).getCartByProcess(selectedProcess) { carts, message ->
                                    activity?.runOnUiThread {
                                        if (carts.isNotEmpty()) {
                                            viewModel.isCurrentCartIsTemplate = true
                                            viewModel.cartListFromServer = carts
                                            val adapter = CartAdapter(viewModel)
                                            instrInCartRecyclerView.adapter = adapter

                                            viewModel.cartListFromServerLiveData.value = viewModel.cartListFromServer
                                        } else {
                                            toastAndLog(message)
                                        }
                                        getAllCartsButton.isEnabled = true
                                    }
                                }
                            }
                            processBuilder.show()
                        } else {
                            toastAndLog(message)
                        }
                    }
                }

            }
            projectBuilder.show()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        viewModel.message.observe(viewLifecycleOwner, Observer { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                viewModel.message.postValue("")
            }
        })
    }

    private fun toastAndLog(message: String) {
        viewModel.message.postValue(message)
        viewModel.setButtonEnabled(true)
        Log.e(TAG, message)
    }

}
