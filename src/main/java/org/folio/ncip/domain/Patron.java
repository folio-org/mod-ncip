package org.folio.ncip.domain;

import java.util.ArrayList;
import java.util.List;


public class Patron {
	private List<Loan> loans = new ArrayList<Loan>();
	private List<Account> accounts = new ArrayList<Account>();
	private boolean canBorrow = true;
	private String name="test";

	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean canBorrow() {
		return canBorrow;
	}
	public void canBorrow(boolean canBorrow) {
		System.out.println("********IN SET CAN BORROW"); //TODO: REMOVE
		this.canBorrow = canBorrow;
	}
	public List<Loan> getLoans() {
		return loans;
	}
	public void setLoans(List<Loan> loans) {
		this.loans = loans;
	}
	public List<Account> getAccounts() {
		return accounts;
	}
	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
	}
	
	public Double getAllCharges() {
		System.out.println("************GET ALL CHARGES CALLED"); //TODO: REMOVE
		Double total = (double) 0;
		java.util.Iterator<Account> loanIterator = getAccounts().iterator();
		while (loanIterator.hasNext()) {
			Account account = loanIterator.next();
			total += account.getRemaining();
		}
		return total;
	}
	
	public Integer getLoanCount() {
		System.out.println("************GET LOAN COUNT CALLED"); //TODO: REMOVE
		return getLoans().size();
		
	}
	
	public Double getOverdueFineAmount() {
		return 400.10;
	}
	
	
	
	
	
}
