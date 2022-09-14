package com.rl.simplejson

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.KeyEvent
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.rl.simplejson.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val prefixPath = "/storage/emulated/0"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var adapterList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private var filePath = ""

    private var masterString = ""
    private var masterCDT = ""
    
    private var history = mutableListOf<String>()

    private var nowString = ""
    private var nowFingerPrint = ' '
    private var nowCDT = ""
    private var nowObject = mutableMapOf<String,String>()
    private var nowArray = mutableListOf<String>()
    private var nowDict = mutableListOf<String>()

    private var openResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == RESULT_OK) {
            filePath = it.data?.data?.path!!.split(':')[1]
            binding.path.text = filePath

            // 準備masterString
            run {
                masterString = File(prefixPath,filePath).readText()
                for (i in masterString) {
                    if (i !in " \t\r\n") nowString += i
                }
                masterString = nowString
            }

            // 設定監聽事件
            run {
                binding.listView.setOnItemClickListener { _, _, i, _ ->
                    nowString = if(nowCDT=="Object") nowObject[nowDict[i]]!! else nowArray[i]
                    nowFingerPrint = nowString[0]
                    if (nowFingerPrint in "{[") {
                        history.add(nowString)
                        plJson(nowString)
                    }
                }
                binding.fabBack.setOnClickListener {
                    onBackPressed()
                }
                binding.fabBack.setOnLongClickListener {
                    nowFingerPrint = masterString[0]
                    plJson(masterString)
                    return@setOnLongClickListener true
                }
            }
            nowFingerPrint = masterString[0]
            plJson(masterString)
            masterCDT = nowCDT
        }
    }

    private var newResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == RESULT_OK) {
            filePath = it.data?.data?.path!!.split(':')[1] + '/'
            binding.path.text = filePath
            binding.CDT.text = getString(R.string.folder)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 請求權限
        if(!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){}.launch(intent)
        }

        // 準備view & adapterList
        run {
            adapterList.add("Key Up : New")
            adapterList.add("Long Key Up : New File Under Current Path")
            adapterList.add("Key Down : Open")

            adapter = ArrayAdapter(this, R.layout.list, adapterList)
            binding.listView.adapter = adapter
            adapter.notifyDataSetChanged()

            binding.path.text = getString(R.string.path)
            binding.CDT.text = getString(R.string.empty)
        }
    }

    // Parse & Layout Json
    private fun plJson(jsonStringOrigin: String) {

        val jsonString = jsonStringOrigin.substring(1,jsonStringOrigin.length-1)
        val splitList = mutableListOf<String>()
        val commaList = mutableListOf<Int>()

        // 準備splitList
        run {
            var tempString = ""
            for (i in jsonString) {

                if (i in "{[") commaList.add(0)

                else if (i in "]}") {
                    for (j in commaList.size-1 downTo 0) {
                        if (commaList[j] == 0) {
                            commaList[j] = 1
                            break
                        }
                    }
                }
                else if (i == ',') {

                    // 這個逗號是否為最外層的(分水嶺)
                    var isWatershed = true
                    for (j in commaList) {
                        if (j == 0) {
                            isWatershed = false
                            break
                        }
                    }
                    if (isWatershed) {
                        splitList.add(tempString)
                        tempString = ""
                        continue
                    }
                }
                tempString += i
            }
            splitList.add(tempString)
        }

        // 準備nowObject & nowArray & nowCDT
        run {
            when (nowFingerPrint) {
                '{' -> {
                    nowCDT = "Object"
                    val map = mutableMapOf<String, String>()

                    for (i in splitList) {
                        val colonList = mutableListOf<String>()

                        // 找出最外層冒號
                        for (j in i.indices) {
                            if (i[j] == ':') {
                                colonList.add(i.substring(0, j))
                                colonList.add(i.substring(j + 1))
                                break
                            }
                        }
                        map[colonList[0]] = colonList[1]
                    }
                    nowObject = map
                }
                '[' -> {
                    nowCDT = "Array"
                    nowArray = splitList
                }
            }
            binding.CDT.text = nowCDT
        }

        // 準備adapterList
        run {
            if (nowCDT == "Object") {
                adapterList.clear()
                nowDict.clear()

                for (i in nowObject) {
                    nowDict.add(i.key)

                    var temp = "${i.key} : "
                    temp += if (i.value[0] in "{[") "${i.value[0]} ${lenJson(i.value)} ${i.value[i.value.length - 1]}" else i.value
                    adapterList.add(temp)
                }
            } else {
                adapterList.clear()

                for (i in nowArray) {
                    adapterList.add(if (i[0] in "{[") "${i[0]} ${lenJson(i)} ${i[i.length - 1]}" else i)
                }
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun lenJson(jsonString_: String): Int {

        val splitList = mutableListOf<String>()
        val commaList = mutableListOf<Int>()

        var tempJsonString: String
        var jsonString = jsonString_

        // 移除最外層括號
        run {
            for (i in jsonString.indices) {
                if (jsonString[i] in "{[") {
                    jsonString = jsonString.substring(i + 1)
                    break
                }
            }
            for (i in jsonString.length - 1 downTo 0) {
                if (jsonString[i] in "]}") {
                    jsonString = jsonString.substring(0, i)
                    break
                }
            }
        }

        // 準備splitList
        run {
            tempJsonString = ""
            for (i in jsonString) {

                if (i in "{[") commaList.add(0)

                else if (i in "]}") {
                    for (j in commaList.size-1 downTo 0) {
                        if (commaList[j] == 0) {
                            commaList[j] = 1
                            break
                        }
                    }
                }
                else if (i == ',') {

                    // 這個逗號是否為最外層的(分水嶺)
                    var isWatershed = true
                    for (j in commaList) {
                        if (j == 0) {
                            isWatershed = false
                            break
                        }
                    }
                    if (isWatershed) {
                        splitList.add(tempJsonString)
                        tempJsonString = ""
                        continue
                    }
                }
                tempJsonString += i
            }
            splitList.add(tempJsonString)
        }

        return splitList.size
    }

    override fun onBackPressed() {
        if (filePath != "") {
            nowString = try {
                history.removeLast()
                history[history.size-1]
            }
            catch (e: Exception) { masterString }

            nowFingerPrint = nowString[0]
            plJson(nowString)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when(keyCode) {
            // New
            KeyEvent.KEYCODE_VOLUME_UP -> {
                event!!.startTracking()
                true
            }
            // Open
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val intent = Intent().setType("application/json").setAction(Intent.ACTION_GET_CONTENT)
                openResultLauncher.launch(Intent.createChooser(intent,"Choose A File"))
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            nameNewFile()
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event!!.flags and KeyEvent.FLAG_CANCELED_LONG_PRESS == 0) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                newResultLauncher.launch(intent)
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun nameNewFile() {
        val fileNameLayout = LinearLayout(this)
        fileNameLayout.orientation = LinearLayout.VERTICAL

        val newFileNameInput = EditText(this)
        fileNameLayout.addView(newFileNameInput)

        val dialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle("Name Your File")
            .setView(fileNameLayout)
            .setPositiveButton("Done", null)
            .setNegativeButton("Cancel", null)
            .show()

        val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        button.setOnClickListener {

            val newFileName = newFileNameInput.text.toString()

            val fileCreationCondition = AlertDialog
                .Builder(this)
                .setCancelable(false)
                .setPositiveButton("OK", null)

            if(newFileName == "") {
                fileCreationCondition
                    .setTitle("Fail")
                    .setMessage("File Name Can't Be Empty")
                    .show()
            }
            else if(newFileName[0] == ' ') {
                fileCreationCondition
                    .setTitle("Fail")
                    .setMessage("The First Letter Of File Name Can't Be Space")
                    .show()
            }
            else {
                fileCreationCondition
                    .setTitle("Success")
                    .setMessage("File Create Successfully At $filePath")
                    .show()

                filePath += "${newFileName}.json"
                binding.path.text = filePath

                newFileNameInput.text = null
                fileNameLayout.removeView(newFileNameInput)
                dialog.dismiss()

                // New JSON Object or JSON Array ?
                run {
                    AlertDialog
                        .Builder(this)
                        .setCancelable(false)
                        .setTitle("New JSON Object or JSON Array ?")
                        .setPositiveButton("Object") { _, _ -> masterCDT = "Object" }
                        .setNeutralButton("Array") { _, _ -> masterCDT = "Array" }
                        .show()

                    nowCDT = masterCDT

                    File(prefixPath, filePath).writeText(
                        if (masterCDT == "Object") JSONObject("{}").toString(4)
                        else JSONArray("[]").toString(4)
                    )
                }
            }
        }
    }
}