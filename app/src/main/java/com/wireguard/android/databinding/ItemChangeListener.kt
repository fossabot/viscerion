/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.databinding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableList
import androidx.databinding.ViewDataBinding
import com.wireguard.android.BR
import java.lang.ref.WeakReference

/**
 * Helper class for binding an ObservableList to the children of a ViewGroup.
 */

internal class ItemChangeListener<T>(private val container: ViewGroup, private val layoutId: Int) {
    private val callback = OnListChangedCallback(this)
    private val layoutInflater: LayoutInflater = LayoutInflater.from(container.context)
    private var list: ObservableList<T>? = null

    private fun getView(position: Int, convertView: View?): View? {
        var binding: ViewDataBinding? = if (convertView != null) DataBindingUtil.getBinding(convertView) else null
        if (binding == null) {
            binding = DataBindingUtil.inflate(layoutInflater, layoutId, container, false)
        }

        requireNotNull(list) { "Trying to get a view while list is still null" }

        binding?.setVariable(BR.collection, list)
        list?.let { binding?.setVariable(BR.item, it[position]) }
        binding?.executePendingBindings()
        return binding?.root
    }

    fun setList(newList: ObservableList<T>?) {
        if (list != null)
            list?.removeOnListChangedCallback(callback)
        list = newList
        if (list != null) {
            list?.let {
                it.addOnListChangedCallback(callback)
                callback.onChanged(it)
            }
        } else {
            container.removeAllViews()
        }
    }

    private class OnListChangedCallback<T>(listener: ItemChangeListener<T>) :
            ObservableList.OnListChangedCallback<ObservableList<T>>() {

        private val weakListener: WeakReference<ItemChangeListener<T>> = WeakReference(listener)

        override fun onChanged(sender: ObservableList<T>) {
            val listener = weakListener.get()
            if (listener != null) {
                // TODO: recycle views
                listener.container.removeAllViews()
                sender.indices.forEach {
                    listener.container.addView(listener.getView(it, null))
                }
            } else {
                sender.removeOnListChangedCallback(this)
            }
        }

        override fun onItemRangeChanged(
            sender: ObservableList<T>,
            positionStart: Int,
            itemCount: Int
        ) {
            val listener = weakListener.get()
            if (listener != null) {
                for (i in positionStart until positionStart + itemCount) {
                    val child = listener.container.getChildAt(i)
                    listener.container.removeViewAt(i)
                    listener.container.addView(listener.getView(i, child))
                }
            } else {
                sender.removeOnListChangedCallback(this)
            }
        }

        override fun onItemRangeInserted(
            sender: ObservableList<T>,
            positionStart: Int,
            itemCount: Int
        ) {
            val listener = weakListener.get()
            if (listener != null) {
                for (i in positionStart until positionStart + itemCount)
                    listener.container.addView(listener.getView(i, null))
            } else {
                sender.removeOnListChangedCallback(this)
            }
        }

        override fun onItemRangeMoved(
            sender: ObservableList<T>,
            fromPosition: Int,
            toPosition: Int,
            itemCount: Int
        ) {
            val listener = weakListener.get()
            if (listener != null) {
                val views = arrayOfNulls<View>(itemCount)
                for (i in 0 until itemCount)
                    views[i] = listener.container.getChildAt(fromPosition + i)
                listener.container.removeViews(fromPosition, itemCount)
                for (i in 0 until itemCount)
                    listener.container.addView(views[i], toPosition + i)
            } else {
                sender.removeOnListChangedCallback(this)
            }
        }

        override fun onItemRangeRemoved(
            sender: ObservableList<T>,
            positionStart: Int,
            itemCount: Int
        ) {
            val listener = weakListener.get()
            if (listener != null) {
                listener.container.removeViews(positionStart, itemCount)
            } else {
                sender.removeOnListChangedCallback(this)
            }
        }
    }
}
