package com.kynetics.uf.clientexample.fragment

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.clientexample.R
import com.kynetics.uf.clientexample.activity.MainActivity
import com.kynetics.uf.clientexample.dummy.MessageHistory
import com.kynetics.uf.clientexample.dummy.toDate
import kotlinx.android.synthetic.main.state_list_content.view.*
import kotlinx.android.synthetic.main.state_list_fragment.view.*

class ListStateFragment : Fragment(), UFServiceInteractionFragment {

    var twoPane = false
    var selectedItem = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_TWO_PANE)) {
                // Load the dummy content specified by the fragment
                // arguments. In a real-world scenario, use a Loader
                // to load content from a content provider.
                twoPane = it.getBoolean(ARG_TWO_PANE)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.state_list_fragment, container, false)

        setupRecyclerView(rootView.state_list_recycler_view)
        return rootView
    }

    override fun onAttachFragment(childFragment: Fragment?) {
        super.onAttachFragment(childFragment)
        adapter?.notifyDataSetChanged()
    }

    var adapter: SimpleItemRecyclerViewAdapter? = null

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        adapter = SimpleItemRecyclerViewAdapter(this.activity!!, MessageHistory.ITEMS, twoPane)
        recyclerView.adapter = adapter
    }

    inner class SimpleItemRecyclerViewAdapter(
        private val parentActivity: FragmentActivity,
        private val values: List<MessageHistory.StateEntry>,
        private val twoPane: Boolean
    ) :
            RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as MessageHistory.StateEntry
                val itemIndex = MessageHistory.ITEMS.indexOf(item)

                val fragment = StateDetailFragment().apply {
                    arguments = Bundle().apply {
                        putLong(StateDetailFragment.ARG_ITEM_ID, item.id)
                    }
                }

                item.unread = 0

                adapter!!.notifyDataSetChanged()

                if (twoPane) {

                    selectedItem = if (itemIndex == selectedItem) -1 else itemIndex

                    parentActivity.supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.state_detail_container, fragment)
                            .commit()
                } else {
                    (parentActivity as MainActivity).changePage(fragment)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.state_list_content, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.idView.text = item.id.toDate()
            holder.contentView.text = item.state.name.toString()
            holder.badge.visibility = if (item.unread == 0) View.GONE else View.VISIBLE
            holder.badge.text = item.unread.toString()
            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }

            if (selectedItem == position) {
                (holder.contentView.parent.parent as ViewGroup).setBackgroundColor(Color.LTGRAY)
            } else {
                (holder.contentView.parent.parent as ViewGroup).setBackgroundColor(Color.WHITE)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val idView: TextView = view.date_text
            val contentView: TextView = view.state_name
            val badge: TextView = view.unread_event
        }
    }

    override fun onMessageReceived(message: UFServiceMessageV1) {
        if (selectedItem >= 0) {
            MessageHistory.ITEMS[selectedItem].unread = 0
        }
        adapter?.notifyDataSetChanged()
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_TWO_PANE = "two_pane"
    }
}
