package com.mediadock.app.core.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object MediaActions {
    fun message(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    fun launch(context: Context, intent: Intent) {
        try { context.startActivity(intent) } catch (_: Exception) { message(context, "No compatible app is available for this action.") }
    }
    fun copy(context: Context, text: String) {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("MediaDock", text))
        message(context, "Copied to clipboard")
    }
    fun shareText(context: Context, text: String) = launch(context, Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share link"))
    fun openWeb(context: Context, url: String) {
        if (com.mediadock.app.core.datastore.WorkspaceRules.validUrl(url)) launch(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
    fun shareFiles(context: Context, uris: List<Uri>, mime: String = "*/*") {
        if (uris.isEmpty()) return
        val intent = if (uris.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
            else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        intent.type = mime
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData = ClipData.newRawUri("MediaDock files", uris.first()).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
        launch(context, Intent.createChooser(intent, "Share media"))
    }
}
