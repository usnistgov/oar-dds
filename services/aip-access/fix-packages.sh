#!/bin/bash

SERVICE_DIR="/Users/one1/oar-dist-ms/services/aip-access/src/main/java"

echo "Fixing package declarations and imports for aip-access-service..."

# Fix AIPAccessController package
sed -i '' 's/package gov\.nist\.oar\.distrib\.web;/package gov.nist.oar.distrib.service.aip.web;/g' \
  "$SERVICE_DIR/gov/nist/oar/distrib/service/aip/web/AIPAccessController.java"

# Fix PreservationBagService package declarations
find "$SERVICE_DIR/gov/nist/oar/distrib/service/aip/service" -name "*.java" -exec sed -i '' \
  -e 's/package gov\.nist\.oar\.distrib\.service\.ds\.service;/package gov.nist.oar.distrib.service.aip.service;/g' \
  -e 's/package gov\.nist\.oar\.distrib\.service;/package gov.nist.oar.distrib.service.aip.service;/g' \
  {} \;

# Fix imports in AIPAccessController
sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.PreservationBagService;/import gov.nist.oar.distrib.service.aip.service.PreservationBagService;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.FileDescription;/import gov.nist.oar.distrib.service.aip.model.FileDescription;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.StreamHandle;/import gov.nist.oar.distrib.service.aip.model.StreamHandle;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.ResourceNotFoundException;/import gov.nist.oar.distrib.service.aip.exception.ResourceNotFoundException;/g' \
  -e 's/import gov\.nist\.oar\.distrib\.DistributionException;/import gov.nist.oar.distrib.service.aip.exception.DistributionException;/g' \
  "$SERVICE_DIR/gov/nist/oar/distrib/service/aip/web/AIPAccessController.java"

# Fix imports in service files
find "$SERVICE_DIR/gov/nist/oar/distrib/service/aip/service" -name "*.java" -exec sed -i '' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.ds\.model\./import gov.nist.oar.distrib.service.aip.model./g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.ds\.exception\./import gov.nist.oar.distrib.service.aip.exception./g' \
  -e 's/import gov\.nist\.oar\.distrib\.service\.ds\.storage\./import gov.nist.oar.distrib.service.aip.storage./g' \
  {} \;

echo "Done fixing aip-access packages!"
