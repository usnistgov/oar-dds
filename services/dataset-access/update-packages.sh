#!/bin/bash

# Script to update package declarations and imports for dataset-access-service

SERVICE_DIR="/Users/one1/oar-dist-ms/services/dataset-access/src/main/java/gov/nist/oar/distrib/service/ds"

echo "Updating package declarations and imports..."

# Update package declaration in controller
sed -i '' 's/package gov\.nist\.oar\.distrib\.web;/package gov.nist.oar.distrib.service.ds.web;/g' "$SERVICE_DIR/web/DatasetAccessController.java"

# Update all import statements across all files
find "$SERVICE_DIR" -name "*.java" -exec sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.DistributionException;/import gov.nist.oar.distrib.service.ds.exception.DistributionException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.ResourceNotFoundException;/import gov.nist.oar.distrib.service.ds.exception.ResourceNotFoundException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.ObjectNotFoundException;/import gov.nist.oar.distrib.service.ds.exception.ObjectNotFoundException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StorageStateException;/import gov.nist.oar.distrib.service.ds.exception.StorageStateException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StorageVolumeException;/import gov.nist.oar.distrib.service.ds.exception.StorageVolumeException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.FileDescription;/import gov.nist.oar.distrib.service.ds.model.FileDescription;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StreamHandle;/import gov.nist.oar.distrib.service.ds.model.StreamHandle;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.BagDescription;/import gov.nist.oar.distrib.service.ds.model.BagDescription;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.FileDownloadService;/import gov.nist.oar.distrib.service.ds.service.FileDownloadService;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.PreservationBagService;/import gov.nist.oar.distrib.service.ds.service.PreservationBagService;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.CacheEnabledFileDownloadService;/import gov.nist.oar.distrib.service.ds.service.CacheEnabledFileDownloadService;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.NerdmDownloadService;/import gov.nist.oar.distrib.service.ds.service.NerdmDownloadService;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.web\.ServiceSyntaxException;/import gov.nist.oar.distrib.service.ds.exception.ServiceSyntaxException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.web\.ErrorInfo;/import gov.nist.oar.distrib.service.ds.exception.ErrorInfo;/g' \
  {} \;

echo "Package declarations and imports updated successfully!"
