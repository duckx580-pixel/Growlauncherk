#include <jni.h>
#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <android/log.h>
#include <android/input.h>
#include <android/native_window.h>
#include <pthread.h>
#include <unistd.h>
#include <string>
#include <atomic>
#include <mutex>
#include <queue>
#include <dlfcn.h>

#include "got_hook.h"

#define IMGUI_IMPL_OPENGL_ES2
#include "imgui/imgui.h"
#include "imgui/backends/imgui_impl_opengl3.h"
#include "imgui/backends/imgui_impl_android.h"

#define LOG_TAG "ZennKuy"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static JavaVM* g_jvm = nullptr;
static jobject g_bridge_class = nullptr;
static std::atomic<bool> g_imgui_ready{false};
static std::atomic<bool> g_menu_open{false};

// ZennKuy native state
static std::atomic<bool> g_force_online_mode{false};
static std::atomic<bool> g_bypass_login{false};
static std::mutex g_token_mutex;
static std::string g_bypass_token;

// Message queue for rendering thread synchronization
static std::queue<int> messageQueue;
static std::mutex messageMutex;
static std::atomic<bool> messageQueueInitialized{false};

typedef EGLBoolean (*eglSwapBuffers_t)(EGLDisplay, EGLSurface);
static eglSwapBuffers_t g_orig_eglSwapBuffers = nullptr;
static std::atomic<bool> g_anzu_consent_hook_attempted{false};

// Anzu 4.0.2 can wait forever while applying a GDPR change. This was first
// reproduced through Houdini, but some native ARM devices block at the same
// point. Growtopia already owns and persists the consent state; preventing
// this secondary advertising notification avoids blocking the game loop.
static void zenn_anzu_set_gdpr_consent(int) {
    LOGI("Skipped blocking Anzu GDPR notification");
}

static int zenn_anzu_initialize(...) {
    LOGI("Skipped blocking Anzu initialization");
    return 0;
}

static void install_anzu_consent_hook() {
    if (g_anzu_consent_hook_attempted.exchange(true)) return;
    void* original = got_hook("libgrowtopia.so", "Anzu_SetGDPRConsent",
                              (void*)zenn_anzu_set_gdpr_consent);
    void* initialize = got_hook("libgrowtopia.so", "Anzu_Initialize",
                                (void*)zenn_anzu_initialize);
    if (original && initialize) LOGI("Installed Anzu deadlock guards");
    else LOGE("Could not install all Anzu deadlock guards");
}

static JNIEnv* get_env() {
    JNIEnv* env = nullptr;
    if (g_jvm) g_jvm->AttachCurrentThread(&env, nullptr);
    return env;
}

static void call_bridge(const char* method) {
    JNIEnv* env = get_env();
    if (!env || !g_bridge_class) return;
    jmethodID mid = env->GetStaticMethodID((jclass)g_bridge_class, method, "()V");
    if (mid) env->CallStaticVoidMethod((jclass)g_bridge_class, mid);
    if (env->ExceptionCheck()) env->ExceptionClear();
}

static void imgui_init() {
    if (g_imgui_ready) return;
    IMGUI_CHECKVERSION();
    ImGui::CreateContext();
    ImGuiIO& io = ImGui::GetIO();
    io.IniFilename = nullptr;
    io.DisplaySize = ImVec2(1080.0f, 2400.0f);
    ImGui::StyleColorsDark();
    ImGuiStyle& style = ImGui::GetStyle();
    style.WindowRounding = 8.0f;
    style.FrameRounding  = 4.0f;
    style.Alpha          = 0.92f;
    style.ScaleAllSizes(2.5f);
    io.FontGlobalScale = 2.0f;
    ImGui_ImplOpenGL3_Init("#version 100");
    g_imgui_ready = true;
    LOGI("ImGui initialised");
}

static int g_tab = 0;

