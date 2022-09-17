package com.rl.simplejson

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
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

    private var masterCDT = ""
    private var masterString = ""

    private var history = mutableListOf<Array<Int>>()

    private var nowIndex = arrayOf<Int>()
    private var nowCDT = ""
    private var nowObject = mutableListOf<Array<Int>>()
    private var nowArray  = mutableListOf<Array<Int>>()
    private var nowList   = mutableListOf<Array<Int>>()

    private var openResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == RESULT_OK) {
            filePath = it.data?.data?.path!!.split(':')[1]
            binding.path.text = filePath

            // masterString
            run {
                masterString = File(prefixPath,filePath).readText()
                var tempString = ""
                var quotation = 0
                for (i in masterString) {
                    if (i == '"')
                        quotation++
                    if ((i == ' ' && quotation%2==1) || i !in " \n\t\r")
                        tempString += i
                }
                masterString = tempString
            }

            plJson(0,masterString.length-1)
            masterCDT = nowCDT

            // view setting
            run {
                binding.listView.setOnItemClickListener { _, _, i, _ ->
                    nowIndex = nowList[i]
                    if (masterString[nowIndex[nowIndex.lastIndex]] in "]}") {
                        if (nowCDT=="Object") {
                            history.add(arrayOf(nowList[0][0]-1, nowList[nowList.lastIndex][2]+1))
                            plJson(nowIndex[1]+1,nowIndex[2])
                        }
                        else {
                            history.add(arrayOf(nowList[0][0]-1, nowList[nowList.lastIndex][1]+1))
                            plJson(nowIndex[0],nowIndex[1])
                        }
                    }
                }
                binding.fabBack.setOnClickListener {
                    onBackPressed()
                }
                binding.fabBack.setOnLongClickListener {
                    history.clear()
                    plJson(0,masterString.length-1)
                    return@setOnLongClickListener true
                }
            }
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

    private fun plJson(startIndex_: Int, endIndex: Int) {
        var startIndex = startIndex_
        var tempString = masterString.substring(startIndex, endIndex+1)
        val fingerPrint = tempString[tempString.lastIndex]
        val commaList = mutableListOf<Int>()

        if (fingerPrint in "]}") {
            tempString = tempString.substring(1,tempString.lastIndex)
            startIndex++
        }

        // nowList
        run {
            nowList.clear()
            val tempList = mutableListOf<Int>()

            for (i in tempString.indices) {
                if (tempString[i] in "{[")
                    commaList.add(0)
                else if (tempString[i] in "]}") {
                    for (j in commaList.lastIndex downTo 0) {
                        if (commaList[j] == 0) {
                            commaList[j] = 1
                            break
                        }
                    }
                }
                else if (tempString[i] == ',') {
                    // 這個逗號是否為最外層的(分水嶺)
                    var isWatershed = true
                    for (j in commaList) {
                        if (j == 0) {
                            isWatershed = false
                            break
                        }
                    }
                    if (isWatershed) {
                        nowList.add(arrayOf(tempList[0],tempList[tempList.lastIndex]))
                        tempList.clear()
                        continue
                    }
                }
                tempList.add(startIndex + i)
            }
            nowList.add(arrayOf(tempList[0],tempList[tempList.lastIndex]))

            Log.i("main",nowList.size.toString())
            for (i in nowList) {
                Log.i("main","${i[0]} / ${i[1]}")
            }
        }

        // nowCDT & nowObject & nowArray & adapterList
        run {
            adapterList.clear()

            if (fingerPrint == '}'){

                nowCDT = "Object"
                val tempObject = mutableListOf<Array<Int>>()
                for (i in nowList) {
                    for (j in i[0]..i[1]) {
                        if (masterString[j] == ':') {
                            tempObject.add(arrayOf(i[0], j, i[1]))
                            break
                        }
                    }
                }
                nowObject = tempObject
                nowList = nowObject

                for (i in nowObject) {
                    adapterList.add("${masterString.substring(i[0],i[1])} : ${
                        if (masterString[i[1]+1] in "{[") "${masterString[i[1]+1]} ${lenJson(i[1]+1,i[2])} ${masterString[i[2]]}"
                        else masterString.substring(i[1]+1,i[2]+1)
                    }")
                }
            }
            else {
                nowCDT = "Array"
                nowArray = nowList

                for (i in nowArray) {
                    adapterList.add(
                        if (masterString[i[0]] in "{[") "${masterString[i[0]]} ${lenJson(i[0],i[1])} ${masterString[i[1]]}"
                        else masterString.substring(i[0],i[1]+1)
                    )
                }
            }
            adapter.notifyDataSetChanged()
            binding.CDT.text = nowCDT
        }
    }

    private fun lenJson(startIndex_: Int, endIndex: Int): Int {
        var startIndex = startIndex_
        var tempString = masterString.substring(startIndex, endIndex+1)
        val fingerPrint = tempString[0]
        val splitList = mutableListOf<Array<Int>>()
        val commaList = mutableListOf<Int>()

        if (fingerPrint in "{[") {
            tempString = tempString.substring(1,tempString.length-1)
            startIndex++
        }

        // splitList
        run {
            val tempList = mutableListOf<Int>()
            for (i in tempString.indices) {

                if (tempString[i] in "{[")
                    commaList.add(0)

                else if (tempString[i] in "]}") {
                    for (j in commaList.size-1 downTo 0) {
                        if (commaList[j] == 0) {
                            commaList[j] = 1
                            break
                        }
                    }
                }
                else if (tempString[i] == ',') {
                    // 這個逗號是否為最外層的(分水嶺)
                    var isWatershed = true
                    for (j in commaList) {
                        if (j == 0) {
                            isWatershed = false
                            break
                        }
                    }
                    if (isWatershed) {
                        splitList.add(arrayOf(tempList[0],tempList[tempList.size-1]))
                        tempList.clear()
                        continue
                    }
                }
                tempList.add(startIndex + i)
            }
            splitList.add(
                arrayOf(tempList[0],tempList[tempList.size-1])
            )
        }

        return splitList.size
    }

    override fun onBackPressed() {
        if (filePath != "") {
            try {
                nowIndex = history[history.lastIndex]
                history.removeLast()
            }
            catch (e: Exception) {
                nowIndex = arrayOf(0,masterString.lastIndex)
            }
            plJson(nowIndex[0],nowIndex[1])
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