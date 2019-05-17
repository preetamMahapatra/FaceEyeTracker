package com.pm.faceeyetracker

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import android.widget.VideoView
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import kotlinx.android.synthetic.main.activity_main.*

import java.io.IOException
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    //For looking logs
    private lateinit var adapter: ArrayAdapter<*>
    private var list = ArrayList<String>()

    private var cameraSource: CameraSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
            Toast.makeText(this, "Grant Permission and restart app", Toast.LENGTH_SHORT).show()
        } else {

            adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
            videoView.setVideoURI(Uri.parse("android.resource://" + packageName + "/" + R.raw.jeep))
            videoView.start()
            createCameraSource()
        }
    }

    //This class will use google vision api to detect eyes
    private inner class EyesTracker internal constructor() : Tracker<Face>() {

        private val THRESHOLD = 0.75f

        override fun onUpdate(detections: Detector.Detections<Face>?, face: Face?) {
            if (face!!.isLeftEyeOpenProbability > THRESHOLD || face.isRightEyeOpenProbability > THRESHOLD) {
                Log.i(TAG, "onUpdate: Eyes Detected")
                showStatus("Eyes Detected and open, so video continues")
                if (!videoView.isPlaying)
                    videoView.start()

            } else {
                if (videoView.isPlaying)
                    videoView.pause()

                showStatus("Eyes Detected and closed, so video paused")
            }
        }

        override fun onMissing(detections: Detector.Detections<Face>?) {
            super.onMissing(detections)
            showStatus("Face Not Detected yet!")
        }

        override fun onDone() {
            super.onDone()
        }
    }

    private inner class FaceTrackerFactory internal constructor() : MultiProcessor.Factory<Face> {

        override fun create(face: Face): Tracker<Face> {
            return EyesTracker()
        }
    }

    fun createCameraSource() {
        val detector = FaceDetector.Builder(this)
            .setTrackingEnabled(true)
            .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
            .setMode(FaceDetector.FAST_MODE)
            .build()
        detector.setProcessor(MultiProcessor.Builder(FaceTrackerFactory()).build())

        cameraSource = CameraSource.Builder(this, detector)
            .setRequestedPreviewSize(1024, 768)
            .setFacing(CameraSource.CAMERA_FACING_FRONT)
            .setRequestedFps(30.0f)
            .build()

        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling

                return
            }
            cameraSource!!.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun onResume() {
        super.onResume()
        if (cameraSource != null) {
            try {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                cameraSource!!.start()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    override fun onPause() {
        super.onPause()
        if (cameraSource != null) {
            cameraSource!!.stop()
        }
        if (videoView.isPlaying) {
            videoView.pause()
        }
    }

    fun showStatus(message: String) {
        runOnUiThread { textView.setText(message) }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (cameraSource != null) {
            cameraSource!!.release()
        }
    }

    companion object {

        private val TAG = "MainActivity"
    }
}