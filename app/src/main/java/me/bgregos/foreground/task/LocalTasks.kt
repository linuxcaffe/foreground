package me.bgregos.foreground.task

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.util.*
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList

object LocalTasks {
    var items:ArrayList<Task> = ArrayList()
    var visibleTasks:ArrayList<Task> = ArrayList()
    var localChanges:ArrayList<Task> = ArrayList()
    var initSync:Boolean = true
    var syncKey:String = ""


    fun save(context: Context) {
        updateVisibleTasks()
        val prefs = context.getSharedPreferences("me.bgregos.BrightTask", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("LocalTasks", Gson().toJson(items))
        editor.putString("LocalTasks.localChanges", Gson().toJson(localChanges))
        editor.putString("LocalTasks.initSync", initSync.toString())
        editor.putString("LocalTasks.syncKey", syncKey)
        editor.apply()
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("me.bgregos.BrightTask", Context.MODE_PRIVATE)
        val taskType = object : TypeToken<ArrayList<Task>>() {}.type
        items = Gson().fromJson(prefs.getString("LocalTasks", ""), taskType) ?: items
        localChanges = Gson().fromJson(prefs.getString("LocalTasks.localChanges", ""), taskType) ?: localChanges
        initSync =  prefs.getString("LocalTasks.initSync", "true")?.toBoolean()?:true
        syncKey = prefs.getString("LocalTasks.syncKey", syncKey)?: syncKey
        val lastSeenVersion = prefs.getInt("lastSeenVersion", 1)

        //migration
        var itemsModified = false
        for (i in items){
            Log.v("migrations","checking migrations for task: "+i.name)
            if (i.others==null){
                itemsModified = true
                i.others = mutableMapOf()
            }
            if (i.createdDate==null){
                Log.v("migrations","adding createdDate for task: "+i.name)
                itemsModified = true
                i.createdDate = Date()
            }
        }
        if (lastSeenVersion<2){ //keep track of breaking changes
            val editor = prefs.edit()
            editor.putInt("lastSeenVersion", 2)
            editor.apply()
            itemsModified = true
            val dfLocal = SimpleDateFormat()
            dfLocal.setTimeZone(TimeZone.getDefault())
            val dfUtc = SimpleDateFormat()
            dfUtc.setTimeZone(TimeZone.getTimeZone("UTC"))
            for (i in items) {
                //convert all Dates from local time to GMT
                if (i.createdDate != null){
                    i.createdDate=dfUtc.parse(dfLocal.format(i.createdDate))
                }
                if (i.modifiedDate != null){
                    i.modifiedDate=dfUtc.parse(dfLocal.format(i.modifiedDate))
                }
                if (i.dueDate != null){
                    i.dueDate=dfUtc.parse(dfLocal.format(i.dueDate))
                }
            }
        }
        if (itemsModified) {
            save(context)
            updateVisibleTasks()
        }
    }

    fun updateVisibleTasks(){
        visibleTasks.clear()
        for (t in items){
            if (Task.shouldDisplay(t))
                visibleTasks.add(t)
        }
        visibleTasks= ArrayList(visibleTasks.sortedWith(Task.DateCompare()))
        items= ArrayList(items.sortedWith(Task.DateCompare()))
    }

    fun getTaskByUUID(uuid: UUID): Task?{
        for(task in items){
            if(task.uuid == uuid){
                return task
            }
        }
        return null
    }
}