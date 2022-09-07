package com.rl.simplejson

import java.io.File

var filePath = "C:\\Users\\nautilus\\AndroidStudioProjects\\SimpleJSON\\app\\src\\main\\java\\com\\rl\\simplejson\\JSON.json"

fun main() {
    val jsonString = File(filePath).readText()
    val base = parse(jsonString).second

    val listview = mutableListOf<String>()
    for(i in base) {
        val o = if(base[i.key]!![0]=='[') "[]" else "{}"
        listview.add("${i.key} : ${o[0]} ${parse(base[i.key]!!).third.size} ${o[1]}")
    }

    for(i in listview) {
        println(i)
    }
}

fun parse(_jsonString: String):
        Triple< String, MutableMap<String, String>, MutableList<String> > {

    val fingerprint = _jsonString[_jsonString.length-1]
    val jsonString = _jsonString.substring(1, _jsonString.length-1)

    val map = mutableMapOf<String, String>()
    val list = mutableListOf<String>()

    val splitList = mutableListOf<String>()

    /* ---------- comma ---------- */

    val commaList = mutableListOf<Int>()
    var num = -1
    for(i in jsonString.indices) {
        if(jsonString[i] in "{[") {
            commaList.add(0)
            num++
        }
        if(jsonString[i] in "]}") {
            commaList[num] = 1
        }
        if(jsonString[i]==',') {

            // 分水嶺
            var isWatershed = true
            for(j in commaList) {
                if(j==0) {
                    isWatershed = false
                    break
                }
            }
            if(isWatershed) {
                splitList.add(jsonString.substring(0,i))
                splitList.add(jsonString.substring(i+1))
                break
            }
        }
    }

    /* ---------- comma ---------- */

    return if (fingerprint == '}') {
        for (i in splitList) {
            val temp = i.split(':').toMutableList()
            map[temp[0]] = temp[1]
        }
        Triple("second", map, list)
    }
    else {
        for(i in splitList) {
            list.add(i)
        }
        Triple("third", map, list)
    }
}