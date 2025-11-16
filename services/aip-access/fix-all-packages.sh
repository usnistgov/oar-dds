#!/bin/bash

SERVICE_DIR="/Users/one1/oar-dist-ms/services/aip-access/src/main/java"

echo "Fixing all package declarations and imports for aip-access-service..."

# Fix web controller package and imports
sed -i '' \
  -e 's/package gov\.nist\.oar\.distrib\.web;/package gov.nist.oar.distrib.service.aip.web;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.PreservationBagService;/import gov.nist.oar.distrib.service.aip.service.PreservationBagService;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.FileDescription;/import gov.nist.oar.distrib.service.aip.model.FileDescription;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StreamHandle;/import gov.nist.oar.distrib.service.aip.model.StreamHandle;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.ResourceNotFoundException;/import gov.nist.oar.distrib.service.aip.exception.ResourceNotFoundException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.DistributionException;/import gov.nist.oar.distrib.service.aip.exception.DistributionException;/g' \
  "$SERVICE_DIR/gov/nist/oar/distrib/service/aip/web/AIPAccessController.java"

# Fix service package declarations and imports
find "$SERVICE_DIR/gov/nist/oar/distrib/service/aip/service" -name "*.java" -exec sed -i '' \
  -e 's/package gov\.nist\.oar\.distrib\.service\.ds\.service;/package gov.nist.oar.distrib.service.aip.service;/g' \
  -e 's/package gov\.nist\.oar\.distrib\.service;/package gov.nist.oar.distrib.service.aip.service;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.ds\.model\./import gov.nist.oar.distrib.service.aip.model./g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.ds\.exception\./import gov.nist.oar.distrib.service.aip.exception./g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.ds\.storage\./import gov.nist.oar.distrib.service.aip.storage./g' \
  {} \;

# Fix model package declarations
find "$SERVICE_DIR/gov/nist/oar/distrib/service/aip/model" -name "*.java" -exec sed -i '' \
  -e 's/package gov\.nist\.oar\.distrib\.service\.ds\.model;/package gov.nist.oar.distrib.service.aip.model;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.ds\.storage\.Checksum;/import gov.nist.oar.distrib.service.aip.storage.Checksum;/g' \
  {} \;

# Fix exception package declarations
find "$SERVICE_DIR/gov/nist/oar/distrib/service/aip/exception" -name "*.java" -exec sed -i '' \
  -e 's/package gov\.nist\.oar\.distrib\.service\.ds\.exception;/package gov.nist.oar.distrib.service.aip.exception;/g' \
  {} \;

# Fix storage package declarations and imports
find "$SERVICE_DIR/gov/nist/oar/distrib/service/aip/storage" -name "*.java" -exec sed -i '' \
  -e 's/package gov\.nist\.oar\.distrib\.service\.ds\.storage;/package gov.nist.oar.distrib.service.aip.storage;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.ds\.exception\./import gov.nist.oar.distrib.service.aip.exception./g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.ds\.model\./import gov.nist.oar.distrib.service.aip.model./g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.ds\.storage\./import gov.nist.oar.distrib.service.aip.storage./g' \
  {} \;

echo "Done fixing all packages!"
