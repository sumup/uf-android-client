/*
 * Copyright © 2017   LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.ufandroidclient.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.kynetics.ufandroidclient.R;
import com.kynetics.ufandroidclient.content.SharedPreferencesWithObject;
import com.kynetics.ufclientserviceapi.UFServiceConfiguration;
import com.kynetics.ufclientserviceapi.UFServiceMessage;
import com.kynetics.ufclientserviceapi.UFServiceMessage.Suspend;
import com.kynetics.updatefactory.update.Event;
import com.kynetics.updatefactory.update.State;
import com.kynetics.updatefactory.update.UFService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import static com.kynetics.ufclientserviceapi.UFServiceCommunicationConstants.MSG_AUTHORIZATION_REQUEST;
import static com.kynetics.ufclientserviceapi.UFServiceCommunicationConstants.MSG_AUTHORIZATION_RESPONSE;
import static com.kynetics.ufclientserviceapi.UFServiceCommunicationConstants.MSG_CONFIGURE_SERVICE;
import static com.kynetics.ufclientserviceapi.UFServiceCommunicationConstants.MSG_REGISTER_CLIENT;
import static com.kynetics.ufclientserviceapi.UFServiceCommunicationConstants.MSG_RESUME_SUSPEND_UPGRADE;
import static com.kynetics.ufclientserviceapi.UFServiceCommunicationConstants.MSG_SEND_STRING;
import static com.kynetics.ufclientserviceapi.UFServiceCommunicationConstants.MSG_SERVICE_CONFIGURATION_STATUS;
import static com.kynetics.ufclientserviceapi.UFServiceCommunicationConstants.MSG_SYNCH_REQUEST;
import static com.kynetics.ufclientserviceapi.UFServiceCommunicationConstants.MSG_UNREGISTER_CLIENT;
import static com.kynetics.ufclientserviceapi.UFServiceCommunicationConstants.SERVICE_DATA_KEY;
import static com.kynetics.ufclientserviceapi.UFServiceMessage.Suspend.DOWNLOAD;
import static com.kynetics.ufclientserviceapi.UFServiceMessage.Suspend.NONE;
import static com.kynetics.ufclientserviceapi.UFServiceMessage.Suspend.UPDATE;

/**
 * @author Daniele Sergio
 */
