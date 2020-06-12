package org.folio.ncip.services;




import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;




@RunWith(Suite.class)
@Suite.SuiteClasses({
  LookupUserServiceTests.class,
  AcceptItemServiceTests.class,
  CheckinItemServiceTest.class,
  CheckoutItemServiceTest.class
})
public class UnitTestSuite {
	


	  @BeforeClass
	  public static void before()  {

	  }
	  
	  @AfterClass
	  public static void after() {

	  }

}
