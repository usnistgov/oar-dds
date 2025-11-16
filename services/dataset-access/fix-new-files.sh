#!/bin/bash

SERVICE_DIR="/Users/one1/oar-dist-ms/services/dataset-access/src/main/java"

echo "Fixing newly copied files..."

# Fix FileCopyRestorer package
sed -i '' 's/package gov\.nist\.oar\.distrib\.cachemgr\.restore;/package gov.nist.oar.distrib.service.ds.cachemgr.restore;/g' \
  "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/cachemgr/restore/FileCopyRestorer.java"

# Fix NullCacheVolume package
sed -i '' 's/package gov\.nist\.oar\.distrib\.cachemgr\.storage;/package gov.nist.oar.distrib.service.ds.cachemgr.storage;/g' \
  "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/cachemgr/storage/NullCacheVolume.java"

# Fix WebLongTermStorage package
sed -i '' 's/package gov\.nist\.oar\.distrib\.storage;/package gov.nist.oar.distrib.service.ds.storage;/g' \
  "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/storage/WebLongTermStorage.java"

# Fix FromBagFileDownloadService package
sed -i '' 's/package gov\.nist\.oar\.distrib\.service;/package gov.nist.oar.distrib.service.ds.service;/g' \
  "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/service/FromBagFileDownloadService.java"

# Fix imports in all these files
find "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/cachemgr/restore" -name "*.java" -exec sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.Checksum;/import gov.nist.oar.distrib.service.ds.storage.Checksum;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StorageVolumeException;/import gov.nist.oar.distrib.service.ds.exception.StorageVolumeException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.LongTermStorage;/import gov.nist.oar.distrib.service.ds.storage.LongTermStorage;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.cachemgr\./import gov.nist.oar.distrib.service.ds.cachemgr./g' \
  {} \;

find "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/cachemgr/storage" -name "NullCacheVolume.java" -exec sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.Checksum;/import gov.nist.oar.distrib.service.ds.storage.Checksum;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.cachemgr\./import gov.nist.oar.distrib.service.ds.cachemgr./g' \
  {} \;

find "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/storage" -name "WebLongTermStorage.java" -exec sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.Checksum;/import gov.nist.oar.distrib.service.ds.storage.Checksum;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.DistributionException;/import gov.nist.oar.distrib.service.ds.exception.DistributionException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.ResourceNotFoundException;/import gov.nist.oar.distrib.service.ds.exception.ResourceNotFoundException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.ObjectNotFoundException;/import gov.nist.oar.distrib.service.ds.exception.ObjectNotFoundException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StorageVolumeException;/import gov.nist.oar.distrib.service.ds.exception.StorageVolumeException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StorageStateException;/import gov.nist.oar.distrib.service.ds.exception.StorageStateException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.FileDescription;/import gov.nist.oar.distrib.service.ds.model.FileDescription;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StreamHandle;/import gov.nist.oar.distrib.service.ds.model.StreamHandle;/g' \
  {} \;

find "$SERVICE_DIR/gov/nist/oar/distrib/service/ds/service" -name "FromBagFileDownloadService.java" -exec sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.Checksum;/import gov.nist.oar.distrib.service.ds.storage.Checksum;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.DistributionException;/import gov.nist.oar.distrib.service.ds.exception.DistributionException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.ResourceNotFoundException;/import gov.nist.oar.distrib.service.ds.exception.ResourceNotFoundException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.FileDescription;/import gov.nist.oar.distrib.service.ds.model.FileDescription;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StreamHandle;/import gov.nist.oar.distrib.service.ds.model.StreamHandle;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.BagStorage;/import gov.nist.oar.distrib.service.ds.storage.BagStorage;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.FileDownloadService;/import gov.nist.oar.distrib.service.ds.service.FileDownloadService;/g' \
  {} \;

echo "Done fixing new files!"
