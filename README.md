# RxSocketClient

 [![API](https://img.shields.io/badge/API-20%2B-brightgreen.svg)](https://android-arsenal.com/api?level=20) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![](https://jitpack.io/v/joansanfeliu/RxSocketClient.svg)](https://jitpack.io/#joansanfeliu/RxSocketClient)


This project is a fork from [codeestX](https://github.com/codeestX/RxSocketClient/)

# Features Added
- SSLSocket support (protocol TLS v1.2 *only*)

# Installation
Step 1. Add the JitPack repository to your build file

	allprojects {
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
	}
   
Step 2. Add the dependency

	dependencies {
	        compile 'com.github.joansanfeliu:RxSocketClient:v0.0.1'
	}
	
## init
```java
SocketClient mClient = RxSocketClient
        .create(new SocketConfig.Builder()
                .setIp(IP)
                .setPort(PORT)
                .setCharset(Charsets.UTF_8)
                .setThreadStrategy(ThreadStrategy.ASYNC)
                .setTimeout(30 * 1000)
                .build())
        .option(new SocketOption.Builder()
                .setHeartBeat(HEART_BEAT, 60 * 1000)
                .setHead(HEAD)
                .setTail(TAIL)
                .build());

```
| value | default | description |
| :--: | :--: | :--: |
| Ip | required | host address |
| Port | required | port number |
| Charset | UTF_8 | the charset when encode a String to byte[] |
| ThreadStrategy | Async | sending data asynchronously or synchronously|
| Timeout | 0 | the timeout of a connection, millisecond |
| HeartBeat | Optional | value and interval of heartbeat, millisecond |
| Head | Optional | appending bytes at head when sending data, not included heartbeat |
| Tail | Optional | appending bytes at last when sending data, not included heartbeat |

## connect
```java
Disposable ref = mClient.connect()
	... // anything else what you can do with RxJava
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SocketSubscriber() {
                    @Override
                    public void onConnected() {
                        //onConnected
                        Log.e(TAG, "onConnected");
                    }

                    @Override
                    public void onDisconnected() {
                        //onDisconnected
                        Log.e(TAG, "onDisconnected");
                    }

                    @Override
                    public void onResponse(@NotNull byte[] data) {
                        //receive data
                        Log.e(TAG, Arrays.toString(data));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        //onError
                        Log.e(TAG, throwable.toString());
                    }
                });
```

## disconnect
```java
mClient.disconnect();
//or
ref.dispose();
```

## sendData
```java
mClient.sendData(bytes);
//or
mClient.sendData(string);
```

# License

      Copyright (c) 2017 codeestX
      Copyright (c) 2018 joansanfeliu

      Licensed under the Apache License, Version 2.0 (the "License");
      you may not use this file except in compliance with the License.
      You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

      Unless required by applicable law or agreed to in writing, software
      distributed under the License is distributed on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
      See the License for the specific language governing permissions and
      limitations under the License.

