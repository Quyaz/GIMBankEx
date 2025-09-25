package com.quyaz.gimbankex;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Player;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.api.Client;
import net.runelite.client.util.ImageUtil;


import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicBorders;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static net.runelite.api.gameval.ItemID.BANK_FILLER;

public class GIMBankExPanel extends PluginPanel {

    private final ItemManager itemManager;
    @Getter
    private final MaterialTabGroup topTabGroup;
    private final BufferedImage removeIcon;
    @Getter
    private final MaterialTab bankTab;
    @Getter
    private final MaterialTab messagesTab;
    @Getter
    private IconTextField searchBar;

    @Inject
    private Client client;
    private JPanel bankTransactionsPanel;
    private final GIMBankExClient gimBankEx;
    private JPanel messagesListPanel;
    private JTextArea messageInput;
    @Getter
    @Setter
    private JLabel pickedIcon;
    @Getter
    @Setter
    private BankItem selectedItem;


    public GIMBankExPanel(GIMBankExClient client, ItemManager manager) {
        itemManager = manager;
        gimBankEx = client;

        setBorder(new EmptyBorder(5, 7, 0, 7));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.insets = new Insets(0, 0, 7, 0);

        JPanel topDisplay = new JPanel();
        topTabGroup = new MaterialTabGroup(topDisplay);
        bankTab = new MaterialTab("Bank", topTabGroup, createbankPanel());
        messagesTab = new MaterialTab("Messages", topTabGroup, createMessagesPanel());

        topTabGroup.setBorder(new EmptyBorder(0, 0, 0, 0));
        topTabGroup.addTab(bankTab);
        topTabGroup.addTab(messagesTab);
        topTabGroup.select(messagesTab);

        add(topTabGroup, gc);
        gc.gridy++;
        add(topDisplay, gc);
        removeIcon = ImageUtil.loadImageResource(getClass(), "/remove.png");
        clearSelectedItem();
    }

    private void clearSelectedItem() {
        selectedItem = new BankItem(BANK_FILLER, 1, "BANK FILLER");
    }

