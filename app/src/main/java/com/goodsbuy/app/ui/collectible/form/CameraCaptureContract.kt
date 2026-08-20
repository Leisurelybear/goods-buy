package com.goodsbuy.app.ui.collectible.form

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * 调用系统相机拍照，照片写入 App 内部 filesDir/images/{UUID}.jpg。
 * 相机取消/失败时删除已创建的空文件，不留残留。
 */
class CameraCaptureContract : ActivityResultContract<Unit, Uri?>() {

    private var pendingFile: File? = null

    override fun createIntent(context: Context, input: Unit): Intent {
        val dir = File(context.filesDir, "images")
        dir.mkdirs()
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        pendingFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        val file = pendingFile ?: return null
        pendingFile = null
        return if (resultCode == Activity.RESULT_OK && file.exists()) {
            Uri.fromFile(file)
        } else {
            file.delete()
            null
        }
    }
}