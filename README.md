# mod-ncip

Copyright (C) 2019-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

NISO Circulation Interchange Protocol (NCIP)  support in FOLIO


## Preparation
1. The NCIP module requires a FOLIO user with the following permissions:
```
	ncip.all
	inventory-storage.items.collection.get
	ui-circulation.settings.overdue-fines-policies
	ui-circulation.settings.lost-item-fees-policies
	automated-patron-blocks.collection.get
	circulation-storage.circulation-rules.get
	manualblocks.collection.get
    
```
### IMPORTANT NOTE ABOUT FOLIO USER - It has to be assigned a patron group. There is an issue with 'create item' which requires the user to be assigned a patron group.

2. If you will be exposing this service externally and will be using the [edge-ncip module](https://github.com/folio-org/edge-ncip), you will need to setup an API key as described [in the readme file of the edge-common module](https://github.com/folio-org/edge-common)

3. There are settings that have to be setup in mod-configuration for the NCIP services to work (more about that below).  The values assigned to these settings must exist in FOLIO.  This is because FOLIO requires specific values to be set when actions occur.  For example, the AcceptItem service creates an instance.  The NCIP module has to know what instance.type.name to use. Here is a list of the configurations you will need to establish values for in FOLIO:

    * (1) instance.type.name   (Settings > Inventory > Instances > Resource Type)
    * (2) instance.source
    * (3) item.material.type.name
    * (4) item.perm.loan.type.name
    * (5) item.status.name
    * (6) item.perm.location.code
    * (7) holdings.perm.location.code
    * (8) instance.custom.identifier.name (Settings -> Inventory -> Instances -> Resource Identifier Types)
    * (9) checkout.service.point.code
    * (10) checkin.service.point.code
    * (11) response.includes.physical.address (optional - will default to false. For LookupUser response)
    * (12) user.priv.ok.status (optional - will default to "ACTIVE")
    * (13) user.priv.blocked.status (optional - will default to "BLOCKED")
    * (14) holdings.source.name (optional - will default to "FOLIO")
    * (15) user.email.type (optional - will default to "electronic mail address" For LookupUser response)
    * (16) cancel.request.reason.name Reason for request cancellation when different item is checkout (Settings -> Circulation -> Request cancellation reasons)
    * (17) cancel.request.reason.patron.name Reason for request cancellation when patron did not checkout item (Settings -> Circulation -> Request cancellation reasons)
    * (18) request.note.name Request note name. Default value "ILL note"
    * (19) request.note.enabled Request note enabled. Will add ILL request ID to loan and ILS request. Default value "false"

Notes 
* You can assign different values to these settings per Agency ID used in the NCIP requests.  This approach lets you setup different values for different Agency IDs.  For example, if Relais calls your NCIP server with the Agency ID of 'Relais' you can configure values for that agency.  If ReShare calls your NCIP server using a different Agency ID, you can set up different configuration values to be used for ReShare requests.  These settings have to exist for each Agency ID that will be used in the NCIP requests.


* The screen prints below illustrate how these values are used by the NCIP module on the instance, holdings and item records:

![Illustrates how the NCIP property values will be used on the instance record](docs/images/instanceNcipExample.png?raw=true "Illustrates how the NCIP property values will be used on the instance record")

![Illustrates how the NCIP property values will be used on the item record](docs/images/ncipItemExample.png?raw=true "Illustrates how the NCIP property values will be used on the item record")

 
## Installing the module


This module does have a companion 'edge' module - edge-ncip - that can be used to expose this service to external applications.
https://github.com/folio-org/edge-ncip


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
   <td>The port the module binds to.  The default is 8081
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
  <tr>
   <td>service_manager_timeout_ms
   </td>
   <td>int
   </td>
   <td>Timeout setting in milliseconds that mod-ncip will use when calling FOLIO APIs (e.g. checkout-item-by-barcode).  Defaults to 30000.
   </td>
  </tr>
</table>


## mod-configuration setup

This document is a shortcut for bare minimum initial setup/testing for the DIKU tenant.  It includes step-by-step instructions with references to Python scripts for DIKU tenant reference values on the snapshot image:
https://docs.google.com/document/d/1wwaAaMXg6L_V5hEjJU72rYTGdBdF2Zzk_iTD0x4UeHU/edit

Note: The instructions below refer to adding entries to mod-configuration.  This is an example of how you could do that:

```java
curl -X POST \
 http://localhost:9130/configurations/entries \
 -H 'Content-Type: application/json' \
 -H 'X-Okapi-Tenant: <tenant>' \
 -H 'x-okapi-token: <token>' \
 -d
   '{
     "module": "NCIP",
     "configName": "Relais",
     "code": "instance.type.name",
     "description": "optional description",
     "default": true,
     "enabled": true,
     "value": "RESHARE"
   }'
```

There are three types of settings that can exist in mod-configuration for the NCIP module:

### Required Configurations:
1) NCIP properties: most of these are the settings required for the NCIP services to work.  See explanation above in the 'preparation' section of this README file.
### Optional Configurations
2) XC NCIP Toolkit properties:  While there are examples of these properties below YOU DO NOT HAVE TO SET THEM.  The NCIP module will use default values.  You can override them in mod-configuration if you need to.


