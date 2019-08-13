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

package com.kynetics.uf.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import com.kynetics.uf.android.R;
import com.kynetics.uf.android.UpdateFactoryService;
import com.kynetics.uf.android.ui.fragment.AuthorizationDialogFragment;
import com.kynetics.uf.android.ui.fragment.AuthorizationDialogFragment.OnAuthorization;
import com.kynetics.uf.android.ui.fragment.UFPreferenceFragment;

/**
 * @author Daniele Sergio
 */
public class MainActivity extends AppCompatActivity implements OnAuthorization{

    public static final String INTENT_TYPE_EXTRA_VARIABLE = "EXTRA_TYPE";
    public static final int INTENT_TYPE_EXTRA_VALUE_SETTINGS = 0;
    public static final int INTENT_TYPE_EXTRA_VALUE_DOWNLOAD = 1;
    public static final int INTENT_TYPE_EXTRA_VALUE_REBOOT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.update_factory_label);
        final Intent intent =  getIntent();
        final int type = intent.getIntExtra(INTENT_TYPE_EXTRA_VARIABLE, 0);
        switch (type){
            case INTENT_TYPE_EXTRA_VALUE_SETTINGS:
                setTheme(R.style.AppTheme);
                setContentView(R.layout.activity_main);
                ActionBar actionBar = getSupportActionBar();
                actionBar.setDisplayHomeAsUpEnabled(true);
                changePage(UFPreferenceFragment.newInstance());
                break;
            case INTENT_TYPE_EXTRA_VALUE_DOWNLOAD:
            case INTENT_TYPE_EXTRA_VALUE_REBOOT:
                setTheme(R.style.AppTransparentTheme);
                showAuthorizationDialog(type);
                break;
            default:
                throw new IllegalArgumentException("");

        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            UpdateFactoryService.getUfServiceCommand().configureService();
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAuthorizationDialog(int type) {
        DialogFragment newFragment = AuthorizationDialogFragment.newInstance(
                getString(type == INTENT_TYPE_EXTRA_VALUE_DOWNLOAD ?
                        R.string.update_download_title :
                        R.string.update_started_title),
                getString(type == INTENT_TYPE_EXTRA_VALUE_DOWNLOAD ?
                        R.string.update_download_content :
                        R.string.update_started_content),
                getString(android.R.string.ok),
                getString(android.R.string.cancel));
        newFragment.show(getSupportFragmentManager(), "authorization");
    }

    private void changePage(Fragment fragment){
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.frame_layout, fragment);
        tx.commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onAuthorizationGrant() {
        UpdateFactoryService.getUfServiceCommand().authorizationGranted();
        finishActivity();
    }

    @Override
    public void onAuthorizationDenied() {
        UpdateFactoryService.getUfServiceCommand().authorizationDenied();
        finishActivity();
    }

    private void finishActivity(){
        new Handler().postDelayed(() -> finish(), 500);
    }
}
