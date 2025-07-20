package com.quyaz.gimbankex;

import lombok.Value;

import java.math.BigInteger;

@Value
public class GIMMessage {
    private BigInteger id;
    private String message;
    private String sender;
    private BigInteger item_id;
    private int amount;
    private String created_at;
}
