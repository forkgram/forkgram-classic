/*
 * Copyright 23rd, 2019.
 */

package org.telegram.ui;

import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.forkgram.DrawerItemsHelper;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * [classic] Picks which entries the side drawer shows.
 *
 * "My Profile" and "Settings" are not listed on purpose — they are the way back into the app, and a
 * user who also turned the bottom tabs off would otherwise have no obvious route to the settings.
 */
public class DrawerItemsActivity extends UniversalFragment {

    private static final int BOT_ID_OFFSET = 1000;

    private ArrayList<TLRPC.TL_attachMenuBot> bots = new ArrayList<>();

    @Override
    protected CharSequence getTitle() {
        return LocaleController.getString(R.string.ForkDrawerItems);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.ForkDrawerItemsHeader)));
        for (DrawerItemsHelper.Entry entry : DrawerItemsHelper.getEntries()) {
            items.add(UItem.asCheck(entry.id, LocaleController.getString(entry.titleRes))
                .setChecked(!DrawerItemsHelper.isHidden(entry.id)));
        }

        // Side-menu bots are pushed by the server (the Wallet is one), so this section only exists
        // while at least one is on offer.
        bots = DrawerItemsHelper.getSideMenuBots();
        if (!bots.isEmpty()) {
            items.add(UItem.asShadow(null));
            items.add(UItem.asHeader(LocaleController.getString(R.string.ForkDrawerItemsBots)));
            for (int i = 0; i < bots.size(); i++) {
                TLRPC.TL_attachMenuBot bot = bots.get(i);
                items.add(UItem.asCheck(BOT_ID_OFFSET + i, bot.short_name)
                    .setChecked(!DrawerItemsHelper.isBotHidden(bot.bot_id)));
            }
        }
        items.add(UItem.asShadow(LocaleController.getString(R.string.ForkDrawerItemsInfo)));
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= BOT_ID_OFFSET) {
            int index = item.id - BOT_ID_OFFSET;
            if (index < 0 || index >= bots.size()) {
                return;
            }
            long botId = bots.get(index).bot_id;
            DrawerItemsHelper.setBotHidden(botId, !DrawerItemsHelper.isBotHidden(botId));
        } else {
            DrawerItemsHelper.setHidden(item.id, !DrawerItemsHelper.isHidden(item.id));
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
        // The drawer only rebuilds its items on notifyDataSetChanged(), so ask for one now instead
        // of leaving the change to appear after the next account switch or theme change.
        LaunchActivity.refreshDrawerItems();
    }
}
