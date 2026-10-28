package com.artiuillab.tieryourlife.feature.play.data.di

import android.content.Context
import com.artiuillab.tieryourlife.feature.play.data.FileSavedGames
import com.artiuillab.tieryourlife.feature.play.domain.SavedGames
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayDataModule {

    @Provides
    @Singleton
    fun provideSavedGames(@ApplicationContext context: Context): SavedGames =
        FileSavedGames(File(context.filesDir, "play/saved-games.json"))
}
