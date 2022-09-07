package com.rl.simplejson

import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.widget.ArrayAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rl.simplejson.databinding.ActivityMainBinding
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var list = mutableListOf<Any>("Press Volume Down Button","to","Ch00se a File")

    private var example = "{\"name\":\"Ray\",\"age\":18}".trim()
    private var copy = example

    private lateinit var format: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivityForResult(intent, 1024)
        }

        /* -------------------------------------------------- */

        bindingSetting()
        algorithm()
    }


    private fun algorithm() {
        if(copy[0]=='{') {
            copy = copy.substring(1,copy.length-1)

            var mapTemp = mutableListOf<String>()
            var json = mutableMapOf<String, Any>("name" to "{2}", "age" to 18)
            
            var temp = ""

            {
                for (i in copy) {
                    if (i == ':') {
                        mapTemp.add(temp)
                        temp = ""
                    } else if (i == ',') {
                        json.put(mapTemp[0], mapTemp[1])
                        mapTemp.clear()
                    } else {
                        temp += i
                    }
                }
            }

            list.clear()
            for(j in json) {
                list.add("${j.key}:${j.value}")
            }
            adapterUpdate()
        }
        else {
            var json = mutableListOf<Any>()
        }
    }

    private fun bindingSetting() {
        binding.json.typeface = Typeface.createFromAsset(assets,"fonts/Consolas.ttf")
        binding.filePath.typeface = Typeface.createFromAsset(assets,"fonts/Consolas.ttf")

        adapterUpdate()

        binding.filePath.text = null

        binding.fabBack.setOnClickListener {

        }
        binding.fabBack.setOnLongClickListener {
            return@setOnLongClickListener true
        }

        binding.fabCurly.setOnClickListener {

        }
        binding.fabText.setOnClickListener {

        }
        binding.fabSquare.setOnClickListener {

        }

    }

    private fun adapterUpdate() {
        binding.ListView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
    }

    override fun onBackPressed() {
        Snackbar.make(binding.CoordinatorLayout, "onBackPressed", Snackbar.ANIMATION_MODE_SLIDE).show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val intent = Intent()
                .setType("application/json")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, "Select a file"), 256)
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
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