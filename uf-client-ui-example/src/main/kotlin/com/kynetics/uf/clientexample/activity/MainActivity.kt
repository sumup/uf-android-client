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

package com.kynetics.uf.clientexample.activity

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.kynetics.uf.android.api.ApiCommunicationVersion
import com.kynetics.uf.android.api.UFServiceCommunicationConstants
import com.kynetics.uf.android.api.UFServiceCommunicationConstants.ACTION_SETTINGS
import com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_AUTHORIZATION_REQUEST
import com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_AUTHORIZATION_RESPONSE
import com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_CONFIGURE_SERVICE
import com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_FORCE_PING
import com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_REGISTER_CLIENT
import com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_SERVICE_CONFIGURATION_STATUS
import com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_SERVICE_STATUS
import com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_SYNC_REQUEST
import com.kynetics.uf.android.api.UFServiceCommunicationConstants.SERVICE_API_VERSION_KEY
import com.kynetics.uf.android.api.UFServiceCommunicationConstants.SERVICE_DATA_KEY
import com.kynetics.uf.android.api.UFServiceConfiguration
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.clientexample.BuildConfig
import com.kynetics.uf.clientexample.R
import com.kynetics.uf.clientexample.dummy.MessageHistory
import com.kynetics.uf.clientexample.fragment.ListStateFragment
import com.kynetics.uf.clientexample.fragment.UFServiceInteractionFragment
import kotlinx.android.synthetic.main.state_list.*

/**
 * @author Daniele Sergio
 */
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, UFActivity {

    private var twoPane: Boolean = false

    /** Messenger for communicating with service.  */
    internal var mService: Messenger? = null
    /** Flag indicating whether we have called bind on the service.  */
    internal var mIsBound: Boolean = false

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    internal val mMessenger = Messenger(this.IncomingHandler())

    /**
     * Class for interacting with the main interface of the service.
     */
    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            mService = Messenger(service)

            Toast.makeText(this@MainActivity, R.string.connected,
                    Toast.LENGTH_SHORT).show()
            try {
                var msg = Message.obtain(null, MSG_REGISTER_CLIENT)
                msg.replyTo = mMessenger
                val bundleWithApiVersion = Bundle()
                bundleWithApiVersion.putInt(SERVICE_API_VERSION_KEY, ApiCommunicationVersion.V1.versionCode)
                msg.data = bundleWithApiVersion
                mService!!.send(msg)
                msg = Message.obtain(null, MSG_SYNC_REQUEST)
                msg.replyTo = mMessenger
                msg.data = bundleWithApiVersion
                mService!!.send(msg)
            } catch (e: RemoteException) {
                Toast.makeText(this@MainActivity, "service communication error",
                        Toast.LENGTH_SHORT).show()
            }

            mIsBound = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mService = null
            Toast.makeText(this@MainActivity, R.string.disconnected,
                    Toast.LENGTH_SHORT).show()
            mIsBound = false
        }
    }

    private var mResumeUpdateFab: FloatingActionButton? = null
    private var mNavigationView: NavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mResumeUpdateFab = findViewById(R.id.fab_resume_update)
        mResumeUpdateFab!!.setOnClickListener { view ->
            val msg = Message.obtain(null,
                    UFServiceCommunicationConstants.MSG_RESUME_SUSPEND_UPGRADE)
            try {
                mService!!.send(msg)
            } catch (e: RemoteException) {
                Toast.makeText(this@MainActivity, "service communication error",
                        Toast.LENGTH_SHORT).show()
            }
        }

        snackbar = Snackbar.make(findViewById<View>(R.id.coordinatorLayout), "Annotations", Snackbar.LENGTH_INDEFINITE)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
                this, drawer, null, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationViewWrapper = findViewById<NavigationView>(R.id.nav_view_wrapper)

        mNavigationView = navigationViewWrapper.findViewById(R.id.nav_view)
        mNavigationView!!.setCheckedItem(R.id.menu_settings)
        mNavigationView!!.setNavigationItemSelectedListener(this)
