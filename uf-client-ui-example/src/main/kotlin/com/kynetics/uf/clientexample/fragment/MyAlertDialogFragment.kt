package com.kynetics.uf.clientexample.fragment

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import com.kynetics.uf.clientexample.activity.MainActivity

class MyAlertDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogType = arguments!!.getString(ARG_DIALOG_TYPE)
        val titleResource = resources.getIdentifier(String.format("%s_%s", dialogType.toLowerCase(), "title"),
            "string", activity!!.packageName)
        val contentResource = resources.getIdentifier(String.format("%s_%s", dialogType.toLowerCase(), "content"),
            "string", activity!!.packageName)

        return AlertDialog.Builder(activity!!)
            // .setIcon(R.drawable.alert_dialog_icon)
            .setTitle(titleResource)
            .setMessage(contentResource)
            .setPositiveButton(android.R.string.ok
            ) { dialog, whichButton -> (activity as MainActivity).sendPermissionResponse(true) }
            .setNegativeButton(android.R.string.cancel
            ) { dialog, whichButton -> (activity as MainActivity).sendPermissionResponse(false) }
            .create()
    }

    companion object {
        private val ARG_DIALOG_TYPE = "DIALOG_TYPE"
        fun newInstance(dialogType: String): MyAlertDialogFragment {
            val frag = MyAlertDialogFragment()
            val args = Bundle()
            args.putString(ARG_DIALOG_TYPE, dialogType)
            frag.arguments = args
            frag.isCancelable = false
            return frag
        }
    }
}
