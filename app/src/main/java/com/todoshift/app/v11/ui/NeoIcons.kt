package com.todoshift.app.v11.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import com.todoshift.app.v11.NeoIconStyle

/** Resolve supported vectors from the same Material icon family throughout Premium. */
@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    val outlined = LocalNeoAppearance.current.iconStyle == NeoIconStyle.OUTLINED
    val icon = if (!LocalV11NeoEnabled.current) imageVector else when (imageVector.name.substringAfterLast('.')) {
        "Add" -> if (outlined) Icons.Outlined.Add else Icons.Rounded.Add
        "Check" -> if (outlined) Icons.Outlined.Check else Icons.Rounded.Check
        "Close" -> if (outlined) Icons.Outlined.Close else Icons.Rounded.Close
        "Delete" -> if (outlined) Icons.Outlined.Delete else Icons.Rounded.Delete
        "Visibility" -> if (outlined) Icons.Outlined.Visibility else Icons.Rounded.Visibility
        "VisibilityOff" -> if (outlined) Icons.Outlined.VisibilityOff else Icons.Rounded.VisibilityOff
        "Edit" -> if (outlined) Icons.Outlined.Edit else Icons.Rounded.Edit
        "Notifications" -> if (outlined) Icons.Outlined.Notifications else Icons.Rounded.Notifications
        "Settings" -> if (outlined) Icons.Outlined.Settings else Icons.Rounded.Settings
        "Lock" -> if (outlined) Icons.Outlined.Lock else Icons.Rounded.Lock
        "Payments" -> if (outlined) Icons.Outlined.Payments else Icons.Rounded.Payments
        "CheckCircle" -> if (outlined) Icons.Outlined.CheckCircle else Icons.Rounded.CheckCircle
        "CalendarMonth" -> if (outlined) Icons.Outlined.CalendarMonth else Icons.Rounded.CalendarMonth
        "DateRange" -> if (outlined) Icons.Outlined.DateRange else Icons.Rounded.DateRange
        "ExpandLess" -> if (outlined) Icons.Outlined.ExpandLess else Icons.Rounded.ExpandLess
        "ExpandMore" -> if (outlined) Icons.Outlined.ExpandMore else Icons.Rounded.ExpandMore
        "KeyboardArrowUp" -> if (outlined) Icons.Outlined.KeyboardArrowUp else Icons.Rounded.KeyboardArrowUp
        "KeyboardArrowDown" -> if (outlined) Icons.Outlined.KeyboardArrowDown else Icons.Rounded.KeyboardArrowDown
        "Refresh" -> if (outlined) Icons.Outlined.Refresh else Icons.Rounded.Refresh
        "Backup" -> if (outlined) Icons.Outlined.Backup else Icons.Rounded.Backup
        "Cloud" -> if (outlined) Icons.Outlined.Cloud else Icons.Rounded.Cloud
        "Download" -> if (outlined) Icons.Outlined.Download else Icons.Rounded.Download
        "Upload" -> if (outlined) Icons.Outlined.Upload else Icons.Rounded.Upload
        "MoreVert" -> if (outlined) Icons.Outlined.MoreVert else Icons.Rounded.MoreVert
        "Info" -> if (outlined) Icons.Outlined.Info else Icons.Rounded.Info
        "Search" -> if (outlined) Icons.Outlined.Search else Icons.Rounded.Search
        "Security" -> if (outlined) Icons.Outlined.Security else Icons.Rounded.Security
        "Work" -> if (outlined) Icons.Outlined.Work else Icons.Rounded.Work
        "AccountBalanceWallet" -> if (outlined) Icons.Outlined.AccountBalanceWallet else Icons.Rounded.AccountBalanceWallet
        "Home" -> if (outlined) Icons.Outlined.Home else Icons.Rounded.Home
        "Fingerprint" -> if (outlined) Icons.Outlined.Fingerprint else Icons.Rounded.Fingerprint
        "RadioButtonUnchecked" -> if (outlined) Icons.Outlined.RadioButtonUnchecked else Icons.Rounded.RadioButtonUnchecked
        "ArrowBack" -> if (outlined) Icons.AutoMirrored.Outlined.ArrowBack else Icons.AutoMirrored.Rounded.ArrowBack
        "ChevronLeft", "KeyboardArrowLeft" -> Icons.AutoMirrored.Rounded.KeyboardArrowLeft
        "ChevronRight", "KeyboardArrowRight" -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
        else -> imageVector
    }
    androidx.compose.material3.Icon(imageVector = icon, contentDescription = contentDescription, modifier = modifier, tint = tint)
}

@Composable
fun Icon(painter: Painter, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    androidx.compose.material3.Icon(painter = painter, contentDescription = contentDescription, modifier = modifier, tint = tint)
}
