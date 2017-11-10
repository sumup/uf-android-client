/*
 * Copyright © 2017   LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.ufclientexample.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.kynetics.ufclientexample.R;
import com.kynetics.ufclientexample.activity.UFActivity;
import com.kynetics.ufclientserviceapi.UFServiceConfiguration;

import static com.kynetics.ufclientserviceapi.UFServiceCommunicationConstants.SERVICE_DATA_KEY;

/**
 * @author Daniele Sergio
 */
public class ConfigurationFragment extends Fragment implements UFServiceInteractionFragment {

    public static ConfigurationFragment newInstance() {
        return new ConfigurationFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mRootView = inflater.inflate(R.layout.fragment_configuration, container, false);
        mSpinner = mRootView.findViewById(R.id.retry_time_spinner);
        mSpinner.setSelection(1);

        final InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (Character.isWhitespace(source.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }

        };
        final InputFilter[] filters = {filter};
        final TextWatcher textWatcher = new TextWatcher() {
            private String withoutWhiteSpace(EditText editText){
                final String text = editText.getText().toString();
                return text.replaceAll("\\s+","");
            }
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                final String url = mUrlEditText.getText().toString();
                boolean isValidUrl = Patterns.WEB_URL.matcher(url).matches();
                if(url != null && !url.isEmpty() && !isValidUrl){
                    mUrlEditText.setError(getString(R.string.invalid_url));
                } else {
                    mUrlEditText.setError(null);
                }

                if(mTenantEditText.getText().length() > 0 &&
                        mControllerIdEditText.getText().length() > 0 &&
                        url.length() >0 && isValidUrl){
                    mButton.setEnabled(true);
                } else {
                    mButton.setEnabled(false);
                }
            }
        };
        mTenantEditText = mRootView.findViewById(R.id.tenant_edittextview);
        mTenantEditText.addTextChangedListener(textWatcher);
        mTenantEditText.setFilters(filters);
        mUrlEditText = mRootView.findViewById(R.id.url_edittextview);
        mUrlEditText.addTextChangedListener(textWatcher);
        mUrlEditText.setFilters(filters);
        mControllerIdEditText = mRootView.findViewById(R.id.controllerid_edittextview);
        mControllerIdEditText.addTextChangedListener(textWatcher);
        mControllerIdEditText.setFilters(filters);
        mButton = mRootView.findViewById(R.id.configure_service_button);
        mButton.setOnClickListener(view -> {
            final Bundle data = new Bundle();
            final long retryDelay = getResources().getIntArray(R.array.retry_time_entries_value)[mSpinner
                    .getSelectedItemPosition()];
            data.putSerializable(SERVICE_DATA_KEY, UFServiceConfiguration.builder()
                    .withControllerId(mControllerIdEditText.getText().toString())
                    .withTenant(mTenantEditText.getText().toString())
                    .withUrl(mUrlEditText.getText().toString())
                    .withRetryDelay(retryDelay)
                    .build());

            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            ((UFActivity)getActivity()).registerToService(data);
        });

        return mRootView;
    }


    @Override
    public void onMessageReceived(String message) {
        Snackbar.make(mRootView, message, Snackbar.LENGTH_LONG).show();
    }

    private View mRootView;
    private Spinner mSpinner;
    private Button mButton;
    private EditText mTenantEditText;
    private EditText mUrlEditText;
    private EditText mControllerIdEditText;
}
