package com.cw.litenote;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.cw.litenote.config.Config;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.DeleteFileAlarmReceiver;
import com.cw.litenote.config.Export_toSDCardFragment;
import com.cw.litenote.config.Import_fromSDCardFragment;
import com.cw.litenote.db.DB;
import com.cw.litenote.media.audio.AudioPlayer;
import com.cw.litenote.media.audio.NoisyAudioStreamReceiver;
import com.cw.litenote.media.audio.UtilAudio;
import com.cw.litenote.media.image.GalleryGridAct;
import com.cw.litenote.media.image.SlideshowInfo;
import com.cw.litenote.media.image.SlideshowPlayer;
import com.cw.litenote.media.image.UtilImage;
import com.cw.litenote.note.Note_addAudio;
import com.cw.litenote.note.Note_addCameraImage;
import com.cw.litenote.note.Note_addCameraVideo;
import com.cw.litenote.note.Note_addNewText;
import com.cw.litenote.note.Note_addNew_optional;
import com.cw.litenote.note.Note_addNew_optional_for_multiple;
import com.cw.litenote.note.Note_addReadyImage;
import com.cw.litenote.note.Note_addReadyVideo;
import com.cw.litenote.note.Note_addLink;
import com.cw.litenote.preference.Define;
import com.cw.litenote.util.EULA_dlg;
import com.cw.litenote.util.MailNotes;
import com.cw.litenote.util.OnBackPressedListener;
import com.cw.litenote.config.MailPagesFragment;
import com.cw.litenote.util.Util;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.FragmentActivity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class DrawerActivity extends FragmentActivity implements OnBackStackChangedListener 
{
    static DrawerLayout mDrawerLayout;
    private DragSortController mController;
    public static DragSortListView mDrawerListView;
    static ActionBarDrawerToggle mDrawerToggle;
    static CharSequence mDrawerChildTitle;
    private CharSequence mAppTitle;
    public static Context mContext;
	public static Config mConfigFragment;
	public static boolean bEnableConfig;
    static Menu mMenu;
    public static DB mDb;
    public static DB mDb_tabs;
    public static DB mDb_notes;
    static DrawerAdapter drawerAdapter;
    static List<String> mDrawerChildTitles;
    public static int mFocus_drawerChildPos;
	static NoisyAudioStreamReceiver noisyAudioStreamReceiver;
	static IntentFilter intentFilter;
	public static FragmentActivity mDrawerActivity;
	public static FragmentManager fragmentManager;
	public static FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener;
	public static int mLastOkTabId = 1;
	OnBackPressedListener onBackPressedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
    	///
//    	 StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//
//    	   .detectDiskReads()
//    	   .detectDiskWrites()
//    	   .detectNetwork() 
//    	   .penaltyLog()
//    	   .build());
//
//    	    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
////    	   .detectLeakedSqlLiteObjects() //??? unmark this line will cause strict mode error
//    	   .penaltyLog() 
//    	   .penaltyDeath()
//    	   .build());     	
    	///
        super.onCreate(savedInstanceState);
        
        mDrawerActivity = this;
        setContentView(R.layout.drawer_activity);

		// Release mode: no debug message
        if(Define.CODE_MODE == Define.RELEASE_MODE)
        {
        	OutputStream nullDev = new OutputStream() 
            {
                public  void    close() {}
                public  void    flush() {}
                public  void    write(byte[] b) {}
                public  void    write(byte[] b, int off, int len) {}
                public  void    write(int b) {}
            }; 
            System.setOut( new PrintStream(nullDev));
        }
        
        //Log.d below can be disabled by applying proguard
        //1. enable proguard-android-optimize.txt in project.properties
        //2. be sure to use newest version to avoid build error
        //3. add the following in proguard-project.txt
        /*-assumenosideeffects class android.util.Log {
        public static boolean isLoggable(java.lang.String, int);
        public static int v(...);
        public static int i(...);
        public static int w(...);
        public static int d(...);
        public static int e(...);
    	}
        */
        Log.d("test log tag","start app");         
        
        System.out.println("================start application ==================");
        System.out.println("DrawerActivity / onCreate");

        UtilImage.getDefaultSacleInPercent(DrawerActivity.this);
        
        mAppTitle = getTitle();
        
        mDrawerChildTitles = new ArrayList<String>();

		Context context = getApplicationContext();

		if(mDb != null)
			mDb.close();

		mDb = new DB(context);
		mDb.initDrawerDb(mDb);

		if(mDb_tabs != null)
			mDb_tabs.close();

		mDb_tabs = new DB(context,Util.getPref_lastTimeView_tabs_tableId(this));
		mDb_tabs.initTabsDb(mDb_tabs);

		if(mDb_notes != null)
			mDb_tabs.close();

		mDb_notes = new DB(context,Util.getPref_lastTimeView_notes_tableId(this));
		mDb_notes.initNotesDb(mDb_notes);

		//Add note with the link
		String intentLink = addIntentLink(getIntent());
		if(!Util.isEmptyString(intentLink) )
		{
			finish(); // for no active DrawerActivity case
		}
		else
		{
			// check DB
			final boolean ENABLE_DB_CHECK = false;//true;//
			if(ENABLE_DB_CHECK)
			{
		        // list all drawer tables
				int drawerCount = mDb.getDrawerChildCount();
				for(int drawerPos=0; drawerPos<drawerCount; drawerPos++)
		    	{
		    		String drawerTitle = mDb.getDrawerChild_Title(drawerPos);
		    		DrawerActivity.mFocus_drawerChildPos = drawerPos;
	
		    		// list all tab tables
		    		int tabsTableId = mDb.getTabsTableId(drawerPos);
		    		System.out.println("--- tabs table Id = " + tabsTableId +
									   ", drawer title = " + drawerTitle);
		    		mDb_tabs = new DB(context,tabsTableId);
		    		mDb_tabs.initTabsDb(mDb_tabs);
		    		int tabsCount = mDb_tabs.getTabsCount(true);
		        	for(int tabPos=0; tabPos<tabsCount; tabPos++)
		        	{
		        		TabsHostFragment.mCurrent_tabIndex = tabPos;
		        		int tabId = mDb_tabs.getTabId(tabPos, true);
		        		int notesTableId = mDb_tabs.getNotesTableId(tabPos, true);
		        		String tabTitle = mDb_tabs.getTabTitle(tabPos, true);
		        		System.out.println("   --- tab Id = " + tabId);
		        		System.out.println("   --- notes table Id = " + notesTableId);
		        		System.out.println("   --- tab title = " + tabTitle);
		        		
		        		mLastOkTabId = tabId;
		        		
		        		try {
	        				 mDb_notes = new DB(context,String.valueOf(notesTableId));
	        				 mDb_notes.initNotesDb(mDb_notes);
		        			 mDb_notes.doOpenNotes();
		        			 mDb_notes.doCloseNotes();
						} catch (Exception e) {
						}
		        	}
		    	}
				
				// recover focus
				int tabsTableId = Util.getPref_lastTimeView_tabs_tableId(this);
	    		DB.setFocus_tabsTableId(tabsTableId);
				String notesTableId = Util.getPref_lastTimeView_notes_tableId(this);
				DB.setFocus_notes_tableId(notesTableId);				
			}//if(ENABLE_DB_CHECK)
			
	        // get last time drawer number, default drawer number: 1
	        if (savedInstanceState == null)
	        {
	        	for(int i=0;i<mDb.getDrawerChildCount();i++)
	        	{
		        	if(	mDb.getTabsTableId(i)== 
		        		Util.getPref_lastTimeView_tabs_tableId(this))
		        	{
		        		mFocus_drawerChildPos =  i;
		    			System.out.println("DrawerActivity / onCreate /  mFocusDrawerId = " + mFocus_drawerChildPos);
		        	}
	        	}
	        	AudioPlayer.mPlayerState = AudioPlayer.PLAYER_AT_STOP;
	        	UtilAudio.mIsCalledWhilePlayingAudio = false;
	        }

			// set drawer title
			if(mDb.getDrawerChildCount() == 0)
			{
		        for(int i = 0; i< Define.ORIGIN_TABS_TABLE_COUNT; i++)
		        {
					String drawerTitle = Define.getDrawerTitle(mDrawerActivity,i);
		        	mDrawerChildTitles.add(drawerTitle);
		        	mDb.insertDrawerChild(i+1, drawerTitle );
		        }
			}
			else
			{
			    for(int i=0;i< mDb.getDrawerChildCount();i++)
		        {
		        	mDrawerChildTitles.add(""); // init only
		        	mDrawerChildTitles.set(i, mDb.getDrawerChild_Title(i)); 
		        }
			}
	
			mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
	        mDrawerListView = (DragSortListView) findViewById(R.id.left_drawer);
	
	        // set a custom shadow that overlays the main content when the drawer opens
	        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
	        
	        // set adapter
	    	mDb.doOpenDrawer();
	    	Cursor cursor = DB.mCursor_drawerChild;
	        
	        String[] from = new String[] { DB.KEY_DRAWER_TITLE};
	        int[] to = new int[] { R.id.drawerText };
	        
	        drawerAdapter = new DrawerAdapter(
					this,
					R.layout.drawer_list_item,
					cursor,
					from,
					to,
					0
					);
	        
	        mDb.doCloseDrawer();
	        
	        mDrawerListView.setAdapter(drawerAdapter);
	   
	        // set up click listener
	        MainUi.addDrawerItemListeners();//??? move to resume?
	        mDrawerListView.setOnItemClickListener(MainUi.itemClick);
	        // set up long click listener
	        mDrawerListView.setOnItemLongClickListener(MainUi.itemLongClick);
	        
	        mController = DrawerListview.buildController(mDrawerListView);
	        mDrawerListView.setFloatViewManager(mController);
	        mDrawerListView.setOnTouchListener(mController);
	
	        // init drawer dragger
	    	mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
	    	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAWER_DRAGGABLE", "no")
	    			                    .equalsIgnoreCase("yes"))
	    		mDrawerListView.setDragEnabled(true);
	    	else
	    		mDrawerListView.setDragEnabled(false);
	        
	        mDrawerListView.setDragListener(DrawerListview.onDrag);
	        mDrawerListView.setDropListener(DrawerListview.onDrop);
	        
	        // enable ActionBar app icon to behave as action to toggle nav drawer
	        getActionBar().setDisplayHomeAsUpEnabled(true);
	        getActionBar().setHomeButtonEnabled(true);
//			getActionBar().setBackgroundDrawable(new ColorDrawable(ColorSet.bar_color));
			getActionBar().setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(mDrawerActivity)));

	        // ActionBarDrawerToggle ties together the the proper interactions
	        // between the sliding drawer and the action bar app icon
	        mDrawerToggle = new ActionBarDrawerToggle(
		                this,                  /* host Activity */
		                mDrawerLayout,         /* DrawerLayout object */
		                R.drawable.ic_drawer,  /* navigation drawer image to replace 'Up' caret */
		                R.string.drawer_open,  /* "open drawer" description for accessibility */
		                R.string.drawer_close  /* "close drawer" description for accessibility */
	                ) 
	        {
	            public void onDrawerClosed(View view) 
	            {
	        		System.out.println("mDrawerToggle onDrawerClosed ");
	        		int pos = mDrawerListView.getCheckedItemPosition();
	        		int tblId = mDb.getTabsTableId(pos);
	        		DB.setSelected_tabsTableId(tblId);        		
	        		mDrawerChildTitle = mDb.getDrawerChild_Title(pos);
	                setTitle(mDrawerChildTitle);
	                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
	                
	                // add for deleting drawer
	                if(TabsHostFragment.mTabHost == null)
	                {
	                	MainUi.selectDrawerChild(mFocus_drawerChildPos);
	            		setTitle(mDrawerChildTitle);
	                }
	            }
	
	            public void onDrawerOpened(View drawerView) 
	            {
	        		System.out.println("mDrawerToggle onDrawerOpened ");
	                setTitle(mAppTitle);
	                drawerAdapter.notifyDataSetChanged();
	                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
	            }
	        };
	        mDrawerLayout.setDrawerListener(mDrawerToggle);
	
	        mContext = getBaseContext();
	        bEnableConfig = false;

			// add on back stack changed listener
	        fragmentManager = getSupportFragmentManager();
			mOnBackStackChangedListener = DrawerActivity.this;
	        fragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener);

			// register an audio stream receiver
			if(noisyAudioStreamReceiver == null)
			{
				noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();
				intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY); 
				registerReceiver(noisyAudioStreamReceiver, intentFilter);
			}
			
		}

		// Show license dialog
		new EULA_dlg(this).show();
    }

	String addIntentLink(Intent intent)
	{
		//Add note with the link
		Bundle extras = intent.getExtras();
		String pathOri = null;
		String path;
		if(extras != null)
			pathOri = extras.getString(Intent.EXTRA_TEXT);

		path = pathOri;

		if(!Util.isEmptyString(pathOri))
		{
			System.out.println("-------link path of Share 1 = " + pathOri);
			// for SoundCloud case, path could contain other strings before URI path
			if(pathOri.contains("http"))
			{
				String[] str = pathOri.split("http");

				for(int i=0;i< str.length;i++)
				{
					if(str[i].contains("://"))
						path = "http".concat(str[i]);
				}
			}

			System.out.println("-------link path of Share 2 = " + path);
			mDb_notes.doOpenNotes();
			mDb_notes.insertNote("", "", "", "", path, "", 0, (long) 0);// add new note, get return row Id
			mDb_notes.doCloseNotes();
			String title;

			// save to top or to bottom
			int count = mDb_notes.getNotesCount(true);
			SharedPreferences mPref_add_new_note_location = getSharedPreferences("add_new_note_option", 0);
			if( mPref_add_new_note_location.getString("KEY_ADD_NEW_NOTE_TO","bottom").equalsIgnoreCase("top") &&
					(count > 1)        )
			{
				NoteFragment.swap();
			}

			if( Util.isYouTubeLink(path))
				title = Util.getYoutubeTitle(path);
			else
				title = pathOri; //??? better way?

			Toast.makeText(this,
					getResources().getText(R.string.add_new_note_option_title) + title,
					Toast.LENGTH_SHORT)
					.show();
			return title;
		}
		else
			return null;
	}

    /*
     * Life cycle
     * 
     */
    // for Rotate screen
    @Override
    protected void onSaveInstanceState(Bundle outState) 
    {
       super.onSaveInstanceState(outState);
//  	   System.out.println("DrawerActivity / onSaveInstanceState / mFocusDrawerPos = " + mFocusDrawerPos);
       outState.putInt("CurrentDrawerIndex",mFocus_drawerChildPos);
       outState.putInt("CurrentPlaying_TabIndex",mCurrentPlaying_tabIndex);
       outState.putInt("CurrentPlaying_DrawerIndex",mCurrentPlaying_drawerIndex);
       outState.putInt("SeekBarProgress",NoteFragment.mProgress);
       outState.putInt("PlayerState",AudioPlayer.mPlayerState);
       outState.putBoolean("CalledWhilePlayingAudio", UtilAudio.mIsCalledWhilePlayingAudio);
       if(MainUi.mHandler != null)
    	   MainUi.mHandler.removeCallbacks(MainUi.mTabsHostRun);
       MainUi.mHandler = null;
    }
    
    // for After Rotate
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
		System.out.println("DrawerActivity / _onRestoreInstanceState ");
    	if(savedInstanceState != null)
    	{
    		mFocus_drawerChildPos = savedInstanceState.getInt("CurrentDrawerIndex");
    		mCurrentPlaying_tabIndex = savedInstanceState.getInt("CurrentPlaying_TabIndex");
    		mCurrentPlaying_drawerIndex = savedInstanceState.getInt("CurrentPlaying_DrawerIndex");
    		AudioPlayer.mPlayerState = savedInstanceState.getInt("PlayerState");
    		NoteFragment.mProgress = savedInstanceState.getInt("SeekBarProgress");
//    		System.out.println("DrawerActivity / onRestoreInstanceState / AudioPlayer.mPlayerState = " + AudioPlayer.mPlayerState);
    		UtilAudio.mIsCalledWhilePlayingAudio = savedInstanceState.getBoolean("CalledWhilePlayingAudio");
    	}    
    	
    }

    @Override
    protected void onPause() {
    	super.onPause();
    	System.out.println("DrawerActivity / _onPause"); 
    }


	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		String intentLink = addIntentLink(intent);
		if(!Util.isEmptyString(intentLink) )
		{
//			finish();
		}
	}



	@Override
    protected void onResume() 
    {
    	System.out.println("DrawerActivity / _onResume");

		// mDrawerActivity will be destroyed after adding note with a YouTube link,
		// so it is necessary to recreate activity
//		if(Build.VERSION.SDK_INT >= 17)
//		{
//			if (mDrawerActivity.isDestroyed()) {
//				System.out.println("DrawerActivity / _onResume / do recreate");//??? can run this?
//				recreate();
//			}
//		}

      	// To Registers a listener object to receive notification when incoming call
     	TelephonyManager telMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
     	if(telMgr != null) 
     	{
     		telMgr.listen(UtilAudio.phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
     	}
        super.onResume();
    }
	
    @Override
    protected void onResumeFragments() {
    	System.out.println("DrawerActivity / _onResumeFragments ");
    	super.onResumeFragments();

		// fix: home button failed after power off/on in Config fragment
		fragmentManager.popBackStack();

    	MainUi.selectDrawerChild(mFocus_drawerChildPos);
    	setTitle(mDrawerChildTitle);
    }
    
    @Override
    protected void onDestroy() 
    {
    	System.out.println("DrawerActivity / onDestroy");
    	
    	//unregister TelephonyManager listener 
        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(mgr != null) {
            mgr.listen(UtilAudio.phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        
		// unregister an audio stream receiver
		if(noisyAudioStreamReceiver != null)
		{
			try
			{
				unregisterReceiver(noisyAudioStreamReceiver);//??? unregister here? 
			}
			catch (Exception e)
			{
			}
			noisyAudioStreamReceiver = null;
		}        
		super.onDestroy();
    }
    
    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        System.out.println("DrawerActivity / onPostCreate");
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        System.out.println("DrawerActivity / onConfigurationChanged");
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    
    
    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        System.out.println("DrawerActivity / onPrepareOptionsMenu");
        // If the navigation drawer is open, hide action items related to the content view
        boolean drawerOpen = false;

		if( mDrawerLayout == null)
			return false;

		if( mDrawerLayout.isDrawerOpen(mDrawerListView) )
			drawerOpen = true;

        if(drawerOpen)
        {
        	mMenu.setGroupVisible(R.id.group0, false);
    		mMenu.setGroupVisible(R.id.group1, true);
        }
        else
        {
            setTitle(mDrawerChildTitle);
    		mMenu.setGroupVisible(R.id.group1, false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

	@Override
    public void setTitle(CharSequence title) {
    	super.setTitle(title);
    	setDrawerTitle(title);
    }

    public static void setDrawerTitle(CharSequence title) {
        if(title == null)
        {
        	title = mDrawerChildTitle;
//        	fragmentManager.popBackStack();
        	initActionBar();
            mDrawerLayout.closeDrawer(mDrawerListView);
        }
        mDrawerActivity.getActionBar().setTitle(title);
    }	    
    
	/******************************************************
	 * Menu
	 * 
	 */
    // Menu identifiers
	static SharedPreferences mPref_show_note_attribute;
	/*
	 * onCreate Options Menu
	 */
	public static MenuItem mSubMenuItemAudio;
	MenuItem playOrStopMusicButton;
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
//		System.out.println("DrawerActivity / onCreateOptionsMenu");
		mMenu = menu;

		// inflate menu
		getMenuInflater().inflate(R.menu.main_menu, menu);

	    // check camera feature
	    PackageManager packageManager = this.getPackageManager();

		// camera image
		if(!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA))
			menu.findItem(R.id.ADD_NEW_IMAGE).setEnabled(false);

		// camera video
		if(!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA))
			menu.findItem(R.id.ADD_NEW_VIDEO).setEnabled(false);

		playOrStopMusicButton = menu.findItem(R.id.PLAY_OR_STOP_MUSIC);

	    // show body
	    mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
    	if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
			menu.findItem(R.id.SHOW_BODY)
     	   		.setIcon(R.drawable.ic_menu_collapse)
				.setTitle(R.string.preview_note_body_no) ;
    	else
			menu.findItem(R.id.SHOW_BODY)
				.setIcon(R.drawable.ic_menu_expand)
				.setTitle(R.string.preview_note_body_yes) ;

    	// show draggable
	    mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
    	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "no").equalsIgnoreCase("yes"))
			menu.findItem(R.id.ENABLE_NOTE_DRAG_AND_DROP)
				.setIcon(R.drawable.ic_dragger_off)
				.setTitle(R.string.draggable_no) ;
		else
			menu.findItem(R.id.ENABLE_NOTE_DRAG_AND_DROP)
				.setIcon(R.drawable.ic_dragger_on)
				.setTitle(R.string.draggable_yes) ;

		//
	    // Group 1 sub_menu for drawer operation
		//

	    // add sub_menu item: add drawer dragger  setting
    	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAWER_DRAGGABLE", "no")
    								.equalsIgnoreCase("yes"))
			menu.findItem(R.id.ENABLE_DRAWER_DRAG_AND_DROP)
				.setIcon(R.drawable.ic_dragger_off)
				.setTitle(R.string.draggable_no) ;
    	else
			menu.findItem(R.id.ENABLE_DRAWER_DRAG_AND_DROP)
				.setIcon(R.drawable.ic_dragger_on)
				.setTitle(R.string.draggable_yes) ;

		return super.onCreateOptionsMenu(menu);
	}
	
	// set activity Enabled/Disabled
