package com.example.chedardemoapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage


class MessageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent) {
        val data = intent.extras
        val pdus = data!!["pdus"] as Array<*>?
        for (i in pdus!!.indices) {
            val smsMessage = SmsMessage.createFromPdu(
                pdus[i] as ByteArray
            )
            //Pass on the text to our listener.
            val messageData = MessageModel(messageText = smsMessage.messageBody, contactNumber = smsMessage.originatingAddress ?: "")

            mListener?.messageReceived(messageData)
        }
    }

    companion object{
        private var mListener: MessageListener? = null

        fun bindListener(listener: MessageListener?) {
            mListener = listener
        }
    }

}