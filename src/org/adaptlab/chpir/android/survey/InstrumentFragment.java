package org.adaptlab.chpir.android.survey;

import java.text.SimpleDateFormat;
import java.util.List;

import org.adaptlab.chpir.android.activerecordcloudsync.ActiveRecordCloudSync;
import org.adaptlab.chpir.android.activerecordcloudsync.NetworkNotificationUtils;
import org.adaptlab.chpir.android.survey.Models.AdminSettings;
import org.adaptlab.chpir.android.survey.Models.Instrument;
import org.adaptlab.chpir.android.survey.Models.Survey;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class InstrumentFragment extends ListFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setListAdapter(new InstrumentAdapter(Instrument.getAll()));       
        AppUtil.appInit(getActivity());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_instrument, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_admin:
            displayPasswordPrompt();
            return true;
        case R.id.menu_item_refresh:
            new RefreshInstrumentsTask().execute();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
        createTabs();
    }
    
    public void createTabs() {
        if (AdminSettings.getInstance().getShowSurveys()) {
            final ActionBar actionBar = getActivity().getActionBar();     
            ActionBar.TabListener tabListener = new ActionBar.TabListener() {    
                @Override
                public void onTabSelected(Tab tab,
                        android.app.FragmentTransaction ft) {
                    if (tab.getText().equals(getActivity().getResources().getString(R.string.surveys))) {
                        if (Survey.getAll().isEmpty())
                            setListAdapter(null);
                        else
                            setListAdapter(new SurveyAdapter(Survey.getAll()));
                    } else {
                        setListAdapter(new InstrumentAdapter(Instrument.getAll()));
                    }
                }
    
                // Required by interface
                public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) { }
                public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) { }
            };
            
            actionBar.removeAllTabs();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.addTab(actionBar.newTab().setText(getActivity().getResources().getString(R.string.instruments)).setTabListener(tabListener));
            actionBar.addTab(actionBar.newTab().setText(getActivity().getResources().getString(R.string.surveys)).setTabListener(tabListener));
        }
    }

    private class InstrumentAdapter extends ArrayAdapter<Instrument> {
        public InstrumentAdapter(List<Instrument> instruments) {
            super(getActivity(), 0, instruments);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(
                        R.layout.list_item_instrument, null);
            }

            Instrument instrument = getItem(position);

            TextView titleTextView = (TextView) convertView
                    .findViewById(R.id.instrument_list_item_titleTextView);
            titleTextView.setText(instrument.getTitle());
            titleTextView.setTypeface(instrument.getTypeFace(getActivity().getApplicationContext()));

            TextView questionCountTextView = (TextView) convertView
                    .findViewById(R.id.instrument_list_item_questionCountTextView);
            
            int numQuestions = instrument.questions().size();
            questionCountTextView.setText(numQuestions + " "
                    + FormatUtils.pluralize(numQuestions, getString(R.string.question), getString(R.string.questions)));

            return convertView;
        }
    }
    
    private class SurveyAdapter extends ArrayAdapter<Survey> {
        public SurveyAdapter(List<Survey> surveys) {
            super(getActivity(), 0, surveys);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(
                        R.layout.list_item_survey, null);
            }

            Survey survey = getItem(position);

            TextView titleTextView = (TextView) convertView
                    .findViewById(R.id.survey_list_item_titleTextView);
            titleTextView.setText(survey.identifier(getActivity()));
            titleTextView.setTypeface(survey.getInstrument().getTypeFace(getActivity().getApplicationContext()));

            TextView progressTextView = (TextView) convertView.findViewById(R.id.survey_list_item_progressTextView);            
            progressTextView.setText(survey.responses().size() + " " + getString(R.string.of) + " " + survey.getInstrument().questions().size());

            TextView instrumentTitleTextView = (TextView) convertView.findViewById(R.id.survey_list_item_instrumentTextView);
            instrumentTitleTextView.setText(survey.getInstrument().getTitle());
            
            TextView lastUpdatedTextView = (TextView) convertView.findViewById(R.id.survey_list_item_lastUpdatedTextView);
            SimpleDateFormat df = new SimpleDateFormat("HH:mm yyyy-MM-dd");
            lastUpdatedTextView.setText(df.format(survey.getLastUpdated()));
            
            return convertView;
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (l.getAdapter() instanceof InstrumentAdapter) {
            Instrument instrument = ((InstrumentAdapter) getListAdapter()).getItem(position);
            if (instrument == null) return;            
            new LoadInstrumentTask().execute(instrument);
        } else if (l.getAdapter() instanceof SurveyAdapter) {
            Survey survey = ((SurveyAdapter) getListAdapter()).getItem(position);
            if (survey == null) return;
            new LoadSurveyTask().execute(survey);            
        }
    }
   
    /*
     * Only display admin area if correct password.
     */
    private void displayPasswordPrompt() {
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.password_title)
            .setMessage(R.string.password_message)
            .setView(input)
            .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() { 
                public void onClick(DialogInterface dialog, int button) {
                    if (AppUtil.checkAdminPassword(input.getText().toString())) {
                        Intent i = new Intent(getActivity(), AdminActivity.class);
                        startActivity(i);
                    } else {
                        Toast.makeText(getActivity(), R.string.incorrect_password, Toast.LENGTH_LONG).show();
                    }
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int button) { }
            }).show();
    }
    
    /*
     * Refresh the receive tables from the server
     */
    private class RefreshInstrumentsTask extends AsyncTask<Void, Void, Void> {
        
        @Override
        protected void onPreExecute() {
            getActivity().setProgressBarIndeterminateVisibility(true);
            setListAdapter(null);            
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (isAdded() && NetworkNotificationUtils.checkForNetworkErrors(getActivity()))
                ActiveRecordCloudSync.syncReceiveTables(getActivity());
            return null;
        }
        
        @Override
        protected void onPostExecute(Void param) {
            if (isAdded()) {
                setListAdapter(new InstrumentAdapter(Instrument.getAll()));
                getActivity().setProgressBarIndeterminateVisibility(false);    
            }
        }        
    }
    
    /*
     * Check that the instrument has been fully loaded from the server before allowing
     * user to begin survey.
     */
    private class LoadInstrumentTask extends AsyncTask<Instrument, Void, Long> {
        ProgressDialog mProgressDialog;
        
        @Override
        protected void onPreExecute() {
            mProgressDialog = ProgressDialog.show(
                    getActivity(),
                    getString(R.string.instrument_loading_progress_header),
                    getString(R.string.instrument_loading_progress_message)
            ); 
        }
        
        /*
         * If instrument is loaded, return the instrument id.
         * If not, return -1.
         */
        @Override
        protected Long doInBackground(Instrument... params) {
            Instrument instrument = params[0];
            if (instrument.loaded()) {
                return instrument.getRemoteId();
            } else {
                return Long.valueOf(-1);
            }
        }
        
        @Override
        protected void onPostExecute(Long instrumentId) {
            mProgressDialog.dismiss();
            if (instrumentId == Long.valueOf(-1)) {
                Toast.makeText(getActivity(), R.string.instrument_not_loaded, Toast.LENGTH_LONG).show();
            } else {
                Intent i = new Intent(getActivity(), SurveyActivity.class);
                i.putExtra(SurveyFragment.EXTRA_INSTRUMENT_ID, instrumentId);
                startActivity(i);
            }
        }
    }
    
    private class LoadSurveyTask extends AsyncTask<Survey, Void, Survey> {
        ProgressDialog mProgressDialog;
        
        @Override
        protected void onPreExecute() {
            mProgressDialog = ProgressDialog.show(
                    getActivity(),
                    getString(R.string.instrument_loading_progress_header),
                    getString(R.string.instrument_loading_progress_message)
            ); 
        }
        
        /*
         * If instrument is loaded, return the survey.
         * If not, return null.
         */
        @Override
        protected Survey doInBackground(Survey... params) {
            Survey survey = params[0];
            Instrument instrument = survey.getInstrument();
            if (instrument.loaded()) {
                return survey;
            } else {
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(Survey survey) {
            mProgressDialog.dismiss();
            if (survey == null) {
                Toast.makeText(getActivity(), R.string.instrument_not_loaded, Toast.LENGTH_LONG).show();
            } else {
                Intent i = new Intent(getActivity(), SurveyActivity.class);
                i.putExtra(SurveyFragment.EXTRA_INSTRUMENT_ID, survey.getInstrument().getRemoteId());
                i.putExtra(SurveyFragment.EXTRA_SURVEY_ID, survey.getId());
                i.putExtra(SurveyFragment.EXTRA_QUESTION_ID, survey.getLastQuestion().getId());
                startActivity(i);
            }
        }
    }
}