    private JComponent createMessagesPanel() {
        JPanel messagesPanel = new JPanel();
        messagesPanel.setLayout(new BorderLayout());
        messagesPanel.setBorder(new EmptyBorder(2, 0, 2, 0));

        messagesListPanel = new JPanel();
        messagesListPanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));
        messagesPanel.add(messagesListPanel);
        messagesListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane scrollPane = new JScrollPane(messagesListPanel);
        scrollPane.setHorizontalScrollBarPolicy(31);
        JPanel wrappedBankPanel = new JPanel();
        wrappedBankPanel.setPreferredSize(new Dimension(242, 0));
        wrappedBankPanel.setLayout(new BorderLayout());
        wrappedBankPanel.add(scrollPane, "Center");

        BufferedImage refreshIcon = ImageUtil.loadImageResource(getClass(), "/Refresh.png");
        BufferedImage sendIcon = ImageUtil.loadImageResource(getClass(), "/send.png");
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setIcon(new ImageIcon(refreshIcon));
        refreshButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                messageInput.setText("");
                gimBankEx.getMessages(true, true);
            }
        });

        JButton sendButton = new JButton("Send");
        sendButton.setIcon(new ImageIcon(sendIcon));
        sendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                gimBankEx.saveMessage(messageInput.getText());
                messageInput.setText("");
            }
        });

        pickedIcon = new JLabel();
        pickedIcon.setVerticalAlignment(SwingConstants.CENTER);
        pickedIcon.setHorizontalAlignment(SwingConstants.CENTER);
        AsyncBufferedImage itemImage = null;
        if (selectedItem == null) {
            itemImage = itemManager.getImage(BANK_FILLER, 1, false);
        } else {
            itemImage = itemManager.getImage(selectedItem.getItemId(), selectedItem.getAmount(), selectedItem.getAmount() > 1);
        }
        itemImage.addTo(pickedIcon);
        BufferedImage clearIcon = ImageUtil.loadImageResource(getClass(), "/remove.png");
        JButton clearButton = new JButton();
        clearButton.setIcon(new ImageIcon(clearIcon));
        clearButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                clearSelectedItem();
                reloadPickIcon();
            }
        });

        JPanel pickIcon = new JPanel();
        pickIcon.setLayout(new DynamicGridLayout(1, 3));
        pickIcon.setBorder(new EmptyBorder(2, 2, 2, 2));
        pickIcon.setBackground(ColorScheme.DARK_GRAY_COLOR);
        pickIcon.add(pickedIcon);
        pickIcon.add(clearButton);
        pickIcon.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                BankItem mySelectedItem = getSelectedItem();
                if (mySelectedItem.getItemId() != BANK_FILLER) {
                    mySelectedItem.setAmount(mySelectedItem.getAmount() - e.getWheelRotation());
                    selectedItem = mySelectedItem;
                    reloadPickIcon();
                }
            }
        });

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new DynamicGridLayout(1, 2));
        buttonsPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
        buttonsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttonsPanel.add(sendButton);
        buttonsPanel.add(pickIcon);

        JPanel messageInputPanel = new JPanel();
        messageInputPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        messageInputPanel.setLayout(new DynamicGridLayout(3, 1));
        messageInput = new JTextArea();
        messageInput.setToolTipText("Send message..");
        messageInput.setBackground(ColorScheme.DARK_GRAY_COLOR);
        messageInput.setBorder(new BasicBorders.FieldBorder(ColorScheme.DARKER_GRAY_COLOR, ColorScheme.DARKER_GRAY_COLOR, ColorScheme.DARKER_GRAY_COLOR, ColorScheme.DARKER_GRAY_COLOR));
        messageInput.setLineWrap(true);
        messageInput.setTabSize(2);
        messageInput.setMinimumSize(new Dimension(PANEL_WIDTH, 100));
        messageInput.setPreferredSize(new Dimension(PANEL_WIDTH, 100));
        messageInput.setAutoscrolls(true);
        messageInputPanel.add(messageInput);
        messageInputPanel.add(buttonsPanel);
        messageInputPanel.add(refreshButton);

        messagesPanel.add(messageInputPanel, "North");
        messagesPanel.add(messagesListPanel, "Center");
        return messagesPanel;

    }

    protected void reloadPickIcon() {
        pickedIcon.removeAll();
        pickedIcon.setBackground(ColorScheme.BRAND_ORANGE);
        AsyncBufferedImage itemImage = itemManager.getImage(selectedItem.getItemId(), selectedItem.getAmount(), selectedItem.getAmount() > 1);
        itemImage.addTo(pickedIcon);
        pickedIcon.revalidate();
    }

    private JPanel createbankPanel() {
        JPanel bankPanel = new JPanel();
        JPanel topBankPanel = new JPanel();
        bankPanel.setLayout(new BorderLayout());
        bankPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        topBankPanel.setLayout(new DynamicGridLayout(1,4));

        bankTransactionsPanel = new JPanel();
        bankTransactionsPanel.setLayout(new DynamicGridLayout(0, 1));
        bankPanel.add(bankTransactionsPanel);
        bankTransactionsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane scrollPane = new JScrollPane(bankTransactionsPanel);
        scrollPane.setHorizontalScrollBarPolicy(31);
        JPanel wrappedBankPanel = new JPanel();
        wrappedBankPanel.setPreferredSize(new Dimension(242, 0));
        wrappedBankPanel.setLayout(new BorderLayout());
        wrappedBankPanel.add(scrollPane, "Center");

        searchBar = new IconTextField();
        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        searchBar.setMinimumSize(new Dimension(0, 30));
        searchBar.addActionListener(e -> lookupTransactions());
        searchBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }
                lookupTransactions();
            }
        });
        searchBar.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                lookupTransactions(searchBar.getText());
            }
        });

        searchBar.addClearListener(() ->
        {
            searchBar.setIcon(IconTextField.Icon.SEARCH);
            searchBar.setEditable(true);
            lookupTransactions();
        });
        topBankPanel.add(searchBar, BorderLayout.LINE_START);

        BufferedImage refreshIcon = ImageUtil.loadImageResource(getClass(), "/Refresh.png");
        JButton refreshButton = new JButton();
        refreshButton.setIcon(new ImageIcon(refreshIcon));
        refreshButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                searchBar.setText("");
                gimBankEx.getBankTransactions(true);
            }
        });

        topBankPanel.add(refreshButton, BorderLayout.AFTER_LINE_ENDS);
        bankPanel.add(topBankPanel, "North");
        bankPanel.add(bankTransactionsPanel, "Center");
        JButton loadMorePanel = new JButton();
        loadMorePanel.setIcon(new ImageIcon(ImageUtil.loadImageResource(getClass(), "/more.png")));
        loadMorePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                gimBankEx.getBankTransactions(true, searchBar.getText());
            }
        });

        bankPanel.add(loadMorePanel, BorderLayout.SOUTH);
        return bankPanel;
    }

    private void lookupTransactions() {
        if (searchBar.getText().isEmpty()) {
            if (client == null) {
                gimBankEx.getBankTransactions(true);
                return;
            }
            Player localPlayer = client.getLocalPlayer();
            searchBar.setText(localPlayer.getName());
        }
        gimBankEx.getBankTransactions(true, searchBar.getText());
    }

    private void lookupTransactions(String search) {
        if (!search.equals(searchBar.getText())) {
            return;
        }
        if (searchBar.getText().isEmpty()) {
            if (client == null) {
                gimBankEx.getBankTransactions(true);
                return;
            }
            Player localPlayer = client.getLocalPlayer();
            searchBar.setText(localPlayer.getName());
        }
        gimBankEx.getBankTransactions(true, searchBar.getText());
    }

    public void refreshTransactions(ArrayList<ItemTransaction> transactions) {
        bankTransactionsPanel.removeAll();
        if (transactions != null && !transactions.isEmpty()) {
            for (ItemTransaction transaction : transactions) {
                bankTransactionsPanel.add(createTransactionPanel(transaction));
            }
            bankTransactionsPanel.revalidate();
        }
    }

    public void refreshMessages(ArrayList<GIMMessage> messages) {
        messagesListPanel.removeAll();
        if (messages != null && !messages.isEmpty()) {
            for (GIMMessage mess : messages) {
                messagesListPanel.add(createMessage(mess));
            }
            messagesListPanel.revalidate();
        }
    }

    private JPanel createTransactionPanel(ItemTransaction transaction) {
        if (transaction == null) {
            return new JPanel();
        }
        transaction.setHidden(false);
        JPanel transactionPanel = new JPanel();
        transactionPanel.setLayout(new BorderLayout());
        transactionPanel.setBorder(new EmptyBorder(2, 2, 2, 2));

        JPanel textContainer = new JPanel();
        textContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        textContainer.setLayout(new DynamicGridLayout());
        textContainer.setBorder(new EmptyBorder(5, 7, 5, 7));

        JLabel titleLabel = new JLabel(transaction.getUser());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getRunescapeSmallFont());

        JLabel statusLabel = new JLabel(gimBankEx.formatDateTime(transaction.getCreated_at()));
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setFont(FontManager.getRunescapeSmallFont());

        textContainer.add(titleLabel, BorderLayout.LINE_START);
        textContainer.add(statusLabel, BorderLayout.CENTER);
        textContainer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                transaction.setHidden(!transaction.isHidden());
                e.getComponent().getParent().getComponent(1).setVisible(!transaction.isHidden());

            }
        });

        JPanel itemsContainer = new JPanel();
