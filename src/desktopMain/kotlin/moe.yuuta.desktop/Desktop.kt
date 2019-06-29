package moe.yuuta.desktop

import kotlinx.cinterop.*
import libui.ktx.MsgBoxError
import libui.ktx.appWindow
import libui.ktx.hbox
import libui.ktx.tableview
import platform.windows.*

const val FLAG = DESKTOP_READOBJECTS or
        DESKTOP_CREATEWINDOW or
        DESKTOP_CREATEMENU or
        DESKTOP_HOOKCONTROL or
        DESKTOP_JOURNALRECORD or
        DESKTOP_JOURNALPLAYBACK or
        DESKTOP_ENUMERATE or
        DESKTOP_WRITEOBJECTS or
        DESKTOP_SWITCHDESKTOP

private val mDesktops = mutableListOf<String>()

@Suppress("unused")
fun main(args: Array<String>?) {
    try {
        appMain()
    } catch (e: Throwable) {
        MessageBoxA(null,
            "App crashed unexpectedly.\n" +
                    "Exception: $e\n" +
                    "System error code: ${GetLastError()}",
            "Secure Desktop",
            (MB_OK or MB_ICONERROR).convert())
    }
}

fun appMain() = appWindow(
    title = "Desktops",
    width = 320,
    height = 100
) {
    refresh()
    if (!mDesktops.contains("CustomDesktop")) {
        WinApi.create("CustomDesktop", FLAG)
        mDesktops.add("CustomDesktop")
    }
    hbox {
        tableview(mDesktops) {
            column("Name") {
                label {
                    data[it]
                }
            }
            column("Action") {
                button("Switch to") {
                    val desk = WinApi.open(FLAG, data[it])
                    if (desk == null) {
                        MsgBoxError("Cannot open the desktop",
                            "The system returns an empty desktop.\n" +
                                    "Error: ${GetLastError()}")
                        return@button
                    }
                    // TODO: Run an instance in a desktop only
                    val pathBuffer = nativeHeap.allocArray<CHARVar>(MAX_PATH)
                    GetModuleFileNameA(null, pathBuffer, MAX_PATH)
                    val path = pathBuffer.toKString()
                    nativeHeap.free(pathBuffer)
                    WinApi.run(path, data[it])
                    if (!WinApi.switch(desk)) {
                        MsgBoxError("Cannot switch to the desktop",
                            "The system reports this operation fails.\n" +
                                    "Error: ${GetLastError()}")
                        return@button
                    }
                }
            }
        }
        // TODO: Support creating new desktops
    }
}

private fun refresh() {
    mDesktops.clear()
    if (EnumDesktopsA(GetProcessWindowStation(),
            staticCFunction { lpszDesktop, _ ->
                if (lpszDesktop == null) return@staticCFunction 1
                mDesktops.add(lpszDesktop.toKString())
                return@staticCFunction 1
            },
            0) == 0) {
        MsgBoxError("Cannot obtain the desktop list",
            "Error code: ${GetLastError()}")
    }
}