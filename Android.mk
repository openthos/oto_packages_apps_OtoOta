LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

appcompat_dir := $(LOCAL_PATH)/../../../prebuilts/sdk/current/support/v7/appcompat/res

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-appcompat
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v13
LOCAL_STATIC_JAVA_LIBRARIES += xutils

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
                   src/com/openthos/ModifyPath.aidl

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res $(appcompat_dir)


LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.appcompat

LOCAL_PACKAGE_NAME := OtoOta
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := xutils:xUtils-2.6.14.jar

include $(BUILD_MULTI_PREBUILT)

ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
