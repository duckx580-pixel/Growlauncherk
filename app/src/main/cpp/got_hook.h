#pragma once
#include <elf.h>
#include <link.h>
#include <dlfcn.h>
#include <sys/mman.h>
#include <unistd.h>
#include <string.h>
#include <android/log.h>

#define LOGTAG "ZennKuy/GOT"

static inline void* get_lib_base(const char* lib_name) {
    FILE* f = fopen("/proc/self/maps", "r");
    if (!f) return nullptr;
    char line[512];
    void* base = nullptr;
    while (fgets(line, sizeof(line), f)) {
        // Houdini maps translated ARM ELF files as read-only segments rather
        // than r-xp. The mapping at file offset zero is the ELF load base on
        // both native ARM devices and translated x86 emulators.
        if (strstr(line, lib_name) && strstr(line, " 00000000 ")) {
            unsigned long addr = 0;
            sscanf(line, "%lx-", &addr);
            base = (void*)addr;
            break;
        }
    }
    fclose(f);
    return base;
}

// Patch a single GOT entry. Returns the old value so the hook can call the original.
static inline void* got_hook(const char* lib_name, const char* sym_name, void* new_func) {
    void* base = get_lib_base(lib_name);
    if (!base) {
        __android_log_print(ANDROID_LOG_ERROR, LOGTAG, "lib %s not found in maps", lib_name);
        return nullptr;
    }

    auto* ehdr = (Elf64_Ehdr*)base;
    auto* phdr = (Elf64_Phdr*)((uint8_t*)base + ehdr->e_phoff);

    Elf64_Dyn* dyn = nullptr;
    for (int i = 0; i < ehdr->e_phnum; i++) {
        if (phdr[i].p_type == PT_DYNAMIC) {
            dyn = (Elf64_Dyn*)((uint8_t*)base + phdr[i].p_vaddr);
            break;
        }
    }
    if (!dyn) return nullptr;

    Elf64_Rela* rela_plt = nullptr;
    size_t rela_plt_sz = 0;
    const char* dynstr = nullptr;
    Elf64_Sym* dynsym = nullptr;

    for (Elf64_Dyn* d = dyn; d->d_tag != DT_NULL; d++) {
        if (d->d_tag == DT_JMPREL)   rela_plt    = (Elf64_Rela*)((uint8_t*)base + d->d_un.d_ptr);
        if (d->d_tag == DT_PLTRELSZ) rela_plt_sz = d->d_un.d_val;
        if (d->d_tag == DT_STRTAB)   dynstr      = (const char*)((uint8_t*)base + d->d_un.d_ptr);
        if (d->d_tag == DT_SYMTAB)   dynsym      = (Elf64_Sym*)((uint8_t*)base + d->d_un.d_ptr);
    }
    if (!rela_plt || !dynstr || !dynsym) return nullptr;

    size_t count = rela_plt_sz / sizeof(Elf64_Rela);
    for (size_t i = 0; i < count; i++) {
        uint32_t sym_idx = ELF64_R_SYM(rela_plt[i].r_info);
        const char* name = dynstr + dynsym[sym_idx].st_name;
        if (strcmp(name, sym_name) != 0) continue;

        void** got_entry = (void**)((uint8_t*)base + rela_plt[i].r_offset);
        void* old_func = *got_entry;

        long page_sz = sysconf(_SC_PAGESIZE);
        void* page = (void*)((uintptr_t)got_entry & ~(page_sz - 1));
        mprotect(page, page_sz, PROT_READ | PROT_WRITE);
        *got_entry = new_func;
        mprotect(page, page_sz, PROT_READ);

        __android_log_print(ANDROID_LOG_INFO, LOGTAG, "Hooked %s in %s: %p -> %p",
            sym_name, lib_name, old_func, new_func);
        return old_func;
    }
    __android_log_print(ANDROID_LOG_WARN, LOGTAG, "Symbol %s not found in %s PLT", sym_name, lib_name);
    return nullptr;
}