public class UpdateFactoryService extends Service {
    private static final String TAG = UpdateFactoryService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final SharedPreferencesWithObject sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_FILE, MODE_PRIVATE);
        final String username = sharedPreferences.getString(SHARED_PREFERENCES_USERNAME_KEY, "");
        if(!username.isEmpty()){
            final String password = sharedPreferences.getString(SHARED_PREFERENCES_USERNAME_KEY, "");
            final String url = sharedPreferences.getString(SHARED_PREFERENCES_UF_URL_KEY, "");
            final String controllerId = sharedPreferences.getString(SHARED_PREFERENCES_CONTROLLER_ID_KEY, "");
            final String tenant = sharedPreferences.getString(SHARED_PREFERENCES_TENANT_KEY, "");
            final long delay = sharedPreferences.getLong(SHARED_PREFERENCES_RETRY_DELAY_KEY, 30000);
            State initialState = sharedPreferences.getObject(SHARED_PREFERENCES_LAST_STATE_KEY, State.class);
            ufService = UFService.builder()
                    .withUrl(url)
                    .withPassword(password)
                    .withRetryDelayOnCommunicationError(delay)
                    .withUsername(username)
                    .withTenant(tenant)
                    .withControllerId(controllerId)
                    .withInitialState(initialState)
                    .build();
            ufService.addObserver(new ObserverState());
            ufService.start();
            if(initialState.getStateName() == State.StateName.UPDATE_STARTED){
                ufService.setUpdateSucceffullyUpdate(UpdateSystem.successInstallation());
            }
        }
        return START_STICKY;
    }

    private ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            SharedPreferencesWithObject sharedPreferences;
            switch (msg.what) {
                case MSG_CONFIGURE_SERVICE:
                    if(ufService != null){
                        ufService.stop();
                    }

                    final UFServiceConfiguration configuration = (UFServiceConfiguration) msg.getData().getSerializable(SERVICE_DATA_KEY);

                    ufService = UFService.builder()
                            .withUrl(configuration.getUrl())
                            .withUsername(configuration.getUsername())
                            .withPassword(configuration.getPassword())
                            .withRetryDelayOnCommunicationError(configuration.getRetryDelay())
                            .withControllerId(configuration.getControllerId())
                            .withTenant(configuration.getTenant())
                            .build();
                    sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_FILE, MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(SHARED_PREFERENCES_USERNAME_KEY, configuration.getUsername());
                    editor.putString(SHARED_PREFERENCES_PASSWORD_KEY, configuration.getPassword());
                    editor.putString(SHARED_PREFERENCES_CONTROLLER_ID_KEY, configuration.getControllerId());
                    editor.putString(SHARED_PREFERENCES_TENANT_KEY, configuration.getTenant());
                    editor.putString(SHARED_PREFERENCES_UF_URL_KEY, configuration.getUrl());
                    editor.putLong(SHARED_PREFERENCES_RETRY_DELAY_KEY, configuration.getRetryDelay());
                    editor.apply();
                    final ObserverState ob = new ObserverState();
                    ufService.addObserver(ob);
                    ufService.start();
                    break;
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_AUTHORIZATION_RESPONSE:
                    ufService.setAuthorized(msg.getData().getBoolean(SERVICE_DATA_KEY));
                    break;
                case MSG_RESUME_SUSPEND_UPGRADE:
                    ufService.restartSuspendState();
                    break;
                case MSG_SYNCH_REQUEST:
                    if(ufService == null){
                        UpdateFactoryService.this.sendMessage(false, MSG_SERVICE_CONFIGURATION_STATUS, msg.replyTo);
                        return;
                    }
                    UpdateFactoryService.this.sendMessage(true, MSG_SERVICE_CONFIGURATION_STATUS, msg.replyTo);

                    sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_FILE, MODE_PRIVATE);
                    final UFServiceMessage lastMessage = sharedPreferences.getObject(SHARED_PREFERENCES_LAST_NOTIFY_MESSAGE, UFServiceMessage.class);
                    if(lastMessage != null){
                        UpdateFactoryService.this.sendMessage(lastMessage, MSG_SEND_STRING, msg.replyTo);
                    }
                    State lastState = sharedPreferences.getObject(SHARED_PREFERENCES_LAST_STATE_KEY, State.class);
                    if(lastState.getStateName() == State.StateName.AUTHORIZATION_WAITING){
                        UpdateFactoryService.this.sendMessage(((State.AuthorizationWaitingState)lastState).getState().getStateName().name(),
                                MSG_AUTHORIZATION_REQUEST,
                                msg.replyTo);
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void sendMessage(Serializable messageContent, int code, Messenger messenger) {
        final Message message = getMessage(messageContent, code);
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Serializable messageContent, int code){
        final Message message = getMessage(messageContent, code);
        int i = 0;
        while (i<mClients.size()) {
            try {
                mClients.get(i).send(message);
                i++;
            } catch (RemoteException e) {
                mClients.remove(i);
            }
        }
    }

    @NonNull
    private Message getMessage(Serializable messageContent, int code) {
        final Message message = Message.obtain(null, code);
        final Bundle data = new Bundle();
        data.putSerializable(SERVICE_DATA_KEY, messageContent);
        message.setData(data);
        return message;
    }

    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private class ObserverState implements Observer {

        @Override
        public void update(Observable observable, Object o) {
            if(o instanceof UFService.SharedEvent) {
                final UFService.SharedEvent eventNotify = (UFService.SharedEvent) o;
                final Event event = eventNotify.getEvent();
                final State newState = eventNotify.getNewState();

                final UFServiceMessage message = new UFServiceMessage(
                        event.getEventName().name(),
                        eventNotify.getOldState().getStateName().name(),
                        newState.getStateName().name(),
                        getSuspend(newState)
                );
                writeObjectToSharedPreference(message, SHARED_PREFERENCES_LAST_NOTIFY_MESSAGE);
                sendMessage(message, MSG_SEND_STRING);
                writeObjectToSharedPreference(eventNotify.getNewState(), SHARED_PREFERENCES_LAST_STATE_KEY);
                if (event.getEventName() == Event.EventName.FILE_DOWNLOADED) {
                    final Event.FileDownloadedEvent fileDownloadedEvent = (Event.FileDownloadedEvent) event;
                        UpdateSystem.copyFile(fileDownloadedEvent.getInputStream(), fileDownloadedEvent.getFileName());
                        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFERENCES_FILE, MODE_PRIVATE).edit();
                        editor.putString(SHARED_PREFERENCES_FILE_NAME_KEY, fileDownloadedEvent.getFileName());
                        editor.putString(SHARED_PREFERENCES_FILE_SHAE_KEY, fileDownloadedEvent.getShae1());
                        editor.putString(SHARED_PREFERENCES_FILE_MD5_KEY, fileDownloadedEvent.getMd5());
                        editor.apply();
                }

                if (newState.getStateName() == State.StateName.AUTHORIZATION_WAITING) {
                    sendMessage(((State.AuthorizationWaitingState) newState).getState().getStateName().name(), MSG_AUTHORIZATION_REQUEST);
                }

                if (eventNotify.getNewState().getStateName() == State.StateName.UPDATE_STARTED) {
                    final SharedPreferences sharedPreferences =
                            getSharedPreferences(SHARED_PREFERENCES_FILE, MODE_PRIVATE);
                    final String fileName = sharedPreferences.getString(SHARED_PREFERENCES_FILE_NAME_KEY, "");
                    if(UpdateSystem.verify(fileName)){
                        UpdateSystem.install(fileName, getApplicationContext());
                    } else {
                        ufService.setUpdateSucceffullyUpdate(false);
                        Toast.makeText(getApplicationContext(),getString(R.string.invalid_update), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    private Suspend getSuspend(State state){
        if(state.getStateName() != State.StateName.WAITING){
            return NONE;
        }
        State.WaitingState waitingState = (State.WaitingState) state;
        if(!waitingState.hasSuspendState()){
            return NONE;
        }
        return waitingState.getSuspendState().getStateName() == State.StateName.UPDATE_DOWNLOAD ?
                DOWNLOAD : UPDATE;
    }

    @Override
    public SharedPreferencesWithObject getSharedPreferences(String name, int mode){
        return new SharedPreferencesWithObject(super.getSharedPreferences(name, mode));
    }

    private void writeObjectToSharedPreference(Serializable obj, String key){
        final SharedPreferencesWithObject sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_FILE,MODE_PRIVATE);
        sharedPreferences.putAndCommitObject(key, obj);
    }

    private UFService ufService;
    private static final String SHARED_PREFERENCES_LAST_STATE_KEY = "LAST_STATE_KEY";
    private static final String SHARED_PREFERENCES_UF_URL_KEY = "URL_KEY";
    private static final String SHARED_PREFERENCES_USERNAME_KEY = "USERNAME_KEY";
    private static final String SHARED_PREFERENCES_PASSWORD_KEY = "PASSWORD_KEY";
    private static final String SHARED_PREFERENCES_TENANT_KEY = "TENANT_KEY";
    private static final String SHARED_PREFERENCES_CONTROLLER_ID_KEY = "CONTROLLER_ID_KEY";
    private static final String SHARED_PREFERENCES_RETRY_DELAY_KEY = "RETRY_DELAY_KEY";
    private static final String SHARED_PREFERENCES_FILE = "UF_SHARED_FILE";
    private static final String SHARED_PREFERENCES_LAST_NOTIFY_MESSAGE = "LAST_NOTIFY_MESSAGE";

    private static final String SHARED_PREFERENCES_FILE_NAME_KEY = "FILE_NAME_KEY";
    private static final String SHARED_PREFERENCES_FILE_SHAE_KEY = "FILE_SHAE_KEY";
    private static final String SHARED_PREFERENCES_FILE_MD5_KEY = "FILE_MD5_KEY";

}