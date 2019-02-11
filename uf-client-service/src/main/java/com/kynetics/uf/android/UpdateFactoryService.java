/*
 * Copyright © 2017-2018  Kynetics  LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.kynetics.uf.android.api.UFServiceConfiguration;
import com.kynetics.uf.android.api.UFServiceMessage;
import com.kynetics.uf.android.api.UFServiceMessage.Suspend;
import com.kynetics.uf.android.apicomptibility.ApiVersion;
import com.kynetics.uf.android.configuration.ConfigurationFileLoader;
import com.kynetics.uf.android.content.SharedPreferencesWithObject;
import com.kynetics.uf.android.ui.MainActivity;
import com.kynetics.updatefactory.ddiclient.api.ClientBuilder;
import com.kynetics.updatefactory.ddiclient.api.ServerType;
import com.kynetics.updatefactory.ddiclient.api.api.DdiRestApi;
import com.kynetics.updatefactory.ddiclient.core.UFService;
import com.kynetics.updatefactory.ddiclient.core.model.event.AbstractEvent;
import com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState;
import com.kynetics.updatefactory.ddiclient.core.model.state.SavingFileState;
import com.kynetics.updatefactory.ddiclient.core.model.state.UpdateStartedState;
import com.kynetics.updatefactory.ddiclient.core.model.state.WaitingState;
import com.kynetics.updatefactory.ddiclient.core.servicecallback.UserInteraction;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import okhttp3.OkHttpClient;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_AUTHORIZATION_REQUEST;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_AUTHORIZATION_RESPONSE;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_CONFIGURE_SERVICE;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_REGISTER_CLIENT;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_RESUME_SUSPEND_UPGRADE;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_SERVICE_STATUS;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_SERVICE_CONFIGURATION_STATUS;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_SYNC_REQUEST;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.MSG_UNREGISTER_CLIENT;
import static com.kynetics.uf.android.api.UFServiceCommunicationConstants.SERVICE_DATA_KEY;
import static com.kynetics.uf.android.api.UFServiceMessage.Suspend.DOWNLOAD;
import static com.kynetics.uf.android.api.UFServiceMessage.Suspend.NONE;
import static com.kynetics.uf.android.api.UFServiceMessage.Suspend.UPDATE;

/**
 * @author Daniele Sergio
 */
public class UpdateFactoryService extends Service implements UpdateFactoryServiceCommand {

    private static final String CHANNEL_ID = "UPDATE_FACTORY_NOTIFICATION_CHANNEL_ID";
    public static final int NOTIFICATION_ID = 1;
    public static final String ANDROID_BUILD_DATE_TARGET_ATTRIBUTE_KEY = "android_build_date";
    public static final String ANDROID_BUILD_TYPE_TARGET_ATTRIBUTE_KEY = "android_build_type";
    public static final String ANDROID_FINGERPRINT_TARGET_ATTRIBUTE_KEY = "android_fingerprint";
    public static final String ANDROID_KEYS_TARGET_ATTRIBUTE_KEY = "android_keys";
    public static final String ANDROID_VERSION_TARGET_ATTRIBUTE_KEY = "android_version";
    public static final String DEVICE_NAME_TARGET_ATTRIBUTE_KEY = "device_name";

    public static UpdateFactoryServiceCommand getUFServiceCommand(){
        return ufServiceCommand;
    }

    @Override
    public void authorizationGranted() {
        userInteraction.setAuthorization(Boolean.TRUE);
    }

    @Override
    public void authorizationDenied() {
        userInteraction.setAuthorization(Boolean.FALSE);
    }


