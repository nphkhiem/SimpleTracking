package com.khiemnph.simpletracking.di.module

import com.khiemnph.domain.interactor.ActivityRecordUseCase
import com.khiemnph.domain.interactor.impl.ActivityRecordUseCaseImpl
import com.khiemnph.simpletracking.presenter.ActivityRecordPresenter
import com.khiemnph.simpletracking.presenter.impl.ActivityRecordPresenterImpl
import dagger.Module
import dagger.Provides

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */

@Module
class RecordModule {

    @Provides
    fun provideRecordPresenter(impl: ActivityRecordPresenterImpl): ActivityRecordPresenter = impl

    @Provides
    fun provideRecordUseCase(impl: ActivityRecordUseCaseImpl): ActivityRecordUseCase = impl
}