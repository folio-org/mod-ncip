import os.path
from git import *
import git, os, shutil
import shutil
import stat
import subprocess
from random import randrange
from pprint import pprint
import json
import sys
import requests
import psycopg2
import time
import urllib.request, json 
from collections import OrderedDict


#url = "https://folio-testing-okapi.aws.indexdata.com"
url = "https://folio-snapshot-load-okapi.aws.indexdata.com"


headers = {"x-okapi-tenant": "diku", "Content-type": "application/json"}

#AUTHENTICATE
user = {}
user['username'] = "diku_admin"
user['password'] = "admin"
user['tenant'] = "diku"
the_data = json.dumps(user)
print(the_data)
response = requests.post(url + "/authn/login",the_data,headers=headers)
token = response.headers['x-okapi-token']
print(response)


#CODE--> configuration (e.g. instance type)
#CONFIGNAME --> AGENCY ID (E.G relais)
#MODULE --> NCIP

headers = {"x-okapi-tenant": "diku", "Content-type": "application/json","x-okapi-token":token}

#SET OF 10 CONFIGURATION VALUES *FOR EACH AGENCY ID*

configuration = {}
configuration['configName'] = "ReShare" #AGENCY ID
configuration['code'] = "instance.type.name"
configuration['value'] = "RESHARE"
configuration['module'] = "NCIP"
the_data = json.dumps(configuration)
print(the_data)
response = requests.post(url + "/configurations/entries",the_data,headers=headers)
print(response)

configuration = {}
configuration['configName'] = "ReShare" #AGENCY ID
configuration['code'] = "instance.source"
configuration['value'] = "RESHARE"
configuration['module'] = "NCIP"
the_data = json.dumps(configuration)
print(the_data)
response = requests.post(url + "/configurations/entries",the_data,headers=headers)
print(response)

configuration = {}
configuration['configName'] = "ReShare" #AGENCY ID
configuration['code'] = "item.material.type.name"
configuration['value'] = "RESHARE MATERIAL"
configuration['module'] = "NCIP"
the_data = json.dumps(configuration)
print(the_data)
response = requests.post(url + "/configurations/entries",the_data,headers=headers)
print(response)

configuration = {}
configuration['configName'] = "ReShare" #AGENCY ID
configuration['code'] = "item.perm.loan.type.name"
configuration['value'] = "RESHARE LOAN"
configuration['module'] = "NCIP"
the_data = json.dumps(configuration)
print(the_data)
response = requests.post(url + "/configurations/entries",the_data,headers=headers)
print(response)

configuration = {}
configuration['configName'] = "ReShare" #AGENCY ID
configuration['code'] = "item.status.name"
configuration['value'] = "Available"
configuration['module'] = "NCIP"
the_data = json.dumps(configuration)
print(the_data)
response = requests.post(url + "/configurations/entries",the_data,headers=headers)
print(response)

configuration = {}
configuration['configName'] = "ReShare" #AGENCY ID
configuration['code'] = "item.perm.location.code"
configuration['value'] = "RESHARE_DATALOGISK"
configuration['module'] = "NCIP"
the_data = json.dumps(configuration)
print(the_data)
response = requests.post(url + "/configurations/entries",the_data,headers=headers)
print(response)

configuration = {}
configuration['configName'] = "ReShare" #AGENCY ID
configuration['code'] = "holdings.perm.location.code"
configuration['value'] = "RESHARE_DATALOGISK"
configuration['module'] = "NCIP"
the_data = json.dumps(configuration)
print(the_data)
response = requests.post(url + "/configurations/entries",the_data,headers=headers)
print(response)

configuration = {}
configuration['configName'] = "ReShare" #AGENCY ID
configuration['code'] = "instance.custom.identifier.name"
configuration['value'] = "ReShare Request ID"
configuration['module'] = "NCIP"
the_data = json.dumps(configuration)
print(the_data)
response = requests.post(url + "/configurations/entries",the_data,headers=headers)
print(response)

configuration = {}
configuration['configName'] = "ReShare" #AGENCY ID
configuration['code'] = "checkout.service.point.code"
configuration['value'] = "Online"
configuration['module'] = "NCIP"
the_data = json.dumps(configuration)
print(the_data)
response = requests.post(url + "/configurations/entries",the_data,headers=headers)
print(response)

configuration = {}
configuration['configName'] = "ReShare" #AGENCY ID
configuration['code'] = "checkin.service.point.code"
configuration['value'] = "Online"
configuration['module'] = "NCIP"
the_data = json.dumps(configuration)
print(the_data)
response = requests.post(url + "/configurations/entries",the_data,headers=headers)
print(response)