#### NCIP Properties 

| MODULE    | configName (the AgencyID) | code                               |  value  (examples) |   
|-----------|:-------------------------:|:-----------------------------------|-------------------:|		
| NCIP      |          Relais           | instance.type.name                 |            RESHARE |	
| NCIP      |          Relais           | instance.source                    |            RESHARE |	
| NCIP      |          Relais           | item.material.type.name            |            RESHARE |	
| NCIP      |          Relais           | item.perm.loan.type.name           |            RESHARE |	
| NCIP      |          Relais           | item.status.name                   |          Available |
| NCIP      |          Relais           | item.perm.location.code            | RESHARE_DATALOGISK |	
| NCIP      |          Relais           | holdings.perm.location.code        | RESHARE_DATALOGISK |	
| NCIP      |          Relais           | instance.custom.identifier.nam     | ReShare Request ID |		
| NCIP      |          Relais           | checkout.service.point.code        |             online |		
| NCIP      |          Relais           | checkin.service.point.code         |             online |		
| NCIP      |          Relais           | response.includes.physical.address |              false |
| NCIP      |          Relais           | user.priv.ok.status                |                 OK |
| NCIP      |          Relais           | user.priv.blocked.status           |            BLOCKED |
| NCIP      |          Relais           | cancel.request.reason.name         | Item Not Available |		
| NCIP      |          Relais           | cancel.request.reason.patron.name  | Item Not Available |		
| NCIP      |          Relais           | request.note.name                  |           ILL note |		
| NCIP      |          Relais           | request.note.enabled               |              false |		


You will need a set of these settings in mod-configuration for each individual Agency ID making NCIP requests.  Example of an AgencyID in an NCIP request:
   

![Illustrates NCIP message pointing out the agency ID](docs/images/ncipMessageIllustratesAgencyId.png?raw=true "Illustrates NCIP message pointing out the agency ID")

#### NCIP Toolkit properties

| MODULE   | configName   |   code          | value  (examples) |   
| --------|:-------------:| :-----------------------------|:------------|		
| NCIP    | toolkit 	| ToolkitConfiguration.PropertiesFileTitle 		| My new value  |	
| NCIP    | toolkit     | TranslatorConfiguration.LogMessages 			| false             |	
| NCIP    | toolkit      | NCIPServiceValidatorConfiguration.AddDefaultNamespaceURI | true     |	
| NCIP    | toolkit 	| RemoteServiceManager.Class 		| org.folio.ncip.FolioRemoteServiceManager |	

