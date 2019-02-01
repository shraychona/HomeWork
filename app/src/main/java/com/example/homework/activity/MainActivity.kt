/*
 * Copyright (C) 2018 CyberAgent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.homework.activity

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.homework.R
import ly.img.android.pesdk.ui.utils.PermissionRequest
import androidx.annotation.NonNull
import ly.img.android.pesdk.backend.model.state.EditorSaveSettings
import ly.img.android.pesdk.backend.model.constant.Directory.DCIM
import ly.img.android.pesdk.assets.font.basic.FontPackBasic
import ly.img.android.pesdk.backend.model.constant.Directory
import ly.img.android.pesdk.ui.model.state.UiConfigText
import ly.img.android.pesdk.backend.model.state.manager.SettingsList
import android.widget.Toast
import android.provider.MediaStore
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.app.ActivityCompat.startActivityForResult
import ly.img.android.pesdk.backend.model.state.EditorLoadSettings
import ly.img.android.pesdk.ui.activity.ImgLyIntent
import android.os.Environment.getExternalStorageDirectory
import android.util.Log
import ly.img.android.pesdk.ui.activity.PhotoEditorBuilder
import ly.img.android.serializer._3._0._0.PESDKFileWriter
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity(), PermissionRequest.Response {

    companion object {
        const val PESDK_RESULT = 1
        const val GALLERY_RESULT = 2
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openSystemGalleryToSelectAnImage()
    }

    // Important permission request for Android 6.0 and above, don't forget to add this!
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        PermissionRequest.onRequestPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun permissionDenied() {

    }

    override fun permissionGranted() {

    }

    private fun createPesdkSettingsList(): SettingsList {


        // Create a empty new SettingsList and apply the changes on this referance.
        val settingsList = SettingsList()

        // If you include our asset Packs and you use our UI you also need to add them to the UI,
        // otherwise they are only available for the backend
        // See the specific feature sections of our guides if you want to know how to add our own Assets.


//        settingsList.getSettingsModel(UiConfigText::class.java).setFontList(
//            FontPackBasic.getFontPack()
//        )

        // Set custom editor image export settings
//        settingsList.getSettingsModel(EditorSaveSettings::class.java)
//            .setExportDir(Directory.DCIM, "SomeFolderName")
//            .setExportPrefix("result_").savePolicy = EditorSaveSettings.SavePolicy.RETURN_ALWAYS_ONLY_OUTPUT

        return settingsList
    }

    private fun openSystemGalleryToSelectAnImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, GALLERY_RESULT)
        } else {
            Toast.makeText(
                this,
                "No Gallery APP installed",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openEditor(inputImage: Uri) {
        val settingsList = createPesdkSettingsList()

        // Set input image
        settingsList.getSettingsModel(EditorLoadSettings::class.java).imageSource = inputImage

        PhotoEditorBuilder(this)
            .setSettingsList(settingsList)
            .startActivityForResult(this, PESDK_RESULT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == GALLERY_RESULT) {
            // Open Editor with some uri in this case with an image selected from the system gallery.
            val selectedImage = data!!.data
            openEditor(selectedImage)

        } else if (resultCode == Activity.RESULT_OK && requestCode == PESDK_RESULT) {
            // Editor has saved an Image.
            val resultURI = data!!.getParcelableExtra<Uri>(ImgLyIntent.RESULT_IMAGE_URI)
            val sourceURI = data.getParcelableExtra<Uri>(ImgLyIntent.SOURCE_IMAGE_URI)

            // Scan result uri to show it up in the Gallery
            if (resultURI != null) {
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).setData(resultURI))
            }

            // Scan source uri to show it up in the Gallery
            if (sourceURI != null) {
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).setData(sourceURI))
            }

            Log.i("PESDK", "Source image is located here " + sourceURI!!)
            Log.i("PESDK", "Result image is located here " + resultURI!!)

            // TODO: Do something with the result image

            // OPTIONAL: read the latest state to save it as a serialisation
            val lastState = data.getParcelableExtra<SettingsList>(ImgLyIntent.SETTINGS_LIST)
            try {
                PESDKFileWriter(lastState).writeJson(
                    File(
                        getExternalStorageDirectory(),
                        "serialisationReadyToReadWithPESDKFileReader.json"
                    )
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }

        } else if (resultCode == Activity.RESULT_CANCELED && requestCode == PESDK_RESULT) {
            // Editor was canceled
            val sourceURI = data!!.getParcelableExtra<Uri>(ImgLyIntent.SOURCE_IMAGE_URI)
            // TODO: Do something with the source...
        }
    }
}