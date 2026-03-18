package com.example.myinstructions.util

import android.graphics.Bitmap
import android.util.Base64
import com.example.myinstructions.ui.share.ShareableTask
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterOutputStream

object QrCodeHelper {

    private const val FORMAT_VERSION = 1
    private const val QR_SIZE = 600

    /** Prefix that marks a compressed+Base64 payload vs a legacy plain-JSON payload. */
    private const val COMPRESSED_PREFIX = "z:"

    // ── Serialisation ────────────────────────────────────────────────────────

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

    // ── Compression helpers ───────────────────────────────────────────────────

    private fun compress(input: String): ByteArray {
        val bos = ByteArrayOutputStream()
        DeflaterOutputStream(bos).use { it.write(input.toByteArray(Charsets.UTF_8)) }
        return bos.toByteArray()
    }

    private fun decompress(input: ByteArray): String {
        val bos = ByteArrayOutputStream()
        InflaterOutputStream(bos).use { it.write(input) }
        return bos.toString("UTF-8")
    }

    /** Returns the string that will be encoded into the QR code. */
    private fun toQrString(tasks: List<ShareableTask>): String {
        val compressed = compress(toJson(tasks))
        return COMPRESSED_PREFIX + Base64.encodeToString(compressed, Base64.NO_WRAP)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun encode(tasks: List<ShareableTask>): Bitmap? {
        return try {
            val payload = toQrString(tasks)
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = MultiFormatWriter().encode(
                payload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints
            )
            BarcodeEncoder().createBitmap(bitMatrix)
        } catch (e: WriterException) {
            null
        }
    }

    /**
     * Decodes a raw QR string. Handles both the current compressed format
     * (prefixed with "z:") and legacy plain-JSON payloads for backward compatibility.
     */
    fun decode(raw: String): List<ShareableTask>? {
        return try {
            val json = if (raw.startsWith(COMPRESSED_PREFIX)) {
                val b64 = raw.removePrefix(COMPRESSED_PREFIX)
                val compressed = Base64.decode(b64, Base64.NO_WRAP)
                decompress(compressed)
            } else {
                raw // legacy plain-JSON QR
            }
            fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
