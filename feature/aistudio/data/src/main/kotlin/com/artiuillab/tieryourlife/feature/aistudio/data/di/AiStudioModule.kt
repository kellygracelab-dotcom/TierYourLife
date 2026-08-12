package com.artiuillab.tieryourlife.feature.aistudio.data.di

import com.artiuillab.tieryourlife.feature.aistudio.data.generation.AndroidBase64Decoder
import com.artiuillab.tieryourlife.feature.aistudio.data.generation.Base64Decoder
import com.artiuillab.tieryourlife.feature.aistudio.data.generation.GeneratedImageStore
import com.artiuillab.tieryourlife.feature.aistudio.data.generation.ImageBytesStore
import com.artiuillab.tieryourlife.feature.aistudio.data.library.PoolGeneratedCardSaver
import com.artiuillab.tieryourlife.feature.aistudio.domain.library.GeneratedCardSaver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiStudioModule {

    @Binds
    @Singleton
    abstract fun bindGeneratedCardSaver(implementation: PoolGeneratedCardSaver): GeneratedCardSaver

    @Binds
    @Singleton
    abstract fun bindImageBytesStore(implementation: GeneratedImageStore): ImageBytesStore

    @Binds
    @Singleton
    abstract fun bindBase64Decoder(implementation: AndroidBase64Decoder): Base64Decoder
}
