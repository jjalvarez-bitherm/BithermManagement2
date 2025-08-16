package com.bithermmanagement.utils

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException

class GoogleSheetsHelper(private val context: Context) {
    private val TAG = "GoogleSheetsHelper"
    private val APPLICATION_NAME = "BithermManagement"
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    private val SCOPES = listOf(SheetsScopes.SPREADSHEETS)

    private fun getCredentials(): GoogleCredentials {
        return try {
            val inputStream: InputStream = context.assets.open("credentials_default.json")
            GoogleCredentials.fromStream(inputStream)
                .createScoped(SCOPES)
        } catch (e: IOException) {
            Log.e(TAG, "Error al cargar las credenciales", e)
            throw e
        }
    }

    private fun getSheetsService(): Sheets {
        return try {
            val credentials = getCredentials()
            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            
            Sheets.Builder(httpTransport, JSON_FACTORY, HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build()
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Error de seguridad al crear el servicio", e)
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error de IO al crear el servicio", e)
            throw e
        }
    }

    fun readSheet(spreadsheetId: String = Constants.SPREADSHEET_ID, range: String): List<List<Any>>? {
        return try {
            val service = getSheetsService()
            val response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute()
            
            response.getValues() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading sheet", e)
            null
        }
    }

    fun updateSheet(spreadsheetId: String = Constants.SPREADSHEET_ID, range: String, values: List<List<Any>>): Boolean {
        return try {
            val service = getSheetsService()
            val valueRange = ValueRange().setValues(values)
            
            service.spreadsheets().values()
                .update(spreadsheetId, range, valueRange)
                .setValueInputOption("RAW")
                .execute()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating sheet", e)
            false
        }
    }
} 