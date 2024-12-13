package com.example.uberbiketoronto.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.uberbiketoronto.data.Bike
import com.google.firebase.firestore.FirebaseFirestore

class BikeViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()

    private val _bikeLocations = MutableLiveData<List<Bike>>()
    val bikeLocations: LiveData<List<Bike>> get() = _bikeLocations

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    fun fetchBikeLocations(showReturnedOnly: Boolean?) {
        firestore.collection("bikes")
            .get()
            .addOnSuccessListener { documents ->
                val bikes = mutableListOf<Bike>()

                for (document in documents) {
                    val latitude = document.getDouble("latitude") ?: continue
                    val longitude = document.getDouble("longitude") ?: continue
                    val isReturned = document.getBoolean("returned") ?: false
                    val address = document.getString("address") ?: ""
                    val name = document.getString("bikeName") ?: "BIKE"

                    // Apply filter logic
                    when (showReturnedOnly) {
                        true -> if (!isReturned) continue
                        false -> if (isReturned) continue
                        else->{}
                    }

                    bikes.add(
                        Bike(
                        latitude=latitude,
                        longitude = longitude,
                        isReturned = isReturned,
                        address = address,
                        bikeName = name)
                    )
                }

                _bikeLocations.postValue(bikes)
            }
            .addOnFailureListener { exception ->
                _errorMessage.postValue("Failed to load bike locations: ${exception.message}")
            }
    }

    fun startRealTimeListener(showReturnedOnly: Boolean?) {
        firestore.collection("bikes")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    _errorMessage.postValue("Failed to listen for bike updates: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    _errorMessage.postValue("No bike locations found.")
                    return@addSnapshotListener
                }

                val bikes = mutableListOf<Bike>()
                for (document in snapshots) {
                    val latitude = document.getDouble("latitude") ?: continue
                    val longitude = document.getDouble("longitude") ?: continue
                    val isReturned = document.getBoolean("returned") ?: false
                    val address = document.getString("address") ?: ""
                    val name = document.getString("bikeName") ?: "BIKE"

                    // Apply filter logic
                    when (showReturnedOnly) {
                        true -> if (!isReturned) continue
                        false -> if (isReturned) continue
                        else->{}
                    }

                    bikes.add(
                        Bike(
                        latitude=latitude,
                        longitude = longitude,
                        isReturned = isReturned,
                        address = address,
                        bikeName = name)
                    )
                }

                _bikeLocations.postValue(bikes)
            }
    }
}
