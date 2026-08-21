package com.digitaldiscipline.spike.intervention.vision

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

/**
 * CameraX Image Analyzer for real-time On-Device Pose Detection.
 * Processes camera frames at 30 FPS, emits skeletal landmark detections, and releases frames immediately.
 */
class CameraPoseAnalyzer(
    private val onPoseDetected: (pose: Pose, imageWidth: Int, imageHeight: Int, rotationDegrees: Int) -> Unit
) : ImageAnalysis.Analyzer {

    // Fast stream mode for real-time video feeds
    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .setPreferredHardwareConfigs(PoseDetectorOptions.CPU_GPU)
        .build()

    private val poseDetector = PoseDetection.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    onPoseDetected(pose, imageProxy.width, imageProxy.height, rotationDegrees)
                }
                .addOnFailureListener {
                    // Silently ignore transient frame drops
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    fun close() {
        poseDetector.close()
    }
}
