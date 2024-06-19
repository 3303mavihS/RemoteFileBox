package com.example.remotefilebox

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.remotefilebox.databinding.ActivityMainBinding
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class MainActivity : AppCompatActivity() {

    //declaring variable for binding the elements to the activity view
    private lateinit var binding : ActivityMainBinding

    //store uris of picked images
    private lateinit var selectedImages: ArrayList<Uri>

    //store urls of uploaded images
    private var uploadedImagesURLs: MutableList<ImageInfo> = mutableListOf()

    //request code to pick image(s)
    private val PICK_IMAGES_CODE = 159

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        /**
         * declaring variable for binding the elements to the activity view
         */
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //init list
        selectedImages = ArrayList()

        binding.selectImages.setOnClickListener(){
            selectImages()
        }
        binding.uploadImages.setOnClickListener(){
            uploadImages(selectedImages)
        }
        binding.allFilesMenu.setOnClickListener(){
            val intent = Intent(this,AllFilesOnServer::class.java)
            startActivity(intent)
        }
    }

    private fun selectImages(){
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent,"Select Image(s)"),PICK_IMAGES_CODE)
    }

    private fun uploadImages(uris : ArrayList<Uri>){
        lifecycleScope.launch {
            val files = uris.mapNotNull { uri ->
                getRealPathFromURI(this@MainActivity, uri)?.let { File(it) }
            }

            val compressedFiles = compressImages(this@MainActivity, files)

            val parts = compressedFiles.map { file ->
                val requestFile = ProgressRequestBody(file, "image/*", object : ProgressListener {
                    override fun onProgressUpdate(percentage: Int) {
                        runOnUiThread {
                            binding.progressBar.progress = percentage
                        }
                    }
                })
                MultipartBody.Part.createFormData("images[]", file.name, requestFile)
            }

            if (parts.isEmpty()) {
                Toast.makeText(this@MainActivity, "No valid files found to upload", Toast.LENGTH_SHORT).show()
                return@launch
            }

            ConnectionBuilderClient.instance.uploadSelectedImages(parts).enqueue(object :
                Callback<UploadResponse> {
                override fun onResponse(
                    call: Call<UploadResponse>,
                    response: Response<UploadResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val uploadedFiles = response.body()?.files
                        binding.responseDiv.visibility = View.VISIBLE
                        binding.response.append("Completed")
                        var urlCount = 1
                        binding.urls.text = ""
                        if (uploadedImagesURLs.isNotEmpty())
                            uploadedImagesURLs.clear()
                        uploadedFiles?.forEach { url ->
                            uploadedImagesURLs.add(ImageInfo(url))
                            binding.urls.append("\nImage$urlCount URL : ${uploadedImagesURLs[urlCount - 1].url}")
                            urlCount++
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Upload failed", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    Log.e("MainActivity", "Upload failed : ${t.message}", t)
                    Toast.makeText(
                        this@MainActivity,
                        "Upload failed: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGES_CODE){
            if (resultCode == Activity.RESULT_OK){
                if(selectedImages.isNotEmpty())
                    selectedImages.clear()
                if (data!!.clipData != null){
                    //picked multiple images
                    //get number of picked images
                    val count = data.clipData!!.itemCount
                    if(count>=6){
                        for (i in 0 until count){
                            if(i<6){
                                val imageUri = data.clipData!!.getItemAt(i).uri
                                //add image to list
                                selectedImages.add(imageUri)
                            }
                        }
                        binding.selectedImages.visibility = View.VISIBLE
                        binding.msg.visibility = View.GONE
                        binding.uploadImages.isClickable = true
                        binding.progressBar.visibility = View.VISIBLE
                        binding.selectedImage1.setImageURI(selectedImages[0])
                        binding.selectedImage2.setImageURI(selectedImages[1])
                        binding.selectedImage3.setImageURI(selectedImages[2])
                        binding.selectedImage4.setImageURI(selectedImages[3])
                        binding.selectedImage5.setImageURI(selectedImages[4])
                        binding.selectedImage6.setImageURI(selectedImages[5])
                    }
                    else{
                        Toast.makeText(this,"You have selected $count images.",Toast.LENGTH_SHORT).show()
                    }
                }
                else{
                    //picked single image
                    val imageUri = data.data
                    binding.selectedImage1.setImageURI(imageUri)
                }

            }

        }
    }

    private fun getRealPathFromURI(context: Context, uri: Uri): String? {
        var result: String? = null
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            if (idx >= 0) {
                result = cursor.getString(idx)
            }
            cursor.close()
        }
        return result
    }

    // Function to compress a single image file
    private suspend fun compressImageFile(context: Context, file: File): File {
        return Compressor.compress(context, file) {
            default(width = 800, quality = 75, format = Bitmap.CompressFormat.JPEG)
        }
    }

    // Compress multiple images and return the list of compressed files
    private suspend fun compressImages(context: Context, files: List<File>): List<File> {
        return files.map { compressImageFile(context, it) }
    }
}