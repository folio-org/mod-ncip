package org.folio.ncip.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.extensiblecatalog.ncip.v2.service.ItemId;
import org.folio.ncip.FolioNcipException;
import org.junit.Test;

public class FolioNcipServiceTest {

    @Test
    public void validateItemIdAcceptsSpecialCharacters() throws Exception {
        FolioNcipService service = new FolioNcipService();
        ItemId itemId = new ItemId();
        itemId.setItemIdentifierValue(")(*)*&&^&$^#%#@!~+<>");

        service.validateItemId(itemId);
    }

    @Test
    public void validateItemIdRejectsBlankValue() throws Exception {
        FolioNcipService service = new FolioNcipService();
        ItemId itemId = new ItemId();
        itemId.setItemIdentifierValue("   ");

        try {
            service.validateItemId(itemId);
            fail("Expected FolioNcipException for blank item id");
        } catch (FolioNcipException e) {
            assertEquals("Item id is invalid", e.getMessage());
        }
    }

    @Test
    public void validateItemIdRejectsOverMaxLength() throws Exception {
        FolioNcipService service = new FolioNcipService();
        ItemId itemId = new ItemId();
        itemId.setItemIdentifierValue("a".repeat(101));

        try {
            service.validateItemId(itemId);
            fail("Expected FolioNcipException for item id length over 100");
        } catch (FolioNcipException e) {
            assertEquals("Item id is invalid", e.getMessage());
        }
    }
}