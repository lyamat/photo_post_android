package com.example.photo_post

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.os.Build
import android.widget.ImageView
import android.content.Intent
import android.widget.Button
import android.widget.AdapterView

private lateinit var projectAdapter: ArrayAdapter<String>

private val REQUEST_CODE_SCANNER = 2001

/**
 * A simple [Fragment] subclass.
 * Use the [PhotoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PhotoFragment : Fragment() {
    private lateinit var projectAdapter: ArrayAdapter<String>
    private lateinit var projectSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photo, container, false)

        projectSpinner = view.findViewById(R.id.projectSpinner)
        projectAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item)
        projectSpinner.adapter = projectAdapter

        // Остальной код и обработчики событий, оставьте без изменений

        val commentEditText = view.findViewById<EditText>(R.id.commentEditText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            commentEditText.showSoftInputOnFocus = true
        }

        view.findViewById<ImageView>(R.id.button_camera).setOnClickListener {
            // dispatchTakePictureIntent() или checkCameraPermission()
        }

        view.findViewById<ImageView>(R.id.button_qr).setOnClickListener {
            val intent = Intent(requireContext(), ScannerActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SCANNER)
        }

        view.findViewById<Button>(R.id.sendToServerButton).setOnClickListener {
//            showDialog()
        }

        projectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedProject = parent.getItemAtPosition(position) as String
                // Ваша обработка выбора элемента
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PhotoFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PhotoFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }


}