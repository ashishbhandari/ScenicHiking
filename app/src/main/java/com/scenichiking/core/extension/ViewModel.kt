package com.scenichiking.core.extension

import androidx.lifecycle.MutableLiveData


fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}
