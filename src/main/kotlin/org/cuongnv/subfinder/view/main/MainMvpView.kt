package org.cuongnv.subfinder.view.main

import org.cuongnv.subfinder.model.SimpleSubtitle

interface MainMvpView {
    fun onSearchSuccess(data: List<SimpleSubtitle>)
    fun onSearchFail()

    fun onDownloadSuccess()
    fun onDownloadFail()
}