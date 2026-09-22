package com.tvsencilla.iptv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tvsencilla.iptv.BuildConfig
import com.tvsencilla.iptv.domain.model.PlayableKind
import com.tvsencilla.iptv.ui.epg.EpgGridScreen
import com.tvsencilla.iptv.ui.favorites.ReorderFavoritesScreen
import com.tvsencilla.iptv.ui.home.HomeScreen
import com.tvsencilla.iptv.ui.live.LiveTvScreen
import com.tvsencilla.iptv.ui.movies.MovieDetailScreen
import com.tvsencilla.iptv.ui.movies.MoviesScreen
import com.tvsencilla.iptv.ui.player.LivePlayerScreen
import com.tvsencilla.iptv.ui.player.VodPlayerScreen
import com.tvsencilla.iptv.ui.remote.RemoteKeyBus
import com.tvsencilla.iptv.ui.search.SearchScreen
import com.tvsencilla.iptv.ui.series.SeriesDetailScreen
import com.tvsencilla.iptv.ui.series.SeriesScreen
import com.tvsencilla.iptv.ui.settings.ParentalScreen
import com.tvsencilla.iptv.ui.settings.SettingsScreen
import com.tvsencilla.iptv.ui.setup.SetupScreen

@Composable
fun AppNavHost(
    remoteKeyBus: RemoteKeyBus,
    startOnSetup: Boolean,
    onExitApp: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (startOnSetup) Routes.SETUP else Routes.HOME,
    ) {
        composable(Routes.SETUP) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onLiveTv = { navController.navigate(Routes.LIVE) },
                onMovies = { navController.navigate(Routes.MOVIES) },
                onSeries = { navController.navigate(Routes.SERIES) },
                onSearch = { navController.navigate(Routes.SEARCH) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onContinueWatching = { item ->
                    val kind = when (item.kind) {
                        PlayableKind.MOVIE -> Routes.KIND_MOVIE
                        PlayableKind.EPISODE -> Routes.KIND_EPISODE
                    }
                    navController.navigate(Routes.vodPlayer(kind, item.itemId, resume = true))
                },
                onJumpToChannel = { channelId ->
                    navController.navigate(Routes.livePlayer(channelId))
                },
                onExit = onExitApp,
            )
        }

        composable(Routes.LIVE) {
            LiveTvScreen(
                remoteKeyBus = remoteKeyBus,
                onPlayChannel = { channel -> navController.navigate(Routes.livePlayer(channel.id)) },
                onReorderFavorites = { navController.navigate(Routes.REORDER_FAVORITES) },
                onOpenFullGuide = { channelId -> navController.navigate(Routes.epgGrid(channelId)) },
                onSearch = { navController.navigate(Routes.SEARCH) },
                onMovies = { navController.navigate(Routes.MOVIES) },
                onSeries = { navController.navigate(Routes.SERIES) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.LIVE_PLAYER,
            arguments = listOf(
                navArgument(Routes.ARG_CHANNEL_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_FROM_SEARCH) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            val fromSearch = entry.arguments?.getBoolean(Routes.ARG_FROM_SEARCH) ?: false
            LivePlayerScreen(
                remoteKeyBus = remoteKeyBus,
                onLeavePlayer = { navController.popBackStack() },
                // En la edición solo directo, lo puesto desde Buscar vuelve a Buscar con Atrás.
                backLeavesDirectly = fromSearch && BuildConfig.SOLO_DIRECTO,
            )
        }

        composable(Routes.REORDER_FAVORITES) {
            ReorderFavoritesScreen(onDone = { navController.popBackStack() })
        }

        composable(Routes.MOVIES) {
            MoviesScreen(onOpenMovie = { id -> navController.navigate(Routes.movieDetail(id)) })
        }

        composable(
            route = Routes.MOVIE_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_MOVIE_ID) { type = NavType.StringType }),
        ) {
            MovieDetailScreen(
                onPlay = { movieId, resume ->
                    navController.navigate(Routes.vodPlayer(Routes.KIND_MOVIE, movieId, resume))
                },
            )
        }

        composable(Routes.SERIES) {
            SeriesScreen(onOpenSeries = { id -> navController.navigate(Routes.seriesDetail(id)) })
        }

        composable(
            route = Routes.SERIES_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_SERIES_ID) { type = NavType.StringType }),
        ) {
            SeriesDetailScreen(
                onPlayEpisode = { episodeId, resume ->
                    navController.navigate(Routes.vodPlayer(Routes.KIND_EPISODE, episodeId, resume))
                },
            )
        }

        composable(
            route = Routes.VOD_PLAYER,
            arguments = listOf(
                navArgument(Routes.ARG_KIND) { type = NavType.StringType },
                navArgument(Routes.ARG_ITEM_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_RESUME) {
                    type = NavType.StringType
                    defaultValue = "true"
                },
            ),
        ) {
            VodPlayerScreen(
                remoteKeyBus = remoteKeyBus,
                onFinished = { navController.popBackStack() },
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onPlayChannel = { channel ->
                    navController.navigate(Routes.livePlayer(channel.id, fromSearch = true))
                },
                onOpenMovie = { id -> navController.navigate(Routes.movieDetail(id)) },
                onOpenSeries = { id -> navController.navigate(Routes.seriesDetail(id)) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onSourceCleared = {
                    navController.navigate(Routes.SETUP) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onOpenParental = { navController.navigate(Routes.PARENTAL) },
            )
        }

        composable(Routes.PARENTAL) {
            ParentalScreen(onDone = { navController.popBackStack() })
        }

        composable(
            route = Routes.EPG_GRID,
            arguments = listOf(navArgument(Routes.ARG_CHANNEL_ID) { type = NavType.StringType }),
        ) {
            EpgGridScreen()
        }
    }
}
