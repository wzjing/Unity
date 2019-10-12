#include <jni.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/prctl.h>
#include <android/log.h>
#include <pthread.h>

#define LOGD(TAG, format, ...) __android_log_print(ANDROID_LOG_DEBUG, TAG, format, ## __VA_ARGS__)
#define LOGE(TAG, format, ...) __android_log_print(ANDROID_LOG_ERROR, TAG, format, ## __VA_ARGS__)

const char *TAG = "unity-daemon";

pid_t sub_proc;
pid_t daemon_proc;

void* sunProcessMonitor(void* data) {
    int status = 0;
    waitpid(sub_proc, &status, 0);
    LOGD(TAG, "sub process %d exit", sub_proc);
    pthread_exit(0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wzjing_unity_MainActivity_createDaemon(JNIEnv *env, jobject thiz) {
    sub_proc = fork();
    if (sub_proc == 0) {
        LOGD(TAG, "sub  process %d created", getpid());
        daemon_proc = fork();
        if (daemon_proc == 0) {
            int counter = 0;
            LOGD(TAG, "daemon process %d created", getpid());
            while (1) {
                counter++;
                LOGD(TAG, "daemon process %d - %d : %d", getpid(), getppid(), counter);
                sleep(1);
            }
        } else if (daemon_proc > 0) {
            _exit(0);
        } else {
            LOGE(TAG, "error: unable to create orphan process (%d)", daemon_proc);
        }
    } else if (sub_proc > 0) {
        pthread_t monitor_thread;
        pthread_create(&monitor_thread, nullptr, sunProcessMonitor, nullptr);
    } else {
        LOGE(TAG, "error: unable to create sub process (%d)", sub_proc);
    }
}