package com.craftcv.resumestudio;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

/**
 * Consent-gated, optional AdMob banner on the native dashboard ONLY.
 * Debug builds use Google's DEMO UNIT, release is OFF until both publisher IDs
 * are supplied during build. No interstitials, forced ad views or CV data upload.
 */
final class CraftCvAds {
    private final Activity activity;
    private final LinearLayout parent;
    private AdView banner;
    private Button privacyChoices;
    private ConsentInformation consent;
    private boolean closed;
    private boolean loading;
    private boolean loaded;

    CraftCvAds(Activity activity, LinearLayout parent) {
        this.activity = activity;
        this.parent = parent;
    }

    void start() {
        if (!BuildConfig.ADMOB_ENABLED) return; // Production without publisher IDs has no ads.
        if (BuildConfig.ADMOB_BANNER_UNIT_ID.trim().isEmpty()) return;

        consent = UserMessagingPlatform.getConsentInformation(activity);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consent.requestConsentInfoUpdate(
            activity,
            params,
            () -> {
                if (closed || activity.isFinishing() || activity.isDestroyed()) return;
                showPrivacyOptionsIfRequired();
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                    if (closed || activity.isFinishing() || activity.isDestroyed()) return;
                    showPrivacyOptionsIfRequired();
                    if (consent.canRequestAds()) initAndLoad();
                });
            },
            requestError -> {
                if (closed || activity.isFinishing() || activity.isDestroyed()) return;
                // When consent cannot be refreshed, use only the status allowed by UMP.
                showPrivacyOptionsIfRequired();
                if (consent.canRequestAds()) initAndLoad();
            }
        );
    }

    private void showPrivacyOptionsIfRequired() {
        if (consent == null || closed) return;
        boolean required=consent.getPrivacyOptionsRequirementStatus()
            == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
        if (!required) {
            if (privacyChoices != null) privacyChoices.setVisibility(View.GONE);
            return;
        }
        if (privacyChoices == null) {
            privacyChoices=new Button(activity);
            privacyChoices.setText("Ad privacy choices");
            privacyChoices.setAllCaps(false);
            privacyChoices.setTextSize(12);
            privacyChoices.setTextColor(0xFF23565A);
            privacyChoices.setOnClickListener(v ->
                UserMessagingPlatform.showPrivacyOptionsForm(activity, error -> {
                    if (!closed && consent.canRequestAds() && !loaded && !loading) initAndLoad();
                })
            );
            parent.addView(privacyChoices,new LinearLayout.LayoutParams(-1,dp(43)));
        }
        privacyChoices.setVisibility(View.VISIBLE);
    }

    private void initAndLoad() {
        if (closed || loaded || loading || !consent.canRequestAds()) return;
        loading=true;
        // Google recommends initializing ads off the main thread.
        new Thread(() -> MobileAds.initialize(activity.getApplicationContext(), status ->
            activity.runOnUiThread(() -> {
                if (closed || activity.isFinishing() || activity.isDestroyed()) {
                    loading=false;
                    return;
                }
                loadBanner();
            })
        ),"CraftCV-AdMob-Init").start();
    }

    private int dp(int size) {
        return Math.round(size * activity.getResources().getDisplayMetrics().density);
    }

    private void loadBanner() {
        if (closed || loaded || banner != null || !consent.canRequestAds()) {
            loading=false;
            return;
        }
        LinearLayout panel=new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(11),dp(12),dp(11),dp(14));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);
        pp.topMargin=dp(22);
        parent.addView(panel,pp);

        TextView label=new TextView(activity);
        label.setText("ADVERTISEMENT");
        label.setTextSize(10);
        label.setLetterSpacing(.1f);
        label.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        label.setTextColor(0xFF78919A);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.bottomMargin=dp(9);
        panel.addView(label,lp);

        FrameLayout frame=new FrameLayout(activity);
        frame.setForegroundGravity(Gravity.CENTER);
        panel.addView(frame,new LinearLayout.LayoutParams(-1,-2));

        int screenPx=activity.getResources().getDisplayMetrics().widthPixels;
        float density=activity.getResources().getDisplayMetrics().density;
        int widthDp=Math.max(1,(int)(screenPx/density)-60);

        banner=new AdView(activity);
        banner.setAdUnitId(BuildConfig.ADMOB_BANNER_UNIT_ID);
        banner.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity,widthDp));
        banner.setAdListener(new AdListener() {
            @Override public void onAdLoaded() {
                if(closed)return;
                loading=false;
                loaded=true;
                panel.setVisibility(View.VISIBLE);
            }
            @Override public void onAdFailedToLoad(LoadAdError error) {
                loading=false;
                panel.setVisibility(View.GONE);
            }
        });
        frame.addView(banner,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER));
        panel.setVisibility(View.GONE);
        banner.loadAd(new AdRequest.Builder().build());
    }

    void pause() { if(banner!=null)banner.pause(); }
    void resume() { if(banner!=null)banner.resume(); }
    void destroy() {
        closed=true;
        if(banner!=null){banner.destroy();banner=null;}
    }
}
