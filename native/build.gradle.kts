/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id("com.android.library")
}

android {
    compileSdkVersion(29)
    buildToolsVersion = "29.0.2"

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            externalNativeBuild {
                cmake {
                    arguments.add("-DANDROID_PACKAGE_NAME=${android.defaultConfig.applicationId}")
                }
            }
        }
        getByName("debug") {
            externalNativeBuild {
                cmake {
                    arguments.add("-DANDROID_PACKAGE_NAME=${android.defaultConfig.applicationId}.debug")
                }
            }
        }
    }
    externalNativeBuild.cmake {
        setPath(file("tools/CMakeLists.txt"))
    }
}
