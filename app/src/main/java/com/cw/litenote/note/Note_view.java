package com.cw.litenote.note;

import java.util.ArrayList;
import java.util.List;

import com.cw.litenote.DrawerActivity;
import com.cw.litenote.NoteFragment;
import com.cw.litenote.R;
import com.cw.litenote.TabsHostFragment;
import com.cw.litenote.util.DeleteFileAlarmReceiver;
import com.cw.litenote.db.DB;
import com.cw.litenote.media.audio.AudioPlayer;
import com.cw.litenote.media.audio.UtilAudio;
import com.cw.litenote.media.image.TouchImageView;
import com.cw.litenote.media.video.AsyncTaskVideoBitmapPager;
import com.cw.litenote.media.video.UtilVideo;
import com.cw.litenote.media.video.VideoPlayer;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.MailNotes;
import com.cw.litenote.util.UilCommon;
import com.cw.litenote.util.Util;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import android.R.color;
import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class Note_view extends FragmentActivity
{
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    public static ViewPager mPager;
    public static boolean isPagerActive;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    public static PagerAdapter mPagerAdapter;

    // DB
    public static DB mDb;
    public static Long mRowId;
    int mEntryPosition;
    public static int mCurrentPosition;
    int EDIT_CURRENT_VIEW = 5;
    int MAIL_CURRENT_VIEW = 6;
    static int mStyle;
    
    static SharedPreferences mPref_show_note_attribute;

	static LinearLayout buttonGroup;
    static Button editButton;
    static Button sendButton;
    static Button backButton;
    
    public static String mAudioUriInDB;
    public static TextView mPager_audio_title;
    static TextView mPager_audio_curr_play_position;
    static TextView mPager_audio_file_length;
    static ViewGroup mAudioBlock;
    public static FragmentActivity mAct;
    public static int mPlayVideoPositionOfInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        System.out.println("Note_view / onCreate");

		// set current selection
		mEntryPosition = getIntent().getExtras().getInt("POSITION");
		mCurrentPosition = mEntryPosition;

		// init video
		UtilVideo.mPlayVideoPosition = 0;   // not played yet
		mPlayVideoPositionOfInstance = 0;
		AsyncTaskVideoBitmapPager.mRotationStr = null;
    } //onCreate end

	void setLayoutView()
	{
        System.out.println("Note_view / _setLayoutView");

		if( UtilVideo.mVideoView != null)
			UtilVideo.mPlayVideoPosition = UtilVideo.mVideoView.getCurrentPosition();

		// video view will be reset after _setContentView
		if(Util.isLandscapeOrientation(this))
			setContentView(R.layout.note_view_landscape);
		else
			setContentView(R.layout.note_view_portrait);

//		if( UtilVideo.mVideoView != null)
//		{
//			// keep play video position
//			mPlayVideoPositionOfInstance = UtilVideo.mPlayVideoPosition;
//		}

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.view_note_title);
		actionBar.setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(this)));

		mAct = this;
		mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);

		UilCommon.init();

		// DB
		mDb = NoteFragment.mDb_notes;

		// Instantiate a ViewPager and a PagerAdapter.
		mPager = (ViewPager) findViewById(R.id.pager);
		mPagerAdapter = new Note_view_pager_adapter(getFragmentManager(),this);
		mPager.setAdapter(mPagerAdapter);
		mPager.setCurrentItem(mCurrentPosition);

		// tab style
		if(TabsHostFragment.mDbTabs != null)
			TabsHostFragment.mDbTabs.close();

		TabsHostFragment.mDbTabs = new DB(mAct,Util.getPref_lastTimeView_tabs_tableId(mAct));
		TabsHostFragment.mDbTabs.initTabsDb(TabsHostFragment.mDbTabs);

		mStyle = TabsHostFragment.mDbTabs.getTabStyle(TabsHostFragment.mCurrent_tabIndex, true);

		if(mDb != null) {
			mRowId = mDb.getNoteId(mCurrentPosition, true);
			mAudioUriInDB = mDb.getNoteAudioUriById(mRowId);
		}

		// audio block
		TextView tag = (TextView) findViewById(R.id.text_view_audio);
		tag.setTextColor(ColorSet.color_white);

		mPager_audio_title = (TextView) findViewById(R.id.pager_audio_title);
		mPager_audio_title.setTextColor(ColorSet.color_white);
		if (Util.isLandscapeOrientation(mAct))
		{
			mPager_audio_title.setMovementMethod(new ScrollingMovementMethod());
			mPager_audio_title.scrollTo(0,0);
		}

		mPager_audio_curr_play_position = (TextView) mAct.findViewById(R.id.pager_audio_current_pos);

		mPager_audio_file_length = (TextView) findViewById(R.id.pager_audio_file_length);

		mAudioBlock = (ViewGroup) findViewById(R.id.audioGroup);
		mAudioBlock.setBackgroundColor(ColorSet.color_black);

		mPager_audio_play_button = (ImageView) mAct.findViewById(R.id.pager_btn_audio_play);
		seekBarProgress = (SeekBar)mAct.findViewById(R.id.pager_img_audio_seek_bar);

		// Note: if mPager.getCurrentItem() is not equal to mEntryPosition, _onPageSelected will
		//       be called again after rotation
		mPager.setOnPageChangeListener(onPageChangeListener);

		buttonGroup = (LinearLayout) findViewById(R.id.view_button_group);
		// edit note button
		editButton = (Button) findViewById(R.id.view_edit);
		editButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_edit, 0, 0, 0);
		editButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				Intent intent = new Intent(Note_view.this, Note_edit.class);
				intent.putExtra(DB.KEY_NOTE_ID, mRowId);
				intent.putExtra(DB.KEY_NOTE_TITLE, mDb.getNoteTitleById(mRowId));
				intent.putExtra(DB.KEY_NOTE_AUDIO_URI , mDb.getNoteAudioUriById(mRowId));
				intent.putExtra(DB.KEY_NOTE_PICTURE_URI , mDb.getNotePictureUriById(mRowId));
				intent.putExtra(DB.KEY_NOTE_LINK_URI , mDb.getNoteLinkUriById(mRowId));
				intent.putExtra(DB.KEY_NOTE_BODY, mDb.getNoteBodyById(mRowId));
				intent.putExtra(DB.KEY_NOTE_CREATED, mDb.getNoteCreatedTimeById(mRowId));
				startActivityForResult(intent, EDIT_CURRENT_VIEW);
			}
		});

		// send note button
		sendButton = (Button) findViewById(R.id.view_send);
		sendButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_send, 0, 0, 0);
		sendButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				// set Sent string Id
				List<Long> rowArr = new ArrayList<Long>();
				rowArr.add(0,mRowId);

				String sentString = Util.getStringWithXmlTag(rowArr);
				sentString = Util.addXmlTag(sentString);

				String picFile = mDb.getNotePictureUriById(mRowId);
				System.out.println("-> picFile = " + picFile);
				String[] picFileArray = null;
				if( (picFile != null) &&
						(picFile.length() > 0) )
				{
					picFileArray = new String[]{picFile};
				}
				new MailNotes(mAct,sentString,picFileArray);
			}
		});

		// back button
		backButton = (Button) findViewById(R.id.view_back);
		backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);
		backButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view) {
				if(isTextMode())
				{
					// back to view all mode
					setViewAllMode();
					showSelectedView();
					mAct.invalidateOptionsMenu();
				}
				else //view all mode
				{
					stopAV();
					finish();
				}
			}
		});

	}

	// on page change listener
	ViewPager.SimpleOnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener()
	{
		@Override
		public void onPageSelected(int nextPosition)
		{
			if(AudioPlayer.mAudioPlayMode  == AudioPlayer.ONE_TIME_MODE)
				UtilAudio.stopAudioPlayer();

			mCurrentPosition = mPager.getCurrentItem();
			System.out.println("Note_view / _onPageSelected");
			System.out.println("    mCurrentPosition = " + mCurrentPosition);
			System.out.println("    nextPosition = " + nextPosition);

			mIsViewModeChanged = false;

			// show audio name
			mRowId = mDb.getNoteId(nextPosition,true);
			System.out.println("Note_view / _onPageSelected / mRowId = " + mRowId);
			mAudioUriInDB = mDb.getNoteAudioUriById(mRowId);
			System.out.println("Note_view / _onPageSelected / mAudioUriInDB = " + mAudioUriInDB);

			if(UtilAudio.hasAudioExtension(mAudioUriInDB))
			{
				mAudioBlock.setVisibility(View.VISIBLE);
				setAudioBlockListener();
				initAudioProgress(mAudioUriInDB);
			}
			else
				mAudioBlock.setVisibility(View.GONE);

			// update playing state of picture mode
			Note_view_pagerUI.showPictureViewUI(nextPosition);

			if((nextPosition == mCurrentPosition+1) || (nextPosition == mCurrentPosition-1))
			{
				if(AudioPlayer.mAudioPlayMode == AudioPlayer.ONE_TIME_MODE)
					AudioPlayer.mAudioIndex = mCurrentPosition;//update Audio index
			}
			// When changing pages, reset the action bar actions since they are dependent
			// on which page is currently active. An alternative approach is to have each
			// fragment expose actions itself (rather than the activity exposing actions),
			// but for simplicity, the activity provides the actions in this sample.
			invalidateOptionsMenu();//The onCreateOptionsMenu(Menu) method will be called the next time it needs to be displayed.
		}
	};

	public static int getStyle() {
		return mStyle;
	}

	public void setStyle(int style) {
		mStyle = style;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		System.out.println("Note_view / _onActivityResult ");
        if((requestCode==EDIT_CURRENT_VIEW) || (requestCode==MAIL_CURRENT_VIEW))
        {
			stopAV();
        }
        else if(requestCode==Note_view_pager_adapter.VIEW_YOUTUBE_LINK)
        {
            // Exit from YouTube, back to view all mode
    		setViewAllMode();
        }
        else if(requestCode==Note_view_pager_adapter.VIEW_WEB_LINK)
        {
			// Exit from Web Link
            if(Util.isLandscapeOrientation(mAct))
                finish();//??? otherwise, will recreate?
            else
                setViewAllMode();

        }
		else if(requestCode == MailNotes.EMAIL)
		{
			Toast.makeText(mAct,R.string.mail_exit,Toast.LENGTH_SHORT).show();
			// note: result code is always 0 (cancel), so it is not used
			new DeleteFileAlarmReceiver(mAct,
					                    System.currentTimeMillis() + 1000 * 60 * 5, // formal: 300 seconds
//						    		    System.currentTimeMillis() + 1000 * 10, // test: 10 seconds
					                    MailNotes.mAttachmentFileName);
		}


	    // check if there is one note at least in the pager
		if( mPager.getAdapter().getCount() > 0 )
		{
			showSelectedView();
            invalidateOptionsMenu();
		}
		else
			finish();
	}
	
    //Refer to http://stackoverflow.com/questions/4434027/android-videoview-orientation-change-with-buffered-video
	/***************************************************************
	video play spec of Pause and Rotate:
	1. Rotate: keep pause state
	 pause -> rotate -> pause -> play -> continue

	2. Rotate: keep play state
	 play -> rotate -> continue play

	3. Key guard: enable pause
	 play -> key guard on/off -> pause -> play -> continue

	4. Key guard and Rotate: keep pause
	 play -> key guard on/off -> pause -> rotate -> pause
	 ****************************************************************/	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    System.out.println("Note_view / _onConfigurationChanged");

		// for audio
