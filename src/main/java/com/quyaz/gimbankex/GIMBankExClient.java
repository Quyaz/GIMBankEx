package com.quyaz.gimbankex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.client.game.ItemManager;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static net.runelite.api.gameval.ItemID.BANK_FILLER;

public class GIMBankExClient {

    private static final Logger log = LoggerFactory.getLogger(GIMBankExClient.class);
    @Inject
    private GimBankExConfig config;

    @Inject
    private GimBankExPlugin plugin;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private Client client;

    @Inject
    private ItemManager itemManager;

    public void saveMessage(String data) {
        String url = config.api();
        String shared = config.messages();
        url = url + (url.endsWith("/") ? shared : ("/" + shared));

        String sender = "Not logged in";
        if (client != null && client.getLocalPlayer() != null) {
            sender = client.getLocalPlayer().getName();
        }

        JsonObject j = new JsonObject();
        j.addProperty("message", data);
        j.addProperty("sender", sender);
        if (plugin.getPanel().getSelectedItem().getItemId() != BANK_FILLER) {
            j.addProperty("item_id", plugin.getPanel().getSelectedItem().getItemId());
            j.addProperty("amount", plugin.getPanel().getSelectedItem().getAmount());
        }
        Request request = new Request.Builder()
                .url(url)
                .addHeader("token", config.token())
                .addHeader("group_name", config.groupName())
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), j.toString()))
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.debug(e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody body = response.body();
                int code = response.code();
                if (code == 500) {
                    log.debug(body.string());
                } else {
                    getMessages(true, true);
                }
            }
        });

    }

    public void getMessages(boolean async, boolean refresh) {
        String url = config.api();
        String messages = config.messages();
        url = url + (url.endsWith("/") ? messages : ("/" + messages));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("token", config.token())
                .addHeader("group_name", config.groupName())
                .get()
                .build();
        if (async) {
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    throw new RuntimeException(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    processMessagesResponse(response);
                    if (refresh) {
                        plugin.updateMessages();
                    }
                }
            });
        } else {
            try (Response response = okHttpClient.newCall(request).execute()) {
                processMessagesResponse(response);
                if (refresh) {
                    plugin.updateMessages();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    protected void getBankTransactions(boolean refresh) {
        getBankTransactions(refresh, null);
    }

    protected void getBankTransactions(boolean refresh, String search) {
        String url = config.api();
        String shared = config.shared();
        url = url + (url.endsWith("/") ? shared : ("/" + shared));
        if (search != null && !search.isEmpty()) {
            url = url + "?search=" + search;
        }
        Request request = new Request.Builder()
                .url(url)
                .addHeader("token", config.token())
                .addHeader("group_name", config.groupName())
                .get()
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                throw new RuntimeException(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                processTransactionsResponse(response);
                if (refresh) {
                    plugin.updatePanel();
                }
            }
        });

    }

    private void processTransactionsResponse(Response response) throws IOException {
        ResponseBody body = response.body();
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(body.string());
        body.close();
        JsonArray array = jsonElement.getAsJsonArray();
        ArrayList<ItemTransaction> temp = new ArrayList<ItemTransaction>();
        ItemTransaction it = null;
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            if (it == null || (it.getCreated_at().compareTo(object.get("created_at").getAsString()) > 0)) {
                if (it != null) {
                    temp.add(it);
                }
                it = new ItemTransaction(object.get("user").getAsString(), object.get("created_at").getAsString(), new ArrayList<BankItem>());
            }
            it.getItems().add(new BankItem(object.get("item_id").getAsInt(), object.get("amount").getAsInt(), object.get("item").getAsString()));
        }
        temp.add(it);
        plugin.setTransactions(temp);
    }

    private void processMessagesResponse(Response response) throws IOException {
        ResponseBody body = response.body();
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(body.string());
        body.close();
        JsonArray array = jsonElement.getAsJsonArray();
        ArrayList<GIMMessage> temp = new ArrayList<GIMMessage>();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            GIMMessage gm = new GIMMessage(
                    object.get("id").getAsBigInteger(),
                    object.get("message").getAsString(),
                    object.get("sender").getAsString(),
                    object.get("item_id").isJsonNull() ? null : BigInteger.valueOf(object.get("item_id").getAsLong()),
                    object.get("amount").isJsonNull() ? 1 : object.get("amount").getAsInt(),
                    object.get("created_at").getAsString());
            temp.add(gm);
        }
        plugin.setGimMessages(temp);
    }

    protected void storeBankTransaction(List<Item> diff) {
        String url = config.api();
        String shared = config.shared();
        url = url + (url.endsWith("/") ? shared : ("/" + shared));
        JsonArray j = new JsonArray();
        for (Item item : diff) {
            JsonObject temp = new JsonObject();
            temp.addProperty("user", client.getLocalPlayer().getName());
            temp.addProperty("item_id", item.getId());
            temp.addProperty("item", itemManager.getItemComposition(item.getId()).getMembersName());
            temp.addProperty("amount", -item.getQuantity());
            j.add(temp);
        }
        Request request = new Request.Builder()
                .url(url)
                .addHeader("token", config.token())
                .addHeader("group_name", config.groupName())
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), j.toString()))
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.debug(e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody body = response.body();
                int code = response.code();
                if (code == 500) {
                    log.debug(body.string());
                }
                getBankTransactions(true);
            }
        });
    }

    public void removeMessage(GIMMessage message) {
        String url = config.api();
        String shared = config.messages();
        url = url + (url.endsWith("/") ? shared : ("/" + shared));
        url = url + "/" + message.getId();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("token", config.token())
                .addHeader("group_name", config.groupName())
                .delete()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.debug(e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody body = response.body();
                int code = response.code();
                if (code == 500) {
                    log.debug(body.string());
                } else {
                    getMessages(true, true);
                }
            }
        });

    }

    public String formatDateTime(String dateTime) {
        String pattern = "HH:mm d/M/y";
        if(!config.dateTimeFormat().toString().isEmpty()){
            pattern = config.dateTimeFormat().toString();
        }
        return LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("u-M-d H:m:s")).format(DateTimeFormatter.ofPattern(pattern));
    }
}
