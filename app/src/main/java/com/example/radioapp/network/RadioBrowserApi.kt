package com.example.radioapp.network

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class ApiStation(
    @SerializedName("stationuuid") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("url_resolved") val urlResolved: String,
    @SerializedName("favicon") val favicon: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("language") val language: String?
)

interface RadioBrowserApi {

    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("country") country: String? = "India",
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true
    ): List<ApiStation>

    companion object {
        // A common entry point for radio-browser.info (they have many DNS round-robin mirrors, this is the main one)
        private const val BASE_URL = "https://de1.api.radio-browser.info/"

        fun create(): RadioBrowserApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(RadioBrowserApi::class.java)
        }
    }
}
