package com.example.myinstructions.util

import android.graphics.Bitmap
import com.example.myinstructions.ui.share.ShareableTask
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object QrCodeHelper {

    private const val FORMAT_VERSION = 1
    private const val QR_SIZE = 600

    fun toJson(tasks: List<ShareableTask>): String {
        val root = JSONObject()
        root.put("v", FORMAT_VERSION)
        val tasksArray = JSONArray()
        for (task in tasks) {
            val obj = JSONObject()
            obj.put("n", task.name)
            val instrArray = JSONArray()
            task.instructions.forEach { instrArray.put(it) }
            obj.put("i", instrArray)
            tasksArray.put(obj)
        }
        root.put("tasks", tasksArray)
        return root.toString()
    }

    fun fromJson(json: String): List<ShareableTask>? {
        return try {
            val root = JSONObject(json)
            val version = root.optInt("v", -1)
            if (version < 1 || version > FORMAT_VERSION) return null
            val tasksArray = root.optJSONArray("tasks") ?: return null
            val tasks = mutableListOf<ShareableTask>()
            for (i in 0 until tasksArray.length()) {
                val obj = tasksArray.getJSONObject(i)
                val name = obj.optString("n").takeIf { it.isNotBlank() } ?: continue
                val instrArray = obj.optJSONArray("i") ?: JSONArray()
                val instructions = (0 until instrArray.length()).map { instrArray.getString(it) }
                tasks.add(ShareableTask(name, instructions))
            }
            tasks
        } catch (e: JSONException) {
            null
        }
    }

    fun encode(tasks: List<ShareableTask>): Bitmap? {
        return try {
            val json = toJson(tasks)
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = MultiFormatWriter().encode(
                json, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints
            )
            BarcodeEncoder().createBitmap(bitMatrix)
        } catch (e: WriterException) {
            null
        }
    }

    fun decode(json: String): List<ShareableTask>? = fromJson(json)
}
