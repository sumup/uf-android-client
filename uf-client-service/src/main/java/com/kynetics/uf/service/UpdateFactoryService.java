/*
 * Copyright © 2017   LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.service;

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

import com.kynetics.uf.service.api.UFServiceConfiguration;
import com.kynetics.uf.service.api.UFServiceMessage;
import com.kynetics.uf.service.api.UFServiceMessage.Suspend;
import com.kynetics.uf.service.content.SharedPreferencesWithObject;
import com.kynetics.uf.service.ui.MainActivity;
import com.kynetics.updatefactory.ddiclient.core.UFService;
import com.kynetics.updatefactory.ddiclient.core.model.Event;
import com.kynetics.updatefactory.ddiclient.core.model.State;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import okhttp3.OkHttpClient;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.kynetics.uf.service.api.UFServiceCommunicationConstants.MSG_AUTHORIZATION_REQUEST;
import static com.kynetics.uf.service.api.UFServiceCommunicationConstants.MSG_AUTHORIZATION_RESPONSE;
import static com.kynetics.uf.service.api.UFServiceCommunicationConstants.MSG_CONFIGURE_SERVICE;
import static com.kynetics.uf.service.api.UFServiceCommunicationConstants.MSG_REGISTER_CLIENT;
import static com.kynetics.uf.service.api.UFServiceCommunicationConstants.MSG_RESUME_SUSPEND_UPGRADE;
import static com.kynetics.uf.service.api.UFServiceCommunicationConstants.MSG_SEND_STRING;
import static com.kynetics.uf.service.api.UFServiceCommunicationConstants.MSG_SERVICE_CONFIGURATION_STATUS;
import static com.kynetics.uf.service.api.UFServiceCommunicationConstants.MSG_SYNCH_REQUEST;
import static com.kynetics.uf.service.api.UFServiceCommunicationConstants.MSG_UNREGISTER_CLIENT;
import static com.kynetics.uf.service.api.UFServiceCommunicationConstants.SERVICE_DATA_KEY;
import static com.kynetics.uf.service.api.UFServiceMessage.Suspend.DOWNLOAD;
import static com.kynetics.uf.service.api.UFServiceMessage.Suspend.NONE;
import static com.kynetics.uf.service.api.UFServiceMessage.Suspend.UPDATE;

/**
 * @author Daniele Sergio
 */
public class UpdateFactoryService extends Service implements UpdateFactoryServiceCommand {
    private static final String TAG = UpdateFactoryService.class.getSimpleName();

    public static UpdateFactoryServiceCommand getUFServiceCommand(){
        return ufServiceCommand;
    }

    @Override
    public void authorizationGranted() {
        ufService.setAuthorized(true);
    }

    @Override
    public void authorizationDenied() {
        ufService.setAuthorized(false);
    }

    @Override
    public void configureService() {
        if(ufService!=null){
            ufService.stop();
        }
        buildServiceFromPreferences();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initSharedPreferencesKeys();
        ufServiceCommand = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        buildServiceFromPreferences();
        return START_STICKY;
    }

    private OkHttpClient.Builder buildOkHttpClient() {
        return new OkHttpClient.Builder();
    }

    private void buildServiceFromPreferences() {
        final SharedPreferencesWithObject sharedPreferences = getSharedPreferences(sharedPreferencesFile, MODE_PRIVATE);
        final boolean serviceIsEnable = sharedPreferences.getBoolean(sharedPreferencesServiceEnableKey, false);
        if(serviceIsEnable){
            final String url = sharedPreferences.getString(sharedPreferencesServerUrlKey, "");
            final String controllerId = sharedPreferences.getString(sharedPreferencesControllerIdKey, "");
            final String gatewayToken = sharedPreferences.getString(sharedPreferencesGatewayToken, "");
            final String targetToken = sharedPreferences.getString(sharedPreferencesTargetToken, "");
            final String tenant = sharedPreferences.getString(sharedPreferencesTenantKey, "");
            final long delay = Long.parseLong(sharedPreferences.getString(sharedPreferencesRetryDelayKey, "30000"));
            final State initialState = sharedPreferences.getObject(sharedPreferencesCurrentStateKey, State.class, new State.WaitingState(0, null));
            final boolean apiMode = sharedPreferences.getBoolean(sharedPreferencesApiModeKey, true);
            ufService = UFService.builder()
                    .withUrl(url)
                    .withRetryDelayOnCommunicationError(delay)
                    .withTenant(tenant)
                    .withControllerId(controllerId)
                    .withInitialState(initialState)
                    .withOkHttClientBuilder(buildOkHttpClient())
                    .withGatewayToken(gatewayToken)
                    .withTargetToken(targetToken)
                    .withTargetData(this::getMap)
                    .build();
            ufService.addObserver(new ObserverState(apiMode));
            if(initialState.getStateName() == State.StateName.UPDATE_STARTED){
                ufService.setUpdateSucceffullyUpdate(UpdateSystem.successInstallation());
            }
        }
        startStopService(serviceIsEnable);
    }

