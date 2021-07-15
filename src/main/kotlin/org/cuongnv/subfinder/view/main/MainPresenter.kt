package org.cuongnv.subfinder.view.main

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.cuongnv.subfinder.model.SimpleSubtitle
import org.cuongnv.subfinder.reactx.MainScheduler

class MainPresenter {
    companion object {
        @Volatile
        private var instance: MainPresenter? = null

        fun getInstance(): MainPresenter {
            if (instance == null) {
                synchronized(MainPresenter::class.java) {
                    if (instance == null) instance = MainPresenter()
                }
            }
            return instance!!
        }

        const val TIMEOUT = 30L
        const val BASE_URL = "https://site.cuongnv.org/subfinder"
    }

    private val gson = Gson()

    private val subscribeScheduler = Schedulers.computation()
    private val observeScheduler = MainScheduler()

    private val client = OkHttpClient.Builder()
        .callTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
        .build()

    private var view: MainMvpView? = null

    private var searchDisposable: Disposable? = null
    private var downloadDisposable: Disposable? = null

    private val compositeDisposable = CompositeDisposable()

    fun attachView(view: MainMvpView?) {
        this.view = view
    }

    fun detachView() {
        this.view = null
        compositeDisposable.dispose()
    }

    private fun <T> runTask(observable: Observable<T>): Disposable {
        val disposable = observable.subscribeOn(subscribeScheduler)
            .observeOn(observeScheduler)
            .subscribe()

        compositeDisposable.add(disposable)
        return disposable
    }

    inline val String.urlString: String
        get() {
            return URLEncoder.encode(this, "UTF-8")
        }

    fun search(keyword: String) {
        searchDisposable?.dispose()
        searchDisposable = runTask(
            Observable
                .create<List<SimpleSubtitle>> {
                    try {
                        val response = client.newCall(
                            Request.Builder()
                                .get()
                                .url("${BASE_URL}/search?query=${keyword.urlString}&l=vietnamese")
                                .build()
                        ).execute().body()?.string()

                        val data =
                            gson.fromJson<List<SimpleSubtitle>>(
                                response,
                                object : TypeToken<List<SimpleSubtitle>>() {}.type
                            )
                        if (!it.isDisposed) {
                            it.onNext(data.sortedByDescending { item -> item.name })
                            it.onComplete()
                        }

                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                        if (!it.isDisposed) it.onError(ex)
                    }
                }
                .doOnNext { view?.onSearchSuccess(it) }
                .doOnError { view?.onSearchFail() }
        )
    }

    fun download(subtitle: SimpleSubtitle, path: String) {
        downloadDisposable?.dispose()
        downloadDisposable = runTask(
            Observable
                .create<String> {
                    try {
                        val response = client.newCall(
                            Request.Builder()
                                .get()
                                .url("${BASE_URL}/get-download-link?link=${subtitle.link.urlString}")
                                .build()
                        ).execute().body()?.string()

                        val json = JsonParser.parseString(response).asJsonObject
                        if (json.has("data") && !json.get("data").asString.isNullOrEmpty()) {
                            if (!it.isDisposed) {
                                it.onNext(json.get("data").asString)
                                it.onComplete()
                            }
                        } else {
                            if (!it.isDisposed) it.onError(Exception("Not found"))
                        }

                    } catch (ex: Throwable) {
                        ex.printStackTrace()
                        if (!it.isDisposed) it.onError(ex)
                    }
                }
                .flatMap { url ->
                    Observable.fromCallable {
                        try {
                            val inputStream = client.newCall(
                                Request.Builder()
                                    .get()
                                    .url(url)
                                    .build()
                            ).execute().body()?.byteStream()!!

                            val file = File(path, "${subtitle.name.urlString}.zip")
                            if (file.exists()) {
                                file.delete()
                            }

                            val outputStream = FileOutputStream(file)
                            outputStream.use { os ->
                                inputStream.buffered().iterator().forEachRemaining { i -> os.write(i.toInt()) }
                            }

                            inputStream.close()

                            return@fromCallable true
                        } catch (ex: Throwable) {
                            ex.printStackTrace()
                            return@fromCallable false
                        }
                    }
                }
                .doOnNext { view?.onDownloadSuccess() }
                .doOnError { view?.onDownloadFail() }
        )
    }
}