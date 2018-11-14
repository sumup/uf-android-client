/*
 * Copyright © 2017-2018  Kynetics  LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.uf.android.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AuthorizationDialogFragment.OnAuthorization} interface
 * to handle interaction events.
 * Use the {@link AuthorizationDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * @author Daniele Sergio
 */
public class AuthorizationDialogFragment extends DialogFragment {

    public interface OnAuthorization {
        void onAuthorizationGrant();
        void onAuthorizationDenied();
    }

    public AuthorizationDialogFragment() {
        // Required empty public constructor
    }

    private static final String ARG_DESCRIPTION = "description_arg";
    private static final String ARG_TITLE = "title_arg";
    private static final String ARG_POSITIVE_BUTTON = "positive_button_arg";
    private static final String ARG_NEGATIVE_BUTTON = "negative_button_arg";

    private String mDescription, mTitle, mPositiveButton, mNegativeButton;


    public static AuthorizationDialogFragment newInstance(String title, String description, String positiveButton, String negativeButton) {
        AuthorizationDialogFragment fragment = new AuthorizationDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putString(ARG_POSITIVE_BUTTON, positiveButton);
        args.putString(ARG_NEGATIVE_BUTTON, negativeButton);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTitle = getArguments().getString(ARG_TITLE);
            mDescription = getArguments().getString(ARG_DESCRIPTION);
            mPositiveButton = getArguments().getString(ARG_POSITIVE_BUTTON);
            mNegativeButton = getArguments().getString(ARG_NEGATIVE_BUTTON);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(mTitle)
                .setMessage(mDescription)
                .setPositiveButton(mPositiveButton,
                        (dialog, whichButton) -> onAuthorizationGrant()
                )
                .setNegativeButton(mNegativeButton,
                        (dialog, whichButton) -> onAuthorizationDenied()
                )
                .create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  super.onCreateView(inflater, container, savedInstanceState);
        setCancelable(false);
        return view;
    }

    private void onAuthorizationGrant(){
        mListener.onAuthorizationGrant();
    }

    private void onAuthorizationDenied(){
        mListener.onAuthorizationDenied();
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnAuthorization) {
            mListener = (OnAuthorization) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private OnAuthorization mListener;

}
