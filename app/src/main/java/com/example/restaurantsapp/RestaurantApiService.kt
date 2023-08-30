package com.example.restaurantsapp

import retrofit2.http.GET
import retrofit2.http.Query

interface RestaurantApiService {
    @GET("restaurants.json")
    suspend fun getRestaurants(): List<Restaurant>

    @GET("restaurants.json?orderBy=\"r_id\"")
    suspend fun getRestaurantDetails(
        @Query("equalTo") id: Int
    ): Map<String, Restaurant>
}