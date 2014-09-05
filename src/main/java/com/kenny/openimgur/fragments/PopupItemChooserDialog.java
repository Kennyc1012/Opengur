package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.util.LogUtil;

/**
 * Created by kcampagna on 9/5/14.
 */
public class PopupItemChooserDialog extends DialogFragment {
    public interface ChooserListener {
        void onItemSelected(int position, String item);
    }

    private static final String KEY_ITEMS = "items";

    private ListView mList;

    private ArrayAdapter<String> mAdapter;

    private ChooserListener mListener;

    /**
     * Creates an instance
     *
     * @param items The items to display in the list
     * @return
     */
    public static PopupItemChooserDialog createInstance(String[] items) {
        if (items == null || items.length <= 0) {
            throw new IllegalArgumentException("Items can not be null or empty");
        }

        PopupItemChooserDialog fragment = new PopupItemChooserDialog();
        Bundle args = new Bundle();
        args.putStringArray(KEY_ITEMS, items);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ChooserListener) {
            mListener = (ChooserListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() == null)
            return;

        // Dialog Fragments are automatically set to wrap_content, so we need to force the width to fit our view
        int dialogWidth = (int) (getResources().getDisplayMetrics().widthPixels * .85);
        getDialog().getWindow().setLayout(dialogWidth, getDialog().getWindow().getAttributes().height);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return inflater.inflate(R.layout.item_chooser_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mList = (ListView) view.findViewById(R.id.list);
        String[] items = getArguments().getStringArray(KEY_ITEMS);
        mAdapter = new ArrayAdapter<String>(getActivity(), R.layout.chooser_item, items);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String item = mAdapter.getItem(position);

                if (mListener != null) {
                    mListener.onItemSelected(position, item);
                } else {
                    LogUtil.w("PopupItemChooserDialog", "Listener not set");
                }

                dismiss();
            }
        });
    }

    @Override
    public void onDestroyView() {
        mList = null;
        super.onDestroyView();
    }

    public void setChooserListener(ChooserListener listener) {
        mListener = listener;
    }
}
