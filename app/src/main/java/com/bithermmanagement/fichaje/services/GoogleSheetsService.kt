package com.bithermmanagement.fichaje.services

import android.content.Context
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream

class GoogleSheetsService(private val context: Context) {
    
    private val applicationName = "BithermManagement"
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()
    
    lateinit var sheetsService: Sheets
    
    init {
        initializeSheetsService()
    }
    
    private fun initializeSheetsService() {
        try {
            // Cargar credenciales desde assets
            val inputStream = context.assets.open("credentials_default.json")
            val credential = GoogleCredential.fromStream(inputStream)
                .createScoped(listOf(SheetsScopes.SPREADSHEETS))
            
            sheetsService = Sheets.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName(applicationName)
                .build()
                
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun appendValues(spreadsheetId: String, range: String, values: List<List<Any>>) {
        withContext(Dispatchers.IO) {
            try {
                val valueRange = ValueRange().setValues(values)
                sheetsService.spreadsheets().values()
                    .append(spreadsheetId, range, valueRange)
                    .setValueInputOption("RAW")
                    .execute()
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }
    
    suspend fun getValues(spreadsheetId: String, range: String): List<List<Any>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute()
                response.getValues() ?: emptyList<List<Any>>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    suspend fun updateValues(spreadsheetId: String, range: String, values: List<List<Any>>) {
        withContext(Dispatchers.IO) {
            try {
                val valueRange = ValueRange().setValues(values)
                sheetsService.spreadsheets().values()
                    .update(spreadsheetId, range, valueRange)
                    .setValueInputOption("RAW")
                    .execute()
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }
} 