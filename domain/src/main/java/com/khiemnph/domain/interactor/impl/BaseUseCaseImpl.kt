package com.khiemnph.domain.interactor.impl

import com.khiemnph.domain.executor.TaskExecutor
import com.khiemnph.domain.interactor.BaseUseCase
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
open class BaseUseCaseImpl: BaseUseCase {

    protected val disposable = CompositeDisposable()

    protected fun <T> addTask(
        single      : Single<T>,
        doOnError   : (Throwable) -> Unit = {},
        doOnSuccess : (T) -> Unit         = {}
    ) {
        single
            .subscribeOn(Schedulers.from(TaskExecutor.instance))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableSingleObserver<T>() {
                override fun onError(e: Throwable) = doOnError(e)
                override fun onSuccess(t: T) = doOnSuccess(t)
            })
            .also { disposable.add(it) }
    }

    protected fun <T> addTask(
        observable  : Observable<T>,
        doOnError   : (Throwable) -> Unit = {},
        doOnComplete: ( ) -> Unit         = {},
        doOnNext    : (T) -> Unit         = {}
    ) {
        observable
            .subscribeOn(Schedulers.from(TaskExecutor.instance))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableObserver<T>() {
                override fun onError(e: Throwable) = doOnError(e)
                override fun onComplete() = doOnComplete()
                override fun onNext(t: T) = doOnNext(t)
            })
            .also { disposable.add(it) }
    }

    protected fun addTask(
        completable : Completable,
        doOnError   : (Throwable) -> Unit = {},
        doOnComplete: ( ) -> Unit         = {}
    ) {
        completable
            .subscribeOn(Schedulers.from(TaskExecutor.instance))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableCompletableObserver() {
                override fun onError(e: Throwable) = doOnError(e)
                override fun onComplete() = doOnComplete()
            })
            .also { disposable.add(it) }
    }

    override fun endTask() {
        TODO("Not yet implemented")
    }
}