//		if(UtilAudio.hasAudioExtension(mAudioUriInDB))
//		{
//			recreate();
//		}
//		else
		{
			// set full screen picture for landscape orientation
			setLayoutView();
			if(Util.isLandscapeOrientation(this))
			{
				Util.setFullScreen(this);
				getActionBar().hide();
				setPictureMode();
			}
			else
			{
				Util.setNotFullScreen(this);
				getActionBar().show();
				setViewAllMode();
			}
			showSelectedView();
			invalidateOptionsMenu();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		System.out.println("Note_view / _onStart");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		System.out.println("Note_view / _onResume");

		setLayoutView();

		isPagerActive = true;

		///
//		if(UtilVideo.mVideoView != null)
//		{
//			if(!UtilVideo.hasMediaControlWidget )
//			{
//				UtilVideo.showVideoPlayButtonState();
//				UtilVideo.mVideoPlayButton.setOnClickListener(UtilVideo.videoPlayBtnListener);
//
//				Note_view_pagerUI.showPictureViewUI(mPager.getCurrentItem());
//			}
//			else if(UtilVideo.hasMediaControlWidget) //??? add App switch event?
//			{
//				UtilVideo.setVideoViewLayout(getCurrentPictureString());
//		   		UtilVideo.playOrPauseVideo(getCurrentPictureString());
//			}
//		}
		///

        // set full screen picture for landscape orientation
		if(Util.isLandscapeOrientation(this))
		{
			Util.setFullScreen(this);
			getActionBar().hide();
			setPictureMode();
		}
		else
		{
			Util.setNotFullScreen(this);
			getActionBar().show();
			setViewAllMode();
		}
		showSelectedView();
		invalidateOptionsMenu();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		System.out.println("Note_view / _onPause");

		// make onReceive stop going on
		Note_view_pagerUI.isWithinDelay = false;

		isPagerActive = false;

		// set pause when key guard is ON
		if( UtilVideo.mVideoView != null)
		{
			UtilVideo.mPlayVideoPosition = UtilVideo.mVideoView.getCurrentPosition();

			// keep play video position
			mPlayVideoPositionOfInstance = UtilVideo.mPlayVideoPosition;
			System.out.println("Note_view / _onPause / mPlayVideoPositionOfInstance = " + mPlayVideoPositionOfInstance);

			if(UtilVideo.mVideoPlayer != null)//??? try more to check if this is better? or still keep video view
				VideoPlayer.stopVideo();
		}

		// to stop YouTube web view running
    	String tagStr = "current"+ Note_view.mPager.getCurrentItem()+"webView";
    	CustomWebView webView = (CustomWebView) mPager.findViewWithTag(tagStr);
    	CustomWebView.pauseWebView(webView);
    	CustomWebView.blankWebView(webView);

		// to stop Link web view running
    	tagStr = "current"+ Note_view.mPager.getCurrentItem()+"linkWebView";
    	CustomWebView linkWebView = (CustomWebView) mPager.findViewWithTag(tagStr);
    	CustomWebView.pauseWebView(linkWebView);
    	CustomWebView.blankWebView(linkWebView);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
//		mIsViewModeChanged = false;
		System.out.println("Note_view / _onStop");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.out.println("Note_view / _onDestroy");
	}

	// avoid exception: has leaked window android.widget.ZoomButtonsController
	@Override
	public void finish() {
		
		if(mPagerHandler != null)
			mPagerHandler.removeCallbacks(mOnBackPressedRun);		
	    
		ViewGroup view = (ViewGroup) getWindow().getDecorView();
	    view.setBackgroundColor(getResources().getColor(color.background_dark)); // avoid white flash
	    view.removeAllViews();
	    
	    super.finish();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		System.out.println("Note_view / _onSaveInstanceState");
	}
	
	// On Create Options Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);

		// inflate menu
		getMenuInflater().inflate(R.menu.pager_menu, menu);

		// menu item: view mode
   		markCurrentSelected(menu.findItem(R.id.VIEW_ALL),"ALL");
		markCurrentSelected(menu.findItem(R.id.VIEW_PICTURE),"PICTURE_ONLY");
		markCurrentSelected(menu.findItem(R.id.VIEW_TEXT),"TEXT_ONLY");

	    // menu item: previous
		MenuItem itemPrev = menu.findItem(R.id.ACTION_PREVIOUS);
		itemPrev.setEnabled(mPager.getCurrentItem() > 0);
		itemPrev.getIcon().setAlpha(mPager.getCurrentItem() > 0?255:30);
		
		// menu item: Next or Finish
		MenuItem itemNext = menu.findItem(R.id.ACTION_NEXT);
		itemNext.setTitle((mPager.getCurrentItem() == mPagerAdapter.getCount() - 1)	?
									R.string.view_note_slide_action_finish :
									R.string.view_note_slide_action_next                  );

        // set Disable and Gray for Last item
		boolean isLastOne = (mPager.getCurrentItem() == (mPagerAdapter.getCount() - 1))? true:false;
        if(isLastOne)
        	itemNext.setEnabled(false);

        itemNext.getIcon().setAlpha(isLastOne?30:255);

        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// called after _onCreateOptionsMenu
        return true;
    }  
    
    // for menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	if(isTextMode())
            	{
        			// back to view all mode
            		setViewAllMode();
	        		showSelectedView();
	        		mAct.invalidateOptionsMenu();             		
            	}
            	else if(isViewAllMode())
            	{
					stopAV();
	            	finish();
            	}
                return true;

            case R.id.VIEW_NOTE_MODE:
            	return true;
            	
            case R.id.VIEW_ALL:
        		setViewAllMode();
        		showSelectedView();
        		invalidateOptionsMenu();
            	return true;
            	
            case R.id.VIEW_PICTURE:
        		setPictureMode();
        		showSelectedView();
        		invalidateOptionsMenu();
            	return true;

            case R.id.VIEW_TEXT:
        		setTextMode();
        		showSelectedView();
        		invalidateOptionsMenu();
            	return true;
            	
            case R.id.ACTION_PREVIOUS:
                // Go to the previous step in the wizard. If there is no previous step,
                // setCurrentItem will do nothing.
            	Note_view.mCurrentPosition--;
            	mPager.setCurrentItem(mPager.getCurrentItem() - 1);
                return true;

            case R.id.ACTION_NEXT:
                // Advance to the next step in the wizard. If there is no next step, setCurrentItem
                // will do nothing.
            	Note_view.mCurrentPosition++;
            	mPager.setCurrentItem(mPager.getCurrentItem() + 1);
            	
            	//TO
