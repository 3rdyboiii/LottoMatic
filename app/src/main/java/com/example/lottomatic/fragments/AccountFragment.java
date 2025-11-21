package com.example.lottomatic.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.lottomatic.R;
import com.example.lottomatic.helper.Account;

public class AccountFragment extends Fragment {

    TextView name;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_account, container, false);


        name = v.findViewById(R.id.nameTxt);

        name.setText(Account.getInstance(getActivity()).getName());

        return v;
    }
}