    public Map<String, String> getMap(){
        final Map<String, String> map = new HashMap<>();
        map.put("test", "test");
        return map;
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
                            .withRetryDelayOnCommunicationError(configuration.getRetryDelay())
                            .withControllerId(configuration.getControllerId())
                            .withTenant(configuration.getTenant())
                            .withOkHttClientBuilder(buildOkHttpClient())
                            .withGatewayToken(configuration.getGatewayToken())
                            .withTargetToken(configuration.getTargetToken())
                            .withTargetData(UpdateFactoryService.this::getMap)
                            .build();
                    sharedPreferences = getSharedPreferences(sharedPreferencesFile, MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(sharedPreferencesControllerIdKey, configuration.getControllerId());
                    editor.putString(sharedPreferencesTenantKey, configuration.getTenant());
                    editor.putString(sharedPreferencesServerUrlKey, configuration.getUrl());
                    editor.putLong(sharedPreferencesRetryDelayKey, configuration.getRetryDelay());
                    editor.putBoolean(sharedPreferencesApiModeKey, configuration.isApiMode());
                    final boolean serviceIsEnable = configuration.isEnalbe();
                    editor.putBoolean(sharedPreferencesServiceEnableKey, serviceIsEnable);
                    editor.apply();
                    final ObserverState ob = new ObserverState(configuration.isApiMode());
                    ufService.addObserver(ob);
                    startStopService(serviceIsEnable);
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

                    sharedPreferences = getSharedPreferences(sharedPreferencesFile, MODE_PRIVATE);
                    final UFServiceMessage lastMessage = sharedPreferences.getObject(SHARED_PREFERENCES_LAST_NOTIFY_MESSAGE, UFServiceMessage.class);
                    if(lastMessage != null){
                        UpdateFactoryService.this.sendMessage(lastMessage, MSG_SEND_STRING, msg.replyTo);
                    }
                    State lastState = sharedPreferences.getObject(sharedPreferencesCurrentStateKey, State.class);
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

    private void startStopService(boolean serviceIsEnable) {
        if(ufService == null){
            return;
        }
        if(serviceIsEnable) {
            ufService.start();
        } else {
            ufService.stop();
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
        private final boolean apiMode;

        public ObserverState(boolean apiMode) {
            this.apiMode = apiMode;
        }

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
                writeObjectToSharedPreference(eventNotify.getNewState(), sharedPreferencesCurrentStateKey);
                switch (newState.getStateName()){
                    case AUTHORIZATION_WAITING:
                        final State.StateName auth = ((State.AuthorizationWaitingState) newState).getState().getStateName();
                        if(apiMode){
                            sendMessage(auth.name(), MSG_AUTHORIZATION_REQUEST);
                        }else {
                            showAuthorizationDialog(auth);
                        }
                        break;
                    case SAVING_FILE:
                        final State.SavingFileState savingFileState = ((State.SavingFileState) newState);
                        final String newFileName = savingFileState.getFileInfo().getLinkInfo().getFileName();
                        SharedPreferences.Editor editor = getSharedPreferences(sharedPreferencesFile, MODE_PRIVATE).edit();
                        editor.putString(SHARED_PREFERENCES_FILE_NAME_KEY, newFileName);
                        editor.apply();
                        UpdateSystem.copyFile(savingFileState.getInputStream(), newFileName);
                    case UPDATE_STARTED:
                        final SharedPreferences sharedPreferences =
                                getSharedPreferences(sharedPreferencesFile, MODE_PRIVATE);
                        final String fileName = sharedPreferences.getString(SHARED_PREFERENCES_FILE_NAME_KEY, "");
                        if(UpdateSystem.verify(fileName)){
                            UpdateSystem.install(fileName, getApplicationContext());
                        } else {
                            ufService.setUpdateSucceffullyUpdate(false);
                            Toast.makeText(getApplicationContext(),getString(R.string.invalid_update), Toast.LENGTH_LONG).show();
                        }
                        break;
                }
            }
        }
    }

    private void showAuthorizationDialog(State.StateName auth) {
        final Intent intent = new Intent(UpdateFactoryService.this, MainActivity.class);
        intent.putExtra(MainActivity.INTENT_TYPE_EXTRA_VARIABLE, auth == State.StateName.UPDATE_DOWNLOAD ?
                MainActivity.INTENT_TYPE_EXTRA_VALUE_DOWNLOAD : MainActivity.INTENT_TYPE_EXTRA_VALUE_REBOOT);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
        final SharedPreferencesWithObject sharedPreferences = getSharedPreferences(sharedPreferencesFile,MODE_PRIVATE);
        sharedPreferences.putAndCommitObject(key, obj);
    }

    private void initSharedPreferencesKeys(){
        sharedPreferencesFile = getString(R.string.shared_preferences_file);
        sharedPreferencesCurrentStateKey = getString(R.string.shared_preferences_current_state_key);
        sharedPreferencesServerUrlKey = getString(R.string.shared_preferences_server_url_key);
        sharedPreferencesApiModeKey = getString(R.string.shared_preferences_api_mode_key);
        sharedPreferencesTenantKey = getString(R.string.shared_preferences_tenant_key);
        sharedPreferencesControllerIdKey = getString(R.string.shared_preferences_controller_id_key);
        sharedPreferencesRetryDelayKey = getString(R.string.shared_preferences_retry_delay_key);
        sharedPreferencesServiceEnableKey = getString(R.string.shared_preferences_is_enable_key);
        sharedPreferencesGatewayToken = getString(R.string.shared_preferences_target_token_key);
        sharedPreferencesTargetToken = getString(R.string.shared_preferences_gateway_token_key);
    }

    private static UpdateFactoryServiceCommand ufServiceCommand;
    private UFService ufService;
    private String sharedPreferencesCurrentStateKey;
    private String sharedPreferencesServerUrlKey;
    private String sharedPreferencesApiModeKey;
    private String sharedPreferencesServiceEnableKey;
    private String sharedPreferencesTenantKey;
    private String sharedPreferencesControllerIdKey;
    private String sharedPreferencesRetryDelayKey;
    private String sharedPreferencesGatewayToken;
    private String sharedPreferencesTargetToken;
    private String sharedPreferencesFile;

    private static final String SHARED_PREFERENCES_LAST_NOTIFY_MESSAGE = "LAST_NOTIFY_MESSAGE";

    private static final String SHARED_PREFERENCES_FILE_NAME_KEY = "FILE_NAME_KEY";

}