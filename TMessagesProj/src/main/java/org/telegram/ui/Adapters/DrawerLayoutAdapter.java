/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Keep;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.forkgram.DrawerItemsHelper;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.DrawerLayoutContainer;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.DrawerActionCell;
import org.telegram.ui.Cells.DrawerAddCell;
import org.telegram.ui.Cells.DrawerProfileCell;
import org.telegram.ui.Cells.DrawerUserCell;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SideMenultItemAnimator;

import java.util.ArrayList;
import java.util.Collections;

public class DrawerLayoutAdapter extends RecyclerListView.SelectionAdapter {

    private Context mContext;
    private DrawerLayoutContainer mDrawerLayoutContainer;
    private ArrayList<Item> items = new ArrayList<>(11);
    private ArrayList<Integer> accountNumbers = new ArrayList<>();
    private boolean accountsShown;
    public DrawerProfileCell profileCell;
    private SideMenultItemAnimator itemAnimator;

    public DrawerLayoutAdapter(Context context, SideMenultItemAnimator animator, DrawerLayoutContainer drawerLayoutContainer) {
        mContext = context;
        mDrawerLayoutContainer = drawerLayoutContainer;
        itemAnimator = animator;
        accountsShown = UserConfig.getActivatedAccountsCount() > 1 && MessagesController.getGlobalMainSettings().getBoolean("accountsShown", true);
        Theme.createCommonDialogResources(context);
        resetItems();
    }

    private int getAccountRowsCount() {
        int count = accountNumbers.size() + 1;
        if (accountNumbers.size() < UserConfig.MAX_ACCOUNT_COUNT) {
            count++;
        }
        return count;
    }

    @Override
    public int getItemCount() {
        int count = items.size() + 2;
        if (accountsShown) {
            count += getAccountRowsCount();
        }
        return count;
    }

    public void setAccountsShown(boolean value, boolean animated) {
        if (accountsShown == value || itemAnimator.isRunning()) {
            return;
        }
        accountsShown = value;
        if (profileCell != null) {
            profileCell.setAccountsShown(accountsShown, animated);
        }
        MessagesController.getGlobalMainSettings().edit().putBoolean("accountsShown", accountsShown).commit();
        if (animated) {
            itemAnimator.setShouldClipChildren(false);
            if (accountsShown) {
                notifyItemRangeInserted(2, getAccountRowsCount());
            } else {
                notifyItemRangeRemoved(2, getAccountRowsCount());
            }
        } else {
            notifyDataSetChanged();
        }
    }

    public boolean isAccountsShown() {
        return accountsShown;
    }

    private View.OnClickListener onPremiumDrawableClick;
    public void setOnPremiumDrawableClick(View.OnClickListener listener) {
        onPremiumDrawableClick = listener;
    }

    @Override
    public void notifyDataSetChanged() {
        resetItems();
        super.notifyDataSetChanged();
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        int itemType = holder.getItemViewType();
        return itemType == 3 || itemType == 4 || itemType == 5 || itemType == 6;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case 0:
                view = profileCell = new DrawerProfileCell(mContext, mDrawerLayoutContainer) {
                    @Override
                    protected void onPremiumClick() {
                        if (onPremiumDrawableClick != null) {
                            onPremiumDrawableClick.onClick(this);
                        }
                    }
                };
                break;
            case 2:
                view = new DividerCell(mContext);
                break;
            case 3:
                view = new DrawerActionCell(mContext);
                break;
            case 4:
                view = new DrawerUserCell(mContext);
                break;
            case 5:
                view = new DrawerAddCell(mContext);
                break;
            case 1:
            default:
                view = new EmptyCell(mContext, AndroidUtilities.dp(8));
                break;
        }
        view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case 0: {
                DrawerProfileCell profileCell = (DrawerProfileCell) holder.itemView;
                profileCell.setUser(MessagesController.getInstance(UserConfig.selectedAccount).getUser(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId()), accountsShown);
                break;
            }
            case 3: {
                DrawerActionCell drawerActionCell = (DrawerActionCell) holder.itemView;
                position -= 2;
                if (accountsShown) {
                    position -= getAccountRowsCount();
                }
                items.get(position).bind(drawerActionCell);
                drawerActionCell.setPadding(0, 0, 0, 0);
                break;
            }
            case 4: {
                DrawerUserCell drawerUserCell = (DrawerUserCell) holder.itemView;
                drawerUserCell.setAccount(accountNumbers.get(position - 2));
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int i) {
        if (i == 0) {
            return 0;
        } else if (i == 1) {
            return 1;
        }
        i -= 2;
        if (accountsShown) {
            if (i < accountNumbers.size()) {
                return 4;
            } else {
                if (accountNumbers.size() < UserConfig.MAX_ACCOUNT_COUNT) {
                    if (i == accountNumbers.size()){
                        return 5;
                    } else if (i == accountNumbers.size() + 1) {
                        return 2;
                    }
                } else {
                    if (i == accountNumbers.size()) {
                        return 2;
                    }
                }
            }
            i -= getAccountRowsCount();
        }
        if (i < 0 || i >= items.size() || items.get(i) == null) {
            return 2;
        }
        return 3;
    }

