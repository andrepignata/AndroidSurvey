package org.adaptlab.chpir.android.survey.QuestionFragments;

import org.adaptlab.chpir.android.survey.QuestionFragment;

import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SliderQuestionFragment extends QuestionFragment {
    private int mProgress;
    private SeekBar mSlider;

    @Override
    protected void createQuestionComponent(ViewGroup questionComponent) {
        mSlider = new SeekBar(getActivity());
        mSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mProgress = progress;
                saveResponse();
            }

            // Required by interface
            public void onStartTrackingTouch(SeekBar seekBar) { }
            public void onStopTrackingTouch(SeekBar seekBar) { }     
        });
        questionComponent.addView(mSlider);
    }

    @Override
    protected String serialize() {
        return String.valueOf(mProgress);
    }

    @Override
    protected void deserialize(String responseText) {
        if (!responseText.equals(""))
            mSlider.setProgress(Integer.parseInt(responseText));
    }
   
}
