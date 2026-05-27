/*
 * Copyright 23rd, 2019.
 */

package org.telegram.messenger.forkgram;

import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * [classic] Lets the user hide entries of the side drawer.
 *
 * "My Profile" and "Settings" are deliberately not listed: they are the anchors back into the app,
 * and hiding Settings would leave a user whose bottom tabs are off with no obvious way to return.
 */
public class DrawerItemsHelper {

    // Ids mirror DrawerLayoutAdapter.Item ids, which LaunchActivity also dispatches clicks on.
    public static final int ID_EMOJI_STATUS = 15;
    public static final int ID_NEW_MESSAGE = 18;
    public static final int ID_NEW_GROUP = 2;
    public static final int ID_NEW_SECRET_CHAT = 3;
    public static final int ID_NEW_CHANNEL = 4;
    public static final int ID_CONTACTS = 6;
    public static final int ID_CALLS = 10;
    public static final int ID_SAVED_MESSAGES = 11;
    public static final int ID_INVITE_FRIENDS = 7;
    public static final int ID_TELEGRAM_FEATURES = 13;

    /** One hideable drawer entry, in the order the drawer itself lays them out. */
    public static class Entry {
        public final int id;
        public final int titleRes;

        Entry(int id, int titleRes) {
            this.id = id;
            this.titleRes = titleRes;
        }
    }

    private static final Entry[] ENTRIES = {
        new Entry(ID_EMOJI_STATUS, R.string.SetEmojiStatus),
        new Entry(ID_NEW_MESSAGE, R.string.NewMessageTitle),
        new Entry(ID_NEW_GROUP, R.string.NewGroup),
        new Entry(ID_NEW_SECRET_CHAT, R.string.NewSecretChat),
        new Entry(ID_NEW_CHANNEL, R.string.NewChannel),
        new Entry(ID_CONTACTS, R.string.Contacts),
        new Entry(ID_CALLS, R.string.Calls),
        new Entry(ID_SAVED_MESSAGES, R.string.SavedMessages),
        new Entry(ID_INVITE_FRIENDS, R.string.InviteFriends),
        new Entry(ID_TELEGRAM_FEATURES, R.string.TelegramFeatures),
    };

    public static Entry[] getEntries() {
        return ENTRIES;
    }

    private static String key(int id) {
        return "drawerItemHidden_" + id;
    }

    private static String botKey(long botId) {
        return "drawerBotHidden_" + botId;
    }

    public static boolean isHidden(int id) {
        return MessagesController.getGlobalMainSettings().getBoolean(key(id), false);
    }

    public static void setHidden(int id, boolean hidden) {
        MessagesController.getGlobalMainSettings().edit().putBoolean(key(id), hidden).apply();
    }

    public static boolean isBotHidden(long botId) {
        return MessagesController.getGlobalMainSettings().getBoolean(botKey(botId), false);
    }

    public static void setBotHidden(long botId, boolean hidden) {
        MessagesController.getGlobalMainSettings().edit().putBoolean(botKey(botId), hidden).apply();
    }

    /** The side-menu bots the server currently offers, e.g. the Wallet. May be empty. */
    public static ArrayList<TLRPC.TL_attachMenuBot> getSideMenuBots() {
        ArrayList<TLRPC.TL_attachMenuBot> result = new ArrayList<>();
        TLRPC.TL_attachMenuBots menuBots =
            MediaDataController.getInstance(UserConfig.selectedAccount).getAttachMenuBots();
        if (menuBots != null && menuBots.bots != null) {
            for (int i = 0; i < menuBots.bots.size(); i++) {
                TLRPC.TL_attachMenuBot bot = menuBots.bots.get(i);
                if (bot.show_in_side_menu) {
                    result.add(bot);
                }
            }
        }
        return result;
    }

    /** How many entries the user has hidden, counting only bots the server still offers. */
    public static int getHiddenCount() {
        int count = 0;
        for (Entry entry : ENTRIES) {
            if (isHidden(entry.id)) {
                count++;
            }
        }
        ArrayList<TLRPC.TL_attachMenuBot> bots = getSideMenuBots();
        for (int i = 0; i < bots.size(); i++) {
            if (isBotHidden(bots.get(i).bot_id)) {
                count++;
            }
        }
        return count;
    }
}
