package com.example.remotefilebox

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.remotefilebox.databinding.ActivityAllfilesonserverBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AllFilesOnServer : AppCompatActivity() {

    private lateinit var binding : ActivityAllfilesonserverBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: UploadedListRecyclerViewAdapter
    private val fileList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAllfilesonserverBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.allFilesMenu.setOnClickListener(){
            startActivity(Intent(this,MainActivity::class.java))
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        fileAdapter = UploadedListRecyclerViewAdapter(fileList)
        recyclerView.adapter = fileAdapter

        fetchUploadedFiles()
    }

    private fun fetchUploadedFiles() {
        ConnectionBuilderClient.instance.uploadedFileList().enqueue(object :
            Callback<UploadList> {
            override fun onResponse(call: Call<UploadList>, response: Response<UploadList>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.success) {
                            fileList.clear()
                            fileList.addAll(it.files)
                            fileAdapter.notifyDataSetChanged()
                        } else {
                            Toast.makeText(this@AllFilesOnServer, "Failed to load files", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this@AllFilesOnServer, "Failed to load files", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UploadList>, t: Throwable) {
                Log.e("AllFilesOnServer", "Error fetching files", t)
                Toast.makeText(this@AllFilesOnServer, "Error fetching files: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}