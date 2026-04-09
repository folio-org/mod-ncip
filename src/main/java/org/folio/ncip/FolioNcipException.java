package org.folio.ncip;

public class FolioNcipException extends Exception {

	private static final long serialVersionUID = 1L;

	public FolioNcipException(String message) {
		super(message);
	}

	public FolioNcipException(Throwable cause) {
		super(cause);
	}

	public FolioNcipException(String message, Throwable cause) {
		super(message, cause);
	}

}
