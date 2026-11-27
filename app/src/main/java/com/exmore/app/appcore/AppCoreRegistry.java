package com.exmore.app.appcore;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Opt-in anonymous registration; no personal content, FCM token or hardware ID. */
public final class AppCoreRegistry {
    private static final String PROJECT = "exmore";
    private static final String INSTANCE = "appcore-registry-v1";
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private static volatile long lastAttempt = -3600000L;
    private static volatile long lastSuccess = -3600000L;
    public interface Callback { void complete(String result); }
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("appcore_registry_v1",Context.MODE_PRIVATE); }
    public static boolean enabled(Context c) { return prefs(c).getBoolean("enabled",false); }
    public static boolean deletionPending(Context c) { return prefs(c).getBoolean("deletePending",false); }
    private static FirebaseApp instance(Context c) {
        FirebaseApp base=FirebaseApp.getInstance();
        if(!"appcore-2026".equals(base.getOptions().getProjectId()))throw new IllegalStateException("Wrong Firebase project");
        try{return FirebaseApp.getInstance(INSTANCE);}catch(IllegalStateException ignored){return FirebaseApp.initializeApp(c,base.getOptions(),INSTANCE);}
    }
    private static void done(Callback cb,String result) { if(cb!=null)new Handler(Looper.getMainLooper()).post(()->cb.complete(result)); }
    private static boolean save(Context c,boolean enabled,boolean pending) { return prefs(c).edit().putBoolean("enabled",enabled).putBoolean("deletePending",pending).commit(); }
    public static void consent(Context context,boolean enable,int version,Callback cb) {
        Context c=context.getApplicationContext();
        if(!BUSY.compareAndSet(false,true)){done(cb,"busy");return;}
        if(!save(c,enable,!enable)){BUSY.set(false);done(cb,"storage_failed");return;}
        lastAttempt=-3600000L;lastSuccess=-3600000L;
        EXEC.execute(()->execute(c,enable,version,cb));
    }
    public static void heartbeat(Context context,int version) {
        Context c=context.getApplicationContext();long now=SystemClock.elapsedRealtime();
        if(!enabled(c)||deletionPending(c)||now-lastAttempt<300000L||now-lastSuccess<3660000L||!BUSY.compareAndSet(false,true))return;
        lastAttempt=now;EXEC.execute(()->execute(c,true,version,null));
    }
    private static void execute(Context c,boolean enable,int version,Callback cb) {
        String result="retry";
        try {
            FirebaseApp app=instance(c);FirebaseAuth auth=FirebaseAuth.getInstance(app);FirebaseUser user=auth.getCurrentUser();
            if(user!=null&&!user.isAnonymous())throw new IllegalStateException("Unexpected auth state");
            if(!enable){
                if(user!=null){
                    Tasks.await(FirebaseFirestore.getInstance(app).collection("registeredDevices").document(PROJECT).collection("items").document(user.getUid()).delete(),20,TimeUnit.SECONDS);
                    auth.signOut();
                }
                if(!save(c,false,false))throw new IllegalStateException("Storage");result="removed";
            }else{
                if(version<1||version>2100000000||!enabled(c))throw new IllegalStateException("Invalid registration");
                if(user==null){user=Tasks.await(auth.signInAnonymously(),20,TimeUnit.SECONDS).getUser();}
                if(user==null||!user.isAnonymous()||!enabled(c))throw new IllegalStateException("Auth unavailable");
                HashMap<String,Object> row=new HashMap<>();row.put("platform","android");row.put("versionCode",version);row.put("lastSeen",FieldValue.serverTimestamp());
                Tasks.await(FirebaseFirestore.getInstance(app).collection("registeredDevices").document(PROJECT).collection("items").document(user.getUid()).set(row),20,TimeUnit.SECONDS);
                lastSuccess=SystemClock.elapsedRealtime();result="accepted";
            }
        }catch(Exception ignored){result=enable?"retry":"delete_pending";}
        finally{lastAttempt=SystemClock.elapsedRealtime();BUSY.set(false);done(cb,result);}
    }
}
