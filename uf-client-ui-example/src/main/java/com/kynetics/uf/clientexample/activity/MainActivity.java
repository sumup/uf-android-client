/*
 * Copyright © 2017-2018  Kynetics  LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.clientexample.activity;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.kynetics.uf.android.api.UFServiceCommunicationConstants;
import com.kynetics.uf.android.api.UFServiceConfiguration;
import com.kynetics.uf.android.api.UFServiceMessage;
import com.kynetics.uf.clientexample.BuildConfig;
import com.kynetics.uf.clientexample.R;
import com.kynetics.uf.clientexample.fragment.ConfigurationFragment;
import com.kynetics.uf.clientexample.fragment.LogFragment;
import com.kynetics.uf.clientexample.fragment.UFServiceInteractionFragment;

import java.io.Serializable;
import java.util.List;

import static android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.ACTION_SETTINGS;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_AUTHORIZATION_RESPONSE;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_SERVICE_CONFIGURATION_STATUS;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_SYNCH_REQUEST;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.SERVICE_DATA_KEY;

/**
 * @author Daniele Sergio
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, UFActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResumeUpdateFab = findViewById(R.id.fab_resume_update);
        mResumeUpdateFab.setOnClickListener(view -> {
            final Message msg = Message.obtain(null,
                    UFServiceCommunicationConstants.MSG_RESUME_SUSPEND_UPGRADE);
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Toast.makeText(MainActivity.this, "service communication error",
                        Toast.LENGTH_SHORT).show();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, null, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setCheckedItem(R.id.menu_settings);
        mNavigationView.setNavigationItemSelectedListener(this);
        changePage(LogFragment.newInstance());
        mNavigationView.setCheckedItem(R.id.menu_log);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final TextView textViewUiVersion = mNavigationView.findViewById(R.id.ui_version);
        final TextView textViewServiceVersion = mNavigationView.findViewById(R.id.service_version);
        textViewUiVersion.setText(String.format(getString(R.string.ui_version), BuildConfig.VERSION_NAME));
        try{
            final PackageInfo pinfo =
                    getPackageManager().getPackageInfo("com.kynetics.uf.service", 0);
            textViewServiceVersion.setText(String.format(getString(R.string.service_version), pinfo.versionName));
        }catch (PackageManager.NameNotFoundException e) {
            textViewServiceVersion.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        doBindService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        doUnbindService();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        final int id = item.getItemId();
        switch (id){
            case R.id.menu_settings:
                changePage(ConfigurationFragment.newInstance());
                break;
            case R.id.menu_default_settings:
                final Intent settingsIntent = new Intent(ACTION_SETTINGS);
                startActivity(settingsIntent);
                break;
            case R.id.menu_log:
                changePage(LogFragment.newInstance());
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /** Messenger for communicating with service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;

    @Override
    public void registerToService(Bundle data) {
        try {
            Message msg = Message.obtain(null,
                    UFServiceCommunicationConstants.MSG_CONFIGURE_SERVICE);
            msg.replyTo = mMessenger;

            msg.setData(data);
            mService.send(msg);


        } catch (RemoteException e) {
            Toast.makeText(MainActivity.this, "service communication error",
                    Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UFServiceCommunicationConstants.MSG_SEND_STRING:
                    UFServiceMessage messageObj = (UFServiceMessage) msg.getData().getSerializable(SERVICE_DATA_KEY);
                    String messageString = String.format(MESSAGE_TEMPLATE,
                            messageObj.getDateTime(),
                            messageObj.getOldState(),
                            messageObj.getEventName(),
                            messageObj.getCurrentState());
                    ((UFServiceInteractionFragment) MainActivity.this.getSupportFragmentManager().findFragmentById(R.id.fragment_content))
                            .onMessageReceived(messageString);
                    switch (messageObj.getSuspend()){
                        case NONE:
                            mResumeUpdateFab.setVisibility(View.GONE);
                            break;
                        case DOWNLOAD:
                            mResumeUpdateFab.setImageResource(R.drawable.ic_get_app_black_48dp);
                            mResumeUpdateFab.setVisibility(View.VISIBLE);
                            break;
                        case UPDATE:
                            mResumeUpdateFab.setImageResource(R.drawable.ic_loop_black_48dp);
                            mResumeUpdateFab.setVisibility(View.VISIBLE);
                            break;
                    }
                    break;
                case UFServiceCommunicationConstants.MSG_AUTHORIZATION_REQUEST:
                    DialogFragment newFragment = MainActivity.MyAlertDialogFragment.newInstance(
                            msg.getData().getString(SERVICE_DATA_KEY));
                    newFragment.show(getSupportFragmentManager(), null);
                    break;
                case MSG_SERVICE_CONFIGURATION_STATUS:
                    final Serializable serializable = msg.getData().getSerializable(SERVICE_DATA_KEY);
                    if(!(serializable instanceof UFServiceConfiguration) ||
                            !((UFServiceConfiguration)serializable).isEnable()) {
                        mNavigationView.setCheckedItem(R.id.menu_settings);
                        changePage(ConfigurationFragment.newInstance());
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void sendPermissionResponse(boolean response){
        Message msg = Message.obtain(null, MSG_AUTHORIZATION_RESPONSE);
        msg.getData().putBoolean(SERVICE_DATA_KEY, response);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new MainActivity.IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = new Messenger(service);

            Toast.makeText(MainActivity.this, R.string.connected,
                    Toast.LENGTH_SHORT).show();
            try {
                Message msg = Message.obtain(null,
                        UFServiceCommunicationConstants.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
                msg = Message.obtain(null, MSG_SYNCH_REQUEST);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                Toast.makeText(MainActivity.this, "service communication error",
                        Toast.LENGTH_SHORT).show();
            }
            mIsBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            Toast.makeText(MainActivity.this, R.string.disconnected,
                    Toast.LENGTH_SHORT).show();
            mIsBound = false;
        }
    };

    public void checkBatteryState() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, filter);

        int chargeState = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        String strState;

        switch (chargeState) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
            case BatteryManager.BATTERY_STATUS_FULL:
                strState = "charging";
                break;
            default:
                strState = "not charging";
        }

        IntentFilter filter2 = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = registerReceiver(null, filter);
        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        Toast.makeText(getApplicationContext(),strState, Toast.LENGTH_LONG);
    }

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        final Intent intent = new Intent(UFServiceCommunicationConstants.SERVICE_ACTION);
        intent.setPackage(UFServiceCommunicationConstants.SERVICE_PACKAGE_NAME);
        intent.setFlags(FLAG_INCLUDE_STOPPED_PACKAGES);
        final boolean serviceExist = bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        if(!serviceExist){
            Toast.makeText(getApplicationContext(), "UpdateFactoryService not found",Toast.LENGTH_LONG).show();
            unbindService(mConnection);
            this.finish();
        }
    }

    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            UFServiceCommunicationConstants.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    public static class MyAlertDialogFragment extends DialogFragment {
        private static final String ARG_DIALOG_TYPE = "DIALOG_TYPE";
        public static MainActivity.MyAlertDialogFragment newInstance(String dialogType) {
            MainActivity.MyAlertDialogFragment frag = new MainActivity.MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_DIALOG_TYPE, dialogType);
            frag.setArguments(args);
            frag.setCancelable(false);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String dialogType = getArguments().getString(ARG_DIALOG_TYPE);
            final int titleResource = getResources().getIdentifier(String.format("%s_%s",dialogType.toLowerCase(), "title"),
                    "string", getActivity().getPackageName());
            final int contentResource =  getResources().getIdentifier(String.format("%s_%s",dialogType.toLowerCase(), "content"),
                    "string", getActivity().getPackageName());

            return new AlertDialog.Builder(getActivity())
                    //.setIcon(R.drawable.alert_dialog_icon)
                    .setTitle(titleResource)
                    .setMessage(contentResource)
                    .setPositiveButton(android.R.string.ok,
                            (dialog, whichButton) -> ((MainActivity)getActivity()).sendPermissionResponse(true)
                    )
                    .setNegativeButton(android.R.string.cancel,
                            (dialog, whichButton) -> ((MainActivity)getActivity()).sendPermissionResponse(false)
                    )
                    .create();
        }
    }

    private void changePage(Fragment fragment){
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragment_content, fragment);
        tx.commit();
    }

    private FloatingActionButton mResumeUpdateFab;
    private NavigationView mNavigationView;

    private static final String MESSAGE_TEMPLATE = "%s: (%s,%s) -> %s";
}
