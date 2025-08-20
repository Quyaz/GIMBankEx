package com.quyaz.gimbankex;

import com.google.inject.Provides;

import javax.inject.Inject;
import javax.swing.*;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import okhttp3.*;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
        name = "GIM Bank Ex"
)
public class GimBankExPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private GimBankExConfig config;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ChatboxPanelManager chatboxPanelManager;

    private List<Item> myInventory = Collections.emptyList();
    @Getter
    private GIMBankExPanel panel;
    private NavigationButton toolbarButton;
    @Getter
    @Setter
    private ArrayList<ItemTransaction> transactions;
    @Inject
    private GIMBankExClient gimClient;
    @Getter
    @Setter
    private ArrayList<GIMMessage> gimMessages;

    protected final String DATE_FORMAT = "HH:mm d/M/y";

    @Override
    protected void startUp() throws Exception {
        log.info("GIM Bank ex started!");
        panel = new GIMBankExPanel(gimClient, itemManager);
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/groupiron.png");
        toolbarButton = NavigationButton.builder()
                .tooltip("GIM Bank Ex")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(toolbarButton);
        gimClient.getBankTransactions(true);
        gimClient.getMessages(true, true);
    }

    protected void updatePanel() {
        updateTransactions();
        updateMessages();
    }

    protected void updateTransactions() {
        panel.refreshTransactions(transactions);
    }

    protected void updateMessages() {
        panel.refreshMessages(gimMessages);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("GIM Bank Ex stopped!");
        clientToolbar.removeNavigation(toolbarButton);
    }

    @Subscribe
    void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() != InterfaceID.SHARED_BANK_SIDE) {
            return;
        }
        if (config.autoOpen() != GimBankExConfig.AutoOpen.NONE) {
            SwingUtilities.invokeLater(() -> {
                clientToolbar.openPanel(toolbarButton);
                if (config.autoOpen() == GimBankExConfig.AutoOpen.BANK) {
                    panel.getTopTabGroup().select(panel.getBankTab());
                } else if (config.autoOpen() == GimBankExConfig.AutoOpen.MESSAGES) {
                    panel.getTopTabGroup().select(panel.getMessagesTab());
                }
            });
        }

        gimClient.getBankTransactions(true);
        gimClient.getMessages(true, true);

        if (client.getItemContainer(net.runelite.api.gameval.InventoryID.INV_PLAYER_TEMP) != null) {
            myInventory = getItemsFromContainer(client.getItemContainer(net.runelite.api.gameval.InventoryID.INV_PLAYER_TEMP));
        }
    }

    @Subscribe
    void onWidgetClosed(WidgetClosed event) {
        if (event.getGroupId() != 293) {
            return;
        }
        String messageText = client.getWidget(293, 1).getText();
        if (!messageText.contains("Saving")) {
            return;
        }
        List<Item> exitItems = getItemsFromContainer(client.getItemContainer(InventoryID.INV));
        List<Item> diff = getDiff(canonicalizeItemList(myInventory), canonicalizeItemList(exitItems));

        if (!diff.isEmpty()) {
            gimClient.storeBankTransaction(diff);
        }
        myInventory = Collections.emptyList();
    }

    private List<Item> getDiff(List<Item> before, List<Item> after) {
        // This part taken from https://github.com/Lazyfaith/runelite-bank-memory-plugin/blob/master/src/main/java/com/bankmemory/ItemListDiffGenerator.java

		/*
		BSD 2-Clause License
		Copyright (c) 2020, Samuel Holloway
		All rights reserved.
		Redistribution and use in source and binary forms, with or without
		modification, are permitted provided that the following conditions are met:
		1. Redistributions of source code must retain the above copyright notice, this
		   list of conditions and the following disclaimer.
		2. Redistributions in binary form must reproduce the above copyright notice,
		   this list of conditions and the following disclaimer in the documentation
		   and/or other materials provided with the distribution.
		THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
		AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
		IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
		DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
		FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
		DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
		SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
		CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
		OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
		OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
		*/

        Map<Integer, Integer> beforeItems = new HashMap<>();
        Map<Integer, Integer> afterItems = new HashMap<>();
        after.forEach(i -> afterItems.put(i.getId(), i.getQuantity()));
        List<Item> results = new ArrayList<>();
        for (Item i : before) {
            if (i == null || i.getId() == -1) {
                continue;
            }
            beforeItems.put(i.getId(), i.getQuantity());
            int diff = afterItems.getOrDefault(i.getId(), 0) - i.getQuantity();
            if (diff != 0) {
                results.add(new Item(i.getId(), diff));
            }
        }
        for (Item i : after) {
            if (!beforeItems.containsKey(i.getId())) {
                results.add(i);
            }
        }
        return results;
    }

    private List<Item> canonicalizeItemList(List<Item> itemList) {
        List<Item> canonList = new ArrayList<>();

        for (int i = 0; i < itemList.size(); i++) {
            int canonId = itemManager.canonicalize(itemList.get(i).getId());
            boolean add = true;

            for (int j = 0; j < canonList.size(); j++) {
                if (canonList.get(j).getId() == canonId) {
                    add = false;
                    int before = canonList.get(j).getQuantity();
                    canonList.remove(j);
                    canonList.add(new Item(canonId, itemList.get(i).getQuantity() + before));
                }
            }

            if (add) {
                canonList.add(new Item(canonId, itemList.get(i).getQuantity()));
            }
        }
        return canonList;
    }

    private List<Item> getItemsFromContainer(ItemContainer shared) {
        Item[] storageItems = shared.getItems();
        if (storageItems.length == 0) return new ArrayList<>();
        List<Item> itemList = Arrays.stream(storageItems).filter(p -> p.getId() > -1).collect(Collectors.toList());

        storageItems = new Item[itemList.size()];
        itemList.toArray(storageItems);

        Arrays.sort(storageItems, new Comparator<Item>() {
            public int compare(Item b1, Item b2) {
                if (b1.getId() > b2.getId()) {
                    return +1;
                } else if (b1.getId() < b2.getId()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });

        List<Item> containerItems = new ArrayList<>();

        for (int i = 0; i < storageItems.length; i++) {
            ItemComposition composition = itemManager.getItemComposition(storageItems[i].getId());

            Item[] finalItems = storageItems;
            int finalI = i;
            boolean alreadyIn = containerItems.stream().anyMatch(o -> o.getId() == finalItems[finalI].getId());
            if (alreadyIn) {
                for (int j = 0; j < containerItems.size(); j++) {
                    if (containerItems.get(j).getId() == storageItems[i].getId()) {
                        int count = containerItems.get(j).getQuantity();
                        containerItems.remove(j);
                        containerItems.add(new Item(composition.getId(), count + storageItems[i].getQuantity()));
                    }
                }
            } else {
                containerItems.add(new Item(composition.getId(), storageItems[i].getQuantity()));
            }
        }
        return containerItems;
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (event.getActionParam1() == InterfaceID.SharedBank.ITEMS
                && event.getOption().equals("Examine")) {
            Widget container = client.getWidget(InterfaceID.SharedBank.ITEMS);
            Widget item = container.getChild(event.getActionParam0());
            int itemId = item.getItemId();

            String text = "Pick icon";

            client.createMenuEntry(-1)
                    .setParam0(event.getActionParam0())
                    .setParam1(event.getActionParam1())
                    .setTarget(event.getTarget())
                    .setOption(text)
                    .setType(MenuAction.RUNELITE)
                    .setIdentifier(event.getIdentifier())
                    .setItemId(event.getItemId())
                    .onClick((e) -> {
                        panel.setSelectedItem(new BankItem(itemId, item.getItemQuantity(), item.getName()));
                        panel.reloadPickIcon();
                    });
        }
    }

    @Provides
    GimBankExConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GimBankExConfig.class);
    }
}
