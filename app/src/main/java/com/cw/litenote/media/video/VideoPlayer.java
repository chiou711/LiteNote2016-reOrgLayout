package com.cw.litenote.media.video;

import java.io.IOException;

import com.cw.litenote.note.Note_view;
import com.cw.litenote.note.Note_view_pagerUI;
import com.cw.litenote.util.Util;

import android.app.Activity;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;

public class VideoPlayer 
{
	private static final String TAG_VIDEO = "VIDEO_PLAYER"; // error logging tag
	static final int DURATION_1S = 1000; // 1000 = 1 second

	static Activity mFragAct;
	public static Handler mVideoHandler;
	static String mPlayingPath;
   
	public VideoPlayer(String picString, FragmentActivity fAct)  
	{
		mFragAct = fAct;
		mPlayingPath = picString;

		if(UtilVideo.getVideoState() == UtilVideo.VIDEO_AT_PLAY) 
		{
			if(UtilVideo.mPlayVideoPosition == 0)
				startVideo();
			else if(UtilVideo.mPlayVideoPosition > 0)
				goOnVideo();
		}
		else if(UtilVideo.getVideoState() == UtilVideo.VIDEO_AT_PAUSE)
		{
			goOnVideo();
		}
	};

	
	static void startVideo()
	{
		System.out.println("VideoPlayer / _startVideo");
		// remove call backs to make sure next toast will appear soon
		if(mVideoHandler != null)
		{
			mVideoHandler.removeCallbacks(mRunPlayVideo); 
		}
		
		// start a new handler
		mVideoHandler = new Handler();
		
		if(UtilVideo.mVideoView != null)
		{
			if(!UtilVideo.hasMediaControlWidget)
			{
				UtilVideo.setVideoPlayerListeners();
			}
			else
			{
				setMediaController();
			}
		}
		mVideoHandler.post(mRunPlayVideo);
	}
	
	public static void stopVideo()
	{
		System.out.println("VideoPlayer / _stopVideo");
		
		if(mVideoHandler != null)
		{
			mVideoHandler.removeCallbacks(mRunPlayVideo); 
		}
		
		if((UtilVideo.mVideoView != null) && UtilVideo.mVideoView.isPlaying())
		{
			UtilVideo.mVideoView.stopPlayback();
		}

		UtilVideo.mVideoView = null;
		UtilVideo.mVideoPlayer = null;

		AsyncTaskVideoBitmapPager.mVideoUrl = null;
		UtilVideo.setVideoState(UtilVideo.VIDEO_AT_STOP);
	}	
	
	public void goOnVideo()
	{
		System.out.println("VideoPlayer / _goOnVideo");
		if(UtilVideo.hasMediaControlWidget)
			setMediaController();
		mVideoHandler.post(mRunPlayVideo);
	}
	
	//
	// Runnable for play video
	//
	public static Runnable mRunPlayVideo = new Runnable()
	{   @Override
		public void run()
		{
			// for remote video
		    String path = AsyncTaskVideoBitmapPager.mVideoUrl;
		    if(Util.isEmptyString(path))
		    	path = mPlayingPath;
		    	
//		    System.out.println("VideoPlayer / mRunPlayVideo / path = " + path);		    	
			if(UtilVideo.mVideoView != null)
			{	
				try 
				{
					if(Util.isEmptyString(path) || (Note_view_pagerUI.videoFileLength_inMilliSeconds ==0))
						Toast.makeText(mFragAct, "Video file URL/path is empty or video file is not playable",Toast.LENGTH_LONG).show();
					else 
					{
						// for key protect
						if(Note_view.mPlayVideoPositionOfInstance > 0)
							UtilVideo.mPlayVideoPosition = Note_view.mPlayVideoPositionOfInstance;
						// for view mode change
						else if(Note_view.mIsViewModeChanged)
							UtilVideo.mPlayVideoPosition = Note_view.mPositionOfChangeView;
						else
							UtilVideo.mPlayVideoPosition = UtilVideo.mVideoView.getCurrentPosition();

						System.out.println("VideoPlayer / mRunPlayVideo/ UtilVideo.mPlayVideoPosition = " + UtilVideo.mPlayVideoPosition);
						
						// start processing video view
						processVideoView(path);
						
						//
						if(!UtilVideo.hasMediaControlWidget)
						{
							if( Note_view_pagerUI.showSeekBarProgress /*&&
							    Note_view_pagerUI.isWithinDelay*/          )
							{
								Note_view_pagerUI.primaryVideoSeekBarProgressUpdater(UtilVideo.mPlayVideoPosition);
							}
								
							// final play
							int diff = Math.abs(UtilVideo.mPlayVideoPosition - Note_view_pagerUI.videoFileLength_inMilliSeconds);
							if( diff  <= 1000)
							{	
								System.out.println("VideoPlayer / mRunPlayVideo/ final play");
//								Note_view.mPlayVideoPositionOfInstance = 1; // for Pause at start
								UtilVideo.setVideoState(UtilVideo.VIDEO_AT_PAUSE);//
								mVideoHandler.postDelayed(mRunPlayVideo,DURATION_1S);
								//will call: UtilVideo / _setOnCompletionListener / _onCompletion
							}
							
							// delay and execute runnable
							if(UtilVideo.getVideoState() == UtilVideo.VIDEO_AT_PLAY)
								mVideoHandler.postDelayed(mRunPlayVideo,DURATION_1S);							
						}
					}
				} 
				catch (Exception e) 
				{
					Log.e(TAG_VIDEO, "VideoPlayer / mRunPlayVideo / error: " + e.getMessage(), e);
					VideoPlayer.stopVideo();
				}
			}
		} 
	};

