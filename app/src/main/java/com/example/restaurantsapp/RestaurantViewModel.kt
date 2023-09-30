package com.example.restaurantsapp

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.ConnectException
import java.net.UnknownHostException

class RestaurantViewModel(private val stateHandle: SavedStateHandle) : ViewModel() {
    companion object {
        const val FAVORITES = "favorites"
    }

    private var restInterface: RestaurantApiService
    private var restaurantsDao =
        RestaurantsDb.getDaoInstance(RestaurantsApplication.getAppContext())
    val state = mutableStateOf(emptyList<Restaurant>())
    private val errorHandler =
        CoroutineExceptionHandler { _, throwable -> throwable.printStackTrace() }

    init {
        val retrofit: Retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://restaurants-compose-1fcd1-default-rtdb.firebaseio.com/")
            .build()

        restInterface = retrofit.create(RestaurantApiService::class.java)
        getRestaurants()
    }

    fun toggleFavorite(id: Int) {
        val restaurants = state.value.toMutableList()
        val itemIndex = restaurants.indexOfFirst {
            it.id == id
        }
        val item = restaurants[itemIndex]
        restaurants[itemIndex] = item.copy(isFavorite = !item.isFavorite)
        storeSelection(restaurants[itemIndex])
        state.value = restaurants
        viewModelScope.launch(errorHandler) {
            val updatedRestaurants = toggleFavoriteRestaurant(id, item.isFavorite)
            state.value = updatedRestaurants
        }
    }

    private fun storeSelection(restaurant: Restaurant) {
        val savedToggle = stateHandle
            .get<List<Int>?>(FAVORITES)
            .orEmpty().toMutableList()

        if (restaurant.isFavorite) savedToggle.add(restaurant.id)
        else savedToggle.remove(restaurant.id)
        stateHandle[FAVORITES] = savedToggle
    }

    private fun List<Restaurant>.restoreSelection(): List<Restaurant> {
        stateHandle.get<List<Int>?>(FAVORITES)?.let { selectedIds ->
            val restaurantsMap = this.associateBy {
                it.id
            }.toMutableMap()
            selectedIds.forEach { id ->
                val restaurant = restaurantsMap[id] ?: return@forEach
                restaurantsMap[id] = restaurant.copy(isFavorite = true)
            }
            return restaurantsMap.values.toList()
        }
        return this
    }

    private fun getRestaurants() {
        viewModelScope.launch(errorHandler) {
            val restaurants = getAllRestaurants()
            state.value = restaurants.restoreSelection()
        }
    }

    private suspend fun getAllRestaurants(): List<Restaurant> {
        return withContext(Dispatchers.IO) {
            try {
                refreshCache()
            } catch (e: Exception) {
                when (e) {
                    is UnknownHostException,
                    is ConnectException,
                    is HttpException -> {
                        if (restaurantsDao.getAll().isEmpty()) throw Exception(
                            "Something went wrong. " + "We have no data to show."
                        )
                    }

                    else -> {
                        throw e
                    }
                }
            }
            return@withContext restaurantsDao.getAll()
        }
    }

    private suspend fun refreshCache() {
        val remoteRestaurants = restInterface.getRestaurants()
        val favoriteRestaurants = restaurantsDao.getAllFavorited()
        restaurantsDao.addAll(remoteRestaurants)
        restaurantsDao.updateAll(
            favoriteRestaurants.map{
                PartialRestaurant(it.id, true)
            }
        )
    }

    private suspend fun toggleFavoriteRestaurant(id: Int, oldValue: Boolean) =
        withContext(Dispatchers.IO) {
            restaurantsDao.update(PartialRestaurant(id, isFavorite = !oldValue))
            restaurantsDao.getAll()
        }

}