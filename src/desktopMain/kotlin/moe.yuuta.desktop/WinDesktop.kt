package moe.yuuta.desktop

import platform.windows.HDESK

data class WinDesktop(
    val desktop: HDESK,
    val name: String?
)