//            	mMP.setVolume(0,0);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static void openLink_YouTube()
    {
		// stop audio and video if playing
		stopAV();

		// start YouTube intent
		String linkUri = Note_view.mDb.getNoteLinkUri(mCurrentPosition,true);
		if(Util.isYouTubeLink(linkUri))
		{
			// open path by YouTube intent
			Intent intentView = new Intent(Intent.ACTION_VIEW, Uri.parse(linkUri));
			mAct.startActivityForResult(intentView,Note_view_pager_adapter.VIEW_YOUTUBE_LINK);
			System.out.println("Note_view / _openLink_YouTube / linkUri = " + linkUri);
        }
    }
    
    // on back pressed
    @Override
    public void onBackPressed() {
		System.out.println("Note_view / _onBackPressed");
    	// web view can go back
    	String tagStr = "current"+ Note_view.mPager.getCurrentItem()+"linkWebView";
    	CustomWebView linkWebView = (CustomWebView) mPager.findViewWithTag(tagStr);
        if (linkWebView.canGoBack()) 
        {
        	linkWebView.goBack();
        }
        else if(isPictureMode())
    	{
            // dispatch touch event to show buttons
            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis() + 100;
            float x = 0.0f;
            float y = 0.0f;
            // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
            int metaState = 0;
            MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN,
                                                    x, y,metaState);
            dispatchTouchEvent(event);
            event.recycle();

            // in order to make sure ImageViewBackButton is effective to be clicked
            mPagerHandler = new Handler();
            mPagerHandler.postDelayed(mOnBackPressedRun, 500);
        }
    	else if(isTextMode())
    	{
			// back to view all mode
    		setViewAllMode();
    		showSelectedView();
    		mAct.invalidateOptionsMenu();      		
    	}
    	else
    	{
    		System.out.println("Note_view / _onBackPressed / view all mode");
			stopAV();
        	finish();
    	}
    }
    
    static Handler mPagerHandler;
	static Runnable mOnBackPressedRun = new Runnable()
	{   @Override
		public void run()
		{
			if(Note_view_pagerUI.picView_back_button != null)
				Note_view_pagerUI.picView_back_button.performClick();
			
			if(Note_view_pager_adapter.mIntentView != null)
				Note_view_pager_adapter.mIntentView = null;
		}
	};
    
    // check if current note has audio Uri
    static boolean currentNoteHasAudioUri()
    {
		String audioStr = getCurrentAudioString();
		return UtilAudio.hasAudioExtension(audioStr)?true:false;
    }
    
    // get current audio string
    public static String getCurrentAudioString()
    {
		return  mDb.getNoteAudioUri(mCurrentPosition,true);
    }    
    
    // check if current note has video Uri
    static boolean currentNoteHasVideoUri()
    {
		String pictureStr = getCurrentPictureString();
		return UtilVideo.hasVideoExtension(pictureStr,mAct)?true:false;
    }
    
    // get current picture string
    public static String getCurrentPictureString()
    {
		return mDb.getNotePictureUri(mCurrentPosition,true);
    }
    
    // get current link string
    public static String getCurrentLinkString()
    {
		return mDb.getNoteLinkUri(mCurrentPosition,true);
    }    
    
    static void playAudioInPager()
    {
		if(currentNoteHasAudioUri())
		{
    		AudioPlayer.mAudioIndex = mCurrentPosition;
    		// new instance
    		if(AudioPlayer.mMediaPlayer == null)
    		{
        		int lastTimeView_NotesTblId =  Integer.valueOf(Util.getPref_lastTimeView_notes_tableId(mAct));
    			DrawerActivity.mCurrentPlaying_notesTableId = lastTimeView_NotesTblId;
        		AudioPlayer.mAudioPlayMode = AudioPlayer.ONE_TIME_MODE;
    		}
    		// If Audio player is NOT at One time mode and media exists
    		else if((AudioPlayer.mMediaPlayer != null) &&
    				(AudioPlayer.mAudioPlayMode == AudioPlayer.CONTINUE_MODE))
    		{
        		AudioPlayer.mAudioPlayMode = AudioPlayer.ONE_TIME_MODE;
        		UtilAudio.stopAudioPlayer();
    		}

   			AudioPlayer.prepareAudioInfo(mAct);

    		AudioPlayer.manageAudioState(mAct);

            updateAudioPlayingState();

    		// update playing state of picture mode
    		Note_view_pagerUI.showPictureViewUI(mCurrentPosition);
		}
    }
    
    // Mark current selected 
    void markCurrentSelected(MenuItem subItem, String str)
    {
        if(mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
        							.equalsIgnoreCase(str))
        	subItem.setIcon(R.drawable.btn_radio_on_holo_dark);
	  	else
        	subItem.setIcon(R.drawable.btn_radio_off_holo_dark);
    }    
    
    // show audio name
    static void showAudioName()
    {
//        String audio_name = "";
//    	if(!Util.isEmptyString(mAudioUriInDB))
//		{
//			audio_name = getResources().getText(R.string.note_audio) +
//						 ": " +
//						 Util.getDisplayNameByUriString(mAudioUriInDB,this);
//		}
//   		mPager_audio_title.setText(audio_name);

		// title: set marquee
		if(Util.isUriExisted(mAudioUriInDB,mAct)) {
			String audio_name = "";
			audio_name = Util.getDisplayNameByUriString(mAudioUriInDB,mAct);
			mPager_audio_title.setText(audio_name);
		}
		else
			mPager_audio_title.setText(R.string.file_not_found);

		mPager_audio_title.setSelected(false);

    }
    
    // Set audio block
    public static ImageView mPager_audio_play_button;
    static SeekBar seekBarProgress; 
    public static int mProgress;
	public static int mediaFileLength_MilliSeconds; // this value contains the song duration in milliseconds. Look at getDuration() method in MediaPlayer class
    
	public static boolean isPausedAtSeekerAnchor;
	public static int mAnchorPosition; 
    static void setAudioBlockListener()
    {
	    // set audio play and pause control image
	    mPager_audio_play_button.setOnClickListener(new View.OnClickListener() 
	    {
			@Override
			public void onClick(View v) 
			{
				isPausedAtSeekerAnchor = false;
            	TabsHostFragment.setAudioPlayingTab_WithHighlight(false);// in case playing audio in pager
            	playAudioInPager();
			}
		});   		
   		
		// set seek bar listener
		seekBarProgress.setOnSeekBarChangeListener(new OnSeekBarChangeListener() 
		{
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) 
			{
				if( AudioPlayer.mMediaPlayer != null  )
				{
					int mPlayAudioPosition = (int) (((float)(mediaFileLength_MilliSeconds / 100)) * seekBar.getProgress());
					AudioPlayer.mMediaPlayer.seekTo(mPlayAudioPosition);
				}
				else
				{
					// pause at seek bar anchor
					isPausedAtSeekerAnchor = true;
					mAnchorPosition = (int) (((float)(mediaFileLength_MilliSeconds / 100)) * seekBar.getProgress());
					playAudioInPager();
				}
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// audio player is one time mode in pager
				if(AudioPlayer.mAudioPlayMode == AudioPlayer.CONTINUE_MODE)
					UtilAudio.stopAudioPlayer();
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
			{
				if(fromUser)
				{	
					// show progress change
			    	int currentPos = mediaFileLength_MilliSeconds*progress/(seekBar.getMax()+1);
			    	int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
			    	int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
			     	int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
	
			    	// set current play time
			    	mPager_audio_curr_play_position.setText(String.format("%2d", curHour)+":" +
			    										   String.format("%02d", curMin)+":" +
			    										   String.format("%02d", curSec) );
			    	
//			    	mProgress = progress;
				}
			}
		});
    }  
    
    public static void primaryAudioSeekBarProgressUpdater() 
    {
    	int currentPos = AudioPlayer.mMediaPlayer.getCurrentPosition();
    	int curHour = Math.round((float)(currentPos / 1000 / 60 / 60));
    	int curMin = Math.round((float)((currentPos - curHour * 60 * 60 * 1000) / 1000 / 60));
     	int curSec = Math.round((float)((currentPos - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
    	// set current play time and the play length of audio file
     	if(mPager_audio_curr_play_position != null)
     	{
     		mPager_audio_curr_play_position.setText(String.format("%2d", curHour)+":" +
    										   String.format("%02d", curMin)+":" +
    										   String.format("%02d", curSec) );//??? why affect audio title?
     	}
     	
     	mProgress = (int)(((float)currentPos/mediaFileLength_MilliSeconds)*100);
     	
     	if(seekBarProgress != null)
     		seekBarProgress.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
    }
    
    public static void initAudioProgress(String audioUriInDB)
    {
    	mProgress = 0;

		showAudioName();
		mPager_audio_title.setSelected(false);
        mPager_audio_play_button.setImageResource(R.drawable.ic_media_play);
        mPager_audio_title.setTextColor(ColorSet.getPauseColor(mAct));
        mPager_audio_title.setSelected(false);

		// current position
    	int curHour = Math.round((float)(mProgress / 1000 / 60 / 60));
    	int curMin = Math.round((float)((mProgress - curHour * 60 * 60 * 1000) / 1000 / 60));
     	int curSec = Math.round((float)((mProgress - curHour * 60 * 60 * 1000 - curMin * 60 * 1000)/ 1000));
     	mPager_audio_curr_play_position.setText(String.format("%2d", curHour)+":" +
    										   String.format("%02d", curMin)+":" +
    										   String.format("%02d", curSec) );//??? why affect audio title?
		mPager_audio_curr_play_position.setTextColor(ColorSet.color_white);
	    // audio seek bar
     	seekBarProgress.setProgress(mProgress); // This math construction give a percentage of "was playing"/"song length"
		seekBarProgress.setMax(99); // It means 100% .0-99
    	seekBarProgress.setVisibility(View.VISIBLE); 
     	
    	// get audio file length
    	try
    	{
    		MediaPlayer mp = MediaPlayer.create(mAct,Uri.parse(mAudioUriInDB));
    		mediaFileLength_MilliSeconds = mp.getDuration();
    		mp.release();
    	}
    	catch(Exception e)
    	{
    		
    	}
    	// set audio file length
     	int fileHour = Math.round((float)(mediaFileLength_MilliSeconds / 1000 / 60 / 60));
     	int fileMin = Math.round((float)((mediaFileLength_MilliSeconds - fileHour * 60 * 60 * 1000) / 1000 / 60));
    	int fileSec = Math.round((float)((mediaFileLength_MilliSeconds - fileHour * 60 * 60 * 1000 - fileMin * 1000 * 60 )/ 1000));
    	mPager_audio_file_length.setText(String.format("%2d", fileHour)+":" +
    										  String.format("%02d", fileMin)+":" +
    										  String.format("%02d", fileSec));
		mPager_audio_file_length.setTextColor(ColorSet.color_white);
    }


    static public void updateAudioPlayingState()
    {
        if(AudioPlayer.mAudioPlayMode != AudioPlayer.ONE_TIME_MODE)
            return;

        // update playing state
        if(AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_PLAY)
        {
            mPager_audio_play_button.setImageResource(R.drawable.ic_media_pause);
			showAudioName();
			mPager_audio_title.setTextColor(ColorSet.getHighlightColor(mAct) );
            mPager_audio_title.setSelected(true);
        }
        else if( (AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_PAUSE) ||
                (AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_STOP)    )
        {
            mPager_audio_play_button.setImageResource(R.drawable.ic_media_play);
			showAudioName();
            mPager_audio_title.setTextColor(ColorSet.getPauseColor(mAct));
            mPager_audio_title.setSelected(false);
        }
    }

    // Show selected view
    static void showSelectedView()
    {
   		mIsViewModeChanged = false;

		if(!Note_view.isTextMode())
   		{
	   		if(UtilVideo.mVideoView != null)
	   		{
	   	   		// keep current video position for NOT text mode
				mPositionOfChangeView = UtilVideo.mPlayVideoPosition;
	   			mIsViewModeChanged = true;

	   			if(VideoPlayer.mVideoHandler != null)
	   			{
					System.out.println("Note_view / _showSelectedView / just remove callbacks");
	   				VideoPlayer.mVideoHandler.removeCallbacks(VideoPlayer.mRunPlayVideo);
	   				if(UtilVideo.hasMediaControlWidget)
	   					VideoPlayer.cancelMediaController();
	   			}
	   		}
   			Note_view_pager_adapter.mLastPosition = -1;
   		}

    	if(mPagerAdapter != null)
    		mPagerAdapter.notifyDataSetChanged(); // will call Note_view_pager_adapter / _setPrimaryItem
    }
    
    public static int mPositionOfChangeView;
    public static boolean mIsViewModeChanged;
    
    // show picture or not
    public static void showImageByTouchImageView(final View spinner, final TouchImageView pictureView, String strPicture) 
    {
    	if(Util.isEmptyString(strPicture))
    	{
    		pictureView.setImageResource(mStyle%2 == 1 ?
	    			R.drawable.btn_radio_off_holo_light:
	    			R.drawable.btn_radio_off_holo_dark);//R.drawable.ic_empty);
    	}
    	else if(!Util.isUriExisted(strPicture,mAct))	
    	{
    		pictureView.setImageResource(R.drawable.ic_not_found);
    	}
    	else
    	{
    		Uri imageUri = Uri.parse(strPicture);
    		if(imageUri.isAbsolute())
    			UilCommon.imageLoader.displayImage(imageUri.toString(), 
    											   pictureView,
    											   UilCommon.optionsForFadeIn,
    											   new SimpleImageLoadingListener()
    		{
				@Override
				public void onLoadingStarted(String imageUri, View view) 
				{
					System.out.println("_showImageByTouchImageView / onLoadingStarted");
					// make spinner appears at center
					spinner.setVisibility(View.VISIBLE);
					view.setVisibility(View.GONE);
				}

				@Override
				public void onLoadingFailed(String imageUri, View view, FailReason failReason) 
				{
					System.out.println("_showImageByTouchImageView / onLoadingFailed");
					String message = null;
					switch (failReason.getType()) 
					{
						case IO_ERROR:
							message = "Input/Output error";
							break;
						case DECODING_ERROR:
							message = "Image can't be decoded";
							break;
						case NETWORK_DENIED:
							message = "Downloads are denied";
							break;
						case OUT_OF_MEMORY:
							message = "Out Of Memory error";
							break;
						case UNKNOWN:
							message = "Unknown error";//??? mark this line?
							break;
					}
					Toast.makeText(mAct, message, Toast.LENGTH_SHORT).show();
					spinner.setVisibility(View.GONE);
					view.setVisibility(View.GONE);
				}

				@Override
				public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage)
				{
					System.out.println("_showImageByTouchImageView / onLoadingComplete");
					spinner.setVisibility(View.GONE);
					view.setVisibility(View.VISIBLE);
                    pictureView.setImageBitmap(loadedImage);
				}
			});
    	}
	}
    
    static void setViewAllMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","ALL")
		   						  .apply();
    }
    
    static void setPictureMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","PICTURE_ONLY")
		   						  .apply();
    }
    
    static void setTextMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","TEXT_ONLY")
		   						  .apply();
    }
    
    
    public static boolean isPictureMode()
    {
	  	return mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
										.equalsIgnoreCase("PICTURE_ONLY");
    }
    
    public static boolean isViewAllMode()
    {
	  	return mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
										.equalsIgnoreCase("ALL");
    }

    public static boolean isTextMode()
    {
	  	return mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
										.equalsIgnoreCase("TEXT_ONLY");
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int maskedAction = event.getActionMasked();
        switch (maskedAction) {

	        case MotionEvent.ACTION_DOWN:
	        case MotionEvent.ACTION_POINTER_DOWN: 
	        	 //??? how to detect zoom image?
        		 // update playing state of picture mode
    			 System.out.println("Note_view / _dispatchTouchEvent / MotionEvent.ACTION_DOWN / mPager.getCurrentItem() =" + mPager.getCurrentItem());
    			 if(Note_view_pagerUI.isWithinDelay)
    			 {
    				 //note: if interval below is too short (e.q. 300), picture view UI will not show after changing page
    				 Note_view_pagerUI.delay_pagerUI_all_off(this,500);//2nd touch to turn off UI
    			 }
    			 else
				 {
					 Note_view_pagerUI.showPictureViewUI(mPager.getCurrentItem());
					 Note_view_pagerUI.delay_pagerUI_all_off(this,3001);//1st touch to turn on UI
				 }
    	  	  	 break;
	        case MotionEvent.ACTION_MOVE: 
	        case MotionEvent.ACTION_UP:
	        case MotionEvent.ACTION_POINTER_UP:
	        case MotionEvent.ACTION_CANCEL: 
	        	 break;
        }

        return super.dispatchTouchEvent(event);
    }

	public static void stopAV()
	{
		if(AudioPlayer.mAudioPlayMode == AudioPlayer.ONE_TIME_MODE)
			UtilAudio.stopAudioPlayer();

		VideoPlayer.stopVideo();
	}
}