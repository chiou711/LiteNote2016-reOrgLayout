package com.cw.litenote;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cw.litenote.media.audio.AudioPlayer;
import com.cw.litenote.media.audio.UtilAudio;
import com.cw.litenote.media.image.AsyncTaskAudioBitmap;
import com.cw.litenote.media.image.UtilImage;
import com.cw.litenote.media.image.UtilImage_bitmapLoader;
import com.cw.litenote.media.video.UtilVideo;
import com.cw.litenote.note.CustomWebView;
import com.cw.litenote.util.UilCommon;
import com.cw.litenote.util.Util;
import com.cw.litenote.util.ColorSet;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;

import java.util.Date;

public class NoteFragmentAdapter extends SimpleDragSortCursorAdapter 
{
	public NoteFragmentAdapter(Context context, int layout, Cursor c,
						String[] from, int[] to, int flags) 
	{
		super(context, layout, c, from, to, flags);
	}

	public class ViewHolder {
		public ImageView imageCheck;
		public TextView rowId;
		public View audioBlock;
		public ImageView imageAudio;
		public TextView audioName;
		public View textTitleBlock;
		public TextView textTitle;
		public View textBodyBlock;
		public TextView textBody;
		public TextView textTime;
		public ImageView imageDragger;
		public View thumbBlock;
		public ImageView thumbPicture;
		public ImageView thumbAudio;
		public CustomWebView thumbWeb;
		public ProgressBar progressBar;
	}
	
