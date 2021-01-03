package com.khiemnph.simpletracking.di.module

import com.khiemnph.domain.interactor.MainUseCase
import com.khiemnph.domain.interactor.impl.MainUseCaseImpl
import com.khiemnph.simpletracking.presenter.MainPresenter
import com.khiemnph.simpletracking.presenter.impl.MainPresenterImpl
import dagger.Module
import dagger.Provides

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
@Module
class MainModule {

    @Provides
    fun provideMainPresenter(impl: MainPresenterImpl): MainPresenter = impl

    @Provides
    fun provideMainUseCase(impl: MainUseCaseImpl): MainUseCase = impl
}