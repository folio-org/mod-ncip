## 1.15.0 2024-06-10
 * [MODNCIP-69](https://folio-org.atlassian.net/browse/MODNCIP-69) API version update (mod-inventory-storage)
 * [MODNCIP-67](https://folio-org.atlassian.net/browse/MODNCIP-67) mod-ncip v1.14.X asserts compatibility with circulation interface v12 and 13 but actually requires v14
## 1.14.5 2024-04-04
 * [MODNCIP-65](https://folio-org.atlassian.net/browse/MODNCIP-65) if preferredFirstName is present use in lookupUserResponse
## 1.14.4 2023-10-25
 * [MODNCIP-63](https://issues.folio.org/browse/MODNCIP-63) Give user.email.type a default value
## 1.14.3 2023-10-16
 * [MODNCIP-62](https://issues.folio.org/browse/MODNCIP-62) Upgrade dependencies (Spring, Vert.x, ...) for Poppy
## 1.14.1 2023-10-04
 * Fix circulation interface version
## 1.14.0 2023-10-04
 * [MODNCIP-59](https://issues.folio.org/browse/MODNCIP-59) Update to Java 17 mod-ncip
 * [MODNCIP-61](https://issues.folio.org/browse/MODNCIP-61) config option for ElectronicAddressType
 * [MODNCIP-60](https://issues.folio.org/browse/MODNCIP-60) RequestItemService placeholder
## 1.13.1 2023-04-05
* [MODNCIP-55](https://issues.folio.org/browse/MODNCIP-55)- alternate user lookup service fix
## 1.13.0 2023-02-23
 * [MODNCIP-49](https://issues.folio.org/browse/MODNCIP-49) -CQL and URL injection
 * Updated logging - common logging
## 1.12.2 2022-12-01
 * [MODNCIP-46](https://issues.folio.org/browse/MODNCIP-46) - Replace slf4j-log4j12 by log4j-slf4j-impl fixing vulns
## 1.12.1 2022-11-22
 * [MODNCIP-47](https://issues.folio.org/browse/MODNCIP-47) - Create holdings record with holdings source ID
 * [MODNCIP-50](https://issues.folio.org/browse/MODNCIP-50) - support additional fields for user search
## 1.12.0 2022-10-24
 * [MODNCIP-44](https://issues.folio.org/browse/MODNCIP-44) - Support inventory 12.0 in ModuleDescriptor "requires"
## 1.11.1 2022-08-29
 * [MODNCIP-39](https://issues.folio.org/browse/MODNCIP-39) - Dependency vulnerabilities
## 1.11.0 2022-06-24
 * [MODNCIP-40](https://issues.folio.org/browse/MODNCIP-40) - mod-ncip - Morning Glory 2022 R2 - Vert.x 3.9.9/v4 upgrade
## 1.10.0 2022-02-24
 * [MODNCIP-33](https://issues.folio.org/browse/MODNCIP-33) - Adapt mod-ncip to the request schema changes
 * [MODNCIP-37](https://issues.folio.org/browse/MODNCIP-37) - Support circulation interface v13
 * [MODNCIP-27](https://issues.folio.org/browse/MODNCIP-27) - Resource leak, input stream not closed
## 1.9.0 2021-10-11
 * [MODNCIP-23](https://issues.folio.org/browse/MODNCIP-23) - align dependency versions affected by Inventory's Optimistic Locking
## 1.8.0 2021-06-15
 * [MODNCIP-21](https://issues.folio.org/browse/MODNCIP-21) - Update circulation interface dependency to support v11
## 1.7.0 2021-03-17
 * Correcting version number for R1 2021
 * README updates
## 1.6.4 2021-03-05
 * [MODNCIP-15](https://issues.folio.org/browse/MODNCIP-15) - specify UTF-8 in POST
 * Default timeout changed to 30 secs (3 was too short - implementers seeing timeouts when system is slow).  As before, the default can be modified on startup.
 * Attempt checkin if checkout fails due to timeout (for convenience).  Only happens when the system gets extremely slow.
 * Removed 'Problem element unknown' from the response when the problem doesn't relate to a specific element.  This was confusing.
## 1.6.3 2020-11-11
 * Putting back patron group lookup (was unintentionally removed)
## 1.6.2 2020-11-09
 * Fix for new dueDate format ParseException
 * Removed loanDate from check-out-by-barcode (not needed)
## 1.6.1 2020-10-23
 * Correct automated patron block endpoint
 * Checkout should check for automated patron blocks
## 1.6.0 2020-10-14
 * [MODNCIP-9](https://issues.folio.org/browse/MODNCIP-9) <br>
Use automated patron blocks for the LookupUser service
## 1.5.0 2020-10-01
 * [MODNCIP-8](https://issues.folio.org/browse/MODNCIP-8) <br>
 Upgrade to openJdk11
## 1.4.2 2020-08-25
 * Added timeouts and try/catch to API calls
## 1.3.2 2020-06-26
 * Added module permission: addresstypes.collection.get
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
