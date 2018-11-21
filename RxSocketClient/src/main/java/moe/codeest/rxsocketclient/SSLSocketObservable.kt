package moe.codeest.rxsocketclient

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import moe.codeest.rxsocketclient.meta.DataWrapper
import moe.codeest.rxsocketclient.meta.SocketConfig
import moe.codeest.rxsocketclient.meta.SocketState
import java.io.BufferedReader
import java.io.IOException
import java.net.InetSocketAddress
import javax.net.ssl.SSLSocket

class SSLSocketObservable(val mConfig: SocketConfig, val mSSLSocket: SSLSocket) : Observable<DataWrapper>() {
    val mReadThread: ReadThread = ReadThread()
    lateinit var observerWrapper: SocketObserver
    var mHeartBeatRef: Disposable? = null

    override fun subscribeActual(observer: Observer<in DataWrapper>?) {
        observerWrapper = SocketObserver(observer)
        observer?.onSubscribe(observerWrapper)
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

    fun setHeartBeatRef(ref: Disposable) {
        mHeartBeatRef = ref
    }

    fun close() {
        observerWrapper.dispose()
    }

    inner class SocketObserver(private val observer: Observer<in DataWrapper>?) : Disposable {

        fun onNext(data: ByteArray) {
            if (mSSLSocket.isConnected) {
                observer?.onNext(DataWrapper(SocketState.CONNECTING, data))
            }
        }

        fun onNext(dataWrapper: DataWrapper) {
            if (mSSLSocket.isConnected) {
                observer?.onNext(dataWrapper)
            }
        }

        override fun dispose() {
            mReadThread.interrupt()
            mHeartBeatRef?.dispose()
            mSSLSocket.close()
            observer?.onNext(DataWrapper(SocketState.CLOSE, ByteArray(0)))
        }

        override fun isDisposed(): Boolean {
            return mSSLSocket.isConnected
        }
    }

    inner class ReadThread : Thread() {
        override fun run() {
            super.run()
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