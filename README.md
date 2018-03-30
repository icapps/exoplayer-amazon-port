[ ![Download](https://api.bintray.com/packages/icapps/maven/exoplayer/images/download.svg) ](https://bintray.com/icapps/maven/exoplayer/_latestVersion)

iCapps port of the Amazon ExoPlayer Port - https://github.com/amzn/exoplayer-amazon-port
This repository is a port of the ExoPlayer project for Amazon devices, modified to support devices with PSSH V1 widevine issues.
Includes support for id segment matching for dynamic dash manifests
See https://github.com/amzn/exoplayer-amazon-port for the original project.
See "README-ORIGINAL.md" for the original ExoPlayer README.

Also see
https://developer.amazon.com/docs/fire-tv/media-players.html#exoplayer


Jcenter repo:
Applications can directly link to the pre-built libraries available in Jcenter repo

```
//Add the repository url to your gradle file
repositories {
    maven {
        url  "https://dl.bintray.com/icapps/maven" 
    }
}

// Add the required dependencies to your gradle file
compile 'com.icapps.android.exoplayer:exoplayer:X.Y.Z'
compile 'com.icapps.android.exoplayer:exoplayer-core:X.Y.Z'
compile 'com.icapps.android.exoplayer:exoplayer-hls:X.Y.Z'
compile 'com.icapps.android.exoplayer:exoplayer-dash:X.Y.Z'
compile 'com.icapps.android.exoplayer:exoplayer-smoothstreaming:X.Y.Z'
compile 'com.icapps.android.exoplayer:exoplayer-ui:X.Y.Z'
compile 'com.icapps.android.exoplayer:extension-gvr:X.Y.Z'
compile 'com.icapps.android.exoplayer:extension-ima:X.Y.Z'
compile 'com.icapps.android.exoplayer:extension-mediasession:X.Y.Z'
compile 'com.icapps.android.exoplayer:extension-okhttp:X.Y.Z'
compile 'com.icapps.android.exoplayer:extension-rtmp:X.Y.Z'

```
