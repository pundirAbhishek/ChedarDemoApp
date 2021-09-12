package com.example.chedardemoapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.chedardemoapp.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseError

import com.google.firebase.database.DataSnapshot

import com.google.firebase.database.ValueEventListener


const val PERMISSION_REQUEST_SMS_REQUEST_CODE = 100
const val PERMISSION_FINE_LOCATION_REQUEST_CODE = 101

const val SMS_PERMISSION_TAG = "android.permission.RECEIVE_SMS"
const val LOCATION_PERMISSION_TAG = "android.permission.ACCESS_FINE_LOCATION"

class MainActivity : AppCompatActivity(), MessageListener {
    private lateinit var binding: ActivityMainBinding

    // The Fused Location Provider provides access to location APIs.
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(applicationContext)
    }

    // Allows class to cancel the location request if it exits the activity.
    // Typically, you use one cancellation source per lifecycle.
    private var cancellationTokenSource = CancellationTokenSource()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        MessageReceiver.bindListener(this)
        updateUI()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setupListView(list: ArrayList<Data>) {
        binding.recyclerView.visibility = VISIBLE
        binding.titleContainer.visibility = VISIBLE
        binding.errorContainer.visibility = GONE

        binding.recyclerView.removeAllViews()

        val adapter = Adapter(list)
        binding.recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    fun setupErrorView() {
        binding.recyclerView.visibility = GONE
        binding.titleContainer.visibility = GONE
        binding.errorContainer.visibility = VISIBLE
    }

    fun updateUI() {
        val reference: DatabaseReference = FirebaseDatabase.getInstance().reference.child("Data")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val list = ArrayList<Data>()
                dataSnapshot.children.forEach { snapshot ->
                    val data: Data? = snapshot.getValue(Data::class.java)
                    if (data != null) {
                        list.add(data)
                    }
                }
                if (list.isEmpty()) {
                    setupErrorView()
                } else {
                    list.reverse()
                    setupListView(list)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                setupErrorView()
                println("The read failed: " + databaseError.code)
            }
        })
    }

    override fun onStop() {
        super.onStop()
        // Cancels location request (if in Flight Mode).
        cancellationTokenSource.cancel()
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            val smsPermission = permissions[SMS_PERMISSION_TAG] ?: false
            val fineLocationPermission = permissions[LOCATION_PERMISSION_TAG] ?: false

            if (!fineLocationPermission) {
                requestPermissionWithRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    PERMISSION_FINE_LOCATION_REQUEST_CODE,
                    fineLocationRationalSnackbar
                )
            }

            if (!smsPermission) {
                requestPermissionWithRationale(
                    Manifest.permission.RECEIVE_SMS,
                    PERMISSION_REQUEST_SMS_REQUEST_CODE,
                    smsRationalSnackbar
                )
            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionResult()")

        when {
            grantResults.isEmpty() ->
                // If user interaction was interrupted, the permission request
                // is cancelled and you receive an empty array.
                Log.d(TAG, "User interaction was cancelled.")

            grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                Snackbar.make(
                    binding.container,
                    R.string.permission_approved_explanation,
                    Snackbar.LENGTH_LONG
                )
                    .show()

            else -> {
                Snackbar.make(
                    binding.container,
                    R.string.permission_denied_explanation,
                    Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.settings) {
                        // Build intent that displays the App settings screen.
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID,
                            null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                    .show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun messageReceived(messageData: MessageModel) {

        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        ) {
            // Returns a single current location fix on the device. Unlike getLastLocation() that
            // returns a cached location, this method could cause active location computation on the
            // device. A single fresh location will be returned if the device location can be
            // determined
            val currentLocationTask: Task<Location> = fusedLocationClient.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )
            currentLocationTask.addOnCompleteListener { task: Task<Location> ->
                if (task.isSuccessful && task.result != null) {
                    val location: Location = task.result
                    val locationData =
                        LocationModel(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    val data = Data(message = messageData, location = locationData)
                    sendData(data)
                } else {
                    val exception = task.exception
                }
            }
        } else {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun sendData(data: Data) {
        val reference: DatabaseReference = FirebaseDatabase.getInstance().reference.child("Data")
        reference.push().setValue(data)
    }

    // If the user denied a previous permission request, but didn't check "Don't ask again", this
    // Snackbar provides an explanation for why user should approve, i.e., the additional rationale.
    private val fineLocationRationalSnackbar by lazy {
        Snackbar.make(
            binding.container,
            R.string.permission_rationale,
            Snackbar.LENGTH_LONG
        ).setAction(R.string.ok) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_FINE_LOCATION_REQUEST_CODE
            )
        }
    }

    private val smsRationalSnackbar by lazy {
        Snackbar.make(
            binding.container,
            R.string.permission_rationale,
            Snackbar.LENGTH_LONG
        ).setAction(R.string.ok) {
            requestPermissions(
                arrayOf(Manifest.permission.RECEIVE_SMS),
                PERMISSION_REQUEST_SMS_REQUEST_CODE
            )
        }
    }

}