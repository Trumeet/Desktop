package moe.yuuta.desktop

import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import platform.windows.*

object WinApi {
    fun run(program: String, desktop: String) {
        memScoped {
            val startupInfo = cValue<STARTUPINFOA> {
                lpDesktop = desktop.cstr.ptr
                dwFlags = STARTF_RUNFULLSCREEN.convert()
            }
            val pi = cValue<PROCESS_INFORMATION> {
            }
            // TODO: Close these objects?
            return@memScoped CreateProcessA(program,
                null,
                null,
                null,
                0,
                0.convert(),
                null,
                null,
                startupInfo.ptr,
                pi.ptr)
        }
    }

    fun close(desktop: WinDesktop): Boolean {
        return CloseDesktop(desktop.desktop) == 1
    }

    fun getThreadDesktop(thread: Int): WinDesktop? {
        val desk = GetThreadDesktop(thread.convert()) ?: return null
        return WinDesktop(desk, null)
    }

    fun getThreadDesktop(): WinDesktop? = getThreadDesktop(getCurrentThreadId())

    fun getCurrentThreadId(): Int = GetCurrentThreadId().convert()

    fun create(lpszDesktop: String,
               dwDesiredAccess: Int): WinDesktop? {
        val desk = CreateDesktopA(lpszDesktop,
            null,
            null,
            0.convert(),
            dwDesiredAccess.convert(),
            null) ?: return null
        return WinDesktop(desk, lpszDesktop)
    }

    fun switch(desktop: WinDesktop): Boolean {
        return SwitchDesktop(desktop.desktop) == 1
    }

    fun setThreadDesktop(desktop: WinDesktop): Boolean {
        return SetThreadDesktop(desktop.desktop) == 1
    }

    fun open(dwDesiredAccess: Int, desktop: String): WinDesktop? {
        val desk = OpenDesktopA(desktop,
            0.convert(),
            0.convert(),
            dwDesiredAccess.convert()) ?: return null
        return WinDesktop(desk, desktop)
    }
}