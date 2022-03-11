package com.example.facerecognition.FaceRecognition

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class FaceNet(assetManager: AssetManager) {
    private val intValues = IntArray(IMAGE_HEIGHT * IMAGE_WIDTH)
    private val imgData: ByteBuffer?
    private var tfliteModel: MappedByteBuffer?
    private var tflite: Interpreter?
    private val tfliteOptions = Interpreter.Options()
    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        if (imgData == null) {
            return
        }
        imgData.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // Convert the image to floating point.
        var pixel = 0
        for (i in 0 until IMAGE_HEIGHT) {
            for (j in 0 until IMAGE_WIDTH) {
                val `val` = intValues[pixel++]
                addPixelValue(`val`)
            }
        }
    }

    private fun addPixelValue(pixelValue: Int) {
        //imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        //imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        //imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        imgData!!.putFloat((pixelValue shr 16 and 0xFF) / 255.0f)
        imgData.putFloat((pixelValue shr 8 and 0xFF) / 255.0f)
        imgData.putFloat((pixelValue and 0xFF) / 255.0f)
    }

    fun inspectModel() {
        val tag = "Model Inspection"
        Log.i(tag, "Number of input tensors: " + tflite!!.inputTensorCount.toString())
        Log.i(tag, "Number of output tensors: " + tflite!!.outputTensorCount.toString())
        Log.i(tag, tflite!!.getInputTensor(0).toString())
        Log.i(tag, "Input tensor data type: " + tflite!!.getInputTensor(0).dataType())
        Log.i(tag, "Input tensor shape: " + Arrays.toString(tflite!!.getInputTensor(0).shape()))
        Log.i(tag, "Output tensor 0 shape: " + Arrays.toString(tflite!!.getOutputTensor(0).shape()))
    }

    private fun resizedBitmap(bitmap: Bitmap, height: Int, width: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun croppedBitmap(
        bitmap: Bitmap,
        upperCornerX: Int,
        upperCornerY: Int,
        height: Int,
        width: Int
    ): Bitmap {
        return Bitmap.createBitmap(bitmap, upperCornerX, upperCornerY, width, height)
    }

    private fun run(bitmap: Bitmap): Array<FloatArray> {
        var bitmap = bitmap
        bitmap = resizedBitmap(bitmap, IMAGE_HEIGHT, IMAGE_WIDTH)
        convertBitmapToByteBuffer(bitmap)
        val embeddings = Array(1) { FloatArray(512) }
        tflite!!.run(imgData, embeddings)
        return embeddings
    }

    fun getSimilarityScore(face1: Bitmap, face2: Bitmap): String {
        val face1_embedding = run(face1)
        val face2_embedding = run(face2)
        var distance = 0.0
        val result: String
        for (i in 0 until EMBEDDING_SIZE) {
            distance += ((face1_embedding[0][i] - face2_embedding[0][i]) * (face1_embedding[0][i] - face2_embedding[0][i])).toDouble()
        }
        distance = Math.sqrt(distance)
        result = if (distance > 6) {
            "Verified"
        } else {
            "Not Verified"
        }
        return result
    }

    fun close() {
        if (tflite != null) {
            tflite!!.close()
            tflite = null
        }
        tfliteModel = null
    } /*public float[][] runFloat(Bitmap bitmap){
        bitmap = resizedBitmap(bitmap, IMAGE_HEIGHT, IMAGE_WIDTH);
        float [][][][] floatTensor = convertBitmapToFloatTensor(bitmap);

        float[][] embeddings = new float[1][512];
        tflite.run(floatTensor, embeddings);

        return embeddings;
    }

    private float[][][][] convertBitmapToFloatTensor(Bitmap bitmap){
        float[][][][] floatTensor = new float[BATCH_SIZE][IMAGE_HEIGHT][IMAGE_WIDTH][NUM_CHANNELS];

        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < IMAGE_HEIGHT; i++){
            for (int j = 0; j < IMAGE_WIDTH; j++){
                final int val = intArray[pixel++];
                floatTensor[0][i][j][0] = (((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
                floatTensor[0][i][j][1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
                floatTensor[0][i][j][2] = ((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            }
        }

        return floatTensor;
    }*/

    companion object {
        private const val MODEL_PATH = "facenet.tflite"
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 127.5f
        private const val BATCH_SIZE = 1
        private const val IMAGE_HEIGHT = 160
        private const val IMAGE_WIDTH = 160
        private const val NUM_CHANNELS = 3
        private const val NUM_BYTES_PER_CHANNEL = 4
        private const val EMBEDDING_SIZE = 512
    }

    init {
        tfliteModel = loadModelFile(assetManager)
        tflite = Interpreter(tfliteModel!!, tfliteOptions)
        imgData = ByteBuffer.allocateDirect(
            BATCH_SIZE
                    * IMAGE_HEIGHT
                    * IMAGE_WIDTH
                    * NUM_CHANNELS
                    * NUM_BYTES_PER_CHANNEL
        )
        imgData.order(ByteOrder.nativeOrder())
    }
}