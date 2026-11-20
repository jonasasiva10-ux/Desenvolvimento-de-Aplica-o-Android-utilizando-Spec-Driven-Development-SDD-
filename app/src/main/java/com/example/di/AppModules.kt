package com.example.di

import androidx.room.Room
import com.example.BuildConfig
import com.example.data.local.PatchPulseDatabase
import com.example.data.network.ConnectivityMonitor
import com.example.data.network.NetworkConnectivityObserver
import com.example.data.remote.api.OpenDotaApi
import com.example.data.remote.api.SteamStoreApi
import com.example.data.remote.api.SteamWebApi
import com.example.data.repository.GameRepositoryImpl
import com.example.data.repository.PatchRepositoryImpl
import com.example.data.repository.PlayerRepositoryImpl
import com.example.domain.repository.GameRepository
import com.example.domain.repository.PatchRepository
import com.example.domain.repository.PlayerRepository
import com.example.domain.usecase.CalculatePlayerMetricsUseCase
import com.example.domain.usecase.FormatRankTierUseCase
import com.example.domain.usecase.GetFavoriteGamesUseCase
import com.example.domain.usecase.GetFavoritePatchesUseCase
import com.example.domain.usecase.GetGameDetailUseCase
import com.example.domain.usecase.GetPatchNotesUseCase
import com.example.domain.usecase.GetPlayerStatsUseCase
import com.example.domain.usecase.GetProPlayersUseCase
import com.example.domain.usecase.GetTrendingGamesUseCase
import com.example.domain.usecase.ObserveSyncMetadataUseCase
import com.example.domain.usecase.RefreshPatchNotesUseCase
import com.example.domain.usecase.RefreshProPlayersUseCase
import com.example.domain.usecase.RefreshTrendingGamesUseCase
import com.example.domain.usecase.ToggleFavoriteGameUseCase
import com.example.presentation.favorites.FavoritesViewModel
import com.example.presentation.gamedetail.GameDetailViewModel
import com.example.presentation.patches.PatchesViewModel
import com.example.presentation.playerdetail.PlayerDetailViewModel
import com.example.presentation.players.PlayersViewModel
import com.example.presentation.trends.TrendsViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

private const val STEAM_WEB_BASE_URL = "https://api.steampowered.com/"
private const val STEAM_STORE_BASE_URL = "https://store.steampowered.com/"
private const val OPENDOTA_BASE_URL = "https://api.opendota.com/api/"

private val QUALIFIER_STEAM_WEB = named("steam_web")
private val QUALIFIER_STEAM_STORE = named("steam_store")
private val QUALIFIER_OPENDOTA = named("opendota")

val networkModule = module {
    single {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    single {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", "SeuJogo-Android/1.0")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    single(QUALIFIER_STEAM_WEB) {
        Retrofit.Builder()
            .baseUrl(STEAM_WEB_BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }

    single(QUALIFIER_STEAM_STORE) {
        Retrofit.Builder()
            .baseUrl(STEAM_STORE_BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }

    single(QUALIFIER_OPENDOTA) {
        Retrofit.Builder()
            .baseUrl(OPENDOTA_BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }

    single<SteamWebApi> { get<Retrofit>(QUALIFIER_STEAM_WEB).create(SteamWebApi::class.java) }
    single<SteamStoreApi> { get<Retrofit>(QUALIFIER_STEAM_STORE).create(SteamStoreApi::class.java) }
    single<OpenDotaApi> { get<Retrofit>(QUALIFIER_OPENDOTA).create(OpenDotaApi::class.java) }
    single<ConnectivityMonitor> { NetworkConnectivityObserver(androidContext()) }
}

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            PatchPulseDatabase::class.java,
            PatchPulseDatabase.DATABASE_NAME,
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<PatchPulseDatabase>().gameDao() }
    single { get<PatchPulseDatabase>().patchNoteDao() }
    single { get<PatchPulseDatabase>().playerDao() }
    single { get<PatchPulseDatabase>().syncStatusDao() }
}

val repositoryModule = module {
    single<GameRepository> {
        GameRepositoryImpl(
            database = get(),
            gameDao = get(),
            syncStatusDao = get(),
            steamWebApi = get(),
            steamStoreApi = get(),
            connectivityMonitor = get(),
        )
    }
    single<PatchRepository> {
        PatchRepositoryImpl(
            patchNoteDao = get(),
            gameDao = get(),
            syncStatusDao = get(),
            steamWebApi = get(),
        )
    }
    single<PlayerRepository> {
        PlayerRepositoryImpl(
            database = get(),
            playerDao = get(),
            syncStatusDao = get(),
            openDotaApi = get(),
            formatRankTier = get(),
            calculateMetrics = get(),
        )
    }
}

val useCaseModule = module {
    factory { FormatRankTierUseCase() }
    factory { CalculatePlayerMetricsUseCase() }
    factory { GetTrendingGamesUseCase(get()) }
    factory { RefreshTrendingGamesUseCase(get()) }
    factory { GetGameDetailUseCase(get()) }
    factory { ToggleFavoriteGameUseCase(get()) }
    factory { GetFavoriteGamesUseCase(get()) }
    factory { ObserveSyncMetadataUseCase(get()) }
    factory { GetPatchNotesUseCase(get()) }
    factory { RefreshPatchNotesUseCase(get()) }
    factory { GetFavoritePatchesUseCase(get()) }
    factory { GetProPlayersUseCase(get()) }
    factory { RefreshProPlayersUseCase(get()) }
    factory { GetPlayerStatsUseCase(get()) }
}

val viewModelModule = module {
    viewModel {
        TrendsViewModel(
            getTrendingGames = get(),
            refreshTrendingGames = get(),
            toggleFavoriteGame = get(),
            observeSyncMetadata = get(),
        )
    }
    viewModel {
        PatchesViewModel(
            getPatchNotes = get(),
            refreshPatchNotes = get(),
            getTrendingGames = get(),
            observeSyncMetadata = get(),
        )
    }
    viewModel {
        PlayersViewModel(
            getProPlayers = get(),
            refreshProPlayers = get(),
            observeSyncMetadata = get(),
        )
    }
    viewModel {
        FavoritesViewModel(
            getFavoriteGames = get(),
            getFavoritePatches = get(),
            toggleFavoriteGame = get(),
            observeSyncMetadata = get(),
        )
    }
    viewModel { (appId: Int) ->
        GameDetailViewModel(
            appId = appId,
            getGameDetail = get(),
            getPatchNotes = get(),
            refreshPatchNotes = get(),
            toggleFavoriteGame = get(),
            observeSyncMetadata = get(),
        )
    }
    viewModel { (accountId: Long) ->
        PlayerDetailViewModel(
            accountId = accountId,
            getPlayerStats = get(),
            observeSyncMetadata = get(),
        )
    }
}

val patchPulseModules = listOf(
    networkModule,
    databaseModule,
    repositoryModule,
    useCaseModule,
    viewModelModule,
)