For the full list of NCIP toolkit properties see: /src/main/resources/toolkit.properties


Your new configuration values will be picked up during the next NCIP request.

As you are setting up mod-nicp, the NCIP properties and the settings values in FOLIO, you can use this utility service to validate the NCIP property values you have set (it attempts to look up each value you have configured):

* To validate your configuration settings --> send a GET request to //yourokapiendpoint/ncipconfigcheck

If the service is able to retrieve a UUID for each of the settings it will send back an “ok” string.  If it cannot locate any of the settings it will return an error message to let you know which setting it couldn’t find.

    
    <Problem>
        <message>problem processing NCIP request</message>
        <exception>java.lang.Exception: The lookup of PALCI_NOTREAL could not be found for relais.instance.type.name</exception>
    </Problem>
## FAQ
<b>The NCIP response says read timeout</b>
<br>
In previous versions, the default timeout (which tells mod-ncip how long to wait for a repsonse from the FOLIO services it calls) was set to 3000 (3 seconds).  This occassionally caused timeouts when the entire system was slow but could be fixed by increasing the timeout setting mentioned in the Configuration section above (when the module is started (java -jar ...) add this configuration: -Dservice_manager_timeout_ms=6000).  However, as of 03/2021 it defaults to 30000 (30 seconds) because 3000 (3 seconds) was not enough at times.  This setting can still be adjusted with the '-Dservice_manager_timeout_ms' setting.  It just defaults to 30 seconds now instead of 3 to help implementers avoid ever running into the timeout.

    
## About the NCIP services
This initial version of the NCIP module supports four of the existing 50ish services in the NCIP protocol.  The endpoint for all of the services is the same:

POST to http://yourokapiendoint/ncip    (if you are calling the mod-ncip directly)

POST to http://youredgencipendpoint/ncip/yourapikey (if you are calling mod-ncip through edge-ncip)
or http://youredgencipendpoint/ncip?apikey=yourapikey

The module determines which service is being called based on the XML passed into the service.
These particular four services were selected because they are required to interact with the D2D software that supports the ILL service that several participating libraries currently use.  Mod-NCIP was written using the Extensible Catalog (XC) NCIP toolkit (more about this below).  This means that adding additional services to this module should mainly involve writing the code that calls the FOLIO web services.  The 'plumbing' that translates the XML to objects and back to XML is built into the toolkit for all of the NCIP messages in the protocol.

#### Supported Services

##### Lookup User
The lookup user service determines whether or not a patron is permitted to borrow.  The response can include details about the patron and will also include a "BLOCKED" or "ACTIVE" value to indicate whether or not a patron can borrow.  The service looks for manual and automated 'blocks' assigned to the patron.  It also looks at the patron 'active' indicator.


Sample XML Request:

https://github.com/folio-org/mod-ncip/blob/master/docs/sampleNcipMessages/lookupUser.xml

##### Accept Item
The accept item service is called when a requested item arrives from another library.  This service essentially creates the temporary record and places it on hold.
It is probably the most complicated of the existing four service.  It:

1. Creates an instance (which is set as 'Suppress from discovery')
2. Creates a holding record (which is set as 'Suppress from discovery')
3. Creates an item (which is set as 'Suppress from discovery')
4. Places a hold (page request) on the item for the patron

If something goes wrong with any of these four steps it does attempt to delete any instance, holding or item that may have been created along the way. 

In regards to placing the hold, the 'Requests' module has a 'Pickup Preference' field with options - 'hold shelf' or 'delivery'.  The NCIP request contains 'PickupLocation'.  I thought about a configuration that would allow the service to translate a pickup location to Pickup Preference = delivery.  
However, when Pickup Preference = delivery, a patron delivery address is required.  I don't think there is a way for me to derive that location - so I've hardcoded pickup preference to 'hold shelf'.
The "PickupLocation" that is included in the request is recorded in the FOLIO "Pickup Service Point" field.  For example...in our current NCIP implementation using OLE at Lehigh, we support three pickup locations (Linderman, Fairchild and Delivery).  I'm guessing that will stay the same for our FOLIO implemenation.  One of those three will be recorded in the FOLIO "Pickup Service Point" field.  


