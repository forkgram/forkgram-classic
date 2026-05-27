package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }

        setIcon(LauncherIcon.DEFAULT);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        for (LauncherIcon i : LauncherIcon.values()) {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    // [classic] #54: small notification icon follows the selected app icon
    // (Classic hardcoded R.drawable.notification — the fork — regardless of the picker).
    public static int getNotificationIcon() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return icon.notification;
            }
        }
        return R.drawable.notification;
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.drawable.icon_01_background_sa, R.mipmap.icon_01_foreground_sa, R.string.AppIconDefault, R.drawable.notification),
        ADAPTIVE("AdaptiveIcon", R.drawable.icon_01_background_sa, R.mipmap.icon_01_foreground_sa, R.string.AppIconAdaptive, R.drawable.notification),
        ORIGINAL("OriginalIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.CropOriginal, R.drawable.notification_plane),
        VINTAGE("VintageIcon", R.drawable.icon_6_background_sa, R.mipmap.icon_6_foreground_sa, R.string.AppIconVintage, R.drawable.notification_vintage),
        AQUA("AquaIcon", R.drawable.icon_4_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconAqua, R.drawable.notification_plane),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground_sa, R.string.AppIconPremium, R.drawable.notification_star, true),
        TURBO("TurboIcon", R.drawable.icon_5_background_sa, R.mipmap.icon_5_foreground_sa, R.string.AppIconTurbo, R.drawable.notification_turbo, true),
        NOX("NoxIcon", R.mipmap.icon_2_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconNox, R.drawable.notification_plane, true);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final int notification; // [classic] #54: status-bar icon matching this app icon
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title, int notification) {
            this(key, background, foreground, title, notification, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, int notification, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.notification = notification;
            this.premium = premium;
        }
    }
}
