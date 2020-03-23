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
print(response)
token = response.headers['x-okapi-token']



headers = {"x-okapi-tenant": "diku", "Content-type": "application/json","x-okapi-token":token}

#CREATE LOCATION
location = {}
servicepoints = []
location['id'] = "801f9b00-af0b-407c-9db1-f6aac50e9e56"
location['name'] = "ReShare Shelf"
location['code'] = "RESHARE_DATALOGISK"
location['discoveryDisplayName'] = "Location for items borrowed via ReShare program"
location['isActive'] = 'true'
location['institutionId'] = "40ee00ca-a518-4b49-be01-0638d0a4ac57"
location['campusId'] = "62cf76b7-cca5-4d33-9217-edf42ce1a848"
location['libraryId'] = "5d78803e-ca04-4b4a-aeae-2c63b924518b"
location['primaryServicePoint'] = "3a40852d-49fd-4df2-a1f9-6e2641a6e91f"
servicepoints.append("3a40852d-49fd-4df2-a1f9-6e2641a6e91f")
location['servicePointIds'] = servicepoints
the_data = json.dumps(location)
print(the_data)
response = requests.post(url + "/locations",the_data,headers=headers)
print(response)
print(response.content)

#CREATE INSTANCE TYPE
instanceType = {}
instanceType['id'] = "660a0ba9-b951-4391-b001-bcf6062def23"
instanceType['name'] = "RESHARE"
instanceType['code'] = "RESHARE"
instanceType['source'] = "reshare"
the_data = json.dumps(instanceType)
print(the_data)
response = requests.post(url + "/instance-types",the_data,headers=headers)
print(response)
print(response.content)

#CREATE LOAN TYPE
loanType = {}
loanType['id'] = "35672bd3-6caa-4f37-a138-23a68082c036"
loanType['name'] = "RESHARE LOAN"
the_data = json.dumps(loanType)
print(the_data)
response = requests.post(url + "/loan-types",the_data,headers=headers)
print(response)
print(response.content)

#CREATE MATERIAL TYPE
materialTypes = {}
materialTypes['id'] = "6d64513d-d08f-478a-8ea8-6bbba2e33a87"
materialTypes['name'] = "RESHARE MATERIAL"
materialTypes['source'] = "reshare"
the_data = json.dumps(materialTypes)
print(the_data)
response = requests.post(url + "/material-types",the_data,headers=headers)
print(response)
print(response.content)

#CREATE INSTANCE CUSTOM IDENTIFIER
instanceIdentifierType = {}
instanceIdentifierType['id'] = 'b514359d-8108-4760-9e5e-6ae2e8fee7f8'
instanceIdentifierType['name'] = 'ReShare Request ID'
instanceIdentifierType['source'] = 'reshare'
the_data = json.dumps(instanceIdentifierType)
print(the_data)
response = requests.post(url + "/identifier-types",the_data,headers=headers)
print(response)
print(response.content)