//	public static void setActivityEnabled(Context context,final Class<? extends Activity> activityClass,final boolean enable)
//    {
//	    final PackageManager pm=context.getPackageManager();
//	    final int enableFlag=enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
//	    pm.setComponentEnabledSetting(new ComponentName(context,activityClass),enableFlag,PackageManager.DONT_KILL_APP);
//    }
	
	/*
	 * on options item selected
	 * 
	 */
	public static SlideshowInfo slideshowInfo;
	static FragmentTransaction mFragmentTransaction;
	public static int mCurrentPlaying_notesTableId;
	public static int mCurrentPlaying_tabIndex;
	public static int mCurrentPlaying_drawerIndex;
	public static int mCurrentPlaying_drawerTabsTableId;
	public final static int REQUEST_ADD_YOUTUBE_LINK = 1;
	public final static int REQUEST_ADD_WEB_LINK = 2;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) //??? java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
    {
		// Go back: check if Configure fragment now
		if( (item.getItemId() == android.R.id.home ))
    	{
    		System.out.println("DrawerActivity / onOptionsItemSelected / Home key of Config is pressed ");

			if(fragmentManager.getBackStackEntryCount() > 0 )
			{
				fragmentManager.popBackStack();
				if(bEnableConfig)
				{
					initActionBar();
					setTitle(mDrawerChildTitle);
					mDrawerLayout.closeDrawer(mDrawerListView);
				}
				return true;
			}
    	}

    	
    	// The action bar home/up action should open or close the drawer.
    	// ActionBarDrawerToggle will take care of this.
    	if (mDrawerToggle.onOptionsItemSelected(item))
    	{
    		System.out.println("mDrawerToggle.onOptionsItemSelected(item) / ActionBarDrawerToggle");
    		return true;
    	}
    	
    	final Intent intent;
        switch (item.getItemId()) 
        {
	    	case MainUi.Constant.ADD_NEW_FOLDER:
	    		MainUi.renewFirstAndLastDrawerId();
	    		MainUi.addNewFolder(mDrawerActivity, MainUi.mLastExist_drawerTabsTableId+1);
				return true;
				
	    	case MainUi.Constant.ENABLE_DRAWER_DRAG_AND_DROP:
            	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAWER_DRAGGABLE", "no")
            			                    .equalsIgnoreCase("yes"))
            	{
            		mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAWER_DRAGGABLE","no")
            								 .apply();
            		mDrawerListView.setDragEnabled(false);
            	}
            	else
            	{
            		mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAWER_DRAGGABLE","yes")
            								 .apply();
            		mDrawerListView.setDragEnabled(true);
            	}
            	drawerAdapter.notifyDataSetChanged();
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                return true; 				
			
        	case MainUi.Constant.ADD_TEXT:
				intent = new Intent(this, Note_addNewText.class);
				new Note_addNew_optional(this, intent);
				return true;

        	case MainUi.Constant.ADD_CAMERA_PICTURE:
				intent = new Intent(this, Note_addCameraImage.class);
				new Note_addNew_optional(this, intent);
	            return true;

        	case MainUi.Constant.ADD_CAMERA_VIDEO:
				intent = new Intent(this, Note_addCameraVideo.class);
				new Note_addNew_optional(this, intent);
	            return true;	            
	            
        	case MainUi.Constant.ADD_READY_PICTURE:
				intent = new Intent(this, Note_addReadyImage.class); 
				new Note_addNew_optional_for_multiple(this, intent);
				return true;

        	case MainUi.Constant.ADD_READY_VIDEO:
				intent = new Intent(this, Note_addReadyVideo.class); 
				new Note_addNew_optional_for_multiple(this, intent);
				return true;
				
        	case MainUi.Constant.ADD_AUDIO:
				intent = new Intent(this, Note_addAudio.class); 
				new Note_addNew_optional_for_multiple(this, intent);
				return true;
        	
        	case MainUi.Constant.ADD_YOUTUBE_LINK:
                intent = new Intent(this, Note_addLink.class);
				intent.putExtra("LinkType",REQUEST_ADD_YOUTUBE_LINK);
                startActivity(intent);
				return true;

        	case MainUi.Constant.ADD_WEB_LINK:
				intent = new Intent(this, Note_addLink.class);
				intent.putExtra("LinkType",REQUEST_ADD_WEB_LINK);
				startActivity(intent);
				return true;

        	case MainUi.Constant.OPEN_PLAY_SUBMENU:
        		// new play instance: stop button is off
        	    if( (AudioPlayer.mMediaPlayer != null) && 
        	    	(AudioPlayer.mPlayerState != AudioPlayer.PLAYER_AT_STOP))
        		{
       		    	// show Stop
           			playOrStopMusicButton.setTitle(R.string.menu_button_stop_audio);
           			playOrStopMusicButton.setIcon(R.drawable.ic_media_stop);
        	    }
        	    else
        	    {
       		    	// show Play
           			playOrStopMusicButton.setTitle(R.string.menu_button_play_audio);
           			playOrStopMusicButton.setIcon(R.drawable.ic_media_play);        	    	
        	    }
        		return true;
        	
        	case MainUi.Constant.PLAY_OR_STOP_AUDIO:
        		if( (AudioPlayer.mMediaPlayer != null) &&
        			(AudioPlayer.mPlayerState != AudioPlayer.PLAYER_AT_STOP))
        		{
					UtilAudio.stopAudioPlayer();
					TabsHostFragment.setAudioPlayingTab_WithHighlight(false);
					NoteFragment.mItemAdapter.notifyDataSetChanged();
					NoteFragment.setFooter();
					return true; // just stop playing, wait for user action
        		}
        		else
        		{
        			AudioPlayer.mAudioPlayMode = AudioPlayer.CONTINUE_MODE;
        			AudioPlayer.mAudioIndex = 0;
       				AudioPlayer.prepareAudioInfo(this);
        			
        			AudioPlayer.manageAudioState(this);
        			
					NoteFragment.mItemAdapter.notifyDataSetChanged();
	        		NoteFragment.setFooter();
	        		
					// update notes table Id
					mCurrentPlaying_notesTableId = TabsHostFragment.mCurrent_notesTableId;
					// update playing tab index
					mCurrentPlaying_tabIndex = TabsHostFragment.mCurrent_tabIndex;
					// update playing drawer index
				    mCurrentPlaying_drawerIndex = mFocus_drawerChildPos;	        		
        		}
        		return true;

        	case MainUi.Constant.SLIDE_SHOW:
        		slideshowInfo = new SlideshowInfo();
    			
        		String notesTableId = Util.getPref_lastTimeView_notes_tableId(this);
    			DB.setFocus_notes_tableId(notesTableId);	
    			
        		// add images for slide show
    			mDb_notes.doOpenNotes();
        		for(int i=0;i< mDb_notes.getNotesCount(false) ;i++)
        		{
        			if(mDb_notes.getNoteMarking(i,false) == 1)
        			{
        				String pictureUri = mDb_notes.getNotePictureUri(i,false);
        				if((pictureUri.length() > 0) && UtilImage.hasImageExtension(pictureUri,this)) // skip empty
        					slideshowInfo.addImage(pictureUri);
        			}
        		}
        		mDb_notes.doCloseNotes();
        		          		
        		if(slideshowInfo.imageSize() > 0)
        		{
					// create new Intent to launch the slideShow player Activity
					Intent playSlideshow = new Intent(this, SlideshowPlayer.class);
					startActivity(playSlideshow);  
        		}
        		else
        			Toast.makeText(mContext,R.string.file_not_found,Toast.LENGTH_SHORT).show();
        		return true;
				
            case MainUi.Constant.ADD_NEW_PAGE:
            	System.out.println("--- MainUi.Constant.ADD_NEW_PAGE / TabsHostFragment.mLastExist_notesTableId = " + TabsHostFragment.mLastExist_notesTableId);
                MainUi.addNewPage(mDrawerActivity,TabsHostFragment.mLastExist_notesTableId + 1);
                
                return true;
                
            case MainUi.Constant.CHANGE_PAGE_COLOR:
            	MainUi.changePageColor(mDrawerActivity);
                return true;    
                
            case MainUi.Constant.SHIFT_PAGE:
            	MainUi.shiftPage(mDrawerActivity);
                return true;  
                
            case MainUi.Constant.SHOW_BODY:
            	mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
            	if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
            		mPref_show_note_attribute.edit().putString("KEY_SHOW_BODY","no").commit();
            	else
            		mPref_show_note_attribute.edit().putString("KEY_SHOW_BODY","yes").commit();
            	TabsHostFragment.updateTabChange(this);
                return true; 

            case MainUi.Constant.ENABLE_NOTE_DRAG_AND_DROP:
            	mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
            	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "no").equalsIgnoreCase("yes"))
            		mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAGGABLE","no").commit();
            	else
            		mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAGGABLE","yes").commit();
            	TabsHostFragment.updateTabChange(this);
                return true;

			case MainUi.Constant.EXPORT_TO_SD_CARD:
				mMenu.setGroupVisible(R.id.group0, false); //hide the menu
				DB dbTabs = new DB(this,DB.getFocus_tabsTableId());
				dbTabs.initTabsDb(dbTabs);
				if(dbTabs.getTabsCount(true)>0)
				{
					Export_toSDCardFragment exportFragment = new Export_toSDCardFragment();
					FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
					transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
					transaction.replace(R.id.content_frame, exportFragment,"export").addToBackStack(null).commit();
				}
				else
				{
					Toast.makeText(this, R.string.config_export_none_toast, Toast.LENGTH_SHORT).show();
				}
				return true;

			case MainUi.Constant.IMPORT_FROM_SD_CARD:
				mMenu.setGroupVisible(R.id.group0, false); //hide the menu
				Import_fromSDCardFragment importFragment = new Import_fromSDCardFragment();
				FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
				transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
				transaction.replace(R.id.content_frame, importFragment,"import").addToBackStack(null).commit();
				return true;

			case MainUi.Constant.SEND_PAGES:
				mMenu.setGroupVisible(R.id.group0, false); //hide the menu

				DB dbTabsMail = new DB(this,DB.getFocus_tabsTableId());
				dbTabsMail.initTabsDb(dbTabsMail);
				if(dbTabsMail.getTabsCount(true)>0)
				{
					MailPagesFragment mailFragment = new MailPagesFragment();
					FragmentTransaction transactionMail = getSupportFragmentManager().beginTransaction();
					transactionMail.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
					transactionMail.replace(R.id.content_frame, mailFragment,"mail").addToBackStack(null).commit();
				}
				else
				{
					Toast.makeText(this, R.string.config_export_none_toast, Toast.LENGTH_SHORT).show();
				}
            	return true;

            case MainUi.Constant.GALLERY:
				Intent i_browsePic = new Intent(this, GalleryGridAct.class);
				startActivity(i_browsePic);
            	return true; 	

            case MainUi.Constant.CONFIG_PREFERENCE:
            	mMenu.setGroupVisible(R.id.group0, false); //hide the menu
        		setTitle(R.string.settings);
        		bEnableConfig = true;
        		
            	mConfigFragment = new Config();
            	mFragmentTransaction = fragmentManager.beginTransaction();
				mFragmentTransaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                mFragmentTransaction.replace(R.id.content_frame, mConfigFragment).addToBackStack("config").commit();
                return true;
                
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    
    /*
     *  on Back button pressed
     *
     */
    @Override
    public void onBackPressed()
    {
        System.out.println("DrawerActivity / _onBackPressed");

		if (onBackPressedListener != null)
        {
            onBackPressedListener.doBack();
        }
		else
			super.onBackPressed();

		// stop audio player
		if (!bEnableConfig)
			UtilAudio.stopAudioPlayer();
    }

    static void initActionBar()
    {
		mConfigFragment = null;  
		bEnableConfig = false;
		mMenu.setGroupVisible(R.id.group0, true);
		mDrawerActivity.getActionBar().setDisplayShowHomeEnabled(true);
		mDrawerActivity.getActionBar().setDisplayHomeAsUpEnabled(true);
		mDrawerToggle.setDrawerIndicatorEnabled(true);
    }

	@Override
	public void onBackStackChanged() {
		int backStackEntryCount = fragmentManager.getBackStackEntryCount();
		System.out.println("--- DrawerActivity / _onBackStackChanged / backStackEntryCount = " + backStackEntryCount);

        if(backStackEntryCount == 1) // Config fragment
		{
			bEnableConfig = true;
			System.out.println("DrawerActivity / _onBackStackChanged / Config");
			getActionBar().setDisplayShowHomeEnabled(false);
			getActionBar().setDisplayHomeAsUpEnabled(true);
			mDrawerToggle.setDrawerIndicatorEnabled(false);
		}
		else if(backStackEntryCount == 0) // DrawerActivity
		{
            onBackPressedListener = null;
			bEnableConfig = false;
			System.out.println("DrawerActivity / _onBackStackChanged / DrawerActivity");
			initActionBar();
			setTitle(mDrawerChildTitle);
			invalidateOptionsMenu();
		}
	}

	public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
		this.onBackPressedListener = onBackPressedListener;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		System.out.println("DrawerActivity / _onActivityResult ");
		String stringFileName = null;

		if(requestCode== MailNotes.EMAIL)
			stringFileName = MailNotes.mAttachmentFileName;
		else if(requestCode== MailPagesFragment.EMAIL_PAGES)
			stringFileName = MailPagesFragment.mAttachmentFileName;

		Toast.makeText(mDrawerActivity,R.string.mail_exit,Toast.LENGTH_SHORT).show();

		// note: result code is always 0 (cancel), so it is not used
		new DeleteFileAlarmReceiver(mDrawerActivity,
					System.currentTimeMillis() + 1000 * 60 * 5, // formal: 300 seconds
//					System.currentTimeMillis() + 1000 * 10, // test: 10 seconds
					stringFileName);
	}
}