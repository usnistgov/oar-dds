#!/bin/bash

SERVICE_DIR="/Users/one1/oar-dist-ms/services/dataset-access/src/main/java"

echo "Fixing all import statements and package declarations..."

# Update storage-related imports
find "$SERVICE_DIR" -name "*.java" -exec sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.BagStorage;/import gov.nist.oar.distrib.service.ds.storage.BagStorage;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.LongTermStorage;/import gov.nist.oar.distrib.service.ds.storage.LongTermStorage;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.Checksum;/import gov.nist.oar.distrib.service.ds.storage.Checksum;/g' \
  {} \;

# Update cache manager imports
find "$SERVICE_DIR" -name "*.java" -exec sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.cachemgr\.CacheObject;/import gov.nist.oar.distrib.service.ds.cachemgr.CacheObject;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.cachemgr\.CacheManager;/import gov.nist.oar.distrib.service.ds.cachemgr.CacheManager;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.cachemgr\.CacheManagementException;/import gov.nist.oar.distrib.service.ds.cachemgr.CacheManagementException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.cachemgr\.pdr\./import gov.nist.oar.distrib.service.ds.cachemgr./g' \
  {} \;

# Update service exception imports
find "$SERVICE_DIR" -name "*.java" -exec sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.NerdmNotFoundException;/import gov.nist.oar.distrib.service.ds.service.NerdmNotFoundException;/g' \
  {} \;

# Update package declarations in storage classes
find "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/storage" -name "*.java" -exec sed -i '' \
  -e 's/package gov\.nist\.oar\.distrib;/package gov.nist.oar.distrib.service.ds.storage;/g' \
  -e 's/package gov\.nist\.oar\.distrib\.storage;/package gov.nist.oar.distrib.service.ds.storage;/g' \
  {} \;

# Update package declarations in cachemgr classes
find "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/cachemgr" -name "*.java" -exec sed -i '' \
  -e 's/package gov\.nist\.oar\.distrib\.cachemgr;/package gov.nist.oar.distrib.service.ds.cachemgr;/g' \
  -e 's/package gov\.nist\.oar\.distrib\.cachemgr\.pdr;/package gov.nist.oar.distrib.service.ds.cachemgr;/g' \
  {} \;

# Update imports within cachemgr classes to point to new locations
find "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/cachemgr" -name "*.java" -exec sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.Checksum;/import gov.nist.oar.distrib.service.ds.storage.Checksum;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StorageVolumeException;/import gov.nist.oar.distrib.service.ds.exception.StorageVolumeException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.BagStorage;/import gov.nist.oar.distrib.service.ds.storage.BagStorage;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.LongTermStorage;/import gov.nist.oar.distrib.service.ds.storage.LongTermStorage;/g' \
  {} \;

# Update imports within storage classes
find "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/storage" -name "*.java" -exec sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.Checksum;/import gov.nist.oar.distrib.service.ds.storage.Checksum;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.DistributionException;/import gov.nist.oar.distrib.service.ds.exception.DistributionException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.ResourceNotFoundException;/import gov.nist.oar.distrib.service.ds.exception.ResourceNotFoundException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.ObjectNotFoundException;/import gov.nist.oar.distrib.service.ds.exception.ObjectNotFoundException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StorageVolumeException;/import gov.nist.oar.distrib.service.ds.exception.StorageVolumeException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StorageStateException;/import gov.nist.oar.distrib.service.ds.exception.StorageStateException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.FileDescription;/import gov.nist.oar.distrib.service.ds.model.FileDescription;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StreamHandle;/import gov.nist.oar.distrib.service.ds.model.StreamHandle;/g' \
  {} \;

echo "All imports and package declarations fixed!"