Sample XML Request:

https://github.com/folio-org/mod-ncip/blob/master/docs/sampleNcipMessages/acceptItem.xml

##### Checkout Item
The checkout item service is called when an item is checked out (either a temporary item being circulated to a local patron or a local item being loaned to another library).
In the 1.0 version of this module, this service does check for blocks on the patron and looks at the active indicator.  If if finds blocks or if the patron is not 'active' the call to the service will fail.  If/when JIRA UXPROD-1683 is completed this check can be removed.

Sample XML Request:

https://github.com/folio-org/mod-ncip/blob/master/docs/sampleNcipMessages/checkOutItem.xml

##### Checkin Item
The checkin item service is called when an item is checked in.  This service can include patron information in the response.  However, if the CheckInItem service is called and there is not an outstanding loan, no patron information will be included in the response. 

Sample XML Request:

https://github.com/folio-org/mod-ncip/blob/master/docs/sampleNcipMessages/checkInItem.xml

### About the Extensible Catalog NCIP Toolkit

The eXtensible Catalog (XC) NCIP Toolkit was developed as a stand-alone Web application that would receive NCIP requests, communicate with your ILS (via a ‘connector’) and send back an XML response.

The FOLIO “mod-ncip” module merges the OKAPI microservices framework with the classes from the core XC NCIP Toolkit project.  The core XC NCIP toolkit Java libraries provide functionality to transform incoming XML (from the NCIP request) to objects, shepards each request to a specific service and then transforms objects back to XML for the response. 

