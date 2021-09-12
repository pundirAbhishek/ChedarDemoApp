package com.example.chedardemoapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class Adapter(private val dataSet: ArrayList<Data>) :
    RecyclerView.Adapter<Adapter.ViewHolder>() {


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.list_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.bind(dataSet[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val smsTextView: TextView = view.findViewById(R.id.sms_text_container)
        private val contactTextView: TextView = view.findViewById(R.id.contact_container)
        private val locationTextView: TextView = view.findViewById(R.id.location_container)

        fun bind(data: Data) {
            smsTextView.text = "Message : " + data.message?.messageText ?: ""
            contactTextView.text = "Contact : " + data.message?.contactNumber ?: ""
            locationTextView.text = data.location.toString()
        }
    }

}