    public void swapElements(int fromIndex, int toIndex) {
        int idx1 = fromIndex - 2;
        int idx2 = toIndex - 2;
        if (idx1 < 0 || idx2 < 0 || idx1 >= accountNumbers.size() || idx2 >= accountNumbers.size()) {
            return;
        }
        final UserConfig userConfig1 = UserConfig.getInstance(accountNumbers.get(idx1));
        final UserConfig userConfig2 = UserConfig.getInstance(accountNumbers.get(idx2));
        final int tempLoginTime = userConfig1.loginTime;
        userConfig1.loginTime = userConfig2.loginTime;
        userConfig2.loginTime = tempLoginTime;
        userConfig1.saveConfig(false);
        userConfig2.saveConfig(false);
        Collections.swap(accountNumbers, idx1, idx2);
        notifyItemMoved(fromIndex, toIndex);
    }

    private void resetItems() {
        accountNumbers.clear();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                accountNumbers.add(a);
            }
        }
        Collections.sort(accountNumbers, (o1, o2) -> {
            long l1 = UserConfig.getInstance(o1).loginTime;
            long l2 = UserConfig.getInstance(o2).loginTime;
            if (l1 > l2) {
                return 1;
            } else if (l1 < l2) {
                return -1;
            }
            return 0;
        });

        items.clear();
        if (!UserConfig.getInstance(UserConfig.selectedAccount).isClientActivated()) {
            return;
        }
        // forkgram-classic: upstream 12.7.3 dropped the holiday-themed
        // drawer icon variants (msg_*_ny, msg_*_14, msg_*_hw) along with a
        // few base variants (msg_contacts, msg_invite). Collapse all event
        // branches to the icons that still ship.
        int newGroupIcon = R.drawable.msg_groups;
        // [classic] #15: restore New Secret Chat / New Channel drawer items (click handlers
        // for id 3/4 still exist in LaunchActivity; drawables msg_secret/msg_channel still ship).
        int newSecretIcon = R.drawable.msg_secret;
        int newChannelIcon = R.drawable.msg_channel;
        int contactsIcon = R.drawable.msg_contacts;
        int callsIcon = R.drawable.msg_calls;
        int savedIcon = R.drawable.msg_saved;
        int settingsIcon = R.drawable.msg_settings_old;
        int inviteIcon = R.drawable.msg_invited;
        int helpIcon = R.drawable.msg_help;
        UserConfig me = UserConfig.getInstance(UserConfig.selectedAccount);
        boolean showDivider = false;
        items.add(new Item(16, LocaleController.getString(R.string.MyProfile), R.drawable.left_status_profile));
        // forkgram-classic: upstream `msg_status_edit` / `msg_status_set`
        // drawables are gone; fall back to the existing left_status_profile.
        // [classic] each entry below can be hidden from Forkgram Settings -> Side menu items.
        if (me != null && me.isPremium() && !DrawerItemsHelper.isHidden(DrawerItemsHelper.ID_EMOJI_STATUS)) {
            if (me.getEmojiStatus() != null) {
                items.add(new Item(15, LocaleController.getString(R.string.ChangeEmojiStatus), R.drawable.left_status_profile));
            } else {
                items.add(new Item(15, LocaleController.getString(R.string.SetEmojiStatus), R.drawable.left_status_profile));
            }
            showDivider = true;
        }
