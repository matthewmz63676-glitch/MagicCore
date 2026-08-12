package com.magicstudios.magiccore.modules.securestorage;

import java.util.Base64;

public record StoredItem(int slot, String itemId, int amount, String payloadBase64,
                         boolean container, boolean nestedContainerNonEmpty, boolean customItem) {
    public StoredItem { if(slot<0||itemId==null||itemId.isBlank()||amount<1)throw new IllegalArgumentException("Invalid stored item");Base64.getDecoder().decode(payloadBase64); }
    public int payloadBytes(){return Base64.getDecoder().decode(payloadBase64).length;}
}
