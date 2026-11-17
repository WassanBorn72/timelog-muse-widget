package app.timelog.musewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.Toast
import java.util.concurrent.Executors

class LifeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { refresh(context) }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != TAP) return
        val type = intent.getStringExtra("type") ?: return
        if (type !in Event.types) return
        // Capture wall-clock time at receipt, before dispatching disk work.
        val event = Event.now(type, Settings(context).repository)
        val pending = goAsync()
        executor.execute {
            try {
                Store(context).use { it.add(event) }
                val hasToken = runCatching { Settings(context).token().isNotBlank() }.getOrDefault(false)
                Settings(context).status = if (hasToken) "送信待ち" else "トークン未設定・端末内に保存しました"
                refresh(context)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, if (hasToken) "記録しました" else "記録しました（トークン未設定）", Toast.LENGTH_SHORT).show()
                }
                SyncWorker.ensurePeriodic(context)
                SyncWorker.enqueue(context)
            } catch (_: Exception) {
                Handler(Looper.getMainLooper()).post { Toast.makeText(context, "保存または送信予約に失敗しました。設定画面を確認してください", Toast.LENGTH_LONG).show() }
            } finally { pending.finish() }
        }
    }
    companion object {
        private const val TAP = "app.timelog.musewidget.TAP"
        private val executor = Executors.newSingleThreadExecutor()
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val views = RemoteViews(context.packageName, R.layout.life_widget)
            val buttons = listOf(R.id.sleep_start, R.id.sleep_end, R.id.departure, R.id.arrival, R.id.schedule_change)
            buttons.zip(Event.types).forEachIndexed { index, (id, type) ->
                val intent = Intent(context, LifeWidget::class.java).setAction(TAP).putExtra("type", type)
                views.setOnClickPendingIntent(id, PendingIntent.getBroadcast(context, index, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            val count = Store(context).use { it.count() }
            views.setTextViewText(R.id.status, "未送信 $count 件\n${Settings(context).status}")
            manager.updateAppWidget(ComponentName(context, LifeWidget::class.java), views)
        }
    }
}
