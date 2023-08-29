package com.example.restaurantsapp

import retrofit2.http.GET

interface RestaurantApiService {
    @GET("restaurants.json")
    suspend fun getRestaurants(): List<Restaurant>
}