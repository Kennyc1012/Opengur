package com.kenny.openimgur.util;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by kcampagna on 9/2/15.
 */
public class PermissionUtils {
    /**
     * The user has denied the permission before
     */
    public static final int PERMISSION_DENIED = -2;

    /**
     * The user has never been asked to accept the permission
     */
    public static final int PERMISSION_NEVER_ASKED = -1;

    /**
     * The user has accepted the permission
     */
    public static final int PERMISSION_AVAILABLE = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PERMISSION_AVAILABLE, PERMISSION_NEVER_ASKED, PERMISSION_DENIED})
    public @interface PermissionLevel {
    }

    /**
     * Check that all given permissions have been granted by verifying that each entry in the
     * given array is of the value {@link PackageManager#PERMISSION_GRANTED}.
     *
     * @param grantResults The results of a permission grant
     * @see {@link Activity#onRequestPermissionsResult(int, String[], int[])}
     */
    public static boolean verifyPermissions(int[] grantResults) {
        // At least one result must be checked.
        if (grantResults == null || grantResults.length < 1) {
            return false;
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks an array of permissions and returns and array containing their statuses of either<br/>
     * {@link PackageManager#PERMISSION_GRANTED} or {@link PackageManager#PERMISSION_DENIED}
     *
     * @param context     Application context
     * @param permissions Array of permissions to check
     * @return Array containing the status of the supplied permissions. Will return null if the permissions are empty
     */
    @Nullable
    public static int[] getGrantResults(@NonNull Context context, String[] permissions) {
        if (permissions == null || permissions.length < 1) return null;

        int numPermissions = permissions.length;
        int[] grantResults = new int[numPermissions];

        for (int i = 0; i < numPermissions; i++) {
            grantResults[i] = ActivityCompat.checkSelfPermission(context, permissions[i]);
        }

        return grantResults;
    }

    /**
     * Returns the {@link PermissionLevel} the app has over the set of permissions
     *
     * @param fragment    Fragment requesting the permissions
     * @param permissions Permissions to get info about
     * @return
     */
    @PermissionLevel
    public static int getPermissionLevel(@NonNull Fragment fragment, String... permissions) {
        int[] grantResults = getGrantResults(fragment.getActivity(), permissions);
        boolean hasAllPermissions = verifyPermissions(grantResults);

        if (hasAllPermissions) {
            return PERMISSION_AVAILABLE;
        }

        for (String s : permissions) {
            if (FragmentCompat.shouldShowRequestPermissionRationale(fragment, s)) return PERMISSION_DENIED;
        }

        return PERMISSION_NEVER_ASKED;
    }

    /**
     * Returns the {@link PermissionLevel} the app has over the set of permissions
     *
     * @param activity    Activity requesting the permissions
     * @param permissions Permissions to get info about
     * @return
     */
    @PermissionLevel
    public static int getPermissionLevel(@NonNull Activity activity, String... permissions) {
        int[] grantResults = getGrantResults(activity, permissions);
        boolean hasAllPermissions = verifyPermissions(grantResults);

        if (hasAllPermissions) {
            return PERMISSION_AVAILABLE;
        }

        for (String s : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, s)) return PERMISSION_DENIED;
        }

        return PERMISSION_NEVER_ASKED;
    }

    /**
     * Returns if the user has granted the given permission
     *
     * @param activity
     * @param permission
     * @return
     */
    public static boolean hasPermission(Activity activity, @NonNull String permission) {
        return getPermissionLevel(activity, permission) == PERMISSION_AVAILABLE;
    }

    /**
     * Returns if the user has granted the given permission
     *
     * @param context
     * @param permissions
     * @return
     */
    public static boolean hasPermission(Context context, @NonNull String... permissions) {
        return verifyPermissions(getGrantResults(context, permissions));
    }
}
