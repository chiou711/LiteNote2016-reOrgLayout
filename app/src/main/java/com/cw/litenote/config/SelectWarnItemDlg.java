package com.cw.litenote.config;


import com.cw.litenote.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckedTextView;

public class SelectWarnItemDlg 
{
	SharedPreferences mPref_delete_warn;
	private CheckedTextView chkDelWarnMain;
	private CheckedTextView chkDelNoteWarn;
	private CheckedTextView chkDelPageWarn;	
	private CheckedTextView chkDelDrawerWarn;	
	private CheckedTextView chkDelCheckedWarn;
	private LayoutInflater mInflator;
	private AlertDialog dialog;

	Context mContext;
	View view;
	SelectWarnItemDlg(){}
	
	SelectWarnItemDlg(View v, Activity activity)
	{
		mContext = activity;
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		
		builder.setTitle(R.string.config_delete_warning)
			   .setPositiveButton(R.string.btn_OK, listener_ok);
		
		// inflate select style layout
		mInflator= (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		v = mInflator.inflate(R.layout.select_warning, null);
		view = v;
		
		chkDelWarnMain = (CheckedTextView) view.findViewById(R.id.chkDeleteWarnMain);
		chkDelNoteWarn = (CheckedTextView) view.findViewById(R.id.chkDeleteNoteWarn);
		chkDelPageWarn = (CheckedTextView) view.findViewById(R.id.chkDeletePageWarn);
		chkDelDrawerWarn = (CheckedTextView) view.findViewById(R.id.chkDeleteDrawerWarn);
		chkDelCheckedWarn = (CheckedTextView) view.findViewById(R.id.chkDeleteCheckedWarn);
		builder.setView(view);
		dialog = builder.create();
		dialog.show();
	}
	
    DialogInterface.OnClickListener listener_ok = new DialogInterface.OnClickListener()
    {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			//end
			dialog.dismiss();
		}
    };
	
    void SelectWarnPref()
    {
    	mPref_delete_warn = mContext.getSharedPreferences("delete_warn", 0);
	    // warning setting: for main
		// show current
		if(mPref_delete_warn.getString("KEY_DELETE_WARN_MAIN","enable").equalsIgnoreCase("enable"))
		{
			chkDelWarnMain.setChecked(true);
			allCheckable(true);
		}
		else
		{
			chkDelWarnMain.setChecked(false);
			allCheckable(false);
		}
		// set new
		chkDelWarnMain.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View viewCheckBoxMain) 
			{
				boolean currentCheck = ((CheckedTextView)viewCheckBoxMain).isChecked();
				((CheckedTextView)viewCheckBoxMain).setChecked(!currentCheck);
				
				if(((CheckedTextView)viewCheckBoxMain).isChecked())
					allCheckable(true);
				else
					allCheckable(false);
			}
		});
		
		// Delete warning setting: for note
		showCurrentChecked("KEY_DELETE_NOTE_WARN",chkDelNoteWarn);
		chkDelNoteWarn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setNewChecked("KEY_DELETE_NOTE_WARN",chkDelNoteWarn);
			}
		});
		
		// Delete warning setting: for page
		showCurrentChecked("KEY_DELETE_PAGE_WARN",chkDelPageWarn);
		chkDelPageWarn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				setNewChecked("KEY_DELETE_PAGE_WARN",chkDelPageWarn);
			}
		});

		// Delete warning setting: for drawer
		showCurrentChecked("KEY_DELETE_DRAWER_WARN",chkDelDrawerWarn);
		chkDelDrawerWarn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				setNewChecked("KEY_DELETE_DRAWER_WARN",chkDelDrawerWarn);
			}
		});		
		
		// Delete warning setting: for checked notes
		showCurrentChecked("KEY_DELETE_CHECKED_WARN",chkDelCheckedWarn);
		chkDelCheckedWarn.setOnClickListener(new View.OnClickListener() { 
		   @Override
		   public void onClick(View v){
			   setNewChecked("KEY_DELETE_CHECKED_WARN",chkDelCheckedWarn);
		   }
		});
	    }
		void allCheckable(boolean yes)
		{
			if(yes)
			{
				mPref_delete_warn.edit().putString("KEY_DELETE_WARN_MAIN", "enable").commit();
				// all checkable
				setCheckable(chkDelNoteWarn);
				setCheckable(chkDelPageWarn);
				setCheckable(chkDelDrawerWarn);
				setCheckable(chkDelCheckedWarn);
			}
			else
			{                    
				mPref_delete_warn.edit().putString("KEY_DELETE_WARN_MAIN", "disable").commit();
				// all uncheckable
				setUncheckable(chkDelNoteWarn);
				setUncheckable(chkDelPageWarn);
				setUncheckable(chkDelDrawerWarn);
				setUncheckable(chkDelCheckedWarn);
			}
	}
	
	// show current
	void showCurrentChecked(String key,CheckedTextView chkTV)
	{
		if(mPref_delete_warn.getString(key,"yes").equalsIgnoreCase("yes")){
			chkTV.setChecked(true);
			mPref_delete_warn.edit().putString(key, "yes").commit();
		}else{
			chkTV.setChecked(false);
			mPref_delete_warn.edit().putString(key, "no").commit();
		}
	}
	
	// set new
	void setNewChecked(String key,CheckedTextView chkTV)
	{
	   chkTV.setChecked(!chkTV.isChecked());
	   if(chkTV.isChecked())
		   mPref_delete_warn.edit().putString(key, "yes").commit();
	   else
		   mPref_delete_warn.edit().putString(key, "no").commit();
	}
	
	// set checkable
	void setCheckable(CheckedTextView chkItem)
	{
		chkItem.setEnabled(true);
		chkItem.setClickable(true);
		chkItem.setTextColor(Color.rgb(255,255,255));
	}

	// set uncheckable
	void setUncheckable(CheckedTextView chkItem)
	{
		chkItem.setEnabled(false);
		chkItem.setClickable(false);
		chkItem.setTextColor(Color.rgb(100,100,100));
	}
}