    @Override
    public void configureService() {
        if(ufService!=null){
            ufService.stop();
        }
        buildServiceFromPreferences(true); // TODO: 11/14/17 fix workaround
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initSharedPreferencesKeys();
        ufServiceCommand = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "service's starting");
        startForeground();
        final ConfigurationFileLoader configurationFile =
                new ConfigurationFileLoader(super.getSharedPreferences(sharedPreferencesFile,MODE_PRIVATE), UF_CONF_FILE, getApplicationContext());
        UFServiceConfiguration serviceConfiguration = configurationFile.getNewFileConfiguration();
        if(serviceConfiguration == null && intent != null){
            Log.i(TAG, "Loaded new configuration from intent");
            final Serializable serializable = intent.getSerializableExtra(SERVICE_DATA_KEY);
            if(serializable instanceof UFServiceConfiguration){
                serviceConfiguration = (UFServiceConfiguration) serializable;
            }
        } else if (serviceConfiguration != null){
            Log.i(TAG, "Loaded new configuration from file");
        }
        saveServiceConfigurationToSharedPreferences(serviceConfiguration);
        buildServiceFromPreferences(serviceConfiguration!=null);
        return START_STICKY;
    }


    private OkHttpClient.Builder buildOkHttpClient() {
        return new OkHttpClient.Builder();
    }

    private AbstractState getInitialState(boolean startNewService,
                                          SharedPreferencesWithObject sharedPreferences) {
        final Long updatePendingId = UpdateSystem.getUpdatePendingId();
        if(updatePendingId != null){
            return new UpdateStartedState(updatePendingId);
        }
        return startNewService ? new WaitingState(0, null) : sharedPreferences.getObject(sharedPreferencesCurrentStateKey, AbstractState.class, new WaitingState(0, null));
    }

    private void buildServiceFromPreferences(boolean startNewService) {
        startStopService(false);
        final SharedPreferencesWithObject sharedPreferences = getSharedPreferences(sharedPreferencesFile, MODE_PRIVATE);
        final boolean serviceIsEnable = sharedPreferences.getBoolean(sharedPreferencesServiceEnableKey, false);
        if(serviceIsEnable) {
            final String url = sharedPreferences.getString(sharedPreferencesServerUrlKey, "");
            final String controllerId = sharedPreferences.getString(sharedPreferencesControllerIdKey, "");
            final String gatewayToken = sharedPreferences.getString(sharedPreferencesGatewayToken, "");
            final String targetToken = sharedPreferences.getString(sharedPreferencesTargetToken, "");
            final String tenant = sharedPreferences.getString(sharedPreferencesTenantKey, "");
            final long delay = sharedPreferences.getLong(sharedPreferencesRetryDelayKey, 30000);
            final AbstractState initialState = getInitialState(startNewService, sharedPreferences);
            final boolean apiMode = sharedPreferences.getBoolean(sharedPreferencesApiModeKey, false);
            final ServerType serverType = sharedPreferences.getObject(sharedPreferencesServerType, ServerType.class, ServerType.UPDATE_FACTORY);
            userInteraction = new AndroidUserInteraction() {
                @Override
                protected void onAuthorizationAsked(Authorization authorization) {
                    if(apiMode){
                        sendMessage(authorization.name(), MSG_AUTHORIZATION_REQUEST);
                    }else {
                        showAuthorizationDialog(authorization);
                    }
                }
            };
            try {
                final DdiRestApi client = new ClientBuilder()
                        .withBaseUrl(url)
                        .withGatewayToken(gatewayToken)
                        .withTargetToken(targetToken)
                        .withHttpBuilder(buildOkHttpClient())
                        .withServerType(serverType)
                        .build();
                final Map<String, String> finalTargetAttributes = decorateTargetAttribute(sharedPreferences);
                ufService = UFService.builder()
                        .withClient(client)
                        .withRetryDelayOnCommunicationError(delay)
                        .withTenant(tenant)
                        .withControllerId(controllerId)
                        .withInitialState(initialState)
                        .withSystemOperation(new AndroidSystemOperation(getApplicationContext(), initialState.getStateName() == AbstractState.StateName.UPDATE_STARTED))
                        .withTargetData(()->finalTargetAttributes)
                        .withUserInteraction(userInteraction)
                        .build();
                ufService.addObserver(new ObserverState());
                startStopService(true);
            }catch (IllegalStateException | IllegalArgumentException e){
                sharedPreferences.edit().putBoolean(sharedPreferencesServiceEnableKey, false).apply();
                Toast.makeText(this,"Update Factory configuration error",Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    @NonNull
    private Map<String, String> decorateTargetAttribute(SharedPreferencesWithObject sharedPreferences) {
        Map<String,String> targetAttributes = sharedPreferences.getObject(sharedPreferencesTargetAttributes, new HashMap<String,String>().getClass());
        if(targetAttributes == null){
            targetAttributes = new HashMap<>();
        }
        targetAttributes.put(CLIENT_VERSION_TARGET_ATTRIBUTE_KEY, BuildConfig.VERSION_NAME); // TODO: 4/17/18 refactor
        final Date buildDate = new Date(android.os.Build.TIME);
        final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.UK);
        targetAttributes.put(ANDROID_BUILD_DATE_TARGET_ATTRIBUTE_KEY,dateFormat.format(buildDate));
        targetAttributes.put(ANDROID_BUILD_TYPE_TARGET_ATTRIBUTE_KEY,android.os.Build.TYPE);
        targetAttributes.put(ANDROID_FINGERPRINT_TARGET_ATTRIBUTE_KEY,android.os.Build.FINGERPRINT);
        targetAttributes.put(ANDROID_KEYS_TARGET_ATTRIBUTE_KEY,(android.os.Build.TAGS));
        targetAttributes.put(ANDROID_VERSION_TARGET_ATTRIBUTE_KEY,(android.os.Build.VERSION.RELEASE));
        targetAttributes.put(DEVICE_NAME_TARGET_ATTRIBUTE_KEY,(android.os.Build.DEVICE));
        return targetAttributes;
    }

    private ArrayList<Messenger> mClients = new ArrayList<>();

    private UFServiceConfiguration getCurrentConfiguration(SharedPreferencesWithObject sharedPreferences){
        final boolean serviceIsEnable = ufService != null && sharedPreferences.getBoolean(sharedPreferencesServiceEnableKey, false);
        final String url = sharedPreferences.getString(sharedPreferencesServerUrlKey, "");
        final String controllerId = sharedPreferences.getString(sharedPreferencesControllerIdKey, "");
        final String gatewayToken = sharedPreferences.getString(sharedPreferencesGatewayToken, "");
        final String targetToken = sharedPreferences.getString(sharedPreferencesTargetToken, "");
        final String tenant = sharedPreferences.getString(sharedPreferencesTenantKey, "");
        final long delay = sharedPreferences.getLong(sharedPreferencesRetryDelayKey, 900_000);
        final boolean apiMode = sharedPreferences.getBoolean(sharedPreferencesApiModeKey, true);
        final Map<String,String> targetAttributes = sharedPreferences.getObject(sharedPreferencesTargetAttributes, new HashMap<String,String>().getClass());
        final ServerType serverType = sharedPreferences.getObject(sharedPreferencesServerType, ServerType.class, ServerType.UPDATE_FACTORY);
        return UFServiceConfiguration.builder()
                .withTargetAttributes(targetAttributes)
                .withEnable(serviceIsEnable)
                .withApiMode(apiMode)
                .withControllerId(controllerId)
                .withGetawayToken(gatewayToken)
                .withRetryDelay(delay)
                .withTargetToken(targetToken)
                .withTenant(tenant)
                .withIsUpdateFactoryServer(serverType == ServerType.UPDATE_FACTORY)
                .withUrl(url)
                .build();
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CONFIGURE_SERVICE:
                    Log.i(TAG, "receive configuration update request");
                    final UFServiceConfiguration configuration = (UFServiceConfiguration) msg.getData().getSerializable(SERVICE_DATA_KEY);
                    saveServiceConfigurationToSharedPreferences(configuration);
                    buildServiceFromPreferences(true);
                    Log.i(TAG, "configuration updated");
                    break;
                case MSG_REGISTER_CLIENT:
                    Log.i(TAG, "receive subscription request");
                    if(msg.replyTo != null){
                        mClients.add(msg.replyTo);
                        Log.i(TAG, "client subscription ignored. Field replyTo mustn't be null");
                    }
                    Log.i(TAG, "client subscription");
                    break;
                case MSG_UNREGISTER_CLIENT:
                    Log.i(TAG, "receive unsubscription request");
                    mClients.remove(msg.replyTo);
                    Log.i(TAG, "client unsubscription");
                    break;
                case MSG_AUTHORIZATION_RESPONSE:
                    Log.i(TAG, "receive authorization response");
                    final boolean response = msg.getData().getBoolean(SERVICE_DATA_KEY);
                    userInteraction.setAuthorization(response);
                    Log.i(TAG, String.format("authorization %s", response ? "granted" : "denied"));
                    break;
                case MSG_RESUME_SUSPEND_UPGRADE:
                    Log.i(TAG, "receive request to resume suspend state");
                    ufService.restartSuspendState();
                    Log.i(TAG, "resumed suspend state");
                    break;
                case MSG_SYNC_REQUEST:
                    Log.i(TAG, "received sync request");
                    final SharedPreferencesWithObject sharedPreferences = getSharedPreferences(sharedPreferencesFile, MODE_PRIVATE);
                    UpdateFactoryService.this.sendMessage(getCurrentConfiguration(sharedPreferences), MSG_SERVICE_CONFIGURATION_STATUS, msg.replyTo);
                    if(ufService == null){
                        return;
                    }
                    final UFServiceMessage lastMessage = sharedPreferences.getObject(SHARED_PREFERENCES_LAST_NOTIFY_MESSAGE, UFServiceMessage.class);
                    if(lastMessage != null){
                        UpdateFactoryService.this.sendMessage(lastMessage, MSG_SERVICE_STATUS, msg.replyTo);
                    }
                    AbstractState lastState = sharedPreferences.getObject(sharedPreferencesCurrentStateKey, AbstractState.class);
                    if(lastState.getStateName() == AbstractState.StateName.AUTHORIZATION_WAITING){
                        UpdateFactoryService.this.sendMessage(userInteraction.getAuthRequest().name(),
                                MSG_AUTHORIZATION_REQUEST,
                                msg.replyTo);
                    }
                    Log.i(TAG, "client synced");
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void saveServiceConfigurationToSharedPreferences(UFServiceConfiguration configuration) {
        if(configuration == null){
            return;
        }
        final SharedPreferencesWithObject sharedPreferences = getSharedPreferences(sharedPreferencesFile, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(sharedPreferencesControllerIdKey, configuration.getControllerId());
        editor.putString(sharedPreferencesTenantKey, configuration.getTenant());
        editor.putString(sharedPreferencesServerUrlKey, configuration.getUrl());
        editor.putString(sharedPreferencesGatewayToken, configuration.getGatewayToken());
        editor.putString(sharedPreferencesTargetToken, configuration.getTargetToken());
        editor.putLong(sharedPreferencesRetryDelayKey, configuration.getRetryDelay());
        editor.putBoolean(sharedPreferencesApiModeKey, configuration.isApiMode());
        editor.putBoolean(sharedPreferencesServiceEnableKey, configuration.isEnable());
        editor.apply();
        sharedPreferences.putAndCommitObject(sharedPreferencesTargetAttributes,configuration.getTargetAttributes());
        sharedPreferences.putAndCommitObject(sharedPreferencesServerType,
                configuration.isUpdateFactoryServe() ? ServerType.UPDATE_FACTORY : ServerType.HAWKBIT);
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
        if(messenger == null){
            Log.i(TAG, "Response isn't' sent because there isn't a receiver (replyTo is null)");
            return;
        }
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

        ObserverState() {
        }

        @Override
        public void update(Observable observable, Object o) {
            if(o instanceof UFService.SharedEvent) {
                final UFService.SharedEvent eventNotify = (UFService.SharedEvent) o;
                final AbstractEvent event = eventNotify.getEvent();
                final AbstractState newState = eventNotify.getNewState();
                final String newStateString = newState.getStateName() == AbstractState.StateName.SAVING_FILE ?
                        String.format("%s (%s%%)", newState.getStateName().name(), (int) Math.floor(((SavingFileState)newState).getPercent() * 100))
                        :  newState.getStateName().name();
                final UFServiceMessage message = new UFServiceMessage(
                        event.getEventName().name(),
                        eventNotify.getOldState().getStateName().name(),
                        newStateString,
                        getSuspend(newState)
                );

                final String messageString = String.format( "Update state changed at %s: (%s,%s) -> %s",
                        message.getDateTime(),
                        message.getOldState(),
                        message.getEventName(),
                        message.getCurrentState());
                Log.i(TAG, messageString);
                writeObjectToSharedPreference(message, SHARED_PREFERENCES_LAST_NOTIFY_MESSAGE);
                sendMessage(message, MSG_SERVICE_STATUS);
                final String notificationString = String.format( "(%s,%s) -> %s",
                        message.getOldState(),
                        message.getEventName(),
                        message.getCurrentState());

                if(newState.getStateName() != AbstractState.StateName.SAVING_FILE) {
                    writeObjectToSharedPreference(eventNotify.getNewState(), sharedPreferencesCurrentStateKey);
                }

                mNotificationManager.notify(NOTIFICATION_ID,getNotification(notificationString));
            }
        }
    }

    private void showAuthorizationDialog(UserInteraction.Authorization auth) {
        final Intent intent = new Intent(UpdateFactoryService.this, MainActivity.class);
        intent.putExtra(MainActivity.INTENT_TYPE_EXTRA_VARIABLE, auth == UserInteraction.Authorization.DOWNLOAD ?
                MainActivity.INTENT_TYPE_EXTRA_VALUE_DOWNLOAD : MainActivity.INTENT_TYPE_EXTRA_VALUE_REBOOT);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private Suspend getSuspend(AbstractState state){
        if(state.getStateName() != AbstractState.StateName.WAITING){
            return NONE;
        }
        WaitingState waitingState = (WaitingState) state;
        if(!waitingState.hasInnerState()){
            return NONE;
        }
        return waitingState.getState().getStateName() == AbstractState.StateName.UPDATE_DOWNLOAD ?
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

    private void startForeground(){
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        ApiVersion.fromVersionCode().configureChannel(CHANNEL_ID, getString(R.string.app_name), mNotificationManager);
        startForeground(NOTIFICATION_ID, getNotification(""));
    }

    private Notification getNotification(String notificationContent) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                 .setSmallIcon(R.drawable.uf_logo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationContent))
                .setContentTitle(getString(R.string.update_factory_running))
                .setContentText(notificationContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
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
        sharedPreferencesGatewayToken = getString(R.string.shared_preferences_gateway_token_key);
        sharedPreferencesTargetToken = getString(R.string.shared_preferences_target_token_key);
        sharedPreferencesTargetAttributes = getString(R.string.shared_preferences_args_key);
        sharedPreferencesServerType = getString(R.string.shared_preferences_server_type_key);
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
    private String sharedPreferencesServerType;
    private String sharedPreferencesTargetAttributes;
    private AndroidUserInteraction userInteraction;
    private NotificationManager mNotificationManager;

    private static final String CLIENT_VERSION_TARGET_ATTRIBUTE_KEY = "client_version";
    private static final String SHARED_PREFERENCES_LAST_NOTIFY_MESSAGE = "LAST_NOTIFY_MESSAGE";
    private static final String EXTERNAL_STORAGE_DIR = Environment.getExternalStorageDirectory().getPath();
    private static final String UF_CONF_FILE = EXTERNAL_STORAGE_DIR + "/UpdateFactoryConfiguration/ufConf.conf" ;
    private static final String TAG = UpdateFactoryService.class.getSimpleName();

}