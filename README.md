# mod-ncip
NISO Circulation Interchange Protocol (NCIP)  support in FOLIO

**README DRAFT** in progress...adding code and documentation to GIT







### Installing the module



*   The module requires Java 11

### Configuration


<table>
  <tr>
   <td>config option
   </td>
   <td>type
   </td>
   <td>description
   </td>
  </tr>
  <tr>
   <td>port
   </td>
   <td>int
   </td>
   <td>The port the module binds to.  The default is 8082
   </td>
  </tr>
  <tr>
   <td>prop_files
   </td>
   <td>string
   </td>
   <td>The location of the property files for the module.  More details about the property files below.
   </td>
  </tr>
  <tr>
   <td>okapi_url
   </td>
   <td>string
   </td>
   <td>You may not need this if you are accessing the NCIP module through the edge-ncip module
   </td>
  </tr>
</table>


### Property Files

#### Setup

There should be a set of three property files for each tenant.  The folder structure for the files should duplicate the examples included in the project (in the resources folder) which is:

/the property file location you specify/**tenants**/your tenant id/the 3 property files

When the module is started properties are initialized for each tenant.  It determines tenants by looking in the **tenants **folder.

#### More about each property file



1. rules.drl - this file contains Drools rules used by the “LookupUser” NCIP service.  It allows you to block users by the amount of fines they owe or the number of checked out items.  If you do not want to use these rules, comment them out or delete. (Leave the file in place) \
Moving forward this functionality can be removed if it is not necessary or as FOLIO evolves. \

2. toolkit.properties - This module was built using the Extensible Catalog NCIP toolkit.  The toolkit.properties file is a part of that toolkit.  To install and use this module you can probably leave this file as it is in the example in the resources folder.  There is a setting for logging in this file.  There are also settings you might have to change if the XML that is passed into the module fails somehow.   If you add support for additional NCIP services to this module you will have to update this file.  (more about that below) \

3. ncip.properties - this file contains the settings required by FOLIO to execute three of the four services currently supported in this module (the LookupUser service does not use these settings).  **You will have to set up this configuration file to contain the values your library is using:**

        **#accept item**
        relais.instance.type.name=PALCI
        relais.instance.source=PALCI
        relais.item.material.type.name=PALCI
        relais.item.perm.loan.type.name=PALCI
        relais.item.status.name=Available
        relais.item.perm.location.code=PALCI_LEHIGH
        relais.holdings.perm.location.code=PALCI_LEHIGH
        relais.instance.custom.identifier.name=PALCI Request ID
        **#check out**
        relais.checkout.service.point.code=FAIRCHILD
        **#check in**
        relais.checkin.service.point.code=FAIRCHILD


    The first ‘section’ of each configuration (in the example above ‘relais’) represents an agency ID.  Typically the requestor calling the NCIP service will include an agency ID in the request (example below).  Having the first section of each configuration value tied to a requestors agency ID gives the module more flexibility.  If you have two requesters calling your NCIP services with unique agency IDs you can configure these values differently for each requestor.  Also, the agency ID is not always required so the ncip.properties file contains a default configuration value for each.  If the request does not contain an agency ID the module will use the values assigned to the default configurations.  More than likely your requestors will send an agency ID with the request.  This is just a precaution.


    

![Illustrates NCIP message pointing out the agency ID](docs/images/ncipMessageIllustratesAgencyId.png?raw=true "Illustrates NCIP message pointing out the agency ID")





The configuration settings are fairly self-explanatory with the exception of the “instance.custom.identifer.name”.   I used the “instance.custom.identifier.name” so I could search for the item in the inventory module.  It shows up like this and is searchable:




![Illustrates the details of an instance record pointing out the custom identifier used by this module](docs/images/folioCustomIdentifer.png?raw=true "Illustrates the details of an instance record pointing out the custom identifier used by this module")




 The inventory module is evolving so this may become unnecessary.  For now it expects the configuration value to be there.  The AcceptItem service will not work without it.  Let me know if I should remove it.


When the first service is called (of the three services that use these configuration settings) the module retrieves all of the UUIDs for these settings and saves them to memory.  The first call to the NCIP services may be slower because of this, but it is a one time initialization.


As you are setting up this module and the values in FOLIO you can use a utility service that validates the values you have set in the ncip.properties file:


