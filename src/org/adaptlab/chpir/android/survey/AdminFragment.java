package org.adaptlab.chpir.android.survey;

import org.adaptlab.chpir.android.activerecordcloudsync.ActiveRecordCloudSync;
import org.adaptlab.chpir.android.activerecordcloudsync.PollService;
import org.adaptlab.chpir.android.survey.Models.AdminSettings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class AdminFragment extends Fragment {

    private EditText mDeviceIdentifierEditText;
    private EditText mSyncIntervalEditText;
    private EditText mApiEndPointEditText;
    private EditText mCustomLocaleEditText;
    private TextView mLastUpdateTextView;
    private TextView mBackendApiKeyTextView;
    private CheckBox mShowSurveysCheckBox;
    private CheckBox mShowSkipCheckBox;
    private CheckBox mShowNACheckBox;
    private CheckBox mShowRFCheckBox;
    private CheckBox mShowDKCheckBox;
    private TextView mVersionCodeTextView;
    private Button mSaveButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_admin_settings, parent,
                false);
        mDeviceIdentifierEditText = (EditText) v
                .findViewById(R.id.device_identifier_edit_text);
        mDeviceIdentifierEditText.setText(getAdminSettingsInstanceDeviceId());

        mSyncIntervalEditText = (EditText) v
                .findViewById(R.id.sync_interval_edit_text);
        mSyncIntervalEditText.setText(getAdminSettingsInstanceSyncInterval());
        
        mApiEndPointEditText = (EditText) v.findViewById(R.id.api_endpoint_edit_text);
        mApiEndPointEditText.setText(getAdminSettingsInstanceApiUrl());
        
        mCustomLocaleEditText = (EditText) v.findViewById(R.id.custom_locale_edit_text);
        mCustomLocaleEditText.setText(getAdminSettingsInstanceCustomLocaleCode());
        
        mShowSurveysCheckBox = (CheckBox) v.findViewById(R.id.show_surveys_checkbox);
        mShowSurveysCheckBox.setChecked(AdminSettings.getInstance().getShowSurveys());

        mShowSkipCheckBox = (CheckBox) v.findViewById(R.id.show_skip_checkbox);
        mShowSkipCheckBox.setChecked(AdminSettings.getInstance().getShowSkip());
        
        mShowNACheckBox = (CheckBox) v.findViewById(R.id.show_na_checkbox);
        mShowNACheckBox.setChecked(AdminSettings.getInstance().getShowNA());
        
        mShowRFCheckBox = (CheckBox) v.findViewById(R.id.show_rf_checkbox);
        mShowRFCheckBox.setChecked(AdminSettings.getInstance().getShowRF());
        
        mShowDKCheckBox = (CheckBox) v.findViewById(R.id.show_dk_checkbox);
        mShowDKCheckBox.setChecked(AdminSettings.getInstance().getShowDK());
        
        mLastUpdateTextView = (TextView) v.findViewById(R.id.last_update_label);
        mLastUpdateTextView.setText(mLastUpdateTextView.getText().toString() + getLastUpdateTime());
        
        mBackendApiKeyTextView = (TextView) v.findViewById(R.id.backend_api_key_label);
        mBackendApiKeyTextView.setText(getString(R.string.api_key_label) + getString(R.string.backend_api_key));
        
        mVersionCodeTextView = (TextView) v.findViewById(R.id.version_code_label);
        mVersionCodeTextView.setText(getString(R.string.version_code) + AppUtil.getVersionCode(getActivity()));
        
        mSaveButton = (Button) v.findViewById(R.id.save_admin_settings_button);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AdminSettings.getInstance().setDeviceIdentifier(mDeviceIdentifierEditText
                        .getText().toString());
                
                AdminSettings.getInstance().setSyncInterval(Integer
                        .parseInt(mSyncIntervalEditText.getText().toString()));
                
                AdminSettings.getInstance().setApiUrl(mApiEndPointEditText.getText().toString());
                
                // If this code is set, it will override the language selection on the device
                // for all instrument translations.
                AdminSettings.getInstance().setCustomLocaleCode(mCustomLocaleEditText.getText().toString());
                
                PollService.setPollInterval(AdminSettings.getInstance().getSyncInterval());
                
                // Restart the polling immediately with new interval.
                // This immediately hits the server again upon save.
                PollService.restartServiceAlarm(getActivity().getApplicationContext());
                
                ActiveRecordCloudSync.setEndPoint(getAdminSettingsInstanceApiUrl());
                
                AdminSettings.getInstance().setShowSurveys(mShowSurveysCheckBox.isChecked());
                AdminSettings.getInstance().setShowSkip(mShowSkipCheckBox.isChecked());
                AdminSettings.getInstance().setShowNA(mShowNACheckBox.isChecked());
                AdminSettings.getInstance().setShowRF(mShowRFCheckBox.isChecked());
                AdminSettings.getInstance().setShowDK(mShowDKCheckBox.isChecked());
                
                getActivity().finish();
            }
        });

        return v;
    }

    //TODO For testing convenience
    
	public String getLastUpdateTime() {
		return (PollService.getLastUpdate()) + "";
	}

	public String getAdminSettingsInstanceCustomLocaleCode() {
		return AdminSettings.getInstance().getCustomLocaleCode();
	}

	public String getAdminSettingsInstanceApiUrl() {
		return AdminSettings.getInstance().getApiUrl();
	}

	public String getAdminSettingsInstanceSyncInterval() {
		return String.valueOf(AdminSettings.getInstance().getSyncIntervalInMinutes());
	}

	public String getAdminSettingsInstanceDeviceId() {
		return AdminSettings.getInstance().getDeviceIdentifier();
	}
}
