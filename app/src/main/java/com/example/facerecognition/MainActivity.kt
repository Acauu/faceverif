package com.example.facerecognition

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.Button
import com.example.facerecognition.FaceDetection.MTCNN
import android.widget.TextView
import android.net.Uri
import android.util.Log
import java.lang.Exception
import android.os.Bundle
import android.content.pm.PackageManager
import android.view.View
import android.content.Intent
import android.widget.Toast
import com.example.facerecognition.FaceRecognition.FaceNet
import java.io.IOException
import android.content.ContentValues
import android.provider.MediaStore
import android.app.Activity
import android.content.ActivityNotFoundException
import android.graphics.BitmapFactory
import java.io.FileNotFoundException
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.startActivityForResult
//import com.google.android.gms.common.internal.Constants
import org.tensorflow.lite.Interpreter

class MainActivity : AppCompatActivity() {
    private var image: Bitmap? = null
    private var image2: Bitmap? = null
    private var imageView: ImageView? = null
    private var imageView2: ImageView? = null
    private var button: Button? = null
    private val mtcnn: MTCNN? = null
    private var textView: TextView? = null
    var image_uri: Uri? = null
    var image_uri2: Uri? = null
    private fun cropFace(bitmap: Bitmap, mtcnn: MTCNN): Bitmap? {
        var croppedBitmap: Bitmap? = null
        try {
            val boxes = mtcnn.detectFaces(bitmap, 10)
            Log.i("MTCNN", "No. of faces detected: " + boxes.size)
            val left = boxes[0].left()
            val top = boxes[0].top()
            val x = boxes[0].left()
            val y = boxes[0].top()
            var width = boxes[0].width()
            var height = boxes[0].height()
            if (y + height >= bitmap.height) height -= y + height - (bitmap.height - 1)
            if (x + width >= bitmap.width) width -= x + width - (bitmap.width - 1)
            Log.i("MTCNN", "Final x: " + (x + width).toString())
            Log.i("MTCNN", "Width: " + bitmap.width)
            Log.i("MTCNN", "Final y: " + (y + width).toString())
            Log.i("MTCNN", "Height: " + bitmap.width)
            croppedBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return croppedBitmap
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.imageView)
        imageView2 = findViewById(R.id.imageView2)
        button = findViewById(R.id.button)
        textView = findViewById(R.id.textView)
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            val permission =
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            requestPermissions(permission, 112)
        }
        imageView?.setOnLongClickListener {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED
            ) {
                val permission = arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                requestPermissions(permission, MY_CAMERA_PERMISSION_CODE)
            } else {
                openCamera()
            }
            false
        }

        imageView2!!.setOnClickListener { view: View? ->
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            startActivityForResult(photoPickerIntent, PICK_IMAGEVIEW2_CONTENT)
        }


//        imageView2!!.setOnClickListener { view: View? ->
//            val photoPickerIntent = Intent(Intent.ACTION_PICK)
//            photoPickerIntent.type = "image/*"
//            getContent.launch("image/*")
//        }

//        imageView2!!.setOnClickListener { view: View? ->
//            val photoPickerIntent = Intent(Intent.ACTION_PICK)
//            photoPickerIntent.type = "image/*"
//            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
//                    result-> onActivityResult(
//                PICK_IMAGEVIEW2_CONTENT,
//                result)}.launch(intent)
//        }
        button?.setOnClickListener { view: View? ->
            if (image == null || image2 == null) {
                Toast.makeText(
                    applicationContext,
                    "One of the images haven't been set yet.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val mtcnn = MTCNN(assets)
                val face1 = cropFace(image!!, mtcnn)
                val face2 = cropFace(image2!!, mtcnn)
                mtcnn.close()
                var facenet: FaceNet? = null
                try {
                    facenet = FaceNet(assets)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (face1 != null) imageView?.setImageBitmap(face1) else Log.i(
                    "detect",
                    "Couldn't crop image 1."
                )
                if (face2 != null) imageView2?.setImageBitmap(face2) else Log.i(
                    "detect",
                    "Couldn't crop image 2."
                )
                if (face1 != null && face2 != null) {
                    val score = facenet!!.getSimilarityScore(face1, face2)
                    Log.i("score", score)
                    //Toast.makeText(MainActivity.this, "Similarity score: " + score, Toast.LENGTH_LONG).show();
                    val text = String.format("Similarity score = $score")
                    textView?.setText(text)
                }
                facenet!!.close()
            }
        }
    }

//    private fun openActivityForResult(photoPickerIntent: Intent, PICK_IMAGEVIEW2_CONTENT: Int,) {
//        startForResult.launch(Intent(this,MainActivity::class.java))
//
//    }
//    val startForResult= registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
//        result: ActivityResult ->
//        if(result.resultCode == Activity.RESULT_OK){
//            val imageUri = result!!.data
////            val imageUri = data!!.data
//            val imageStream = contentResolver.openInputStream(imageUri!!)
//            image2 = BitmapFactory.decodeStream(imageStream)
//            imageView2!!.setImageBitmap(image2)
//            textView!!.text = ""
//        }
//    }

//    val imageUri = data!!.data
//    val imageStream = contentResolver.openInputStream(imageUri!!)

//    private fun onActivityResult(requestCode: Int, result: ActivityResult) {
//        if (result.resultCode == Activity.RESULT_OK &&  requestCode == PICK_IMAGEVIEW2_CONTENT) {
//            val imageUri = result.data
//            val imageStream = contentResolver.openInputStream(imageUri)
//            image2 = BitmapFactory.decodeStream(imageStream)
//            imageView2!!.setImageBitmap(image2)
//            textView!!.text = ""
//        }
//    }

//    val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
//        if (success) {
//            // The image was saved into the given Uri -> do something with it
//        }
//    }



    private fun openCamera() {

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }


    private fun openCamera2() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture 2 ")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera 2")
        image_uri2 = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri2)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

//override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//    super.onActivityResult(requestCode, resultCode, data)
//    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//        val image = data?.extras?.get("data") as Bitmap
//        imageView?.setImageBitmap(image)
//    }
//}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == RESULT_OK) {

            if (requestCode == IMAGE_CAPTURE_CODE && resultCode == RESULT_OK) {
                image = uriToBitmap(image_uri)
                imageView!!.setImageBitmap(image)
                textView!!.text = ""
            }
        } else if (requestCode == PICK_IMAGEVIEW2_CONTENT && resultCode == RESULT_OK) {
            try {
                val imageUri = data!!.data
                val imageStream = contentResolver.openInputStream(imageUri!!)
                image2 = BitmapFactory.decodeStream(imageStream)
                imageView2!!.setImageBitmap(image2)
                textView!!.text = ""
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Error loading gallery image.", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun uriToBitmap(selectedFileUri: Uri?): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(
                selectedFileUri!!, "r"
            )
            val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val photo = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return photo
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun uriToBitmap2(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image2 = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image2
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    companion object {
        private const val PICK_IMAGEVIEW_CONTENT = 1
        private const val PICK_IMAGEVIEW2_CONTENT = 2
        private const val CAMERA_REQUEST = 1888
        private const val MY_CAMERA_PERMISSION_CODE = 100
        private const val RESULT_LOAD_IMAGE = 123
        const val IMAGE_CAPTURE_CODE = 654
        val REQUEST_IMAGE_CAPTURE = 1
    }
}