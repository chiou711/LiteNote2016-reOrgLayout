package com.cw.litenote.preference;

import android.app.Activity;

import com.cw.litenote.R;

/**
 * Created by CW on 2016/6/16.
 *
 * build apk file size:
 * 1) perfer w/ assets files: 15,483 KB
 *
 * 2) default w/ assets files: 15,483 KB
 *
 * 3) default w/o assets files: 1,173 KB
 *
 * 4) release: 706 KB
 */
public class Define {

    static boolean DEBUG_MODE = false;
    public static boolean RELEASE_MODE = !DEBUG_MODE;

    /**
     * Set release/debug mode
     * - RELEASE_MODE
     * - DEBUG_MODE
     */
    public static boolean CODE_MODE = DEBUG_MODE;

    /**
     * Has preferred table
     * - true: need to add preferred/assets/ in build.gradle
     * - false: need to remove preferred/assets/ in build.gradle
     */
    public static boolean HAS_PREFERRED_TABLES = false;

    // Apply system default for picture path
    public static boolean PICTURE_PATH_BY_SYSTEM_DEFAULT = true;

    // default table count
    public static int ORIGIN_NOTES_TABLE_COUNT = 1;//5; // Notes1_1, Notes1_2, Notes1_3, Notes1_4, Notes1_5
    public static int ORIGIN_TABS_TABLE_COUNT = 2;  // Tabs1, Tabs2, Tabs3

    // default style
    public static int STYLE_DEFAULT = 1;
    public static int STYLE_PREFER = 2;

    public static String getDrawerTitle(Activity act,Integer i)
    {
        String title = null;
        if(Define.HAS_PREFERRED_TABLES) {
            if (i == 0)
                title = act.getResources().getString(R.string.prefer_folder_name_local);
            else if (i == 1)
                title = act.getResources().getString(R.string.prefer_folder_name_web);
        }
        else {
            title = act.getResources().getString(R.string.default_folder_name).concat(String.valueOf(i+1));
        }
        return title;
    }

    public static String getTabTitle(Activity act,Integer Id)
    {
        String title;

        if(Define.HAS_PREFERRED_TABLES) {
            title = act.getResources().getString(R.string.prefer_page_name).concat(String.valueOf(Id));
        }
        else {
            title = act.getResources().getString(R.string.default_page_name).concat(String.valueOf(Id));
        }
        return title;
    }
}