//        changePage(LogFragment.newInstance())
//        mNavigationView!!.setCheckedItem(R.id.menu_log)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val textViewUiVersion = navigationViewWrapper.findViewById<TextView>(R.id.ui_version)
        val textViewServiceVersion = navigationViewWrapper.findViewById<TextView>(R.id.service_version)
        textViewUiVersion.text = String.format(getString(R.string.ui_version), BuildConfig.VERSION_NAME)
        try {
            val pinfo = packageManager.getPackageInfo("com.kynetics.uf.service", 0)
            textViewServiceVersion.text = String.format(getString(R.string.service_version), pinfo.versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            textViewServiceVersion.visibility = View.GONE
        }

        twoPane = state_detail_container != null
        val listStateFragment = ListStateFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ListStateFragment.ARG_TWO_PANE, this@MainActivity.twoPane)
            }
        }
        if (twoPane) {
            this.supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.state_list_container, listStateFragment)
                    .commit()
        } else {
            changePage(listStateFragment, false)
        }
    }

    override fun onStart() {
        super.onStart()
        doBindService()
    }

    override fun onStop() {
        super.onStop()
        doUnbindService()
    }

    override fun onBackPressed() {
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        when {
            drawer.isDrawerOpen(GravityCompat.START) -> drawer.closeDrawer(GravityCompat.START)
            !twoPane -> onBackPressedWithOnePane()
            else -> super.onBackPressed()
        }
    }

    private fun onBackPressedWithOnePane() {
        val count = supportFragmentManager.backStackEntryCount
        if (count == 0) {
            super.onBackPressed()
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId
        when (id) {
            R.id.menu_settings -> {
                val settingsIntent = Intent(ACTION_SETTINGS)
                startActivity(settingsIntent)
            }
//            R.id.menu_log -> changePage(LogFragment.newInstance())
            R.id.force_ping -> {
                Log.d(TAG, "Force Ping Request")
                try {
                    if (mService != null) {
                        mService!!.send(Message.obtain(null, MSG_FORCE_PING))
                    }
                } catch (e: RemoteException) {
                    Log.d(TAG, "Failed to send force ping", e)
                }
            }

            R.id.menu_back -> onBackPressedWithOnePane()
        }

        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun registerToService(data: Bundle) {
        try {
            val msg = Message.obtain(null, MSG_CONFIGURE_SERVICE)
            msg.replyTo = mMessenger
            data.putInt(SERVICE_API_VERSION_KEY, ApiCommunicationVersion.V1.versionCode)
            msg.data = data
            mService!!.send(msg)
        } catch (e: RemoteException) {
            Toast.makeText(this@MainActivity, "service communication error",
                    Toast.LENGTH_SHORT).show()
        }
    }

    var snackbar: Snackbar? = null

    /**
     * Handler of incoming messages from service.
     */
    internal inner class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {

                MSG_SERVICE_STATUS -> handleServiceStatusMsg(msg)

                MSG_AUTHORIZATION_REQUEST -> handleAuthorizationRequestMsg(msg)

                MSG_SERVICE_CONFIGURATION_STATUS -> handleServiceConfigurationMsg(msg)

                else -> super.handleMessage(msg)
            }
        }

        private fun handleServiceConfigurationMsg(msg: Message) {
            val serializable = msg.data.getSerializable(SERVICE_DATA_KEY)
            if (serializable !is UFServiceConfiguration || !serializable.isEnable) {
                mNavigationView!!.setCheckedItem(R.id.menu_settings)
                val settingsIntent = Intent(ACTION_SETTINGS)
                startActivity(settingsIntent)
            }
        }

        private fun handleAuthorizationRequestMsg(msg: Message) {
            val newFragment = MyAlertDialogFragment.newInstance(
                msg.data.getString(SERVICE_DATA_KEY)
            )
            newFragment.show(supportFragmentManager, null)
        }

        private fun handleServiceStatusMsg(msg: Message) {
            val messageContent = UFServiceMessageV1.fromJson(msg.data.getString(SERVICE_DATA_KEY))

            when (messageContent) {
                is UFServiceMessageV1.Event -> {
                    MessageHistory.appendEvent(messageContent)
                }
                is UFServiceMessageV1.State -> {
                    MessageHistory.addState(MessageHistory.StateEntry(state = messageContent))
                }
            }

            this@MainActivity.supportFragmentManager.fragments
                .filterIsInstance<UFServiceInteractionFragment>()
                .forEach { fragment -> fragment.onMessageReceived(messageContent) }

            when (messageContent) {
                is UFServiceMessageV1.State.WaitingDownloadAuthorization -> {
                    mResumeUpdateFab!!.setImageResource(R.drawable.ic_get_app_black_48dp)
                    mResumeUpdateFab!!.show()
                }

                is UFServiceMessageV1.State.WaitingUpdateAuthorization -> {
                    mResumeUpdateFab!!.setImageResource(R.drawable.ic_loop_black_48dp)
                    mResumeUpdateFab!!.show()
                }

                is UFServiceMessageV1.State -> mResumeUpdateFab!!.hide()

                else -> { }
            }
        }
    }

    private fun sendPermissionResponse(response: Boolean) {
        val msg = Message.obtain(null, MSG_AUTHORIZATION_RESPONSE)
        msg.data.putBoolean(SERVICE_DATA_KEY, response)
        try {
            mService!!.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun checkBatteryState() {
/*
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, filter)

        val chargeState = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val strState: String

        when (chargeState) {
            BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL -> strState = "charging"
            else -> strState = "not charging"
        }

        val filter2 = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = registerReceiver(null, filter)
        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

        Toast.makeText(applicationContext, strState, Toast.LENGTH_LONG)
*/
        TODO("Not yet implemented")
    }

    private fun doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        val intent = Intent(UFServiceCommunicationConstants.SERVICE_ACTION)
        intent.setPackage(UFServiceCommunicationConstants.SERVICE_PACKAGE_NAME)
        intent.flags = FLAG_INCLUDE_STOPPED_PACKAGES
        val serviceExist = bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        if (!serviceExist) {
            Toast.makeText(applicationContext, "UpdateFactoryService not found", Toast.LENGTH_LONG).show()
            unbindService(mConnection)
            this.finish()
        }
    }

    private fun doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    val msg = Message.obtain(null,
                            UFServiceCommunicationConstants.MSG_UNREGISTER_CLIENT)
                    msg.replyTo = mMessenger
                    mService!!.send(msg)
                } catch (e: RemoteException) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mConnection)
            mIsBound = false
        }
    }

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

    fun changePage(fragment: Fragment, addToBackStack: Boolean = true) {
        val tx = supportFragmentManager.beginTransaction()
                .replace(R.id.main_content, fragment)

        if (addToBackStack) {
            tx.addToBackStack(null)
        }

        tx.commit()
    }

    companion object {

        private val TAG = MainActivity::class.java.simpleName

        private val MESSAGE_TEMPLATE = "%s: %s"
    }
}
