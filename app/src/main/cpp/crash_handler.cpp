// Signal handler that writes a symbolized native crash report next to the Java reports.
// Devices that block READ_LOGS (and Android 9 without adb) give no other way to see why the
// prebuilt Growtopia engine dies, so the report is written from inside the crashing process.

#include <jni.h>

#include <android/log.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <signal.h>
#include <stdarg.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <unwind.h>

#define LOG_TAG "GtCrashHandler"

namespace {

constexpr int kSignals[] = {SIGSEGV, SIGABRT, SIGBUS, SIGILL, SIGFPE, SIGSYS, SIGTRAP};
constexpr size_t kSignalCount = sizeof(kSignals) / sizeof(kSignals[0]);
constexpr size_t kMaxFrames = 64;
constexpr size_t kPathMax = 512;

char g_report_path[kPathMax];
struct sigaction g_previous[kSignalCount];
char g_stack[64 * 1024];
bool g_handling;

struct BacktraceState {
    void** current;
    void** end;
};

_Unwind_Reason_Code unwind_frame(struct _Unwind_Context* context, void* arg) {
    auto* state = static_cast<BacktraceState*>(arg);
    uintptr_t pc = _Unwind_GetIP(context);
    if (pc != 0) {
        if (state->current == state->end) {
            return _URC_END_OF_STACK;
        }
        *state->current++ = reinterpret_cast<void*>(pc);
    }
    return _URC_NO_REASON;
}

void write_all(int fd, const char* text) {
    size_t remaining = strlen(text);
    while (remaining > 0) {
        ssize_t written = write(fd, text, remaining);
        if (written <= 0) {
            return;
        }
        text += written;
        remaining -= static_cast<size_t>(written);
    }
}

void write_line(int fd, const char* format, ...) __attribute__((format(printf, 2, 3)));

void write_line(int fd, const char* format, ...) {
    char line[1024];
    va_list args;
    va_start(args, format);
    vsnprintf(line, sizeof(line), format, args);
    va_end(args);
    write_all(fd, line);
}

const char* signal_name(int signo) {
    switch (signo) {
        case SIGSEGV: return "SIGSEGV";
        case SIGABRT: return "SIGABRT";
        case SIGBUS: return "SIGBUS";
        case SIGILL: return "SIGILL";
        case SIGFPE: return "SIGFPE";
        case SIGSYS: return "SIGSYS";
        case SIGTRAP: return "SIGTRAP";
        default: return "UNKNOWN";
    }
}

void write_backtrace(int fd) {
    void* frames[kMaxFrames];
    BacktraceState state{frames, frames + kMaxFrames};
    _Unwind_Backtrace(unwind_frame, &state);
    size_t count = static_cast<size_t>(state.current - frames);

    write_all(fd, "\nbacktrace:\n");
    for (size_t i = 0; i < count; ++i) {
        Dl_info info{};
        const char* library = "<unknown>";
        const char* symbol = "";
        uintptr_t offset = reinterpret_cast<uintptr_t>(frames[i]);
        if (dladdr(frames[i], &info) != 0) {
            if (info.dli_fname != nullptr) {
                library = info.dli_fname;
                offset -= reinterpret_cast<uintptr_t>(info.dli_fbase);
            }
            if (info.dli_sname != nullptr) {
                symbol = info.dli_sname;
            }
        }
        write_line(fd, "  #%02zu pc %016lx  %s%s%s\n", i, static_cast<unsigned long>(offset),
                   library, symbol[0] != '\0' ? "  " : "", symbol);
    }
}

void handle_signal(int signo, siginfo_t* info, void* context) {
    if (!g_handling) {
        g_handling = true;

        int fd = open(g_report_path, O_WRONLY | O_CREAT | O_TRUNC, 0644);
        if (fd >= 0) {
            write_line(fd, "signal %d (%s), code %d, fault addr %p\n", signo, signal_name(signo),
                       info != nullptr ? info->si_code : 0,
                       info != nullptr ? info->si_addr : nullptr);
            write_line(fd, "pid %d, tid %d\n", getpid(), gettid());
            write_backtrace(fd);
            write_all(fd, "\n(end of native report)\n");
            close(fd);
        }
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "native crash: signal %d (%s)", signo,
                            signal_name(signo));
    }

    for (size_t i = 0; i < kSignalCount; ++i) {
        if (kSignals[i] == signo) {
            sigaction(signo, &g_previous[i], nullptr);
            break;
        }
    }
    raise(signo);
}

}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_gentz_launcher_CrashLogger_installNativeHandler(JNIEnv* env, jclass, jstring report_path) {
    const char* path = env->GetStringUTFChars(report_path, nullptr);
    if (path == nullptr) {
        return JNI_FALSE;
    }
    snprintf(g_report_path, sizeof(g_report_path), "%s", path);
    env->ReleaseStringUTFChars(report_path, path);

    stack_t alternate{};
    alternate.ss_sp = g_stack;
    alternate.ss_size = sizeof(g_stack);
    sigaltstack(&alternate, nullptr);

    struct sigaction action{};
    action.sa_sigaction = handle_signal;
    action.sa_flags = SA_SIGINFO | SA_ONSTACK;
    sigemptyset(&action.sa_mask);

    for (size_t i = 0; i < kSignalCount; ++i) {
        if (sigaction(kSignals[i], &action, &g_previous[i]) != 0) {
            __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "cannot hook signal %d", kSignals[i]);
        }
    }
    return JNI_TRUE;
}

// Deliberate crash used to verify the report pipeline on a device without adb.
extern "C" JNIEXPORT void JNICALL
Java_com_gentz_launcher_CrashLogger_nativeSelfTest(JNIEnv*, jclass) {
    volatile int* invalid = nullptr;
    *invalid = 1;
}
