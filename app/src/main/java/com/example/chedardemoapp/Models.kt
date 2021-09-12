package com.example.chedardemoapp

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Data(val message: MessageModel? = null, val location: LocationModel? = null)

@IgnoreExtraProperties
data class LocationModel(val latitude: Double? = null, val longitude: Double? = null) {

    override fun toString(): String {
        val latitude = latitude ?: 0
        val longitude = longitude ?: 0
        return "Latitude : $latitude\nLongitude : $longitude"
    }
}

@IgnoreExtraProperties
data class MessageModel(val messageText: String? = null, val contactNumber: String? = null)