//package com.olup.notable
//
//import android.content.Context
//import androidx.datastore.core.DataStore
//import androidx.datastore.preferences.core.Preferences
//import androidx.datastore.preferences.core.intPreferencesKey
//import androidx.datastore.preferences.core.stringPreferencesKey
//import androidx.datastore.preferences.preferencesDataStore
//
//
//const val APP_DATASTORE ="app_datastore"
//private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = APP_DATASTORE)
//
//class DataStoreManager (val context: Context) {
//
//    companion object {
//        val OPENED_NOTEBOOK_ID = intPreferencesKey("OPENED_NOTEBOOK_ID")
//        val OEPENED_PAGE_ID = intPreferencesKey("OEPENED_PAGE_ID")
//        val SELECTED_PEN = stringPreferencesKey("SELECTED_PEN")
//        val PEN_CONFIG = stringPreferencesKey("PEN_CONFIG")
//    }
//
//    suspend fun saveToDataStore(merchantDetail: MerchantDetail) {
//        context.dataStore.edit {
//            it[EMAIL] = merchantDetail.emailAddress
//            it[MOBILE_NUMBER] = merchantDetail.mobileNumber
//            it[BUSINESS_NAME] = merchantDetail.businessName
//            it[BUSINESS_ADDRESS] = merchantDetail.businessAddress
//            it[BUSINESS_WEBSITE] = merchantDetail.businessWebsite
//            it[BUSINESS_CATEGORY] = merchantDetail.businessCategory
//            it[BUSINESS_LOCATION] = merchantDetail.businessLocation
//        }
//    }
//
//    fun getFromDataStore() = context.dataStore.data.map {
//        MerchantDetail(
//            emailAddress = it[EMAIL]?:"",
//            mobileNumber = it[MOBILE_NUMBER]?:"",
//            businessName = it[BUSINESS_NAME]?:""
//        )
//    }
//
//    suspend fun clearDataStore() = context.dataStore.edit {
//        it.clear()
//    }
//
//}