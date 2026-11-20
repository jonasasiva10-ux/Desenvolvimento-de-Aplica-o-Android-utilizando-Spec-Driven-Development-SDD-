package com.example.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.domain.model.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

interface ConnectivityMonitor {
    fun isOnline(): Flow<Boolean>
    fun isCurrentlyConnected(): Boolean
}

class NetworkConnectivityObserver(
    context: Context,
) : ConnectivityMonitor {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun isCurrentlyConnected(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun isOnline(): Flow<Boolean> = callbackFlow {
        trySend(isCurrentlyConnected())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(isCurrentlyConnected())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                val hasInternet =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(hasInternet)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        runCatching {
            connectivityManager.registerNetworkCallback(request, callback)
        }

        awaitClose {
            runCatching {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
    }.distinctUntilChanged()
}

suspend fun <T> safeApiCall(block: suspend () -> T): Pair<T?, AppError?> {
    return try {
        block() to null
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (e: SocketTimeoutException) {
        null to AppError.Timeout
    } catch (e: UnknownHostException) {
        null to AppError.NetworkUnavailable
    } catch (e: HttpException) {
        val error = when (e.code()) {
            404 -> AppError.NotFound
            else -> AppError.ServerError(e.code(), e.message())
        }
        null to error
    } catch (e: IOException) {
        null to AppError.NetworkUnavailable
    } catch (e: Exception) {
        null to AppError.Unknown(e.localizedMessage)
    }
}
