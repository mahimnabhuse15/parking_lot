#!/bin/bash
# ==============================================================================
# AutoPark Unified Cloud Packaging Tool
# ==============================================================================
# This script compiles the React frontend and bundles it directly into the
# Spring Boot backend, generating a single, unified, cloud-ready fat JAR.
# ==============================================================================

# Exit immediately if any command fails
set -e

# Resolve the absolute directory path of this script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Color constants for premium console logs
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================================================${NC}"
echo -e "${BLUE}         AutoPark Smart Parking - Unified Cloud Packager                ${NC}"
echo -e "${BLUE}========================================================================${NC}"

# Step 1: Pre-flight Checks
echo -e "\n${CYAN}[1/4] Running pre-flight system checks...${NC}"

if ! command -v node &> /dev/null; then
    echo -e "${RED}Error: Node.js is not installed or not in your PATH.${NC}"
    exit 1
fi
echo -e "Found Node.js: ${GREEN}$(node --version)${NC}"

if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java (JDK) is not installed or not in your PATH.${NC}"
    exit 1
fi
echo -e "Found Java: ${GREEN}$(java -version 2>&1 | head -n 1)${NC}"

# Step 2: Compile React Frontend
echo -e "\n${CYAN}[2/4] Compiling React Frontend with Vite...${NC}"
cd frontend
npm run build
cd ..

# Validate that the React build was successful
if [ ! -d "frontend/dist" ]; then
    echo -e "${RED}Error: React compile failed to produce the dist/ output directory.${NC}"
    exit 1
fi
echo -e "React static assets successfully compiled: ${GREEN}frontend/dist/${NC}"

# Step 3: Bundle Frontend into Spring Boot Static Resources
echo -e "\n${CYAN}[3/4] Bundling React static assets inside Spring Boot static resources...${NC}"

# Create/Clean resources static folder
rm -rf src/main/resources/static
mkdir -p src/main/resources/static

# Copy built assets
cp -R frontend/dist/* src/main/resources/static/
echo -e "React assets successfully copied to: ${GREEN}src/main/resources/static/${NC}"

# Step 4: Compile & Package Spring Boot Backend via local Maven
echo -e "\n${CYAN}[4/4] Packaging Spring Boot unified executable JAR...${NC}"
./apache-maven-3.9.6/bin/mvn clean package -DskipTests

# Validate that the output fat JAR was built
FAT_JAR="target/parking-lot-1.0-SNAPSHOT-exec.jar"
if [ ! -f "$FAT_JAR" ]; then
    echo -e "${RED}Error: Maven build failed to produce the executable fat JAR.${NC}"
    exit 1
fi

# Recreate the output distribution folder
rm -rf dist
mkdir -p dist
cp "$FAT_JAR" dist/autopark.jar

echo -e "\n${GREEN}========================================================================${NC}"
echo -e "${GREEN}  BUILD COMPLETE! Standalone Cloud JAR generated successfully!         ${NC}"
echo -e "${GREEN}========================================================================${NC}"
echo -e "Deployable Artifact generated in ${BLUE}$SCRIPT_DIR/dist/${NC}:"
echo -e "  1. ${CYAN}dist/autopark.jar${NC} -> Standalone unified Web JAR (Serves both React UI and REST API)"
echo -e "------------------------------------------------------------------------"
echo -e "To launch your unified application locally: ${YELLOW}java -jar dist/autopark.jar${NC}"
echo -e "Then open: ${YELLOW}http://localhost:8080${NC} inside your browser!"
echo -e "========================================================================"
