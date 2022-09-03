package com.rl.simplejson

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.ArrayAdapter
import com.rl.simplejson.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val list = mutableListOf("A","B","C")
    private var selectedFile: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivityForResult(intent, 1024)
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        binding.listview.adapter = adapter

        binding.fabOpen.setOnClickListener {
            val intent = Intent()
                .setType("application/json")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, "Select a file"), 256)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 256 && resultCode == RESULT_OK) {
            selectedFile = data?.data?.path?.substring(18)
        }
    }

    private fun load() {

    }

    private fun update() {

    }
}