//        itemsContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        List<BankItem> items = transaction.getItems();
        final int rowSize = ((items.size() % 5 == 0) ? 0 : 1) + items.size() / 5;

        itemsContainer.setLayout(new GridLayout(rowSize, 5, 1, 1));
        itemsContainer.setBorder(new EmptyBorder(5, 5, 5, 5));
        for (BankItem item : transaction.getItems()) {
            final JPanel slotContainer = new JPanel();
            if (item.getAmount() > 0) {
                slotContainer.setBackground(new Color(26, 96, 39));
            } else {
                slotContainer.setBackground(new Color(144, 44, 44, 180));
            }

            final JLabel imageLabel = new JLabel();
            imageLabel.setToolTipText(buildToolTip(item));
            imageLabel.setVerticalAlignment(SwingConstants.CENTER);
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

            AsyncBufferedImage itemImage = itemManager.getImage(item.getItemId(), item.getAmount(), item.getAmount() > 1 || item.getAmount() < -1);
            itemImage.addTo(imageLabel);
            slotContainer.add(imageLabel);

            itemsContainer.add(slotContainer);
        }
        for (int i = 0; i < (5 * rowSize - transaction.getItems().size()); i++) {
            final JPanel slotContainer = new JPanel();
            slotContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            itemsContainer.add(slotContainer);
        }

        itemsContainer.revalidate();

        transactionPanel.add(textContainer, BorderLayout.NORTH);
        if (!transaction.isHidden()) {
            transactionPanel.add(itemsContainer, BorderLayout.CENTER);
        }

        transactionPanel.revalidate();
        return transactionPanel;
    }

    private String buildToolTip(BankItem item) {
        return item.getAmount() + " " + item.getItem();
    }

    private JPanel createMessage(GIMMessage message) {

        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new DynamicGridLayout(2, 1, 2, 2));
        messagePanel.setBorder(new EmptyBorder(2, 2, 2, 2));
        messagePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JPanel textContainer = new JPanel();
        textContainer.setLayout(new DynamicGridLayout(1, 3));
        textContainer.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel titleLabel = new JLabel(message.getSender());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getRunescapeSmallFont());

        JLabel statusLabel = new JLabel(gimBankEx.formatDateTime(message.getCreated_at()));
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setFont(FontManager.getRunescapeSmallFont());

        JLabel removeLabel = new JLabel();
        removeLabel.setIcon(new ImageIcon(removeIcon));
        removeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                gimBankEx.removeMessage(message);
            }
        });


        textContainer.add(titleLabel, BorderLayout.LINE_START);
        textContainer.add(statusLabel, BorderLayout.LINE_END);
        textContainer.add(removeLabel, BorderLayout.AFTER_LINE_ENDS);

        JPanel messageWrapper = new JPanel();
        if (message.getItem_id() != null) {
            messageWrapper.setLayout(new DynamicGridLayout(1, 2));
            messageWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        }
        JTextArea myMessage = new JTextArea(message.getMessage());
        JScrollPane sb = new JScrollPane(myMessage);
        myMessage.setBackground(ColorScheme.DARKER_GRAY_COLOR);


        myMessage.setLineWrap(true);
        myMessage.setWrapStyleWord(true);
        myMessage.setOpaque(false);
        myMessage.setEditable(false);
        myMessage.setFocusable(false);
        myMessage.setBorder(new EmptyBorder(0, 0, 0, 0));

        myMessage.setEditable(false);


        messagePanel.add(textContainer);
        if (message.getItem_id() != null) {
            final JLabel imageLabel = new JLabel();
//            imageLabel.setToolTipText(buildToolTip(item));
            imageLabel.setVerticalAlignment(SwingConstants.CENTER);
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

            AsyncBufferedImage itemImage = itemManager.getImage(message.getItem_id().intValue(), message.getAmount(), message.getAmount() > 1);
            itemImage.addTo(imageLabel);
            messageWrapper.add(imageLabel);
            messageWrapper.add(myMessage);
            messagePanel.add(messageWrapper);
        } else {
            messagePanel.add(myMessage);
        }

        messagePanel.revalidate();
        return messagePanel;
    }
}
