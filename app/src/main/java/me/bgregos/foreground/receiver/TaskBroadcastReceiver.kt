package me.bgregos.foreground.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.bgregos.foreground.tasklist.TaskRepository
import java.util.*
import javax.inject.Inject

class TaskBroadcastReceiver: BroadcastReceiver() {

    @Inject
    lateinit var tasksRepository: TaskRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action) {
            "BRIGHTTASK_MARK_TASK_DONE" -> {
                Log.i("notif", "Received task done")
                val notifID: Int? = intent.extras?.getInt("notification_id")
                if(notifID != null){
                    val mgr = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    mgr.cancel(notifID)
                }
                val uuid: String = (intent.extras?.get("uuid") ?: "") as String
                if(uuid == "") {
                    Log.e("notif", "Failed to mark task done- no UUID")
                }
                if(context != null){
                    CoroutineScope(Dispatchers.Main).launch {
                        tasksRepository.load()
                        val item = tasksRepository.getTaskByUUID(UUID.fromString(uuid))
                        if(item == null) {
                            Log.e("LocalTasks", "Failed to find a task with the given UUID")
                            return@launch
                        }
                        item.modifiedDate=Date() //update modified date
                        item.status = "completed"
                        if (!tasksRepository.localChanges.contains(item)){
                            tasksRepository.localChanges.plus(item)

                        }
                        tasksRepository.tasks.minus(item)
                        tasksRepository.save()
                        val localIntent: Intent = Intent("BRIGHTTASK_REMOTE_TASK_UPDATE") //Send local broadcast
                        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)
                    }
                } else {
                    Log.e("notif", "Failed to save task marked done- null context")
                }

            }
        }
    }
}