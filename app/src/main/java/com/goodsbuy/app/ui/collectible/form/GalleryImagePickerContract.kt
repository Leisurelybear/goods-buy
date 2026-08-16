package com.goodsbuy.app.ui.collectible.form

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

/** Opens the system photo picker, or the device gallery on pre-Android 13 devices. */
class GalleryImagePickerContract(
    private val maxItems: Int
) : ActivityResultContract<Unit, List<Uri>>() {

    init {
        require(maxItems > 1) { "GalleryImagePickerContract requires at least two items" }
    }

    override fun createIntent(context: Context, input: Unit): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = "image/*"
                putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, maxItems)
            }
        } else {
            Intent(Intent.ACTION_PICK).apply {
                setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK || intent == null) return emptyList()

        val selectedUris = buildList {
            intent.clipData?.let { clipData ->
                repeat(clipData.itemCount) { index -> add(clipData.getItemAt(index).uri) }
            }
            intent.data?.let(::add)
        }
        return selectedUris.distinct().take(maxItems)
    }
}
