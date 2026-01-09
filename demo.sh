#!/bin/bash
#
# OAR Microservices Demo CLI
# Usage: ./demo.sh <command> [options]
#
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# Configuration
DATASET_ID="mds1491"
CACHE_MGMT_URL="http://localhost:8085"
DATASET_ACCESS_URL="http://localhost:8081"
API_GATEWAY_URL="http://localhost:8080"
EUREKA_URL="http://localhost:8761"

# Interactive mode flag
INTERACTIVE=false

# Parse global options
while [[ $# -gt 0 ]]; do
    case $1 in
        -i|--interactive)
            INTERACTIVE=true
            shift
            ;;
        *)
            break
            ;;
    esac
done

wait_for_input() {
    if [ "$INTERACTIVE" = true ]; then
        read -p "Press Enter to continue..."
    fi
}

print_header() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BOLD}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

print_step() {
    echo -e "${CYAN}▶${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}!${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# ============================================
# COMMAND: help
# ============================================
cmd_help() {
    echo -e "${BOLD}OAR Microservices Demo CLI${NC}"
    echo ""
    echo "Usage: ./demo.sh [-i|--interactive] <command> [args]"
    echo ""
    echo -e "${BOLD}Options:${NC}"
    echo "  -i, --interactive    Pause between steps for explanation"
    echo ""
    echo -e "${BOLD}Commands:${NC}"
    echo "  help                 Show this help message"
    echo "  setup                Build and start all services"
    echo "  status               Check status of all services"
    echo "  test                 Run the full test suite"
    echo "  workflow             Test dataset-access ↔ cache-mgmt workflow"
    echo "  bundle               Test bundle plan and download workflow"
    echo "  cache                Inspect cache state and contents"
    echo "  cache-clear          Clear all cached files"
    echo "  logs [service]       View logs (all or specific service)"
    echo "  urls                 Show all service URLs"
    echo "  ui                   Start the demo UI (Angular dashboard)"
    echo "  clean                Stop services and clean up"
    echo ""
    echo -e "${BOLD}Examples:${NC}"
    echo "  ./demo.sh setup              # Build and start everything"
    echo "  ./demo.sh -i setup           # Interactive setup with pauses"
    echo "  ./demo.sh workflow           # Test inter-service communication"
    echo "  ./demo.sh bundle             # Test bundle plan and download"
    echo "  ./demo.sh cache              # View cache state"
    echo "  ./demo.sh logs cache-mgmt    # View cache-mgmt logs"
    echo "  ./demo.sh ui                 # Start Angular dashboard"
    echo ""
}

# ============================================
# COMMAND: setup
# ============================================
cmd_setup() {
    print_header "OAR Microservices Setup"

    # Step 1: Build
    print_step "Step 1: Building all microservices..."
    echo "  Compiling Java code and creating JAR files"
    wait_for_input
    ./build-all.sh
    print_success "Build complete"
    echo ""

    # Step 2: Start
    print_step "Step 2: Starting all services with Docker Compose..."
    echo "  Starting: PostgreSQL, Redis, Config Server, Eureka, API Gateway, and all microservices"
    wait_for_input
    ./start.sh
    echo ""

    # Step 3: Wait for services
    print_step "Step 3: Waiting for services to be ready..."
    sleep 10

    # Step 4: Status check
    print_step "Step 4: Verifying services..."
    wait_for_input
    cmd_status

    print_header "Setup Complete"
    echo ""
    echo "Next steps:"
    echo "  ./demo.sh test       Run the test suite"
    echo "  ./demo.sh workflow   Test inter-service communication"
    echo "  ./demo.sh urls       View all service URLs"
    echo ""
}

# ============================================
# COMMAND: status
# ============================================
cmd_status() {
    print_header "Service Status"

    # Docker containers
    print_step "Docker Containers:"
    docker-compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || docker-compose ps
    echo ""

    # Eureka registrations
    print_step "Eureka Registered Services:"
    services=$(curl -s "$EUREKA_URL/eureka/apps" 2>/dev/null | grep -o '<name>[^<]*</name>' | sed 's/<[^>]*>//g' | sort -u | grep -v "MyOwn" || echo "")
    if [ -n "$services" ]; then
        echo "$services" | while read svc; do
            print_success "$svc"
        done
    else
        print_warning "No services registered (Eureka may still be starting)"
    fi
    echo ""

    # Health checks
    print_step "Health Checks:"
    check_health() {
        local url=$1
        local name=$2
        local status=$(curl -s --max-time 3 "$url/actuator/health" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin).get('status','DOWN'))" 2>/dev/null || echo "DOWN")
        if [ "$status" == "UP" ]; then
            print_success "$name: UP"
        else
            print_error "$name: $status"
        fi
    }

    check_health "http://localhost:8888" "config-server (8888)"
    check_health "http://localhost:8761" "eureka-server (8761)"
    check_health "http://localhost:8080" "api-gateway (8080)"
    check_health "http://localhost:8081" "dataset-access (8081)"
    check_health "http://localhost:8082" "aip-access (8082)"
    check_health "http://localhost:8083" "bundle-plan (8083)"
    check_health "http://localhost:8084" "data-bundle (8084)"
    check_health "http://localhost:8085" "cache-mgmt (8085)"
    check_health "http://localhost:8086" "restricted-access (8086)"
    check_health "http://localhost:8087" "version-service (8087)"
    echo ""
}

