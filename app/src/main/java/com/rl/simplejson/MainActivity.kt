package com.rl.simplejson

import java.lang.Exception
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.*
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rl.custom.itLog
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val prefixPath = "/storage/emulated/0"
private const val delay: Long = 200

class MainActivity : AppCompatActivity() {
    private lateinit var filePathView: TextView
    private lateinit var nowTypeView: TextView
    private lateinit var listView: ListView
    private lateinit var moveButton: FloatingActionButton
    private lateinit var adapter: ArrayAdapter<String>
    private var adapterList = mutableListOf<String>()
    private lateinit var layout: View
    private lateinit var alertDialog: AlertDialog

    private var filePath = ""
    private var isNowArray: Boolean? = true // nowType
    private var isMasterArray: Boolean? = true // masterType
    private var masterString = ""
    private var isMoveMode = false
    private var moveModeIndex: Int? = null
    private var nowList  = mutableListOf<Array<Int>>()
    private var nowLevel = arrayOf<Int>()
    private var historyList = mutableListOf<Array<Int>>()

    private var openResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == RESULT_OK) {
            filePath = it.data?.data?.path!!.split(':')[1]
            filePathView.text = filePath

            masterString = File(prefixPath,filePath).readText()
            var tempString = ""
            var quotation = 0
            for(i in masterString) {
                if(i == '"') {
                    quotation++
                }
                if((i==' ' && quotation%2==1) || i !in " \n\t\r") {
                    tempString += i
                }
            }
            masterString = tempString

            var isJsonArray = true
            var isJsonObject = true

            // 檢查檔案是否符合格式
            try {
                JSONArray(masterString)
            }
            catch(_: Exception) {
                isJsonArray = false
            }
            try {
                JSONObject(masterString)
            }
            catch(_: Exception) {
                isJsonObject = false
            }

            // 檔案不符合格式
            if(!isJsonArray && !isJsonObject && masterString != "") {
                updateNowType(null)

                layout = View.inflate(this, R.layout.dialog_file_error, null)
                alertDialog = AlertDialog.Builder(this).setCancelable(false).setView(layout).show()
                layout.findViewById<TextView>(R.id.choose).setOnClickListener {
                    delayForAnimation()
                    openFile()
                }
            }
            else {
                back(true)
                isMasterArray = isNowArray

                // 空檔案
                if(isMasterArray == null) {
                    layout = View.inflate(this, R.layout.dialog_notify_blankfile, null)
                    alertDialog = AlertDialog.Builder(this).setCancelable(false).setView(layout).show()
                    layout.findViewById<TextView>(R.id.ok).setOnClickListener {
                        delayForAnimation()
                        generateMasterType()
                    }
                }
                else {
                    viewSetting()
                    try {
                        clearMove()
                    }
                    catch(_: Exception) {}
                }
            }
        }
    }
    private var newResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == RESULT_OK) {
            filePath = "${it.data?.data?.path!!.split(':')[1]}/"
            filePathView.text = filePath
            nowTypeView.text = getString(R.string.folder)

            layout = View.inflate(this, R.layout.dialog_new_file, null)
            val fileName = layout.findViewById<EditText>(R.id.file_name)

            alertDialog = AlertDialog.Builder(this).setCancelable(false).setView(layout).show()
            layout.findViewById<TextView>(R.id.done).setOnClickListener {
                if(fileName.text.toString() != "") {
                    filePath = "${filePath}${fileName.text}.json"
                    filePathView.text = filePath
                    delayForAnimation()
                    generateMasterType()
                }
                else {
                    Toast.makeText(this, "File Name cannot be null", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setCustomView(R.layout.action_bar)
        setContentView(R.layout.activity_main)

        // 請求權限
        if(!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){}.launch(intent)
        }

        val actionBar = supportActionBar!!.customView
        actionBar.findViewById<ImageView>(R.id.menu).setOnClickListener {
            layout = View.inflate(this, R.layout.dialog_menu, null)
            alertDialog = AlertDialog.Builder(this).setCancelable(true).setView(layout).show()
            
            layout.findViewById<AppCompatButton>(R.id.open_file).setOnClickListener {
                alertDialog.dismiss()
                openFile()
            }
            layout.findViewById<AppCompatButton>(R.id.new_file).setOnClickListener {
                alertDialog.dismiss()
                newFile()
            }
            layout.findViewById<AppCompatButton>(R.id.about).setOnClickListener {
                alertDialog.dismiss()
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RLGreenTea/SimpleJSON")))
            }
        }
        actionBar.findViewById<ImageView>(R.id.leave).setOnClickListener {
            finish()
            Toast.makeText(this, "LEAVE", Toast.LENGTH_LONG).show()
        }

        filePathView = findViewById(R.id.path)
        nowTypeView = findViewById(R.id.type)
        listView = findViewById(R.id.listview)

        filePathView.text = getString(R.string.path)
        nowTypeView.text = getString(R.string.empty)

        adapter = ArrayAdapter(this, R.layout.list, adapterList)
        listView.adapter = adapter
        adapterList.add("This")
        adapterList.add("Is")
        adapterList.add("Sample")
        adapter.notifyDataSetChanged()
    }

    private fun openFile() {
        val intent = Intent().setType("application/json").setAction(Intent.ACTION_GET_CONTENT)
        openResultLauncher.launch(Intent.createChooser(intent,"Choose A File"))
    }
    private fun newFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        newResultLauncher.launch(intent)
    }
    private fun saveFile() {
        if(isMasterArray!!) {
            File(prefixPath,filePath).writeText(JSONArray(masterString).toString(4))
        }
        else {
            File(prefixPath,filePath).writeText(JSONObject(masterString).toString(4))
        }
    }

    private fun viewSetting() {
        listView.setOnItemClickListener { _, _, i, _ ->
            if(isMoveMode) {
                if(moveModeIndex != null) {
                    listView.getChildAt(moveModeIndex!!).setBackgroundColor(getMyColor(R.color.dark_gray))
                }
                moveModeIndex = i
                listView.getChildAt(i).setBackgroundColor(getMyColor(R.color.blue))
            }
            else {
                if(isNonempty(nowList[i])) {
                    if(isNowArray!!) {
                        enterLevel(nowList[i][0], nowList[i][1])
                        historyList.add(arrayOf(nowLevel[0],nowLevel[1]))
                    }
                    else {
                        enterLevel(nowList[i][1]+1, nowList[i][2])
                        historyList.add(arrayOf(nowLevel[0],nowLevel[1]))
                    }
                }
                else {
                    editItem(i)
                }
            }
        }
        listView.setOnItemLongClickListener { _, _, i, _ ->
            if(isMoveMode) {
                if(moveModeIndex != null) {
                    listView.getChildAt(moveModeIndex!!).setBackgroundColor(getMyColor(R.color.dark_gray))
                }
                moveModeIndex = i
                listView.getChildAt(i).setBackgroundColor(getMyColor(R.color.blue))
            }
            else {
                if(isNonempty(nowLevel)) {
                    editItem(i)
                }
            }
            return@setOnItemLongClickListener true
        }

        findViewById<FloatingActionButton>(R.id.back_fab).setOnClickListener {
            if(isMoveMode) {
                clearMove()
            }
            else {
                back()
            }
        }
        findViewById<FloatingActionButton>(R.id.back_fab).setOnLongClickListener {
            if(isMoveMode) {
                clearMove()
            }
            else {
                back(true)
            }
            return@setOnLongClickListener true
        }

        findViewById<FloatingActionButton>(R.id.add_fab).setOnClickListener {
            if(!isMoveMode) {
                addItem()
            }
        }

        moveButton = findViewById(R.id.move_fab)
        moveButton.setOnClickListener {
            // turn off moveMode
            if(isMoveMode) {
                clearMove()
            }
            // turn on moveMode
            else {
                isMoveMode = true
                moveButton.backgroundTintList = ColorStateList.valueOf(getMyColor(R.color.purple))
                Toast.makeText(this, "Click on the item you'd like to move",Toast.LENGTH_LONG).show()
                Toast.makeText(this, "Press volume up and down to move item", Toast.LENGTH_LONG).show()
            }
        }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if(isMoveMode && moveModeIndex != null) {
                    moveItem(true)
                    return true
                }
                else {
                    return super.onKeyUp(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if(isMoveMode && moveModeIndex != null) {
                    moveItem(false)
                    return true
                }
                else {
                    return super.onKeyDown(keyCode, event)
                }
            }
            else -> {
                return super.onKeyDown(keyCode, event)
            }
        }
    }
    private fun hideSoftKeyboard(windowToken: IBinder) {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }
    private fun showSoftKeyboard(editText: EditText) {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(editText, 0)
    }
    private fun delayForAnimation() {
        Handler(Looper.getMainLooper()).postDelayed({alertDialog.dismiss()}, delay)
    }
    private fun getMyColor(id: Int): Int {
        return ContextCompat.getColor(this, id)
    }
    private fun clearMove() {
        isMoveMode = false
        moveButton.backgroundTintList = ColorStateList.valueOf(getMyColor(R.color.blue))
        if(moveModeIndex != null) {
            listView.getChildAt(moveModeIndex!!).setBackgroundColor(getMyColor(R.color.dark_gray))
        }
        moveModeIndex = null
    }

    private fun generateMasterType() {
        val layout = View.inflate(this, R.layout.dialog_generate_mastertype, null)
        val radioArray = layout.findViewById<RadioButton>(R.id.array)
        val radioObject = layout.findViewById<RadioButton>(R.id.`object`)

        val alertDialog = AlertDialog.Builder(this).setCancelable(false).setView(layout).show()
        layout.findViewById<TextView>(R.id.cancel_and_choose).setOnClickListener {
            Handler(Looper.getMainLooper()).postDelayed({alertDialog.dismiss()}, delay)
            updateNowType(null)
            openFile()
        }
        layout.findViewById<TextView>(R.id.done).setOnClickListener {
            if(radioArray.isChecked || radioObject.isChecked) {
                Handler(Looper.getMainLooper()).postDelayed({alertDialog.dismiss()}, delay)
                if(radioArray.isChecked) {
                    masterString = "[]"
                    isMasterArray = true
                }
                else {
                    masterString = "{}"
                    isMasterArray = false
                }
                updateNowType(isMasterArray)
                back(true)
                viewSetting()
                saveFile()
            }
        }
    }
    private fun enterLevel(startIndex_: Int, endIndex: Int) {
        nowList.clear()
        adapterList.clear()

        val startIndex = startIndex_+1
        val tempString: String
        try {
            tempString = masterString.substring(startIndex, endIndex)
        }
        // 檔案是空的
        catch (_: Exception) {
            nowLevel = arrayOf()
            updateNowType(null)
            adapter.notifyDataSetChanged()
            return
        }

        val fingerPrint = masterString[endIndex]
        val commaList = mutableListOf<Int>()

        var inString = 0
        val tempList = mutableListOf<Int>()

        for (i in tempString.indices) {
            if (tempString[i] == '"') {
                inString = (inString+1) % 2
            }

            if (tempString[i] in "{[" && inString == 0) {
                commaList.add(0)
            }
            else if (tempString[i] in "]}" && inString == 0) {
                for (j in commaList.lastIndex downTo 0) {
                    if (commaList[j] == 0) {
                        commaList[j] = 1
                        break
                    }
                }
            }
            else if (tempString[i] == ',' && inString == 0) {
                var isWatershed = true
                for (j in commaList) {
                    if (j == 0) {
                        isWatershed = false
                        break
                    }
                }
                if (isWatershed) {
                    nowList.add(arrayOf(tempList[0], tempList.last()))
                    tempList.clear()
                    continue
                }
            }
            tempList.add(startIndex+i)
        }
        if (tempList.size > 0) {
            nowList.add(arrayOf(tempList[0], tempList.last()))
        }

        if (fingerPrint == ']') {
            isNowArray = true
            for(i in nowList) {
                if (masterString[i[0]] in "{[") {
                    adapterList.add("${masterString[i[0]]} ${countLevel(i[0], i[1])} ${masterString[i[1]]}")
                }
                else {
                    adapterList.add(masterString.substring(i[0], i[1]+1))
                }
            }
        }
        else {
            isNowArray = false
            val tempObject = mutableListOf<Array<Int>>()

            for (i in nowList) {
                inString = 0
                for (j in i[0]..i[1]) {
                    if(masterString[j] == '"') {
                        inString = (inString+1)%2
                    }
                    if (masterString[j] == ':' && inString == 0) {
                        tempObject.add(arrayOf(i[0], j, i[1]))
                        break
                    }
                }
            }
            nowList = tempObject

            for (i in nowList) {
                if (masterString[i[1] + 1] in "{[") {
                    adapterList.add("${masterString.substring(i[0], i[1])} : ${masterString[i[1]+1]} ${countLevel(i[1]+1, i[2])} ${masterString[i[2]]}")
                }
                else {
                    adapterList.add("${masterString.substring(i[0], i[1])} : ${masterString.substring(i[1]+1, i[2]+1)}")
                }
            }
        }

        try {
            nowLevel = arrayOf(nowList[0][0]-1, nowList.last().last()+1)
        }
        // Array或Object裡沒有項目
        catch (_: Exception) {
            nowLevel = arrayOf(startIndex_, endIndex)
        }
        updateNowType(isNowArray)
        adapter.notifyDataSetChanged()
    }
    private fun countLevel(startIndex_: Int, endIndex: Int): Int {
        var startIndex = startIndex_
        var tempString = masterString.substring(startIndex, endIndex+1)
        val fingerPrint = tempString[0]
        val splitList = mutableListOf<Array<Int>>()
        val commaList = mutableListOf<Int>()
        var inString = 0
        val tempList = mutableListOf<Int>()

        if(fingerPrint in "{[") {
            tempString = tempString.substring(1, tempString.length-1)
            startIndex++
        }

        for(i in tempString.indices) {
            if(tempString[i] == '"') {
                inString = (inString + 1) % 2
            }

            if(tempString[i] in "{[") {
                commaList.add(0)
            }
            else if(tempString[i] in "]}") {
                for (j in commaList.lastIndex downTo 0) {
                    if (commaList[j] == 0) {
                        commaList[j] = 1
                        break
                    }
                }
            }
            else if(tempString[i] == ',' && inString == 0) {
                var isWatershed = true
                for(j in commaList) {
                    if(j == 0) {
                        isWatershed = false
                        break
                    }
                }
                if(isWatershed) {
                    splitList.add(arrayOf(tempList[0], tempList.last()))
                    tempList.clear()
                    continue
                }
            }
            tempList.add(startIndex + i)
        }

        try {
            splitList.add(arrayOf(tempList[0], tempList.last()))
            return splitList.size
        }
        catch(_: Exception) {
            return 0
        }
    }
    private fun addItem() {
        if(isNowArray!!) {
            layout = View.inflate(this, R.layout.dialog_add_array, null)

            val value = layout.findViewById<EditText>(R.id.value)
            val isString = layout.findViewById<CheckBox>(R.id.isString)
            val radioGroup = layout.findViewById<RadioGroup>(R.id.radio_group)
            val radioArray = layout.findViewById<RadioButton>(R.id.array)
            val radioObject = layout.findViewById<RadioButton>(R.id.`object`)

            value.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s.toString() in arrayOf("true", "false", "null")) {
                        value.setTextColor(getMyColor(R.color.orange))
                    }
                    else {
                        value.setTextColor(getMyColor(R.color.white))
                    }
                }
            })
            value.setOnFocusChangeListener { _, hasFocus ->
                if(hasFocus) {
                    radioGroup.clearCheck()
                }
            }
            value.setOnClickListener {
                radioGroup.clearCheck()
            }

            isString.setOnClickListener {
                radioGroup.clearCheck()
                value.requestFocus()
                showSoftKeyboard(value)
            }

            radioArray.setOnClickListener {
                value.text = null
                value.clearFocus()
                isString.isChecked = false
                hideSoftKeyboard(value.windowToken)
            }
            radioObject.setOnClickListener {
                value.text = null
                value.clearFocus()
                isString.isChecked = false
                hideSoftKeyboard(value.windowToken)
            }

            alertDialog = AlertDialog.Builder(this).setCancelable(false).setView(layout).show()
            layout.findViewById<TextView>(R.id.cancel).setOnClickListener {
                delayForAnimation()
            }
            layout.findViewById<TextView>(R.id.done).setOnClickListener {
                delayForAnimation()

                val valueText: String
                if(radioArray.isChecked || radioObject.isChecked) {
                    if(radioArray.isChecked) {
                        valueText = "[]"
                        adapterList.add("[ 0 ]")
                    }
                    else {
                        valueText = "{}"
                        adapterList.add("{ 0 }")
                    }
                }
                else {
                    if(isString.isChecked) {
                        valueText = "\"${value.text}\""
                    }
                    else {
                        if(value.text.toString() == "") {
                            valueText = "null"
                        }
                        else {
                            valueText = value.text.toString()
                        }
                    }
                    adapterList.add(valueText)
                }
                adapter.notifyDataSetChanged()

                var valueDiff = valueText.length
                if(nowList.size == 0) {
                    masterString = masterString.substring(0, nowLevel[0]+1) + valueText + masterString.substring(nowLevel[1])
                    saveFile()

                    nowLevel[1] += valueDiff
                    nowList.add(arrayOf(nowLevel[0]+1, nowLevel[1]-1))
                }
                else {
                    masterString = masterString.substring(0, nowList.last()[1]+1) + ",$valueText"+ masterString.substring(nowLevel[1])
                    saveFile()

                    valueDiff++
                    nowLevel[1] += valueDiff
                    nowList.add(arrayOf(nowList.last()[1]+2, nowLevel[1]-1))
                }

                if(historyList.size > 0) {
                    for(i in historyList.indices) {
                        historyList[i][1] += valueDiff
                    }
                }
            }
        }
        else{
            layout = View.inflate(this, R.layout.dialog_add_object, null)

            val name = layout.findViewById<EditText>(R.id.name)
            val value = layout.findViewById<EditText>(R.id.value)
            val isString = layout.findViewById<CheckBox>(R.id.isString)
            val radioGroup = layout.findViewById<RadioGroup>(R.id.radio_group)
            val radioArray = layout.findViewById<RadioButton>(R.id.array)
            val radioObject = layout.findViewById<RadioButton>(R.id.`object`)

            value.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s.toString() in arrayOf("true", "false", "null")) {
                        value.setTextColor(getMyColor(R.color.orange))
                    }
                    else {
                        value.setTextColor(getMyColor(R.color.white))
                    }
                }
            })
            value.setOnFocusChangeListener { _, hasFocus ->
                if(hasFocus) {
                    radioGroup.clearCheck()
                }
            }
            value.setOnClickListener {
                radioGroup.clearCheck()
            }

            isString.setOnClickListener {
                radioGroup.clearCheck()
                value.requestFocus()
                showSoftKeyboard(value)
            }

            radioArray.setOnClickListener {
                value.text = null
                value.clearFocus()
                isString.isChecked = false
                hideSoftKeyboard(value.windowToken)
            }
            radioObject.setOnClickListener {
                value.text = null
                value.clearFocus()
                isString.isChecked = false
                hideSoftKeyboard(value.windowToken)
            }

            alertDialog = AlertDialog.Builder(this).setCancelable(false).setView(layout).show()
            layout.findViewById<TextView>(R.id.cancel).setOnClickListener {
                delayForAnimation()
            }
            layout.findViewById<TextView>(R.id.done).setOnClickListener {
                delayForAnimation()

                val updateText: String
                if(radioArray.isChecked || radioObject.isChecked) {
                    if(radioArray.isChecked) {
                        updateText = "\"${name.text}\":[]"
                        adapterList.add("\"${name.text}\" : [ 0 ]")
                    }
                    else {
                        updateText = "\"${name.text}\":{}"
                        adapterList.add("\"${name.text}\" : { 0 }")
                    }
                }
                else {
                    if(isString.isChecked) {
                        updateText = "\"${name.text}\":\"${value.text}\""
                        adapterList.add("\"${name.text}\" : \"${value.text}\"")
                    }
                    else {
                        if(value.text.toString() == "") {
                            updateText = "\"${name.text}\":null"
                            adapterList.add("\"${name.text}\" : null")
                        }
                        else {
                            updateText = "\"${name.text}\":${value.text}"
                            adapterList.add("\"${name.text}\" : ${value.text}")
                        }
                    }
                }
                adapter.notifyDataSetChanged()

                var diff = updateText.length
                if(nowList.size == 0) {
                    masterString = masterString.substring(0, nowLevel[0]+1) + updateText + masterString.substring(nowLevel[1])
                    saveFile()

                    nowLevel[1] += diff
                    nowList.add(arrayOf(nowLevel[0]+1, nowLevel[0]+(name.text.length+2)+1, nowLevel[1]-1))
                }
                else {
                    masterString = masterString.substring(0, nowList.last()[2]+1) + ",$updateText"+ masterString.substring(nowLevel[1])
                    saveFile()

                    diff++
                    nowLevel[1] += diff
                    nowList.add(arrayOf(nowList.last()[2]+2, (nowList.last()[2]+1)+(name.text.length+2)+1, nowLevel[1]-1))
                }

                if(historyList.size > 0) {
                    for(i in historyList.indices) {
                        historyList[i][1] += diff
                    }
                }
            }
        }
    }
    private fun deleteItem(i: Int) {
        if(isNowArray!!) {
            val diff: Int
            if(i == 0) {
                if(nowList.size == 1) {
                    diff = (nowList[0][1]+1) - nowList[0][0]
                    masterString = masterString.removeRange(nowList[0][0], nowList[0][1]+1)
                }
                else {
                    diff = (nowList[0][1]+2) - nowList[0][0]
                    masterString = masterString.removeRange(nowList[0][0], nowList[0][1]+2)
                }
            }
            else {
                diff = (nowList[i][1]+1) - (nowList[i][0]-1)
                masterString = masterString.removeRange(nowList[i][0]-1, nowList[i][1]+1)
            }
            adapterList.removeAt(i)
            adapter.notifyDataSetChanged()

            nowLevel[1] -= diff

            for(j in i+1 until nowList.size) {
                nowList[j][0] -= diff
                nowList[j][1] -= diff
            }
            nowList.removeAt(i)

            for(j in 0 until historyList.size) {
                historyList[j][1] -= diff
            }
        }
        else {
            val diff: Int
            if(i == 0) {
                if(nowList.size == 1) {
                    diff = (nowList[0][2]+1) - nowList[0][0]
                    masterString = masterString.removeRange(nowList[0][0], nowList[0][2]+1)
                    saveFile()
                }
                else {
                    diff = (nowList[0][2]+2) - nowList[0][0]
                    masterString = masterString.removeRange(nowList[0][0], nowList[0][2]+2)
                    saveFile()
                }
            }
            else {
                diff = (nowList[i][2]+1) - (nowList[i][0]-1)
                masterString = masterString.removeRange(nowList[i][0]-1, nowList[i][2]+1)
                saveFile()
            }
            adapterList.removeAt(i)
            adapter.notifyDataSetChanged()

            nowLevel[1] -= diff

            for(j in i+1 until nowList.size) {
                nowList[j][0] -= diff
                nowList[j][1] -= diff
                nowList[j][2] -= diff
            }
            nowList.removeAt(i)

            for(j in 0 until historyList.size) {
                historyList[j][1] -= diff
            }
        }
    }
    private fun editItem(i: Int) {
        if(isNowArray!!) {
            layout = View.inflate(this, R.layout.dialog_edit_array, null)

            val value = layout.findViewById<EditText>(R.id.value)
            val isString = layout.findViewById<CheckBox>(R.id.isString)

            value.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s.toString() in arrayOf("true", "false", "null")) {
                        value.setTextColor(getMyColor(R.color.orange))
                    }
                    else {
                        value.setTextColor(getMyColor(R.color.white))
                    }
                }
            })

            if(isNonempty(nowList[i])) {
                value.setText(getString(R.string.uneditable))
                value.setTextColor(getMyColor(R.color.unable))
                value.isFocusable = false
                value.isLongClickable = false
                value.isClickable = false

                isString.setTextColor(getMyColor(R.color.unable))
                isString.isChecked = false
                isString.buttonTintList =
                    ColorStateList.valueOf(getMyColor(R.color.unable))
                isString.isClickable = false

                alertDialog = AlertDialog.Builder(this).setCancelable(false).setView(layout).show()
                layout.findViewById<TextView>(R.id.cancel).setOnClickListener {
                    delayForAnimation()
                }
                layout.findViewById<TextView>(R.id.done).setOnClickListener {
                    delayForAnimation()
                }
                layout.findViewById<TextView>(R.id.delete).setOnClickListener {
                    delayForAnimation()
                    deleteItem(i)
                }
            }
            else {
                if (isValueString(nowList[i])) {
                    value.setText(masterString.substring(nowList[i][0] + 1, nowList[i][1]))
                    isString.isChecked = true
                }
                else {
                    value.setText(masterString.substring(nowList[i][0], nowList[i][1] + 1))
                    isString.isChecked = false
                }

                alertDialog = AlertDialog.Builder(this).setCancelable(false).setView(layout).show()
                layout.findViewById<TextView>(R.id.cancel).setOnClickListener {
                    delayForAnimation()
                }
                layout.findViewById<TextView>(R.id.done).setOnClickListener {
                    delayForAnimation()

                    var valueText = value.text.toString()
                    if(value.text.toString() == "" && !isString.isChecked) {
                        valueText = "null"
                    }
                    if (isString.isChecked) {
                        valueText = "\"${valueText}\""
                    }
                    else {
                        if(valueText!="true" && valueText!="false" && valueText!="null") {
                            valueText = "\"${valueText}\""
                        }
                    }

                    masterString = masterString.substring(0, nowList[i][0]) + valueText + masterString.substring(nowList[i][1] + 1)
                    saveFile()

                    adapterList[i] = valueText
                    adapter.notifyDataSetChanged()

                    val valueDiff = valueText.length - (nowList[i][1] - nowList[i][0] + 1)

                    nowList[i][1] += valueDiff
                    for (j in i + 1 until nowList.size) {
                        nowList[j][0] += valueDiff
                        nowList[j][1] += valueDiff
                    }

                    nowLevel[1] += valueDiff

                    for (j in 0 until historyList.size) {
                        historyList[j][1] += valueDiff
                    }
                }
                layout.findViewById<TextView>(R.id.delete).setOnClickListener {
                    delayForAnimation()
                    deleteItem(i)
                }
            }
        }
        else {
            layout = View.inflate(this, R.layout.dialog_edit_object, null)

            val name = layout.findViewById<EditText>(R.id.name)
            val value = layout.findViewById<EditText>(R.id.value)
            val isString = layout.findViewById<CheckBox>(R.id.isString)

            name.setText(masterString.substring(nowList[i][0] + 1, nowList[i][1] - 1))
            value.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s.toString() in arrayOf("true", "false", "null")) {
                        value.setTextColor(getMyColor(R.color.orange))
                    }
                    else {
                        value.setTextColor(getMyColor(R.color.white))
                    }
                }
            })

            if(isNonempty(nowList[i])) {
                value.setText(getString(R.string.uneditable))
                value.setTextColor(getMyColor(R.color.unable))
                value.isFocusable = false
                value.isLongClickable = false
                value.isClickable = false

                isString.setTextColor(getMyColor(R.color.unable))
                isString.isChecked = false
                isString.buttonTintList = ColorStateList.valueOf(getMyColor(R.color.unable))
                isString.isClickable = false

                alertDialog = AlertDialog.Builder(this).setCancelable(false).setView(layout).show()

                layout.findViewById<TextView>(R.id.cancel).setOnClickListener {
                    delayForAnimation()
                }
                layout.findViewById<TextView>(R.id.delete).setOnClickListener {
                    delayForAnimation()
                    deleteItem(i)
                }
                layout.findViewById<TextView>(R.id.done).setOnClickListener {
                    if(name.text.toString() != "") {
                        delayForAnimation()

                        val valueText = "${masterString[nowList[i][1]+1]} ${countLevel(nowList[i][1]+1, nowList[i][2])} ${masterString[nowList[i][2]]}"

                        masterString = masterString.substring(0, nowList[i][0]) + "\"${name.text}\"" + masterString.substring(nowList[i][1])
                        saveFile()
                        adapterList[i] = "\"${name.text}\" : $valueText"
                        adapter.notifyDataSetChanged()

                        val nameDiff = (name.text.length + 2) - (nowList[i][1] - nowList[i][0])

                        nowList[i][1] += nameDiff
                        nowList[i][2] += nameDiff

                        nowLevel[1] += nameDiff

                        for(j in i + 1 until nowList.size) {
                            nowList[j][0] += nameDiff
                            nowList[j][1] += nameDiff
                            nowList[j][2] += nameDiff
                        }
                        for(j in 0 until historyList.size) {
                            historyList[j][1] += nameDiff
                        }
                    }
                    else {
                        Toast.makeText(this, "Name cannot be null", Toast.LENGTH_LONG).show()
                    }
                }
            }
            else {
                if (isValueString(nowList[i])) {
                    value.setText(masterString.substring(nowList[i][1] + 2, nowList[i][2]))
                    isString.isChecked = true
                }
                else {
                    value.setText(masterString.substring(nowList[i][1] + 1, nowList[i][2] + 1))
                    isString.isChecked = false
                }

                alertDialog = AlertDialog.Builder(this).setCancelable(false).setView(layout).show()
                layout.findViewById<TextView>(R.id.cancel).setOnClickListener {
                    delayForAnimation()
                }
                layout.findViewById<TextView>(R.id.done).setOnClickListener {
                    if(name.text.toString() == "") {
                        Toast.makeText(this, "Name cannot be null", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        delayForAnimation()
                        var valueText = value.text.toString()
                        if(isString.isChecked) {
                            valueText = "\"${valueText}\""
                        }
                        else {
                            if(valueText == "") {
                                valueText = "null"
                            }
                            else if(valueText!="true" && valueText!="false" && valueText!="null") {
                                valueText = "\"${valueText}\""
                            }
                        }

                        masterString = masterString.substring(0, nowList[i][0]) + "\"${name.text}\":${valueText}" + masterString.substring(nowList[i][2] + 1)
                        saveFile()
                        adapterList[i] = "\"${name.text}\" : $valueText"
                        adapter.notifyDataSetChanged()

                        val nameDiff = (name.text.length + 2) - (nowList[i][1] - nowList[i][0])
                        val valueDiff = valueText.length - (nowList[i][2] - nowList[i][1])

                        nowList[i][1] += nameDiff
                        nowList[i][2] += nameDiff + valueDiff

                        nowLevel[1] += nameDiff + valueDiff

                        for(j in i + 1 until nowList.size) {
                            nowList[j][0] += nameDiff + valueDiff
                            nowList[j][1] += nameDiff + valueDiff
                            nowList[j][2] += nameDiff + valueDiff
                        }
                        for(j in 0 until historyList.size) {
                            historyList[j][1] += nameDiff + valueDiff
                        }
                    }
                }
                layout.findViewById<TextView>(R.id.delete).setOnClickListener {
                    delayForAnimation()
                    deleteItem(i)
                }
            }
        }
    }
    private fun moveItem(moveUp: Boolean) {
        val i = moveModeIndex
        val mutableList = mutableListOf<String>()
        for(j in nowList.indices) {
            mutableList.add(masterString.substring(nowList[j][0], nowList[j].last()+1))
        }

        if(nowList.size > 1) {
            if(moveUp) {
                if(i != 0) {
                    var old = adapterList[i!!-1]
                    adapterList[i-1] = adapterList[i]
                    adapterList[i] = old
                    adapter.notifyDataSetChanged()

                    old = mutableList[i-1]
                    mutableList[i-1] = mutableList[i]
                    mutableList[i] = old

                    listView.getChildAt(i).setBackgroundColor(getMyColor(R.color.dark_gray))
                    listView.getChildAt(i-1).setBackgroundColor(getMyColor(R.color.blue))
                    moveModeIndex = moveModeIndex!!-1
                }
                else {
                    return
                }
            }
            else {
                if(i != nowList.lastIndex) {
                    var old = adapterList[i!!+1]
                    adapterList[i+1] = adapterList[i]
                    adapterList[i] = old
                    adapter.notifyDataSetChanged()

                    old = mutableList[i+1]
                    mutableList[i+1] = mutableList[i]
                    mutableList[i] = old

                    listView.getChildAt(i).setBackgroundColor(getMyColor(R.color.dark_gray))
                    listView.getChildAt(i+1).setBackgroundColor(getMyColor(R.color.blue))
                    moveModeIndex = moveModeIndex!!+1
                }
                else {
                    return
                }
            }
            var tempString = ""
            for(item in mutableList) {
                tempString += ",$item"
            }
            tempString = tempString.substring(1)
            masterString = masterString.substring(0, nowLevel[0]+1) + tempString + masterString.substring(nowLevel[1])
            saveFile()

            nowList.clear()
            if(isNowArray!!) {
                var front = nowLevel[0]+1
                for(j in mutableList.indices) {
                    val size = mutableList[j].length-1
                    nowList.add(arrayOf(front, front+size))
                    front += size+2
                }
            }
            else {
                var front = nowLevel[0]+1
                for(j in mutableList.indices) {
                    var inString = 0
                    for(k in mutableList[j].indices) {
                        if(mutableList[j][k] == '"') {
                            inString = (inString+1)%2
                        }
                        if(mutableList[j][k] == ':' && inString == 0) {
                            val valueSize = mutableList[j].length-k-1
                            nowList.add(arrayOf(front, front+k, front+k+valueSize))
                            front += k+valueSize+2
                            break
                        }
                    }
                }
            }
        }
    }
    private fun back(backToRoot: Boolean = false) {
        if(backToRoot) {
            historyList.clear()
            historyList.add(arrayOf(0, masterString.lastIndex))
        }
        if(historyList.size > 1) {
            historyList.removeLast()
        }
        enterLevel(historyList.last()[0], historyList.last()[1])
    }
    private fun updateNowType(isNowArray_: Boolean?) {
        isNowArray = isNowArray_
        if(isNowArray == null) {
            nowTypeView.text = getString(R.string.empty)
        }
        else {
            if(isNowArray as Boolean) {
                nowTypeView.text = getString(R.string.array)
            }
            else {
                nowTypeView.text = getString(R.string.`object`)
            }
        }
    }

    private fun isNonempty(array: Array<Int>): Boolean {
        return masterString[array.last()] in "]}"
    }
    private fun isValueString(array: Array<Int>): Boolean {
        return masterString[array.last()] == '"'
    }
}