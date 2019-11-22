# mod-ncip
NISO Circulation Interchange Protocol (NCIP)  support in FOLIO

**DRAFT** in progress...adding code and documentation to GIT







Installing the module



*   The module requires Java 11

Configuration


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
</table>


Property Files

Setup

There should be a set of three property files for each tenant.  The folder structure for the files should duplicate the examples included in the project (in the resources folder) which is:

/the property file location you specify/**tenants**/your tenant id/the 3 property files

When the module is started properties are initialized for each tenant.  It determines tenants by looking in the **tenants **folder.

More about each property file



1. rules.drl - this file contains Drools rules used by the “LookupUser” NCIP service.  It allows you to block users by the amount of fines they owe or the number of checked out items.  If you do not want to use these rules comment them out or delete them.  (Leave the file in place) \
Moving forward this functionality can be removed if it is not necessary or as FOLIO evolves. \

2. toolkit.properties - This module was built using the Extensible Catalog NCIP toolkit.  The toolkit.properties file is a part of that toolkit.  To install and use this module you can probably leave this file as it is.  There is a setting for logging in this file.  There are also settings you might have to change if the XML that is passed into the module fails somehow.   If you add support for additional NCIP services to this module you will have to update this file.  (more about that below) \

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


    

<p id="gdcalert1" ><span style="color: red; font-weight: bold">>>>>>  gd2md-html alert: inline image link here (to images/MOD-NCIP-DRAFT-DOCUMENTATION0.png). Store image on your image server and adjust path/filename if necessary. </span><br>(<a href="#">Back to top</a>)(<a href="#gdcalert2">Next alert</a>)<br><span style="color: red; font-weight: bold">>>>>> </span></p>


![alt_text](images/MOD-NCIP-DRAFT-DOCUMENTATION0.png "image_tooltip")



    Illustrates NCIP message pointing out the agency ID.


    The configuration settings are fairly self-explanatory with the exception of the “instance.custom.identifer.name”.   I used the “instance.custom.identifier.name” so I could search for the item in the inventory module.  It shows up like this and is searchable:


    

<p id="gdcalert2" ><span style="color: red; font-weight: bold">>>>>>  gd2md-html alert: inline image link here (to images/MOD-NCIP-DRAFT-DOCUMENTATION1.png). Store image on your image server and adjust path/filename if necessary. </span><br>(<a href="#">Back to top</a>)(<a href="#gdcalert3">Next alert</a>)<br><span style="color: red; font-weight: bold">>>>>> </span></p>


![alt_text](images/MOD-NCIP-DRAFT-DOCUMENTATION1.png "image_tooltip")



    Illustrates the details of an instance record pointing out the custom identifier used by this module.


    The inventory module is evolving so this may become unnecessary.  For now it expects the configuration value to be there.  The AcceptItem service will not work without it.  Let me know if I should remove it.


    When the first service is called (of the three services that use these configuration settings) the module retrieves all of the UUIDs for these settings and saves them to memory.  The first call to the NCIP services may be slower because of this but it is a one time initialization.


    As you are setting up this module and the values in FOLIO you can use a utility service that validates the values you have set:


    If you are using the edge-ncip module to access the ncip services send a GET request to: [http://okapiurl/circapi/ncipconfigcheck?apikey=yourapikey


    You can access it directly through the NCIP module by sending a GET request to: [http://okapiurl/ncipconfigcheck](http://okapiurl/ncipconfigcheck)


    If the service is able to retrieve a UUID for each of the settings in your configuration file it will send back an “ok” string.  If it cannot locate any of the settings it will return an error message to let you know which setting it couldn’t find.


    ```
    <Problem>
        <message>problem processing NCIP request</message>
        <exception>java.lang.Exception: The lookup of PALCI_NOTREAL could not be found for relais.instance.type.name</exception>
    </Problem>

    ```