//        if (MessagesController.getInstance(UserConfig.selectedAccount).storiesEnabled()) {
//            items.add(new Item(17, LocaleController.getString(R.string.ProfileStories), R.drawable.msg_menu_stories));
//            showDivider = true;
//        }
        showDivider = true;
        // forkgram-classic: ApplicationLoader.extendDrawer() was removed
        // upstream. Classic drawer doesn't need a hook for extra items.
        TLRPC.TL_attachMenuBots menuBots = MediaDataController.getInstance(UserConfig.selectedAccount).getAttachMenuBots();
        if (menuBots != null && menuBots.bots != null) {
            for (int i = 0; i < menuBots.bots.size(); i++) {
                TLRPC.TL_attachMenuBot bot = menuBots.bots.get(i);
                if (bot.show_in_side_menu && !DrawerItemsHelper.isBotHidden(bot.bot_id)) {
                    items.add(new Item(bot));
                    showDivider = true;
                }
            }
        }
        if (showDivider) {
            items.add(null); // divider
        }
        // [classic] #61: New Message moved here from the top-right overflow menu (the reporter
        // dislikes the bottom-right compose FAB). Opens the same contact picker as
        // DialogsActivity.openWriteContacts() (handled by id 18 in LaunchActivity).
        addUnlessHidden(new Item(18, LocaleController.getString(R.string.NewMessageTitle), R.drawable.msg_msgbubble3));
        addUnlessHidden(new Item(2, LocaleController.getString(R.string.NewGroup), newGroupIcon));
        addUnlessHidden(new Item(3, LocaleController.getString(R.string.NewSecretChat), newSecretIcon));
        addUnlessHidden(new Item(4, LocaleController.getString(R.string.NewChannel), newChannelIcon));
        addUnlessHidden(new Item(6, LocaleController.getString(R.string.Contacts), contactsIcon));
        addUnlessHidden(new Item(10, LocaleController.getString(R.string.Calls), callsIcon));
        addUnlessHidden(new Item(11, LocaleController.getString(R.string.SavedMessages), savedIcon));
        items.add(new Item(8, LocaleController.getString(R.string.Settings), settingsIcon));
        items.add(null); // divider
        addUnlessHidden(new Item(7, LocaleController.getString(R.string.InviteFriends), inviteIcon));
        addUnlessHidden(new Item(13, LocaleController.getString(R.string.TelegramFeatures), helpIcon));
        dropUnusedDividers();
    }

    // [classic] "My Profile" (16) and "Settings" (8) are never routed through here: they stay put so
    // there is always a way back into the app.
    private void addUnlessHidden(Item item) {
        if (!DrawerItemsHelper.isHidden(item.id)) {
            items.add(item);
        }
    }

    // [classic] dividers are plain nulls in the item list, so hiding a whole group would leave a
    // doubled, leading or trailing separator behind. Collapse them once the list is final.
    private void dropUnusedDividers() {
        for (int a = items.size() - 1; a >= 0; a--) {
            if (items.get(a) == null && (a == items.size() - 1 || items.get(a + 1) == null)) {
                items.remove(a);
            }
        }
        if (!items.isEmpty() && items.get(0) == null) {
            items.remove(0);
        }
    }

    public boolean click(View view, int position) {
        position -= 2;
        if (accountsShown) {
            position -= getAccountRowsCount();
        }
        if (position < 0 || position >= items.size()) {
            return false;
        }
        Item item = items.get(position);
        if (item != null && item.listener != null) {
            item.listener.onClick(view);
            return true;
        }
        return false;
    }

    public int getId(int position) {
        position -= 2;
        if (accountsShown) {
            position -= getAccountRowsCount();
        }
        if (position < 0 || position >= items.size()) {
            return -1;
        }
        Item item = items.get(position);
        return item != null ? item.id : -1;
    }

    public int getFirstAccountPosition() {
        if (!accountsShown) {
            return RecyclerView.NO_POSITION;
        }
        return 2;
    }

    public int getLastAccountPosition() {
        if (!accountsShown) {
            return RecyclerView.NO_POSITION;
        }
        return 1 + accountNumbers.size();
    }

    public TLRPC.TL_attachMenuBot getAttachMenuBot(int position) {
        position -= 2;
        if (accountsShown) {
            position -= getAccountRowsCount();
        }
        if (position < 0 || position >= items.size()) {
            return null;
        }
        Item item = items.get(position);
        return item != null ? item.bot : null;
    }

    public static class Item {
        public int icon;
        public CharSequence text;
        public int id;
        TLRPC.TL_attachMenuBot bot;
        View.OnClickListener listener;
        public boolean error;

        public Item(int id, CharSequence text, int icon) {
            this.icon = icon;
            this.id = id;
            this.text = text;
        }

        public Item(TLRPC.TL_attachMenuBot bot) {
            this.bot = bot;
            this.id = (int) (100 + (bot.bot_id >> 16));
        }

        public void bind(DrawerActionCell actionCell) {
            if (this.bot != null) {
                actionCell.setBot(bot);
            } else {
                actionCell.setTextAndIcon(id, text, icon);
            }
            actionCell.setError(error);
        }

        @Keep
        public Item onClick(View.OnClickListener listener) {
            this.listener = listener;
            return this;
        }

        @Keep
        public Item withError() {
            this.error = true;
            return this;
        }
    }
}
