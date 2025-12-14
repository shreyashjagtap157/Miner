package com.meetmyartist.miner.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.meetmyartist.miner.MainActivity
import com.meetmyartist.miner.R

class MiningWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_UPDATE_WIDGET -> {
                val hashrate = intent.getDoubleExtra(EXTRA_HASHRATE, 0.0)
                val temperature = intent.getFloatExtra(EXTRA_TEMPERATURE, 0f)
                val isMining = intent.getBooleanExtra(EXTRA_IS_MINING, false)
                val uptime = intent.getLongExtra(EXTRA_UPTIME, 0L)
                
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, MiningWidgetProvider::class.java)
                )
                
                widgetIds.forEach { widgetId ->
                    updateWidgetWithData(
                        context, appWidgetManager, widgetId,
                        hashrate, temperature, isMining, uptime
                    )
                }
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_mining_status)
        
        // Set up click to open app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // Default values
        views.setTextViewText(R.id.widget_hashrate, "-- H/s")
        views.setTextViewText(R.id.widget_temperature, "-- °C")
        views.setTextViewText(R.id.widget_status, "Tap to open")
        
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun updateWidgetWithData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        hashrate: Double,
        temperature: Float,
        isMining: Boolean,
        uptime: Long
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_mining_status)
        
        // Set up click to open app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        // Update data
        views.setTextViewText(R.id.widget_hashrate, "${hashrate.toInt()} H/s")
        views.setTextViewText(R.id.widget_temperature, "${temperature.toInt()}°C")
        
        val status = if (isMining) {
            val hours = uptime / 3600
            val minutes = (uptime % 3600) / 60
            "Mining: ${hours}h ${minutes}m"
        } else {
            "Stopped"
        }
        views.setTextViewText(R.id.widget_status, status)
        
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.meetmyartist.miner.ACTION_UPDATE_WIDGET"
        const val EXTRA_HASHRATE = "extra_hashrate"
        const val EXTRA_TEMPERATURE = "extra_temperature"
        const val EXTRA_IS_MINING = "extra_is_mining"
        const val EXTRA_UPTIME = "extra_uptime"

        fun sendUpdateBroadcast(
            context: Context,
            hashrate: Double,
            temperature: Float,
            isMining: Boolean,
            uptime: Long
        ) {
            val intent = Intent(context, MiningWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
                putExtra(EXTRA_HASHRATE, hashrate)
                putExtra(EXTRA_TEMPERATURE, temperature)
                putExtra(EXTRA_IS_MINING, isMining)
                putExtra(EXTRA_UPTIME, uptime)
            }
            context.sendBroadcast(intent)
        }
    }
}
