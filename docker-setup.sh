#!/bin/bash

# Docker Setup Script for URL Shortener
# This script sets up the development environment with Docker

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."

    if ! command_exists docker; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi

    if ! command_exists docker-compose; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi

    print_success "Prerequisites check passed"
}

# Create necessary directories
create_directories() {
    print_status "Creating necessary directories..."

    mkdir -p data/postgres
    mkdir -p data/redis
    mkdir -p ssl
    mkdir -p logs

    print_success "Directories created"
}

# Copy configuration files
setup_config() {
    print_status "Setting up configuration files..."

    # Check if application-docker.yml exists
    if [ ! -f "src/main/resources/application-docker.yml" ]; then
        print_warning "application-docker.yml not found. You may need to create it."
    fi

    # Check if redis.conf exists
    if [ ! -f "redis.conf" ]; then
        print_warning "redis.conf not found. Using default Redis configuration."
    fi

    print_success "Configuration setup completed"
}

# Build and start services
start_services() {
    local profile=${1:-""}

    print_status "Building and starting services..."

    if [ -n "$profile" ]; then
        print_status "Using profile: $profile"
        docker-compose --profile "$profile" up --build -d
    else
        docker-compose up --build -d
    fi

    print_success "Services are starting up..."
}

# Wait for services to be healthy
wait_for_services() {
    print_status "Waiting for services to be healthy..."

    local max_attempts=60
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if docker-compose ps | grep -q "Up (healthy)"; then
            print_success "Services are healthy!"
            return 0
        fi

        print_status "Attempt $attempt/$max_attempts - Waiting for services..."
        sleep 5
        ((attempt++))
    done

    print_error "Services did not become healthy within expected time"
    docker-compose logs
    return 1
}

# Show service status
show_status() {
    print_status "Service Status:"
    docker-compose ps

    echo ""
    print_status "Service URLs:"
    echo "• URL Shortener API: http://localhost:8080/api/v1"
    echo "• API Documentation: http://localhost:8080/api/v1/swagger-ui.html"
    echo "• Health Check: http://localhost:8080/api/v1/health"

    if docker-compose ps | grep -q redis-commander; then
        echo "• Redis Commander: http://localhost:8081"
    fi

    if docker-compose ps | grep -q pgadmin; then
        echo "• pgAdmin: http://localhost:8082 (admin@urlshortener.local / admin123)"
    fi

    if docker-compose ps | grep -q nginx; then
        echo "• Nginx Proxy: http://localhost"
    fi
}

# Stop services
stop_services() {
    print_status "Stopping services..."
    docker-compose down
    print_success "Services stopped"
}

# Clean up everything
cleanup() {
    print_status "Cleaning up..."
    docker-compose down -v --remove-orphans

    # Remove any corrupted data directories
    if [ -d "data" ]; then
        print_status "Removing corrupted data directories..."
        rm -rf data/
    fi

    docker system prune -f
    print_success "Cleanup completed"
}

# Show logs
show_logs() {
    local service=${1:-""}

    if [ -n "$service" ]; then
        docker-compose logs -f "$service"
    else
        docker-compose logs -f
    fi
}

# Run tests
run_tests() {
    print_status "Running tests..."

    # Build test image
    docker build -t urlshortener-test --target builder .

    # Run tests in container
    docker run --rm urlshortener-test ./mvnw test

    print_success "Tests completed"
}

# Show usage
usage() {
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  start [profile]     - Start all services (optional profile: tools, nginx)"
    echo "  stop               - Stop all services"
    echo "  restart [profile]  - Restart all services"
    echo "  status             - Show service status"
    echo "  logs [service]     - Show logs (optionally for specific service)"
    echo "  test              - Run tests"
    echo "  cleanup           - Stop services and clean up volumes/images"
    echo "  help              - Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 start                    # Start core services (app, postgres, redis)"
    echo "  $0 start tools              # Start with management tools (redis-commander, pgadmin)"
    echo "  $0 start nginx              # Start with nginx reverse proxy"
    echo "  $0 logs app                 # Show application logs"
    echo "  $0 restart tools            # Restart with tools profile"
}

# Main script logic
main() {
    case ${1:-""} in
        "start")
            check_prerequisites
            create_directories
            setup_config
            start_services "$2"
            wait_for_services
            show_status
            ;;
        "stop")
            stop_services
            ;;
        "restart")
            stop_services
            start_services "$2"
            wait_for_services
            show_status
            ;;
        "status")
            show_status
            ;;
        "logs")
            show_logs "$2"
            ;;
        "test")
            run_tests
            ;;
        "cleanup")
            cleanup
            ;;
        "help"|"--help"|"-h")
            usage
            ;;
        "")
            print_error "No command specified"
            usage
            exit 1
            ;;
        *)
            print_error "Unknown command: $1"
            usage
            exit 1
            ;;
    esac
}

# Run the main function with all arguments
main "$@"