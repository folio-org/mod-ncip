package org.folio.ncip;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.extensiblecatalog.ncip.v2.service.BibliographicId;
import org.extensiblecatalog.ncip.v2.service.ItemId;
import org.extensiblecatalog.ncip.v2.service.RequestItemInitiationData;
import org.folio.util.PercentCodec;
import org.folio.util.StringUtil;

/** Resolves RequestItem identifiers, including an isolated legacy RequestId barcode workaround. */
public final class RequestItemIdentifier {
    private static final Logger LOGGER = LogManager.getLogger(RequestItemIdentifier.class);

    private final String value;
    private final boolean titleRequest;
    private final boolean barcode;
    private final boolean legacyRequestIdFallback;

    private RequestItemIdentifier(String value, boolean titleRequest, boolean barcode) {
        this(value, titleRequest, barcode, false);
    }

    private RequestItemIdentifier(String value, boolean titleRequest, boolean barcode, boolean legacyRequestIdFallback) {
        this.value = value;
        this.titleRequest = titleRequest;
        this.barcode = barcode;
        this.legacyRequestIdFallback = legacyRequestIdFallback;
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
            if (titleRequest) {
                throw new FolioNcipException("Bibliographic id missing");
            }
            return legacyRequestIdBarcodeFallback(data);
        }
        return new RequestItemIdentifier(hrid, titleRequest, false);
    }

    /**
     * Legacy compatibility hack: some production ILL clients put the item barcode
     * only in RequestId, omitting ItemId and sending an empty bibliographic identifier.
     * Call only for item requests after both ItemId and the bibliographic HRID are
     * missing or blank; never use this after a failed lookup or for title requests.
     * New clients should send the barcode in ItemId instead.
     */
    private static RequestItemIdentifier legacyRequestIdBarcodeFallback(RequestItemInitiationData data)
            throws FolioNcipException {
        String requestId = data.getRequestId() == null ? null : data.getRequestId().getRequestIdentifierValue();
        if (StringUtils.isBlank(requestId)) {
            throw new FolioNcipException("Item id missing");
        }
        return new RequestItemIdentifier(requestId, false, true, true);
    }

    public boolean isTitleRequest() {
        return titleRequest;
    }

    public String searchUrl(String baseUrl) {
        // Warn at lookup time, since resolve is also called during input validation.
        if (legacyRequestIdFallback) {
            LOGGER.warn("RequestItem is using the legacy RequestId barcode workaround because ItemId and "
                    + "the bibliographic HRID are missing or blank; clients should send the barcode in ItemId");
        }
        String query = (barcode ? "barcode" : "hrid") + "==" + StringUtil.cqlEncode(value);
        return baseUrl + (titleRequest ? Constants.INSTANCE_URL : Constants.ITEM_URL)
                + "?limit=1&query=" + PercentCodec.encode(query);
    }
}
