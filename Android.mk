LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += $(call all-java-files-under, gen)
LOCAL_PACKAGE_NAME := Klaxon
LOCAL_SDK_VERSION := current
LOCAL_STATIC_JAVA_LIBRARIES := libdb

include $(BUILD_PACKAGE)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libdb:libs/com.antlersoft.android.db_0.1.3.jar

include $(BUILD_MULTI_PREBUILT)