static void render_menu() {
    ImGuiIO& io = ImGui::GetIO();
    float btn_sz = 60.0f;
    ImGui::SetNextWindowPos(ImVec2(io.DisplaySize.x - btn_sz - 8, 8), ImGuiCond_Always);
    ImGui::SetNextWindowSize(ImVec2(btn_sz, btn_sz), ImGuiCond_Always);
    ImGui::SetNextWindowBgAlpha(0.7f);
    ImGui::Begin("##toggle", nullptr,
                 ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_NoMove |
                 ImGuiWindowFlags_NoNav | ImGuiWindowFlags_NoSavedSettings);
    if (ImGui::Button("ZK", ImVec2(btn_sz - 16, btn_sz - 16)))
        g_menu_open = !g_menu_open.load();
    ImGui::End();

    if (!g_menu_open) return;

    ImGui::SetNextWindowPos(ImVec2(20, 80), ImGuiCond_Once);
    ImGui::SetNextWindowSize(ImVec2(io.DisplaySize.x - 40, 360), ImGuiCond_Once);
    ImGui::Begin("ZennKuy", nullptr,
                 ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_NoCollapse);

    const char* tabs[] = { "Google Login", "Status" };
    ImGui::BeginTabBar("##tabs");
    for (int i = 0; i < 2; i++) {
        if (ImGui::BeginTabItem(tabs[i])) { g_tab = i; ImGui::EndTabItem(); }
    }
    ImGui::EndTabBar();
    ImGui::Separator();

    if (g_tab == 0) {
        ImGui::TextWrapped("Error 10 fix: open Google in Chrome. MAC / RID / GID live in Settings.");
        ImGui::Spacing();
        ImGui::TextColored(ImVec4(0.4f, 1.0f, 0.4f, 1.0f),
                           "Tap Start Resolving to pick a Google account.");
        ImGui::Spacing();
        if (ImGui::Button("Start Resolving", ImVec2(-1, 70))) {
            call_bridge("startResolving");
        }
    } else {
        ImGui::Text("ZennKuy Status");
        ImGui::Separator();

        // Status indicators
        ImVec4 active_color = ImVec4(0.2f, 1.0f, 0.2f, 1.0f);
        ImVec4 inactive_color = ImVec4(0.7f, 0.7f, 0.7f, 1.0f);

        if (g_force_online_mode) {
            ImGui::TextColored(active_color, "● Force Online Mode: ACTIVE");
        } else {
            ImGui::TextColored(inactive_color, "● Force Online Mode: INACTIVE");
        }

        if (g_bypass_login) {
            ImGui::TextColored(active_color, "● Login Bypass: ACTIVE");
        } else {
            ImGui::TextColored(inactive_color, "● Login Bypass: INACTIVE");
        }

        ImGui::TextWrapped("OnlineGameController initialized and ready.");
    }
    ImGui::End();
}

static EGLBoolean my_eglSwapBuffers(EGLDisplay display, EGLSurface surface) {
    if (!g_imgui_ready) imgui_init();
    if (g_imgui_ready) {
        EGLint w = 0, h = 0;
        eglQuerySurface(display, surface, EGL_WIDTH,  &w);
        eglQuerySurface(display, surface, EGL_HEIGHT, &h);
        ImGuiIO& io = ImGui::GetIO();
        if (w > 0 && h > 0) io.DisplaySize = ImVec2((float)w, (float)h);
        ImGui_ImplOpenGL3_NewFrame();
        ImGui_ImplAndroid_NewFrame();
        ImGui::NewFrame();
        render_menu();
        ImGui::Render();
        ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());
    }
    return g_orig_eglSwapBuffers(display, surface);
}

