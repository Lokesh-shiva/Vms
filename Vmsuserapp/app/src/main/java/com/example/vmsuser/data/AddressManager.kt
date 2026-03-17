package com.example.vmsuser.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.vmsuser.models.Address
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AddressManager(private val context: Context) {

    companion object {
        private val KEY_NAME = stringPreferencesKey("address_name")
        private val KEY_PHONE = stringPreferencesKey("address_phone")
        private val KEY_ADDRESS = stringPreferencesKey("address_line")
        private val KEY_PINCODE = stringPreferencesKey("address_pincode")
    }

    val addressFlow: Flow<Address> = context.dataStore.data.map { preferences ->
        Address(
            name = preferences[KEY_NAME] ?: "",
            phone = preferences[KEY_PHONE] ?: "",
            address = preferences[KEY_ADDRESS] ?: "",
            pincode = preferences[KEY_PINCODE] ?: ""
        )
    }

    suspend fun saveAddress(address: Address) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NAME] = address.name
            preferences[KEY_PHONE] = address.phone
            preferences[KEY_ADDRESS] = address.address
            preferences[KEY_PINCODE] = address.pincode
        }
    }

    suspend fun clearAddress() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_NAME)
            preferences.remove(KEY_PHONE)
            preferences.remove(KEY_ADDRESS)
            preferences.remove(KEY_PINCODE)
        }
    }
}
