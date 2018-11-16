/*
 * Copyright (C) 2017 codeestX
 * Copyright (C) 2018 joansanfeliu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package moe.codeest.rxsocketclient

import io.reactivex.Observable
import moe.codeest.rxsocketclient.meta.DataWrapper
import moe.codeest.rxsocketclient.meta.SocketConfig
import moe.codeest.rxsocketclient.meta.SocketOption
import moe.codeest.rxsocketclient.meta.ThreadStrategy
import moe.codeest.rxsocketclient.post.AsyncPoster
import moe.codeest.rxsocketclient.post.IPoster
import moe.codeest.rxsocketclient.post.SyncPoster
import java.net.Socket
import java.security.cert.X509Certificate
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * @author: Est <codeest.dev@gmail.com>
 * @date: 2017/7/9
 * @description:
 */

class SocketClient(val mConfig: SocketConfig) {

    var mSocket: Socket = Socket()
    lateinit var mSSLSocket: SSLSocket
    var mOption: SocketOption? = null
    lateinit var mObservable: Observable<DataWrapper>
    lateinit var mIPoster: IPoster
    var mExecutor: Executor = Executors.newCachedThreadPool()

    fun option(option: SocketOption): SocketClient {
        mOption = option
        return this
    }

    fun connect(): Observable<DataWrapper> {
        if (mConfig.mSSL) {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
            })
            val context: SSLContext = SSLContext.getInstance("TLSv1.2").apply {
                init(null, trustAllCerts, null)
            }
            mSSLSocket = context.socketFactory.createSocket() as SSLSocket
            val protocols = Array<String>(1) {"TLSv1.2"}
            mSSLSocket.apply {
                enabledProtocols = protocols
                soTimeout = 30000
                useClientMode = true
            }

            mObservable = SocketObservable(mConfig, null, mSSLSocket)
        } else {
            mObservable = SocketObservable(mConfig, mSocket, null)
        }
        mIPoster = if (mConfig.mThreadStrategy == ThreadStrategy.ASYNC) AsyncPoster(this, mExecutor) else SyncPoster(this, mExecutor)
        initHeartBeat()
        return mObservable
    }

    fun disconnect() {
        if (mObservable is SocketObservable) {
            (mObservable as SocketObservable).close()
        }
    }

    private fun initHeartBeat() {
        mOption?.apply {
            if (mHeartBeatConfig != null) {
            val disposable = Observable.interval(mHeartBeatConfig.interval, TimeUnit.MILLISECONDS)
                        .subscribe({
                            mIPoster.enqueue(mHeartBeatConfig.data?: ByteArray(0))
                        })
                if (mObservable is SocketObservable) {
                    (mObservable as SocketObservable).setHeartBeatRef(disposable)
                }
            }
        }
    }

    fun sendData(data: ByteArray) {
        mOption?.apply {
            if (mHead != null || mTail != null) {
                var result: String = data.toString()
                mHead?.let {
                    if (mHead.isNotEmpty()) {
                        mHead.toString().plus(result)
                    }
                }
                mTail?.let {
                    if (mTail.isNotEmpty()) {
                        result.plus(mTail.toString())
                    }
                }
                mIPoster.enqueue(result.toByteArray(charset = mConfig.mCharset))
                    return@sendData
            }
        }
        mIPoster.enqueue(data)
    }

    fun sendData(string: String) {
        sendData(string.toByteArray(charset = mConfig.mCharset))
    }

    fun isConnecting(): Boolean {
        if (mConfig.mSSL) {
            return mSSLSocket.isConnected
        } else {
            return mSocket.isConnected
        }
    }
}