default:
    @just --list

clean:
    rm -rf result

build:
    nix build .\#all

run: build
    process-compose -f build/process-compose.yml -t=false backend/run frontend/run

dev target:
    #!/bin/bash
    case {{target}} in
        frontend)
            process-compose -f build/process-compose.yml -t=false backend/run frontend/dev
            ;;
        backend)
            process-compose -f build/process-compose.yml -t=false backend/dev
    esac