	@Override
	public int getCount() {
		int count = NoteFragment.mDb_notes.getNotesCount(true);
		return count;
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
//		System.out.println("NoteFragmentAdapter / _getView / position = " +  position);
		View view = convertView;
		final ViewHolder holder;
		
		if (convertView == null) 
		{
			view = NoteFragment.mAct.getLayoutInflater().inflate(R.layout.activity_main_list_view_row, parent, false);

			// set rectangular background
//				view.setBackgroundColor(Util.mBG_ColorArray[mStyle]);
			
			//set round corner and background color
    		switch(NoteFragment.mStyle)
    		{
    			case 0:
    				view.setBackgroundResource(R.drawable.bg_0);
    				break;
    			case 1:
    				view.setBackgroundResource(R.drawable.bg_1);
    				break;
    			case 2:
    				view.setBackgroundResource(R.drawable.bg_2);
    				break;
    			case 3:
    				view.setBackgroundResource(R.drawable.bg_3);
    				break;
    			case 4:
    				view.setBackgroundResource(R.drawable.bg_4);
    				break;
    			case 5:
    				view.setBackgroundResource(R.drawable.bg_5);
    				break;
    			case 6:
    				view.setBackgroundResource(R.drawable.bg_6);
    				break;
    			case 7:
    				view.setBackgroundResource(R.drawable.bg_7);
    				break;
    			case 8:
    				view.setBackgroundResource(R.drawable.bg_8);
    				break;
    			case 9:
    				view.setBackgroundResource(R.drawable.bg_9);
    				break;
    			default:
    				break;
    		}
    		
			holder = new ViewHolder();
			holder.rowId= (TextView) view.findViewById(R.id.row_id);
			holder.audioBlock = view.findViewById(R.id.audio_block);
			holder.imageAudio = (ImageView) view.findViewById(R.id.img_audio);
			holder.audioName = (TextView) view.findViewById(R.id.row_audio_name);
			holder.imageCheck= (ImageView) view.findViewById(R.id.img_check);
			holder.thumbBlock = view.findViewById(R.id.image_view_block);
			holder.thumbPicture = (ImageView) view.findViewById(R.id.image_view_thumb_picture);
			holder.thumbAudio = (ImageView) view.findViewById(R.id.image_view_thumb_audio);
			holder.thumbWeb = (CustomWebView) view.findViewById(R.id.image_view_thumb_web);
			holder.imageDragger = (ImageView) view.findViewById(R.id.img_dragger);
			holder.progressBar = (ProgressBar) view.findViewById(R.id.img_progress);
			holder.textTitleBlock = view.findViewById(R.id.row_title_block);
			holder.textTitle = (TextView) view.findViewById(R.id.row_title);
			holder.textBodyBlock = view.findViewById(R.id.row_body);
			holder.textBody = (TextView) view.findViewById(R.id.row_body_text_view);
			holder.textTime = (TextView) view.findViewById(R.id.row_time);
			view.setTag(holder);
		} 
		else 
		{
			holder = (ViewHolder) view.getTag();
		}
		
		// show row Id
		holder.rowId.setText(String.valueOf(position+1));
		holder.rowId.setTextColor(ColorSet.mText_ColorArray[NoteFragment.mStyle]);
		
		// show check box, title , picture
		String strTitle = NoteFragment.mDb_notes.getNoteTitle(position,true);
		String pictureUri = NoteFragment.mDb_notes.getNotePictureUri(position,true);
		String audioUri = NoteFragment.mDb_notes.getNoteAudioUri(position,true);
		String linkUri = NoteFragment.mDb_notes.getNoteLinkUri(position,true);
		
		if( Util.isEmptyString(strTitle) &&
			Util.isYouTubeLink(linkUri)     )
		{
			strTitle = Util.getYoutubeTitle(linkUri);

            // set title with YouTube link title
            if(NoteFragment.mPref_show_note_attribute
                           .getString("KEY_ENABLE_LINK_TITLE_SAVE", "yes")
                           .equalsIgnoreCase("yes"))
            {
                String strBody = NoteFragment.mDb_notes.getNoteBody(position, true);
                int marking = NoteFragment.mDb_notes.getNoteMarking(position, true);
                long rowId = NoteFragment.mDb_notes.getNoteId(position, true);
                boolean isOK;
                Date now = new Date();
                isOK = NoteFragment.mDb_notes.updateNote(rowId, strTitle, pictureUri, audioUri, "", linkUri, strBody, marking, now.getTime(), true); // update note
                System.out.println("NoteFragmentAdapter / update note of YouTube link / isOK = " + isOK);
            }
		}
		
		// set title
		holder.textTitleBlock.setVisibility(View.VISIBLE);
		holder.textTitle.setText(strTitle);
		holder.textTitle.setTextColor(ColorSet.mText_ColorArray[NoteFragment.mStyle]);

		// set audio name
		String audio_name = null;
		if(!Util.isEmptyString(audioUri))
			audio_name = Util.getDisplayNameByUriString(audioUri, NoteFragment.mAct);

		if(Util.isUriExisted(audioUri,NoteFragment.mAct))
			holder.audioName.setText(audio_name);
		else
			holder.audioName.setText(R.string.file_not_found);

		holder.audioName.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
//			holder.audioName.setTextSize(12.0f);
		
		// show audio highlight if audio is not at Stop
		if( MainUi.isSameNotesTable() &&
			(position == AudioPlayer.mAudioIndex)  &&
			(AudioPlayer.mMediaPlayer != null) &&
			(AudioPlayer.mPlayerState != AudioPlayer.PLAYER_AT_STOP) &&
			(AudioPlayer.mAudioPlayMode == AudioPlayer.CONTINUE_MODE))
		{
			NoteFragment.mHighlightPosition = position;
			holder.audioName.setTextColor(ColorSet.getHighlightColor(NoteFragment.mAct));
			holder.audioBlock.setBackgroundResource(R.drawable.bg_highlight_border);
			holder.audioBlock.setVisibility(View.VISIBLE);
			holder.imageAudio.setVisibility(View.VISIBLE);
			holder.imageAudio.setImageResource(R.drawable.ic_audio_selected);

			// set type face
			holder.audioName.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

			// set animation
			Animation animation = AnimationUtils.loadAnimation(mContext , R.anim.right_in);
			holder.audioBlock.startAnimation(animation);
		}
		else
		{
			if(!Util.isEmptyString(audioUri))
			{
//					holder.audioBlock.setBackgroundColor(Color.argb(0x80,0x80,0x80,0x80));
				holder.audioName.setTextColor(ColorSet.mText_ColorArray[NoteFragment.mStyle]);
			}
			holder.audioBlock.setBackgroundResource(R.drawable.bg_gray_border);
			holder.audioBlock.setVisibility(View.VISIBLE);
			holder.imageAudio.setVisibility(View.VISIBLE);
			holder.imageAudio.setImageResource(R.drawable.ic_audio_unselected);
		}
		
		// audio icon and block
		if(Util.isEmptyString(audioUri))
		{
			holder.imageAudio.setVisibility(View.INVISIBLE);
			holder.audioBlock.setVisibility(View.INVISIBLE);
		}
		
		
		// Show image thumb nail if picture Uri is none and YouTube link exists 
		if(Util.isEmptyString(pictureUri) &&
		   Util.isYouTubeLink(linkUri)      )
		{
			pictureUri = "http://img.youtube.com/vi/"+Util.getYoutubeId(linkUri)+"/0.jpg";
		}
//		System.out.println("NoteFragment_itemAdapter / _getView / pictureUri = " + pictureUri);
		
		// show thumb nail if picture Uri exists
		if(UtilImage.hasImageExtension(pictureUri,NoteFragment.mAct ) ||
		   UtilVideo.hasVideoExtension(pictureUri,NoteFragment.mAct )   )
		{
			holder.thumbBlock.setVisibility(View.VISIBLE);
			holder.thumbPicture.setVisibility(View.VISIBLE);
			holder.thumbAudio.setVisibility(View.GONE);
			holder.thumbWeb.setVisibility(View.GONE);
			// load bitmap to image view
			try
			{
				new UtilImage_bitmapLoader(holder.thumbPicture,
										   pictureUri,
										   holder.progressBar, 
										   (NoteFragment.mStyle % 2 == 1 ? 
											UilCommon.optionsForRounded_light: 
											UilCommon.optionsForRounded_dark),
										   NoteFragment.mAct);
			}
			catch(Exception e)
			{
				Log.e("NoteFragmentAdapter", "UtilImage_bitmapLoader error");
				holder.thumbBlock.setVisibility(View.GONE);
				holder.thumbPicture.setVisibility(View.GONE);
				holder.thumbAudio.setVisibility(View.GONE);
				holder.thumbWeb.setVisibility(View.GONE);
			}				
		}
		// show audio thumb nail if picture Uri is none and audio Uri exists
		else if((Util.isEmptyString(pictureUri) && UtilAudio.hasAudioExtension(audioUri) ) )
		{
			holder.thumbBlock.setVisibility(View.VISIBLE);
			holder.thumbPicture.setVisibility(View.GONE);
			holder.thumbAudio.setVisibility(View.VISIBLE);
			holder.thumbWeb.setVisibility(View.GONE);
			try
			{
			    AsyncTaskAudioBitmap audioAsyncTask;
			    audioAsyncTask = new AsyncTaskAudioBitmap(NoteFragment.mAct,
						    							  audioUri, 
						    							  holder.thumbAudio, 
						    							  holder.progressBar,
                                                          true);
				audioAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Searching media ...");
			}
			catch(Exception e)
			{
				Log.e("NoteFragmentAdapter", "AsyncTaskAudioBitmap error");
				holder.thumbBlock.setVisibility(View.GONE);
				holder.thumbPicture.setVisibility(View.GONE);
				holder.thumbAudio.setVisibility(View.GONE);
				holder.thumbWeb.setVisibility(View.GONE);
			}
		}
		// set web title and web view thumb nail of link if no title content
		else if(!Util.isEmptyString(linkUri) &&
                linkUri.startsWith("http")   &&
				!Util.isYouTubeLink(linkUri)   )
		{
			// reset web view
			CustomWebView.pauseWebView(holder.thumbWeb);
			CustomWebView.blankWebView(holder.thumbWeb);

			holder.thumbBlock.setVisibility(View.VISIBLE);
			holder.thumbWeb.setInitialScale(50);
			holder.thumbWeb.getSettings().setJavaScriptEnabled(true);//Using setJavaScriptEnabled can introduce XSS vulnerabilities
			holder.thumbWeb.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT );
            // speed up
            if (Build.VERSION.SDK_INT >= 19) {
                // chromium, enable hardware acceleration
                holder.thumbWeb.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else {
                // older android version, disable hardware acceleration
                holder.thumbWeb.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
			holder.thumbWeb.loadUrl(linkUri);
			holder.thumbWeb.setVisibility(View.VISIBLE);

			// no interactive response
			holder.thumbWeb.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return true;
				}
			});



