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

package com.kynetics.uf.clientexample.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.clientexample.R
import com.kynetics.uf.clientexample.adapter.MyAdapter
import java.util.*

/**
 * @author Daniele Sergio
 */
class LogFragment : Fragment(), UFServiceInteractionFragment {

    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: MyAdapter? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null
    private var mTextViewEvent: TextView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_log, container, false)
        mRecyclerView = view.findViewById(R.id.my_recycler_view)
        mTextViewEvent = view.findViewById(R.id.last_event_text_view)
        mRecyclerView!!.setHasFixedSize(true)
        mLayoutManager = LinearLayoutManager(context)
        mRecyclerView!!.layoutManager = mLayoutManager
        mAdapter = MyAdapter(ArrayList())
        mRecyclerView!!.adapter = mAdapter
        return view
    }

    override fun onMessageReceived(message: UFServiceMessageV1) {
        val now = Date()
        val messageV1 = """
            $now
            $message
            ${message.description}""".trimIndent()

        val messageV2 = """
            $now
            $message
        """.trimIndent()

        val finalMessage = if(messageV1.length > messageV2.length) messageV1 else messageV2

        when (message) {
            is UFServiceMessageV1.Event.Polling -> {
                if(currentState is UFServiceMessageV1.State.Waiting) {
                    mTextViewEvent?.visibility = View.VISIBLE
                    mTextViewEvent?.text = finalMessage
                }
            }

            is UFServiceMessageV1.Event -> {
                mTextViewEvent?.visibility = View.VISIBLE
                mTextViewEvent?.text = finalMessage
            }

            is UFServiceMessageV1.State -> {
                currentState = message
                mAdapter!!.addItem(finalMessage)
                mRecyclerView!!.post { mRecyclerView!!.smoothScrollToPosition(mAdapter!!.itemCount) }
            }

        }
    }

    var currentState:UFServiceMessageV1.State? = null

    companion object {

        fun newInstance(): LogFragment {
            return LogFragment()
        }
    }
}
