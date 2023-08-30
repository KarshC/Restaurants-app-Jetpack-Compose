package com.example.restaurantsapp

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RestaurantDetailsViewModel: ViewModel() {
    private var restInterface : RestaurantApiService
    val state = mutableStateOf<Restaurant?>(null)
    init {
        val retrofit: Retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://restaurants-compose-1fcd1-default-rtdb.firebaseio.com/")
            .build()

        restInterface = retrofit.create(RestaurantApiService::class.java)
        viewModelScope.launch {
            val restaurant = getRemoteRestaurant(2)
            state.value = restaurant
        }
    }

    private suspend fun getRemoteRestaurant(id: Int): Restaurant{
        return withContext(Dispatchers.IO){
            val responseMap = restInterface.getRestaurantDetails(id)
            return@withContext responseMap.values.first()
        }
    }
}