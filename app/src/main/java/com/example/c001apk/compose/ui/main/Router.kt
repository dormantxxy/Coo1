package com.example.c001apk.compose.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.c001apk.compose.R

/**
 * Created by bggRGjQaUbCoE on 2024/5/30
 */
sealed class Router(
    val name: String,
    @StringRes val stringId: Int? = null,
    @DrawableRes val unselectedIcon: Int? = null,
    @DrawableRes val selectedIcon: Int? = null,
) {

    data object MAIN : Router(
        name = "MAIN"
    )

    data object HOME : Router(
        name = "HOME",
        stringId = R.string.home,
        unselectedIcon = R.drawable.ic_ios_home_outline,
        selectedIcon = R.drawable.ic_ios_home_filled
    )

    data object MESSAGE : Router(
        name = "MESSAGE",
        stringId = R.string.message,
        unselectedIcon = R.drawable.ic_ios_message_outline,
        selectedIcon = R.drawable.ic_ios_message_filled
    )

    data object SETTINGS : Router(
        name = "SETTINGS",
        stringId = R.string.settings,
        unselectedIcon = R.drawable.ic_ios_settings_outline,
        selectedIcon = R.drawable.ic_ios_settings_filled
    )

    data object PARAMS : Router(name = "PARAMS")

    data object ABOUT : Router(name = "ABOUT")

    data object LICENSE : Router(name = "LICENSE")

    data object BLACKLIST : Router(name = "BLACKLIST")

    data object SEARCH : Router(name = "SEARCH")

    data object SEARCHRESULT : Router(name = "SEARCHRESULT")

    data object TAB : Router(name = "TAB")

    data object FEED : Router(name = "FEED")

    data object USER : Router(name = "USER")

    data object TOPIC : Router(name = "TOPIC")

    data object COPY : Router(name = "COPY")

    data object WEBVIEW : Router(name = "WEBVIEW")

    data object APP : Router(name = "APP")

    data object LOGIN : Router(name = "LOGIN")

    data object CAROUSEL : Router(name = "CAROUSEL")

    data object UPDATE : Router(name = "UPDATE")

    data object FFFLIST : Router(name = "FFFLIST")

    data object DYH : Router(name = "DYH")

    data object COOLPIC : Router(name = "COOLPIC")

    data object NOTICE : Router(name = "NOTICE")

    data object HISTORY : Router(name = "HISTORY")

    data object CHAT : Router(name = "CHAT")

    data object COLLECTION : Router(name = "COLLECTION")

}
