## 1.3.1 2020-06-19
 * Added module permission: automated-patron-blocks.collection.get
## 1.3.0 2020-06-12
 * Physical address included in LookupUser response is configurable
 * OK and BLOCKED message (for LookupUser) configurable
 * Removed check for 'request' block in Checkout service
 * Fix for locations with '/' characters
 * Dynamic types in addresses
 * Include more of physical address
## 1.1.2 2020-04-21
 * Corrected - include email in response when no physical address exists for patron
## 1.1.1 2020-04-02
 * Config values are looked up during each request
 * Corrected due date formatting
 * Added /admin/health
 * Corrected max-fine rule checking
 * Corrected patron block check for requests
## 1.1.0 2020-03-30
 * Healthcheck endpoint changed to /nciphealthcheck
## 1.0.0 2020-03-13
 * MODNCIP-2 - Move configuration from property files to mod-configuration
 * MODNCIP-3 - Add permissionSets to mod-ncip ModuleDescriptor
