package com.example.uberbiketoronto



import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class BikeDataInputs (private val context: Context){
    private val db = FirebaseFirestore.getInstance()
    fun generateRandomBikeAndUpload() {
        val batch = db.batch() // Use batch writes for efficiency
        val bikesCollection = db.collection("bikes")

          for (i in 1..1) {
            val latitude = Random.nextDouble(43.66, 43.79)
            val longitude = Random.nextDouble(-79.51, -79.33)
            val bikeName=Random.nextInt(100,999).toString()
            // Generate random data
            val bike = Bike(
                latitude = latitude,
                longitude = longitude,
                address = getStreetAddress(latitude, longitude),
                runTime = "${Random.nextInt(1, 100)} mins",
                description = "Ride is good in Toronto",
                bikeName = "BIKE-${bikeName}"
            )
            // Add to the batch
            val bikeRef = bikesCollection.document()
            batch.set(bikeRef, bike)
        }

        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                println("Successfully added 50 bikes.")
            }
            .addOnFailureListener { e ->
                println("Error adding bikes: ${e.message}")
            }
    }

    fun deleteAllBikes() {
        val bikesCollection = db.collection("bikes")

        bikesCollection.get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (document in querySnapshot.documents) {
                    batch.delete(document.reference)
                }
                // Commit the batch
                batch.commit()
                    .addOnSuccessListener {
                        println("Successfully deleted all bikes.")
                    }
                    .addOnFailureListener { e ->
                        println("Error deleting houses: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                println("Error fetching documents: ${e.message}")
            }
    }

    private fun getStreetAddress(latitude: Double, longitude: Double): String {
        // TODO: instantiate the geocoder class
        val geocoder = Geocoder(context)
        return try {
            // Retrieve location results using Geocoder
            val searchResults:MutableList<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            // Handle case where no results are found
            if (searchResults.isNullOrEmpty()) {
                return "No address found for the given location."
            }
            // Extract address details from the result
            val foundLocation: Address = searchResults[0]
            foundLocation.getAddressLine(0) ?: "Address not available"
        } catch (ex: Exception) {
            Log.e("TESTING", "Error while getting street address", ex)
            "Error while retrieving address: ${ex.localizedMessage}"
        }
    }

}