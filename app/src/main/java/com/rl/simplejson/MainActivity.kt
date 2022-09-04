package com.rl.simplejson

import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.ArrayAdapter
import com.google.android.material.snackbar.Snackbar
import com.rl.simplejson.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var list = mutableListOf("A","B","C")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivityForResult(intent, 1024)
        }

        /* -------------------------------------------------- */

        binding.json.typeface = Typeface.createFromAsset(assets,"fonts/Consolas.ttf")
        binding.filePath.typeface = Typeface.createFromAsset(assets,"fonts/Consolas.ttf")

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        binding.ListView.adapter = adapter

        binding.filePath.setOnClickListener {

            val intent = Intent()
                .setType("application/json")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, "Select a file"), 256)
        }

        binding.fabBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        Snackbar.make(binding.CoordinatorLayout, "Back Pressed", Snackbar.ANIMATION_MODE_SLIDE).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 256 && resultCode == RESULT_OK) {
            val selectedFile = data?.data?.path?.substring(18)
            Snackbar.make(binding.CoordinatorLayout, "$selectedFile", Snackbar.ANIMATION_MODE_SLIDE).show()
        }
    }
}