			holder.thumbPicture.setVisibility(View.GONE);
			holder.thumbAudio.setVisibility(View.GONE);

			//Add for non-stop showing of full screen web view
			holder.thumbWeb.setWebViewClient(new WebViewClient() {
				@Override
			    public boolean shouldOverrideUrlLoading(WebView view, String url)
			    {
			        view.loadUrl(url);
			        return true;
			    }
			});


			if (Util.isEmptyString(strTitle)) {

				holder.thumbWeb.setWebChromeClient(new WebChromeClient() {
					@Override
					public void onReceivedTitle(WebView view, String title) {
						super.onReceivedTitle(view, title);
						if (!TextUtils.isEmpty(title) &&
								!title.equalsIgnoreCase("about:blank")) {
							holder.textTitleBlock.setVisibility(View.VISIBLE);
							holder.textTitle.setText(title);

							holder.rowId.setText(String.valueOf(position + 1));
							holder.rowId.setTextColor(ColorSet.mText_ColorArray[NoteFragment.mStyle]);

							// set title with http link title
							if (NoteFragment.mPref_show_note_attribute
									.getString("KEY_ENABLE_LINK_TITLE_SAVE", "yes")
									.equalsIgnoreCase("yes")) {
								String strTitle = NoteFragment.mDb_notes.getNoteTitle(position, true);
								if (Util.isEmptyString(strTitle))
									strTitle = title; // replaced with title got from link
								String strBody = NoteFragment.mDb_notes.getNoteBody(position, true);
								String pictureUri = NoteFragment.mDb_notes.getNotePictureUri(position, true);
								String audioUri = NoteFragment.mDb_notes.getNoteAudioUri(position, true);
								String linkUri = NoteFragment.mDb_notes.getNoteLinkUri(position, true);
								int marking = NoteFragment.mDb_notes.getNoteMarking(position, true);
								long rowId = NoteFragment.mDb_notes.getNoteId(position, true);

								boolean isOK;
								Date now = new Date();
								isOK = NoteFragment.mDb_notes.updateNote(rowId, strTitle, pictureUri, audioUri, "", linkUri, strBody, marking, now.getTime(), true); // update note
								System.out.println("NoteFragmentAdapter / onReceivedTitle / isOK = " + isOK);
							}
						}
					}
				});
			}
		}
		else
		{
			holder.thumbBlock.setVisibility(View.GONE);
			holder.thumbPicture.setVisibility(View.GONE);
			holder.thumbAudio.setVisibility(View.GONE);
			holder.thumbWeb.setVisibility(View.GONE);
		}

		
		// Show note body or not
		NoteFragment.mPref_show_note_attribute = NoteFragment.mAct.getSharedPreferences("show_note_attribute", 0);
	  	if(NoteFragment.mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
	  	{
	  		// test only: enabled for showing picture path
	  		String strBody = NoteFragment.mDb_notes.getNoteBody(position,true);
	  		if(!Util.isEmptyString(strBody))
	  		{}	//do nothing
	  		else if(!Util.isEmptyString(pictureUri))
	  			strBody = pictureUri;
	  		else if(!Util.isEmptyString(linkUri))
	  			strBody = linkUri;

			holder.textBody.setText(strBody);
//			holder.textBody.setTextSize(12);
	  		
	  		// time stamp
			holder.textBody.setTextColor(ColorSet.mText_ColorArray[NoteFragment.mStyle]);
			holder.textTime.setText(Util.getTimeString(NoteFragment.mDb_notes.getNoteCreatedTime(position,true)));
			holder.textTime.setTextColor(ColorSet.mText_ColorArray[NoteFragment.mStyle]);
	  	}
	  	else
	  	{
	  		holder.textBodyBlock.setVisibility(View.GONE);
	  	}			
		
		
	  	// dragger
	  	NoteFragment.mPref_show_note_attribute = NoteFragment.mAct.getSharedPreferences("show_note_attribute", 0);
	  	if(NoteFragment.mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "no").equalsIgnoreCase("yes"))
	  		holder.imageDragger.setVisibility(View.VISIBLE); 
	  	else
	  		holder.imageDragger.setVisibility(View.GONE); 
		
	  	// marking
		if( NoteFragment.mDb_notes.getNoteMarking(position, true) == 1)
			holder.imageCheck.setBackgroundResource(NoteFragment.mStyle%2 == 1 ?
	    			R.drawable.btn_check_on_holo_light:
	    			R.drawable.btn_check_on_holo_dark);	
		else
			holder.imageCheck.setBackgroundResource(NoteFragment.mStyle%2 == 1 ?
					R.drawable.btn_check_off_holo_light:
					R.drawable.btn_check_off_holo_dark);

		return view;
	}

}