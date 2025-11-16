#!/bin/bash

SERVICE_DIR="/Users/one1/oar-dist-ms/services/dataset-access/src/main/java"

echo "Fixing all package declarations and imports..."

# Fix all files in cachemgr/restore directory
find "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/cachemgr/restore" -name "*.java" -exec sed -i '' \
  -e 's/package gov\.nist\.oar\.distrib\.cachemgr\.restore;/package gov.nist.oar.distrib.service.ds.cachemgr.restore;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.Checksum;/import gov.nist.oar.distrib.service.ds.storage.Checksum;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.ObjectNotFoundException;/import gov.nist.oar.distrib.service.ds.exception.ObjectNotFoundException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StorageVolumeException;/import gov.nist.oar.distrib.service.ds.exception.StorageVolumeException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.LongTermStorage;/import gov.nist.oar.distrib.service.ds.storage.LongTermStorage;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.cachemgr\./import gov.nist.oar.distrib.service.ds.cachemgr./g' \
  {} \;

# Fix BagStorage interface
sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.ResourceNotFoundException;/import gov.nist.oar.distrib.service.ds.exception.ResourceNotFoundException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StorageVolumeException;/import gov.nist.oar.distrib.service.ds.exception.StorageVolumeException;/g' \
  "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/storage/BagStorage.java"

# Fix LongTermStorage interface
sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.StorageVolumeException;/import gov.nist.oar.distrib.service.ds.exception.StorageVolumeException;/g' \
  "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/storage/LongTermStorage.java"

# Fix WebLongTermStorage
sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.LongTermStorage;/import gov.nist.oar.distrib.service.ds.storage.LongTermStorage;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.storage\./import gov.nist.oar.distrib.service.ds.storage./g' \
  "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/storage/WebLongTermStorage.java"

# Fix SimpleCacheManager
sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.storage\./import gov.nist.oar.distrib.service.ds.storage./g' \
  "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/cachemgr/simple/SimpleCacheManager.java"

# Fix FromBagFileDownloadService - replace javax.activation with jakarta.activation
sed -i '' \
  -e 's/import javax\.activation\./import jakarta.activation./g' \
  "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/service/FromBagFileDownloadService.java"

# Fix NullCacheVolume
sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.ObjectNotFoundException;/import gov.nist.oar.distrib.service.ds.exception.ObjectNotFoundException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StorageVolumeException;/import gov.nist.oar.distrib.service.ds.exception.StorageVolumeException;/g' \
  "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/cachemgr/storage/NullCacheVolume.java"

echo "Done fixing all packages!"