extern "C" {

// Original touch handler
JNIEXPORT void JNICALL
Java_com_rtsoft_growtopia_Main_nativeOnTouch(JNIEnv*, jclass,
                                             jint action, jfloat x, jfloat y)
{
    if (!g_imgui_ready) return;
    ImGuiIO& io = ImGui::GetIO();
    switch (action & AMOTION_EVENT_ACTION_MASK) {
        case AMOTION_EVENT_ACTION_DOWN:
        case AMOTION_EVENT_ACTION_POINTER_DOWN:
            io.AddMousePosEvent(x, y);
            io.AddMouseButtonEvent(0, true);
            break;
        case AMOTION_EVENT_ACTION_UP:
        case AMOTION_EVENT_ACTION_POINTER_UP:
        case AMOTION_EVENT_ACTION_CANCEL:
            io.AddMouseButtonEvent(0, false);
            break;
        case AMOTION_EVENT_ACTION_MOVE:
            io.AddMousePosEvent(x, y);
            break;
    }
}

// New ZennKuyRenderer touch handler (called from AppRenderer)
JNIEXPORT jboolean JNICALL
Java_com_rtsoft_growtopia_Main_00024ZennKuyRenderer_nativeOnTouch(JNIEnv*, jclass,
                                                                  jint x, jint y, jint action)
{
    if (!g_imgui_ready) return JNI_FALSE;
    ImGuiIO& io = ImGui::GetIO();
    switch (action) {
        case 0: // ACTION_DOWN
            io.AddMousePosEvent(x, y);
            io.AddMouseButtonEvent(0, true);
            LOGI("nativeOnTouch: DOWN at (%d, %d)", x, y);
            break;
        case 1: // ACTION_UP
            io.AddMouseButtonEvent(0, false);
            LOGI("nativeOnTouch: UP at (%d, %d)", x, y);
            break;
        case 2: // ACTION_MOVE
            io.AddMousePosEvent(x, y);
            LOGI("nativeOnTouch: MOVE at (%d, %d)", x, y);
            break;
    }
    return io.WantCaptureMouse ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_rtsoft_growtopia_Main_isImGuiCapturingInput(JNIEnv*, jclass)
{
    if (!g_imgui_ready) return JNI_FALSE;
    return ImGui::GetIO().WantCaptureMouse ? JNI_TRUE : JNI_FALSE;
}

// ZennKuyRenderer native methods - Critical for GL synchronization and auth bypass

JNIEXPORT void JNICALL
Java_com_rtsoft_growtopia_Main_00024ZennKuyRenderer_nativeDrawFrame(JNIEnv*, jclass) {
    // Called every frame from AppRenderer to synchronize the GL thread.
}

// Message queue implementation - fixes the freeze by processing queued messages
// Called from rendering thread to get next message
JNIEXPORT jint JNICALL
Java_com_rtsoft_growtopia_Main_00024ZennKuyRenderer_nativeGetMessageZennKuy(JNIEnv*, jclass) {
    if (!messageQueueInitialized.load()) {
        return 0;
    }

    std::lock_guard<std::mutex> lock(messageMutex);

    if (messageQueue.empty()) {
        return 0;  // No message waiting - unblocks rendering loop
    }

    int message = messageQueue.front();
    messageQueue.pop();

    LOGI("nativeGetMessageZennKuy returning: %d", message);
    return message;
}

// Called from Java/UI thread to queue a message for processing
JNIEXPORT void JNICALL
Java_com_rtsoft_growtopia_Main_00024ZennKuyRenderer_nativeQueueMessageZennKuy(JNIEnv*, jclass, jint messageType) {
    std::lock_guard<std::mutex> lock(messageMutex);
    messageQueue.push(messageType);
    LOGI("nativeQueueMessageZennKuy queued: %d", messageType);
}

// Initialize the message queue when native lib loads
JNIEXPORT void JNICALL
Java_com_rtsoft_growtopia_Main_00024ZennKuyRenderer_nativeInitMessageQueueZennKuy(JNIEnv*, jclass) {
    messageQueueInitialized.store(true);
    LOGI("Message queue initialized");
}

JNIEXPORT void JNICALL
Java_com_rtsoft_growtopia_Main_00024ZennKuyRenderer_nativeSurfaceChanged(JNIEnv*, jclass, jint width, jint height) {
    install_anzu_consent_hook();
    LOGI("nativeSurfaceChanged: %dx%d", width, height);
    glViewport(0, 0, width, height);
}

JNIEXPORT void JNICALL
Java_com_rtsoft_growtopia_Main_00024ZennKuyRenderer_nativeForcedOnlineMode(JNIEnv*, jclass, jboolean force) {
    g_force_online_mode = (force == JNI_TRUE);
    LOGI("nativeForcedOnlineMode: %s", force ? "TRUE" : "FALSE");
}

JNIEXPORT void JNICALL
Java_com_rtsoft_growtopia_Main_00024ZennKuyRenderer_nativeBypassLogin(JNIEnv* env, jclass, jstring token_str) {
    const char* token = env->GetStringUTFChars(token_str, nullptr);
    if (token) {
        std::lock_guard<std::mutex> lock(g_token_mutex);
        g_bypass_token = token;
        g_bypass_login = true;
        LOGI("nativeBypassLogin: Token received (length: %zu), Login bypass activated", std::strlen(token));
        env->ReleaseStringUTFChars(token_str, token);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_rtsoft_growtopia_Main_00024ZennKuyRenderer_nativeGetBypassLoginStatus(JNIEnv*, jclass) {
    return g_bypass_login ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_rtsoft_growtopia_Main_00024ZennKuyRenderer_nativeGetForceOnlineModeStatus(JNIEnv*, jclass) {
    return g_force_online_mode ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_rtsoft_growtopia_Main_00024ZennKuyRenderer_nativeGetBypassToken(JNIEnv* env, jclass) {
    std::lock_guard<std::mutex> lock(g_token_mutex);
    return env->NewStringUTF(g_bypass_token.c_str());
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_jvm = vm;
    JNIEnv* env = nullptr;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) return -1;
    jclass cls = env->FindClass("com/rtsoft/growtopia/ZennKuyBridge");
    if (cls) {
        g_bridge_class = env->NewGlobalRef(cls);
        env->DeleteLocalRef(cls);
        LOGI("ZennKuyBridge class cached");
    } else {
        LOGE("ZennKuyBridge class not found");
        env->ExceptionClear();
    }
    // Keep the ARM game library untouched when Android translates it on an
    // x86 emulator. Rewriting its GOT can deadlock Growtopia's update thread
    // when the consent controller completes. ZennKuy's Java overlay continues
    // to provide the launcher controls without intercepting eglSwapBuffers.
    LOGI("Using Java ZennKuy overlay; native EGL interception is disabled");
    return JNI_VERSION_1_6;
}

} // extern "C"
