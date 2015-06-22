package com.nuvyyo.cordova.plugins.tablovideoplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;

public class PlayerAPK {
    private static final String TAG = "PlayerAPK";
    
    private static final String VIDEO_PLAYER_PACKAGE = "com.nuvyyo.android.slipstream.exovideoplayer";
    private static final String PREFERENCES_NAME     = "com.nuvyyo.android.slipstream.PlayerAPK";
    
    private static final String KEY_PROTECTED        = "PROTECTED";
    
    /**
     * Indicates if the player apk has been "protected". This will be true if the 
     * user has indicated they do not want the player apk to be removed (i.e. selecting 
     * "cancel" on the uninstallation prompt).
     * 
     * Once the player apk has been marked "protected" the app should no longer 
     * attempt to uninstall it.
     * 
     * @return <code>true</code> if the player apk should _not_ be uninstalled.
     */
    public static boolean isProtected(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PROTECTED, null) != null;
    }
    
    /**
     * Sets the player apk as protected or not protected. See {@link isProtected} 
     * for more details on behaviour when protected.
     * 
     * @param context
     */
    public static void setProtected(Context context, boolean isProtected) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        
        Editor editor = prefs.edit();
        if( isProtected )
            editor.putString(KEY_PROTECTED, "protected");
        else
            editor.remove(KEY_PROTECTED);
        
        editor.commit();
    }
        
    public static boolean isInstalled(final Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(VIDEO_PLAYER_PACKAGE, PackageManager.GET_ACTIVITIES);
            return true;
            
        } catch (NameNotFoundException e) {
            return false;
        }
    }
    
    public static void uninstallPlayerAPK(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("Tablo Video Player");
        builder.setMessage("Tablo Video Player is no longer required as a separate application. " +
        		"You will now be prompted to uninstall it.");

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                performUninstall(context);
            }
            
        });
        builder.show();
    }
    
    private static void performUninstall(final Context context) {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        
        intent.setData(Uri.parse("package:"+VIDEO_PLAYER_PACKAGE));
        context.startActivity(intent);
    }
    
    public static void showCompleteDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("Tablo Legacy Player Removed");
        builder.setMessage("If you had previously enabled installation from 'unknown sources' you may " +
        		"now disable it without affecting Tablo.");

        builder.setPositiveButton("Go to Settings", new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                context.startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS));
            }
            
        });
        builder.setNegativeButton("Later", new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
            
        });
        builder.show();
    }
}