	static int mCount = 0;
	// process video view
	static void processVideoView(String path) throws IOException
	{
		int state = UtilVideo.getVideoState();

		mCount++;
        String prefix = "VideoPlayer / _processVideoView / to state = ";
		if(state == 1) {
			System.out.println(prefix + "VIDEO_AT_STOP");
			mCount = 0;
		}
		else if(state == 2)
			System.out.println(prefix + "VIDEO_AT_PLAY " + mCount);
		else if(state == 3) {
			System.out.println(prefix + "VIDEO_AT_PAUSE");
			mCount = 0;
		}


		// To Play state
		if(state ==  UtilVideo.VIDEO_AT_PLAY)     
		{
			// after view mode is changed
			if(Note_view.mIsViewModeChanged)
			{
				System.out.println("VideoPlayer / _processVideoView/ isViewModeChanged/ to Play");
				UtilVideo.currentPicturePath = path;
				UtilVideo.mVideoView.setVideoPath(UtilVideo.getVideoDataSource(path));
				UtilVideo.mVideoView.seekTo(UtilVideo.mPlayVideoPosition);
				UtilVideo.mVideoView.start();
				UtilVideo.mVideoView.requestFocus();

				Note_view.mIsViewModeChanged = false;
			}
			// from Stop to Play
			else if(UtilVideo.mPlayVideoPosition == 0) 
			{
				System.out.println("VideoPlayer / _processVideoView/ from Stop to Play: normal start video");
				UtilVideo.currentPicturePath = path;
				UtilVideo.mVideoView.setVideoPath(UtilVideo.getVideoDataSource(path));
                UtilVideo.mVideoView.start();
				UtilVideo.mVideoView.requestFocus();

				if(!UtilVideo.hasMediaControlWidget)
					UtilVideo.updateVideoPlayButtonState();
			}
			// from Pause to Play
			else if((UtilVideo.mPlayVideoPosition > 0) &&
				     path.equals(UtilVideo.currentPicturePath) &&
				     !UtilVideo.mVideoView.isPlaying()            )
			{
                System.out.println("VideoPlayer / _processVideoView / from Pause to Play");
				UtilVideo.mVideoView.start();
				UtilVideo.mVideoView.requestFocus();

				if(!UtilVideo.hasMediaControlWidget)
					UtilVideo.updateVideoPlayButtonState();
			}
		}
		
		// To Pause state
		if(state ==  UtilVideo.VIDEO_AT_PAUSE)     
		{
			if(Note_view.mIsViewModeChanged || (Note_view.mPlayVideoPositionOfInstance > 0) )
			{
                System.out.println("VideoPlayer / mRunPlayVideo/ mIsViewModeChanged / to Pause");
				UtilVideo.currentPicturePath = path;
				UtilVideo.mVideoView.setVideoPath(UtilVideo.getVideoDataSource(path));
				UtilVideo.mVideoView.seekTo(UtilVideo.mPlayVideoPosition);
				UtilVideo.mVideoView.pause();
				UtilVideo.mVideoView.requestFocus();

				// reset
				Note_view.mIsViewModeChanged = false;
				Note_view.mPlayVideoPositionOfInstance = 0;
			}
			// from Play to Pause
			else if(UtilVideo.mVideoView.isPlaying())
			{
                System.out.println("VideoPlayer / mRunPlayVideo/ from Play to Pause");
				UtilVideo.mVideoView.pause();
				UtilVideo.mVideoView.requestFocus();
				if(!UtilVideo.hasMediaControlWidget)
					UtilVideo.updateVideoPlayButtonState();	
			}
			// keep pausing
			else if(!UtilVideo.mVideoView.isPlaying())
			{
				// do nothing
			}
		}		
	}
	
	static MediaController mMediaController;
	// set media controller
	static void setMediaController()
	{
		System.out.println("VideoPlayer / _setMediaController");
		//MediaController
		mMediaController = new MediaController(mFragAct);
		mMediaController.setVisibility(View.VISIBLE);
		mMediaController.setAnchorView(UtilVideo.mVideoView);
		UtilVideo.mVideoView.setMediaController(mMediaController);			
		
		mMediaController.setPrevNextListeners(
		    // for next
			new View.OnClickListener() 
			{
				public void onClick(View v) 
				{
	            	Note_view.mCurrentPosition++;
	            	
	            	if(Note_view.mCurrentPosition >= Note_view.mPagerAdapter.getCount())
	            		Note_view.mCurrentPosition--;
	            	else
	            		Note_view.mPager.setCurrentItem(Note_view.mPager.getCurrentItem() + 1);
				}
			}, 
			// for previous
			new View.OnClickListener() 
			{
				public void onClick(View v) 
				{
					Note_view.mCurrentPosition--;
            	
	            	if(Note_view.mCurrentPosition <0 )
	            		Note_view.mCurrentPosition++;
	            	else
	            		Note_view.mPager.setCurrentItem(Note_view.mPager.getCurrentItem() - 1);
				}
			});			
	}
	
	public static void cancelMediaController()
	{
		if(mMediaController != null)
		{
			mMediaController.cancelPendingInputEvents();
			mMediaController = null;
		}
	}
}