The XC NCIP Toolkit supports all of the services in the [NCIP 2 protocol](http://www.ncip.info/uploads/7/1/4/6/7146749/z39-83-1-2012_ncip.pdf).  This means adding new services to the FOLIO module will involve a small amount of setup plus writing the code that will call the FOLIO APIs to process the request.

### Adding support for additional services to mod-ncip
To illustrate the steps required to add support for additional services I've used the "Request Item" service as an example:

#### Step 1: Update the toolkit.properties file
Update the toolkit.properties file with the new service name pointing it to the class that you will create which will process the requests for this service:

Note: Correct capitalization is important for this configuration.  CheckinItemService did not work.  CheckInItemService worked and it took a bit to figure out what the problem was.

![Illustrates updating the toolkit.properties file by adding a configuration for the Request Item Service](docs/images/newServiceToolkit.png?raw=true "Illustrates updating the toolkit.properties file")

These are the default values used for the NCIP Toolkit configuration.

#### Step 2: Create the class you configured in step 1
The new class should implement the Toolkit's interface for this service.  In this example your new class would implement the RequestItemService interface.  This means your class is required to have a 'performService' method as illustrated below.
When an NCIP request is received, the toolkit looks at the XML in the body of the request to to decide which class will process it (based on the toolkit configuraiton).  For example, if a request comes in that contains the RequestItem Service XML, the toolkit will instantiate your implemenation of the RequestItem service and then the performService method will be called.  You can see this in the 'ncipProcess' method in the FolioNcipHelper class.

![Illustrates the new FolioRequestItemService class](docs/images/requestItemService.png?raw=true "Illustrates the new FolioRequestItemService class")

The RequestItemInitiationData (in this example) contains all of the values that were contained in the XML in the body of the request. The RequestItemInitiationData object should contains values like 'requestType' and 'itemIds'.

The performService method is responsible for returning the response data object (in this example RequestItemResponseData).  This is the object that will be transformed into the XML that will be included in the response. 

The RemoteServiceManager interface has no methods.  The methods written for this class should be whatever is needed for your implementation.  More about this class from the XC documentation:
 
 "The methods that are written for the RemoteServiceManager implementation class are whatever is needed by the implementations of NCIPService (e.g. LookupItemService, RequestItemService, etc.). It’s certainly possible to put all of the functionality required to access the ILS in the implementations of NCIPService, and that might make sense. But what the RemoteServiceManager provides is a shared object for accessing the ILS, in case you need that to maintain state, cache objects..."
 
 In FOLIO's mod-ncip module, the FolioRemoteServiceManager class is the point where the FOLIO APIs (like check out item) are called.  You can continue this pattern for your new service if you think it makes sense.  The example above illustrates the request item service calling the RemoteServiceManagers 'requestItem' method.  The FolioRemoteServiceManager class contains all of the NCIP property values.
 
 

#### Step 4: Create a method in the FolioRemoteServiceManager class 
Create a method (or methods) in the FolioRemoteServiceManager class that will take care of the interaction with the FOLIO API.  The FolioRequestItemService (performService method) could then call this method.

You can look at the existing services for examples.  The FolioCheckInItemService calls the FolioRemoteServiceManager 'checkIn' method after some initial validation.  This method passes back the required data for the response.  The FolioCheckInItemService contructs the ResponseData object and returns it.  The toolkit takes care of transforming that into XML.  (see createResponseMessageStream in the FolioNcipHelper class).

![Illustrates the new FolioRemoteServiceManager class](docs/images/remoteServiceManager.png?raw=true "Illustrates FolioRemoteServiceManager class")

#### XC NCIP Toolkit - Additional resources
[http://catalogablog.blogspot.com/2009/03/extensible-catalog-ncip-toolkit.html](http://catalogablog.blogspot.com/2009/03/extensible-catalog-ncip-toolkit.html)

[https://web.archive.org/web/20160416142842/http://code.google.com/p/xcncip2toolkit/wiki/DriverSummary](https://web.archive.org/web/20160416142842/http://code.google.com/p/xcncip2toolkit/wiki/DriverSummary)

[https://web.archive.org/web/20130508170543/http://www.extensiblecatalog.org/news/oclc-contributes-ncip-20-code-xc-ncip-toolkit](https://web.archive.org/web/20130508170543/http://www.extensiblecatalog.org/news/oclc-contributes-ncip-20-code-xc-ncip-toolkit)

[https://code.google.com/archive/p/xcnciptoolkit/](https://code.google.com/archive/p/xcnciptoolkit/)

[https://www.carli.illinois.edu/frequently-asked-questions-about-xc](https://www.carli.illinois.edu/frequently-asked-questions-about-xc)

[https://www.oclc.org/developer/news/2010/developer-collaboration-leads-to-implementation-of-ncip-20.en.html](https://www.oclc.org/developer/news/2010/developer-collaboration-leads-to-implementation-of-ncip-20.en.html) 

### More about the toolkit settings
https://github.com/eXtensibleCatalog/NCIP2-Toolkit/wiki/GeneralConfiguration
https://github.com/moravianlibrary/xcncip2toolkit/blob/master/connectors/aleph/22/trunk/web/src/main/resources/toolkit.properties

## Additional information

### Issue tracker

See project [MODNCIP](https://issues.folio.org/browse/MODNCIP)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).
<br>
Related issue: [due date on the UI doesn't match the api](https://issues.folio.org/browse/UICHKOUT-670)

### ModuleDescriptor

See the built `target/ModuleDescriptor.json` for the interfaces that this module
requires and provides, the permissions, and the additional module metadata.

### Code analysis

[SonarQube analysis](https://sonarcloud.io/dashboard?id=org.folio%3Amod-ncip).

### Download and configuration

The built artifacts for this module are available.
See [configuration](https://dev.folio.org/download/artifacts) for repository access,
and the [Docker image](https://hub.docker.com/r/folioorg/mod-ncip/).

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)

