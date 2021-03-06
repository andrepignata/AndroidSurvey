package org.adaptlab.chpir.android.survey;

import java.util.UUID;

import org.adaptlab.chpir.android.activerecordcloudsync.ActiveRecordCloudSync;
import org.adaptlab.chpir.android.activerecordcloudsync.PollService;
import org.adaptlab.chpir.android.survey.Models.AdminSettings;
import org.adaptlab.chpir.android.survey.Models.Instrument;
import org.adaptlab.chpir.android.survey.Models.Option;
import org.adaptlab.chpir.android.survey.Models.Question;
import org.adaptlab.chpir.android.survey.Models.Response;
import org.adaptlab.chpir.android.survey.Models.Survey;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import com.crashlytics.android.Crashlytics;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class AppUtil {
    private final static String TAG = "AppUtil";
    public final static boolean REQUIRE_SECURITY_CHECKS = false;
    
    public static String ADMIN_PASSWORD_HASH;
    public static String ACCESS_TOKEN;
    
    /*
     * Get the version code from the AndroidManifest
     */
    public static int getVersionCode(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (NameNotFoundException nnfe) {
            Log.e(TAG, "Error finding version code: " + nnfe);
        }
        return -1;
    }
    
    public static final void appInit(Context context) {
        if (AppUtil.REQUIRE_SECURITY_CHECKS) {
            if (!AppUtil.runDeviceSecurityChecks(context)) {
                // Device has failed security checks
                return;
            }
        }
        
        Log.i(TAG, "Initializing application...");
        
        ADMIN_PASSWORD_HASH = context.getResources().getString(R.string.admin_password_hash);
        ACCESS_TOKEN = context.getResources().getString(R.string.backend_api_key);  
        
        if (!BuildConfig.DEBUG)
            Crashlytics.start(context);
        
        DatabaseSeed.seed(context);

        if (AdminSettings.getInstance().getDeviceIdentifier() == null) {
            AdminSettings.getInstance().setDeviceIdentifier(UUID.randomUUID().toString());
        }

        ActiveRecordCloudSync.setAccessToken(ACCESS_TOKEN);
        ActiveRecordCloudSync.setVersionCode(AppUtil.getVersionCode(context));
        ActiveRecordCloudSync.setEndPoint(AdminSettings.getInstance().getApiUrl());
        ActiveRecordCloudSync.addReceiveTable("instruments", Instrument.class);
        ActiveRecordCloudSync.addReceiveTable("questions", Question.class);
        ActiveRecordCloudSync.addReceiveTable("options", Option.class);
        ActiveRecordCloudSync.addSendTable("surveys", Survey.class);
        ActiveRecordCloudSync.addSendTable("responses", Response.class);

        PollService.setServiceAlarm(context.getApplicationContext(), true);
    }
    
    /*
     * Security checks that must pass for the application to start.
     * 
     * If the application fails any security checks, display
     * AlertDialog indicating why and immediately stop execution
     * of the application.
     * 
     * Current security checks: require encryption
     */
    public static final boolean runDeviceSecurityChecks(Context context) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (devicePolicyManager.getStorageEncryptionStatus() != DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE) {
            new AlertDialog.Builder(context)
            .setTitle(R.string.encryption_required_title)
            .setMessage(R.string.encryption_required_text)
            .setCancelable(false)
            .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // Kill app on OK
                    int pid = android.os.Process.myPid(); 
                    android.os.Process.killProcess(pid);
                }
             })
             .show();
            return false;
        }
        return true;
    }
    
    
    /*
     * Hash the entered password and compare it with admin password hash
     */
    public static boolean checkAdminPassword(String password) {
        String hash = new String(Hex.encodeHex(DigestUtils.sha256(password)));
        return hash.equals(ADMIN_PASSWORD_HASH);
    }
}
