package com.example.uberbiketoronto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.uberbiketoronto.databinding.FragmentBikeDetailsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore

class BikeDetailsFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentBikeDetailsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_BIKE_NAME = "bikeName"
        private const val ARG_BIKE_ADDRESS = "bikeAddress"

        fun newInstance(bikeName: String, bikeAddress: String): BikeDetailsFragment {
            val fragment = BikeDetailsFragment()
            val args = Bundle().apply {
                putString(ARG_BIKE_NAME, bikeName)
                putString(ARG_BIKE_ADDRESS, bikeAddress)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBikeDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bikeName = arguments?.getString(ARG_BIKE_NAME) ?: "Unknown Bike"
        val bikeAddress = arguments?.getString(ARG_BIKE_ADDRESS) ?: "Unknown Address"

        binding.tvBikeName.text = bikeName
        binding.tvBikeAddress.text = bikeAddress
        binding.btnClose.setOnClickListener {
            if (bikeName != null) {
                markBikeAsReturned(bikeName)
            } else {
                dismiss()
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun markBikeAsReturned(bikeName: String) {
        val firestore = FirebaseFirestore.getInstance()

        // Assuming the bikeName uniquely identifies the bike in your Firestore
        firestore.collection("bikes")
            .whereEqualTo("bikeName", bikeName)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showToast("Bike not found.")
                    dismiss()
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val bikeId = document.id
                    firestore.collection("bikes").document(bikeId)
                        .update("returned", true)
                        .addOnSuccessListener {
                           showToast("Bike marked as returned.")
                            dismiss()
                        }
                        .addOnFailureListener { exception ->
                           showToast("Failed to update bike: ${exception.message}")
                        }
                }
            }
            .addOnFailureListener { exception ->
                showToast("Failed to fetch bike: ${exception.message}")
            }
    }
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
