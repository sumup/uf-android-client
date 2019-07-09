/*
 *
 *  Copyright © 2017-2019  Kynetics  LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.uf.clientexample.adapter

import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.kynetics.uf.clientexample.R
import com.kynetics.uf.clientexample.adapter.MyAdapter.ViewHolder

/**
 * @author Daniele Sergio
 */
class MyAdapter(private val mDataset: MutableList<String>) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

    class ViewHolder(v: CardView) : RecyclerView.ViewHolder(v) {
        var mTextView: TextView

        init {
            mTextView = v.findViewById<View>(R.id.info_text) as TextView
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyAdapter.ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.log_card, parent, false) as CardView
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mTextView.text = mDataset[position]
    }

    override fun getItemCount(): Int {
        return mDataset.size
    }

    fun addItem(item: String) {
        mDataset.add(item)
        notifyDataSetChanged()
    }
}