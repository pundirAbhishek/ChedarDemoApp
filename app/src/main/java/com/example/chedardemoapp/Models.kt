package com.example.chedardemoapp

data class Data(val message : MessageModel, val location : LocationModel)

data class LocationModel (val latitude : Double, val longitude : Double)

data class MessageModel(val messageText : String, val contactNumber : String,)