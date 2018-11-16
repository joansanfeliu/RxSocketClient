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

package moe.codeest.rxsocketclient.meta

import java.nio.charset.Charset

/**
 * @author: Est <codeest.dev@gmail.com>
 * @date: 2017/7/8
 * @description:
 */

class SocketConfig(
        val mSSL: Boolean,
        val mIp: String?,
        val mPort: Int?,
        val mTimeout: Int?,
        val mCharset: Charset = Charsets.UTF_8,
        val mThreadStrategy: Int?
) {

    private constructor(builder: Builder) : this(builder.mSSL, builder.mIp, builder.mPort,
            builder.mTimeout, builder.mCharset, builder.mThreadStrategy)

    class Builder {
        var mSSL: Boolean = false
            private set

        var mIp: String? = null
            private set

        var mPort: Int? = null
            private set

        var mTimeout: Int? = null
            private set

        var mCharset: Charset = Charsets.UTF_8
            private set

        var mThreadStrategy: Int? = ThreadStrategy.ASYNC

        fun setSSL(ssl: Boolean) = apply { this.mSSL = ssl }

        fun setIp(ip: String) = apply { this.mIp = ip }

        fun setPort(port: Int) = apply { this.mPort = port }

        fun setTimeout(timeout: Int) = apply { this.mTimeout = timeout }

        fun setCharset(charset: Charset) = apply { this.mCharset = charset}

        fun setThreadStrategy(threadStrategy: Int) = apply { this.mThreadStrategy = threadStrategy }

        fun build() = SocketConfig(this)
    }
}