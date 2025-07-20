package com.quyaz.gimbankex;

import lombok.*;

import java.util.List;

@Setter
@Getter
@RequiredArgsConstructor
public class ItemTransaction {
    @NonNull
    String user;
    @NonNull
    String created_at;
    @NonNull
    List<BankItem> items;
    boolean hidden;
}
