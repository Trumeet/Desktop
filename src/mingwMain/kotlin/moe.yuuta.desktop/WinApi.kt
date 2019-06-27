package moe.yuuta.desktop

import kotlinx.cinterop.convert
import platform.windows.*

object WinApi {
    /*
    fun list(lParam: Long, callback: (lpszDesktop: LPSTR?,
                                      lParam: Long) -> Int): Boolean {
        return EnumDesktopsA(GetProcessWindowStation(),
            staticCFunction(callback),
            lParam) == 1
    }
    */
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
}