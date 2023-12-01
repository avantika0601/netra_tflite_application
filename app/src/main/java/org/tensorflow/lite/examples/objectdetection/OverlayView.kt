/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import java.util.LinkedList
import kotlin.math.max
import org.tensorflow.lite.task.vision.detector.Detection
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.util.Log
import android.os.Handler
import android.os.Looper
import java.util.UUID
//import java.util.LinkedList
import java.util.Queue
import android.os.Bundle
import android.speech.tts.UtteranceProgressListener


class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs),TextToSpeech.OnInitListener {

    private var results: List<Detection> = LinkedList<Detection>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: Float = 1f

    private var bounds = Rect()
    private var textToSpeech: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())
    private val objectQueue: Queue<String> = LinkedList<String>()
    private var isSpeaking = false
    init {
        initPaints()
        initTextToSpeech()
    }
    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(context, this)
        textToSpeech?.language = Locale.US

        // Set up UtteranceProgressListener to detect when the speech finishes
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // Not needed for this implementation
            }

            override fun onDone(utteranceId: String?) {
                // Speak the next object in the queue after the previous speech finishes
                isSpeaking=false
                speakNext()
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Text-to-speech error")
                // Speak the next object in the queue even if an error occurs
                isSpeaking=false
                speakNext()
            }
        })
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // TextToSpeech initialization successful
            textToSpeech?.language = Locale.US
        } else {
            Log.e(TAG, "TextToSpeech initialization failed")
        }
    }
    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        for (result in results) {
            val boundingBox = result.boundingBox

            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

            // Draw bounding box around detected objects
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)
            val objectName = result.categories[0].label
            objectQueue.add(objectName)

            // If the queue contains only this object, initiate the speech
            if (objectQueue.size == 1 && !isSpeaking) {
                speakNext()
            }
//            handler.postDelayed({
//                // Speak the detected object's name
//                speak(objectName)
//            }, 2000)
            // Create text to display alongside detected objects
            val drawableText =
                result.categories[0].label + " " +
                        String.format("%.2f", result.categories[0].score)
//            speak(objectName)


            // Draw rect behind disp
            // lay text
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + Companion.BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + Companion.BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // Draw text for detected object
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }
    }
    private fun speakNext() {
        // If the queue is not empty, initiate speech for the next object
        if (objectQueue.isNotEmpty()) {
            val nextObject = objectQueue.poll()
            handler.postDelayed({ speak(nextObject) }, 2000)
        }
    }
    private fun speak(text: String) {
        if (text.isNotEmpty()) {
            isSpeaking = true
            // Use a unique utterance ID to differentiate between speech utterances
            val utteranceId = UUID.randomUUID().toString()
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

            // Speak the text with the given utterance ID
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }
    fun setResults(
      detectionResults: MutableList<Detection>,
      imageHeight: Int,
      imageWidth: Int,
    ) {
        results = detectionResults

        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }
//    private fun speak(text: String) {
//        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
//    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
        private const val TAG = "ObjectDetection"
    }
}
