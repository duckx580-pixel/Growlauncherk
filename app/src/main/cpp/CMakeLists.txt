cmake_minimum_required(VERSION 3.22.1)
project(growtopia_launcher_native)

# ── gtcrash (existing crash handler) ─────────────────────────────────────────
add_library(gtcrash SHARED crash_handler.cpp)
target_link_libraries(gtcrash log)

# ── ImGui sources ─────────────────────────────────────────────────────────────
set(IMGUI_DIR ${CMAKE_CURRENT_SOURCE_DIR}/imgui)

add_library(imgui STATIC
    ${IMGUI_DIR}/imgui.cpp
    ${IMGUI_DIR}/imgui_draw.cpp
    ${IMGUI_DIR}/imgui_tables.cpp
    ${IMGUI_DIR}/imgui_widgets.cpp
    ${IMGUI_DIR}/backends/imgui_impl_opengl3.cpp
    ${IMGUI_DIR}/backends/imgui_impl_android.cpp
)
target_include_directories(imgui PUBLIC
    ${IMGUI_DIR}
    ${IMGUI_DIR}/backends
)
target_compile_definitions(imgui PUBLIC IMGUI_IMPL_OPENGL_ES2)
target_compile_options(imgui PRIVATE -O2)

# ── libzennkuy ────────────────────────────────────────────────────────────────
add_library(zennkuy SHARED zennkuy.cpp)

target_include_directories(zennkuy PRIVATE
    ${IMGUI_DIR}
    ${IMGUI_DIR}/backends
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_compile_definitions(zennkuy PRIVATE IMGUI_IMPL_OPENGL_ES2)

target_link_libraries(zennkuy
    imgui
    android
    log
    EGL
    GLESv2
)
