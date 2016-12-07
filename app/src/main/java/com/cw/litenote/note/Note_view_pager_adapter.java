package com.cw.litenote.note;

import com.cw.litenote.NoteFragment;
import com.cw.litenote.R;
import com.cw.litenote.db.DB;
import com.cw.litenote.media.audio.AudioPlayer;
import com.cw.litenote.media.audio.UtilAudio;
import com.cw.litenote.media.image.AsyncTaskAudioBitmap;
import com.cw.litenote.media.image.TouchImageView;
import com.cw.litenote.media.image.UtilImage;
import com.cw.litenote.media.video.UtilVideo;
import com.cw.litenote.media.video.VideoPlayer;
import com.cw.litenote.media.video.VideoViewCustom;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.Util;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.Layout.Alignment;
import android.text.style.AlignmentSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.VideoView;

public class Note_view_pager_adapter extends FragmentStatePagerAdapter //PagerAdapter 
{
    static final int VIEW_YOUTUBE_LINK = 7;
    static final int VIEW_WEB_LINK = 8;
	static int mLastPosition;
	static LayoutInflater inflater;
	static FragmentActivity mAct;
	static String mWebTitle;
	
    public Note_view_pager_adapter(FragmentManager fm,FragmentActivity act) 
    {
    	super(fm);
    	mAct = act;
        inflater = mAct.getLayoutInflater();
        mLastPosition = -1;
        System.out.println("Note_view_pager_adapter / constructor / mLastPosition = -1;");
    }
    
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		container.removeView((View) object);
		object = null;
	}

    @SuppressLint("SetJavaScriptEnabled")
	@Override
	public Object instantiateItem(ViewGroup container, final int position) 
    {
    	System.out.println("Note_view_pager_adapter / instantiateItem / position = " + position);
    	// Inflate the layout containing 
    	// 1. picture group: image,video, thumb nail, control buttons
    	// 2. text group: title, body, time 
    	View pagerView = inflater.inflate(R.layout.note_view_pager_adapter, container, false);
    	int style = Note_view.getStyle();
        pagerView.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

    	// Picture group
        ViewGroup pictureGroup = (ViewGroup) pagerView.findViewById(R.id.pictureContent);
        String tagPictureStr = "current"+ position +"pictureView";
        pictureGroup.setTag(tagPictureStr);
    	
        // image view
    	TouchImageView imageView = ((TouchImageView) pagerView.findViewById(R.id.image_view));
        String tagImageStr = "current"+ position +"imageView";
        imageView.setTag(tagImageStr);

		// video view
    	VideoViewCustom videoView = ((VideoViewCustom) pagerView.findViewById(R.id.video_view));
        String tagVideoStr = "current"+ position +"videoView";
        videoView.setTag(tagVideoStr);

		ProgressBar spinner = (ProgressBar) pagerView.findViewById(R.id.loading);

        // link web view
		CustomWebView linkWebView = ((CustomWebView) pagerView.findViewById(R.id.link_web_view));
        String tagStr = "current"+position+"linkWebView";
        linkWebView.setTag(tagStr);

//        setLinkWebView(linkWebView,spinner,CustomWebView.LINK_VIEW);
//		linkWebView.loadUrl(Note_view_pager.mDb.getNoteLinkUri(position,true));

        // line view
        View line_view = pagerView.findViewById(R.id.line_view);

    	// text group
        ViewGroup textGroup = (ViewGroup) pagerView.findViewById(R.id.textGroup);

        // Set tag for text web view
    	CustomWebView textWebView = ((CustomWebView) pagerView.findViewById(R.id.textGroup).findViewById(R.id.textBody));
        tagStr = "current"+position+"textWebView";
        textWebView.setTag(tagStr);

		// set text web view
        setWebView(textWebView,spinner,CustomWebView.TEXT_VIEW);

        String linkUri = Note_view.mDb.getNoteLinkUri(position,true);
        String strTitle = Note_view.mDb.getNoteTitle(position,true);
        String strBody = Note_view.mDb.getNoteBody(position,true);

        // View mode
    	// picture only
	  	if(Note_view.isPictureMode())
	  	{
			System.out.println("Note_view_pager_adapter / _instantiateItem / isPictureMode ");
	  		pictureGroup.setVisibility(View.VISIBLE);
	  	    showPictureView(pictureGroup, position,imageView,videoView,linkWebView,spinner);

	  	    line_view.setVisibility(View.GONE);
	  	    textGroup.setVisibility(View.GONE);
	  	}
	    // text only
	  	else if(Note_view.isTextMode())
	  	{
			System.out.println("Note_view_pager_adapter / _instantiateItem / isTextMode ");
	  		pictureGroup.setVisibility(View.GONE);

	  		line_view.setVisibility(View.VISIBLE);
	  		textGroup.setVisibility(View.VISIBLE);

	  	    if( Util.isYouTubeLink(linkUri) ||
	 	  	   !Util.isEmptyString(strTitle)||
	 	  	   !Util.isEmptyString(strBody)   )
	  	    {
	  	    	showTextWebView(position,textWebView);
	  	    }
	  	}
  		// picture and text
	  	else if(Note_view.isViewAllMode())
	  	{
			System.out.println("Note_view_pager_adapter / _instantiateItem / isViewAllMode ");

			// picture
			pictureGroup.setVisibility(View.VISIBLE);
	  	    showPictureView(pictureGroup, position,imageView,videoView,linkWebView,spinner);

	  	    line_view.setVisibility(View.VISIBLE);
	  	    textGroup.setVisibility(View.VISIBLE);

			// text
	  	    if( Util.isYouTubeLink(linkUri) ||
	  	       !Util.isEmptyString(strTitle)||
	  	       !Util.isEmptyString(strBody)   )
	  	    {
	  	    	showTextWebView(position,textWebView);
	  	    }
	  	}
        
    	container.addView(pagerView, 0);
    	
		return pagerView;			
    } //instantiateItem
	
    // show text web view
    static void showTextWebView(int position,CustomWebView textWebView)
    {
    	System.out.println("Note_view_pager_adapter/ _showTextView / position = " + position);

    	int viewPort;
    	// load text view data
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
			viewPort = VIEW_PORT_BY_DEVICE_WIDTH;
    	else
    		viewPort = VIEW_PORT_BY_NONE;

    	String strHtml;
		strHtml = getHtmlStringWithViewPort(position,viewPort);
		textWebView.loadData(strHtml,"text/html; charset=utf-8", "UTF-8");
    }
    
    // show picture view
    void showPictureView(ViewGroup viewGroup,
						 int position,
    		             TouchImageView imageView,
    		             VideoView videoView,
    		             CustomWebView linkWebView,
    		             ProgressBar spinner          )
    {
		String linkUri = Note_view.mDb.getNoteLinkUri(position,true);
		String pictureUri = Note_view.mDb.getNotePictureUri(position,true);
    	String audioUri = Note_view.mDb.getNoteAudioUri(position,true);

    	// Check if Uri is for YouTube
    	if(Util.isEmptyString(pictureUri) && 
    	   Util.isYouTubeLink(linkUri)   )// &&
//		   Util.isEmptyString(audioUri)         )
    	{
			pictureUri = "http://img.youtube.com/vi/"+Util.getYoutubeId(linkUri)+"/0.jpg";
			System.out.println("Note_view_pager_adapter / _showPictureView / pictureUri = " + pictureUri);

			// set play button
			Button playButton = (Button) (viewGroup.findViewById(R.id.video_view_play_video));
			playButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_play, 0, 0, 0);
			playButton.setVisibility(View.VISIBLE);

			// set listener for running YouTube
			playButton.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View view)
				{
					Note_view.openLink_YouTube();
				}
			});
		}

        if( ( !linkUri.startsWith("http") ||
			  Util.isYouTubeLink(linkUri)    ) &&
			!UtilVideo.hasVideoExtension(pictureUri,mAct))
		{
			Note_view_pagerUI.showPictureViewUI(position);
		}

        // show image view
  		if( UtilImage.hasImageExtension(pictureUri,mAct)||
  		    (Util.isEmptyString(pictureUri)&& 
  		     Util.isEmptyString(audioUri)&& 
  		     Util.isEmptyString(linkUri)      )             ) // for wrong path icon
  		{
			System.out.println("Note_view_pager_adapter / _showPictureView / show image view");
  			videoView.setVisibility(View.GONE);
  			linkWebView.setVisibility(View.GONE);
  			UtilVideo.mVideoView = null;
  			imageView.setVisibility(View.VISIBLE);
  			Note_view.showImageByTouchImageView(spinner, imageView, pictureUri);
  		}
  		// show video view
  		else if(UtilVideo.hasVideoExtension(pictureUri, mAct))
  		{
			System.out.println("Note_view_pager_adapter / _showPictureView / show video view");
  			linkWebView.setVisibility(View.GONE);
  			imageView.setVisibility(View.GONE);
  			videoView.setVisibility(View.VISIBLE);
  		}
  		// show audio thumb nail view
  		else if(Util.isEmptyString(pictureUri)&& 
  				!Util.isEmptyString(audioUri)    )
  		{
			System.out.println("Note_view_pager_adapter / _showPictureView / show audio thumb nail view");
  			videoView.setVisibility(View.GONE);
  			UtilVideo.mVideoView = null;
  			linkWebView.setVisibility(View.GONE);
  			imageView.setVisibility(View.VISIBLE);
  			try
			{
			    AsyncTaskAudioBitmap audioAsyncTask;
			    audioAsyncTask = new AsyncTaskAudioBitmap(NoteFragment.mAct,
						    							  audioUri, 
						    							  imageView,
						    							  null, //??? set this has unknown bug
														  false);
				audioAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Searching media ...");
			}
			catch(Exception e)
			{
				System.out.println("Note_view_pager_adapter / _AsyncTaskAudioBitmap / exception");
			}
  		}
  		// show link thumb view
  		else if(Util.isEmptyString(pictureUri)&&
  				Util.isEmptyString(audioUri)  &&
  				!Util.isEmptyString(linkUri))
  		{
			System.out.println("Note_view_pager_adapter / _showPictureView / show link thumb view");
  			videoView.setVisibility(View.GONE);
  			UtilVideo.mVideoView = null;
  			imageView.setVisibility(View.GONE);	
  			linkWebView.setVisibility(View.VISIBLE);
  		}
		else
			System.out.println("Note_view_pager_adapter / _showPictureView / show none");
    }
    
    
    // Add for FragmentStatePagerAdapter
    @Override
	public Fragment getItem(int arg0) {
        return null;
	}
    
    // Add for calling mPagerAdapter.notifyDataSetChanged() 
    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
    
	@Override
    public int getCount() 
    {
		if(Note_view.mDb != null)
			return Note_view.mDb.getNotesCount(true);
		else
			return 0;
    	//08-26 14:42:00.230: E/AndroidRuntime(13936): android.database.CursorWindowAllocationException: Cursor window allocation of 2048 kb failed. # Open Cursors=612 (# cursors opened by this proc=612)
    	//11-18 17:31:00.594: E/AndroidRuntime(7372): java.lang.RuntimeException: Unable to start activity ComponentInfo{com.cwc.litenote.alpha/com.cwc.litenote.note.Note_view_pager}: java.lang.NullPointerException
    	//12-11 21:56:00.458: E/AndroidRuntime(15089): Caused by: java.lang.NullPointerException: Attempt to invoke virtual method 'int com.cwc.litenote.db.DB.getNotesCount(boolean)' on a null object reference
    }

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view.equals(object);
	}
	
	static Intent mIntentView;
	
	@Override
	public void setPrimaryItem(final ViewGroup container, int position, Object object) 
	{
		// set primary item only
	    if(mLastPosition != position)
		{
			System.out.println("Note_view_pager_adapter / _setPrimaryItem / mLastPosition = " + mLastPosition);
            System.out.println("Note_view_pager_adapter / _setPrimaryItem / position = " + position);

			String lastPictureStr = null;
			String lastLinkUri = null;
			String lastAudioUri = null;

			if(mLastPosition != -1)
			{
				lastPictureStr = Note_view.mDb.getNotePictureUri(mLastPosition,true);
				lastLinkUri = Note_view.mDb.getNoteLinkUri(mLastPosition, true);
				lastAudioUri = Note_view.mDb.getNoteAudioUri(mLastPosition, true);
			}

			String pictureStr = Note_view.mDb.getNotePictureUri(position,true);
			String linkUri = Note_view.mDb.getNoteLinkUri(position,true);
			String audioUri = Note_view.mDb.getNoteAudioUri(position,true);

			// remove last text web view
			if (!Note_view.isPictureMode())
			{
				String tag = "current" + mLastPosition + "textWebView";
				CustomWebView textWebView = (CustomWebView) Note_view.mPager.findViewWithTag(tag);
				if (textWebView != null) {
					textWebView.onPause();
					textWebView.onResume();
				}
			}

			// for web view
			if (!UtilImage.hasImageExtension(pictureStr, mAct) &&
				!UtilVideo.hasVideoExtension(pictureStr, mAct)   )
			{
				// remove last link web view
				if(	!UtilImage.hasImageExtension(lastPictureStr, mAct) &&
					!UtilVideo.hasVideoExtension(lastPictureStr, mAct) &&
					!UtilAudio.hasAudioExtension(lastAudioUri)         &&
					!Util.isYouTubeLink(lastLinkUri)                      )
				{
					String tag = "current" + mLastPosition + "linkWebView";
					CustomWebView lastLinkWebView = (CustomWebView) Note_view.mPager.findViewWithTag(tag);

					if (lastLinkWebView != null)
					{
						CustomWebView.pauseWebView(lastLinkWebView);
						CustomWebView.blankWebView(lastLinkWebView);//??? if last page has image/video, this line will blank it
					}
				}

				// set current link web view in case no picture Uri
				if (  Util.isEmptyString(pictureStr) &&
					 !Util.isYouTubeLink(linkUri) &&
					  linkUri.startsWith("http") &&
					 !Note_view.isTextMode()      )
				{
					if(Note_view.isViewAllMode() )
					{
						String tagStr = "current" + position + "linkWebView";
						CustomWebView linkWebView = (CustomWebView) Note_view.mPager.findViewWithTag(tagStr);
						linkWebView.setVisibility(View.VISIBLE);
                        setWebView(linkWebView,object,CustomWebView.LINK_VIEW);
						System.out.println("Note_view_pager_adapter / _setPrimaryItem / load linkUri = " + linkUri);
						linkWebView.loadUrl(linkUri);

						//Add for non-stop showing of full screen web view
						linkWebView.setWebViewClient(new WebViewClient() {
							@Override
							public boolean shouldOverrideUrlLoading(WebView view, String url)
							{
								view.loadUrl(url);
								return true;
							}
						});

					}
					else if(Note_view.isPictureMode())
					{
//						Intent i = new Intent(Intent.ACTION_VIEW);
//						i.setData(Uri.parse(linkUri));
//						i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//						mAct.startActivityForResult(i,VIEW_WEB_LINK);

                        Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(linkUri));
                        mAct.startActivityForResult(i, VIEW_WEB_LINK);
                    }
				}
			}

			// for video view
			if (!Note_view.isTextMode() )
			{

				// stop last video view running
				if (mLastPosition != -1)
				{
					String tagVideoStr = "current" + mLastPosition + "videoView";
					VideoViewCustom lastVideoView = (VideoViewCustom) Note_view.mPager.findViewWithTag(tagVideoStr);
					lastVideoView.stopPlayback();
				}

				// Set video view
				if ( UtilVideo.hasVideoExtension(pictureStr, mAct) &&
					 !UtilImage.hasImageExtension(pictureStr, mAct)   )
				{
					// update current pager view
					UtilVideo.mCurrentPagerView = (View) object;

					// for view mode change
					if (Note_view.mIsViewModeChanged && (Note_view.mPlayVideoPositionOfInstance == 0) )
					{
						UtilVideo.mPlayVideoPosition = Note_view.mPositionOfChangeView;
						UtilVideo.setVideoViewLayout(pictureStr);

						if (!UtilVideo.hasMediaControlWidget)
							UtilVideo.setVideoViewUI();

						if (UtilVideo.mPlayVideoPosition > 0)
							UtilVideo.playOrPauseVideo(Note_view.getCurrentPictureString());
					}
					else
					{
						// for key protect
						if (Note_view.mPlayVideoPositionOfInstance > 0)
						{
							UtilVideo.setVideoState(UtilVideo.VIDEO_AT_PAUSE);
							UtilVideo.setVideoViewLayout(pictureStr);

							if (!UtilVideo.hasMediaControlWidget)
								UtilVideo.setVideoViewUI();

							UtilVideo.playOrPauseVideo(Note_view.getCurrentPictureString());
						}
						else
						{
							if (UtilVideo.hasMediaControlWidget)
								UtilVideo.setVideoState(UtilVideo.VIDEO_AT_PLAY);
							else
								UtilVideo.setVideoState(UtilVideo.VIDEO_AT_STOP);

							UtilVideo.mPlayVideoPosition = 0; // make sure play video position is 0 after page is changed
							UtilVideo.initVideoView(pictureStr, mAct);
						}
					}

					Note_view_pagerUI.showPictureViewUI(position);
					UtilVideo.currentPicturePath = pictureStr;
				}
			}

			// init audio block of pager
			if(UtilAudio.hasAudioExtension(audioUri))
			{
				Note_view.mAudioBlock.setVisibility(View.VISIBLE);
				Note_view.setAudioBlockListener();
				Note_view.initAudioProgress(audioUri);

				if(AudioPlayer.mAudioPlayMode == AudioPlayer.ONE_TIME_MODE)
				{
					if (AudioPlayer.mPlayerState != AudioPlayer.PLAYER_AT_STOP)
						Note_view.primaryAudioSeekBarProgressUpdater();
				}

//				if(AudioPlayer.mMediaPlayer != null)
					Note_view.updateAudioPlayingState();
			}
			else
				Note_view.mAudioBlock.setVisibility(View.GONE);
		}
	    mLastPosition = position;
	    
	} //setPrimaryItem		

	// Set web view
    static boolean bWebViewIsShown;
