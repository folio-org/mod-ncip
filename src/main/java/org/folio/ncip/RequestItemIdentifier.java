package org.folio.ncip;

import org.apache.commons.lang.StringUtils;
import org.extensiblecatalog.ncip.v2.service.BibliographicId;
import org.extensiblecatalog.ncip.v2.service.ItemId;
import org.extensiblecatalog.ncip.v2.service.RequestItemInitiationData;
import org.folio.util.PercentCodec;
import org.folio.util.StringUtil;

/** Resolves RequestItem identifiers without treating the request ID as an item ID. */
public final class RequestItemIdentifier {
    private final String value;
    private final boolean titleRequest;
    private final boolean barcode;

    private RequestItemIdentifier(String value, boolean titleRequest, boolean barcode) {
        this.value = value;
        this.titleRequest = titleRequest;
        this.barcode = barcode;
    }

    public static RequestItemIdentifier resolve(RequestItemInitiationData data) throws FolioNcipException {
        String scope = data.getRequestScopeType() == null ? null : data.getRequestScopeType().getValue();
        boolean titleRequest = "Title".equalsIgnoreCase(scope) || "Bibliographic Item".equalsIgnoreCase(scope);

        if (!titleRequest && data.getItemIds() != null && !data.getItemIds().isEmpty()) {
            ItemId itemId = data.getItemId(0);
            if (itemId != null && StringUtils.isNotBlank(itemId.getItemIdentifierValue())) {
                return new RequestItemIdentifier(itemId.getItemIdentifierValue(), false, true);
            }
        }

        BibliographicId bibliographicId = data.getBibliographicIds() == null || data.getBibliographicIds().isEmpty()
                ? null : data.getBibliographicId(0);
        String hrid = bibliographicId == null || bibliographicId.getBibliographicRecordId() == null
                ? null : bibliographicId.getBibliographicRecordId().getBibliographicRecordIdentifier();
        if (StringUtils.isBlank(hrid)) {
            throw new FolioNcipException(titleRequest ? "Bibliographic id missing" : "Item id missing");
        }
        return new RequestItemIdentifier(hrid, titleRequest, false);
    }

    public boolean isTitleRequest() {
        return titleRequest;
    }

    public String searchUrl(String baseUrl) {
        String query = (barcode ? "barcode" : "hrid") + "==" + StringUtil.cqlEncode(value);
        return baseUrl + (titleRequest ? Constants.INSTANCE_URL : Constants.ITEM_URL)
                + "?limit=1&query=" + PercentCodec.encode(query);
    }
}
