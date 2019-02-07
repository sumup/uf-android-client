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

package com.kynetics.uf.android.ui.fragment;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.widget.Toast;

import com.kynetics.uf.android.R;
import com.kynetics.uf.android.UpdateFactoryService;
import com.kynetics.uf.android.apicomptibility.ApiVersion;
import com.kynetics.uf.android.content.SharedPreferencesWithObject;
import com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState;

/**
 * A simple {@link PreferenceFragmentCompat} subclass.
 */
public class UFPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    public UFPreferenceFragment() {
    }


    public static UFPreferenceFragment newInstance(){
        return new UFPreferenceFragment();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(getString(R.string.shared_preferences_file));
        setPreferencesFromResource(R.xml.pref_general, rootKey);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        String[] editTextKey = {getString(R.string.shared_preferences_controller_id_key),
                getString(R.string.shared_preferences_tenant_key),
                getString(R.string.shared_preferences_server_url_key)};

        for(String key : editTextKey){
            EditTextPreference editTextPreference = (EditTextPreference) findPreference(key);
            editTextPreference.setOnPreferenceChangeListener(notEmptyEditTextListener);
        }
    }

    Preference.OnPreferenceChangeListener notEmptyEditTextListener = (preference, newValue) -> {
        if (newValue.toString().trim().equals("")) {
            Toast.makeText(getActivity(), "Filed can't be empty",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    };

    @Override
    public void onStart() {
        super.onStart();
        final Intent myIntent = new Intent(getContext(), UpdateFactoryService.class);
        ApiVersion.fromVersionCode().startService(getContext(), myIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        final SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();

        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
            Preference preference = getPreferenceScreen().getPreference(i);
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
                    Preference singlePref = preferenceGroup.getPreference(j);
                    updatePreference(singlePref, singlePref.getKey(), sharedPrefs);
                }
            } else {
                updatePreference(preference, preference.getKey(), sharedPrefs);
            }
        }
        enableDisableActivePreference(sharedPrefs);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(!isAdded()){
            return;
        }
        final SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();
        final Preference preference = findPreference(key);
        updatePreference(preference, key, sharedPrefs);
        enableDisableActivePreference(sharedPrefs);

//        final boolean isSwitchPreference = preference instanceof SwitchPreferenceCompat;
        if( key.equals(getString(R.string.shared_preferences_is_enable_key)) &&
                !((SwitchPreferenceCompat)preference).isChecked()){
            new AlertDialog.Builder(getContext())
                    .setTitle(getResources().getString(R.string.stop_service_dialog_title))
                    .setCancelable(false)
                    .setMessage(
                            getResources().getString(R.string.stop_service_dialog_message))
                    .setPositiveButton(
                            getResources().getString(android.R.string.ok),
                            (dialog, which) ->{} )
                    .setNegativeButton(
                            getResources().getString(android.R.string.cancel),
                            (dialogInterface, i) -> ((SwitchPreferenceCompat)preference).setChecked(true)).show();
        }
    }

    private void enableDisableActivePreference(SharedPreferences sharedPreferences){
        Preference activePreference = findPreference(getString(R.string.shared_preferences_is_enable_key));
        if(sharedPreferences.getString(getString(R.string.shared_preferences_server_url_key), "").isEmpty() ||
                sharedPreferences.getString(getString(R.string.shared_preferences_tenant_key), "").isEmpty() ||
                sharedPreferences.getString(getString(R.string.shared_preferences_controller_id_key), "").isEmpty()){
            activePreference.setEnabled(false);
        } else {
            activePreference.setEnabled(true);
        }
    }

    private void updatePreference(Preference preference, String key, SharedPreferences sharedPrefs) {
        if (preference == null || preference instanceof SwitchPreferenceCompat) {
            return;
        }

        if (preference instanceof ListPreference) {
            final ListPreference listPreference = (ListPreference) preference;
            listPreference.setSummary(listPreference.getEntry());
            return;
        }

        if (preference instanceof EditTextPreference){
            final EditTextPreference editTextPreference = (EditTextPreference) preference;
            editTextPreference.setSummary(editTextPreference.getText());
        }

        if(key.equals(getString(R.string.shared_preferences_current_state_key))){
            preference.setSummary(getStateName(key, sharedPrefs));
            return;
        }

        if(key.equals(getString(R.string.shared_preferences_retry_delay_key))){
            preference.setSummary(String.valueOf(sharedPrefs.getLong(key, 12_000)));
            return;
        }

    }

    private String getStateName(String key, SharedPreferences sharedPreferences){
        final SharedPreferencesWithObject sharedPrefs = new SharedPreferencesWithObject(sharedPreferences);
        AbstractState state = sharedPrefs.getObject(key, AbstractState.class);
        if(state != null){
            return state.getStateName().name();
        }
        return "";
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().edit().apply();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}