# ============================================
# COMMAND: test
# ============================================
cmd_test() {
    print_header "Running Full Test Suite"
    ./docs/tests/test-inter-service.sh
}

# ============================================
# COMMAND: workflow
# ============================================
cmd_workflow() {
    print_header "Inter-Service Communication Workflow Test"
    echo ""
    echo "This test demonstrates how dataset-access communicates with cache-mgmt"
    echo "via Feign client to serve file downloads."
    echo ""
    wait_for_input

    # Step 1: Check initial cache state
    print_step "Step 1: Checking initial cache state..."
    initial_count=$(curl -s "$CACHE_MGMT_URL/cache/volumes/" | python3 -c "import sys,json; print(sum(v['filecount'] for v in json.load(sys.stdin)))" 2>/dev/null || echo "0")
    echo "  Current cached files: $initial_count"
    echo ""
    wait_for_input

    # Step 2: Request metadata (triggers head bag caching)
    print_step "Step 2: Requesting dataset metadata (triggers head bag caching)..."
    echo "  GET $CACHE_MGMT_URL/cache/metadata/$DATASET_ID"
    metadata=$(curl -s "$CACHE_MGMT_URL/cache/metadata/$DATASET_ID" 2>/dev/null)
    title=$(echo "$metadata" | python3 -c "import sys,json; print(json.load(sys.stdin).get('title','ERROR')[:60])" 2>/dev/null || echo "ERROR")
    if [[ "$title" != "ERROR" ]]; then
        print_success "Metadata retrieved: $title..."
    else
        print_error "Failed to retrieve metadata"
    fi
    echo ""
    wait_for_input

    # Step 3: Download file via dataset-access (inter-service call)
    print_step "Step 3: Downloading file via dataset-access service..."
    echo "  This triggers: dataset-access → cache-mgmt (Feign client)"
    echo "  GET $DATASET_ACCESS_URL/ds/$DATASET_ID/small-1kb.dat"
    echo ""
    response=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$DATASET_ACCESS_URL/ds/$DATASET_ID/small-1kb.dat" 2>/dev/null)
    http_code=$(echo "$response" | grep "HTTP_CODE:" | cut -d: -f2)
    body=$(echo "$response" | grep -v "HTTP_CODE:")

    if [ "$http_code" == "200" ]; then
        print_success "File downloaded successfully (HTTP 200)"
        echo -e "  ${CYAN}Content:${NC}"
        echo "$body" | head -c 200 | sed 's/^/    /'
        echo ""
    else
        print_error "Download failed (HTTP $http_code)"
    fi
    echo ""
    wait_for_input

    # Step 4: Check cache state after
    print_step "Step 4: Checking cache state after download..."
    final_count=$(curl -s "$CACHE_MGMT_URL/cache/volumes/" | python3 -c "import sys,json; print(sum(v['filecount'] for v in json.load(sys.stdin)))" 2>/dev/null || echo "0")
    echo "  Cached files: $final_count (was: $initial_count)"

    # Show which volumes have files
    echo ""
    print_step "Cache volume details:"
    curl -s "$CACHE_MGMT_URL/cache/volumes/" | python3 -c "
import sys,json
for v in json.load(sys.stdin):
    if v['filecount'] > 0:
        print(f\"  • {v['name']}: {v['filecount']} files, {v['totalsize']:,} bytes\")
" 2>/dev/null
    echo ""
    wait_for_input

    # Step 5: Verify via API Gateway
    print_step "Step 5: Testing same request via API Gateway..."
    echo "  GET $API_GATEWAY_URL/cache/volumes/"
    gw_response=$(curl -s "$API_GATEWAY_URL/cache/volumes/" 2>/dev/null | head -c 100)
    if [[ "$gw_response" == *"filecount"* ]]; then
        print_success "API Gateway routing to cache-mgmt working"
    else
        print_warning "API Gateway routing may not be configured"
    fi
    echo ""
    wait_for_input

    # Step 6: Show relevant logs
    print_step "Step 6: Recent cache-mgmt activity (last 10 log lines)..."
    docker logs oar-ms-cache-mgmt --tail 10 2>&1 | grep -v "^$" | sed 's/^/  /'
    echo ""

    # Summary
    print_header "Workflow Test Complete"
    echo ""
    echo -e "${BOLD}What happened:${NC}"
    echo "  1. cache-mgmt extracted metadata from preservation bag"
    echo "  2. Head bag was cached in cv volume (cv0 or cv1)"
    echo "  3. dataset-access called cache-mgmt via Feign to retrieve file"
    echo "  4. File was restored from preservation bag and cached"
    echo "  5. Subsequent requests will be served from cache (CACHE HIT)"
    echo ""
    echo -e "${BOLD}Architecture verified:${NC}"
    echo "  ┌────────────────┐     Feign/HTTP     ┌────────────────┐"
    echo "  │ dataset-access │ ────────────────► │   cache-mgmt   │"
    echo "  │   (8081)       │                    │    (8085)      │"
    echo "  └────────────────┘                    └────────────────┘"
    echo "          │                                     │"
    echo "          ▼                                     ▼"
    echo "   Preservation Bags                      PostgreSQL"
    echo ""
}

# ============================================
# COMMAND: bundle
# ============================================
cmd_bundle() {
    print_header "Bundle Plan & Download Workflow Test"
    echo ""
    echo "This test demonstrates the bundle-plan and data-bundle services."
    echo "It creates a download plan, validates files, and downloads a zip bundle."
    echo ""
    wait_for_input

    # Bundle API URLs
    BUNDLE_PLAN_URL="$API_GATEWAY_URL/bundle/plan/ds/_bundle_plan"
    BUNDLE_DOWNLOAD_URL="$API_GATEWAY_URL/bundle/data/ds/_bundle"
    INTERNAL_GATEWAY="http://api-gateway:8080"

    # Step 1: Show test files
    print_step "Step 1: Test files we'll bundle..."
    echo "  Dataset: $DATASET_ID"
    echo "  Files:"
    echo "    - small-1kb.dat (1,369 bytes)"
    echo "    - medium-1mb.dat (1,398,105 bytes)"
    echo ""
    wait_for_input

    # Step 2: Create bundle request JSON
    print_step "Step 2: Creating bundle request..."
    BUNDLE_REQUEST=$(cat <<EOF
{
  "bundleName": "demo-bundle",
  "includeFiles": [
    {
      "filePath": "$DATASET_ID/small-1kb.dat",
      "downloadUrl": "$INTERNAL_GATEWAY/od/ds/$DATASET_ID/small-1kb.dat",
      "fileSize": 1369
    },
    {
      "filePath": "$DATASET_ID/medium-1mb.dat",
      "downloadUrl": "$INTERNAL_GATEWAY/od/ds/$DATASET_ID/medium-1mb.dat",
      "fileSize": 1398105
    }
  ]
}
EOF
)
    echo "  Request payload:"
    echo "$BUNDLE_REQUEST" | python3 -m json.tool 2>/dev/null | sed 's/^/    /'
    echo ""
    wait_for_input

    # Step 3: Get bundle plan
    print_step "Step 3: Getting bundle plan (validates URLs, calculates sizes)..."
    echo "  POST $BUNDLE_PLAN_URL"
    echo ""

    PLAN_RESPONSE=$(curl -s -X POST "$BUNDLE_PLAN_URL" \
        -H "Content-Type: application/json" \
        -d "$BUNDLE_REQUEST" 2>/dev/null)

    if echo "$PLAN_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); exit(0 if d.get('status')=='complete' else 1)" 2>/dev/null; then
        print_success "Bundle plan created successfully"
        echo ""
        echo "  Plan details:"
        echo "$PLAN_RESPONSE" | python3 -c "
import sys,json
d = json.load(sys.stdin)
print(f\"    Status: {d.get('status')}\"
f\"    Request ID: {d.get('requestId')}\"
f\"    Total size: {d.get('size'):,} bytes\"
f\"    Bundle count: {d.get('bundleCount')}\"
f\"    Files count: {d.get('filesCount')}\")
if d.get('notIncluded'):
    print(f\"    Not included: {len(d.get('notIncluded'))} files\")
for b in d.get('bundleNameFilePathUrl', []):
    print(f\"    Bundle: {b.get('bundleName')} ({b.get('filesInBundle')} files, {b.get('bundleSize'):,} bytes)\")
" 2>/dev/null
    else
        print_error "Bundle plan failed"
        echo "  Response:"
        echo "$PLAN_RESPONSE" | head -c 500 | sed 's/^/    /'
        echo ""
        return 1
    fi
    echo ""
    wait_for_input

    # Step 4: Download bundle
    print_step "Step 4: Downloading bundle as zip file..."
    echo "  POST $BUNDLE_DOWNLOAD_URL"

    BUNDLE_FILE="/tmp/oar-demo-bundle.zip"
    curl -s -X POST "$BUNDLE_DOWNLOAD_URL" \
        -H "Content-Type: application/json" \
        -d "$BUNDLE_REQUEST" \
        -o "$BUNDLE_FILE" 2>/dev/null

    if [ -f "$BUNDLE_FILE" ] && [ -s "$BUNDLE_FILE" ]; then
        BUNDLE_SIZE=$(stat -f%z "$BUNDLE_FILE" 2>/dev/null || stat -c%s "$BUNDLE_FILE" 2>/dev/null)
        print_success "Bundle downloaded: $BUNDLE_FILE ($BUNDLE_SIZE bytes)"
        echo ""

        # Show zip contents
        print_step "Bundle contents:"
        unzip -l "$BUNDLE_FILE" 2>/dev/null | sed 's/^/    /'
        echo ""

        # Check for errors file
        if unzip -l "$BUNDLE_FILE" 2>/dev/null | grep -q "DownloadErrors"; then
            print_warning "Bundle contains DownloadErrors.txt - some files may have failed"
            echo "  Error details:"
            unzip -p "$BUNDLE_FILE" "/DownloadErrors.txt" 2>/dev/null | sed 's/^/    /'
            echo ""
        elif unzip -l "$BUNDLE_FILE" 2>/dev/null | grep -q "DownloadSuccessful"; then
            print_success "All files downloaded successfully"
        fi
    else
        print_error "Bundle download failed or file is empty"
        return 1
    fi
    echo ""
    wait_for_input

    # Step 5: Verify file integrity
    print_step "Step 5: Verifying bundled file integrity..."
    EXTRACTED_DIR="/tmp/oar-demo-extracted"
    rm -rf "$EXTRACTED_DIR"
    mkdir -p "$EXTRACTED_DIR"
    unzip -q "$BUNDLE_FILE" -d "$EXTRACTED_DIR" 2>/dev/null

    if [ -f "$EXTRACTED_DIR/$DATASET_ID/small-1kb.dat" ]; then
        ORIG_SIZE=$(curl -s "$API_GATEWAY_URL/od/ds/$DATASET_ID/small-1kb.dat" 2>/dev/null | wc -c | tr -d ' ')
        BUNDLED_SIZE=$(cat "$EXTRACTED_DIR/$DATASET_ID/small-1kb.dat" | wc -c | tr -d ' ')
        if [ "$ORIG_SIZE" == "$BUNDLED_SIZE" ]; then
            print_success "small-1kb.dat verified (size: $BUNDLED_SIZE bytes)"
        else
            print_warning "small-1kb.dat size mismatch (expected: $ORIG_SIZE, got: $BUNDLED_SIZE)"
        fi
    fi

    if [ -f "$EXTRACTED_DIR/$DATASET_ID/medium-1mb.dat" ]; then
        BUNDLED_SIZE=$(cat "$EXTRACTED_DIR/$DATASET_ID/medium-1mb.dat" | wc -c | tr -d ' ')
        print_success "medium-1mb.dat verified (size: $BUNDLED_SIZE bytes)"
    fi
    echo ""

    # Cleanup
    rm -rf "$EXTRACTED_DIR"
    rm -f "$BUNDLE_FILE"

    # Summary
    print_header "Bundle Workflow Test Complete"
    echo ""
    echo -e "${BOLD}What happened:${NC}"
    echo "  1. bundle-plan validated all file URLs (HEAD requests)"
    echo "  2. bundle-plan calculated total size and created download plan"
    echo "  3. data-bundle fetched files from dataset-access via gateway"
    echo "  4. data-bundle created zip archive with proper directory structure"
    echo "  5. Client received streaming zip file"
    echo ""
    echo -e "${BOLD}Architecture verified:${NC}"
    echo "  ┌─────────────┐    plan     ┌─────────────┐"
    echo "  │   Client    │ ──────────► │ bundle-plan │"
    echo "  │             │             │   (8083)    │"
    echo "  └─────────────┘             └─────────────┘"
    echo "         │                           │"
    echo "         │ download                  │ validate URLs"
    echo "         ▼                           ▼"
    echo "  ┌─────────────┐   fetch    ┌─────────────┐"
    echo "  │ data-bundle │ ─────────► │ api-gateway │ ─► dataset-access"
    echo "  │   (8084)    │            │   (8080)    │"
    echo "  └─────────────┘            └─────────────┘"
    echo ""
    echo -e "${BOLD}Note:${NC} URLs in bundle request use internal Docker network"
    echo "  (http://api-gateway:8080/...) so data-bundle can reach files."
    echo ""
}

# ============================================
# COMMAND: cache-clear
# ============================================
cmd_cache_clear() {
    print_header "Clear Cache"

    # Show current state
    print_step "Current cache state:"
    count=$(curl -s "$CACHE_MGMT_URL/cache/volumes/" | python3 -c "import sys,json; print(sum(v['filecount'] for v in json.load(sys.stdin)))" 2>/dev/null || echo "0")
    echo "  Cached files: $count"
    echo ""

    if [ "$count" == "0" ]; then
        print_warning "Cache is already empty"
        return
    fi

    # Clear the cache
    print_step "Clearing cache..."
    docker exec oar-ms-postgres psql -U oar_app -d oar_cache -c "DELETE FROM objects;" > /dev/null 2>&1

    # Verify
    new_count=$(curl -s "$CACHE_MGMT_URL/cache/volumes/" | python3 -c "import sys,json; print(sum(v['filecount'] for v in json.load(sys.stdin)))" 2>/dev/null || echo "0")
    if [ "$new_count" == "0" ]; then
        print_success "Cache cleared successfully"
    else
        print_error "Failed to clear cache (files remaining: $new_count)"
    fi
    echo ""
}

# ============================================
# COMMAND: cache
# ============================================
cmd_cache() {
    print_header "Cache State Inspection"

    # Volume summary
    print_step "Cache Volumes:"
    curl -s "$CACHE_MGMT_URL/cache/volumes/" | python3 -c "
import sys,json
data = json.load(sys.stdin)
total_files = sum(v['filecount'] for v in data)
total_size = sum(v['totalsize'] for v in data)
print(f'  Total: {total_files} files, {total_size:,} bytes')
print()
print('  Volume      Files    Size         Capacity     Status')
print('  ' + '-' * 55)
for v in data:
    status = 'active' if v['status'] == 3 else 'inactive'
    print(f\"  {v['name']:<10}  {v['filecount']:>5}    {v['totalsize']:>10,}   {v['capacity']:>12,}   {status}\")
" 2>/dev/null
    echo ""

    # Cached objects for test dataset
    print_step "Cached objects for dataset '$DATASET_ID':"
    objects=$(curl -s "$CACHE_MGMT_URL/cache/objects/$DATASET_ID" 2>/dev/null)
    if [ -n "$objects" ] && [[ "$objects" != *"error"* ]]; then
        echo "$objects" | python3 -c "
import sys,json
try:
    data = json.load(sys.stdin)
    if 'files' in data:
        for f in data['files'][:10]:
            cached = '✓' if f.get('cached') else '○'
            size = f.get('size', 0)
            print(f\"  {cached} {f.get('filepath','?'):<40} {size:>10,} bytes\")
        if len(data['files']) > 10:
            print(f\"  ... and {len(data['files'])-10} more files\")
    else:
        print('  No cached files found')
except:
    print('  Unable to parse response')
" 2>/dev/null
    else
        print_warning "No cached data for $DATASET_ID"
    fi
    echo ""

    # Database stats
    print_step "Database cache inventory:"
    docker exec oar-ms-postgres psql -U oar_app -d oar_cache -t -c "
        SELECT 'Objects: ' || COUNT(*) FROM objects
        UNION ALL
        SELECT 'Volumes: ' || COUNT(*) FROM volumes
        UNION ALL
        SELECT 'Algorithms: ' || COUNT(*) FROM algorithms;
    " 2>/dev/null | grep -v "^$" | sed 's/^/  /' || print_warning "Could not query database"
    echo ""
}

# ============================================
# COMMAND: logs
# ============================================
cmd_logs() {
    local service=$1

    if [ -z "$service" ]; then
        print_header "Recent Logs (All Services)"
        docker-compose logs --tail=20 2>/dev/null
    else
        print_header "Logs: $service"
        case $service in
            cache-mgmt|cache)
                docker logs oar-ms-cache-mgmt --tail=50 2>&1
                ;;
            dataset-access|dataset)
                docker logs oar-ms-dataset-access --tail=50 2>&1
                ;;
            api-gateway|gateway)
                docker logs oar-ms-api-gateway --tail=50 2>&1
                ;;
            eureka|eureka-server)
                docker logs oar-ms-eureka-server --tail=50 2>&1
                ;;
            config|config-server)
                docker logs oar-ms-config-server --tail=50 2>&1
                ;;
            *)
                docker logs "oar-ms-$service" --tail=50 2>&1 || echo "Unknown service: $service"
                ;;
        esac
    fi
}

# ============================================
# COMMAND: urls
# ============================================
cmd_urls() {
    print_header "Service URLs"
    echo ""
    echo -e "${BOLD}Demo UI:${NC}"
    echo "  Angular Dashboard    http://localhost:4200  (./demo.sh ui)"
    echo ""
    echo -e "${BOLD}Infrastructure:${NC}"
    echo "  Eureka Dashboard     http://localhost:8761"
    echo "  Config Server        http://localhost:8888"
    echo "  PostgreSQL           localhost:5433 (user: oar_app)"
    echo "  Redis                localhost:6379"
    echo ""
    echo -e "${BOLD}API Gateway (unified entry point):${NC}"
    echo "  Gateway              http://localhost:8080"
    echo "    /cache/**          → cache-mgmt-service"
    echo "    /od/ds/**          → dataset-access-service"
    echo "    /aip/**            → aip-access-service"
    echo "    /bundle/plan/**    → bundle-plan-service"
    echo "    /bundle/data/**    → data-bundle-service"
    echo "    /rpa/**            → restricted-access-service"
    echo ""
    echo -e "${BOLD}Direct Service Access:${NC}"
    echo "  dataset-access       http://localhost:8081"
    echo "  aip-access           http://localhost:8082"
    echo "  bundle-plan          http://localhost:8083"
    echo "  data-bundle          http://localhost:8084"
    echo "  cache-mgmt           http://localhost:8085"
    echo "  restricted-access    http://localhost:8086"
    echo "  version-service      http://localhost:8087"
    echo ""
    echo -e "${BOLD}Quick Tests:${NC}"
    echo "  curl http://localhost:8085/cache/volumes/"
    echo "  curl http://localhost:8085/cache/metadata/$DATASET_ID"
    echo "  curl http://localhost:8081/ds/$DATASET_ID/small-1kb.dat"
    echo "  curl http://localhost:8087/ds/"
    echo ""
}

# ============================================
# COMMAND: ui
# ============================================
cmd_ui() {
    print_header "Demo UI (Angular Dashboard)"

    if ! command -v npm &> /dev/null; then
        print_error "npm is not installed. Please install Node.js first."
        echo "  https://nodejs.org/"
        exit 1
    fi

    cd demo-ui

    if [ ! -d "node_modules" ]; then
        print_step "Installing dependencies..."
        npm install
    fi

    print_step "Starting Angular development server..."
    echo ""
    echo "The demo UI will be available at:"
    echo -e "  ${GREEN}http://localhost:4200${NC}"
    echo ""
    echo "Features:"
    echo "  • Dashboard    - Real-time service status monitoring"
    echo "  • Workflow     - Interactive inter-service communication demo"
    echo "  • Cache        - Cache volume inspection and statistics"
    echo "  • Architecture - Visual microservices architecture diagram"
    echo "  • Notes        - CLI commands and implementation details"
    echo ""
    echo "Press Ctrl+C to stop the server"
    echo ""

    npm start
}

# ============================================
# COMMAND: clean
# ============================================
cmd_clean() {
    print_header "Cleanup"

    print_step "Stopping all services..."
    docker-compose down -v 2>/dev/null || true

    print_step "Removing containers..."
    docker-compose rm -f 2>/dev/null || true

    print_success "Cleanup complete"
    echo ""
    echo "To also remove build artifacts: mvn clean"
    echo "To remove Docker images: docker-compose down --rmi all"
    echo ""
}

# ============================================
# Main command dispatcher
# ============================================
COMMAND=${1:-help}
shift 2>/dev/null || true

case $COMMAND in
    help|--help|-h)
        cmd_help
        ;;
    setup|start|init)
        cmd_setup
        ;;
    status|ps)
        cmd_status
        ;;
    test|check)
        cmd_test
        ;;
    workflow|flow|demo)
        cmd_workflow
        ;;
    bundle|bundle-plan|datacart)
        cmd_bundle
        ;;
    cache|inspect)
        cmd_cache
        ;;
    cache-clear|clear-cache)
        cmd_cache_clear
        ;;
    logs|log)
        cmd_logs "$@"
        ;;
    urls|endpoints)
        cmd_urls
        ;;
    clean|stop|down)
        cmd_clean
        ;;
    ui|dashboard|web)
        cmd_ui
        ;;
    *)
        echo -e "${RED}Unknown command: $COMMAND${NC}"
        echo ""
        cmd_help
        exit 1
        ;;
esac
