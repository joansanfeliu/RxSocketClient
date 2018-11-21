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
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import moe.codeest.rxsocketclient.meta.DataWrapper
import moe.codeest.rxsocketclient.meta.SocketConfig
import java.net.InetSocketAddress
import java.net.Socket
import moe.codeest.rxsocketclient.meta.SocketState
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.IOException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


/**
 * @author: Est <codeest.dev@gmail.com>
 * @date: 2017/7/9
 * @description:
 */

class SocketObservable(val mConfig: SocketConfig, val mSocket: Socket?, val mSSLSocket: SSLSocket?) : Observable<DataWrapper>() {

    val mReadThread: ReadThread = ReadThread()
    lateinit var observerWrapper: SocketObserver
    var mHeartBeatRef: Disposable? = null

    override fun subscribeActual(observer: Observer<in DataWrapper>?) {
        observerWrapper = SocketObserver(observer)
        observer?.onSubscribe(observerWrapper)

        if (mSocket != null) {
            Thread(Runnable {
                try {
                    mSocket.connect(InetSocketAddress(mConfig.mIp, mConfig.mPort ?: 1080), mConfig.mTimeout ?: 0)
                    observer?.onNext(DataWrapper(SocketState.OPEN, ByteArray(0)))
                    mReadThread.start()
                } catch (e: IOException) {
                    println(e.toString())
                    observer?.onNext(DataWrapper(SocketState.CLOSE, ByteArray(0)))
                }
            }).start()
        }

        if (mSSLSocket != null) {
            Thread(Runnable {
                try {
                    mSSLSocket.connect(InetSocketAddress(mConfig.mIp, mConfig.mPort ?: 1080), mConfig.mTimeout ?: 0)
                    observer?.onNext(DataWrapper(SocketState.OPEN, ByteArray(0)))
                    mReadThread.start()
                } catch (e: IOException) {
                    println(e.toString())
                    observer?.onNext(DataWrapper(SocketState.CLOSE, ByteArray(0)))
                }
            }).start()
        }
    }

    fun setHeartBeatRef(ref: Disposable) {
        mHeartBeatRef = ref
    }

    fun close() {
        observerWrapper.dispose()
    }

    inner class SocketObserver(private val observer: Observer<in DataWrapper>?) : Disposable {

        fun onNext(data: ByteArray) {
            if (mSocket != null) {
                if (mSocket.isConnected) {
                    observer?.onNext(DataWrapper(SocketState.CONNECTING, data))
                }
            } else {
                if (mSSLSocket!!.isConnected) {
                    observer?.onNext(DataWrapper(SocketState.CONNECTING, data))
                }
            }
        }

        fun onNext(dataWrapper: DataWrapper) {
            if (mSocket != null) {
                if (mSocket.isConnected) {
                    observer?.onNext(dataWrapper)
                }
            } else {
                if (mSSLSocket!!.isConnected) {
                    observer?.onNext(dataWrapper)
                }
            }
        }

        override fun dispose() {
            mReadThread.interrupt()
            mHeartBeatRef?.dispose()
            if (mSocket != null) {
                mSocket.close()
            }
            if (mSSLSocket != null) {
                mSSLSocket.close()
            }
            observer?.onNext(DataWrapper(SocketState.CLOSE, ByteArray(0)))
        }

        override fun isDisposed(): Boolean {
            if (mSocket != null) {
                return mSocket.isConnected
            } else {
                return mSSLSocket!!.isConnected
            }
        }
    }

    inner class ReadThread : Thread() {
        override fun run() {
            super.run()
            if (mSocket != null) {
                try {
                    while (!mReadThread.isInterrupted && mSocket.isConnected) {
                        val input = DataInputStream(mSocket.getInputStream())
                        var buffer: ByteArray = ByteArray(input.available())
                        if (buffer.isNotEmpty()) {
                            input.read(buffer)
                            observerWrapper.onNext(buffer)
                        }
                    }
                } catch (e: Exception) {

                }
            }
            if (mSSLSocket != null) {
                try {
                    while (!mReadThread.isInterrupted && mSSLSocket.isConnected) {
                        val reader = BufferedReader(mSSLSocket.inputStream.bufferedReader(charset = mConfig.mCharset))
                        var line: String = reader.readLine()
                        if (line.isNotEmpty()) {
                            observerWrapper.onNext(line.toByteArray(charset = mConfig.mCharset))
                        }
                    }
                } catch (e: Exception) {

                }
            }
        }
    }
}