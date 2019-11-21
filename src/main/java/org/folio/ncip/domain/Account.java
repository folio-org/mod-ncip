package org.folio.ncip.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

//represents fees/fines
public class Account {

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("remaining")
    private Double remaining;

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public Double getRemaining() {
		return remaining;
	}

	public void setRemaining(Double remaining) {
		this.remaining = remaining;
	}
    
    
    
	
}