If you are using the edge-ncip module to access the ncip services send a GET request to: 
[http://okapiurl/circapi/ncipconfigcheck?apikey=yourapikey] (http://okapiurl/circapi/ncipconfigcheck?apikey=yourapikey)


You can access it directly through the NCIP module by sending a GET request to: 
[http://okapiurl/ncipconfigcheck] (http://okapiurl/ncipconfigcheck)


If the service is able to retrieve a UUID for each of the settings in your configuration file it will send back an “ok” string.  If it cannot locate any of the settings it will return an error message to let you know which setting it couldn’t find.


    
    <Problem>
        <message>problem processing NCIP request</message>
        <exception>java.lang.Exception: The lookup of PALCI_NOTREAL could not be found for relais.instance.type.name</exception>
    </Problem>

    
### About the NCIP services
This initial version of the NCIP module supports four of the existing 50ish services in the NCIP protocol.  The endpoint for all services is the same:

POST to .../ncip    (if you are calling the service directly)
POST to ..../circapi/ncip (if you are calling mod-ncip through edge-ncip)

The module determines which service is being called based on the XML passed into the service.
These particular four services were selected because they are required to interact with the D2D software that supports the ILL service that several participating libraries currently use.  Mod-NCIP was written using the Extensible Catalog XC toolkit (more about this below).  This means that adding additional services to this module should mainly involve writing the code that calls the FOLIO web services.  The 'plumbing' that translates the XML to objects and back to XML is built into the toolkit for all of the NCIP messages in the protocol.

#### Supported Services

##### Lookup User
The lookup user service determines whether or not a patron is permitted to borrow.  The response can include details about the patron and will also include a "blocked" or "ok" value to indicate whether or not a patron can borrow.  The service looks for 'blocks' assigned to the patron.  It also looks at the patron 'active' indicator.

This service also uses the Drools rules to help determine the 'blocked' or 'ok' value for the response.  The Drools rules look at the number of items checked out and the amount of outstanding fines.  The rules can be adjusted in the rules.drl file.  If you don't want to use them, delete or comment out the rules, but leave the rest of the file as is.

Sample XML Request
https://github.com/folio-org/mod-ncip/blob/master/docs/sampleNcipMessages/lookupUser.xml

##### Accept Item
The accept item service is called when a requested item arrives from another library.  This service essentially creates the temporary record and places it on hold.
It is probably the most complicated of the existing four service.  It:

1. Creates an instance (which is noted as 'Suppress from discovery')
2. Creates a holding record (which is noted as 'Suppress from discovery')
3. Creates an item (which is noted as 'Suppress from discovery')
4. Places a hold (page request) on the item for the patron

If something goes wrong with any of these four steps it does attempt to delete any instance, holding or item that may have been created along the way. 

In regards to placing the hold, the 'Requests' module has a 'Pickup Preference' field with options - 'hold shelf' or 'delivery'.  The NCIP request contains 'PickupLocation'.  I thought about a configuration that would allow the service to translate a pickup location to Pickup Preference = delivery.  
However, when Pickup Preference = delivery, a patron delivery address is required.  I don't think there is a way for me to derive that location - so I've hardcoded pickup preference to 'hold shelf'.
The "PickupLocation" that is included in the request is recorded in the FOLIO "Pickup Service Point" field.  In our current NCIP implementation using OLE at Lehigh, we support three pickup locations (Linderman, Fairchild and Delivery).  I'm guessing that will stay the same for our FOLIO implemenation.  One of those three will be record in the FOLIO "Pickup Service Point" field.  


Sample XML Request
https://github.com/folio-org/mod-ncip/blob/master/docs/sampleNcipMessages/acceptItem.xml

##### Checkout Item
The checkout item service is called when an item is checked out (either a temporary item being circulated to a local patron or a local item being loaned to another library).
In the 1.0 version of this module, this service does check for blocks on the patron and looks at the active indicator.  If if finds blocks or if the patron is not 'active' the call to the service will fail.  If/when JIRA UXPROD-1683 is completed this check can be removed.

Sampple XML Request
https://github.com/folio-org/mod-ncip/blob/master/docs/sampleNcipMessages/checkOutItem.xml

##### Checkin Item
The checkin item service is called when an item is checked in.  This service can include patron information in the response.  However, if the CheckInItem service is called and there is not an outstanding loan, no patron information will be included in the response. 

Sample XML Request
https://github.com/folio-org/mod-ncip/blob/master/docs/sampleNcipMessages/checkInItem.xml

### About the Extensible Catalog NCIP Toolkit

The eXtensible Catalog (XC) NCIP Toolkit was developed as a stand-alone Web application that would receive NCIP requests, communicate with your ILS (via a ‘connector’) and send back an XML response.

The FOLIO “mod-ncip” module merges the OKAPI microservices framework with the classes from the core XC NCIP Toolkit project.  The core XC NCIP toolkit Java libraries provide functionality to transform incoming XML (from the NCIP request) to objects, shepards each request to a specific service and then transforms objects back to XML for the response. 

The XC NCIP Toolkit supports all of the services in the [NCIP 2 protocol](http://www.ncip.info/uploads/7/1/4/6/7146749/z39-83-1-2012_ncip.pdf).  This means adding new services to the FOLIO module will involve a small amount of setup plus writing the code that will call the FOLIO APIs to process the request.

### Adding support for additional services to mod-ncip
To illustrate the steps required to add support for additional services I've used the "Request Item" service as an example:

#### Step 1: Update the toolkit.properties file
Update the toolkit.properties file with the new service name pointing it to the class that you will create which will process the requests for this service:

![Illustrates updating the toolkit.properties file by adding a configuration for the Request Item Service](docs/images/newServiceToolkit.png?raw=true "Illustrates updating the toolkit.properties file")

#### Step 2: Create the class you configured in step 1
The new class should implement the Toolkit's interface for this service.  In this example your new class would implement the RequestItemService interface.  This means your class is required to have a 'performService' method as illustrated below.
When an NCIP request is received, the toolkit looks at the XML in the body of the request to to decide which class will process it (based on the configuration in the toolkit.properties file).  For example, if a request comes in that contains the RequestItem Service XML, the toolkit will instantiate your implemenation of the RequestItem service and then the performService method will be called.  You can see this in the 'ncipProcess' method in the FolioNcipHelper class.

![Illustrates the new FolioRequestItemService class](docs/images/requestItemService.png?raw=true "Illustrates the new FolioRequestItemService class")

The RequestItemInitiationData (in this example) contains all of the values that were contained in the XML in the body of the request. The RequestItemInitiationData object should contains values like 'requestType' and itemIds.

The performService method is responsible for returning the response data object (in this example RequestItemResponseData).  This is the object that will be transformed into the XML that will be included in the response. 

The RemoteServiceManager (FolioRemoteServiceManager) input parameter (for the performService method) contains the NCIP property values (from the ncip.properties file).  The RemoteServiceManager interface has no methods.  The methods written for this class should be whatever is needed for your implementation.  More about this class from the XC documentation:
 
 "The methods that are written for the RemoteServiceManager implementation class are whatever is needed by the implementations of NCIPService (e.g. LookupItemService, RequestItemService, etc.). It’s certainly possible to put all of the functionality required to access the ILS in the implementations of NCIPService, and that might make sense. But what the RemoteServiceManager provides is a shared object for accessing the ILS, in case you need that to maintain state, cache objects..."
 
 In FOLIO's mod-ncip module, the FolioRemoteServiceManager class is the point where the FOLIO APIs (like check out item) are called.  You can continue this pattern for your new service if you think it makes sense.

#### Step 4: Create a method in the FolioRemoteServiceManager class 
Create a method (or methods) in the FolioRemoteServiceManager class that will take care of the interaction with the FOLIO API.  The FolioRequestItemService (performService method) could then call this method.

You can look at the existing services for examples.  The FolioCheckInItemService calls the FolioRemoteServiceManager 'checkIn' method after some initial validation.  This method passes back the required data for the response.  The FolioCheckInItemService contructs the ResponseData object and returns it.  The toolkit takes care of transforming that into XML.  (see createResponseMessageStream in the FolioNcipHelper class).

![Illustrates the new FolioRemoteServiceManager class](docs/images/remoteServiceManager.png?raw=true "Illustrates FolioRemoteServiceManager class")

#### Additional resources
[http://catalogablog.blogspot.com/2009/03/extensible-catalog-ncip-toolkit.html](http://catalogablog.blogspot.com/2009/03/extensible-catalog-ncip-toolkit.html)

[https://web.archive.org/web/20160416142842/http://code.google.com/p/xcncip2toolkit/wiki/DriverSummary](https://web.archive.org/web/20160416142842/http://code.google.com/p/xcncip2toolkit/wiki/DriverSummary)

[https://web.archive.org/web/20130508170543/http://www.extensiblecatalog.org/news/oclc-contributes-ncip-20-code-xc-ncip-toolkit](https://web.archive.org/web/20130508170543/http://www.extensiblecatalog.org/news/oclc-contributes-ncip-20-code-xc-ncip-toolkit)

[https://code.google.com/archive/p/xcnciptoolkit/](https://code.google.com/archive/p/xcnciptoolkit/)

[https://www.carli.illinois.edu/frequently-asked-questions-about-xc](https://www.carli.illinois.edu/frequently-asked-questions-about-xc)

[https://www.oclc.org/developer/news/2010/developer-collaboration-leads-to-implementation-of-ncip-20.en.html](https://www.oclc.org/developer/news/2010/developer-collaboration-leads-to-implementation-of-ncip-20.en.html) 
