# UFAndroidClient #

UFAndroidClient is an Android application that apply software update received from an [UpdateFactory](https://www.kynetics.com/iot-platform-update-factory) or [Hawkbit](https://eclipse.org/hawkbit/) servers.

Links to official documentation:
- [installation](https://docs.updatefactory.io/devices/android/android-client-packages/)
- [configuration file](https://docs.updatefactory.io/devices/android/android-config-files/)
- [third-party integration](https://docs.updatefactory.io/devices/android/third-party-integration/)

build: [![Build Status](https://travis-ci.org/Kynetics/uf-android-client.svg?branch=master)](https://travis-ci.org/Kynetics/uf-android-client) 
[![Maintainability](https://api.codeclimate.com/v1/badges/3dcb8f7ce1c2a6c9f9e2/maintainability)](https://codeclimate.com/github/Kynetics/uf-android-client/maintainability)

## uf-client-service
uf-client-service is an android service that run in background and manage the updates.

uf-client-service must be install as **SYSTEM** application. [Android hidden api](https://github.com/anggrayudi/android-hidden-api).
are used from the version 1.0 to support update of sytems with double partitions

### State diagrams
#### Main
![UF STM Main](https://drive.google.com/uc?export=view&id=1g8r0gk7tNlrCbquzMlhXmDDGMxYc6kxT)
#### Update
![UF STM Update](https://drive.google.com/uc?export=view&id=1-EWX7pIpEWcBf3RFFW8MBhleooD8Nbp8)

## uf-client-ui-example
Uf-client-ui-example is an example of application that use the uf-client-service

## Third-Party Libraries
* [uf-ddiclient](https://github.com/Kynetics/uf-ddiclient) library

## Authors
* **Daniele Sergio** - *Initial work* - [danielesergio](https://github.com/danielesergio)
* **Andrea Zoleo** 
* **Diego Rondini**

See also the list of [contributors](https://github.com/Kynetics/UfAndroidClient/graphs/contributors) who participated in this project.

## License
Copyright © 2017-2019, [Kynetics LLC](https://www.kynetics.com).
Released under the [EPLv1 License](http://www.eclipse.org/legal/epl-v10.html).
