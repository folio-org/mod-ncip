package org.folio.ncip;

public class Constants {
	public static final String BLOCKED = "BLOCKED";
	public static final String OK = "OK";
	public static final String AUTH_UID = "Username";
	public static final String AUTH_BARCODE = "Barcode Id";
	public static final String ACTIVE = "ACTIVE";
	public static final String VALUE_KEY = "value";
	public static final String CODE_KEY = "code";
	public static final String UNABLE_TO_INIT_TOOLKIT = "Unable to initialize the default toolkit properties.";
	public static final String BORROWING_BLOCK = "borrowing";
	public static final String REQUEST_BLOCK = "requesting";
	public static final String ACCEPT_ITEM_PROBLEM = "Problem performing AcceptItem";
	public static final String ACCEPT_ITEM_INPUT_PROBLEM = "Problem occurred validating input (user id, item id or pickup location)";
	public static final String LOOKUP_USER_VALIDATION_PROBLEM = "Problem occurred validating user id";
	public static final String GENERAL_ERROR = "General Error";
	public static final String LOOKUP_USER_FAILED = "Lookup User Failed";
	public static final String UNABLE_TO_PROCESS_RESPONSE = "Unable to process response: ";
	public static final String USERID = "Userid";
	public static final String USER_NOT_FOUND = "Did not find a user with that id";
	public static final String PERSONAL = "personal";
	public static final String CAMPUS = "CAMPUS";
	public static final String USER_PROBLEM_ELEMENT = "AuthenticationInput or UserId";
	public static final String CHECK_OUT_PROBLEM = "Problem performing checkout";
	public static final String CHECK_OUT_INPUT_PROBLEM = "Problem occurred validating input (user id or item id)";
	public static final String UNKNOWN_DATA_ELEMENT = "Unknown Data Element";
	public static final String CHECK_IN_PROBLEM = "Problem performing check in";
	public static final String FATAL_ERROR_OCCURED = "An error occured processing this request.  Unable to construct a proper NCIP response with problem element";
	public static final String USER_ID_MISSING = "User id missing";
	public static final String USER_ID_IS_INVALID  = "User id is invalid";
	public static final String ITEM_ID_IS_MISSING = "Item id missing";
	public static final String  ITEM_ID_IS_INVALID = "Item id is invalid";
	public static final String PICKUP_LOCATION_MISSING = "Pickup location is missing";
	
	//MISC
	public static final String NCIP_TOOLKIT_PROPS = "ncip_toolkit_props";
	public static final String APP_XML = "application/xml";
	public static final String SYS_PORT = "port";
	public static final String DEFAULT_PORT = "8081";
	public static final String INIT_PROP_FILE = "init-values.properties";
	public static final String TOOLKIT_PROP_FILE = "toolkit.properties";
	public static final String NCIP_PROP_FILE = "ncip.properties";
	public static final String RULES_FILE = "rules.drl";
	public static final String X_OKAPI_TENANT = "x-okapi-tenant";
	public static final String X_OKAPI_TOKEN = "X-Okapi-Token";
	public static final String X_OKAPI_URL = "X-Okapi-Url";
	public static final String ACCEPT_TEXT = "Accept";
	public static final String CONTENT_TYPE_TEXT = "Content-Type";
	public static final String CONTENT_JSON_AND_PLAIN = "application/json,text/plain";
	public static final String TEXT_PLAIN_STRING = "text/plain";
	public static final String CONTENT_JSON = "application/json";
	public static final String DATE_FORMAT_FOR_CIRC = "yyyy-MM-dd'T'HH:mm:ss";
	public static final String INITIALIZED_PROPERTY = ".ncip.initialized";
	public static final String SCHEME = "Scheme";
	public static final String ITEM_BARCODE = "Item Barcode";
	public static final String USER_BARCODE = "User Barcode";
	public static final String REQUEST_ID = "Request Id";
	public static final String MAX_FINE_AMOUNT = "max-fine-amount";
	public static final String MAX_LOAN_COUNT = "max-loan-count";
	public static final String CONFIGS = "configs";
	public static final String AGENCY_ID = "Agency ID";
	
	
	//URLS
	public static final String CHECK_IN_BY_BARCODE = "/circulation/check-in-by-barcode";
	public static final String CHECK_OUT_BY_BARCODE  = "/circulation/check-out-by-barcode";
	public static final String INSTANCE_URL = "/inventory/instances";
	public static final String HOLDINGS_URL = "/holdings-storage/holdings";
	public static final String ITEM_URL = "/inventory/items";
	public static final String REQUEST_URL = "/circulation/requests";

}