//	static void setWebView(final CustomWebView webView,ProgressBar spinner, int whichView)
	static void setWebView(final CustomWebView webView,Object object, int whichView)
	{
        final SharedPreferences pref_web_view = mAct.getSharedPreferences("web_view", 0);
		final ProgressBar spinner = (ProgressBar) ((View)object).findViewById(R.id.loading);
        if( whichView == CustomWebView.TEXT_VIEW )
        {
            int scale = pref_web_view.getInt("KEY_WEB_VIEW_SCALE",0);
            webView.setInitialScale(scale);
        }
        else if( whichView == CustomWebView.LINK_VIEW )
        {
            bWebViewIsShown = false;
            webView.setInitialScale(30);
        }

        int style = Note_view.getStyle();
		webView.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

    	webView.getSettings().setBuiltInZoomControls(true);
    	webView.getSettings().setSupportZoom(true);
    	webView.getSettings().setUseWideViewPort(true);
//    	customWebView.getSettings().setLoadWithOverviewMode(true);
    	webView.getSettings().setJavaScriptEnabled(true);//Using setJavaScriptEnabled can introduce XSS vulnerabilities

		// speed up
		if (Build.VERSION.SDK_INT >= 19) {
			// chromium, enable hardware acceleration
			webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		} else {
			// older android version, disable hardware acceleration
			webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}


//        if( (whichView == CustomWebView.LINK_VIEW) ||
//                (whichView == CustomWebView.TEXT_VIEW)   )
        if( whichView == CustomWebView.TEXT_VIEW )
   		{
	    	webView.setWebViewClient(new WebViewClient()
	        {
	            @Override
	            public void onScaleChanged(WebView web_view, float oldScale, float newScale)
	            {
	                super.onScaleChanged(web_view, oldScale, newScale);
	//                System.out.println("Note_view_pager / onScaleChanged");
	//                System.out.println("    oldScale = " + oldScale);
	//                System.out.println("    newScale = " + newScale);

	                int newDefaultScale = (int) (newScale*100);
	                pref_web_view.edit().putInt("KEY_WEB_VIEW_SCALE",newDefaultScale).commit();

	                //update current position
	                Note_view.mCurrentPosition = Note_view.mPager.getCurrentItem();
	            }

	            @Override
	            public void onPageFinished(WebView view, String url) {}
	        });

   		}
	    
    	if(whichView == CustomWebView.LINK_VIEW)
    	{
	        webView.setWebChromeClient(new WebChromeClient()
	        {
	            public void onProgressChanged(WebView view, int progress)
	            {
                    System.out.println("---------------- spinner progress = " + progress);

                    if(spinner != null )
	            	{
						if(bWebViewIsShown == false)
						{
							if (progress < 100 && (spinner.getVisibility() == ProgressBar.GONE)) {
								webView.setVisibility(View.GONE);
								spinner.setVisibility(ProgressBar.VISIBLE);
							}

							spinner.setProgress(progress);

							if (progress > 30)
								bWebViewIsShown = true;
						}

						if((bWebViewIsShown == true) || (progress == 100))
						{
							spinner.setVisibility(ProgressBar.GONE);
							webView.setVisibility(View.VISIBLE);
						}
	            	}
	            }

	            @Override
			    public void onReceivedTitle(WebView view, String title) {
			        super.onReceivedTitle(view, title);
			        if (!TextUtils.isEmpty(title) &&
			        	!title.equalsIgnoreCase("about:blank"))
			        {
			        	System.out.println("Note_view_pager_adapter / _onReceivedTitle / title = " + title);

						int position = Note_view.mCurrentPosition;
				    	String tag = "current"+position+"textWebView";
				    	CustomWebView textWebView = (CustomWebView) Note_view.mPager.findViewWithTag(tag);

				    	DB mDb = Note_view.mDb;
				    	String strLink = mDb.getNoteLinkUri(position,true);

						// show title of http link
				    	if((textWebView != null) &&
				    	    !Util.isYouTubeLink(strLink) &&
				    	    strLink.startsWith("http")        )
			        	{
				        	mWebTitle = title;
		        			showTextWebView(position,textWebView);
			        	}
			        }
			    }
			});
    	}
	}

    static int VIEW_PORT_BY_NONE = 0;
    static int VIEW_PORT_BY_DEVICE_WIDTH = 1;
    static int VIEW_PORT_BY_SCREEN_WIDTH = 2; 
    
    // Get HTML string with view port
    static String getHtmlStringWithViewPort(int position, int viewPort)
    {
    	DB mDb = Note_view.mDb;
    	int mStyle = Note_view.mStyle;
    	
    	System.out.println("Note_view_pager_adapter / _getHTMLstringWithViewPort");
    	String strTitle = mDb.getNoteTitle(position,true);
    	String strBody = mDb.getNoteBody(position,true);
    	String audioUri = mDb.getNoteAudioUri(position,true);
    	String linkUri = mDb.getNoteLinkUri(position,true);

    	// replace note title 
    	if(Util.isEmptyString(strTitle))
    	{
    		// with web title
    		if( Util.isEmptyString(audioUri) &&
    		   !Util.isYouTubeLink(linkUri)  &&
    		   !Util.isEmptyString(mWebTitle)  )
    	   	{
    		   strTitle = mWebTitle;
    	   	}
    	   	// with YouTube title
	   		else if(Util.isYouTubeLink(linkUri))
				strTitle = Util.getYoutubeTitle(linkUri);
    	}
    	
    	Long createTime = mDb.getNoteCreatedTime(position,true);
    	String head = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"+
		       	  	  "<html><head>" +
	  		       	  "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />";
    	
    	if(viewPort == VIEW_PORT_BY_NONE)
    	{
	    	head = head + "<head>";
    	}
    	else if(viewPort == VIEW_PORT_BY_DEVICE_WIDTH)
    	{
	    	head = head + 
	    		   "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
	     	  	   "<head>";
    	}
    	else if(viewPort == VIEW_PORT_BY_SCREEN_WIDTH)
    	{
//        	int screen_width = UtilImage.getScreenWidth(mAct);
        	int screen_width = 640;
	    	head = head +
	    		   "<meta name=\"viewport\" content=\"width=" + String.valueOf(screen_width) + ", initial-scale=1\">"+
   	  			   "<head>";
    	}
    		
       	String separatedLineTitle = (!Util.isEmptyString(strTitle) == true)?"<hr size=2 color=blue width=99% >":"";
       	String separatedLineBody = (!Util.isEmptyString(strBody) == true)?"<hr size=1 color=black width=99% >":"";

       	// title
       	if(!Util.isEmptyString(strTitle))
       	{
       		Spannable spanTitle = new SpannableString(strTitle);
       		Linkify.addLinks(spanTitle, Linkify.ALL);
       		spanTitle.setSpan(new AlignmentSpan.Standard(Alignment.ALIGN_CENTER), 
       							0,
       							spanTitle.length(), 
       							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
       		strTitle = Html.toHtml(spanTitle);
       	}
       	else
       		strTitle = "";
    	
    	// body
       	if(!Util.isEmptyString(strBody))
       	{
	    	Spannable spanBody = new SpannableString(strBody);
	    	Linkify.addLinks(spanBody, Linkify.ALL);
	    	strBody = Html.toHtml(spanBody);
       	}
       	else
       		strBody = "";
	    	
    	// set web view text color
    	String colorStr = Integer.toHexString(ColorSet.mText_ColorArray[mStyle]);
    	colorStr = colorStr.substring(2);
    	
    	String bgColorStr = Integer.toHexString(ColorSet.mBG_ColorArray[mStyle]);
    	bgColorStr = bgColorStr.substring(2);
    	
    	String content = head + "<body color=\"" + bgColorStr + "\">" +
		         "<p align=\"center\"><b>" + 
				 "<font color=\"" + colorStr + "\">" + strTitle + "</font>" + 
         		 "</b></p>" + separatedLineTitle +
		         "<p>" + 
				 "<font color=\"" + colorStr + "\">" + strBody + "</font>" +
				 "</p>" + separatedLineBody +
		         "<p align=\"right\">" + 
				 "<font color=\"" + colorStr + "\">"  + Util.getTimeString(createTime) + "</font>" +
		         "</p>" + 
		         "</body></html>";
		return content;
    }	
	
}//class Note_view_pager_adapter extends PagerAdapter