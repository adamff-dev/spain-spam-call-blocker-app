package com.addev.listaspam.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.addev.listaspam.R
import com.addev.listaspam.util.CallLogEntry
import com.addev.listaspam.util.SpamUtils
import com.addev.listaspam.util.addNumberToWhitelist
import com.addev.listaspam.util.removeSpamNumber
import com.addev.listaspam.util.removeWhitelistNumber
import com.addev.listaspam.util.saveSpamNumber
import java.text.SimpleDateFormat

class CallLogAdapter(
    private val context: Context,
    var callLogs: List<CallLogEntry>,
    var blockedNumbers: Set<String>,
    var whitelistNumbers: Set<String>
) : RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder>() {

    interface OnItemChangedListener {
        fun onItemChanged(number: String)
    }

    companion object {
        // URLs
        const val GOOGLE_URL_TEMPLATE = "https://www.google.com/search?q=%s"
        const val REPORT_URL_TEMPLATE = "https://www.listaspam.com/busca.php?Telefono=%s#denuncia"
    }

    private val formatter = SimpleDateFormat("dd/MM/yyyy HH:ss")
    private var onItemChangedListener: OnItemChangedListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_call_log, parent, false)
        return CallLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallLogViewHolder, position: Int) {
        val callLog = callLogs[position]
        holder.bind(callLog, blockedNumbers.contains(callLog.number), whitelistNumbers.contains(callLog.number))
    }

    override fun getItemCount(): Int = callLogs.size

    inner class CallLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val numberTextView: TextView = itemView.findViewById(R.id.numberTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val durationTextView: TextView = itemView.findViewById(R.id.durationTextView)
        private val overflowMenuButton = itemView.findViewById<ImageButton>(R.id.overflowMenuButton)

        fun bind(callLog: CallLogEntry, isBlocked: Boolean, isWhitelisted: Boolean = false) {
            val number = callLog.number ?: ""
            val textToShow = if (isBlocked) {
                context.getString(R.string.blocked_text_format, number)
            } else if (isWhitelisted) {
                context.getString(R.string.whitelisted_text_format, number)
            } else {
                number
            }
            numberTextView.text = textToShow
            dateTextView.text = formatter.format(callLog.date)
            durationTextView.text = context.getString(R.string.duration_label, callLog.duration)

            if (isBlocked) {
                numberTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
            } else if (isWhitelisted) {
                numberTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            } else {
                numberTextView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            }

            overflowMenuButton.setOnClickListener {
                val popupMenu = PopupMenu(itemView.context, overflowMenuButton, Gravity.NO_GRAVITY, android.R.attr.popupMenuStyle, R.style.PopupMenuStyle)
                popupMenu.inflate(R.menu.item_actions)

                setDynamicTitles(popupMenu, isBlocked, isWhitelisted)

                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.search_action -> {
                            searchAction(number)
                            true
                        }
                        R.id.report_action -> {
                            reportAction(number)
                            true
                        }
                        R.id.whitelist_action -> {
                            if (isWhitelisted) {
                                removeWhitelistNumber(context, number)
                            } else {
                                addNumberToWhitelist(context, number)
                            }
                            onItemChangedListener?.onItemChanged(number)
                            true
                        }
                        R.id.block_action -> {
                            if (isBlocked) {
                                removeSpamNumber(context, number)
                            } else {
                                saveSpamNumber(context, number)
                            }
                            onItemChangedListener?.onItemChanged(number)
                            true
                        }
                        else -> false
                    }
                }
                popupMenu.show()
            }

            // Copy number to clipboard
            itemView.setOnLongClickListener {
                clipboardAction(number)
                true
            }
        }

        private fun setDynamicTitles(
            popupMenu: PopupMenu,
            isBlocked: Boolean,
            isWhitelisted: Boolean
        ) {
            val blockMenuItem = popupMenu.menu.findItem(R.id.block_action)
            val whitelistMenuItem = popupMenu.menu.findItem(R.id.whitelist_action)
            if (isBlocked) {
                blockMenuItem.setTitle(R.string.unblock)
            } else {
                blockMenuItem.setTitle(R.string.block)
            }

            if (isWhitelisted) {
                whitelistMenuItem.setTitle(R.string.remove_from_whitelist)
            } else {
                whitelistMenuItem.setTitle(R.string.add_to_whitelist)
            }
        }
    }

    private fun clipboardAction(number: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("phone number", number)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            context,
            context.getString(R.string.number_copied_to_clipboard),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun reportAction(number: String) {
        val url = String.format(REPORT_URL_TEMPLATE, number)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    private fun searchAction(number: String) {
        val url = String.format(GOOGLE_URL_TEMPLATE, number)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    fun setOnItemChangedListener(listener: OnItemChangedListener) {
        this.onItemChangedListener = listener
    }
}